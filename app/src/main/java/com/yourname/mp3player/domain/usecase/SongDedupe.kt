package com.yourname.mp3player.domain.usecase

import com.yourname.mp3player.domain.DedupeMode
import com.yourname.mp3player.domain.Song
import com.yourname.mp3player.domain.SongStats

/** Keeps ONE song per group. Winner: most points, then longest, then oldest (lowest id). */
object SongDedupe {
    private val copySuffix = Regex("""\s*\(\d+\)$""")   // "Song (1)" -> "Song"
    private val spaces = Regex("\\s+")

    fun key(title: String): String =
        title.trim().lowercase().replace(copySuffix, "").replace(spaces, " ").trim()

    /** Stable identity of a song across devices/reinstalls (used by backup). */
    fun fingerprint(title: String, artist: String): String =
        key(title) + "|" + artist.trim().lowercase()

    private fun groupKey(song: Song, mode: DedupeMode): String = when (mode) {
        DedupeMode.TITLE -> key(song.title)
        DedupeMode.TITLE_ARTIST -> fingerprint(song.title, song.artist)
        DedupeMode.OFF -> ""
    }

    fun dedupe(songs: List<Song>, stats: Map<Long, SongStats>, mode: DedupeMode): List<Song> {
        if (mode == DedupeMode.OFF) return songs
        val winners: Map<String, Long> = songs.groupBy { groupKey(it, mode) }.mapValues { (_, group) ->
            group.maxWithOrNull(
                compareBy<Song>({ stats[it.id]?.points ?: 0 }, { it.durationMs }, { -it.id })
            )!!.id
        }
        return songs.filter { key(it.title).isBlank() || winners[groupKey(it, mode)] == it.id }
    }
}
