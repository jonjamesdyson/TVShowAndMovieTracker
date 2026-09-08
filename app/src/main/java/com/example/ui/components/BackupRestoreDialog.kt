package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.viewmodel.TvViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupRestoreDialog(
    viewModel: TvViewModel,
    onDismissRequest: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf("Note", "Export JSON", "Import JSON")

    var noteContent by remember { mutableStateOf<String?>(null) }
    var jsonContent by remember { mutableStateOf<String?>(null) }
    var isLoadingExport by remember { mutableStateOf(false) }

    var importJsonText by remember { mutableStateOf("") }
    var isImporting by remember { mutableStateOf(false) }
    var importStatusMessage by remember { mutableStateOf<String?>(null) }
    var isImportSuccess by remember { mutableStateOf(false) }

    // Document creators for downloading Note & JSON
    val createNoteLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/plain")
    ) { uri: Uri? ->
        if (uri != null && noteContent != null) {
            try {
                context.contentResolver.openOutputStream(uri)?.use { stream ->
                    stream.write(noteContent!!.toByteArray(Charsets.UTF_8))
                }
                Toast.makeText(context, "Note downloaded successfully!", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to download note: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val createJsonLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        if (uri != null && jsonContent != null) {
            try {
                context.contentResolver.openOutputStream(uri)?.use { stream ->
                    stream.write(jsonContent!!.toByteArray(Charsets.UTF_8))
                }
                Toast.makeText(context, "JSON backup downloaded successfully!", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to download JSON: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // File picker launcher for JSON import
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    val text = stream.bufferedReader().use { it.readText() }
                    importJsonText = text
                    importStatusMessage = "Loaded file (${text.length} characters). Ready to import."
                    isImportSuccess = false
                }
            } catch (e: Exception) {
                importStatusMessage = "Failed to read file: ${e.localizedMessage}"
                isImportSuccess = false
            }
        }
    }

    // Load exports when tabs are switched
    LaunchedEffect(selectedTabIndex) {
        when (selectedTabIndex) {
            0 -> {
                if (noteContent == null) {
                    isLoadingExport = true
                    noteContent = viewModel.getExportNote()
                    isLoadingExport = false
                }
            }
            1 -> {
                if (jsonContent == null) {
                    isLoadingExport = true
                    jsonContent = viewModel.getExportJson()
                    isLoadingExport = false
                }
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        modifier = Modifier.testTag("backup_restore_dialog"),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 36.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Storage,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Backup & Export Data",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
                IconButton(onClick = onDismissRequest) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close"
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Navigation Tabs
            ScrollableTabRow(
                selectedTabIndex = selectedTabIndex,
                edgePadding = 0.dp,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                        color = MaterialTheme.colorScheme.primary
                    )
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        text = {
                            Text(
                                text = title,
                                fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Content per tab
            when (selectedTabIndex) {
                0 -> {
                    // Export Note Tab
                    Text(
                        text = "Export all watched shows, ratings, and progress formatted as a clean note.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    if (isLoadingExport) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    } else {
                        val note = noteContent ?: ""
                        // Note preview box
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 240.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
                                .padding(12.dp)
                                .verticalScroll(rememberScrollState())
                        ) {
                            Text(
                                text = note,
                                style = MaterialTheme.typography.bodySmall,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                lineHeight = 16.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Action Buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    val clip = ClipData.newPlainText("SeriesTracker Note", note)
                                    clipboard.setPrimaryClip(clip)
                                    Toast.makeText(context, "Note copied to clipboard!", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("copy_note_button"),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Copy")
                            }

                            FilledTonalButton(
                                onClick = {
                                    createNoteLauncher.launch("SeriesTracker_Watchlist_Note.txt")
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("download_note_button"),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(imageVector = Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Download")
                            }
                        }
                    }
                }

                1 -> {
                    // Export JSON Tab
                    Text(
                        text = "Export your full database backup in JSON format to restore on any device.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    if (isLoadingExport) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    } else {
                        val json = jsonContent ?: ""
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 240.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
                                .padding(12.dp)
                                .verticalScroll(rememberScrollState())
                        ) {
                            Text(
                                text = json,
                                style = MaterialTheme.typography.bodySmall,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp,
                                lineHeight = 14.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    val clip = ClipData.newPlainText("SeriesTracker Backup JSON", json)
                                    clipboard.setPrimaryClip(clip)
                                    Toast.makeText(context, "JSON copied to clipboard!", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("copy_json_button"),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Copy")
                            }

                            FilledTonalButton(
                                onClick = {
                                    createJsonLauncher.launch("SeriesTracker_Backup.json")
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("download_json_button"),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(imageVector = Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Download")
                            }
                        }
                    }
                }

                2 -> {
                    // Import JSON Tab
                    Text(
                        text = "Restore shows, watch progress, and ratings from a previously exported JSON backup.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                filePickerLauncher.launch("*/*")
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("pick_file_button"),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(imageVector = Icons.Default.FileUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Pick File", fontSize = 12.sp)
                        }

                        OutlinedButton(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = clipboard.primaryClip
                                if (clip != null && clip.itemCount > 0) {
                                    val pasted = clip.getItemAt(0).text?.toString() ?: ""
                                    if (pasted.isNotBlank()) {
                                        importJsonText = pasted
                                        importStatusMessage = "Pasted ${pasted.length} characters from clipboard."
                                    } else {
                                        Toast.makeText(context, "Clipboard is empty", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("paste_clipboard_button"),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(imageVector = Icons.Default.ContentPaste, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Paste Text", fontSize = 12.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = importJsonText,
                        onValueChange = {
                            importJsonText = it
                            importStatusMessage = null
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                            .testTag("import_json_textfield"),
                        placeholder = { Text("Paste JSON backup content here or choose a file...", fontSize = 12.sp) },
                        textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace, fontSize = 11.sp),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface
                        )
                    )

                    if (importStatusMessage != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = importStatusMessage ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isImportSuccess) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = {
                            if (importJsonText.isBlank()) {
                                importStatusMessage = "Please paste JSON text or select a backup file first."
                                isImportSuccess = false
                                return@Button
                            }
                            isImporting = true
                            importStatusMessage = null
                            viewModel.importBackup(importJsonText) { result ->
                                isImporting = false
                                result.onSuccess { (shows, eps) ->
                                    isImportSuccess = true
                                    importStatusMessage = "Successfully restored $shows shows and $eps episodes!"
                                    Toast.makeText(context, "Backup restored successfully!", Toast.LENGTH_LONG).show()
                                }.onFailure { error ->
                                    isImportSuccess = false
                                    importStatusMessage = "Import failed: ${error.localizedMessage ?: "Invalid JSON"}"
                                }
                            }
                        },
                        enabled = !isImporting && importJsonText.isNotBlank(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("execute_import_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (isImporting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Restoring Data...")
                        } else {
                            Icon(imageVector = Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Restore Database from Backup")
                        }
                    }
                }
            }
        }
    }
}
