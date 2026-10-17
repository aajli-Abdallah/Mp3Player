package com.yourname.mp3player.data

import com.yourname.mp3player.data.db.RankingDao
import com.yourname.mp3player.data.db.StatsEntity
import com.yourname.mp3player.domain.RankingRepository
import com.yourname.mp3player.domain.SongStats
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.json.JSONArray
import java.io.File

class RoomRankingRepository(
    private val dao: RankingDao,
    private val legacyJson: File,
    scope: CoroutineScope
) : RankingRepository {

    override val stats: StateFlow<Map<Long, SongStats>> = dao.observeStats()
        .map { list ->
            list.associate {
                it.songId to SongStats(it.songId, it.points, it.listens, it.lastPlayedAt, it.completions)
            }
        }
        .stateIn(scope, SharingStarted.Eagerly, emptyMap())

    // One-time import of the old ranking.json (v1.1.x) into the database
    private val migration = scope.launch(Dispatchers.IO) { migrateJson() }

    override suspend fun recordListen(songId: Long, points: Int) {
        migration.join()
        dao.record(songId, points, completion = false, now = System.currentTimeMillis())
    }

    override suspend fun recordCompletion(songId: Long, points: Int) {
        migration.join()
        dao.record(songId, points, completion = true, now = System.currentTimeMillis())
    }

    override suspend fun reset(songId: Long) {
        migration.join()
        dao.resetSong(songId)
    }

    override suspend fun resetAll() {
        migration.join()
        dao.resetAll()
    }

    private suspend fun migrateJson() {
        if (!legacyJson.exists()) return
        runCatching {
            val arr = JSONArray(legacyJson.readText())
            val list = buildList {
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    add(
                        StatsEntity(
                            songId = o.getLong("id"),
                            points = o.getInt("points"),
                            listens = o.getInt("listens"),
                            completions = o.optInt("done", 0),
                            lastPlayedAt = o.optLong("last", 0L)
                        )
                    )
                }
            }
            dao.upsertAllStats(list)
            legacyJson.renameTo(File(legacyJson.parentFile, "ranking.json.migrated"))
        }
    }
}
