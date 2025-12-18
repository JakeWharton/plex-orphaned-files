# Changelog

## [Unreleased]

### Added

* Linux Arm variant of Docker container.
* `--output` option to direct report to a file, or use `-` to explicitly target stdout (the default).
* `--cron` option to continue running and generate a report based on the provided schedule.
* `--hc-id` and `--hc-host` options for specifying a HealthChecks.io ID and host, respectively, to notify before and after running.
* Docker container now reads environment variables for specifying option values specifically for use in Docker Compose. The environment variables are: `PLEX_ORPHANED_BASE_URL`, `PLEX_ORPHANED_TOKEN`, `PLEX_ORPHANED_CRON`, `PLEX_ORPHANED_HC_ID`, `PLEX_ORPHANED_HC_HOST`, `PLEX_ORPHANED_OUTPUT`.

### Changed

* File paths from Plex are now checked to exist to ensure the tool has access to the same directories.

### Fixed

* Do not report a movie's extras (like trailers, behind the scenes, etc.) as orphaned.
* Do not report an artist's non-album songs (from EPs, live albums, etc.) as orphaned.


## [1.1.1] - 2021-03-12

### Fixed

* Do not crash on empty libraries.


## [1.1.0] - 2021-03-10

### Added

* `--exclude-files` option accepts a [glob](https://en.wikipedia.org/wiki/Glob_(programming)) of file paths to ignore even if they are not indexed by Plex.

  For example, you may ignore `/media/music/**/cover.*` to ignore cover images or `/media/music/**/*.m3u` to ignore album playlists.
* `--exclude-library` option accepts names of Plex libraries to skip checks.
* The command now accepts explicit Plex library names to check. This is mutually exclusive to `--exclude-library`.

  For example, `./plex-orphaned-libraries --base-url .. --token .. Music Photos`


## [1.0.0] - 2021-03-06

 - Initial release


[Unreleased]: https://github.com/JakeWharton/plex-orphaned-files/compare/1.1.1...HEAD
[1.1.1]: https://github.com/JakeWharton/plex-orphaned-files/releases/tag/1.1.1
[1.1.0]: https://github.com/JakeWharton/plex-orphaned-files/releases/tag/1.1.0
[1.0.0]: https://github.com/JakeWharton/plex-orphaned-files/releases/tag/1.0.0
