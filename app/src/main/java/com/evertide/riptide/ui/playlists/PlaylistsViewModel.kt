package com.evertide.riptide.ui.playlists

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.evertide.riptide.data.local.PreferencesManager
import com.evertide.riptide.data.model.Playlist
import com.evertide.riptide.data.repository.PlaylistRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class PlaylistsUiState(
        val playlists: List<Playlist> = emptyList(),
        val loading: Boolean = true,
)

@HiltViewModel
class PlaylistsViewModel
@Inject
constructor(
        private val playlistRepository: PlaylistRepository,
        prefs: PreferencesManager,
) : ViewModel() {

    val isAdmin: StateFlow<Boolean> =
            prefs.authState
                    .map { it.role == "admin" }
                    .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    private val _uiState = MutableStateFlow(PlaylistsUiState())
    val uiState: StateFlow<PlaylistsUiState> = _uiState

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(loading = true)
            val playlists =
                    runCatching { playlistRepository.getPlaylists() }.getOrDefault(emptyList())
            _uiState.value = PlaylistsUiState(playlists = playlists, loading = false)
        }
    }

    fun createPlaylist(name: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            runCatching { playlistRepository.createPlaylist(name) }
            load()
        }
    }
}
