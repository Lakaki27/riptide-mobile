package com.evertide.riptide.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.evertide.riptide.data.local.PreferencesManager
import com.evertide.riptide.data.model.Music
import com.evertide.riptide.data.repository.DownloadsRepository
import com.evertide.riptide.data.repository.LibraryJobsRepository
import com.evertide.riptide.data.repository.MusicRepository
import com.evertide.riptide.data.repository.PlayerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HomeUiState(
        val allMusics: List<Music> = emptyList(),
        val musics: List<Music> = emptyList(),
        val query: String = "",
        val loading: Boolean = true, // first load only (nothing to show yet)
        val refreshing: Boolean = false,
        val searching: Boolean = false,
        val error: Boolean = false,
)

@OptIn(FlowPreview::class)
@HiltViewModel
class HomeViewModel
@Inject
constructor(
        private val musicRepository: MusicRepository,
        val playerRepository: PlayerRepository,
        downloadsRepository: DownloadsRepository,
        libraryJobsRepository: LibraryJobsRepository,
        prefs: PreferencesManager,
) : ViewModel() {

    val isAdmin: StateFlow<Boolean> =
            prefs.authState
                    .map { it.role == "admin" }
                    .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val activeTrackId: StateFlow<String?> =
            playerRepository
                    .state
                    .map { it.currentTrack?.id }
                    .distinctUntilChanged()
                    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState

    private val queryFlow = MutableStateFlow("")
    private var loadJob: Job? = null

    init {
        loadMusics()
        viewModelScope.launch { downloadsRepository.completedEvents.collect { loadMusics() } }
        viewModelScope.launch { libraryJobsRepository.libraryChanged.collect { loadMusics() } }
        viewModelScope.launch {
            queryFlow.debounce(300).distinctUntilChanged().collectLatest { runSearch(it) }
        }
    }

    fun loadMusics() {
        loadJob?.cancel()
        loadJob =
                viewModelScope.launch {
                    _uiState.update {
                        it.copy(
                                loading = it.allMusics.isEmpty(),
                                refreshing = it.allMusics.isNotEmpty(),
                                error = false,
                        )
                    }
                    try {
                        val list = musicRepository.getAllMusics()
                        _uiState.update { s ->
                            s.copy(
                                    allMusics = list,
                                    musics = if (s.query.isBlank()) list else s.musics,
                                    loading = false,
                                    refreshing = false,
                            )
                        }
                        // A search result set holds stale URLs too: re-run it against the new data
                        val q = _uiState.value.query
                        if (q.isNotBlank()) runSearch(q)
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        // keep whatever we already had
                        _uiState.update {
                            it.copy(loading = false, refreshing = false, error = true)
                        }
                    }
                }
    }

    fun onQueryChange(query: String) {
        _uiState.update { it.copy(query = query) }
        queryFlow.value = query
    }

    private suspend fun runSearch(q: String) {
        if (q.isBlank()) {
            _uiState.update { it.copy(musics = it.allMusics, searching = false) }
            return
        }
        _uiState.update { it.copy(searching = true) }
        try {
            val results = musicRepository.search(q)
            _uiState.update { it.copy(musics = results, searching = false) }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            _uiState.update { it.copy(searching = false) } // keep previous results
        }
    }
}
