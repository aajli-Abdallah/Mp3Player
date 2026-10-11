package com.yourname.mp3player.player

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.yourname.mp3player.domain.ListenEvent
import com.yourname.mp3player.domain.PlaybackState
import com.yourname.mp3player.domain.PlayerController
import com.yourname.mp3player.domain.RepeatMode
import com.yourname.mp3player.domain.Song
import com.yourname.mp3player.domain.usecase.RankingRules
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class Media3PlayerController(private val context: Context) : PlayerController {

    private val _state = MutableStateFlow(PlaybackState())
    override val state: StateFlow<PlaybackState> = _state.asStateFlow()

    private val _events = MutableSharedFlow<ListenEvent>(extraBufferCapacity = 16)
    override val events: SharedFlow<ListenEvent> = _events.asSharedFlow()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var controller: MediaController? = null
    private var queue: List<Song> = emptyList()
    private val pending = mutableListOf<() -> Unit>()

    // Listen tracking for the current "play instance" of a song
    private var playedMs = 0L
    private var listenCounted = false

    private fun resetListen() {
        playedMs = 0L
        listenCounted = false
    }

    private val listener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _state.update { it.copy(isPlaying = isPlaying) }
        }

        // Also fires with reason REPEAT when repeat-one restarts the same song
        override fun onMediaItemTransition(item: MediaItem?, reason: Int) {
            resetListen()
            _state.update { it.copy(currentSong = resolve(item), positionMs = 0) }
        }

        override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
            _state.update { it.copy(shuffle = shuffleModeEnabled) }
        }

        override fun onRepeatModeChanged(repeatMode: Int) {
            _state.update { it.copy(repeat = repeatMode.toRepeat()) }
        }
    }

    init {
        val token = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        val future = MediaController.Builder(context, token).buildAsync()
        future.addListener({
            val c = future.get()
            controller = c
            c.addListener(listener)
            syncFrom(c)
            pending.toList().forEach { it() }
            pending.clear()
        }, ContextCompat.getMainExecutor(context))

        // Ticker (only while playing): position + real listening time
        scope.launch {
            while (isActive) {
                delay(TICK_MS)
                val c = controller
                if (c != null && c.isPlaying) {
                    _state.update { it.copy(positionMs = c.currentPosition) }
                    playedMs += TICK_MS
                    checkListen(c)
                }
            }
        }
    }

    private fun checkListen(c: MediaController) {
        if (listenCounted) return
        val song = _state.value.currentSong ?: return
        val duration = if (song.durationMs > 0) song.durationMs else c.duration.coerceAtLeast(0L)
        if (RankingRules.isListen(playedMs, duration)) {
            listenCounted = true
            _events.tryEmit(ListenEvent(song))
        }
    }

    /** If the service was already playing (app reopened), restore the UI state. */
    private fun syncFrom(c: MediaController) {
        _state.update {
            it.copy(
                currentSong = resolve(c.currentMediaItem),
                isPlaying = c.isPlaying,
                positionMs = c.currentPosition,
                shuffle = c.shuffleModeEnabled,
                repeat = c.repeatMode.toRepeat()
            )
        }
    }

    private fun resolve(item: MediaItem?): Song? {
        item ?: return null
        queue.firstOrNull { it.id.toString() == item.mediaId }?.let { return it }
        val md = item.mediaMetadata
        return Song(
            id = item.mediaId.toLongOrNull() ?: 0L,
            title = md.title?.toString() ?: "Unknown",
            artist = md.artist?.toString() ?: "Unknown artist",
            album = md.albumTitle?.toString() ?: "",
            durationMs = controller?.duration?.takeIf { it > 0 } ?: 0L,
            uri = item.localConfiguration?.uri ?: Uri.EMPTY,
            albumArtUri = md.artworkUri
        )
    }

    private fun Int.toRepeat() = when (this) {
        Player.REPEAT_MODE_ONE -> RepeatMode.ONE
        Player.REPEAT_MODE_ALL -> RepeatMode.ALL
        else -> RepeatMode.OFF
    }

    private fun withController(block: (MediaController) -> Unit) {
        val c = controller
        if (c != null) block(c) else pending += { controller?.let(block) }
    }

    override fun play(songs: List<Song>, startIndex: Int) {
        queue = songs
        resetListen()
        _state.update {
            it.copy(queue = songs, currentSong = songs.getOrNull(startIndex), positionMs = 0)
        }
        withController { c ->
            c.setMediaItems(songs.map { it.toMediaItem() }, startIndex, 0L)
            c.prepare()
            c.play()
        }
    }

    override fun togglePlayPause() = withController { c ->
        if (c.isPlaying) c.pause() else c.play()
    }

    override fun next() = withController { it.seekToNextMediaItem() }
    override fun previous() = withController { it.seekToPreviousMediaItem() }

    override fun seekTo(positionMs: Long) = withController {
        it.seekTo(positionMs)
        _state.update { s -> s.copy(positionMs = positionMs) }
    }

    override fun setShuffle(enabled: Boolean) = withController { it.shuffleModeEnabled = enabled }

    override fun setRepeat(mode: RepeatMode) = withController {
        it.repeatMode = when (mode) {
            RepeatMode.OFF -> Player.REPEAT_MODE_OFF
            RepeatMode.ALL -> Player.REPEAT_MODE_ALL
            RepeatMode.ONE -> Player.REPEAT_MODE_ONE
        }
    }

    private fun Song.toMediaItem() = MediaItem.Builder()
        .setMediaId(id.toString())
        .setUri(uri)
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setTitle(title)
                .setArtist(artist)
                .setAlbumTitle(album)
                .setArtworkUri(albumArtUri)
                .build()
        )
        .build()

    private companion object {
        const val TICK_MS = 500L
    }
}
