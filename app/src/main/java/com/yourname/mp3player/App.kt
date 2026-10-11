package com.yourname.mp3player

import android.app.Application
import com.yourname.mp3player.data.JsonRankingRepository
import com.yourname.mp3player.data.MediaStoreMusicRepository
import com.yourname.mp3player.domain.MusicRepository
import com.yourname.mp3player.domain.PlayerController
import com.yourname.mp3player.domain.RankingRepository
import com.yourname.mp3player.domain.usecase.RankingRecorder
import com.yourname.mp3player.player.Media3PlayerController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/** Tiny manual DI. Can be replaced by Hilt later without touching the UI. */
class AppContainer(app: Application) {
    val musicRepository: MusicRepository = MediaStoreMusicRepository(app)
    val rankingRepository: RankingRepository = JsonRankingRepository(app)
    val playerController: PlayerController by lazy { Media3PlayerController(app) }

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    // Created eagerly so listens are counted even when the UI is closed.
    val rankingRecorder = RankingRecorder(playerController, rankingRepository, appScope)
}

class App : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
