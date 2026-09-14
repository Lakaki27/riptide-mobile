package com.evertide.riptide.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.evertide.riptide.data.local.PreferencesManager
import com.evertide.riptide.data.remote.ApiService
import com.evertide.riptide.navigation.Routes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface RootUiState {
    data object Loading : RootUiState
    data class Ready(val startDestination: String) : RootUiState
}

@HiltViewModel
class RootViewModel @Inject constructor(
    private val prefs: PreferencesManager,
    private val api: ApiService,
) : ViewModel() {

    private val _uiState = MutableStateFlow<RootUiState>(RootUiState.Loading)
    val uiState: StateFlow<RootUiState> = _uiState.asStateFlow()

    init {
        resolveStartDestination()
    }

    private fun resolveStartDestination() {
        viewModelScope.launch {
            val serverUrl = prefs.serverUrl.value
            if (serverUrl.isNullOrBlank()) {
                _uiState.value = RootUiState.Ready(Routes.SERVER_SETUP)
                return@launch
            }

            val authEnabled = runCatching { api.getConfig().authEnabled }.getOrDefault(true)
            val loggedIn = prefs.authState.value.isLoggedIn

            val destination = when {
                authEnabled && !loggedIn -> Routes.AUTH
                else -> Routes.HOME
            }
            _uiState.value = RootUiState.Ready(destination)
        }
    }
}
