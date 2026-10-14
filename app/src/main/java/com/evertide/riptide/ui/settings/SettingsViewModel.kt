package com.evertide.riptide.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.evertide.riptide.data.local.PreferencesManager
import com.evertide.riptide.data.local.ThemePreference
import com.evertide.riptide.data.local.ThemePreferenceStore
import com.evertide.riptide.data.model.AdminUser
import com.evertide.riptide.data.model.Music
import com.evertide.riptide.data.repository.LibraryJobsRepository
import com.evertide.riptide.data.repository.MusicRepository
import com.evertide.riptide.data.repository.SettingsRepository
import com.evertide.riptide.ui.common.ToastBus
import com.evertide.riptide.util.toUserMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

data class SettingsUiState(
        val language: String = "en",
        val minPasswordLength: Int = 0,
        val currentPassword: String = "",
        val newPassword: String = "",
        val confirmPassword: String = "",
        val passwordError: String? = null,
        val users: List<AdminUser> = emptyList(),
        val showCreateUserModal: Boolean = false,
        val newUserEmail: String = "",
        val newUserRole: String = "user",
        val credentialModal: Pair<String, String>? = null, // email, password
        val confirmDeleteUser: AdminUser? = null,
        val confirmResetUser: AdminUser? = null,
        val showDeleteSongModal: Boolean = false,
        val songQuery: String = "",
        val songResults: List<Music> = emptyList(),
        val confirmDeleteSong: Music? = null,
)

