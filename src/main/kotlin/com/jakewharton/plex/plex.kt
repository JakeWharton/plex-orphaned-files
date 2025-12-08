package com.jakewharton.plex

import com.jakewharton.plex.PlexSections.SectionHeader.Location
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request

interface PlexApi {
	suspend fun sections(): List<PlexSection>
	suspend fun sectionPaths(sectionKey: String): List<String>
}

data class PlexSection(
	val key: String,
	val title: String,
	val locations: List<String>,
)

class HttpPlexApi(
	private val client: OkHttpClient,
	private val baseUrl: HttpUrl,
	private val token: String,
) : PlexApi {
	private val json = Json {
		ignoreUnknownKeys = true
	}

	override suspend fun sections(): List<PlexSection> {
		val sectionsUrl = baseUrl.newBuilder()
			.addPathSegment("library")
			.addPathSegment("sections")
			.addQueryParameter("X-Plex-Token", token)
			.build()
		val sectionsRequest = Request.Builder()
			.url(sectionsUrl)
			.header("Accept", "application/json")
			.build()
		val sectionsJson = client.newCall(sectionsRequest).awaitString()
		val sectionsResponse = json.decodeFromString(PlexResponse.serializer(PlexSections.serializer()), sectionsJson)
		return sectionsResponse.mediaContainer.sections.map {
			PlexSection(it.key, it.title, it.locations.map(Location::path))
		}
	}

	override suspend fun sectionPaths(sectionKey: String): List<String> {
		val sectionUrl = baseUrl.newBuilder()
			.addPathSegment("library")
			.addPathSegment("sections")
			.addPathSegment(sectionKey)
			.addPathSegment("all")
			.addQueryParameter("X-Plex-Token", token)
			.build()
		val sectionRequest = Request.Builder()
			.url(sectionUrl)
			.header("Accept", "application/json")
			.build()
		val sectionJson = client.newCall(sectionRequest).awaitString()
		val sectionResponse = json.decodeFromString(PlexResponse.serializer(PlexMetadataList.serializer()), sectionJson)

		return sectionResponse.mediaContainer.metadata
			.flatMap { mediaPaths(it) + extraPaths(it) }
	}

	private suspend fun mediaPaths(metadata: PlexMetadata): List<String> {
		if (metadata.media != null) {
			return metadata.media
				.flatMap { it.parts }
				.map { it.file }
		}

		val metadataUrl = baseUrl.newBuilder()
			.addPathSegment("library")
			.addPathSegment("metadata")
			.addPathSegment(metadata.key)
			.addPathSegment("allLeaves")
			.addQueryParameter("X-Plex-Token", token)
			.build()
		val metadataRequest = Request.Builder()
			.url(metadataUrl)
			.header("Accept", "application/json")
			.build()
		val metadataJson = client.newCall(metadataRequest).awaitString()
		val metadataList = json.decodeFromString(PlexResponse.serializer(PlexMetadataList.serializer()), metadataJson)

		return metadataList.mediaContainer.metadata.flatMap { mediaPaths(it) }
	}

	private suspend fun extraPaths(metadata: PlexMetadata): List<String> {
		val extrasUrl = baseUrl.newBuilder()
			.addPathSegment("library")
			.addPathSegment("metadata")
			.addPathSegment(metadata.key)
			.addPathSegment("extras")
			.addQueryParameter("X-Plex-Token", token)
			.build()
		val extrasRequest = Request.Builder()
			.url(extrasUrl)
			.header("Accept", "application/json")
			.build()

		val extrasCall = client.newCall(extrasRequest)

		// TODO Would be nice to determine ahead of time if we need to make this call or not as opposed to letting it 404.
		val extrasJson = try {
			extrasCall.awaitString()
		} catch (e: HttpException) {
			if (e.code == 404) {
				return emptyList()
			}
			throw e
		}
		val extrasList = json.decodeFromString(PlexResponse.serializer(PlexMetadataList.serializer()), extrasJson)

		return extrasList.mediaContainer.metadata.flatMap { mediaPaths(it) }
	}
}

@Serializable
private data class PlexResponse<T>(
	@SerialName("MediaContainer")
	val mediaContainer: T,
)

@Serializable
private data class PlexSections(
	@SerialName("Directory")
	val sections: List<SectionHeader>,
) {
	@Serializable
	data class SectionHeader(
		val key: String,
		val title: String,
		@SerialName("Location")
		val locations: List<Location>,
	) {
		@Serializable
		data class Location(
			val path: String,
		)
	}
}

@Serializable
private data class PlexMetadataList(
	@SerialName("Metadata")
	val metadata: List<PlexMetadata> = emptyList(), // Will be absent from JSON if no content.
)

@Serializable
private data class PlexMetadata(
	@SerialName("ratingKey")
	val key: String,
	@SerialName("Media")
	val media: List<PlexMedia>? = null,
)

@Serializable
private data class PlexMedia(
	@SerialName("Part")
	val parts: List<Part>,
) {
	@Serializable
	data class Part(
		val file: String,
	)
}
