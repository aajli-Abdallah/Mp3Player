package com.yourname.mp3player

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.yourname.mp3player.feature.home.HomeScreen
import com.yourname.mp3player.feature.nowplaying.NowPlayingScreen

object Routes {
    const val LIBRARY = "library"
    const val PLAYER = "player"
}

@Composable
fun AppNav() {
    val nav = rememberNavController()
    NavHost(nav, startDestination = Routes.LIBRARY) {
        composable(Routes.LIBRARY) {
            HomeScreen(onOpenPlayer = { nav.navigate(Routes.PLAYER) })
        }
        composable(
            Routes.PLAYER,
            enterTransition = { slideInVertically(tween(320)) { it } },
            exitTransition = { fadeOut(tween(200)) },
            popEnterTransition = { fadeIn(tween(200)) },
            popExitTransition = { slideOutVertically(tween(320)) { it } }
        ) {
            NowPlayingScreen(onBack = { nav.popBackStack() })
        }
    }
}
