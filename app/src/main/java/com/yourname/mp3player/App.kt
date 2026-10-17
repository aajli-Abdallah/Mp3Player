package com.yourname.mp3player

import android.app.Application
import com.yourname.mp3player.data.BackupManager
import com.yourname.mp3player.data.DedupMusicRepository
import com.yourname.mp3player.data.MediaStoreMusicRepository
import com.yourname.mp3player.data.RoomRankingRepository
import com.yourname.mp3player.data.RoomUserDataRepository
import com.yourname.mp3player.data.SettingsRepository
import com.yourname.mp3player.data.db.AppDatabase
import com.yourname.mp3player.domain.MusicRepository
import com.yourname.mp3player.domain.PlayerController
import com.yourname.mp3player.domain.RankedSong
import com.yourname.mp3player.domain.RankingRepository
import com.yourname.mp3player.domain.UserDataRepository
import com.yourname.mp3player.domain.usecase.Ranking
import com.yourname.mp3player.domain.usecase.RankingRecorder
import com.yourname.mp3player.player.Media3PlayerController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.io.File

/** Tiny manual DI. Can be replaced by Hilt later without touching the UI. */
class AppContainer(app: Application) {
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val db = AppDatabase.build(app)

    val settings = SettingsRepository(app)

    val rankingRepository: RankingRepository =
        RoomRankingRepository(db.rankingDao(), File(app.filesDir, "ranking.json"), appScope)

    val userData: UserDataRepository = RoomUserDataRepository(db.userDataDao(), appScope)

    /** Every MP3 on the device (no filtering). */
    val rawMusicRepository: MusicRepository = MediaStoreMusicRepository(app)

    /** What the app shows: duplicates and hidden songs removed. */
    val musicRepository: MusicRepository = DedupMusicRepository(
        rawMusicRepository, rankingRepository.stats, userData.hidden, settings.dedupeMode
    )

    val playerController: PlayerController by lazy { Media3PlayerController(app) }

    /** Single source of truth for the ranking, shared by every screen. */
    val rankedSongs: StateFlow<List<RankedSong>> =
        combine(musicRepository.getSongs(), rankingRepository.stats) { songs, stats ->
            Ranking.build(songs, stats)
        }.stateIn(appScope, SharingStarted.Eagerly, emptyList())

    // Created eagerly so listens are counted even when the UI is closed.
    val rankingRecorder = RankingRecorder(playerController, rankingRepository, appScope)

    val backup = BackupManager(app, db, rawMusicRepository, settings)
}

class App : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
