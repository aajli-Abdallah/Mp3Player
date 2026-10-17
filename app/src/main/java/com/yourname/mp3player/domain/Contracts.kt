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
    fun playNext(song: Song)
    fun addToQueue(song: Song)
    fun togglePlayPause()
    fun next()
    fun previous()
    fun seekTo(positionMs: Long)
    fun setShuffle(enabled: Boolean)
    fun setRepeat(mode: RepeatMode)
}

interface RankingRepository {
    val stats: StateFlow<Map<Long, SongStats>>
    suspend fun recordListen(songId: Long, points: Int)
    suspend fun recordCompletion(songId: Long, points: Int)
    suspend fun reset(songId: Long)
    suspend fun resetAll()
}

/** Everything the user creates: favorites, hidden songs, playlists. */
interface UserDataRepository {
    val favorites: StateFlow<List<Long>>        // newest first
    val hidden: StateFlow<Set<Long>>
    val playlists: StateFlow<List<Playlist>>

    suspend fun setFavorite(songId: Long, favorite: Boolean)
    suspend fun hide(songId: Long)
    suspend fun unhide(songId: Long)
    suspend fun unhideAll()
    suspend fun createPlaylist(name: String): Long
    suspend fun renamePlaylist(id: Long, name: String)
    suspend fun deletePlaylist(id: Long)
    suspend fun addToPlaylist(playlistId: Long, songIds: List<Long>)
    suspend fun removeFromPlaylist(playlistId: Long, songId: Long)
    suspend fun setPlaylistOrder(playlistId: Long, songIds: List<Long>)
}
