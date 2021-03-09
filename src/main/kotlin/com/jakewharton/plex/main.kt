@file:JvmName("OrphanedFiles")

package com.jakewharton.plex

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.convert
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.help
import com.github.ajalt.clikt.parameters.options.multiple
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import kotlinx.coroutines.job
import kotlinx.coroutines.runBlocking
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import okhttp3.logging.HttpLoggingInterceptor.Level.BASIC
import java.nio.file.FileSystem
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.isDirectory
import kotlin.streams.toList
import kotlin.system.exitProcess

private class OrphanedFilesCommand(
	private val fs: FileSystem,
) : CliktCommand(
	name = "plex-orphaned-files",
	help = "Find files in your Plex libraries which are not indexed by Plex.",
) {
	private val baseUrl by option(metavar = "URL")
		.help("Base URL of Plex server web interface (e.g., http://plex:32400/)")
		.convert { it.toHttpUrl() }
		.required()

	private val token by option(metavar = "TOKEN")
		.help("Plex authentication token. See: https://support.plex.tv/articles/204059436-finding-an-authentication-token-x-plex-token/")
		.required()

	private val folderMappings by option("--folder-mapping", metavar = "MAPPING")
		.help("Map a plex folder path to a local filesystem path (e.g., /media:/tank/media)")
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
			from to to
		}
		.multiple()

	private val debug by option(hidden = true).flag()

	private fun String.withFolderMapping(): String {
		for ((from, to) in folderMappings) {
			if (startsWith(from)) {
				return to + substring(from.length)
			}
		}
		return this
	}

	override fun run() {
		val httpLogger = HttpLoggingInterceptor(::println)
		val client = OkHttpClient.Builder()
			.addNetworkInterceptor(httpLogger)
			.build()
		if (debug) {
			httpLogger.level = BASIC
		}

		val plexApi = HttpPlexApi(client, baseUrl, token)

		var hasOrphans = false

		runBlocking {
			coroutineContext.job.invokeOnCompletion {
				client.dispatcher.executorService.shutdown()
				client.connectionPool.evictAll()
			}

			for (section in plexApi.sections()) {
				val locations = section.locations
					.map { it.withFolderMapping() }
					.map(fs::getPath)
					.flatMap { path ->
						Files.walk(path).filter { !it.isDirectory() }
							.map(Path::toString)
							.toList()
					}
					.toSet()

				val paths = plexApi.sectionPaths(section.key)
					.map { it.withFolderMapping() }

				val orphaned = locations - paths
				if (orphaned.isNotEmpty()) {
					hasOrphans = true
				}
				orphaned.forEach {
					println("${section.title}: $it")
				}
			}
		}

		if (hasOrphans) {
			exitProcess(1)
		}
	}
}

fun main(vararg args: String) {
	OrphanedFilesCommand(FileSystems.getDefault()).main(args)
}
