package com.example.data.api

import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface TvmazeApi {
    @GET("search/shows")
    suspend fun searchShows(@Query("q") query: String): List<SearchShowItem>

    @GET("shows/{id}/episodes")
    suspend fun getShowEpisodes(@Path("id") showId: Long): List<EpisodeDto>

    @GET("shows/{id}")
    suspend fun getShowDetail(@Path("id") showId: Long): ShowDto
}
