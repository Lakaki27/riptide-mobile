package com.evertide.riptide.ui.player

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil3.ImageLoader
import com.evertide.riptide.util.getDominantColor
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class ExpandedPlayerViewModel
@Inject
constructor(
        @ApplicationContext private val context: Context,
        private val imageLoader: ImageLoader,
) : ViewModel() {
    private val _ambientColor = MutableStateFlow<Color?>(null)
    val ambientColor: StateFlow<Color?> = _ambientColor

    fun loadAmbientColor(url: String?) {
        viewModelScope.launch { _ambientColor.value = getDominantColor(context, imageLoader, url) }
    }
}
