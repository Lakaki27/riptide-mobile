package com.evertide.riptide.di

import android.content.Context
import coil3.ImageLoader
import coil3.disk.DiskCache
import coil3.memory.MemoryCache
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import com.evertide.riptide.data.image.StableKeyInterceptor
import com.evertide.riptide.data.local.CachedImageDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import okhttp3.OkHttpClient
import okio.Path.Companion.toOkioPath

private val ForceMonthCache =
        okhttp3.Interceptor { chain ->
            val r = chain.proceed(chain.request())
            if (!r.isSuccessful) r
            else
                    r.newBuilder()
                            .removeHeader("Pragma")
                            .removeHeader("Expires")
                            .removeHeader("Vary")
                            .header("Cache-Control", "public, max-age=2592000") // 30 days
                            .build()
        }

@Module
@InstallIn(SingletonComponent::class)
object ImageLoaderModule {
    @Provides
    @Singleton
    fun provideImageLoader(@ApplicationContext context: Context, dao: CachedImageDao): ImageLoader {
        // Plain client: no host rewrite, no auth header. Both would break presigned signatures.
        val client = OkHttpClient.Builder().addNetworkInterceptor(ForceMonthCache).build()
        return ImageLoader.Builder(context)
                .components {
                    add(StableKeyInterceptor(dao))
                    add(OkHttpNetworkFetcherFactory(callFactory = { client }))
                }
                .memoryCache { MemoryCache.Builder().maxSizePercent(context, 0.25).build() }
                .diskCache {
                    DiskCache.Builder()
                            .directory(
                                    context.filesDir.resolve("thumbnail_cache").toOkioPath()
                            ) // cacheDir can be purged by the OS
                            .maxSizeBytes(300L * 1024 * 1024)
                            .build()
                }
                .build()
    }
}
