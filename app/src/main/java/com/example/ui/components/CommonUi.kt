package com.example.ui.components

import android.text.Html
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.db.EpisodeEntity
import com.example.data.db.ListStatus
import com.example.data.db.NextUpShowEpisode
import com.example.data.db.ShowWithProgress
import com.example.ui.theme.AmberRating
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.EmeraldWatched
import com.example.ui.theme.IndigoPrimary

fun cleanHtml(html: String?): String {
    if (html.isNullOrBlank()) return ""
    return Html.fromHtml(html, Html.FROM_HTML_MODE_LEGACY).toString().trim()
}

@Composable
fun StatusBadge(status: String, modifier: Modifier = Modifier) {
    val (bgColor, textColor) = when (status) {
        ListStatus.WATCHING -> MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
        ListStatus.COMPLETED -> EmeraldWatched.copy(alpha = 0.2f) to EmeraldWatched
        ListStatus.PLAN_TO_WATCH -> CyanAccent.copy(alpha = 0.2f) to CyanAccent
        ListStatus.DROPPED -> MaterialTheme.colorScheme.errorContainer to MaterialTheme.colorScheme.onErrorContainer
        else -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
    }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = bgColor
    ) {
        Text(
            text = status,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = textColor
        )
    }
}

@Composable
fun ShowProgressIndicator(
    watchedEpisodes: Int,
    totalEpisodes: Int,
    modifier: Modifier = Modifier
) {
    val progress = if (totalEpisodes > 0) watchedEpisodes.toFloat() / totalEpisodes else 0f
    val animatedProgress by animateFloatAsState(targetValue = progress, label = "progress")
    val isComplete = totalEpisodes > 0 && watchedEpisodes >= totalEpisodes
    val barColor by animateColorAsState(
        targetValue = if (isComplete) EmeraldWatched else IndigoPrimary,
        label = "barColor"
    )

    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "$watchedEpisodes / $totalEpisodes watched",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "${(progress * 100).toInt()}%",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = if (isComplete) EmeraldWatched else MaterialTheme.colorScheme.primary
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        LinearProgressIndicator(
            progress = { animatedProgress },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = barColor,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
            strokeCap = StrokeCap.Round
        )
    }
}

@Composable
fun NextUpCard(
    nextUp: NextUpShowEpisode,
    onShowClick: (Long) -> Unit,
    onMarkWatched: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onShowClick(nextUp.showId) }
            .testTag("next_up_card_${nextUp.episodeId}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Thumbnail / Poster
            Box(
                modifier = Modifier
                    .size(width = 80.dp, height = 80.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surface)
            ) {
                if (!nextUp.showImageMedium.isNullOrEmpty()) {
                    AsyncImage(
                        model = nextUp.showImageMedium,
                        contentDescription = nextUp.showName,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.size(80.dp)
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier
                            .size(32.dp)
                            .align(Alignment.Center),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Text Info
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = nextUp.showName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "S${nextUp.season} · E${nextUp.number}${if (!nextUp.episodeName.isNullOrBlank()) " - ${nextUp.episodeName}" else ""}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (nextUp.runtime != null) {
                        Text(
                            text = "${nextUp.runtime} min",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (!nextUp.airdate.isNullOrBlank()) {
                        Text(
                            text = "·",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = nextUp.airdate,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Quick Mark Watched Button
            FilledTonalButton(
                onClick = { onMarkWatched(nextUp.episodeId) },
                shape = CircleShape,
                modifier = Modifier
                    .size(46.dp)
                    .testTag("quick_watch_${nextUp.episodeId}"),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Mark watched",
                    tint = EmeraldWatched
                )
            }
        }
    }
}

@Composable
fun TrackedShowItem(
    showWithProgress: ShowWithProgress,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("show_card_${showWithProgress.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            // Poster
            Box(
                modifier = Modifier
                    .width(76.dp)
                    .height(110.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                if (!showWithProgress.imageMedium.isNullOrEmpty()) {
                    AsyncImage(
                        model = showWithProgress.imageMedium,
                        contentDescription = showWithProgress.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.size(width = 76.dp, height = 110.dp)
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.LiveTv,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                        modifier = Modifier.size(32.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Details
            Column(
                modifier = Modifier
                    .weight(1f)
                    .align(Alignment.CenterVertically)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = showWithProgress.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    StatusBadge(status = showWithProgress.listStatus)
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (showWithProgress.ratingAverage != null) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = "Rating",
                                tint = AmberRating,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "%.1f".format(showWithProgress.ratingAverage),
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                    if (!showWithProgress.premiered.isNullOrBlank()) {
                        val year = showWithProgress.premiered.take(4)
                        Text(
                            text = year,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (showWithProgress.genres.isNotBlank()) {
                        val firstGenre = showWithProgress.genres.split(",").firstOrNull()?.trim()
                        if (firstGenre != null) {
                            Text(
                                text = "· $firstGenre",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                ShowProgressIndicator(
                    watchedEpisodes = showWithProgress.watchedEpisodes,
                    totalEpisodes = showWithProgress.totalEpisodes
                )
            }
        }
    }
}

@Composable
fun EpisodeItemRow(
    episode: EpisodeEntity,
    onToggleWatched: (Boolean) -> Unit,
    onMarkPreviousWatched: () -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    var menuOpen by remember { mutableStateOf(false) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { expanded = !expanded }
            .testTag("episode_item_${episode.id}"),
        colors = CardDefaults.cardColors(
            containerColor = if (episode.isWatched)
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            else
                MaterialTheme.colorScheme.surface
        ),
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = if (episode.isWatched) EmeraldWatched.copy(alpha = 0.3f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Episode Code badge (e.g. "E01")
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(
                            if (episode.isWatched) EmeraldWatched.copy(alpha = 0.2f)
                            else MaterialTheme.colorScheme.primaryContainer
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "E${episode.number}",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (episode.isWatched) EmeraldWatched else MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Title and Meta
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = episode.name ?: "Episode ${episode.number}",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = if (expanded) 3 else 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (!episode.airdate.isNullOrBlank()) {
                            Text(
                                text = episode.airdate,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (episode.runtime != null) {
                            Text(
                                text = "· ${episode.runtime}m",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Options menu (Mark previous watched)
                Box {
                    IconButton(
                        onClick = { menuOpen = true },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Episode options",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    DropdownMenu(
                        expanded = menuOpen,
                        onDismissRequest = { menuOpen = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Mark up to here as watched") },
                            onClick = {
                                menuOpen = false
                                onMarkPreviousWatched()
                            }
                        )
                    }
                }

                // Watched Checkbox Icon
                IconButton(
                    onClick = { onToggleWatched(episode.isWatched) },
                    modifier = Modifier
                        .size(44.dp)
                        .testTag("toggle_watch_${episode.id}")
                ) {
                    Icon(
                        imageVector = if (episode.isWatched) Icons.Filled.CheckCircle else Icons.Outlined.CheckCircle,
                        contentDescription = if (episode.isWatched) "Marked as watched" else "Mark as watched",
                        tint = if (episode.isWatched) EmeraldWatched else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            // Expandable Summary & Thumbnail
            if (expanded) {
                Spacer(modifier = Modifier.height(10.dp))
                if (!episode.imageMedium.isNullOrBlank()) {
                    AsyncImage(
                        model = episode.imageMedium,
                        contentDescription = episode.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                            .clip(RoundedCornerShape(8.dp))
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
                val summaryText = cleanHtml(episode.summary)
                if (summaryText.isNotBlank()) {
                    Text(
                        text = summaryText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 18.sp
                    )
                } else {
                    Text(
                        text = "No summary available for this episode.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}
