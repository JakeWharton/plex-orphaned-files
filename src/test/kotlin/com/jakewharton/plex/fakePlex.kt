package com.jakewharton.plex

import com.google.common.jimfs.Configuration
import com.google.common.jimfs.Configuration.unix
import com.google.common.jimfs.Jimfs
import java.nio.file.FileSystem
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.isDirectory
import kotlin.streams.toList

fun fakePlex(
	configuration: Configuration = unix(),
	manipulator: PlexApiManipulator.() -> Unit,
): PlexApi {
	val fs = Jimfs.newFileSystem(configuration)
	val sections = mutableMapOf<PlexSection, Set<String>>()
	FakePlexApiManipulator(fs, sections).apply(manipulator)
	return FakePlexApi(sections)
}

interface PlexApiManipulator {
	fun section(title: String, manipulator: PlexSectionManipulator.() -> Unit)
}

interface PlexSectionManipulator {
	fun location(path: String, manipulator: (DirectoryManipulator.() -> Unit)? = null)
}

private class FakePlexApiManipulator(
	private val fs: FileSystem,
	private val sections: MutableMap<PlexSection, Set<String>>,
) : PlexApiManipulator {
	override fun section(title: String, manipulator: PlexSectionManipulator.() -> Unit) {
		val sectionManipulator = FakePlexSectionManipulator(fs).apply(manipulator)
		val section = PlexSection(title, title, sectionManipulator.locations.map { it.toString() })
		val paths = sectionManipulator.locations
			.flatMapTo(mutableSetOf<String>()) { location ->
				Files.walk(location)
					.filter { !it.isDirectory() }
					.map(Path::toString)
					.toList()
			}
		if (sections.putIfAbsent(section, paths) != null) {
			throw IllegalArgumentException("Duplicate section: $title")
		}
	}
}

private class FakePlexSectionManipulator(
	private val fs: FileSystem,
) : PlexSectionManipulator {
	val locations = mutableSetOf<Path>()

	override fun location(path: String, manipulator: (DirectoryManipulator.() -> Unit)?) {
		val dir = fs.getPath(path)
		dir.createDirectories()
		if (!locations.add(dir)) {
			throw IllegalArgumentException("Duplication location: $path")
		}
		if (manipulator != null) {
			PathDirectoryManipulator(dir).apply(manipulator)
		}
	}
}

private class FakePlexApi(val sections: Map<PlexSection, Set<String>>) : PlexApi {
	override suspend fun sections(): List<PlexSection> {
		return sections.keys.toList()
	}

	override suspend fun sectionPaths(sectionKey: String): List<String> {
		return sections.entries.firstOrNull { it.key.key == sectionKey }?.value?.toList()
			?: throw IllegalArgumentException("No section with key: $sectionKey")
	}
}
