package com.yourname.mp3player.domain

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

interface MusicRepository {
    fun getSongs(): Flow<List<Song>>
    suspend fun refresh()
}

/** The UI talks to the player ONLY through this interface. */
interface PlayerController {
    val state: StateFlow<PlaybackState>

    /** Emits once each time the current song counts as "listened". */
    val events: SharedFlow<ListenEvent>

    fun play(songs: List<Song>, startIndex: Int)
    fun togglePlayPause()
    fun next()
    fun previous()
    fun seekTo(positionMs: Long)
    fun setShuffle(enabled: Boolean)
    fun setRepeat(mode: RepeatMode)
}

interface RankingRepository {
    val stats: StateFlow<Map<Long, SongStats>>
    suspend fun addPoints(songId: Long, points: Int)
}
