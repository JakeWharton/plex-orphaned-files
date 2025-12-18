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
$ docker run jakewharton/plex-orphaned-files
```

(Image also available at `ghcr.io/jakewharton/plex-orphaned-files`)

[![Docker Image Version](https://img.shields.io/docker/v/jakewharton/plex-orphaned-files?sort=semver&style=flat-square)][hub]
[![Docker Image Size](https://img.shields.io/docker/image-size/jakewharton/plex-orphaned-files?sort=semver&style=flat-square)][hub]<br>
[![Docker Image Version](https://img.shields.io/docker/v/jakewharton/plex-orphaned-files/trunk?style=flat-square)][hub]
[![Docker Image Size](https://img.shields.io/docker/image-size/jakewharton/plex-orphaned-files/trunk?style=flat-square)][hub]

[hub]: https://hub.docker.com/r/jakewharton/plex-orphaned-files/

The tool will need to access the filesystem in the same way Plex would. For simplicity, it's easiest
to mirror the volume mounts of your Plex Docker container. If you are not running Plex in Docker,
you can mount your library folders in the container using the same paths as they exist on the host.

If for whatever reason you cannot mirror the filesystem in the container exactly as Plex sees it,
use the `--folder-mapping` argument to change Plex's file paths into paths that can be read inside
the container.

By default, the tool will run a single check and then exit.

```
$ docker run \
    jakewharton/plex-orphaned-files \
      --token abc123 \
      --base-url https://radarr.example.com
```

See [command-line usage](#command-line) for how to run the binary.

If you specify the `--cron` option with a valid cron specifier, the tool will not exit and perform automatic checks in accordance with the schedule.
For help creating a valid cron specifier, visit [cron.help](https://cron.help/#0_*_*_*_*).

To be notified when sync is failing visit https://healthchecks.io, create a check, and specify the ID to the container using the `--hc-id` option.
You can also specify a custom host with `--hc-host`.

If you're using Docker Compose, all the options are available as environment variables.

```yaml
services:
  gitout:
    image: jakewharton/plex-orphaned-files:latest
    restart: unless-stopped
    environment:
      - "PLEX_ORPHANED_TOKEN=abc123xyz"
      - "PLEX_ORPHANED_BASE_URL=http://plexms:32400"
      - "PLEX_ORPHANED_CRON=0 * * * *"
      #Optional:
      - "PLEX_ORPHANED_HC_ID=..."
      - "PLEX_ORPHANED_HC_HOST=..."
```

Note: You may want to specify an explicit version rather than `latest`.
See https://hub.docker.com/r/jakewharton/plex-orphaned-files/tags or `CHANGELOG.md` for the available versions.
Use `trunk` for the latest changes.

### Command-Line

```
Usage: plex-orphaned-files [OPTIONS] [LIBRARY]...

  Find files in your Plex libraries which are not indexed by Plex.

Options:
  --base-url URL            Base URL of Plex server web interface (e.g.,
                            http://plex:32400/)
  --token TOKEN             Plex authentication token. See:
                            https://support.plex.tv/articles/204059436-finding-an-authentication-token-x-plex-token/
  --folder-mapping MAPPING  Map a Plex folder path to filesystem path (e.g.,
                            /media:/tank/media)
  --exclude-files GLOB      Glob pattern of files to ignore (e.g.,
                            /media/**/*.nfo, /music/**/cover.*)
  --exclude-library NAME    Name of libraries to exclude. Mutually exclusive
                            with LIBRARY arguments.
  -h, --help                Show this message and exit

Arguments:
  LIBRARY  Name of libraries to scan. All libraries will be scanned if none
           specified. Mutually exclusive with --exclude-library
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
