package com.jakewharton.plex

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import org.junit.Test

class OrphanedFilesTest {
	@Test fun libraryAndLibraryExcludesAreMutuallyExclusive() {
		val plex = fakePlex { }
		val fs = fakeFs { }

		OrphanedFiles(plexApi = plex, fileSystem = fs)
		OrphanedFiles(plexApi = plex, fileSystem = fs, libraries = setOf("Stuff"))
		OrphanedFiles(plexApi = plex, fileSystem = fs, libraryExcludes = setOf("Things"))

		assertThrows<IllegalArgumentException> {
			OrphanedFiles(plexApi = plex,
				fileSystem = fs,
				libraries = setOf("Stuff"),
				libraryExcludes = setOf("Things"),
			)
		}.hasMessageThat()
			.isEqualTo("Libraries and library excludes are mutually exclusive. Specify neither or one, not both.")
	}

	@Test fun emptySection() = runBlocking<Unit> {
		val plex = fakePlex {
			section("Stuff") {
				location("/media")
			}
		}

		val fs = fakeFs {
			dir("media")
		}

		val orphanedFiles = OrphanedFiles(
			plexApi = plex,
			fileSystem = fs,
		)
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

		val orphanedFiles = OrphanedFiles(
			plexApi = plex,
			fileSystem = fs,
		)
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

		val orphanedFiles = OrphanedFiles(
			plexApi = plex,
			fileSystem = fs,
		)
		val orphans = orphanedFiles.find()
		assertThat(orphans).isEmpty()
	}

	@Test fun unindexedFileReported() = runBlocking<Unit> {
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
				file("Movie_2.mkv")
			}
		}

		val orphanedFiles = OrphanedFiles(
			plexApi = plex,
			fileSystem = fs,
		)
		val orphans = orphanedFiles.find()
		assertThat(orphans).containsExactly(
			OrphanedFile("Stuff", "/media/Movie_2.mkv")
		)
	}

	@Test fun unindexedFileIgnoredInUnspecifiedLibrary() = runBlocking<Unit> {
		val plex = fakePlex {
			section("Stuff") {
				location("/stuff") {
					file("Movie_1.mkv")
				}
			}
			section("Things") {
				location("/things") {
					file("Movie_1.mkv")
				}
			}
		}

		val fs = fakeFs {
			dir("stuff") {
				file("Movie_1.mkv")
			}
			dir("things") {
				file("Movie_1.mkv")
				file("Movie_2.mkv")
			}
		}

		val orphanedFiles = OrphanedFiles(
			plexApi = plex,
			fileSystem = fs,
			libraries = setOf("Stuff"),
		)
		val orphans = orphanedFiles.find()
		assertThat(orphans).isEmpty()
	}

	@Test fun unindexedFileIgnoredInExcludedLibrary() = runBlocking<Unit> {
		val plex = fakePlex {
			section("Stuff") {
				location("/stuff") {
					file("Movie_1.mkv")
				}
			}
			section("Things") {
				location("/things") {
					file("Movie_1.mkv")
				}
			}
		}

		val fs = fakeFs {
			dir("stuff") {
				file("Movie_1.mkv")
			}
			dir("things") {
				file("Movie_1.mkv")
				file("Movie_2.mkv")
			}
		}

		val orphanedFiles = OrphanedFiles(
			plexApi = plex,
			fileSystem = fs,
			libraryExcludes = setOf("Things"),
		)
		val orphans = orphanedFiles.find()
		assertThat(orphans).isEmpty()
	}

	@Test fun unindexedFileExcludedIsIgnored() = runBlocking<Unit> {
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
				file("Movie_1.nfo")
			}
		}

		val excludes = listOf(
			fs.getPathMatcher("glob:/media/*.nfo")
		)

		val orphanedFiles = OrphanedFiles(
			plexApi = plex,
			fileSystem = fs,
			fileExcludes = excludes,
		)
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

		val orphanedFiles = OrphanedFiles(
			plexApi = plex,
			fileSystem = fs,
			folderMappings = folderMappings,
		)
		val orphans = orphanedFiles.find()
		assertThat(orphans).containsExactly(
			OrphanedFile("Stuff", "/tank/media/Movie_2.mkv")
		)
	}
}
