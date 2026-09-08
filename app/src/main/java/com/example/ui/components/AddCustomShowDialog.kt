package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.data.db.ListStatus

private val POPULAR_GENRES = listOf(
    "Drama", "Comedy", "Animation", "Sci-Fi", "Fantasy", "Action", "Documentary", "Thriller"
)

private val POPULAR_PLATFORMS = listOf(
    "YouTube", "Web Series", "Patreon", "Netflix", "Custom"
)

@Composable
fun AddCustomShowDialog(
    initialTitle: String = "",
    onDismissRequest: () -> Unit,
    onSaveShow: (
        name: String,
        summary: String?,
        imageUrl: String?,
        genres: String,
        seasonsCount: Int,
        episodesPerSeason: Int,
        averageRuntime: Int,
        networkName: String?,
        listStatus: String,
        premieredYear: String?
    ) -> Unit
) {
    var title by remember { mutableStateOf(initialTitle) }
    var selectedStatus by remember { mutableStateOf(ListStatus.WATCHING) }
    var seasonsCount by remember { mutableIntStateOf(1) }
    var episodesPerSeason by remember { mutableIntStateOf(10) }
    var averageRuntime by remember { mutableIntStateOf(45) }
    var selectedGenre by remember { mutableStateOf("Drama") }
    var customGenre by remember { mutableStateOf("") }
    var selectedPlatform by remember { mutableStateOf("Custom") }
    var premieredYear by remember { mutableStateOf("") }
    var imageUrl by remember { mutableStateOf("") }
    var synopsis by remember { mutableStateOf("") }

    val isTitleValid = title.trim().isNotEmpty()

    AlertDialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier
            .padding(16.dp)
            .fillMaxWidth()
            .testTag("add_custom_show_dialog"),
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Tv,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Column {
                    Text(
                        text = "Add Custom Show",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Track any series not found online",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 480.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Title Field
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Show Title *") },
                    placeholder = { Text("e.g. Critical Role, Web Series...") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("custom_show_title_input"),
                    shape = RoundedCornerShape(12.dp),
                    trailingIcon = {
                        if (title.isNotEmpty()) {
                            IconButton(onClick = { title = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear")
                            }
                        }
                    }
                )

                // Status Chips
                Column {
                    Text(
                        text = "Initial Status",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ListStatus.all.forEach { status ->
                            val isSelected = selectedStatus == status
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedStatus = status },
                                label = { Text(status) },
                                shape = RoundedCornerShape(10.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                )
                            )
                        }
                    }
                }

                // Seasons and Episodes Count Steppers
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Seasons Stepper
                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Seasons",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                IconButton(
                                    onClick = { if (seasonsCount > 1) seasonsCount-- },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Default.Remove, contentDescription = "Decrease seasons", modifier = Modifier.size(16.dp))
                                }
                                Text(
                                    text = "$seasonsCount",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp)
                                )
                                IconButton(
                                    onClick = { if (seasonsCount < 50) seasonsCount++ },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = "Increase seasons", modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }

                    // Episodes per Season Stepper
                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Episodes / Season",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                IconButton(
                                    onClick = { if (episodesPerSeason > 1) episodesPerSeason-- },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Default.Remove, contentDescription = "Decrease episodes", modifier = Modifier.size(16.dp))
                                }
                                Text(
                                    text = "$episodesPerSeason",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp)
                                )
                                IconButton(
                                    onClick = { if (episodesPerSeason < 100) episodesPerSeason++ },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = "Increase episodes", modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }

                // Average Runtime (min)
                Column {
                    Text(
                        text = "Episode Runtime: $averageRuntime min",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(20, 30, 45, 60, 90).forEach { mins ->
                            val isSelected = averageRuntime == mins
                            FilterChip(
                                selected = isSelected,
                                onClick = { averageRuntime = mins },
                                label = { Text("${mins}m") },
                                shape = RoundedCornerShape(8.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            )
                        }
                    }
                }

                // Genre selection
                Column {
                    Text(
                        text = "Genre",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        POPULAR_GENRES.forEach { g ->
                            val isSelected = selectedGenre == g && customGenre.isBlank()
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    selectedGenre = g
                                    customGenre = ""
                                },
                                label = { Text(g) },
                                shape = RoundedCornerShape(8.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = customGenre,
                        onValueChange = { customGenre = it },
                        placeholder = { Text("Or other genre (e.g. Podcast, Reality)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )
                }

                // Platform / Network
                Column {
                    Text(
                        text = "Network / Platform",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        POPULAR_PLATFORMS.forEach { p ->
                            val isSelected = selectedPlatform == p
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedPlatform = p },
                                label = { Text(p) },
                                shape = RoundedCornerShape(8.dp)
                            )
                        }
                    }
                }

                // Release Year
                OutlinedTextField(
                    value = premieredYear,
                    onValueChange = { if (it.length <= 4 && it.all { char -> char.isDigit() }) premieredYear = it },
                    label = { Text("Release Year (Optional)") },
                    placeholder = { Text("e.g. 2024") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                // Poster Image URL (Optional)
                Column {
                    OutlinedTextField(
                        value = imageUrl,
                        onValueChange = { imageUrl = it },
                        label = { Text("Poster Image URL (Optional)") },
                        placeholder = { Text("https://example.com/poster.jpg") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )
                    if (imageUrl.isNotBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Box(
                            modifier = Modifier
                                .size(width = 70.dp, height = 95.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            AsyncImage(
                                model = imageUrl.trim(),
                                contentDescription = "Poster preview",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }

                // Synopsis / Notes (Optional)
                OutlinedTextField(
                    value = synopsis,
                    onValueChange = { synopsis = it },
                    label = { Text("Synopsis / Summary (Optional)") },
                    placeholder = { Text("Brief description of this series...") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    maxLines = 4,
                    shape = RoundedCornerShape(10.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (isTitleValid) {
                        val finalGenre = if (customGenre.isNotBlank()) customGenre.trim() else selectedGenre
                        onSaveShow(
                            title.trim(),
                            synopsis.trim().ifBlank { null },
                            imageUrl.trim().ifBlank { null },
                            finalGenre,
                            seasonsCount,
                            episodesPerSeason,
                            averageRuntime,
                            selectedPlatform.trim().ifBlank { null },
                            selectedStatus,
                            premieredYear.trim().ifBlank { null }
                        )
                    }
                },
                enabled = isTitleValid,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("save_custom_show_button")
            ) {
                Text("Add Show")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismissRequest,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("cancel_custom_show_button")
            ) {
                Text("Cancel")
            }
        }
    )
}
