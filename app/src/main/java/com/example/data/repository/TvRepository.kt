package com.example.data.repository

import com.example.data.api.ApiClient
import com.example.data.api.EpisodeDto
import com.example.data.api.ShowDto
import com.example.data.api.TvmazeApi
import com.example.data.db.EpisodeEntity
import com.example.data.db.ListStatus
import com.example.data.db.NextUpShowEpisode
import com.example.data.db.ShowEntity
import com.example.data.db.ShowWithProgress
import com.example.data.db.TvDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

data class OverallStatistics(
    val totalWatchTimeMinutes: Long = 0,
    val totalEpisodesWatched: Int = 0,
    val totalShowsTracked: Int = 0,
    val completedShowsCount: Int = 0,
    val watchingShowsCount: Int = 0,
    val planToWatchShowsCount: Int = 0,
    val droppedShowsCount: Int = 0
) {
    val days: Long get() = totalWatchTimeMinutes / (24 * 60)
    val hours: Long get() = (totalWatchTimeMinutes % (24 * 60)) / 60
    val minutes: Long get() = totalWatchTimeMinutes % 60

    val formattedWatchTime: String
        get() {
            val parts = mutableListOf<String>()
            if (days > 0) parts.add("${days}d")
            if (hours > 0 || days > 0) parts.add("${hours}h")
            parts.add("${minutes}m")
            return parts.joinToString(" ")
        }
}

