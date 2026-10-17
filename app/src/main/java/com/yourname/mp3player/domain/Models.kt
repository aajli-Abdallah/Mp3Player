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
    val lastPlayedAt: Long,
    val completions: Int = 0
)

/** A song with its position in the ranking (1 = best). */
data class RankedSong(
    val rank: Int,
    val song: Song,
    val points: Int,
    val listens: Int,
    val completions: Int = 0
)

/** Emitted by the player each time a song has been genuinely listened to. */
enum class ListenType { LISTEN, COMPLETION }

data class ListenEvent(val song: Song, val type: ListenType = ListenType.LISTEN)

data class Playlist(val id: Long, val name: String, val songIds: List<Long>)

enum class ThemeMode(val label: String) {
    SYSTEM("System"), LIGHT("Light"), DARK("Dark"), AMOLED("AMOLED black")
}

enum class DedupeMode(val label: String, val hint: String) {
    TITLE("By title", "Same title = one song, even if the artists differ"),
    TITLE_ARTIST("Title + artist", "Same title AND same artist = one song"),
    OFF("Off", "Show every file")
}
