package com.yourname.mp3player.domain

import android.net.Uri

data class Song(
    val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val durationMs: Long,
    val uri: Uri,
    val albumArtUri: Uri?
)

enum class RepeatMode { OFF, ALL, ONE }

data class PlaybackState(
    val currentSong: Song? = null,
    val isPlaying: Boolean = false,
    val positionMs: Long = 0,
    val queue: List<Song> = emptyList(),
    val shuffle: Boolean = false,
    val repeat: RepeatMode = RepeatMode.OFF
)

/** Persisted ranking data for one song. */
data class SongStats(
    val songId: Long,
    val points: Int,
    val listens: Int,
    val lastPlayedAt: Long
)

/** A song with its position in the ranking (1 = best). */
data class RankedSong(
    val rank: Int,
    val song: Song,
    val points: Int,
    val listens: Int
)

/** Emitted by the player each time a song has been genuinely listened to. */
data class ListenEvent(val song: Song)
