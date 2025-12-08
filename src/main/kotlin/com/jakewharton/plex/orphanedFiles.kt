package com.jakewharton.plex

import java.nio.file.FileSystem
import java.nio.file.Path
import java.nio.file.PathMatcher
import kotlin.io.path.exists
import kotlin.io.path.isDirectory
import kotlin.io.path.notExists
import kotlin.io.path.walk

class OrphanedFiles(
	private val debug: Boolean = false,
	private val plexApi: PlexApi,
	private val libraries: Set<String> = emptySet(),
	private val libraryExcludes: Set<String> = emptySet(),
	private val fileSystem: FileSystem,
	private val fileExcludes: List<PathMatcher> = emptyList(),
	private val folderMappings: List<FolderMapping> = emptyList(),
) {
	init {
		require(libraries.isEmpty() or libraryExcludes.isEmpty()) {
			"Libraries and library excludes are mutually exclusive. Specify neither or one, not both."
		}
	}

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
			if (libraries.isNotEmpty() && section.title !in libraries || section.title in libraryExcludes) {
				if (debug) {
					println("Skipping ${section.title}...")
				}
				continue
			}
			if (debug) {
				println("Checking ${section.title}...")
			}

			val paths = plexApi.sectionPaths(section.key)
				.mapTo(LinkedHashSet()) { it.withFolderMapping() }

			for (path in paths) {
				val realPath = fileSystem.getPath(path)
				require(realPath.exists()) {
					"${section.title} path $path not found. Did you mount and map the directories correctly?"
				}
			}

			val locations = section.locations
				.map { it.withFolderMapping() }
				.map(fileSystem::getPath)
				.flatMap { path ->
					path.walk()
						.filter { !it.isDirectory() }
						.filter { file -> fileExcludes.none { it.matches(file) } }
						.map(Path::toString)
				}

			for (location in locations) {
				if (location !in paths) {
					add(OrphanedFile(section.title, location))
				}
			}
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
