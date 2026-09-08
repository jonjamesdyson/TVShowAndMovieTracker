package com.example.data.backup

import com.example.data.db.EpisodeEntity
import com.example.data.db.ListStatus
import com.example.data.db.ShowEntity
import com.example.data.db.ShowWithProgress
import com.example.data.repository.OverallStatistics
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class BackupData(
    val shows: List<ShowEntity>,
    val episodes: List<EpisodeEntity>
)

object TvBackupManager {

    private val dateFormatter = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

    /**
     * Formats all user shows and ratings as a clean, readable text note.
     */
    fun generateExportNote(
        shows: List<ShowWithProgress>,
        stats: OverallStatistics
    ): String {
        val dateString = dateFormatter.format(Date())
        val sb = StringBuilder()
        sb.append("=========================================\n")
        sb.append("📺 MY SERIES WATCHLIST & RATINGS\n")
        sb.append("Exported: $dateString\n")
        sb.append("=========================================\n\n")

        if (shows.isEmpty()) {
            sb.append("No shows in watchlist yet.\n\n")
        } else {
            // Group shows by status
            val grouped = shows.groupBy { it.listStatus }
            val statusOrder = listOf(
                ListStatus.WATCHING,
                ListStatus.COMPLETED,
                ListStatus.PLAN_TO_WATCH,
                ListStatus.DROPPED
            )

            for (status in statusOrder) {
                val statusShows = grouped[status] ?: continue
                if (statusShows.isNotEmpty()) {
                    sb.append("─── $status (${statusShows.size}) ───\n")
                    statusShows.forEachIndexed { index, show ->
                        val ratingText = if (show.userRating > 0) {
                            "★ ${show.userRating}/10"
                        } else {
                            "Unrated"
                        }
                        sb.append("${index + 1}. ${show.name}\n")
                        sb.append("   • User Rating: $ratingText\n")
                        sb.append("   • Progress: ${show.watchedEpisodes}/${show.totalEpisodes} episodes (${show.progressPercent}%)\n")
                        if (!show.customNotes.isNullOrBlank()) {
                            sb.append("   • Notes: ${show.customNotes.trim()}\n")
                        }
                        if (show.genres.isNotBlank()) {
                            sb.append("   • Genres: ${show.genres}\n")
                        }
                    }
                    sb.append("\n")
                }
            }
        }

        sb.append("=========================================\n")
        sb.append("📊 OVERALL SUMMARY\n")
        sb.append("• Total Series Tracked: ${stats.totalShowsTracked}\n")
        sb.append("• Total Episodes Watched: ${stats.totalEpisodesWatched}\n")
        sb.append("• Total Watch Time: ${stats.formattedWatchTime}\n")
        sb.append("• Completed Series: ${stats.completedShowsCount}\n")
        sb.append("• Currently Watching: ${stats.watchingShowsCount}\n")
        sb.append("Exported from SeriesTracker\n")
        sb.append("=========================================")

        return sb.toString()
    }

    /**
     * Exports all tracked shows and episodes to a valid JSON string.
     */
    fun exportToJson(
        shows: List<ShowEntity>,
        episodes: List<EpisodeEntity>
    ): String {
        val root = JSONObject()
        root.put("version", 1)
        root.put("appName", "SeriesTracker")
        root.put("exportedAt", System.currentTimeMillis())
        root.put("exportedDate", dateFormatter.format(Date()))
        root.put("totalShows", shows.size)
        root.put("totalEpisodes", episodes.size)

        val showsArray = JSONArray()
        for (show in shows) {
            val showObj = JSONObject()
            showObj.put("id", show.id)
            showObj.put("name", show.name)
            showObj.put("summary", show.summary ?: JSONObject.NULL)
            showObj.put("imageMedium", show.imageMedium ?: JSONObject.NULL)
            showObj.put("imageOriginal", show.imageOriginal ?: JSONObject.NULL)
            showObj.put("status", show.status ?: JSONObject.NULL)
            showObj.put("premiered", show.premiered ?: JSONObject.NULL)
            showObj.put("ended", show.ended ?: JSONObject.NULL)
            showObj.put("genres", show.genres)
            if (show.ratingAverage != null) {
                showObj.put("ratingAverage", show.ratingAverage)
            }
            if (show.averageRuntime != null) {
                showObj.put("averageRuntime", show.averageRuntime)
            }
            showObj.put("networkName", show.networkName ?: JSONObject.NULL)
            showObj.put("listStatus", show.listStatus)
            showObj.put("userRating", show.userRating)
            showObj.put("customNotes", show.customNotes ?: JSONObject.NULL)
            showObj.put("addedTimestamp", show.addedTimestamp)
            showObj.put("lastUpdatedTimestamp", show.lastUpdatedTimestamp)
            showsArray.put(showObj)
        }
        root.put("shows", showsArray)

        val episodesArray = JSONArray()
        for (ep in episodes) {
            val epObj = JSONObject()
            epObj.put("id", ep.id)
            epObj.put("showId", ep.showId)
            epObj.put("name", ep.name ?: JSONObject.NULL)
            epObj.put("season", ep.season)
            epObj.put("number", ep.number)
            epObj.put("airdate", ep.airdate ?: JSONObject.NULL)
            if (ep.runtime != null) {
                epObj.put("runtime", ep.runtime)
            }
            epObj.put("summary", ep.summary ?: JSONObject.NULL)
            epObj.put("imageMedium", ep.imageMedium ?: JSONObject.NULL)
            epObj.put("isWatched", ep.isWatched)
            if (ep.watchedTimestamp != null) {
                epObj.put("watchedTimestamp", ep.watchedTimestamp)
            }
            episodesArray.put(epObj)
        }
        root.put("episodes", episodesArray)

        return root.toString(2)
    }

