package com.evertide.riptide.ui.artists

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.evertide.riptide.data.model.Artist
import com.evertide.riptide.data.repository.ArtistRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class ArtistsUiState(
        val artists: List<Artist> = emptyList(),
        val query: String = "",
        val loading: Boolean = true,
)

@HiltViewModel
class ArtistsViewModel
@Inject
constructor(
        private val artistRepository: ArtistRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ArtistsUiState())
    val uiState: StateFlow<ArtistsUiState> = _uiState

    private var allArtists: List<Artist> = emptyList()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(loading = true)
            allArtists = runCatching { artistRepository.getAllArtists() }.getOrDefault(emptyList())
            _uiState.value = _uiState.value.copy(artists = allArtists, loading = false)
        }
    }

    fun onQueryChange(query: String) {
        _uiState.value = _uiState.value.copy(query = query)
        viewModelScope.launch {
            if (query.isBlank()) {
                _uiState.value = _uiState.value.copy(artists = allArtists)
                return@launch
            }
            val results = runCatching { artistRepository.search(query) }.getOrDefault(emptyList())
            _uiState.value = _uiState.value.copy(artists = results)
        }
    }
}
