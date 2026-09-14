package com.evertide.riptide.data.remote

import com.evertide.riptide.data.model.*
import okhttp3.MultipartBody
import retrofit2.http.*

interface ApiService {

    @Headers("${AuthHeaderInterceptor.SKIP_AUTH_HEADER}: true")
    @GET("api/config")
    suspend fun getConfig(): ConfigResponse

    // --- Auth ---
    @Headers("${AuthHeaderInterceptor.SKIP_AUTH_HEADER}: true")
    @POST("api/auth/login")
    suspend fun login(@Body body: LoginRequest): LoginResponse

    @Headers("${AuthHeaderInterceptor.SKIP_AUTH_HEADER}: true")
    @POST("api/auth/complete-reset")
    suspend fun completeReset(@Body body: CompleteResetRequest): TokensResponse

    @GET("api/auth/password-policy") suspend fun getPasswordPolicy(): PasswordPolicyResponse

    @GET("api/auth/me") suspend fun getMe(): Me

    @PATCH("api/auth/me") suspend fun updateMe(@Body body: UpdateMeRequest)

    @POST("api/auth/change-password") suspend fun changePassword(@Body body: ChangePasswordRequest)

    @GET("api/auth/users") suspend fun getUsers(): List<AdminUser>

    @POST("api/auth/users")
    suspend fun createUser(@Body body: CreateUserRequest): CredentialResponse

    @POST("api/auth/users/{id}/reset-password")
    suspend fun resetUserPassword(@Path("id") id: String): ResetOnlyResponse

    @DELETE("api/auth/users/{id}") suspend fun deleteUser(@Path("id") id: String)

    // --- Musics ---
    @GET("api/musics") suspend fun getMusics(): List<Music>

    @DELETE("api/musics/{id}") suspend fun deleteMusic(@Path("id") id: String)

    @GET("api/musics/{id}/stream-url")
    suspend fun getStreamUrl(@Path("id") id: String): StreamUrlResponse

    @GET("api/search")
    suspend fun search(
            @Query("type") type: String,
            @Query("q") query: String,
    ): List<Music>

    // --- Artists ---
    @GET("api/artists") suspend fun getArtists(): List<Artist>

    @GET("api/artists/{id}") suspend fun getArtistDetail(@Path("id") id: String): ArtistDetail

    @GET("api/search")
    suspend fun searchArtists(
            @Query("type") type: String = "artist",
            @Query("q") query: String,
    ): List<Artist>

    // --- Playlists ---
    @GET("api/playlists") suspend fun getPlaylists(): List<Playlist>

    @POST("api/playlists") suspend fun createPlaylist(@Body body: CreatePlaylistRequest)

    @GET("api/playlists/{id}") suspend fun getPlaylist(@Path("id") id: String): Playlist

    @POST("api/playlists/{id}")
    suspend fun addSongToPlaylist(@Path("id") id: String, @Body body: AddSongRequest)

    @DELETE("api/playlists/{playlistId}/musics/{musicId}")
    suspend fun removeSongFromPlaylist(
            @Path("playlistId") playlistId: String,
            @Path("musicId") musicId: String,
    )

    // --- Statistics ---
    @GET("api/stats/heatmap")
    suspend fun getHeatmap(
            @Query("startDate") startDate: String,
            @Query("endDate") endDate: String,
            @Query("scope") scope: String,
    ): HeatmapResponse

    @GET("api/stats/top-songs")
    suspend fun getTopSongs(
            @Query("limit") limit: Int,
            @Query("scope") scope: String,
    ): TopSongsResponse

    @GET("api/stats/top-artists")
    suspend fun getTopArtists(
            @Query("limit") limit: Int,
            @Query("scope") scope: String,
    ): TopArtistsResponse

    @GET("api/stats/earliest-play") suspend fun getEarliestPlay(): EarliestPlayResponse

    @POST("api/stats/plays") suspend fun recordPlay(@Body body: RecordPlayRequest)

    // --- Downloads / Uploads ---
    @POST("api/downloads/preview")
    suspend fun previewDownload(@Body body: DownloadPreviewRequest): DownloadPreviewResponse

    @POST("api/downloads")
    suspend fun startDownload(@Body body: DownloadStartRequest): JobIdResponse

    @GET("api/downloads/{id}")
    suspend fun getDownloadStatus(@Path("id") id: String): JobStatusResponse

    @Multipart
    @POST("api/uploads")
    suspend fun uploadFiles(@Part files: List<MultipartBody.Part>): UploadResponse

    // --- Library management ---
    @POST("api/library/consume") suspend fun startConsume(): JobIdResponse

    @GET("api/library/consume/{id}")
    suspend fun getConsumeStatus(@Path("id") id: String): JobStatusResponse

    @POST("api/library/resync") suspend fun startResync(@Body body: ResyncRequest): JobIdResponse

    @GET("api/library/resync/{id}")
    suspend fun getResyncStatus(@Path("id") id: String): JobStatusResponse
}
