package com.example.data.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "episodes",
    foreignKeys = [
        ForeignKey(
            entity = ShowEntity::class,
            parentColumns = ["id"],
            childColumns = ["showId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("showId"),
        Index(value = ["showId", "season", "number"])
    ]
)
data class EpisodeEntity(
    @PrimaryKey val id: Long,
    val showId: Long,
    val name: String?,
    val season: Int,
    val number: Int,
    val airdate: String?,
    val runtime: Int?,
    val summary: String?,
    val imageMedium: String?,
    val isWatched: Boolean = false,
    val watchedTimestamp: Long? = null
)
