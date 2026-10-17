package com.yourname.mp3player.feature.ranking

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.yourname.mp3player.App
import com.yourname.mp3player.core.ui.AlbumArt
import com.yourname.mp3player.domain.RankedSong
import com.yourname.mp3player.feature.player.rememberPlayerViewModel
import com.yourname.mp3player.feature.songmenu.SongActionsViewModel
import com.yourname.mp3player.feature.songmenu.SongMenuButton
import com.yourname.mp3player.feature.songmenu.rememberSongActions
import android.widget.Toast
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedTextField
import kotlin.math.roundToInt

@Composable
fun RankingScreen() {
    val app = LocalContext.current.applicationContext as App
    val vm: RankingViewModel = viewModel(
        factory = viewModelFactory {
            initializer {
                RankingViewModel(
                    app.container.rankedSongs,
                    app.container.rankingRepository,
                    app.container.playerController,
                    app.container.userData
                )
            }
        }
    )
    val playerVm = rememberPlayerViewModel()

    val ranked by vm.ranked.collectAsState()
    val selected by vm.selected.collectAsState()
    val option by vm.option.collectAsState()
    val custom by vm.custom.collectAsState()
    val playback by playerVm.state.collectAsState()

    var confirmAll by remember { mutableStateOf(false) }
    var confirmSong by remember { mutableStateOf<RankedSong?>(null) }
    var saveName by remember { mutableStateOf<String?>(null) }
    val actions = rememberSongActions()
    val ctx = LocalContext.current

    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp, top = 16.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    "Ranking",
                    style = MaterialTheme.typography.headlineLarge.copy(
                        brush = Brush.linearGradient(
                            listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary)
                        ),
                        fontWeight = FontWeight.ExtraBold
                    )
                )
                Text(
                    if (ranked.size == 1) "1 ranked song" else "${ranked.size} ranked songs",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (ranked.isNotEmpty()) {
                IconButton(onClick = { confirmAll = true }) {
                    Icon(Icons.Default.DeleteSweep, "Reset ranking")
                }
            }
        }

        if (ranked.isEmpty()) {
            EmptyRanking()
        } else {
            SmartPlaylistCard(
                option = option,
                count = selected.size,
                points = selected.sumOf { it.points },
                totalRanked = ranked.size,
                custom = custom,
                onSelect = vm::select,
                onCustom = vm::setCustom,
                onPlay = { vm.playAll(shuffle = false) },
                onShuffle = { vm.playAll(shuffle = true) },
                onSave = {
                    saveName = when (option) {
                        TopOption.ALL -> "Full ranking"
                        TopOption.CUSTOM -> "Top $custom ranking"
                        else -> "${option.label} ranking"
                    }
                }
            )
            Spacer(Modifier.height(8.dp))
            LazyColumn(Modifier.fillMaxSize()) {
                itemsIndexed(selected, key = { _, r -> r.song.id }) { index, item ->
                    RankRow(
                        item = item,
                        isCurrent = playback.currentSong?.id == item.song.id,
                        onClick = { vm.playFrom(index) },
                        onReset = { confirmSong = item },
                        actions = actions
                    )
                }
            }
        }
    }

    if (confirmAll) {
        AlertDialog(
            onDismissRequest = { confirmAll = false },
            title = { Text("Reset the whole ranking?") },
            text = { Text("All points and play counts will be erased. This can't be undone.") },
            confirmButton = {
                TextButton(onClick = { vm.resetAll(); confirmAll = false }) { Text("Reset all") }
            },
            dismissButton = { TextButton(onClick = { confirmAll = false }) { Text("Cancel") } }
        )
    }

    saveName?.let { name ->
        AlertDialog(
            onDismissRequest = { saveName = null },
            title = { Text("Save as playlist") },
            text = {
                OutlinedTextField(
                    value = name,
                    onValueChange = { saveName = it },
                    singleLine = true,
                    label = { Text("Playlist name") }
                )
            },
            confirmButton = {
                TextButton(
                    enabled = name.isNotBlank(),
                    onClick = {
                        vm.saveAsPlaylist(name)
                        saveName = null
                        Toast.makeText(ctx, "Saved in Playlists", Toast.LENGTH_SHORT).show()
                    }
                ) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { saveName = null }) { Text("Cancel") } }
        )
    }

    confirmSong?.let { item ->
        AlertDialog(
            onDismissRequest = { confirmSong = null },
            title = { Text("Reset this song?") },
            text = {
                Text("\"${item.song.title}\" will lose its ${item.points} pts and leave the ranking.")
            },
            confirmButton = {
                TextButton(onClick = { vm.resetSong(item.song.id); confirmSong = null }) { Text("Reset") }
            },
            dismissButton = { TextButton(onClick = { confirmSong = null }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun SmartPlaylistCard(
    option: TopOption,
    count: Int,
    points: Int,
    totalRanked: Int,
    custom: Int,
    onSelect: (TopOption) -> Unit,
    onCustom: (Int) -> Unit,
    onPlay: () -> Unit,
    onShuffle: () -> Unit,
    onSave: () -> Unit
) {
    Surface(
        Modifier.fillMaxWidth().padding(horizontal = 20.dp),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) { Icon(Icons.Default.EmojiEvents, null, tint = Color.White) }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text("Top Ranked", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        "$count songs · $points pts",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(TopOption.entries) { o ->
                    FilterChip(
                        selected = o == option,
                        onClick = { onSelect(o) },
                        label = { Text(o.label) }
                    )
                }
            }
            if (option == TopOption.CUSTOM && totalRanked >= 2) {
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Top $custom", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Slider(
                        value = custom.toFloat(),
                        onValueChange = { onCustom(it.roundToInt()) },
                        valueRange = 1f..totalRanked.toFloat(),
                        modifier = Modifier.weight(1f).padding(start = 12.dp)
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = onPlay, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.PlayArrow, null)
                    Spacer(Modifier.width(6.dp))
                    Text("Play")
                }
                OutlinedButton(onClick = onShuffle, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.Shuffle, null)
                    Spacer(Modifier.width(6.dp))
                    Text("Shuffle")
                }
            }
            TextButton(onClick = onSave, modifier = Modifier.align(Alignment.End)) {
                Icon(Icons.Default.PlaylistAdd, null)
                Spacer(Modifier.width(6.dp))
                Text("Save as playlist")
            }
        }
    }
}

