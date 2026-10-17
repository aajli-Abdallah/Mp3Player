package com.yourname.mp3player.feature.playlists

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
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
import com.yourname.mp3player.domain.Song
import com.yourname.mp3player.feature.player.formatTime
import com.yourname.mp3player.feature.player.rememberPlayerViewModel
import com.yourname.mp3player.feature.songmenu.SongActionsViewModel
import com.yourname.mp3player.feature.songmenu.SongMenuButton
import com.yourname.mp3player.feature.songmenu.rememberSongActions
import androidx.compose.ui.graphics.vector.ImageVector

@Composable
fun PlaylistsScreen() {
    val app = LocalContext.current.applicationContext as App
    val vm: PlaylistsViewModel = viewModel(
        factory = viewModelFactory {
            initializer {
                PlaylistsViewModel(
                    app.container.userData,
                    app.container.musicRepository,
                    app.container.playerController
                )
            }
        }
    )
    var openId by rememberSaveable { mutableStateOf<Long?>(null) }
    BackHandler(enabled = openId != null) { openId = null }

    val id = openId
    if (id == null) PlaylistList(vm, onOpen = { openId = it })
    else PlaylistDetail(id, vm, onBack = { openId = null })
}

// ------------------------------------------------------------------ list

@Composable
private fun PlaylistList(vm: PlaylistsViewModel, onOpen: (Long) -> Unit) {
    val playlists by vm.playlists.collectAsState()
    val favs by vm.favorites.collectAsState()
    var showCreate by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf("") }

    Column(Modifier.fillMaxSize()) {
        Column(Modifier.padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 12.dp)) {
            Text(
                "Playlists",
                style = MaterialTheme.typography.headlineLarge.copy(
                    brush = Brush.linearGradient(
                        listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary)
                    ),
                    fontWeight = FontWeight.ExtraBold
                )
            )
            Text(
                "${playlists.size} playlists · ${favs.size} favorites",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        LazyColumn(Modifier.fillMaxSize()) {
            item {
                PlaylistRow(Icons.Default.Favorite, "Favorites", "${favs.size} songs", special = true) {
                    onOpen(FAVORITES_ID)
                }
            }
            itemsIndexed(playlists, key = { _, p -> p.id }) { _, p ->
                PlaylistRow(Icons.Default.QueueMusic, p.name, "${p.songIds.size} songs", special = false) {
                    onOpen(p.id)
                }
            }
            item {
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = { newName = ""; showCreate = true },
                    modifier = Modifier.padding(horizontal = 20.dp).fillMaxWidth()
                ) {
                    Icon(Icons.Default.Add, null)
                    Spacer(Modifier.width(8.dp))
                    Text("New playlist")
                }
                if (playlists.isEmpty()) {
                    Text(
                        "Create playlists, then add songs from the ⋮ menu of any song.",
                        modifier = Modifier.padding(20.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }

    if (showCreate) {
        AlertDialog(
            onDismissRequest = { showCreate = false },
            title = { Text("New playlist") },
            text = {
                OutlinedTextField(
                    value = newName, onValueChange = { newName = it },
                    label = { Text("Name") }, singleLine = true
                )
            },
            confirmButton = {
                TextButton(
                    enabled = newName.isNotBlank(),
                    onClick = { vm.create(newName); showCreate = false }
                ) { Text("Create") }
            },
            dismissButton = { TextButton(onClick = { showCreate = false }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun PlaylistRow(icon: ImageVector, name: String, subtitle: String, special: Boolean, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 20.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(
                    if (special) Brush.linearGradient(
                        listOf(MaterialTheme.colorScheme.secondary, MaterialTheme.colorScheme.primary)
                    ) else Brush.linearGradient(
                        listOf(MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.secondaryContainer)
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = if (special) Color.White else MaterialTheme.colorScheme.onPrimaryContainer)
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(
                name, maxLines = 1, overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium
            )
            Text(
                subtitle, style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// ---------------------------------------------------------------- detail

@Composable
private fun PlaylistDetail(id: Long, vm: PlaylistsViewModel, onBack: () -> Unit) {
    val playlists by vm.playlists.collectAsState()
    val favIds by vm.favorites.collectAsState()
    val byId by vm.songsById.collectAsState()
    val actions = rememberSongActions()
    val playback by rememberPlayerViewModel().state.collectAsState()

    val isFav = id == FAVORITES_ID
    val playlist = playlists.firstOrNull { it.id == id }
    val ids = if (isFav) favIds else (playlist?.songIds ?: emptyList())
    val songs = remember(ids, byId) { ids.mapNotNull { byId[it] } }
    val title = if (isFav) "Favorites" else (playlist?.name ?: "")

    var edit by remember { mutableStateOf(false) }
    var menu by remember { mutableStateOf(false) }
    var showRename by remember { mutableStateOf(false) }
    var showDelete by remember { mutableStateOf(false) }
    var renameText by remember { mutableStateOf("") }

    // playlist deleted (or restored away): go back to the list
    LaunchedEffect(isFav, playlist == null) { if (!isFav && playlist == null) onBack() }

    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(start = 4.dp, end = 4.dp, top = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
            Column(Modifier.weight(1f)) {
                Text(
                    title, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold
                )
                Text(
                    "${songs.size} songs", style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (songs.isNotEmpty()) {
                IconButton(onClick = { edit = !edit }) {
                    Icon(if (edit) Icons.Default.Check else Icons.Default.Edit, if (edit) "Done" else "Edit")
                }
            }
            if (!isFav) {
                Box {
                    IconButton(onClick = { menu = true }) { Icon(Icons.Default.MoreVert, "More") }
                    DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                        DropdownMenuItem(
                            text = { Text("Rename") },
                            onClick = { menu = false; renameText = title; showRename = true }
                        )
                        DropdownMenuItem(
                            text = { Text("Delete playlist") },
                            onClick = { menu = false; showDelete = true }
                        )
                    }
                }
            }
        }

        if (songs.isEmpty()) {
            Column(
                Modifier.fillMaxSize().padding(32.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Nothing here yet", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(8.dp))
                Text(
                    if (isFav) "Tap the heart on a song to add it to your favorites."
                    else "Use the ⋮ menu of any song and choose \"Add to playlist\".",
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(onClick = { vm.play(songs, 0, false) }, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.PlayArrow, null)
                    Spacer(Modifier.width(6.dp))
                    Text("Play")
                }
                OutlinedButton(onClick = { vm.play(songs, 0, true) }, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.Shuffle, null)
                    Spacer(Modifier.width(6.dp))
                    Text("Shuffle")
                }
            }
            LazyColumn(Modifier.fillMaxSize()) {
                itemsIndexed(songs, key = { _, s -> s.id }) { index, song ->
                    DetailRow(
                        song = song,
                        actions = actions,
                        isCurrent = playback.currentSong?.id == song.id,
                        edit = edit,
                        canReorder = !isFav,
                        canUp = index > 0,
                        canDown = index < songs.lastIndex,
                        onClick = { vm.play(songs, index, null) },
                        onUp = { vm.swap(id, song.id, songs[index - 1].id) },
                        onDown = { vm.swap(id, song.id, songs[index + 1].id) },
                        onRemove = { vm.remove(id, song.id) }
                    )
                }
            }
        }
    }

    if (showRename) {
        AlertDialog(
            onDismissRequest = { showRename = false },
            title = { Text("Rename playlist") },
            text = {
                OutlinedTextField(value = renameText, onValueChange = { renameText = it }, singleLine = true)
            },
            confirmButton = {
                TextButton(
                    enabled = renameText.isNotBlank(),
                    onClick = { vm.rename(id, renameText); showRename = false }
                ) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { showRename = false }) { Text("Cancel") } }
        )
    }
    if (showDelete) {
        AlertDialog(
            onDismissRequest = { showDelete = false },
            title = { Text("Delete \"$title\"?") },
            text = { Text("The songs themselves are not deleted, only the playlist.") },
            confirmButton = {
                TextButton(onClick = { vm.delete(id); showDelete = false }) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { showDelete = false }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun DetailRow(
    song: Song,
    actions: SongActionsViewModel,
    isCurrent: Boolean,
    edit: Boolean,
    canReorder: Boolean,
    canUp: Boolean,
    canDown: Boolean,
    onClick: () -> Unit,
    onUp: () -> Unit,
    onDown: () -> Unit,
    onRemove: () -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(enabled = !edit, onClick = onClick)
            .padding(start = 20.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AlbumArt(song, Modifier.size(48.dp), cornerRadius = 12.dp)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                song.title, maxLines = 1, overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
            )
            Text(
                song.artist, maxLines = 1, overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (edit) {
            if (canReorder) {
                IconButton(onClick = onUp, enabled = canUp, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.KeyboardArrowUp, "Move up")
                }
                IconButton(onClick = onDown, enabled = canDown, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.KeyboardArrowDown, "Move down")
                }
            }
            IconButton(onClick = onRemove, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Default.Close, "Remove", tint = MaterialTheme.colorScheme.error)
            }
        } else {
            Text(
                formatTime(song.durationMs), style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.width(4.dp))
            SongMenuButton(song, actions)
        }
    }
}
