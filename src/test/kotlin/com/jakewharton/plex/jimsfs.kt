package com.jakewharton.plex

import com.google.common.jimfs.Configuration
import com.google.common.jimfs.Configuration.unix
import com.google.common.jimfs.Jimfs
import java.nio.file.FileSystem
import java.nio.file.Path
import kotlin.io.path.createDirectory
import kotlin.io.path.createFile

fun fakeFs(configuration: Configuration = unix(), manipulator: DirectoryManipulator.() -> Unit): FileSystem {
	val fs = Jimfs.newFileSystem(configuration)
	val root = fs.rootDirectories.first()
	PathDirectoryManipulator(root).apply(manipulator)
	return fs
}

interface DirectoryManipulator {
	fun dir(name: String, manipulator: (DirectoryManipulator.() -> Unit)? = null)
	fun file(name: String)
}

class PathDirectoryManipulator(
	private val path: Path,
) : DirectoryManipulator {
	override fun dir(name: String, manipulator: (DirectoryManipulator.() -> Unit)?) {
		val child = path.resolve(name)
		child.createDirectory()
		if (manipulator != null) {
			PathDirectoryManipulator(child).apply(manipulator)
		}
	}

	override fun file(name: String) {
		val child = path.resolve(name)
		child.createFile()
	}
}
