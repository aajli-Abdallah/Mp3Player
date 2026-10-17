package com.yourname.mp3player.data.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "song_stats")
data class StatsEntity(
    @PrimaryKey val songId: Long,
    val points: Int,
    val listens: Int,
    val completions: Int,
    val lastPlayedAt: Long
)

/** One row per counted listen (type 0) or completion (type 1). Feeds future stats / Wrapped. */
@Entity(tableName = "listen_history", indices = [Index("songId"), Index("timestamp")])
data class HistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val songId: Long,
    val type: Int,
    val points: Int,
    val timestamp: Long
)

@Entity(tableName = "favorites")
data class FavoriteEntity(@PrimaryKey val songId: Long, val addedAt: Long)

@Entity(tableName = "hidden_songs")
data class HiddenEntity(@PrimaryKey val songId: Long)

@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val createdAt: Long
)

@Entity(
    tableName = "playlist_songs",
    primaryKeys = ["playlistId", "songId"],
    foreignKeys = [ForeignKey(
        entity = PlaylistEntity::class,
        parentColumns = ["id"],
        childColumns = ["playlistId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("playlistId")]
)
data class PlaylistSongEntity(val playlistId: Long, val songId: Long, val position: Int)
