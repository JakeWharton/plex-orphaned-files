@file:JvmName("Main")

package com.jakewharton.plex

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.help
import com.github.ajalt.clikt.parameters.arguments.multiple
import com.github.ajalt.clikt.parameters.options.convert
import com.github.ajalt.clikt.parameters.options.counted
import com.github.ajalt.clikt.parameters.options.help
import com.github.ajalt.clikt.parameters.options.multiple
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import kotlinx.coroutines.runBlocking
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import okhttp3.logging.HttpLoggingInterceptor.Level.BASIC
import okhttp3.logging.HttpLoggingInterceptor.Level.BODY
import okhttp3.logging.HttpLoggingInterceptor.Level.NONE
import java.nio.file.FileSystem
import java.nio.file.FileSystems
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
			FolderMapping(from, to)
		}
		.multiple()

	private val excludes by option("--exclude", "-e", metavar = "GLOB")
		.help("Glob pattern of files ignore (e.g., /media/**/*.nfo, /music/**/cover.*)")
		.convert {
			fs.getPathMatcher("glob:$it")!!
		}
		.multiple()

	private val libraries by argument(name = "LIBRARY")
		.help("Name of libraries to scan. All libraries will be scanned if none specified")
		.multiple()

	private val debug by option(hidden = true).counted()

	override fun run() {
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

		val plexApi = HttpPlexApi(client, baseUrl, token)
		val orphanedFiles = OrphanedFiles(
			plexApi = plexApi,
			libraries = libraries.toSet(),
			fileSystem = fs,
			excludes = excludes,
			folderMappings = folderMappings,
			debug = debug > 0,
		)

		val orphans = try {
			runBlocking { orphanedFiles.find() }
		} finally {
			client.dispatcher.executorService.shutdown()
			client.connectionPool.evictAll()
		}

		if (orphans.isNotEmpty()) {
			orphans.forEach {
				println("${it.section}: ${it.path}")
			}
			exitProcess(1)
		}
	}
}

fun main(vararg args: String) {
	OrphanedFilesCommand(FileSystems.getDefault()).main(args)
}
