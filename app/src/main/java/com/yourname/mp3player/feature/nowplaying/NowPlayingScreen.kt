package com.yourname.mp3player.feature.nowplaying

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import com.yourname.mp3player.feature.songmenu.SongMenuButton
import com.yourname.mp3player.feature.songmenu.rememberSongActions
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import com.yourname.mp3player.domain.RankedSong
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.yourname.mp3player.core.ui.AlbumArt
import com.yourname.mp3player.domain.RepeatMode
import com.yourname.mp3player.feature.player.formatTime
import com.yourname.mp3player.feature.player.rememberPlayerViewModel

@Composable
fun NowPlayingScreen(onBack: () -> Unit) {
    val vm = rememberPlayerViewModel()
    val state by vm.state.collectAsState()
    val song = state.currentSong
    val ranked by vm.ranked.collectAsState()
    val rank = remember(ranked, song?.id) { ranked.firstOrNull { it.song.id == song?.id } }
    val actions = rememberSongActions()
    val favs by actions.favorites.collectAsState()
    val isFav = song != null && song.id in favs
    var dragging by remember { mutableStateOf<Float?>(null) }

    val duration = (song?.durationMs ?: 0L).coerceAtLeast(1L)
    val sliderValue = dragging ?: (state.positionMs.toFloat() / duration).coerceIn(0f, 1f)

    val active = MaterialTheme.colorScheme.primary
    val inactive = MaterialTheme.colorScheme.onSurfaceVariant

    Box(
        Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f),
                        MaterialTheme.colorScheme.background
                    )
                )
            )
    ) {
        Column(
            Modifier.fillMaxSize().systemBarsPadding().padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.KeyboardArrowDown, "Close", Modifier.size(32.dp))
                }
                Text(
                    "NOW PLAYING",
                    Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelLarge,
                    color = inactive
                )
                Spacer(Modifier.size(48.dp))
            }

            Spacer(Modifier.weight(1f))
            AlbumArt(
                song,
                Modifier.fillMaxWidth().widthIn(max = 380.dp).aspectRatio(1f),
                cornerRadius = 28.dp,
                iconSize = 96.dp,
                px = 720
            )
            Spacer(Modifier.weight(1f))

            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    song?.title ?: "", maxLines = 1, overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold
                )
                Text(
                    song?.artist ?: "", maxLines = 1, overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.titleMedium, color = inactive
                )
                Spacer(Modifier.height(10.dp))
                RankChip(rank)
            }
            if (song != null) {
                IconButton(onClick = { actions.toggleFavorite(song) }) {
                    Icon(
                        if (isFav) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        "Favorite",
                        tint = if (isFav) MaterialTheme.colorScheme.secondary else inactive
                    )
                }
                SongMenuButton(song, actions)
            }
            }

            Spacer(Modifier.height(12.dp))
            Slider(
                value = sliderValue,
                onValueChange = { dragging = it },
                onValueChangeFinished = {
                    dragging?.let { vm.seekTo((it * duration).toLong()) }
                    dragging = null
                }
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(formatTime((sliderValue * duration).toLong()), style = MaterialTheme.typography.bodySmall, color = inactive)
                Text(formatTime(song?.durationMs ?: 0L), style = MaterialTheme.typography.bodySmall, color = inactive)
            }

            Spacer(Modifier.height(16.dp))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = vm::toggleShuffle) {
                    Icon(Icons.Default.Shuffle, "Shuffle", tint = if (state.shuffle) active else inactive)
                }
                IconButton(onClick = vm::previous, modifier = Modifier.size(56.dp)) {
                    Icon(Icons.Default.SkipPrevious, "Previous", Modifier.size(36.dp))
                }
                FilledIconButton(onClick = vm::togglePlayPause, modifier = Modifier.size(76.dp)) {
                    Icon(
                        if (state.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        "Play/Pause", Modifier.size(42.dp)
                    )
                }
                IconButton(onClick = vm::next, modifier = Modifier.size(56.dp)) {
                    Icon(Icons.Default.SkipNext, "Next", Modifier.size(36.dp))
                }
                IconButton(onClick = vm::cycleRepeat) {
                    Icon(
                        if (state.repeat == RepeatMode.ONE) Icons.Default.RepeatOne else Icons.Default.Repeat,
                        "Repeat",
                        tint = if (state.repeat != RepeatMode.OFF) active else inactive
                    )
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun RankChip(rank: RankedSong?) {
    val text = if (rank != null) "#${rank.rank} · ${rank.points} pts · ${rank.listens}× played"
    else "Not ranked yet · 0 pts"
    Surface(
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Row(
            Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.EmojiEvents, null,
                Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.width(6.dp))
            Text(text, style = MaterialTheme.typography.labelMedium)
        }
    }
}