@Composable
private fun RankRow(
    item: RankedSong,
    isCurrent: Boolean,
    onClick: () -> Unit,
    onReset: () -> Unit,
    actions: SongActionsViewModel
) {
    val (badgeBg, badgeFg) = when (item.rank) {
        1 -> Color(0xFFFFC94D) to Color(0xFF3A2A00)
        2 -> Color(0xFFD5DBE8) to Color(0xFF1F2430)
        3 -> Color(0xFFE0A070) to Color(0xFF3A1F08)
        else -> MaterialTheme.colorScheme.surfaceContainerHigh to MaterialTheme.colorScheme.onSurfaceVariant
    }
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(start = 20.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier.size(34.dp).clip(CircleShape).background(badgeBg),
            contentAlignment = Alignment.Center
        ) {
            Text("${item.rank}", color = badgeFg, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
        }
        Spacer(Modifier.width(12.dp))
        AlbumArt(item.song, Modifier.size(48.dp), cornerRadius = 12.dp)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                item.song.title, maxLines = 1, overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
            )
            Text(
                item.song.artist, maxLines = 1, overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.width(8.dp))
        Column(horizontalAlignment = Alignment.End) {
            Text(
                "${item.points} pts", fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.titleSmall
            )
            Text(
                "${item.listens}× · ${item.completions} ✓",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        SongMenuButton(item.song, actions) { close ->
            HorizontalDivider()
            DropdownMenuItem(
                text = { Text("Reset points") },
                onClick = { close(); onReset() }
            )
        }
    }
}

@Composable
private fun EmptyRanking() {
    Column(
        Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Default.EmojiEvents, null,
            Modifier.size(64.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(16.dp))
        Text("No ranking yet", style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text(
            "Listen to your music to earn points:\n+1 for each listen\n+2 when you repeat a song\n+2 bonus when you finish a song\n\n" +
                "A song counts as listened after 30 seconds (or half of a short track).",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}
