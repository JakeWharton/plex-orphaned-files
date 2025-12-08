@file:JvmName("Main")

package com.jakewharton.plex

import com.github.ajalt.clikt.command.SuspendingCliktCommand
import com.github.ajalt.clikt.command.main
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.help
import com.github.ajalt.clikt.parameters.arguments.multiple
import com.github.ajalt.clikt.parameters.options.convert
import com.github.ajalt.clikt.parameters.options.counted
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.help
import com.github.ajalt.clikt.parameters.options.multiple
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import io.github.kevincianfarini.cardiologist.PulseBackpressureStrategy.Companion.SkipNext
import io.github.kevincianfarini.cardiologist.PulseSchedule
import io.github.kevincianfarini.cardiologist.schedulePulse
import java.nio.file.FileSystem
import java.nio.file.FileSystems
import kotlin.io.path.writeText
import kotlin.system.exitProcess
import kotlin.time.Clock
import kotlinx.datetime.TimeZone
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import okhttp3.logging.HttpLoggingInterceptor.Level.BASIC
import okhttp3.logging.HttpLoggingInterceptor.Level.BODY
import okhttp3.logging.HttpLoggingInterceptor.Level.NONE

private class OrphanedFilesCommand(
	private val fs: FileSystem,
	private val clock: Clock,
	private val timeZone: TimeZone,
) : SuspendingCliktCommand("plex-orphaned-files") {
	override fun help(context: Context): String {
		return "Find files in your Plex libraries which are not indexed by Plex."
	}

	private val host by option(metavar = "URL", envvar = "PLEX_ORPHANED_HOST")
		.help("Plex server host web interface (e.g., http://plex:32400/)")
		.convert { it.toHttpUrl() }
		.required()

	private val token by option(metavar = "TOKEN", envvar = "PLEX_ORPHANED_TOKEN")
		.help("Plex authentication token. See: https://support.plex.tv/articles/204059436-finding-an-authentication-token-x-plex-token/")
		.required()

	private val folderMappings by option("--folder-mapping", metavar = "MAPPING")
		.help("Map a Plex folder path to filesystem path (e.g., /media:/tank/media)")
		.convert {
			val partition = it.indexOf(':')
			require(partition != -1) {
				"Folder mapping must contain colon (:) separating 'from' path from 'to' path: \"$it\""
			}
			val from = it.substring(0, partition)
			require(from.isNotBlank()) {
				"Folder mapping 'from' must not be blank: \"$from\""
			}
			val to = it.substring(partition + 1)
			require(to.isNotBlank()) {
				"Folder mapping 'to' must not be blank: \"$to\""
			}
			FolderMapping(from, to)
		}
		.multiple()

	private val fileExcludes by option("--exclude-files", metavar = "GLOB")
		.help("Glob pattern of files to ignore (e.g., /media/**/*.nfo, /music/**/cover.*)")
		.convert {
			fs.getPathMatcher("glob:$it")!!
		}
		.multiple()

	private val libraryExcludes by option("--exclude-library", metavar = "NAME")
		.help("""
			|Name of libraries to exclude.
			|Mutually exclusive with LIBRARY arguments.
			""".trimMargin())
		.multiple()

	private val libraries by argument(name = "LIBRARY")
		.help("""
			|Name of libraries to scan.
			|All libraries will be scanned if none specified.
			|Mutually exclusive with --exclude-library
			""".trimMargin())
		.multiple()

	private val schedule by option("--cron", metavar = "expression", envvar = "PLEX_ORPHANED_CRON")
		.help("Run command forever and perform sync on this schedule")
		.convert { PulseSchedule.parseCron(it) }

	private val healthCheckId by option("--hc-id", metavar = "id", envvar = "PLEX_ORPHANED_HC_ID")
		.help("ID of Healthchecks.io service to notify")

	private val healthCheckHost by option("--hc-host", metavar = "url", envvar = "PLEX_ORPHANED_HC_HOST")
		.convert { it.toHttpUrl() }
		.default("https://hc-ping.com".toHttpUrl())
		.help("Host of Healthchecks.io service to notify. Requires --hc-id")

	private val output by option("--output", envvar = "PLEX_ORPHANED_OUTPUT")
		.default("-")
		.help("Report destination, or '-' to write to stdout (default)")

	private val debug by option(hidden = true).counted()

	override suspend fun run() {
		val httpLogger = HttpLoggingInterceptor(::println)
			.apply {
				level = when (debug) {
					0, 1 -> NONE
					2 -> BASIC
					else -> BODY
				}
			}
		val client = OkHttpClient.Builder()
			.addNetworkInterceptor(httpLogger)
			.build()

		val plexApi = HttpPlexApi(client, host, token)
		val orphanedFiles = OrphanedFiles(
			plexApi = plexApi,
			libraries = libraries.toSet(),
			libraryExcludes = libraryExcludes.toSet(),
			fileSystem = fs,
			fileExcludes = fileExcludes,
			folderMappings = folderMappings,
			debug = debug > 0,
		)

		val healthCheckService = HealthCheckService(healthCheckHost, client)
		val healthCheck = healthCheckId?.let(healthCheckService::newCheck)

		val hasOrphans = try {
			val schedule = schedule
			if (schedule != null) {
				println("Sync schedule: $schedule")
				val pulse = clock.schedulePulse(schedule, timeZone)
				pulse.beat(strategy = SkipNext) {
					checkForOrphans(orphanedFiles, healthCheck)
				}
				error("unreachable") // https://github.com/kevincianfarini/cardiologist/issues/117
			} else {
				checkForOrphans(orphanedFiles, healthCheck)
			}
		} finally {
			client.dispatcher.executorService.shutdown()
			client.connectionPool.evictAll()
		}

		if (hasOrphans) {
			exitProcess(1)
		}
	}

	private suspend fun checkForOrphans(
		orphanedFiles: OrphanedFiles,
		healthCheck: HealthCheck?,
	): Boolean {
		val startedCheck = healthCheck?.start()

		val orphans = orphanedFiles.find()
		val report = buildString {
			for (orphan in orphans) {
				append(orphan.section)
				append(": ")
				append(orphan.path)
				append('\n')
			}
		}

		if (output == "-") {
			print(report)
		} else {
			fs.getPath(output).writeText(report)
		}

		startedCheck?.complete()

		return orphans.isNotEmpty()
	}
}

suspend fun main(vararg args: String) {
	OrphanedFilesCommand(
		FileSystems.getDefault(),
		Clock.System,
		TimeZone.currentSystemDefault(),
	).main(args)
}
