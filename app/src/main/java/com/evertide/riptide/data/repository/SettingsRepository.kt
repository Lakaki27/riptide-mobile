package com.evertide.riptide.data.repository

import com.evertide.riptide.data.model.AdminUser
import com.evertide.riptide.data.model.ChangePasswordRequest
import com.evertide.riptide.data.model.CreateUserRequest
import com.evertide.riptide.data.model.CredentialResponse
import com.evertide.riptide.data.model.Me
import com.evertide.riptide.data.model.ResetOnlyResponse
import com.evertide.riptide.data.model.ResyncRequest
import com.evertide.riptide.data.model.UpdateMeRequest
import com.evertide.riptide.data.remote.ApiService
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepository
@Inject
constructor(
        private val api: ApiService,
) {
    suspend fun getMe(): Me = api.getMe()

    suspend fun updateTheme(theme: String) = api.updateMe(UpdateMeRequest(theme = theme))

    suspend fun updateLanguage(language: String) =
            api.updateMe(UpdateMeRequest(language = language))

    suspend fun getMinPasswordLength(): Int = api.getPasswordPolicy().minLength

    suspend fun changePassword(currentPassword: String, newPassword: String) =
            api.changePassword(ChangePasswordRequest(currentPassword, newPassword))

    suspend fun getUsers(): List<AdminUser> = api.getUsers()

    suspend fun createUser(email: String, role: String): CredentialResponse =
            api.createUser(CreateUserRequest(email, role))

    suspend fun resetUserPassword(id: String): ResetOnlyResponse = api.resetUserPassword(id)

    suspend fun deleteUser(id: String) = api.deleteUser(id)

    suspend fun startConsume(): String = api.startConsume().jobId

    suspend fun getConsumeStatus(jobId: String) = api.getConsumeStatus(jobId)

    suspend fun startResync(regenerateThumbnails: Boolean): String =
            api.startResync(ResyncRequest(regenerateThumbnails)).jobId

    suspend fun getResyncStatus(jobId: String) = api.getResyncStatus(jobId)

    suspend fun deleteMusic(id: String) = api.deleteMusic(id)
}
