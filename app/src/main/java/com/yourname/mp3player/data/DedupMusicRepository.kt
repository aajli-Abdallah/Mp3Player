package com.yourname.mp3player.data

import com.yourname.mp3player.domain.DedupeMode
import com.yourname.mp3player.domain.MusicRepository
import com.yourname.mp3player.domain.Song
import com.yourname.mp3player.domain.SongStats
import com.yourname.mp3player.domain.usecase.SongDedupe
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine

/** Wraps the raw MediaStore list: removes duplicates (per setting) and hidden songs. */
class DedupMusicRepository(
    private val inner: MusicRepository,
    private val stats: StateFlow<Map<Long, SongStats>>,
    private val hidden: StateFlow<Set<Long>>,
    private val mode: StateFlow<DedupeMode>
) : MusicRepository {

    override fun getSongs(): Flow<List<Song>> =
        combine(inner.getSongs(), stats, hidden, mode) { songs, st, hid, m ->
            // dedupe first, then hide: hiding a song removes its whole duplicate group
            SongDedupe.dedupe(songs, st, m).filter { it.id !in hid }
        }

    override suspend fun refresh() = inner.refresh()
}
