package com.yourname.mp3player.feature.player

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.yourname.mp3player.App
import com.yourname.mp3player.domain.PlayerController
import com.yourname.mp3player.domain.RepeatMode

class PlayerViewModel(private val player: PlayerController) : ViewModel() {
    val state = player.state

    fun togglePlayPause() = player.togglePlayPause()
    fun next() = player.next()
    fun previous() = player.previous()
    fun seekTo(ms: Long) = player.seekTo(ms)
    fun toggleShuffle() = player.setShuffle(!state.value.shuffle)
    fun cycleRepeat() = player.setRepeat(
        when (state.value.repeat) {
            RepeatMode.OFF -> RepeatMode.ALL
            RepeatMode.ALL -> RepeatMode.ONE
            RepeatMode.ONE -> RepeatMode.OFF
        }
    )
}

@Composable
fun rememberPlayerViewModel(): PlayerViewModel {
    val app = LocalContext.current.applicationContext as App
    return viewModel(
        factory = viewModelFactory {
            initializer { PlayerViewModel(app.container.playerController) }
        }
    )
}

fun formatTime(ms: Long): String {
    val s = (ms / 1000).coerceAtLeast(0)
    return "%d:%02d".format(s / 60, s % 60)
}
