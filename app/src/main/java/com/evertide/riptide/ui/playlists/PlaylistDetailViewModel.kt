package com.evertide.riptide.ui.playlists

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.evertide.riptide.data.model.Music
import com.evertide.riptide.data.model.Playlist
import com.evertide.riptide.data.repository.MusicRepository
import com.evertide.riptide.data.repository.PlayerRepository
import com.evertide.riptide.data.repository.PlaylistRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class PlaylistDetailUiState(
        val playlist: Playlist? = null,
        val localQuery: String = "",
        val showAddModal: Boolean = false,
        val addQuery: String = "",
        val addResults: List<Music> = emptyList(),
)

@HiltViewModel
class PlaylistDetailViewModel
@Inject
constructor(
        private val playlistRepository: PlaylistRepository,
        private val musicRepository: MusicRepository,
        val playerRepository: PlayerRepository,
        savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val playlistId: String = checkNotNull(savedStateHandle["playlistId"])

    private val _uiState = MutableStateFlow(PlaylistDetailUiState())
    val uiState: StateFlow<PlaylistDetailUiState> = _uiState

    init {
        loadPlaylist()
    }

    fun loadPlaylist() {
        viewModelScope.launch {
            val playlist = runCatching { playlistRepository.getPlaylist(playlistId) }.getOrNull()
            _uiState.value = _uiState.value.copy(playlist = playlist)
        }
    }

    fun onLocalQueryChange(query: String) {
        _uiState.value = _uiState.value.copy(localQuery = query)
    }

    fun filteredMusics(): List<Music> {
        val playlist = _uiState.value.playlist ?: return emptyList()
        val query = _uiState.value.localQuery
        return if (query.isBlank()) {
            playlist.musics
        } else {
            playlist.musics.filter { it.title.contains(query, ignoreCase = true) }
        }
    }

    fun playAll() {
        _uiState.value.playlist?.let { playerRepository.setQueue(it.musics, 0) }
    }

    fun setAddModalVisible(visible: Boolean) {
        _uiState.value =
                _uiState.value.copy(showAddModal = visible, addQuery = "", addResults = emptyList())
    }

    fun onAddQueryChange(query: String) {
        _uiState.value = _uiState.value.copy(addQuery = query)
        viewModelScope.launch {
            if (query.isBlank()) {
                _uiState.value = _uiState.value.copy(addResults = emptyList())
                return@launch
            }
            val results = runCatching { musicRepository.search(query) }.getOrDefault(emptyList())
            _uiState.value = _uiState.value.copy(addResults = results)
        }
    }

    fun visibleAddResults(): List<Music> {
        val playlistMusicIds = _uiState.value.playlist?.musics?.map { it.id }?.toSet() ?: emptySet()
        return _uiState.value.addResults.filterNot { it.id in playlistMusicIds }
    }

    fun addSong(musicId: String) {
        viewModelScope.launch {
            runCatching { playlistRepository.addSong(playlistId, musicId) }
            loadPlaylist()
        }
    }

    fun removeSong(musicId: String) {
        viewModelScope.launch {
            runCatching { playlistRepository.removeSong(playlistId, musicId) }
            loadPlaylist()
        }
    }
}
