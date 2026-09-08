package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.RemoveCircleOutline
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.data.api.ShowDto
import com.example.data.db.EpisodeEntity
import com.example.data.db.ListStatus
import com.example.ui.components.EpisodeItemRow
import com.example.ui.components.ShowProgressIndicator
import com.example.ui.components.StatusBadge
import com.example.ui.components.cleanHtml
import com.example.ui.theme.AmberRating
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.EmeraldWatched
import com.example.ui.theme.IndigoPrimary
import com.example.ui.viewmodel.TvViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ShowDetailScreen(
    showId: Long,
    fallbackShowDto: ShowDto?,
    viewModel: TvViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    LaunchedEffect(showId) {
        viewModel.loadShowDetails(showId, fallbackShowDto)
    }

    val trackedShowWithProgress by viewModel.currentShowWithProgress.collectAsStateWithLifecycle()
    val episodes by viewModel.currentEpisodes.collectAsStateWithLifecycle()
    val remoteShowDto by viewModel.remoteShowDto.collectAsStateWithLifecycle()
    val remoteEpisodesDto by viewModel.remoteEpisodesDto.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoadingDetail.collectAsStateWithLifecycle()
    val selectedSeason by viewModel.selectedSeason.collectAsStateWithLifecycle()

    val isTracked = trackedShowWithProgress != null
    val showName = trackedShowWithProgress?.name ?: remoteShowDto?.name ?: fallbackShowDto?.name ?: "Show Details"
    val showSummary = trackedShowWithProgress?.summary ?: remoteShowDto?.summary ?: fallbackShowDto?.summary
    val showImageOriginal = trackedShowWithProgress?.imageOriginal ?: remoteShowDto?.image?.original ?: fallbackShowDto?.image?.original
    val showImageMedium = trackedShowWithProgress?.imageMedium ?: remoteShowDto?.image?.medium ?: fallbackShowDto?.image?.medium
    val ratingAvg = trackedShowWithProgress?.ratingAverage ?: remoteShowDto?.rating?.average ?: fallbackShowDto?.rating?.average
    val showStatus = trackedShowWithProgress?.status ?: remoteShowDto?.status ?: fallbackShowDto?.status
    val premiered = trackedShowWithProgress?.premiered ?: remoteShowDto?.premiered ?: fallbackShowDto?.premiered
    val network = trackedShowWithProgress?.networkName ?: remoteShowDto?.network?.name ?: fallbackShowDto?.network?.name
    val genresList = if (trackedShowWithProgress != null) {
        trackedShowWithProgress?.genres?.split(",")?.map { it.trim() }?.filter { it.isNotBlank() } ?: emptyList()
    } else {
        remoteShowDto?.genres ?: fallbackShowDto?.genres ?: emptyList()
    }

    // Convert remote episodes to EpisodeEntities if not yet tracked in Room
    val displayEpisodes: List<EpisodeEntity> = if (isTracked && episodes.isNotEmpty()) {
        episodes
    } else {
        remoteEpisodesDto.map { dto ->
            EpisodeEntity(
                id = dto.id,
                showId = showId,
                name = dto.name ?: "Episode ${dto.number ?: ""}",
                season = dto.season,
                number = dto.number ?: 0,
                airdate = dto.airdate,
                runtime = dto.runtime,
                summary = dto.summary,
                imageMedium = dto.image?.medium,
                isWatched = false
            )
        }
    }

    // Calculate seasons list
    val seasons = remember(displayEpisodes) {
        displayEpisodes.map { it.season }.distinct().sorted()
    }

    val episodesInSelectedSeason = remember(displayEpisodes, selectedSeason) {
        displayEpisodes.filter { it.season == selectedSeason }.sortedBy { it.number }
    }

    val totalEpisodesCount = if (isTracked) (trackedShowWithProgress?.totalEpisodes ?: displayEpisodes.size) else displayEpisodes.size
    val watchedEpisodesCount = if (isTracked) (trackedShowWithProgress?.watchedEpisodes ?: 0) else 0

    var statusMenuExpanded by remember { mutableStateOf(false) }
    var ratingDialogOpen by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("show_detail_screen"),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = showName,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("detail_back_button")) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (isTracked) {
                        IconButton(
                            onClick = { viewModel.onRemoveShow(showId) },
                            modifier = Modifier.testTag("remove_show_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Remove from my shows",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(bottom = 60.dp)
        ) {
            // --- Hero Banner / Poster ---
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(230.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    val bannerUrl = showImageOriginal ?: showImageMedium
                    if (!bannerUrl.isNullOrEmpty()) {
                        AsyncImage(
                            model = bannerUrl,
                            contentDescription = showName,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        // Gradient scrim for readability
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            Color.Transparent,
                                            MaterialTheme.colorScheme.background.copy(alpha = 0.85f),
                                            MaterialTheme.colorScheme.background
                                        ),
                                        startY = 60f
                                    )
                                )
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                                            MaterialTheme.colorScheme.background
                                        )
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.LiveTv,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                            )
                        }
                    }

                    // Content over hero banner
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomStart)
                            .padding(horizontal = 20.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.Bottom
                    ) {
                        // Floating Poster Card
                        Box(
                            modifier = Modifier
                                .width(90.dp)
                                .height(130.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            if (!showImageMedium.isNullOrEmpty()) {
                                AsyncImage(
                                    model = showImageMedium,
                                    contentDescription = showName,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.LiveTv,
                                    contentDescription = null,
                                    modifier = Modifier.size(36.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        // Title & Status
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = showName,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                if (ratingAvg != null) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Star,
                                            contentDescription = null,
                                            tint = AmberRating,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text(
                                            text = "%.1f".format(ratingAvg),
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    }
                                }
                                if (!showStatus.isNullOrBlank()) {
                                    Text(
                                        text = "· $showStatus",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                if (!premiered.isNullOrBlank()) {
                                    Text(
                                        text = "· ${premiered.take(4)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // --- Action Toolbar (Tracking & User Status) ---
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (!isTracked) {
                            Box(modifier = Modifier.weight(1f)) {
                                Button(
                                    onClick = { statusMenuExpanded = true },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("track_show_button"),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text(
                                        text = "Add to list",
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(
                                        imageVector = Icons.Default.KeyboardArrowDown,
                                        contentDescription = "Add to list options"
                                    )
                                }

                                DropdownMenu(
                                    expanded = statusMenuExpanded,
                                    onDismissRequest = { statusMenuExpanded = false }
                                ) {
                                    ListStatus.all.forEach { statusOption ->
                                        DropdownMenuItem(
                                            text = { Text(statusOption) },
                                            onClick = {
                                                statusMenuExpanded = false
                                                val dto = remoteShowDto ?: fallbackShowDto ?: ShowDto(id = showId, name = showName)
                                                viewModel.onTrackShow(dto, statusOption)
                                            }
                                        )
                                    }
                                }
                            }
                        } else {
                            // Status Picker Button
                            Box(modifier = Modifier.weight(1f)) {
                                FilledTonalButton(
                                    onClick = { statusMenuExpanded = true },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("status_picker_button"),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text(
                                        text = "Status: ${trackedShowWithProgress?.listStatus ?: "Watching"}",
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(imageVector = Icons.Default.KeyboardArrowDown, contentDescription = null)
                                }

                                DropdownMenu(
                                    expanded = statusMenuExpanded,
                                    onDismissRequest = { statusMenuExpanded = false }
                                ) {
                                    ListStatus.all.forEach { statusOption ->
                                        DropdownMenuItem(
                                            text = { Text(statusOption) },
                                            onClick = {
                                                statusMenuExpanded = false
                                                viewModel.onUpdateShowStatus(showId, statusOption)
                                            }
                                        )
                                    }
                                }
                            }

                            // User Rating Button
                            OutlinedButton(
                                onClick = { ratingDialogOpen = true },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.testTag("user_rating_button")
                            ) {
                                val userRating = trackedShowWithProgress?.userRating ?: 0
                                Icon(
                                    imageVector = if (userRating > 0) Icons.Default.Star else Icons.Outlined.StarBorder,
                                    contentDescription = "User rating",
                                    tint = if (userRating > 0) AmberRating else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(if (userRating > 0) "$userRating/10" else "Rate")
                            }
                        }
                    }

                    // Genres chips
                    if (genresList.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            genresList.forEach { genre ->
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                                ) {
                                    Text(
                                        text = genre,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Synopsis
                    val cleanSummary = cleanHtml(showSummary)
                    if (cleanSummary.isNotBlank()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = cleanSummary,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                            lineHeight = 21.sp
                        )
                    }
                }
            }

            // --- Overall Show Progress Card ---
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 10.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Overall Show Progress",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            val percent = if (totalEpisodesCount > 0) (watchedEpisodesCount * 100) / totalEpisodesCount else 0
                            Text(
                                text = "$percent%",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (percent == 100) EmeraldWatched else MaterialTheme.colorScheme.primary
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        ShowProgressIndicator(
                            watchedEpisodes = watchedEpisodesCount,
                            totalEpisodes = totalEpisodesCount
                        )

                        // Quick action buttons for entire show
                        if (isTracked) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = { viewModel.onMarkAllEpisodesWatched(showId, true) },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = EmeraldWatched
                                    ),
                                    contentPadding = PaddingValues(vertical = 8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Mark All Watched", fontSize = 12.sp)
                                }

                                if (watchedEpisodesCount > 0) {
                                    OutlinedButton(
                                        onClick = { viewModel.onMarkAllEpisodesWatched(showId, false) },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(10.dp),
                                        contentPadding = PaddingValues(vertical = 8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.RemoveCircleOutline,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Unmark All", fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // --- Season Selector & Season Controls ---
            if (seasons.isNotEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp)
                    ) {
                        // Season Tabs
                        ScrollableTabRow(
                            selectedTabIndex = seasons.indexOf(selectedSeason).coerceAtLeast(0),
                            edgePadding = 20.dp,
                            containerColor = MaterialTheme.colorScheme.surface,
                            indicator = { tabPositions ->
                                val index = seasons.indexOf(selectedSeason).coerceAtLeast(0)
                                if (index < tabPositions.size) {
                                    TabRowDefaults.SecondaryIndicator(
                                        modifier = Modifier.tabIndicatorOffset(tabPositions[index]),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        ) {
                            seasons.forEach { seasonNum ->
                                val isSelected = seasonNum == selectedSeason
                                Tab(
                                    selected = isSelected,
                                    onClick = { viewModel.onSelectSeason(seasonNum) },
                                    text = {
                                        Text(
                                            text = "Season $seasonNum",
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                )
                            }
                        }

                        // Season Quick Action Bar
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val seasonWatchedCount = episodesInSelectedSeason.count { it.isWatched }
                            Text(
                                text = "Season $selectedSeason: $seasonWatchedCount/${episodesInSelectedSeason.size} watched",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (showId < 0) {
                                    TextButton(
                                        onClick = {
                                            viewModel.onAddCustomEpisode(showId, selectedSeason)
                                        },
                                        modifier = Modifier.testTag("add_custom_episode_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Add,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("+ Episode", fontSize = 12.sp)
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                }

                                if (isTracked) {
                                    val allSeasonWatched = episodesInSelectedSeason.isNotEmpty() && episodesInSelectedSeason.all { it.isWatched }
                                    TextButton(
                                        onClick = {
                                            viewModel.onMarkSeasonWatched(showId, selectedSeason, !allSeasonWatched)
                                        },
                                        modifier = Modifier.testTag("mark_season_button")
                                    ) {
                                        Icon(
                                            imageVector = if (allSeasonWatched) Icons.Default.RemoveCircleOutline else Icons.Default.Check,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp),
                                            tint = if (allSeasonWatched) MaterialTheme.colorScheme.error else EmeraldWatched
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = if (allSeasonWatched) "Unmark season" else "Mark season watched",
                                            fontSize = 12.sp,
                                            color = if (allSeasonWatched) MaterialTheme.colorScheme.error else EmeraldWatched
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // --- Episodes List ---
                items(episodesInSelectedSeason, key = { "ep_${it.id}" }) { episode ->
                    Box(modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)) {
                        EpisodeItemRow(
                            episode = episode,
                            onToggleWatched = {
                                if (!isTracked) {
                                    val dto = remoteShowDto ?: fallbackShowDto ?: ShowDto(id = showId, name = showName)
                                    viewModel.onTrackShow(dto, ListStatus.WATCHING)
                                }
                                viewModel.onToggleEpisode(episode.id, it)
                            },
                            onMarkPreviousWatched = {
                                if (!isTracked) {
                                    val dto = remoteShowDto ?: fallbackShowDto ?: ShowDto(id = showId, name = showName)
                                    viewModel.onTrackShow(dto, ListStatus.WATCHING)
                                }
                                viewModel.onMarkPreviousEpisodesWatched(showId, episode.season, episode.number)
                            }
                        )
                    }
                }
            } else if (isLoading) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                }
            } else {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No episodes found for this show.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }

    // User Rating Dialog (1-10)
    if (ratingDialogOpen) {
        var tempRating by remember { mutableIntStateOf(trackedShowWithProgress?.userRating ?: 5) }
        AlertDialog(
            onDismissRequest = { ratingDialogOpen = false },
            title = { Text("Rate $showName", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = if (tempRating > 0) "$tempRating / 10" else "Unrated",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = AmberRating
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        items(10) { index ->
                            val score = index + 1
                            IconButton(
                                onClick = { tempRating = score },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = if (score <= tempRating) Icons.Default.Star else Icons.Outlined.StarBorder,
                                    contentDescription = "Rate $score",
                                    tint = if (score <= tempRating) AmberRating else MaterialTheme.colorScheme.outline
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        ratingDialogOpen = false
                        viewModel.onUpdateUserRating(showId, tempRating)
                    }
                ) {
                    Text("Save Rating")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        ratingDialogOpen = false
                        viewModel.onUpdateUserRating(showId, 0)
                    }
                ) {
                    Text("Clear")
                }
            }
        )
    }
}
