package com.example.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "shows")
data class ShowEntity(
    @PrimaryKey val id: Long,
    val name: String,
    val summary: String?,
    val imageMedium: String?,
    val imageOriginal: String?,
    val status: String?, // e.g., "Running", "Ended"
    val premiered: String?,
    val ended: String?,
    val genres: String, // comma-separated
    val ratingAverage: Double?,
    val averageRuntime: Int?,
    val networkName: String?,
    val listStatus: String = ListStatus.WATCHING,
    val userRating: Int = 0, // 0 to 10 (0 = unrated)
    val customNotes: String? = null,
    val addedTimestamp: Long = System.currentTimeMillis(),
    val lastUpdatedTimestamp: Long = System.currentTimeMillis()
)

object ListStatus {
    const val WATCHING = "Watching"
    const val COMPLETED = "Completed"
    const val PLAN_TO_WATCH = "Plan to Watch"
    const val DROPPED = "Dropped"

    val all = listOf(WATCHING, COMPLETED, PLAN_TO_WATCH, DROPPED)
}
