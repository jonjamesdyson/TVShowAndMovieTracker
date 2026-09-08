package com.example.data.api

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class SearchShowItem(
    @Json(name = "score") val score: Double? = null,
    @Json(name = "show") val show: ShowDto
)

@JsonClass(generateAdapter = true)
data class ShowDto(
    @Json(name = "id") val id: Long,
    @Json(name = "url") val url: String? = null,
    @Json(name = "name") val name: String,
    @Json(name = "type") val type: String? = null,
    @Json(name = "language") val language: String? = null,
    @Json(name = "genres") val genres: List<String>? = null,
    @Json(name = "status") val status: String? = null,
    @Json(name = "runtime") val runtime: Int? = null,
    @Json(name = "averageRuntime") val averageRuntime: Int? = null,
    @Json(name = "premiered") val premiered: String? = null,
    @Json(name = "ended") val ended: String? = null,
    @Json(name = "officialSite") val officialSite: String? = null,
    @Json(name = "rating") val rating: RatingDto? = null,
    @Json(name = "network") val network: NetworkDto? = null,
    @Json(name = "webChannel") val webChannel: NetworkDto? = null,
    @Json(name = "image") val image: ImageDto? = null,
    @Json(name = "summary") val summary: String? = null
)

@JsonClass(generateAdapter = true)
data class EpisodeDto(
    @Json(name = "id") val id: Long,
    @Json(name = "url") val url: String? = null,
    @Json(name = "name") val name: String? = null,
    @Json(name = "season") val season: Int = 1,
    @Json(name = "number") val number: Int? = null,
    @Json(name = "type") val type: String? = null,
    @Json(name = "airdate") val airdate: String? = null,
    @Json(name = "airtime") val airtime: String? = null,
    @Json(name = "runtime") val runtime: Int? = null,
    @Json(name = "rating") val rating: RatingDto? = null,
    @Json(name = "image") val image: ImageDto? = null,
    @Json(name = "summary") val summary: String? = null
)

@JsonClass(generateAdapter = true)
data class RatingDto(
    @Json(name = "average") val average: Double? = null
)

@JsonClass(generateAdapter = true)
data class ImageDto(
    @Json(name = "medium") val medium: String? = null,
    @Json(name = "original") val original: String? = null
)

@JsonClass(generateAdapter = true)
data class NetworkDto(
    @Json(name = "id") val id: Long? = null,
    @Json(name = "name") val name: String? = null
)