    /**
     * Parses and validates a JSON string created by SeriesTracker.
     * Throws an exception if the format is invalid or has no shows.
     */
    fun parseBackupJson(jsonString: String): BackupData {
        val root = JSONObject(jsonString.trim())

        if (!root.has("shows")) {
            throw IllegalArgumentException("Invalid backup format: missing 'shows' section.")
        }

        val showsArray = root.getJSONArray("shows")
        val parsedShows = mutableListOf<ShowEntity>()

        for (i in 0 until showsArray.length()) {
            val obj = showsArray.getJSONObject(i)
            val show = ShowEntity(
                id = obj.getLong("id"),
                name = obj.getString("name"),
                summary = obj.optString("summary").takeIf { it.isNotBlank() && it != "null" },
                imageMedium = obj.optString("imageMedium").takeIf { it.isNotBlank() && it != "null" },
                imageOriginal = obj.optString("imageOriginal").takeIf { it.isNotBlank() && it != "null" },
                status = obj.optString("status").takeIf { it.isNotBlank() && it != "null" },
                premiered = obj.optString("premiered").takeIf { it.isNotBlank() && it != "null" },
                ended = obj.optString("ended").takeIf { it.isNotBlank() && it != "null" },
                genres = obj.optString("genres", ""),
                ratingAverage = if (obj.has("ratingAverage") && !obj.isNull("ratingAverage")) obj.getDouble("ratingAverage") else null,
                averageRuntime = if (obj.has("averageRuntime") && !obj.isNull("averageRuntime")) obj.getInt("averageRuntime") else null,
                networkName = obj.optString("networkName").takeIf { it.isNotBlank() && it != "null" },
                listStatus = obj.optString("listStatus", ListStatus.WATCHING),
                userRating = obj.optInt("userRating", 0),
                customNotes = obj.optString("customNotes").takeIf { it.isNotBlank() && it != "null" },
                addedTimestamp = obj.optLong("addedTimestamp", System.currentTimeMillis()),
                lastUpdatedTimestamp = obj.optLong("lastUpdatedTimestamp", System.currentTimeMillis())
            )
            parsedShows.add(show)
        }

        val parsedEpisodes = mutableListOf<EpisodeEntity>()
        if (root.has("episodes")) {
            val episodesArray = root.getJSONArray("episodes")
            for (i in 0 until episodesArray.length()) {
                val obj = episodesArray.getJSONObject(i)
                val ep = EpisodeEntity(
                    id = obj.getLong("id"),
                    showId = obj.getLong("showId"),
                    name = obj.optString("name").takeIf { it.isNotBlank() && it != "null" },
                    season = obj.optInt("season", 1),
                    number = obj.optInt("number", 1),
                    airdate = obj.optString("airdate").takeIf { it.isNotBlank() && it != "null" },
                    runtime = if (obj.has("runtime") && !obj.isNull("runtime")) obj.getInt("runtime") else null,
                    summary = obj.optString("summary").takeIf { it.isNotBlank() && it != "null" },
                    imageMedium = obj.optString("imageMedium").takeIf { it.isNotBlank() && it != "null" },
                    isWatched = obj.optBoolean("isWatched", false),
                    watchedTimestamp = if (obj.has("watchedTimestamp") && !obj.isNull("watchedTimestamp")) obj.getLong("watchedTimestamp") else null
                )
                parsedEpisodes.add(ep)
            }
        }

        return BackupData(shows = parsedShows, episodes = parsedEpisodes)
    }
}
