package com.yourname.mp3player.domain.usecase

import com.yourname.mp3player.domain.ListenType
import com.yourname.mp3player.domain.PlayerController
import com.yourname.mp3player.domain.RankedSong
import com.yourname.mp3player.domain.RankingRepository
import com.yourname.mp3player.domain.Song
import com.yourname.mp3player.domain.SongStats
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/** ALL ranking rules live here: change a number and the whole app follows. */
object RankingRules {
    const val LISTEN_MIN_MS = 30_000L        // a listen = 30 s ...
    const val LISTEN_MIN_FRACTION = 0.5      // ... or half of the song if it is shorter
    const val POINTS_LISTEN = 1
    const val POINTS_REPEAT = 2
    const val POINTS_COMPLETION = 2          // bonus when the song is finished
    const val COMPLETE_MIN_FRACTION = 0.9    // must really have listened to 90% (no seek-to-end cheating)

    fun isListen(playedMs: Long, durationMs: Long): Boolean {
        val threshold = if (durationMs > 0)
            minOf(LISTEN_MIN_MS, (durationMs * LISTEN_MIN_FRACTION).toLong())
        else LISTEN_MIN_MS
        return playedMs >= threshold
    }

    fun isCompleted(playedMs: Long, durationMs: Long): Boolean =
        durationMs > 0 && playedMs >= (durationMs * COMPLETE_MIN_FRACTION).toLong()

    fun pointsFor(isRepeat: Boolean) = if (isRepeat) POINTS_REPEAT else POINTS_LISTEN
}

object Ranking {
    /** Points desc, then most recently played, then title. Songs with 0 points are not ranked. */
    fun build(songs: List<Song>, stats: Map<Long, SongStats>): List<RankedSong> =
        songs.mapNotNull { song ->
            stats[song.id]?.takeIf { it.points > 0 }?.let { song to it }
        }
            .sortedWith(
                compareByDescending<Pair<Song, SongStats>> { it.second.points }
                    .thenByDescending { it.second.lastPlayedAt }
                    .thenBy { it.first.title.lowercase() }
            )
            .mapIndexed { i, (song, st) -> RankedSong(i + 1, song, st.points, st.listens, st.completions) }
}

/** Listens to the player and turns listens into points. */
class RankingRecorder(
    player: PlayerController,
    private val repo: RankingRepository,
    scope: CoroutineScope
) {
    private var lastSongId: Long? = null

    init {
        scope.launch {
            player.events.collect { event ->
                when (event.type) {
                    ListenType.LISTEN -> {
                        val isRepeat = event.song.id == lastSongId
                        lastSongId = event.song.id
                        repo.recordListen(event.song.id, RankingRules.pointsFor(isRepeat))
                    }
                    ListenType.COMPLETION ->
                        repo.recordCompletion(event.song.id, RankingRules.POINTS_COMPLETION)
                }
            }
        }
    }
}
