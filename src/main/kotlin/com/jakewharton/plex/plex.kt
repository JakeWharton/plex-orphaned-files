package com.jakewharton.plex

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PlexResponse<T>(
	@SerialName("MediaContainer")
	val mediaContainer: T,
)

@Serializable
data class PlexSections(
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
data class PlexMetadataList(
	@SerialName("Metadata")
	val metadata: List<PlexMetadata>,
)

@Serializable
data class PlexMetadata(
	val key: String,
	@SerialName("Media")
	val media: List<PlexMedia>? = null,
)

@Serializable
data class PlexMedia(
	@SerialName("Part")
	val parts: List<Part>,
) {
	@Serializable
	data class Part(
		val file: String,
	)
}
