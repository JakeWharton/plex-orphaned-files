# Plex Orphaned Files

Find files in your Plex libraries which are not indexed by Plex.

Available as a standalone binary and Docker container.


## Usage

You will need a Plex authentication token to use this tool.
See [Plex's documentation](https://support.plex.tv/articles/204059436-finding-an-authentication-token-x-plex-token/) on how to obtain yours.

From there, you can access the tool in one of two ways:

 * [Binary](#binary)
 * [Docker](#docker)

### Binary

Install on Mac with:
```
$ brew install JakeWharton/repo/plex-orphaned-files
```
which will put the `plex-orphaned-files` on your shell path (assuming Homebrew is set up correctly).

For other platforms, download ZIP from
[latest release](https://github.com/JakeWharton/plex-orphaned-files/releases/latest)
and run `bin/plex-orphaned-files` or `bin/plex-orphaned-files.bat`.

See [command-line usage](#command-line) for how to run the binary.

### Docker

The binary is available through Docker to simplify use:
```
$ docker run jakewharton/plex-orphaned-files:1
```

(Image also available at `ghcr.io/jakewharton/plex-orphaned-files:1`)

[![Docker Image Version](https://img.shields.io/docker/v/jakewharton/plex-orphaned-files?sort=semver)][hub]
[![Docker Image Size](https://img.shields.io/docker/image-size/jakewharton/plex-orphaned-files)][layers]

[hub]: https://hub.docker.com/r/jakewharton/plex-orphaned-files/
[layers]: https://microbadger.com/images/jakewharton/plex-orphaned-files

The tool will need to access the filesystem in the same way Plex would. For simplicity, it's easiest
to mirror the volume mounts of your Plex Docker container. If you are not running Plex in Docker,
you can mount your library folders in the container using the same paths as they exist on the host.

If for whatever reason you cannot mirror the filesystem in the container exactly as Plex sees it,
use the `--folder-mapping` argument to change Plex's file paths into paths that can be read inside
the container.

See [command-line usage](#command-line) for how to run the binary.

### Command-Line

```
Usage: plex-orphaned-files [OPTIONS]

  Find files in your Plex libraries which are not indexed by Plex.

Options:
  --base-url URL            Base URL of Plex server web interface (e.g.,
                            http://plex:32400/)
  --token TOKEN             Plex authentication token. See:
                            https://support.plex.tv/articles/204059436-finding-an-authentication-token-x-plex-token/
  --folder-mapping MAPPING  Map a plex folder path to a local filesystem path
                            (e.g., /media:/tank/media)
  -h, --help                Show this message and exit
```

The `--base-url` and `--token` arguments are required.

When run, the tool will traverse all of your Plex libraries to get their folder paths. Then, it will
obtain every file in those paths and compare it to items in that Plex library. Any files which are
not indexed by Plex will be output, and the command will have an exit code of 1.

```
$ plex-orphaned-files --base-url http://plexms:32400/ --token MY_TOKEN
Photos: /media/photos/Area_51/blueprints.png
```


## Development

To run the latest code build with `./gradlew installDist`.  This will put the application into
`build/install/plex-orphaned-files/`. From there you can use the
[command-line instructions](#command-line) instructions to run.

The Docker containers can be built with `docker build .`, which also runs the full set of checks
as CI would.


# License

    Copyright 2021 Jake Wharton

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
