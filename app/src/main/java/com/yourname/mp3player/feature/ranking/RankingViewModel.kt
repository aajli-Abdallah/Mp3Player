package com.yourname.mp3player.feature.ranking

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yourname.mp3player.domain.PlayerController
import com.yourname.mp3player.domain.RankedSong
import com.yourname.mp3player.domain.RankingRepository
import com.yourname.mp3player.domain.UserDataRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** The choices of the "Top Ranked" smart playlist. */
enum class TopOption(val label: String, val count: Int?) {
    TOP_10("Top 10", 10),
    TOP_25("Top 25", 25),
    TOP_50("Top 50", 50),
    TOP_100("Top 100", 100),
    ALL("All", null),
    CUSTOM("Custom", null)
}

class RankingViewModel(
    val ranked: StateFlow<List<RankedSong>>,
    private val rankingRepo: RankingRepository,
    private val player: PlayerController,
    private val userData: UserDataRepository
) : ViewModel() {

    private val _option = MutableStateFlow(TopOption.TOP_10)
    val option = _option.asStateFlow()

    private val _custom = MutableStateFlow(20)

    /** Custom N, always kept between 1 and the number of ranked songs. */
    val custom: StateFlow<Int> = combine(_custom, ranked) { n, list ->
        n.coerceIn(1, maxOf(1, list.size))
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 20)

    /** The songs of the playlist for the chosen option, in rank order. */
    val selected: StateFlow<List<RankedSong>> =
        combine(ranked, _option, custom) { list, opt, n ->
            when (opt) {
                TopOption.ALL -> list
                TopOption.CUSTOM -> list.take(n)
                else -> list.take(opt.count ?: list.size)
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun select(option: TopOption) { _option.value = option }
    fun setCustom(n: Int) { _custom.value = n }

    fun playAll(shuffle: Boolean) {
        val songs = selected.value.map { it.song }
        if (songs.isEmpty()) return
        player.setShuffle(shuffle)
        player.play(songs, 0)
    }

    fun playFrom(index: Int) {
        val songs = selected.value.map { it.song }
        if (songs.isEmpty()) return
        player.play(songs, index)
    }

    /** Saves the currently selected Top N as a real, editable playlist (a snapshot). */
    fun saveAsPlaylist(name: String) {
        val ids = selected.value.map { it.song.id }
        if (ids.isEmpty() || name.isBlank()) return
        viewModelScope.launch {
            val id = userData.createPlaylist(name.trim())
            userData.addToPlaylist(id, ids)
        }
    }

    fun resetSong(songId: Long) { viewModelScope.launch { rankingRepo.reset(songId) } }
    fun resetAll() { viewModelScope.launch { rankingRepo.resetAll() } }
}
