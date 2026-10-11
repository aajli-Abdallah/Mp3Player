package com.yourname.mp3player.feature.ranking

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yourname.mp3player.domain.MusicRepository
import com.yourname.mp3player.domain.PlayerController
import com.yourname.mp3player.domain.RankedSong
import com.yourname.mp3player.domain.RankingRepository
import com.yourname.mp3player.domain.usecase.Ranking
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

/** The choices of the "Top Ranked" smart playlist. `count == null` means everything. */
enum class TopOption(val label: String, val count: Int?) {
    TOP_10("Top 10", 10),
    TOP_25("Top 25", 25),
    TOP_50("Top 50", 50),
    TOP_100("Top 100", 100),
    ALL("All", null)
}

class RankingViewModel(
    musicRepo: MusicRepository,
    rankingRepo: RankingRepository,
    private val player: PlayerController
) : ViewModel() {

    val ranked: StateFlow<List<RankedSong>> =
        combine(musicRepo.getSongs(), rankingRepo.stats) { songs, stats -> Ranking.build(songs, stats) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _option = MutableStateFlow(TopOption.TOP_10)
    val option = _option.asStateFlow()

    /** The songs of the playlist for the chosen option, in rank order. */
    val selected: StateFlow<List<RankedSong>> =
        combine(ranked, _option) { list, opt -> opt.count?.let { list.take(it) } ?: list }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun select(option: TopOption) { _option.value = option }

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
}
