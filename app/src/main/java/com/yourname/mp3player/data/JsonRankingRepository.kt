package com.yourname.mp3player.data

import android.content.Context
import com.yourname.mp3player.domain.RankingRepository
import com.yourname.mp3player.domain.SongStats
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/** Simple, dependency-free storage. Can be replaced by Room later (same interface). */
class JsonRankingRepository(context: Context) : RankingRepository {

    private val file = File(context.filesDir, "ranking.json")
    private val mutex = Mutex()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _stats = MutableStateFlow<Map<Long, SongStats>>(emptyMap())
    override val stats: StateFlow<Map<Long, SongStats>> = _stats.asStateFlow()

    private val loadJob = scope.launch {
        mutex.withLock { _stats.value = read() }
    }

    override suspend fun addPoints(songId: Long, points: Int) {
        loadJob.join() // never write before the saved data is loaded
        mutex.withLock {
            val current = _stats.value[songId] ?: SongStats(songId, 0, 0, 0L)
            val updated = current.copy(
                points = current.points + points,
                listens = current.listens + 1,
                lastPlayedAt = System.currentTimeMillis()
            )
            val newMap = _stats.value + (songId to updated)
            _stats.value = newMap
            withContext(Dispatchers.IO) { write(newMap) }
        }
    }

    private fun read(): Map<Long, SongStats> {
        if (!file.exists()) return emptyMap()
        return runCatching {
            val arr = JSONArray(file.readText())
            buildMap<Long, SongStats> {
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    val id = o.getLong("id")
                    put(id, SongStats(id, o.getInt("points"), o.getInt("listens"), o.optLong("last", 0L)))
                }
            }
        }.getOrDefault(emptyMap())
    }

    private fun write(map: Map<Long, SongStats>) {
        runCatching {
            val arr = JSONArray()
            map.values.forEach {
                arr.put(
                    JSONObject()
                        .put("id", it.songId)
                        .put("points", it.points)
                        .put("listens", it.listens)
                        .put("last", it.lastPlayedAt)
                )
            }
            val tmp = File(file.parentFile, "ranking.json.tmp")
            tmp.writeText(arr.toString())
            if (!tmp.renameTo(file)) file.writeText(arr.toString())
        }
    }
}
