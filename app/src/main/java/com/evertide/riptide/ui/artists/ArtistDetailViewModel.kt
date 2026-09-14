package com.evertide.riptide.ui.artists

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.evertide.riptide.data.model.ArtistDetail
import com.evertide.riptide.data.repository.ArtistRepository
import com.evertide.riptide.data.repository.PlayerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class ArtistDetailViewModel
@Inject
constructor(
        private val artistRepository: ArtistRepository,
        val playerRepository: PlayerRepository,
        savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val artistId: String = checkNotNull(savedStateHandle["artistId"])

    private val _artist = MutableStateFlow<ArtistDetail?>(null)
    val artist: StateFlow<ArtistDetail?> = _artist

    init {
        viewModelScope.launch {
            _artist.value = runCatching { artistRepository.getArtistDetail(artistId) }.getOrNull()
        }
    }

    fun playAll() {
        _artist.value?.let { playerRepository.setQueue(it.musics, 0) }
    }
}
