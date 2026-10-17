package com.yourname.mp3player.feature.playlists

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yourname.mp3player.domain.MusicRepository
import com.yourname.mp3player.domain.PlayerController
import com.yourname.mp3player.domain.Song
import com.yourname.mp3player.domain.UserDataRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Pseudo playlist id for the automatic "Favorites" playlist. */
const val FAVORITES_ID = -1L

class PlaylistsViewModel(
    private val userData: UserDataRepository,
    music: MusicRepository,
    private val player: PlayerController
) : ViewModel() {

    val playlists = userData.playlists
    val favorites = userData.favorites

    /** Songs currently visible in the library (not hidden), by id. */
    val songsById: StateFlow<Map<Long, Song>> = music.getSongs()
        .map { list -> list.associateBy { it.id } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    fun create(name: String) { viewModelScope.launch { userData.createPlaylist(name.trim()) } }
    fun rename(id: Long, name: String) { viewModelScope.launch { userData.renamePlaylist(id, name.trim()) } }
    fun delete(id: Long) { viewModelScope.launch { userData.deletePlaylist(id) } }

    fun remove(id: Long, songId: Long) {
        viewModelScope.launch {
            if (id == FAVORITES_ID) userData.setFavorite(songId, false)
            else userData.removeFromPlaylist(id, songId)
        }
    }

    /** Swaps two songs of a playlist (works on the FULL list, so hidden songs are kept). */
    fun swap(id: Long, songA: Long, songB: Long) {
        val ids = playlists.value.firstOrNull { it.id == id }?.songIds?.toMutableList() ?: return
        val i = ids.indexOf(songA)
        val j = ids.indexOf(songB)
        if (i < 0 || j < 0) return
        ids[i] = songB
        ids[j] = songA
        viewModelScope.launch { userData.setPlaylistOrder(id, ids) }
    }

    fun play(songs: List<Song>, index: Int, shuffle: Boolean?) {
        if (songs.isEmpty()) return
        if (shuffle != null) player.setShuffle(shuffle)
        player.play(songs, index)
    }
}
