package com.jakewharton.plex

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import org.junit.Test

class OrphanedFilesTest {
	@Test fun emptySection() = runBlocking<Unit> {
		val plex = fakePlex {
			section("Stuff") {
				location("/media")
			}
		}

		val fs = fakeFs {
			dir("media")
		}

		val orphanedFiles = OrphanedFiles(plex, fs)
		val orphans = orphanedFiles.find()
		assertThat(orphans).isEmpty()
	}

	@Test fun allFilesIndexed() = runBlocking<Unit> {
		val plex = fakePlex {
			section("Stuff") {
				location("/media") {
					file("Movie_1.mkv")
				}
			}
		}

		val fs = fakeFs {
			dir("media") {
				file("Movie_1.mkv")
			}
		}

		val orphanedFiles = OrphanedFiles(plex, fs)
		val orphans = orphanedFiles.find()
		assertThat(orphans).isEmpty()
	}

	@Test fun allFilesIndexedMultipleLocations() = runBlocking<Unit> {
		val plex = fakePlex {
			section("Stuff") {
				location("/media") {
					file("Movie_1.mkv")
				}
				location("/other/stuff") {
					file("Movie_2.mkv")
				}
			}
		}

		val fs = fakeFs {
			dir("media") {
				file("Movie_1.mkv")
			}
			dir("other") {
				dir("stuff") {
					file("Movie_2.mkv")
				}
			}
		}

		val orphanedFiles = OrphanedFiles(plex, fs)
		val orphans = orphanedFiles.find()
		assertThat(orphans).isEmpty()
	}

	@Test fun folderMappingWorks() = runBlocking<Unit> {
		val plex = fakePlex {
			section("Stuff") {
				location("/media") {
					file("Movie_1.mkv")
				}
			}
		}

		val fs = fakeFs {
			dir("tank") {
				dir("media") {
					file("Movie_1.mkv")
					file("Movie_2.mkv")
				}
			}
		}

		val folderMappings = listOf(
			FolderMapping("/media", "/tank/media")
		)

		val orphanedFiles = OrphanedFiles(plex, fs, folderMappings)
		val orphans = orphanedFiles.find()
		assertThat(orphans).containsExactly(
			OrphanedFile("Stuff", "/tank/media/Movie_2.mkv")
		)
	}
}
