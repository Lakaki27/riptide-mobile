package com.evertide.riptide.data.repository

import com.evertide.riptide.data.local.PreferencesManager
import com.evertide.riptide.data.model.CompleteResetRequest
import com.evertide.riptide.data.model.LoginRequest
import com.evertide.riptide.data.remote.ApiService
import javax.inject.Inject
import javax.inject.Singleton

sealed interface LoginOutcome {
    data object Success : LoginOutcome
    data class NeedsPasswordReset(val resetToken: String) : LoginOutcome
}

@Singleton
class AuthRepository @Inject constructor(
    private val api: ApiService,
    private val prefs: PreferencesManager,
) {
    suspend fun login(email: String, password: String): LoginOutcome {
        val response = api.login(LoginRequest(email, password))
        return if (response.needsPasswordReset) {
            LoginOutcome.NeedsPasswordReset(response.resetToken!!)
        } else {
            prefs.saveTokens(response.accessToken!!, response.refreshToken!!)
            LoginOutcome.Success
        }
    }

    suspend fun completeReset(resetToken: String, newPassword: String) {
        val tokens = api.completeReset(CompleteResetRequest(resetToken, newPassword))
        prefs.saveTokens(tokens.accessToken, tokens.refreshToken)
    }

    suspend fun getMinPasswordLength(): Int = api.getPasswordPolicy().minLength

    suspend fun getAuthEnabled(): Boolean = api.getConfig().authEnabled

    fun logout() = prefs.clearTokens()
}
