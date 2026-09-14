package com.evertide.riptide.data.model

import kotlinx.serialization.Serializable

@Serializable data class ConfigResponse(val authEnabled: Boolean)

@Serializable data class LoginRequest(val email: String, val password: String)

@Serializable
data class LoginResponse(
        val accessToken: String? = null,
        val refreshToken: String? = null,
        val needsPasswordReset: Boolean = false,
        val resetToken: String? = null,
)

@Serializable data class TokensResponse(val accessToken: String, val refreshToken: String)

@Serializable data class RefreshRequest(val refreshToken: String)

@Serializable data class CompleteResetRequest(val resetToken: String, val newPassword: String)

@Serializable data class PasswordPolicyResponse(val minLength: Int)

@Serializable
data class ChangePasswordRequest(val currentPassword: String, val newPassword: String)

@Serializable
data class Me(
        val id: String,
        val email: String,
        val role: String,
        val theme: String,
        val language: String,
)

@Serializable data class UpdateMeRequest(val theme: String? = null, val language: String? = null)

@Serializable
data class AdminUser(
        val id: String,
        val email: String,
        val role: String,
        val mustResetPassword: Boolean,
        val createdAt: String,
)

@Serializable data class CreateUserRequest(val email: String, val role: String)

@Serializable data class CredentialResponse(val email: String, val password: String)

@Serializable data class ResetOnlyResponse(val password: String)

@Serializable data class JobIdResponse(val jobId: String)

@Serializable
data class JobStatusResponse(
        val status: String,
        val processed: Int? = null,
        val total: Int? = null,
        val error: String? = null,
)

@Serializable
data class DownloadStartRequest(
        val url: String,
        val title: String? = null,
        val artist: String? = null,
)

@Serializable data class DownloadPreviewRequest(val url: String)

@Serializable
data class DownloadPreviewResponse(
        val title: String,
        val artist: String,
        val thumbnailUrl: String? = null,
)

@Serializable data class UploadResponse(val jobIds: List<String>)

@Serializable data class AddSongRequest(val songId: String)

@Serializable data class CreatePlaylistRequest(val name: String)

@Serializable data class HeatmapDay(val date: String, val count: Int)

@Serializable
data class HeatmapResponse(
        val results: List<HeatmapDay>,
        val startDate: String,
        val endDate: String
)

@Serializable data class TopSong(val musicId: String, val title: String, val playCount: String)

@Serializable data class TopSongsResponse(val results: List<TopSong>)

@Serializable data class TopArtist(val artistId: String, val name: String, val playCount: String)

@Serializable data class TopArtistsResponse(val results: List<TopArtist>)

@Serializable data class EarliestPlayResponse(val date: String? = null)

@Serializable data class RecordPlayRequest(val musicId: String)

@Serializable data class StreamUrlResponse(val url: String)

@Serializable data class ResyncRequest(val regenerateThumbnails: Boolean)

@Serializable data class ErrorResponse(val error: String? = null)

@Serializable
data class ArtistDetail(
        val id: String,
        val name: String,
        val createdAt: String,
        val musics: List<Music>,
        val total: Int,
)
