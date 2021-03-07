FROM adoptopenjdk:8-jdk-hotspot AS build
ENV GRADLE_OPTS="-Dorg.gradle.daemon=false -Dkotlin.incremental=false"
WORKDIR /app

COPY gradlew settings.gradle ./
COPY gradle ./gradle
RUN ./gradlew --version

COPY build.gradle ./
COPY src ./src
RUN ./gradlew build


FROM alpine:3.13
LABEL maintainer="Jake Wharton <docker@jakewharton.com>"

RUN apk add --no-cache \
      curl \
      openjdk8-jre \
 && rm -rf /var/cache/* \
 && mkdir /var/cache/apk

WORKDIR /app
COPY --from=build /app/build/install/plex-orphaned-files ./

ENTRYPOINT ["/app/bin/plex-orphaned-files"]
CMD ["--help"]
