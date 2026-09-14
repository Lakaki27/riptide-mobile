package com.evertide.riptide.ui.components

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.evertide.riptide.data.local.PreferencesManager
import com.evertide.riptide.data.model.Music
import com.evertide.riptide.data.repository.PlayerRepository
import com.evertide.riptide.data.repository.PlaylistRepository
import com.evertide.riptide.ui.common.ToastBus
import com.evertide.riptide.util.toUserMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

@HiltViewModel
class SongMenuViewModel
@Inject
constructor(
        private val playerRepository: PlayerRepository,
        private val playlistRepository: PlaylistRepository,
        private val prefs: PreferencesManager,
        private val toastBus: ToastBus,
        private val json: Json,
) : ViewModel() {

    val isAdmin: StateFlow<Boolean> =
            prefs.authState
                    .map { it.role == "admin" }
                    .stateIn(viewModelScope, kotlinx.coroutines.flow.SharingStarted.Eagerly, false)

    private val _playlists =
            MutableStateFlow<List<com.evertide.riptide.data.model.Playlist>>(emptyList())
    val playlists: StateFlow<List<com.evertide.riptide.data.model.Playlist>> = _playlists

    private var loaded = false

    fun ensurePlaylistsLoaded() {
        if (loaded || !isAdmin.value) return
        viewModelScope.launch {
            _playlists.value =
                    runCatching { playlistRepository.getPlaylists() }.getOrDefault(emptyList())
            loaded = true
        }
    }

    fun addToQueue(music: Music) {
        playerRepository.addToQueue(music)
        toastBus.show("Added to queue")
    }

    fun playNext(music: Music) {
        playerRepository.playNext(music)
        toastBus.show("Playing next")
    }

    fun addToPlaylist(playlistId: String, playlistName: String, musicId: String) {
        viewModelScope.launch {
            try {
                playlistRepository.addSong(playlistId, musicId)
                toastBus.show("Added to $playlistName")
            } catch (t: Throwable) {
                val message = t.toUserMessage(json)
                toastBus.show(
                        if (message.contains("already", ignoreCase = true))
                                "Already in $playlistName"
                        else "Could not add song"
                )
            }
        }
    }

    fun removeFromPlaylist(playlistId: String, musicId: String) {
        viewModelScope.launch {
            runCatching { playlistRepository.removeSong(playlistId, musicId) }
            toastBus.show("Removed from playlist")
        }
    }
}
