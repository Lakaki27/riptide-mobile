package com.evertide.riptide.di

import android.content.Context
import coil3.ImageLoader
import coil3.disk.DiskCache
import coil3.memory.MemoryCache
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import okio.Path.Companion.toOkioPath

@Module
@InstallIn(SingletonComponent::class)
object ImageLoaderModule {

    @Provides
    @Singleton
    fun provideImageLoader(@ApplicationContext context: Context): ImageLoader {
        return ImageLoader.Builder(context)
                .components { add(OkHttpNetworkFetcherFactory()) }
                .memoryCache { MemoryCache.Builder().maxSizePercent(context, 0.25).build() }
                .diskCache {
                    DiskCache.Builder()
                            .directory(context.cacheDir.resolve("thumbnail_cache").toOkioPath())
                            .maxSizeBytes(200L * 1024 * 1024) // 200 MB
                            .build()
                }
                .build()
    }
}
