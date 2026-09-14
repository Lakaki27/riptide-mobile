package com.evertide.riptide.data.remote

import com.evertide.riptide.data.local.PreferencesManager
import com.evertide.riptide.data.model.RefreshRequest
import com.evertide.riptide.data.model.TokensResponse
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.Authenticator
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import javax.inject.Inject
import javax.inject.Singleton

/** Placeholder base URL — actual host/scheme is swapped in per-request by [DynamicHostInterceptor]. */
private const val PLACEHOLDER_BASE_URL = "http://riptide.invalid/"

class DynamicHostInterceptor @Inject constructor(
    private val prefs: PreferencesManager,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        val configuredUrl = prefs.getServerUrlSync()
            ?: return chain.proceed(original) // no server configured yet (e.g. during URL entry screen itself)

        val configuredHttpUrl = configuredUrl.toHttpUrlOrNull()
            ?: return chain.proceed(original)

        val newUrl = original.url.newBuilder()
            .scheme(configuredHttpUrl.scheme)
            .host(configuredHttpUrl.host)
            .port(configuredHttpUrl.port)
            .build()

        return chain.proceed(original.newBuilder().url(newUrl).build())
    }
}

class AuthHeaderInterceptor @Inject constructor(
    private val prefs: PreferencesManager,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        if (original.header(SKIP_AUTH_HEADER) != null) {
            val stripped = original.newBuilder().removeHeader(SKIP_AUTH_HEADER).build()
            return chain.proceed(stripped)
        }
        val token = prefs.getAccessTokenSync()
        val request = if (token != null) {
            original.newBuilder().addHeader("Authorization", "Bearer $token").build()
        } else {
            original
        }
        return chain.proceed(request)
    }

    companion object {
        const val SKIP_AUTH_HEADER = "X-Skip-Auth"
    }
}

/**
 * Mirrors the web app's apiFetch: on 401, attempt a refresh using a plain client
 * (no authenticator, to avoid recursion), retry once, else clear tokens.
 */
class TokenAuthenticator @Inject constructor(
    private val prefs: PreferencesManager,
    private val json: Json,
) : Authenticator {
    override fun authenticate(route: Route?, response: Response): Request? {
        if (responseCount(response) >= 2) return null // already retried once
        val refreshToken = prefs.getRefreshTokenSync() ?: run {
            prefs.clearTokens()
            return null
        }

        val configuredUrl = prefs.getServerUrlSync()?.toHttpUrlOrNull() ?: return null
        val refreshUrl = configuredUrl.newBuilder().addPathSegments("api/auth/refresh").build()

        val plainClient = OkHttpClient.Builder().build()
        val body = json.encodeToString(RefreshRequest.serializer(), RefreshRequest(refreshToken))
            .toRequestBody("application/json".toMediaType())

        val refreshRequest = Request.Builder().url(refreshUrl).post(body).build()

        return runCatching {
            plainClient.newCall(refreshRequest).execute().use { refreshResponse ->
                if (!refreshResponse.isSuccessful) {
                    prefs.clearTokens()
                    return null
                }
                val text = refreshResponse.body?.string() ?: return null
                val tokens = json.decodeFromString(TokensResponse.serializer(), text)
                prefs.saveTokens(tokens.accessToken, tokens.refreshToken)
                response.request.newBuilder()
                    .header("Authorization", "Bearer ${tokens.accessToken}")
                    .build()
            }
        }.getOrElse {
            prefs.clearTokens()
            null
        }
    }

    private fun responseCount(response: Response): Int {
        var count = 1
        var prior = response.priorResponse
        while (prior != null) {
            count++
            prior = prior.priorResponse
        }
        return count
    }
}

private fun String.toRequestBody(mediaType: okhttp3.MediaType) =
    okhttp3.RequestBody.create(mediaType, this)

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(
        hostInterceptor: DynamicHostInterceptor,
        authInterceptor: AuthHeaderInterceptor,
        authenticator: TokenAuthenticator,
    ): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }
        return OkHttpClient.Builder()
            .addInterceptor(hostInterceptor)
            .addInterceptor(authInterceptor)
            .authenticator(authenticator)
            .addInterceptor(logging)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(client: OkHttpClient, json: Json): Retrofit {
        val contentType = "application/json".toMediaType()
        return Retrofit.Builder()
            .baseUrl(PLACEHOLDER_BASE_URL)
            .client(client)
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()
    }

    @Provides
    @Singleton
    fun provideApiService(retrofit: Retrofit): ApiService =
        retrofit.create(ApiService::class.java)
}
