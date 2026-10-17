package com.yourname.mp3player.feature.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import com.yourname.mp3player.feature.library.LibraryScreen
import com.yourname.mp3player.feature.miniplayer.MiniPlayer
import com.yourname.mp3player.feature.player.rememberPlayerViewModel
import com.yourname.mp3player.feature.playlists.PlaylistsScreen
import com.yourname.mp3player.feature.ranking.RankingScreen

private enum class Tab(val label: String, val icon: ImageVector) {
    SONGS("Songs", Icons.Default.LibraryMusic),
    PLAYLISTS("Playlists", Icons.Default.QueueMusic),
    RANKING("Ranking", Icons.Default.EmojiEvents)
}

@Composable
fun HomeScreen(onOpenPlayer: () -> Unit, onOpenSettings: () -> Unit) {
    val playerVm = rememberPlayerViewModel()
    val playback by playerVm.state.collectAsState()
    var tab by rememberSaveable { mutableStateOf(Tab.SONGS) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            Column {
                if (playback.currentSong != null) {
                    MiniPlayer(
                        state = playback,
                        onOpen = onOpenPlayer,
                        onToggle = playerVm::togglePlayPause,
                        onNext = playerVm::next
                    )
                }
                NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainer) {
                    Tab.entries.forEach { t ->
                        NavigationBarItem(
                            selected = tab == t,
                            onClick = { tab = t },
                            icon = { Icon(t.icon, t.label) },
                            label = { Text(t.label) }
                        )
                    }
                }
            }
        }
    ) { padding ->
        Box(Modifier.padding(padding)) {
            when (tab) {
                Tab.SONGS -> LibraryScreen(onOpenSettings = onOpenSettings)
                Tab.PLAYLISTS -> PlaylistsScreen()
                Tab.RANKING -> RankingScreen()
            }
        }
    }
}
