# Changelog

## [Unreleased]


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
