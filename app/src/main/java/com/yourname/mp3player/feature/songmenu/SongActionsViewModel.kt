package com.yourname.mp3player.feature.songmenu

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.yourname.mp3player.App
import com.yourname.mp3player.domain.PlayerController
import com.yourname.mp3player.domain.RankedSong
import com.yourname.mp3player.domain.Song
import com.yourname.mp3player.domain.UserDataRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Everything you can do to a single song. Shared by all screens that show songs. */
class SongActionsViewModel(
    private val player: PlayerController,
    private val userData: UserDataRepository,
    val ranked: StateFlow<List<RankedSong>>
) : ViewModel() {

    val favorites: StateFlow<Set<Long>> = userData.favorites
        .map { it.toSet() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    val playlists = userData.playlists

    fun toggleFavorite(song: Song) {
        viewModelScope.launch {
            userData.setFavorite(song.id, song.id !in userData.favorites.value)
        }
    }

    fun hide(song: Song) { viewModelScope.launch { userData.hide(song.id) } }
    fun playNext(song: Song) = player.playNext(song)
    fun addToQueue(song: Song) = player.addToQueue(song)

    fun addToPlaylist(playlistId: Long, song: Song) {
        viewModelScope.launch { userData.addToPlaylist(playlistId, listOf(song.id)) }
    }

    fun createPlaylistWith(name: String, song: Song) {
        viewModelScope.launch {
            val id = userData.createPlaylist(name)
            userData.addToPlaylist(id, listOf(song.id))
        }
    }
}

@Composable
fun rememberSongActions(): SongActionsViewModel {
    val app = LocalContext.current.applicationContext as App
    return viewModel(
        factory = viewModelFactory {
            initializer {
                SongActionsViewModel(
                    app.container.playerController,
                    app.container.userData,
                    app.container.rankedSongs
                )
            }
        }
    )
}