class TvRepository(
    private val tvDao: TvDao,
    private val api: TvmazeApi = ApiClient.tvmazeApi
) {

    fun getShowsWithProgress(status: String? = null): Flow<List<ShowWithProgress>> {
        return if (status == null || status == "All") {
            tvDao.getAllShowsWithProgress()
        } else {
            tvDao.getShowsWithProgressByStatus(status)
        }
    }

    fun getShowWithProgress(showId: Long): Flow<ShowWithProgress?> {
        return tvDao.getShowWithProgressById(showId)
    }

    fun getShowEntity(showId: Long): Flow<ShowEntity?> {
        return tvDao.getShowById(showId)
    }

    fun getEpisodes(showId: Long): Flow<List<EpisodeEntity>> {
        return tvDao.getEpisodesForShow(showId)
    }

    fun getNextUpEpisodes(): Flow<List<NextUpShowEpisode>> {
        return combine(
            tvDao.getUnwatchedEpisodesForWatchingShows(),
            tvDao.getShowsWithProgressByStatus(ListStatus.WATCHING)
        ) { unwatchedEpisodes, watchingShows ->
            val showMap = watchingShows.associateBy { it.id }
            // Group unwatched episodes by showId and take the first one (lowest season, number)
            unwatchedEpisodes
                .groupBy { it.showId }
                .mapNotNull { (showId, episodes) ->
                    val show = showMap[showId] ?: return@mapNotNull null
                    val nextEpisode = episodes.firstOrNull() ?: return@mapNotNull null
                    NextUpShowEpisode(
                        episodeId = nextEpisode.id,
                        showId = showId,
                        showName = show.name,
                        showImageMedium = nextEpisode.imageMedium ?: show.imageMedium,
                        episodeName = nextEpisode.name,
                        season = nextEpisode.season,
                        number = nextEpisode.number,
                        runtime = nextEpisode.runtime ?: show.averageRuntime,
                        airdate = nextEpisode.airdate,
                        summary = nextEpisode.summary
                    )
                }
        }.flowOn(Dispatchers.Default)
    }

    fun getStatistics(): Flow<OverallStatistics> {
        return combine(
            tvDao.getTotalEpisodesWatchedCount(),
            tvDao.getTotalShowsCount(),
            tvDao.getStatusCounts(),
            tvDao.getWatchedEpisodeRuntimes()
        ) { totalEpisodes, totalShows, statusCounts, runtimes ->
            var totalMinutes = 0L
            for (item in runtimes) {
                val mins = item.runtime ?: item.averageRuntime ?: 40
                totalMinutes += mins
            }

            var watching = 0
            var completed = 0
            var planToWatch = 0
            var dropped = 0

            for (status in statusCounts) {
                when (status.listStatus) {
                    ListStatus.WATCHING -> watching = status.count
                    ListStatus.COMPLETED -> completed = status.count
                    ListStatus.PLAN_TO_WATCH -> planToWatch = status.count
                    ListStatus.DROPPED -> dropped = status.count
                }
            }

            OverallStatistics(
                totalWatchTimeMinutes = totalMinutes,
                totalEpisodesWatched = totalEpisodes,
                totalShowsTracked = totalShows,
                completedShowsCount = completed,
                watchingShowsCount = watching,
                planToWatchShowsCount = planToWatch,
                droppedShowsCount = dropped
            )
        }.flowOn(Dispatchers.Default)
    }

    suspend fun searchShows(query: String): Result<List<ShowDto>> = withContext(Dispatchers.IO) {
        try {
            if (query.isBlank()) return@withContext Result.success(emptyList())
            val results = api.searchShows(query)
            Result.success(results.map { it.show })
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun fetchShowDetail(showId: Long): Result<ShowDto> = withContext(Dispatchers.IO) {
        try {
            val show = api.getShowDetail(showId)
            Result.success(show)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun fetchShowEpisodes(showId: Long): Result<List<EpisodeDto>> = withContext(Dispatchers.IO) {
        try {
            val episodes = api.getShowEpisodes(showId)
            Result.success(episodes)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun addOrTrackShow(
        showDto: ShowDto,
        initialStatus: String = ListStatus.WATCHING,
        episodesDto: List<EpisodeDto>? = null
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val existing = tvDao.getShowByIdDirect(showDto.id)
            val showEntity = ShowEntity(
                id = showDto.id,
                name = showDto.name,
                summary = showDto.summary,
                imageMedium = showDto.image?.medium,
                imageOriginal = showDto.image?.original,
                status = showDto.status,
                premiered = showDto.premiered,
                ended = showDto.ended,
                genres = showDto.genres?.joinToString(", ") ?: "",
                ratingAverage = showDto.rating?.average,
                averageRuntime = showDto.averageRuntime ?: showDto.runtime,
                networkName = showDto.network?.name ?: showDto.webChannel?.name,
                listStatus = existing?.listStatus ?: initialStatus,
                userRating = existing?.userRating ?: 0,
                customNotes = existing?.customNotes,
                addedTimestamp = existing?.addedTimestamp ?: System.currentTimeMillis(),
                lastUpdatedTimestamp = System.currentTimeMillis()
            )
            tvDao.insertShow(showEntity)

            // Episodes
            val episodesToSave = episodesDto ?: try {
                api.getShowEpisodes(showDto.id)
            } catch (_: Exception) {
                emptyList()
            }

            if (episodesToSave.isNotEmpty()) {
                val existingEpisodes = tvDao.getEpisodesForShowDirect(showDto.id).associateBy { it.id }
                val entities = episodesToSave.map { dto ->
                    val existingEp = existingEpisodes[dto.id]
                    EpisodeEntity(
                        id = dto.id,
                        showId = showDto.id,
                        name = dto.name ?: "Episode ${dto.number ?: ""}",
                        season = dto.season,
                        number = dto.number ?: 0,
                        airdate = dto.airdate,
                        runtime = dto.runtime ?: showDto.averageRuntime,
                        summary = dto.summary,
                        imageMedium = dto.image?.medium,
                        isWatched = existingEp?.isWatched ?: false,
                        watchedTimestamp = existingEp?.watchedTimestamp
                    )
                }
                tvDao.insertEpisodes(entities)
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun toggleEpisodeWatched(episodeId: Long, currentWatched: Boolean) = withContext(Dispatchers.IO) {
        val nextWatched = !currentWatched
        val timestamp = if (nextWatched) System.currentTimeMillis() else null
        tvDao.setEpisodeWatched(episodeId, nextWatched, timestamp)
    }

    suspend fun markSeasonWatched(showId: Long, season: Int, isWatched: Boolean) = withContext(Dispatchers.IO) {
        val timestamp = if (isWatched) System.currentTimeMillis() else null
        tvDao.setSeasonWatched(showId, season, isWatched, timestamp)
    }

    suspend fun markPreviousEpisodesWatched(showId: Long, upToSeason: Int, upToNumber: Int) = withContext(Dispatchers.IO) {
        val timestamp = System.currentTimeMillis()
        tvDao.setPreviousEpisodesWatched(showId, upToSeason, upToNumber, timestamp)
    }

    suspend fun markAllEpisodesWatched(showId: Long, isWatched: Boolean) = withContext(Dispatchers.IO) {
        val timestamp = if (isWatched) System.currentTimeMillis() else null
        tvDao.setAllEpisodesWatched(showId, isWatched, timestamp)
        if (isWatched) {
            tvDao.updateShowListStatus(showId, ListStatus.COMPLETED)
        }
    }

    suspend fun updateShowStatus(showId: Long, status: String) = withContext(Dispatchers.IO) {
        tvDao.updateShowListStatus(showId, status)
    }

    suspend fun updateUserRating(showId: Long, rating: Int) = withContext(Dispatchers.IO) {
        tvDao.updateShowUserRating(showId, rating)
    }

    suspend fun removeShow(showId: Long) = withContext(Dispatchers.IO) {
        tvDao.deleteShow(showId)
    }

    suspend fun getAllShowsDirect(): List<ShowEntity> = withContext(Dispatchers.IO) {
        tvDao.getAllShowsDirect()
    }

    suspend fun getAllEpisodesDirect(): List<EpisodeEntity> = withContext(Dispatchers.IO) {
        tvDao.getAllEpisodesDirect()
    }

    suspend fun restoreBackup(shows: List<ShowEntity>, episodes: List<EpisodeEntity>) = withContext(Dispatchers.IO) {
        // Insert shows first to satisfy foreign key constraints
        tvDao.insertShows(shows)
        if (episodes.isNotEmpty()) {
            tvDao.insertEpisodes(episodes)
        }
    }

    suspend fun addCustomShow(
        name: String,
        summary: String? = null,
        imageUrl: String? = null,
        genres: String = "",
        seasonsCount: Int = 1,
        episodesPerSeason: Int = 10,
        averageRuntime: Int = 45,
        networkName: String? = null,
        listStatus: String = ListStatus.WATCHING,
        premieredYear: String? = null
    ): Result<Long> = withContext(Dispatchers.IO) {
        try {
            // Negative show ID ensures it never conflicts with TVmaze positive IDs
            val showId = -System.currentTimeMillis()
            val showEntity = ShowEntity(
                id = showId,
                name = name.trim(),
                summary = summary?.trim()?.ifBlank { null },
                imageMedium = imageUrl?.trim()?.ifBlank { null },
                imageOriginal = imageUrl?.trim()?.ifBlank { null },
                status = "Running",
                premiered = if (!premieredYear.isNullOrBlank()) "$premieredYear-01-01" else null,
                ended = null,
                genres = genres.trim(),
                ratingAverage = null,
                averageRuntime = averageRuntime,
                networkName = networkName?.trim()?.ifBlank { "Custom" },
                listStatus = listStatus,
                userRating = 0,
                customNotes = null,
                addedTimestamp = System.currentTimeMillis(),
                lastUpdatedTimestamp = System.currentTimeMillis()
            )
            tvDao.insertShow(showEntity)

            val safeSeasons = seasonsCount.coerceIn(1, 50)
            val safeEpisodes = episodesPerSeason.coerceIn(1, 100)
            val episodes = mutableListOf<EpisodeEntity>()
            for (s in 1..safeSeasons) {
                for (ep in 1..safeEpisodes) {
                    val episodeId = showId * 1000L - (s * 100L + ep)
                    episodes.add(
                        EpisodeEntity(
                            id = episodeId,
                            showId = showId,
                            name = "Episode $ep",
                            season = s,
                            number = ep,
                            airdate = null,
                            runtime = averageRuntime,
                            summary = null,
                            imageMedium = null,
                            isWatched = false,
                            watchedTimestamp = null
                        )
                    )
                }
            }
            if (episodes.isNotEmpty()) {
                tvDao.insertEpisodes(episodes)
            }
            Result.success(showId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun addCustomEpisode(
        showId: Long,
        season: Int,
        name: String? = null,
        runtime: Int? = null
    ): Result<EpisodeEntity> = withContext(Dispatchers.IO) {
        try {
            val existing = tvDao.getEpisodesForShowDirect(showId)
            val seasonEpisodes = existing.filter { it.season == season }
            val nextNumber = (seasonEpisodes.maxOfOrNull { it.number } ?: 0) + 1
            val episodeId = if (showId < 0) {
                showId * 1000L - (season * 100L + nextNumber) - (System.currentTimeMillis() % 99L)
            } else {
                System.currentTimeMillis()
            }
            val ep = EpisodeEntity(
                id = episodeId,
                showId = showId,
                name = name?.trim()?.ifBlank { null } ?: "Episode $nextNumber",
                season = season,
                number = nextNumber,
                airdate = null,
                runtime = runtime ?: 45,
                summary = null,
                imageMedium = null,
                isWatched = false,
                watchedTimestamp = null
            )
            tvDao.insertEpisodes(listOf(ep))
            Result.success(ep)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