@HiltViewModel
class SettingsViewModel
@Inject
constructor(
        private val settingsRepository: SettingsRepository,
        private val musicRepository: MusicRepository,
        private val themePreferenceStore: ThemePreferenceStore,
        private val prefs: PreferencesManager,
        private val toastBus: ToastBus,
        private val json: Json,
        val libraryJobsRepository: LibraryJobsRepository, // new
) : ViewModel() {

    val isAdmin: StateFlow<Boolean> =
            prefs.authState
                    .map { it.role == "admin" }
                    .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val themePreference: StateFlow<ThemePreference> =
            themePreferenceStore.theme.stateIn(
                    viewModelScope,
                    SharingStarted.Eagerly,
                    ThemePreference.SYSTEM
            )

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState

    init {
        viewModelScope.launch {
            val minLength =
                    runCatching { settingsRepository.getMinPasswordLength() }.getOrDefault(0)
            _uiState.value = _uiState.value.copy(minPasswordLength = minLength)

            val me = runCatching { settingsRepository.getMe() }.getOrNull()
            me?.let { _uiState.value = _uiState.value.copy(language = it.language) }

            if (isAdmin.value) loadUsers()
        }
    }

    // --- Preferences ---

    fun setTheme(theme: ThemePreference) {
        viewModelScope.launch {
            themePreferenceStore.setTheme(theme)
            runCatching { settingsRepository.updateTheme(theme.name.lowercase()) }
            toastBus.show("Theme updated")
        }
    }

    fun setLanguage(language: String) {
        _uiState.value = _uiState.value.copy(language = language)
        viewModelScope.launch {
            runCatching { settingsRepository.updateLanguage(language) }
            toastBus.show("Language updated")
        }
    }

    // --- Password change ---

    fun onCurrentPasswordChange(v: String) {
        _uiState.value = _uiState.value.copy(currentPassword = v)
    }
    fun onNewPasswordChange(v: String) {
        _uiState.value = _uiState.value.copy(newPassword = v)
    }
    fun onConfirmPasswordChange(v: String) {
        _uiState.value = _uiState.value.copy(confirmPassword = v)
    }

    fun submitPasswordChange() {
        val state = _uiState.value
        if (state.minPasswordLength > 0 && state.newPassword.length < state.minPasswordLength) {
            _uiState.value =
                    state.copy(
                            passwordError =
                                    "New password must be at least ${state.minPasswordLength} characters"
                    )
            return
        }
        if (state.newPassword != state.confirmPassword) {
            _uiState.value = state.copy(passwordError = "Passwords do not match")
            return
        }
        viewModelScope.launch {
            try {
                settingsRepository.changePassword(state.currentPassword, state.newPassword)
                _uiState.value =
                        _uiState.value.copy(
                                currentPassword = "",
                                newPassword = "",
                                confirmPassword = "",
                                passwordError = null,
                        )
                toastBus.show("Password changed")
            } catch (t: Throwable) {
                _uiState.value =
                        _uiState.value.copy(
                                passwordError = t.toUserMessage(json, "Failed to change password")
                        )
            }
        }
    }

    // --- Admin: users ---

    fun loadUsers() {
        viewModelScope.launch {
            _uiState.value =
                    _uiState.value.copy(
                            users =
                                    runCatching { settingsRepository.getUsers() }
                                            .getOrDefault(emptyList())
                    )
        }
    }

    fun setCreateUserModalVisible(visible: Boolean) {
        _uiState.value = _uiState.value.copy(showCreateUserModal = visible)
    }
    fun onNewUserEmailChange(v: String) {
        _uiState.value = _uiState.value.copy(newUserEmail = v)
    }
    fun onNewUserRoleChange(v: String) {
        _uiState.value = _uiState.value.copy(newUserRole = v)
    }

    fun submitCreateUser() {
        val state = _uiState.value
        if (state.newUserEmail.isBlank()) return
        viewModelScope.launch {
            val result =
                    runCatching {
                                settingsRepository.createUser(state.newUserEmail, state.newUserRole)
                            }
                            .getOrNull()
            if (result != null) {
                _uiState.value =
                        _uiState.value.copy(
                                credentialModal = result.email to result.password,
                                newUserEmail = "",
                                newUserRole = "user",
                                showCreateUserModal = false,
                        )
                loadUsers()
            }
        }
    }

    fun setConfirmResetUser(user: AdminUser?) {
        _uiState.value = _uiState.value.copy(confirmResetUser = user)
    }

    fun confirmResetUserPassword() {
        val user = _uiState.value.confirmResetUser ?: return
        viewModelScope.launch {
            try {
                val result = settingsRepository.resetUserPassword(user.id)
                _uiState.value =
                        _uiState.value.copy(
                                credentialModal = user.email to result.password,
                                confirmResetUser = null
                        )
                loadUsers()
            } catch (t: Throwable) {
                toastBus.show(t.toUserMessage(json, "Could not reset password"))
                _uiState.value = _uiState.value.copy(confirmResetUser = null)
            }
        }
    }

    fun setConfirmDeleteUser(user: AdminUser?) {
        _uiState.value = _uiState.value.copy(confirmDeleteUser = user)
    }

    fun confirmDeleteUserAction() {
        val user = _uiState.value.confirmDeleteUser ?: return
        viewModelScope.launch {
            try {
                settingsRepository.deleteUser(user.id)
                toastBus.show("User deleted")
                loadUsers()
            } catch (t: Throwable) {
                toastBus.show(t.toUserMessage(json, "Could not delete user"))
            }
            _uiState.value = _uiState.value.copy(confirmDeleteUser = null)
        }
    }

    fun dismissCredentialModal() {
        _uiState.value = _uiState.value.copy(credentialModal = null)
    }

    fun setDeleteSongModalVisible(visible: Boolean) {
        _uiState.value =
                _uiState.value.copy(
                        showDeleteSongModal = visible,
                        songQuery = "",
                        songResults = emptyList()
                )
    }

    fun onSongQueryChange(query: String) {
        _uiState.value = _uiState.value.copy(songQuery = query)
        viewModelScope.launch {
            if (query.isBlank()) {
                _uiState.value = _uiState.value.copy(songResults = emptyList())
                return@launch
            }
            _uiState.value =
                    _uiState.value.copy(
                            songResults =
                                    runCatching { musicRepository.search(query) }
                                            .getOrDefault(emptyList())
                    )
        }
    }

    fun setConfirmDeleteSong(music: Music?) {
        _uiState.value = _uiState.value.copy(confirmDeleteSong = music)
    }

    fun confirmDeleteSongAction() {
        val music = _uiState.value.confirmDeleteSong ?: return
        viewModelScope.launch {
            runCatching { settingsRepository.deleteMusic(music.id) }
            toastBus.show("Song deleted")
            _uiState.value =
                    _uiState.value.copy(
                            songResults =
                                    _uiState.value.songResults.filterNot { it.id == music.id },
                            confirmDeleteSong = null,
                    )
        }
    }

    // --- Library / thumbnails ---

    fun refetchThumbnails() {
        libraryJobsRepository.refetchThumbnails()
        toastBus.show("Thumbnails cleared, refetching…")
    }

    // --- Logout ---

    fun logout() = prefs.clearTokens()
}
