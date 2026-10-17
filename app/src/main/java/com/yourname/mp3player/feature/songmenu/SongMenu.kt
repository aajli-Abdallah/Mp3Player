package com.yourname.mp3player.feature.songmenu

import android.content.Context
import android.media.MediaMetadataRetriever
import android.os.Build
import android.provider.MediaStore
import android.widget.Toast
import androidx.compose.runtime.produceState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.DateFormat
import java.util.Date
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.yourname.mp3player.domain.Song
import com.yourname.mp3player.feature.player.formatTime

/** The "⋮" button of a song: play next, queue, playlist, favorite, info, hide. */
@Composable
fun SongMenuButton(
    song: Song,
    actions: SongActionsViewModel,
    modifier: Modifier = Modifier,
    /** Optional extra menu entries (e.g. "Reset points" on the Ranking tab). */
    extra: (@Composable (close: () -> Unit) -> Unit)? = null
) {
    val context = LocalContext.current
    var menu by remember { mutableStateOf(false) }
    var showPlaylists by remember { mutableStateOf(false) }
    var showInfo by remember { mutableStateOf(false) }
    val favs by actions.favorites.collectAsState()
    val isFav = song.id in favs

    fun toast(msg: String) = Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()

    Box(modifier) {
        IconButton(onClick = { menu = true }, modifier = Modifier.size(36.dp)) {
            Icon(Icons.Default.MoreVert, "More", Modifier.size(20.dp))
        }
        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
            DropdownMenuItem(
                text = { Text("Play next") },
                onClick = { menu = false; actions.playNext(song); toast("Will play next") }
            )
            DropdownMenuItem(
                text = { Text("Add to queue") },
                onClick = { menu = false; actions.addToQueue(song); toast("Added to queue") }
            )
            DropdownMenuItem(
                text = { Text("Add to playlist…") },
                onClick = { menu = false; showPlaylists = true }
            )
            DropdownMenuItem(
                text = { Text(if (isFav) "Remove from favorites" else "Add to favorites") },
                onClick = { menu = false; actions.toggleFavorite(song) }
            )
            DropdownMenuItem(
                text = { Text("Song info") },
                onClick = { menu = false; showInfo = true }
            )
            DropdownMenuItem(
                text = { Text("Hide from library") },
                onClick = { menu = false; actions.hide(song); toast("Hidden. Undo in Settings.") }
            )
            extra?.invoke { menu = false }
        }
    }

    if (showPlaylists) {
        AddToPlaylistDialog(
            song = song,
            actions = actions,
            onDone = { msg -> showPlaylists = false; if (msg != null) toast(msg) }
        )
    }
    if (showInfo) SongInfoDialog(song, actions) { showInfo = false }
}

@Composable
private fun AddToPlaylistDialog(song: Song, actions: SongActionsViewModel, onDone: (String?) -> Unit) {
    val playlists by actions.playlists.collectAsState()
    var newName by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = { onDone(null) },
        title = { Text("Add to playlist") },
        text = {
            Column {
                Column(Modifier.heightIn(max = 240.dp).verticalScroll(rememberScrollState())) {
                    if (playlists.isEmpty()) {
                        Text(
                            "No playlist yet. Create the first one below.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    playlists.forEach { pl ->
                        Text(
                            pl.name,
                            Modifier.fillMaxWidth()
                                .clickable { actions.addToPlaylist(pl.id, song); onDone("Added to ${pl.name}") }
                                .padding(vertical = 12.dp),
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
                Spacer(Modifier.size(8.dp))
                HorizontalDivider()
                Spacer(Modifier.size(8.dp))
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    label = { Text("New playlist") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = newName.isNotBlank(),
                onClick = {
                    actions.createPlaylistWith(newName.trim(), song)
                    onDone("Added to ${newName.trim()}")
                }
            ) { Text("Create & add") }
        },
        dismissButton = { TextButton(onClick = { onDone(null) }) { Text("Cancel") } }
    )
}

@Composable
private fun SongInfoDialog(song: Song, actions: SongActionsViewModel, onDismiss: () -> Unit) {
    val ranked by actions.ranked.collectAsState()
    val rank = remember(ranked, song.id) { ranked.firstOrNull { it.song.id == song.id } }
    val context = LocalContext.current
    val tech by produceState<TechInfo?>(initialValue = null, key1 = song.id) {
        value = withContext(Dispatchers.IO) { loadTechInfo(context, song) }
    }
    val loaded = tech != null
    fun txt(v: String?) = v ?: if (loaded) "—" else "…"

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } },
        title = { Text(song.title, maxLines = 2) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                InfoLine("Artist", song.artist)
                InfoLine("Album", song.album.ifBlank { "Unknown album" })
                InfoLine("Duration", formatTime(song.durationMs))
                InfoLine(
                    "Ranking",
                    if (rank != null) "#${rank.rank} · ${rank.points} pts · ${rank.listens} listens · ${rank.completions} completed"
                    else "Not ranked yet"
                )
                InfoLine("File", txt(tech?.fileName))
                InfoLine("Size", txt(tech?.sizeBytes?.let { "%.1f MB".format(it / 1_048_576.0) }))
                InfoLine(
                    "Audio (bitrate · sample rate)",
                    txt(
                        listOfNotNull(
                            tech?.bitrateKbps?.let { "$it kbps" },
                            tech?.sampleRateHz?.let { "%.1f kHz".format(it / 1000.0) }
                        ).joinToString(" · ").ifBlank { null }
                    )
                )
                InfoLine(
                    "Added",
                    txt(tech?.addedAtMs?.takeIf { it > 0 }?.let { DateFormat.getDateInstance().format(Date(it)) })
                )
            }
        }
    )
}

@Composable
private fun InfoLine(label: String, value: String) {
    Column(Modifier.padding(vertical = 4.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyLarge)
    }
}

/** Technical details read from MediaStore + the file itself (loaded off the main thread). */
private data class TechInfo(
    val fileName: String?,
    val sizeBytes: Long?,
    val bitrateKbps: Int?,
    val sampleRateHz: Int?,   // only available on Android 12+ (API 31)
    val addedAtMs: Long?
)

private fun loadTechInfo(context: Context, song: Song): TechInfo {
    var name: String? = null
    var size: Long? = null
    var added: Long? = null
    runCatching {
        context.contentResolver.query(
            song.uri,
            arrayOf(
                MediaStore.MediaColumns.DISPLAY_NAME,
                MediaStore.MediaColumns.SIZE,
                MediaStore.MediaColumns.DATE_ADDED
            ),
            null, null, null
        )?.use { c ->
            if (c.moveToFirst()) {
                name = c.getString(0)
                size = c.getLong(1)
                added = c.getLong(2) * 1000L
            }
        }
    }
    var bitrate: Int? = null
    var rate: Int? = null
    runCatching {
        val r = MediaMetadataRetriever()
        try {
            r.setDataSource(context, song.uri)
            bitrate = r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE)
                ?.toIntOrNull()?.div(1000)
            if (Build.VERSION.SDK_INT >= 31) {
                rate = r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_SAMPLERATE)?.toIntOrNull()
            }
        } finally {
            r.release()
        }
    }
    return TechInfo(name, size, bitrate, rate, added)
}
