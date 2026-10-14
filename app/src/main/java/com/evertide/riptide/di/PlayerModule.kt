package com.evertide.riptide.di

import android.content.Context
import androidx.media3.common.util.UnstableApi
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.ResolvingDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.datasource.okhttp.OkHttpDataSource
import com.evertide.riptide.player.StreamUrlResolver
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.io.File
import javax.inject.Named
import javax.inject.Singleton
import okhttp3.OkHttpClient

@UnstableApi
@Module
@InstallIn(SingletonComponent::class)
object PlayerModule {
    @Provides
    @Singleton
    @Named("media")
    fun mediaClient(): OkHttpClient =
            OkHttpClient.Builder().build() // NOT the API client (host rewrite + auth)

    @Provides
    @Singleton
    fun audioCache(@ApplicationContext c: Context): SimpleCache =
            SimpleCache(
                    File(c.filesDir, "audio_cache"),
                    LeastRecentlyUsedCacheEvictor(1L shl 30),
                    StandaloneDatabaseProvider(c)
            )

    @Provides
    @Singleton
    fun cacheDataSourceFactory(
            cache: SimpleCache,
            @Named("media") client: OkHttpClient,
            resolver: StreamUrlResolver,
    ): CacheDataSource.Factory =
            CacheDataSource.Factory()
                    .setCache(cache)
                    .setUpstreamDataSourceFactory(
                            ResolvingDataSource.Factory(OkHttpDataSource.Factory(client), resolver)
                    )
                    .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
}
