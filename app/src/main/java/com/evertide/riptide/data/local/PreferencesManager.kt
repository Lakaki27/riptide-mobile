package com.evertide.riptide.data.local

import android.content.Context
import android.util.Base64
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonPrimitive
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

data class AuthState(
    val accessToken: String? = null,
    val refreshToken: String? = null,
    val role: String? = null,
) {
    val isLoggedIn get() = accessToken != null && refreshToken != null
}

@Singleton
class PreferencesManager @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs = EncryptedSharedPreferences.create(
        context,
        "riptide_secure_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
    )

    private val _authState = MutableStateFlow(loadAuthState())
    val authState: StateFlow<AuthState> = _authState

    private val _serverUrl = MutableStateFlow(prefs.getString(KEY_SERVER_URL, null))
    val serverUrl: StateFlow<String?> = _serverUrl

    // --- Server URL ---

    fun setServerUrl(url: String) {
        val normalized = url.trim().trimEnd('/')
        prefs.edit().putString(KEY_SERVER_URL, normalized).apply()
        _serverUrl.value = normalized
    }

    fun getServerUrlSync(): String? = prefs.getString(KEY_SERVER_URL, null)

    // --- Auth ---

    private fun loadAuthState(): AuthState {
        val access = prefs.getString(KEY_ACCESS, null)
        val refresh = prefs.getString(KEY_REFRESH, null)
        val role = prefs.getString(KEY_ROLE, null)
        return AuthState(access, refresh, role)
    }

    fun saveTokens(accessToken: String, refreshToken: String) {
        val role = decodeRole(accessToken)
        prefs.edit()
            .putString(KEY_ACCESS, accessToken)
            .putString(KEY_REFRESH, refreshToken)
            .putString(KEY_ROLE, role)
            .apply()
        _authState.value = AuthState(accessToken, refreshToken, role)
    }

    fun clearTokens() {
        prefs.edit()
            .remove(KEY_ACCESS)
            .remove(KEY_REFRESH)
            .remove(KEY_ROLE)
            .apply()
        _authState.value = AuthState()
    }

    fun getAccessTokenSync(): String? = prefs.getString(KEY_ACCESS, null)
    fun getRefreshTokenSync(): String? = prefs.getString(KEY_REFRESH, null)

    private fun decodeRole(token: String): String? = runCatching {
        val payload = token.split(".")[1]
        val decoded = Base64.decode(payload, Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP)
        JSONObject(String(decoded)).optString("role", null)
    }.getOrNull()

    companion object {
        private const val KEY_SERVER_URL = "server_url"
        private const val KEY_ACCESS = "access_token"
        private const val KEY_REFRESH = "refresh_token"
        private const val KEY_ROLE = "role"
    }
}
