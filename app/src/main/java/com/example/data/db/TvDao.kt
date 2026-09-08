package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

data class ShowWithProgress(
    val id: Long,
    val name: String,
    val summary: String?,
    val imageMedium: String?,
    val imageOriginal: String?,
    val status: String?,
    val premiered: String?,
    val ended: String?,
    val genres: String,
    val ratingAverage: Double?,
    val averageRuntime: Int?,
    val networkName: String?,
    val listStatus: String,
    val userRating: Int,
    val customNotes: String?,
    val addedTimestamp: Long,
    val lastUpdatedTimestamp: Long,
    val totalEpisodes: Int,
    val watchedEpisodes: Int
) {
    val progressFraction: Float
        get() = if (totalEpisodes > 0) watchedEpisodes.toFloat() / totalEpisodes else 0f

    val progressPercent: Int
        get() = (progressFraction * 100).toInt()

    val isAllWatched: Boolean
        get() = totalEpisodes > 0 && watchedEpisodes >= totalEpisodes
}

data class NextUpShowEpisode(
    val episodeId: Long,
    val showId: Long,
    val showName: String,
    val showImageMedium: String?,
    val episodeName: String?,
    val season: Int,
    val number: Int,
    val runtime: Int?,
    val airdate: String?,
    val summary: String?
)

data class StatusCount(
    val listStatus: String,
    val count: Int
)

data class WatchedRuntimeInfo(
    val runtime: Int?,
    val averageRuntime: Int?
)

@Dao
interface TvDao {

    @Query("""
        SELECT s.*,
               COUNT(e.id) as totalEpisodes,
               SUM(CASE WHEN e.isWatched = 1 THEN 1 ELSE 0 END) as watchedEpisodes
        FROM shows s
        LEFT JOIN episodes e ON s.id = e.showId
        GROUP BY s.id
        ORDER BY s.lastUpdatedTimestamp DESC
    """)
    fun getAllShowsWithProgress(): Flow<List<ShowWithProgress>>

    @Query("""
        SELECT s.*,
               COUNT(e.id) as totalEpisodes,
               SUM(CASE WHEN e.isWatched = 1 THEN 1 ELSE 0 END) as watchedEpisodes
        FROM shows s
        LEFT JOIN episodes e ON s.id = e.showId
        WHERE s.listStatus = :status
        GROUP BY s.id
        ORDER BY s.lastUpdatedTimestamp DESC
    """)
    fun getShowsWithProgressByStatus(status: String): Flow<List<ShowWithProgress>>

    @Query("""
        SELECT s.*,
               COUNT(e.id) as totalEpisodes,
               SUM(CASE WHEN e.isWatched = 1 THEN 1 ELSE 0 END) as watchedEpisodes
        FROM shows s
        LEFT JOIN episodes e ON s.id = e.showId
        WHERE s.id = :showId
        GROUP BY s.id
    """)
    fun getShowWithProgressById(showId: Long): Flow<ShowWithProgress?>

    @Query("SELECT * FROM shows WHERE id = :showId")
    fun getShowById(showId: Long): Flow<ShowEntity?>

    @Query("SELECT * FROM shows WHERE id = :showId")
    suspend fun getShowByIdDirect(showId: Long): ShowEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShow(show: ShowEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShows(shows: List<ShowEntity>)

    @Query("SELECT * FROM shows ORDER BY name ASC")
    suspend fun getAllShowsDirect(): List<ShowEntity>

    @Query("SELECT * FROM episodes ORDER BY showId ASC, season ASC, number ASC")
    suspend fun getAllEpisodesDirect(): List<EpisodeEntity>

    @Update
    suspend fun updateShow(show: ShowEntity)

    @Query("DELETE FROM shows WHERE id = :showId")
    suspend fun deleteShow(showId: Long)

    @Query("SELECT * FROM episodes WHERE showId = :showId ORDER BY season ASC, number ASC")
    fun getEpisodesForShow(showId: Long): Flow<List<EpisodeEntity>>

    @Query("SELECT * FROM episodes WHERE showId = :showId ORDER BY season ASC, number ASC")
    suspend fun getEpisodesForShowDirect(showId: Long): List<EpisodeEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEpisodes(episodes: List<EpisodeEntity>)

    @Query("""
        UPDATE episodes 
        SET isWatched = :isWatched, 
            watchedTimestamp = :timestamp 
        WHERE id = :episodeId
    """)
    suspend fun setEpisodeWatched(episodeId: Long, isWatched: Boolean, timestamp: Long?)

    @Query("""
        UPDATE episodes 
        SET isWatched = :isWatched, 
            watchedTimestamp = :timestamp 
        WHERE showId = :showId AND season = :season
    """)
    suspend fun setSeasonWatched(showId: Long, season: Int, isWatched: Boolean, timestamp: Long?)

    @Query("""
        UPDATE episodes 
        SET isWatched = 1, 
            watchedTimestamp = :timestamp 
        WHERE showId = :showId 
          AND (season < :upToSeason OR (season = :upToSeason AND number <= :upToNumber))
    """)
    suspend fun setPreviousEpisodesWatched(
        showId: Long,
        upToSeason: Int,
        upToNumber: Int,
        timestamp: Long?
    )

    @Query("""
        UPDATE episodes 
        SET isWatched = :isWatched, 
            watchedTimestamp = :timestamp 
        WHERE showId = :showId
    """)
    suspend fun setAllEpisodesWatched(showId: Long, isWatched: Boolean, timestamp: Long?)

    @Query("UPDATE shows SET listStatus = :status, lastUpdatedTimestamp = :timestamp WHERE id = :showId")
    suspend fun updateShowListStatus(showId: Long, status: String, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE shows SET userRating = :rating, lastUpdatedTimestamp = :timestamp WHERE id = :showId")
    suspend fun updateShowUserRating(showId: Long, rating: Int, timestamp: Long = System.currentTimeMillis())

    @Query("""
        SELECT e.* 
        FROM episodes e
        INNER JOIN shows s ON e.showId = s.id
        WHERE s.listStatus = 'Watching' AND e.isWatched = 0
        ORDER BY e.showId, e.season ASC, e.number ASC
    """)
    fun getUnwatchedEpisodesForWatchingShows(): Flow<List<EpisodeEntity>>

    @Query("SELECT COUNT(*) FROM episodes WHERE isWatched = 1")
    fun getTotalEpisodesWatchedCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM shows")
    fun getTotalShowsCount(): Flow<Int>

    @Query("SELECT listStatus, COUNT(*) as count FROM shows GROUP BY listStatus")
    fun getStatusCounts(): Flow<List<StatusCount>>

    @Query("""
        SELECT e.runtime, s.averageRuntime 
        FROM episodes e 
        INNER JOIN shows s ON e.showId = s.id 
        WHERE e.isWatched = 1
    """)
    fun getWatchedEpisodeRuntimes(): Flow<List<WatchedRuntimeInfo>>
}
