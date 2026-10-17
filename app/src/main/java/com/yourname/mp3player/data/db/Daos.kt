package com.yourname.mp3player.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
abstract class RankingDao {
    @Query("SELECT * FROM song_stats")
    abstract fun observeStats(): Flow<List<StatsEntity>>

    @Query("SELECT * FROM song_stats")
    abstract suspend fun allStats(): List<StatsEntity>

    @Query("SELECT * FROM listen_history")
    abstract suspend fun allHistory(): List<HistoryEntity>

    @Query("SELECT * FROM song_stats WHERE songId = :id")
    abstract suspend fun getStats(id: Long): StatsEntity?

    @Upsert abstract suspend fun upsertStats(e: StatsEntity)
    @Upsert abstract suspend fun upsertAllStats(list: List<StatsEntity>)
    @Insert abstract suspend fun insertHistory(h: HistoryEntity)
    @Insert abstract suspend fun insertAllHistory(list: List<HistoryEntity>)

    @Query("DELETE FROM song_stats WHERE songId = :id") abstract suspend fun deleteStats(id: Long)
    @Query("DELETE FROM listen_history WHERE songId = :id") abstract suspend fun deleteHistoryOf(id: Long)
    @Query("DELETE FROM song_stats") abstract suspend fun deleteAllStats()
    @Query("DELETE FROM listen_history") abstract suspend fun deleteAllHistory()

    /** Updates the totals AND writes a history row, atomically. */
    @Transaction
    open suspend fun record(songId: Long, points: Int, completion: Boolean, now: Long) {
        val cur = getStats(songId) ?: StatsEntity(songId, 0, 0, 0, 0L)
        upsertStats(
            cur.copy(
                points = cur.points + points,
                listens = cur.listens + if (completion) 0 else 1,
                completions = cur.completions + if (completion) 1 else 0,
                lastPlayedAt = now
            )
        )
        insertHistory(
            HistoryEntity(songId = songId, type = if (completion) 1 else 0, points = points, timestamp = now)
        )
    }

    @Transaction
    open suspend fun resetSong(id: Long) { deleteStats(id); deleteHistoryOf(id) }

    @Transaction
    open suspend fun resetAll() { deleteAllStats(); deleteAllHistory() }
}

@Dao
abstract class UserDataDao {
    // favorites
    @Query("SELECT songId FROM favorites ORDER BY addedAt DESC")
    abstract fun observeFavorites(): Flow<List<Long>>
    @Query("SELECT * FROM favorites") abstract suspend fun allFavorites(): List<FavoriteEntity>
    @Insert(onConflict = OnConflictStrategy.REPLACE) abstract suspend fun insertFavorite(e: FavoriteEntity)
    @Insert(onConflict = OnConflictStrategy.REPLACE) abstract suspend fun insertFavorites(list: List<FavoriteEntity>)
    @Query("DELETE FROM favorites WHERE songId = :id") abstract suspend fun deleteFavorite(id: Long)
    @Query("DELETE FROM favorites") abstract suspend fun deleteAllFavorites()

    // hidden
    @Query("SELECT songId FROM hidden_songs") abstract fun observeHidden(): Flow<List<Long>>
    @Query("SELECT * FROM hidden_songs") abstract suspend fun allHidden(): List<HiddenEntity>
    @Insert(onConflict = OnConflictStrategy.REPLACE) abstract suspend fun insertHidden(e: HiddenEntity)
    @Insert(onConflict = OnConflictStrategy.REPLACE) abstract suspend fun insertHiddenAll(list: List<HiddenEntity>)
    @Query("DELETE FROM hidden_songs WHERE songId = :id") abstract suspend fun deleteHidden(id: Long)
    @Query("DELETE FROM hidden_songs") abstract suspend fun deleteAllHidden()

    // playlists
    @Query("SELECT * FROM playlists ORDER BY createdAt") abstract fun observePlaylists(): Flow<List<PlaylistEntity>>
    @Query("SELECT * FROM playlist_songs ORDER BY playlistId, position")
    abstract fun observePlaylistSongs(): Flow<List<PlaylistSongEntity>>
    @Query("SELECT * FROM playlists") abstract suspend fun allPlaylists(): List<PlaylistEntity>
    @Query("SELECT * FROM playlist_songs") abstract suspend fun allPlaylistSongs(): List<PlaylistSongEntity>
    @Query("SELECT * FROM playlist_songs WHERE playlistId = :pid ORDER BY position")
    abstract suspend fun songsOf(pid: Long): List<PlaylistSongEntity>

    @Insert abstract suspend fun insertPlaylist(e: PlaylistEntity): Long
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertPlaylistSongs(list: List<PlaylistSongEntity>)
    @Query("UPDATE playlists SET name = :name WHERE id = :id") abstract suspend fun renamePlaylist(id: Long, name: String)
    @Query("DELETE FROM playlists WHERE id = :id") abstract suspend fun deletePlaylistRow(id: Long)

    /** Removes the songs explicitly, then the playlist, in one transaction (no reliance on FK pragma). */
    @Transaction
    open suspend fun deletePlaylist(id: Long) {
        deleteSongsOf(id)
        deletePlaylistRow(id)
    }
    @Query("DELETE FROM playlists") abstract suspend fun deleteAllPlaylists()
    @Query("DELETE FROM playlist_songs") abstract suspend fun deleteAllPlaylistSongs()
    @Query("DELETE FROM playlist_songs WHERE playlistId = :pid AND songId = :sid")
    abstract suspend fun deletePlaylistSong(pid: Long, sid: Long)
    @Query("DELETE FROM playlist_songs WHERE playlistId = :pid") abstract suspend fun deleteSongsOf(pid: Long)

    @Transaction
    open suspend fun appendToPlaylist(pid: Long, songIds: List<Long>) {
        val existing = songsOf(pid)
        val have = existing.map { it.songId }.toSet()
        var pos = (existing.maxOfOrNull { it.position } ?: -1) + 1
        insertPlaylistSongs(
            songIds.distinct().filter { it !in have }.map { PlaylistSongEntity(pid, it, pos++) }
        )
    }

    @Transaction
    open suspend fun setPlaylistOrder(pid: Long, ids: List<Long>) {
        deleteSongsOf(pid)
        insertPlaylistSongs(ids.mapIndexed { i, id -> PlaylistSongEntity(pid, id, i) })
    }
}
