package com.yourname.mp3player.feature.settings

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yourname.mp3player.data.BackupManager
import com.yourname.mp3player.data.SettingsRepository
import com.yourname.mp3player.domain.MusicRepository
import com.yourname.mp3player.domain.RankingRepository
import com.yourname.mp3player.domain.Song
import com.yourname.mp3player.domain.UserDataRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    val settings: SettingsRepository,
    rawMusic: MusicRepository,
    private val userData: UserDataRepository,
    private val ranking: RankingRepository,
    private val backup: BackupManager
) : ViewModel() {

    /** Hidden songs with their names (needs the raw, unfiltered library). */
    val hiddenSongs: StateFlow<List<Song>> =
        combine(rawMusic.getSongs(), userData.hidden) { all, hidden -> all.filter { it.id in hidden } }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _status = MutableStateFlow<String?>(null)
    val status = _status.asStateFlow()

    private val _busy = MutableStateFlow(false)
    val busy = _busy.asStateFlow()

    fun unhide(songId: Long) { viewModelScope.launch { userData.unhide(songId) } }
    fun unhideAll() { viewModelScope.launch { userData.unhideAll() } }
    fun resetRanking() { viewModelScope.launch { ranking.resetAll(); _status.value = "Ranking reset." } }

    fun export(uri: Uri) = runBackup { backup.export(uri) }
    fun import(uri: Uri) = runBackup { backup.import(uri) }

    private fun runBackup(block: suspend () -> Result<String>) {
        viewModelScope.launch {
            _busy.value = true
            _status.value = null
            val result = block()
            _status.value = result.getOrElse { "Failed: ${it.message ?: it.javaClass.simpleName}" }
            _busy.value = false
        }
    }
}
