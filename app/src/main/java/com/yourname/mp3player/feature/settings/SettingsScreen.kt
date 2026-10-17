package com.yourname.mp3player.feature.settings

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.yourname.mp3player.App
import com.yourname.mp3player.core.AppInfo
import com.yourname.mp3player.core.ui.AboutDialog
import com.yourname.mp3player.domain.DedupeMode
import com.yourname.mp3player.domain.ThemeMode
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val app = LocalContext.current.applicationContext as App
    val vm: SettingsViewModel = viewModel(
        factory = viewModelFactory {
            initializer {
                val c = app.container
                SettingsViewModel(c.settings, c.rawMusicRepository, c.userData, c.rankingRepository, c.backup)
            }
        }
    )

    val theme by vm.settings.theme.collectAsState()
    val dedupe by vm.settings.dedupeMode.collectAsState()
    val hidden by vm.hiddenSongs.collectAsState()
    val status by vm.status.collectAsState()
    val busy by vm.busy.collectAsState()

    var pendingImport by remember { mutableStateOf<Uri?>(null) }
    var confirmReset by remember { mutableStateOf(false) }
    var showAbout by remember { mutableStateOf(false) }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri -> if (uri != null) vm.export(uri) }
    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri -> if (uri != null) pendingImport = uri }

    Column(Modifier.fillMaxSize().systemBarsPadding()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
            Text("Settings", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        }

        LazyColumn(contentPadding = PaddingValues(bottom = 32.dp)) {
            // ---------------- appearance
            item { Section("Appearance") }
            item {
                ChipRow {
                    ThemeMode.entries.forEach { m ->
                        FilterChip(selected = theme == m, onClick = { vm.settings.setTheme(m) }, label = { Text(m.label) })
                    }
                }
            }

            // ---------------- duplicates
            item { Section("Duplicate songs") }
            item {
                ChipRow {
                    DedupeMode.entries.forEach { m ->
                        FilterChip(selected = dedupe == m, onClick = { vm.settings.setDedupeMode(m) }, label = { Text(m.label) })
                    }
                }
                Hint(dedupe.hint)
            }

            // ---------------- hidden
            item { Section("Hidden songs (${hidden.size})") }
            if (hidden.isEmpty()) {
                item { Hint("Songs you hide from the ⋮ menu appear here, so you can bring them back.") }
            } else {
                items(hidden, key = { it.id }) { song ->
                    Row(
                        Modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp, top = 4.dp, bottom = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(song.title, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodyLarge)
                            Text(
                                song.artist, maxLines = 1, overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        TextButton(onClick = { vm.unhide(song.id) }) { Text("Unhide") }
                    }
                }
                item { TextButton(onClick = vm::unhideAll, modifier = Modifier.padding(horizontal = 12.dp)) { Text("Unhide all") } }
            }

            // ---------------- backup
            item { Section("Backup & restore") }
            item {
                Hint("Saves your ranking, listening history, favorites, playlists, hidden songs and settings in one file. Restore it after a reinstall or on a new phone.")
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        enabled = !busy,
                        onClick = {
                            val day = SimpleDateFormat("yyyyMMdd", Locale.US).format(Date())
                            exportLauncher.launch("sonora-backup-$day.json")
                        },
                        modifier = Modifier.weight(1f)
                    ) { Text("Export") }
                    OutlinedButton(
                        enabled = !busy,
                        onClick = { importLauncher.launch(arrayOf("*/*")) },
                        modifier = Modifier.weight(1f)
                    ) { Text("Restore") }
                }
                if (busy) LinearProgressIndicator(Modifier.fillMaxWidth().padding(horizontal = 20.dp))
                status?.let { Hint(it) }
            }

            // ---------------- ranking
            item { Section("Ranking") }
            item {
                OutlinedButton(onClick = { confirmReset = true }, modifier = Modifier.padding(horizontal = 20.dp)) {
                    Text("Reset ranking", color = MaterialTheme.colorScheme.error)
                }
            }

            // ---------------- about
            item { Section("About") }
            item {
                Column(Modifier.fillMaxWidth().clickable { showAbout = true }.padding(horizontal = 20.dp, vertical = 8.dp)) {
                    Text("About ${AppInfo.APP_NAME}", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        AppInfo.COPYRIGHT, style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }

    if (showAbout) AboutDialog(onDismiss = { showAbout = false })

    if (confirmReset) {
        AlertDialog(
            onDismissRequest = { confirmReset = false },
            title = { Text("Reset the whole ranking?") },
            text = { Text("All points, play counts and listening history will be erased. This can't be undone.") },
            confirmButton = { TextButton(onClick = { vm.resetRanking(); confirmReset = false }) { Text("Reset all") } },
            dismissButton = { TextButton(onClick = { confirmReset = false }) { Text("Cancel") } }
        )
    }

    pendingImport?.let { uri ->
        AlertDialog(
            onDismissRequest = { pendingImport = null },
            title = { Text("Restore this backup?") },
            text = { Text("This REPLACES your current ranking, favorites, playlists and hidden songs with the content of the backup.") },
            confirmButton = { TextButton(onClick = { vm.import(uri); pendingImport = null }) { Text("Restore") } },
            dismissButton = { TextButton(onClick = { pendingImport = null }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun Section(title: String) {
    Text(
        title,
        modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 8.dp),
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary
    )
}

@Composable
private fun Hint(text: String) {
    Text(
        text,
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
private fun ChipRow(content: @Composable () -> Unit) {
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) { content() }
}
