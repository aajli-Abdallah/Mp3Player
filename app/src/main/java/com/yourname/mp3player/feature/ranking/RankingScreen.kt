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
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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

@Composable
fun RankingScreen() {
    val app = LocalContext.current.applicationContext as App
    val vm: RankingViewModel = viewModel(
        factory = viewModelFactory {
            initializer {
                RankingViewModel(
                    app.container.musicRepository,
                    app.container.rankingRepository,
                    app.container.playerController
                )
            }
        }
    )
    val playerVm = rememberPlayerViewModel()

    val ranked by vm.ranked.collectAsState()
    val selected by vm.selected.collectAsState()
    val option by vm.option.collectAsState()
    val playback by playerVm.state.collectAsState()

    Column(Modifier.fillMaxSize()) {
        Column(Modifier.padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 12.dp)) {
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

        if (ranked.isEmpty()) {
            EmptyRanking()
        } else {
            SmartPlaylistCard(
                option = option,
                count = selected.size,
                points = selected.sumOf { it.points },
                onSelect = vm::select,
                onPlay = { vm.playAll(shuffle = false) },
                onShuffle = { vm.playAll(shuffle = true) }
            )
            Spacer(Modifier.height(8.dp))
            LazyColumn(Modifier.fillMaxSize()) {
                itemsIndexed(selected, key = { _, r -> r.song.id }) { index, item ->
                    RankRow(
                        item = item,
                        isCurrent = playback.currentSong?.id == item.song.id,
                        onClick = { vm.playFrom(index) }
                    )
                }
            }
        }
    }
}

@Composable
private fun SmartPlaylistCard(
    option: TopOption,
    count: Int,
    points: Int,
    onSelect: (TopOption) -> Unit,
    onPlay: () -> Unit,
    onShuffle: () -> Unit
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
        }
    }
}

@Composable
private fun RankRow(item: RankedSong, isCurrent: Boolean, onClick: () -> Unit) {
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
            .padding(horizontal = 20.dp, vertical = 8.dp),
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
        Spacer(Modifier.width(12.dp))
        Column(horizontalAlignment = Alignment.End) {
            Text(
                "${item.points} pts", fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.titleSmall
            )
            Text(
                "${item.listens}× played",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
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
            "Listen to your music to earn points:\n+1 for each listen\n+2 when you repeat a song\n\n" +
                "A song counts as listened after 30 seconds (or half of a short track).",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}
