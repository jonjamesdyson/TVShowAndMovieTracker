package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.db.ListStatus
import com.example.ui.components.BackupRestoreDialog
import com.example.ui.components.NextUpCard
import com.example.ui.components.TrackedShowItem
import com.example.ui.viewmodel.TvViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: TvViewModel,
    onNavigateToShowDetail: (Long) -> Unit,
    onNavigateToSearch: () -> Unit,
    modifier: Modifier = Modifier
) {
    val selectedTab by viewModel.selectedStatusTab.collectAsStateWithLifecycle()
    val shows by viewModel.myShows.collectAsStateWithLifecycle()
    val allTrackedShows by viewModel.allTrackedShows.collectAsStateWithLifecycle()
    val nextUpEpisodes by viewModel.nextUpEpisodes.collectAsStateWithLifecycle()

    var isSearchActive by rememberSaveable { mutableStateOf(false) }
    var mySeriesSearchQuery by rememberSaveable { mutableStateOf("") }

    val filteredMyShows = remember(mySeriesSearchQuery, allTrackedShows) {
        if (mySeriesSearchQuery.isBlank()) {
            allTrackedShows
        } else {
            val q = mySeriesSearchQuery.trim().lowercase()
            allTrackedShows.filter { show ->
                show.name.lowercase().contains(q) ||
                show.genres.lowercase().contains(q) ||
                show.listStatus.lowercase().contains(q)
            }.sortedBy { it.name }
        }
    }

    val tabs = ListStatus.all

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .testTag("dashboard_screen"),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            // Top Header
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "My Series",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )

                        FilledTonalButton(
                            onClick = {
                                isSearchActive = !isSearchActive
                                if (!isSearchActive) mySeriesSearchQuery = ""
                            },
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                            modifier = Modifier.testTag("search_my_series_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Search", fontSize = 13.sp)
                        }
                    }

                    if (isSearchActive) {
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedTextField(
                            value = mySeriesSearchQuery,
                            onValueChange = { mySeriesSearchQuery = it },
                            placeholder = { Text("Search title, genre, or status...") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = "Search My Series",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            },
                            trailingIcon = {
                                if (mySeriesSearchQuery.isNotEmpty()) {
                                    IconButton(onClick = { mySeriesSearchQuery = "" }) {
                                        Icon(imageVector = Icons.Default.Clear, contentDescription = "Clear search")
                                    }
                                } else {
                                    IconButton(onClick = { isSearchActive = false }) {
                                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close search")
                                    }
                                }
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.surface,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surface
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("search_my_series_text_field")
                        )
                    }
                }
            }

            // If user is searching My Series
            if (isSearchActive && mySeriesSearchQuery.isNotBlank()) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Results in My Series (${filteredMyShows.size})",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                if (filteredMyShows.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No series in your list match \"$mySeriesSearchQuery\"",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    items(filteredMyShows, key = { "search_my_show_${it.id}" }) { showWithProgress ->
                        Box(modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)) {
                            TrackedShowItem(
                                showWithProgress = showWithProgress,
                                onClick = { onNavigateToShowDetail(showWithProgress.id) }
                            )
                        }
                    }
                }
            } else {
                // --- Next Up Section ---
                if (nextUpEpisodes.isNotEmpty()) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary)
                                )
                                Text(
                                    text = "Next Up",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "(${nextUpEpisodes.size})",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                        }
                    }

                    items(nextUpEpisodes, key = { "next_up_${it.episodeId}" }) { nextUp ->
                        Box(modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)) {
                            NextUpCard(
                                nextUp = nextUp,
                                onShowClick = onNavigateToShowDetail,
                                onMarkWatched = { viewModel.onQuickToggleNextUp(it) }
                            )
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }

                // --- Status Tabs ---
                item {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        LazyRow(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(tabs) { tab ->
                                val selected = tab == selectedTab
                                FilterChip(
                                    selected = selected,
                                    onClick = { viewModel.onSelectStatusTab(tab) },
                                    label = {
                                        Text(
                                            text = tab,
                                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                                        )
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                    ),
                                    shape = RoundedCornerShape(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }

                // --- Shows List in Selected Tab ---
                if (shows.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 24.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Default.LiveTv,
                                    contentDescription = null,
                                    modifier = Modifier.size(48.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "No shows in \"$selectedTab\"",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = when (selectedTab) {
                                        ListStatus.WATCHING -> "Add shows you are currently watching to easily track episodes."
                                        ListStatus.COMPLETED -> "Shows you've completely watched will appear here."
                                        ListStatus.PLAN_TO_WATCH -> "Bookmark shows to watch later."
                                        else -> "Shows marked as dropped will appear here."
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Button(
                                    onClick = onNavigateToSearch,
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text("Discover Shows")
                                }
                            }
                        }
                    }
                } else {
                    items(shows, key = { "show_${it.id}" }) { showWithProgress ->
                        Box(modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)) {
                            TrackedShowItem(
                                showWithProgress = showWithProgress,
                                onClick = { onNavigateToShowDetail(showWithProgress.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}
