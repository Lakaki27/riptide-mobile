package com.evertide.riptide.player

import android.net.Uri
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.ResolvingDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.CacheWriter
import com.evertide.riptide.data.model.Music
import com.evertide.riptide.data.repository.MusicRepository
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

@UnstableApi
fun Music.toMediaItem(): MediaItem =
        MediaItem.Builder()
                .setMediaId(id)
                .setUri(StreamUrlResolver.uriFor(id))
                .setCustomCacheKey(id)
                .setMediaMetadata(
                        MediaMetadata.Builder()
                                .setTitle(title)
                                .setArtist(artist.name)
                                .setDurationMs(durationSeconds * 1000L)
                                .setArtworkUri(thumbnailUrl?.let(Uri::parse))
                                .build()
                )
                .build()

@UnstableApi
class StreamUrlResolver
@Inject
constructor(
        private val musicRepository: MusicRepository,
) : ResolvingDataSource.Resolver {
    companion object {
        private const val SCHEME = "riptide"
        fun uriFor(id: String): Uri = Uri.parse("$SCHEME://track/$id")
    }

    // Runs on ExoPlayer's loader thread, so blocking is fine.
    override fun resolveDataSpec(dataSpec: DataSpec): DataSpec {
        if (dataSpec.uri.scheme != SCHEME) return dataSpec
        val id = dataSpec.uri.lastPathSegment ?: throw IOException("Bad track uri")
        val url =
                try {
                    runBlocking { musicRepository.getStreamUrl(id) }
                } catch (e: Exception) {
                    throw IOException("Stream URL lookup failed", e)
                }
        return dataSpec.withUri(url.toUri())
    }
}

@UnstableApi
@Singleton
class TrackPrefetcher @Inject constructor(private val factory: CacheDataSource.Factory) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val jobs = ConcurrentHashMap<String, Job>()

    fun prefetch(trackId: String) {
        if (jobs.containsKey(trackId)) return
        jobs[trackId] =
                scope.launch {
                    val spec =
                            DataSpec.Builder()
                                    .setUri(StreamUrlResolver.uriFor(trackId))
                                    .setKey(trackId)
                                    .build()
                    val writer = CacheWriter(factory.createDataSource(), spec, null, null)
                    val h = coroutineContext.job.invokeOnCompletion { writer.cancel() }
                    try {
                        writer.cache()
                    } // skips bytes already cached
                    catch (_: Exception) {
                        /* offline or cancelled: playback just streams */
                    } finally {
                        h.dispose()
                        jobs.remove(trackId)
                    }
                }
    }
}
