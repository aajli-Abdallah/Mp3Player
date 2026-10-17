package com.yourname.mp3player.feature.library

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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.MusicOff
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.yourname.mp3player.feature.player.formatTime
import com.yourname.mp3player.feature.player.rememberPlayerViewModel
import com.yourname.mp3player.feature.songmenu.SongMenuButton
import com.yourname.mp3player.feature.songmenu.rememberSongActions

@Composable
fun LibraryScreen(onOpenSettings: () -> Unit) {
    val app = LocalContext.current.applicationContext as App
    val vm: LibraryViewModel = viewModel(
        factory = viewModelFactory {
            initializer {
                LibraryViewModel(
                    app.container.musicRepository,
                    app.container.playerController,
                    app.container.rankedSongs
                )
            }
        }
    )
    val playerVm = rememberPlayerViewModel()
    val actions = rememberSongActions()

    val songs by vm.songs.collectAsState()
    val total by vm.library.collectAsState()
    val query by vm.query.collectAsState()
    val loading by vm.loading.collectAsState()
    val ranks by vm.ranks.collectAsState()
    val playback by playerVm.state.collectAsState()
    val favs by actions.favorites.collectAsState()

    Column(Modifier.fillMaxSize()) {
        Header(count = total.size, onRefresh = vm::refresh, onSettings = onOpenSettings)
        SearchField(query, vm::onQueryChange)
        if (songs.isNotEmpty()) {
            Spacer(Modifier.height(10.dp))
            Button(
                onClick = vm::shufflePlay,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)
            ) {
                Icon(Icons.Default.Shuffle, null)
                Spacer(Modifier.width(8.dp))
                Text(if (query.isBlank()) "Shuffle play" else "Shuffle results")
            }
        }
        Spacer(Modifier.height(8.dp))

        when {
            loading && total.isEmpty() -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                CircularProgressIndicator()
            }
            songs.isEmpty() -> EmptyState(searching = query.isNotBlank(), query = query, onRescan = vm::refresh)
            else -> LazyColumn(Modifier.fillMaxSize()) {
                itemsIndexed(songs, key = { _, s -> s.id }) { index, song ->
                    val isCurrent = playback.currentSong?.id == song.id
                    val accent = if (isCurrent) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurface
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable { vm.onSongClick(index) }
                            .padding(start = 20.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AlbumArt(song, Modifier.size(52.dp), cornerRadius = 12.dp)
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                song.title, color = accent, maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    song.artist, maxLines = 1, overflow = TextOverflow.Ellipsis,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.weight(1f, fill = false)
                                )
                                ranks[song.id]?.let { rk ->
                                    Text(
                                        "  ·  #${rk.rank} · ${rk.points} pts",
                                        maxLines = 1,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                        Spacer(Modifier.width(8.dp))
                        if (song.id in favs) {
                            Icon(
                                Icons.Default.Favorite, "Favorite",
                                Modifier.size(16.dp), tint = MaterialTheme.colorScheme.secondary
                            )
                            Spacer(Modifier.width(8.dp))
                        }
                        if (isCurrent && playback.isPlaying) {
                            Icon(Icons.Default.GraphicEq, null, tint = MaterialTheme.colorScheme.primary)
                        } else {
                            Text(
                                formatTime(song.durationMs),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        SongMenuButton(song, actions)
                    }
                }
            }
        }
    }
}

@Composable
private fun Header(count: Int, onRefresh: () -> Unit, onSettings: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp, top = 16.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                "Sonora",
                style = MaterialTheme.typography.headlineLarge.copy(
                    brush = Brush.linearGradient(
                        listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary)
                    ),
                    fontWeight = FontWeight.ExtraBold
                )
            )
            Text(
                if (count == 1) "1 song" else "$count songs",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        IconButton(onClick = onRefresh) { Icon(Icons.Default.Refresh, "Rescan") }
        IconButton(onClick = onSettings) { Icon(Icons.Default.Settings, "Settings") }
    }
}

@Composable
private fun SearchField(query: String, onChange: (String) -> Unit) {
    TextField(
        value = query,
        onValueChange = onChange,
        singleLine = true,
        placeholder = { Text("Search songs, artists, albums") },
        leadingIcon = { Icon(Icons.Default.Search, null) },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onChange("") }) { Icon(Icons.Default.Close, "Clear") }
            }
        },
        shape = RoundedCornerShape(28.dp),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent
        ),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)
    )
}

@Composable
private fun EmptyState(searching: Boolean, query: String, onRescan: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Default.MusicOff, null,
            Modifier.size(64.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(16.dp))
        Text(
            if (searching) "No results for \"$query\"" else "No MP3 files found",
            style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center
        )
        if (!searching) {
            Spacer(Modifier.height(8.dp))
            Text(
                "Put some .mp3 files in your Music or Download folder, then rescan.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(20.dp))
            Button(onClick = onRescan) { Text("Rescan") }
        }
    }
}
