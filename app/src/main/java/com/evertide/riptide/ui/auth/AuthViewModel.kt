package com.evertide.riptide.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.evertide.riptide.data.repository.AuthRepository
import com.evertide.riptide.data.repository.LoginOutcome
import com.evertide.riptide.util.toUserMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import javax.inject.Inject

data class AuthUiState(
    val email: String = "",
    val password: String = "",
    val loading: Boolean = false,
    val error: String? = null,

    val resetToken: String? = null,
    val newPassword: String = "",
    val confirmPassword: String = "",
    val resetLoading: Boolean = false,
    val resetError: String? = null,

    val minPasswordLength: Int = 0,
    val showForgotModal: Boolean = false,
)

sealed interface AuthEvent {
    data object NavigateHome : AuthEvent
}

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val repository: AuthRepository,
    private val json: Json,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState

    private val _events = Channel<AuthEvent>()
    val events = _events.receiveAsFlow()

    init {
        viewModelScope.launch {
            val minLength = runCatching { repository.getMinPasswordLength() }.getOrDefault(0)
            _uiState.value = _uiState.value.copy(minPasswordLength = minLength)
        }
    }

    fun onEmailChange(value: String) { _uiState.value = _uiState.value.copy(email = value) }
    fun onPasswordChange(value: String) { _uiState.value = _uiState.value.copy(password = value) }
    fun onNewPasswordChange(value: String) { _uiState.value = _uiState.value.copy(newPassword = value) }
    fun onConfirmPasswordChange(value: String) { _uiState.value = _uiState.value.copy(confirmPassword = value) }
    fun setForgotModalVisible(visible: Boolean) { _uiState.value = _uiState.value.copy(showForgotModal = visible) }

    fun submitLogin() {
        val state = _uiState.value
        _uiState.value = state.copy(loading = true, error = null)

        viewModelScope.launch {
            try {
                when (val outcome = repository.login(state.email, state.password)) {
                    is LoginOutcome.Success -> {
                        _uiState.value = _uiState.value.copy(loading = false)
                        _events.send(AuthEvent.NavigateHome)
                    }
                    is LoginOutcome.NeedsPasswordReset -> {
                        _uiState.value = _uiState.value.copy(
                            loading = false,
                            resetToken = outcome.resetToken,
                        )
                    }
                }
            } catch (t: Throwable) {
                _uiState.value = _uiState.value.copy(
                    loading = false,
                    error = t.toUserMessage(json, "Invalid email or password"),
                )
            }
        }
    }

    fun submitReset() {
        val state = _uiState.value
        val resetToken = state.resetToken ?: return

        if (state.minPasswordLength > 0 && state.newPassword.length < state.minPasswordLength) {
            _uiState.value = state.copy(
                resetError = "Password must be at least ${state.minPasswordLength} characters",
            )
            return
        }
        if (state.newPassword != state.confirmPassword) {
            _uiState.value = state.copy(resetError = "Passwords do not match")
            return
        }

        _uiState.value = state.copy(resetLoading = true, resetError = null)

        viewModelScope.launch {
            try {
                repository.completeReset(resetToken, state.newPassword)
                _uiState.value = _uiState.value.copy(resetLoading = false)
                _events.send(AuthEvent.NavigateHome)
            } catch (t: Throwable) {
                _uiState.value = _uiState.value.copy(
                    resetLoading = false,
                    resetError = t.toUserMessage(json, "Failed to reset password"),
                )
            }
        }
    }
}
