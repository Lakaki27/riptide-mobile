package com.evertide.riptide.data.image

import coil3.ImageLoader
import coil3.decode.DataSource
import coil3.intercept.Interceptor
import coil3.request.ImageResult
import coil3.request.SuccessResult
import com.evertide.riptide.data.local.CachedImageDao
import com.evertide.riptide.data.local.CachedImageEntity
import javax.inject.Inject
import javax.inject.Singleton

class StableKeyInterceptor(private val dao: CachedImageDao) : Interceptor {
    override suspend fun intercept(chain: Interceptor.Chain): ImageResult {
        val request = chain.request
        val key = stableKey(request.data) ?: return chain.proceed()
        val keyed =
                request.newBuilder()
                        .memoryCacheKey(request.memoryCacheKey ?: key)
                        .diskCacheKey(request.diskCacheKey ?: key)
                        .build()
        val result = chain.withRequest(keyed).proceed()
        if (result is SuccessResult && result.dataSource == DataSource.NETWORK) {
            runCatching { dao.upsert(CachedImageEntity(key, System.currentTimeMillis())) }
        }
        return result
    }

    /** Only touches presigned URLs; same object => same key whatever the signature. */
    private fun stableKey(data: Any): String? {
        val s =
                when (data) {
                    is String -> data
                    is android.net.Uri -> data.toString()
                    else -> return null
                }
        if (!s.startsWith("http", ignoreCase = true)) return null
        val q = s.indexOf('?')
        if (q < 0 || !s.substring(q).contains("X-Amz-Signature", ignoreCase = true)) return null
        return s.substring(0, q)
    }
}

@Singleton
class ImageCacheManager @Inject constructor(private val dao: CachedImageDao) {
    companion object {
        const val TTL_MS = 30L * 24 * 60 * 60 * 1000
    }

    suspend fun evictExpired(loader: ImageLoader) {
        val cutoff = System.currentTimeMillis() - TTL_MS
        dao.expiredKeys(cutoff).forEach { loader.diskCache?.remove(it) }
        dao.deleteOlderThan(cutoff)
    }

    /** Call after a "regenerate thumbnails" resync, since keys ignore the signature. */
    suspend fun clearAll(loader: ImageLoader) {
        loader.diskCache?.clear()
        loader.memoryCache?.clear()
        dao.deleteAll()
    }
}
