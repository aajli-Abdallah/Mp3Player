package com.yourname.mp3player.data

import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import com.yourname.mp3player.data.db.AppDatabase
import com.yourname.mp3player.data.db.FavoriteEntity
import com.yourname.mp3player.data.db.HiddenEntity
import com.yourname.mp3player.data.db.HistoryEntity
import com.yourname.mp3player.data.db.PlaylistEntity
import com.yourname.mp3player.data.db.PlaylistSongEntity
import com.yourname.mp3player.data.db.StatsEntity
import com.yourname.mp3player.domain.DedupeMode
import com.yourname.mp3player.domain.MusicRepository
import com.yourname.mp3player.domain.Song
import com.yourname.mp3player.domain.ThemeMode
import com.yourname.mp3player.domain.usecase.SongDedupe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

/**
 * Export / import of everything the user owns (ranking, history, favorites, hidden songs,
 * playlists, settings) as one JSON file.
 *
 * Songs are stored with title + artist, so a backup can be restored even if Android gave
 * the files new ids (reinstall, new phone).
 */
class BackupManager(
    private val context: Context,
    private val db: AppDatabase,
    private val rawMusic: MusicRepository,
    private val settings: SettingsRepository
) {

    private suspend fun library(): List<Song> {
        var lib = rawMusic.getSongs().first()
        if (lib.isEmpty()) {
            rawMusic.refresh()
            lib = rawMusic.getSongs().first()
        }
        return lib
    }

    suspend fun export(uri: Uri): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val lib = library().associateBy { it.id }
            val rd = db.rankingDao()
            val ud = db.userDataDao()

            val stats = rd.allStats().filter { it.songId in lib }
            val history = rd.allHistory().filter { it.songId in lib }
            val favs = ud.allFavorites().filter { it.songId in lib }
            val hidden = ud.allHidden().filter { it.songId in lib }
            val playlists = ud.allPlaylists()
            val plSongs = ud.allPlaylistSongs().filter { it.songId in lib }.groupBy { it.playlistId }

            val used = LinkedHashSet<Long>()
            stats.forEach { used += it.songId }
            history.forEach { used += it.songId }
            favs.forEach { used += it.songId }
            hidden.forEach { used += it.songId }
            plSongs.values.forEach { l -> l.forEach { used += it.songId } }

            val root = JSONObject()
                .put("app", "Sonora")
                .put("version", 1)
                .put("exportedAt", System.currentTimeMillis())

            root.put("songs", JSONArray().also { arr ->
                used.forEach { id ->
                    val s = lib.getValue(id)
                    arr.put(
                        JSONObject().put("id", id).put("title", s.title)
                            .put("artist", s.artist).put("album", s.album).put("duration", s.durationMs)
                    )
                }
            })
            root.put("stats", JSONArray().also { arr ->
                stats.forEach {
                    arr.put(
                        JSONObject().put("id", it.songId).put("points", it.points)
                            .put("listens", it.listens).put("done", it.completions).put("last", it.lastPlayedAt)
                    )
                }
            })
            root.put("history", JSONArray().also { arr ->
                history.forEach {
                    arr.put(
                        JSONObject().put("id", it.songId).put("type", it.type)
                            .put("points", it.points).put("ts", it.timestamp)
                    )
                }
            })
            root.put("favorites", JSONArray().also { arr ->
                favs.forEach { arr.put(JSONObject().put("id", it.songId).put("at", it.addedAt)) }
            })
            root.put("hidden", JSONArray().also { arr -> hidden.forEach { arr.put(it.songId) } })
            root.put("playlists", JSONArray().also { arr ->
                playlists.forEach { p ->
                    val ids = JSONArray()
                    (plSongs[p.id] ?: emptyList()).sortedBy { it.position }.forEach { ids.put(it.songId) }
                    arr.put(JSONObject().put("name", p.name).put("created", p.createdAt).put("songs", ids))
                }
            })
            root.put(
                "settings",
                JSONObject().put("theme", settings.theme.value.name).put("dedupe", settings.dedupeMode.value.name)
            )

            context.contentResolver.openOutputStream(uri, "wt")!!.use {
                it.write(root.toString().toByteArray())
            }
            "Backup saved: ${stats.size} ranked songs, ${favs.size} favorites, ${playlists.size} playlists."
        }
    }

    /** Replaces ALL current user data with the content of the backup. */
    suspend fun import(uri: Uri): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val text = context.contentResolver.openInputStream(uri)!!.bufferedReader().use { it.readText() }
            val root = JSONObject(text)
            require(root.optString("app") == "Sonora") { "This is not a Sonora backup file." }

            rawMusic.refresh()
            val lib = rawMusic.getSongs().first()
            require(lib.isNotEmpty()) { "No music found on this device yet. Rescan first." }
            val byId = lib.associateBy { it.id }
            val byFingerprint = lib.groupBy { SongDedupe.fingerprint(it.title, it.artist) }

            val table = HashMap<Long, Pair<String, String>>()
            root.getJSONArray("songs").let { a ->
                for (i in 0 until a.length()) {
                    val o = a.getJSONObject(i)
                    table[o.getLong("id")] = o.getString("title") to o.optString("artist", "")
                }
            }

            val cache = HashMap<Long, Long?>()
            val missing = HashSet<Long>()
            fun map(oldId: Long): Long? {
                if (cache.containsKey(oldId)) return cache[oldId]
                val entry = table[oldId]
                val result: Long? = if (entry == null) null else {
                    val direct = byId[oldId]
                    if (direct != null && SongDedupe.key(direct.title) == SongDedupe.key(entry.first)) oldId
                    else byFingerprint[SongDedupe.fingerprint(entry.first, entry.second)]
                        ?.minByOrNull { it.id }?.id
                }
                cache[oldId] = result
                if (result == null) missing += oldId
                return result
            }

            // stats (merge if two old ids land on the same song)
            val stats = HashMap<Long, StatsEntity>()
            root.optJSONArray("stats")?.let { a ->
                for (i in 0 until a.length()) {
                    val o = a.getJSONObject(i)
                    val nid = map(o.getLong("id")) ?: continue
                    val e = StatsEntity(nid, o.getInt("points"), o.getInt("listens"), o.optInt("done", 0), o.optLong("last", 0L))
                    val prev = stats[nid]
                    stats[nid] = if (prev == null) e else prev.copy(
                        points = prev.points + e.points,
                        listens = prev.listens + e.listens,
                        completions = prev.completions + e.completions,
                        lastPlayedAt = maxOf(prev.lastPlayedAt, e.lastPlayedAt)
                    )
                }
            }

            val history = ArrayList<HistoryEntity>()
            root.optJSONArray("history")?.let { a ->
                for (i in 0 until a.length()) {
                    val o = a.getJSONObject(i)
                    val nid = map(o.getLong("id")) ?: continue
                    history += HistoryEntity(songId = nid, type = o.getInt("type"), points = o.getInt("points"), timestamp = o.getLong("ts"))
                }
            }

            val favorites = ArrayList<FavoriteEntity>()
            root.optJSONArray("favorites")?.let { a ->
                for (i in 0 until a.length()) {
                    val o = a.getJSONObject(i)
                    val nid = map(o.getLong("id")) ?: continue
                    favorites += FavoriteEntity(nid, o.optLong("at", 0L))
                }
            }

            val hidden = ArrayList<HiddenEntity>()
            root.optJSONArray("hidden")?.let { a ->
                for (i in 0 until a.length()) {
                    val nid = map(a.getLong(i)) ?: continue
                    hidden += HiddenEntity(nid)
                }
            }

            val playlists = ArrayList<Triple<String, Long, List<Long>>>()
            root.optJSONArray("playlists")?.let { a ->
                for (i in 0 until a.length()) {
                    val o = a.getJSONObject(i)
                    val ids = o.getJSONArray("songs")
                    val mapped = ArrayList<Long>()
                    for (j in 0 until ids.length()) map(ids.getLong(j))?.let { mapped += it }
                    playlists += Triple(o.getString("name"), o.optLong("created", System.currentTimeMillis()), mapped)
                }
            }

            val rd = db.rankingDao()
            val ud = db.userDataDao()
            db.withTransaction {
                rd.deleteAllStats(); rd.deleteAllHistory()
                ud.deleteAllFavorites(); ud.deleteAllHidden()
                ud.deleteAllPlaylistSongs(); ud.deleteAllPlaylists()

                rd.upsertAllStats(stats.values.toList())
                rd.insertAllHistory(history)
                ud.insertFavorites(favorites)
                ud.insertHiddenAll(hidden)
                playlists.forEach { (name, created, ids) ->
                    val pid = ud.insertPlaylist(PlaylistEntity(name = name, createdAt = created))
                    ud.insertPlaylistSongs(ids.distinct().mapIndexed { i, id -> PlaylistSongEntity(pid, id, i) })
                }
            }

            root.optJSONObject("settings")?.let { s ->
                runCatching { settings.setTheme(ThemeMode.valueOf(s.getString("theme"))) }
                runCatching { settings.setDedupeMode(DedupeMode.valueOf(s.getString("dedupe"))) }
            }

            buildString {
                append("Restored ${stats.size} ranked songs, ${favorites.size} favorites, ${playlists.size} playlists.")
                if (missing.isNotEmpty()) append(" ${missing.size} songs were not found on this device and were skipped.")
            }
        }
    }
}
