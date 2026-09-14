package com.evertide.riptide.ui.serversetup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.evertide.riptide.data.local.PreferencesManager
import com.evertide.riptide.data.remote.ApiService
import com.evertide.riptide.navigation.Routes
import com.evertide.riptide.util.toUserMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import javax.inject.Inject

data class ServerSetupUiState(
    val url: String = "",
    val loading: Boolean = false,
    val error: String? = null,
)

sealed interface ServerSetupEvent {
    data class NavigateTo(val route: String) : ServerSetupEvent
}

@HiltViewModel
class ServerSetupViewModel @Inject constructor(
    private val prefs: PreferencesManager,
    private val api: ApiService,
    private val json: Json,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ServerSetupUiState())
    val uiState: StateFlow<ServerSetupUiState> = _uiState

    private val _events = Channel<ServerSetupEvent>()
    val events = _events.receiveAsFlow()

    fun onUrlChange(value: String) {
        _uiState.value = _uiState.value.copy(url = value, error = null)
    }

    fun submit() {
        val trimmed = _uiState.value.url.trim()
        if (trimmed.isBlank()) {
            _uiState.value = _uiState.value.copy(error = "Enter a server address")
            return
        }
        val withScheme = if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            trimmed
        } else {
            "https://$trimmed"
        }

        _uiState.value = _uiState.value.copy(loading = true, error = null)
        prefs.setServerUrl(withScheme)

        viewModelScope.launch {
            try {
                val authEnabled = api.getConfig().authEnabled
                val nextRoute = if (authEnabled) Routes.AUTH else Routes.HOME
                _events.send(ServerSetupEvent.NavigateTo(nextRoute))
            } catch (t: Throwable) {
                _uiState.value = _uiState.value.copy(
                    loading = false,
                    error = "Could not reach server: ${t.toUserMessage(json)}",
                )
            }
        }
    }
}
