package com.jakewharton.plex

import java.nio.file.FileSystem
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.PathMatcher
import kotlin.io.path.isDirectory
import kotlin.streams.toList

class OrphanedFiles(
	private val debug: Boolean = false,
	private val plexApi: PlexApi,
	private val libraries: Set<String> = emptySet(),
	private val fileSystem: FileSystem,
	private val excludes: List<PathMatcher> = emptyList(),
	private val folderMappings: List<FolderMapping> = emptyList(),
) {
	private fun String.withFolderMapping(): String {
		for ((from, to) in folderMappings) {
			if (startsWith(from)) {
				return to + substring(from.length)
			}
		}
		return this
	}

	suspend fun find() = buildList {
		for (section in plexApi.sections()) {
			if (libraries.isNotEmpty() && section.title !in libraries) {
				if (debug) {
					println("Skipping ${section.title}...")
				}
				continue
			}
			if (debug) {
				println("Checking ${section.title}...")
			}

			val locations = section.locations
				.map { it.withFolderMapping() }
				.map(fileSystem::getPath)
				.flatMap { path ->
					Files.walk(path)
						.filter { !it.isDirectory() }
						.filter { file -> excludes.none { it.matches(file) } }
						.map(Path::toString)
						.toList()
				}
				.toSet()

			val paths = plexApi.sectionPaths(section.key)
				.map { it.withFolderMapping() }

			val orphaned = locations - paths
			if (debug) {
				println("Found ${orphaned.size} orphan(s)")
			}
			addAll(orphaned.map {
				OrphanedFile(section.title, it)
			})
		}
	}
}

data class OrphanedFile(
	val section: String,
	val path: String,
)

data class FolderMapping(
	val from: String,
	val to: String,
)
