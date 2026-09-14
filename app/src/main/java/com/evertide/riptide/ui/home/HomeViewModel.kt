package com.evertide.riptide.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.evertide.riptide.data.local.PreferencesManager
import com.evertide.riptide.data.model.Music
import com.evertide.riptide.data.repository.DownloadsRepository
import com.evertide.riptide.data.repository.MusicRepository
import com.evertide.riptide.data.repository.PlayerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HomeUiState(
        val allMusics: List<Music> = emptyList(),
        val musics: List<Music> = emptyList(),
        val query: String = "",
        val loading: Boolean = true,
)

@HiltViewModel
class HomeViewModel
@Inject
constructor(
        private val musicRepository: MusicRepository,
        val playerRepository: PlayerRepository,
        downloadsRepository: DownloadsRepository,
        prefs: PreferencesManager,
) : ViewModel() {

    val isAdmin: StateFlow<Boolean> =
            prefs.authState
                    .map { it.role == "admin" }
                    .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState

    init {
        loadMusics()
        viewModelScope.launch { downloadsRepository.completedEvents.collect { loadMusics() } }
    }

    fun loadMusics() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(loading = true)
            val musics = runCatching { musicRepository.getAllMusics() }.getOrDefault(emptyList())
            _uiState.value =
                    _uiState.value.copy(allMusics = musics, musics = musics, loading = false)
        }
    }

    fun onQueryChange(query: String) {
        _uiState.value = _uiState.value.copy(query = query)
        viewModelScope.launch {
            if (query.isBlank()) {
                _uiState.value = _uiState.value.copy(musics = _uiState.value.allMusics)
                return@launch
            }
            val results = runCatching { musicRepository.search(query) }.getOrDefault(emptyList())
            _uiState.value = _uiState.value.copy(musics = results)
        }
    }
}
