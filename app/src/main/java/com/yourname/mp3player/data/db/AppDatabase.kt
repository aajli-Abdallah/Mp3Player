package com.yourname.mp3player.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        StatsEntity::class, HistoryEntity::class, FavoriteEntity::class,
        HiddenEntity::class, PlaylistEntity::class, PlaylistSongEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun rankingDao(): RankingDao
    abstract fun userDataDao(): UserDataDao

    companion object {
        fun build(context: Context): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, "sonora.db").build()
    }
}
