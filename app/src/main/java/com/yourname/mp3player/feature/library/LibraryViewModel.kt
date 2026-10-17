package com.yourname.mp3player.feature.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yourname.mp3player.domain.MusicRepository
import com.yourname.mp3player.domain.PlayerController
import com.yourname.mp3player.domain.RankedSong
import com.yourname.mp3player.domain.Song
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class LibraryViewModel(
    private val repo: MusicRepository,
    private val player: PlayerController,
    rankedSongs: StateFlow<List<RankedSong>>
) : ViewModel() {

    val library: StateFlow<List<Song>> = repo.getSongs()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    /** song id -> rank info, to show points in the list. */
    val ranks: StateFlow<Map<Long, RankedSong>> = rankedSongs
        .map { list -> list.associateBy { it.song.id } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    private val _query = MutableStateFlow("")
    val query = _query.asStateFlow()

    private val _loading = MutableStateFlow(true)
    val loading = _loading.asStateFlow()

    val songs: StateFlow<List<Song>> = combine(library, _query) { list, q ->
        if (q.isBlank()) list
        else list.filter {
            it.title.contains(q, ignoreCase = true) ||
                it.artist.contains(q, ignoreCase = true) ||
                it.album.contains(q, ignoreCase = true)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init { refresh() }

    fun refresh() {
        viewModelScope.launch {
            _loading.value = true
            repo.refresh()
            _loading.value = false
        }
    }

    fun onQueryChange(q: String) { _query.value = q }
    fun onSongClick(index: Int) = player.play(songs.value, index)

    /** Plays the songs currently shown (respects the search) in random order, random first song. */
    fun shufflePlay() {
        val list = songs.value
        if (list.isEmpty()) return
        player.setShuffle(true)
        player.play(list, kotlin.random.Random.nextInt(list.size))
    }
}
