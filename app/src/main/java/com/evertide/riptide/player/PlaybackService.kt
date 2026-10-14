package com.evertide.riptide.player

import android.app.PendingIntent
import android.content.Intent
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.evertide.riptide.MainActivity
import com.evertide.riptide.data.repository.PlayMode
import com.evertide.riptide.data.repository.PlayerRepository
import com.evertide.riptide.data.repository.PlayerState
import com.evertide.riptide.data.repository.StatsRepository
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@UnstableApi
@AndroidEntryPoint
class PlaybackService : MediaSessionService() {

    @Inject lateinit var playerRepository: PlayerRepository
    @Inject lateinit var statsRepository: StatsRepository
    @Inject lateinit var bitmapLoader: CoilBitmapLoader
    @Inject lateinit var cacheDataSourceFactory: CacheDataSource.Factory
    @Inject lateinit var prefetcher: TrackPrefetcher

    private lateinit var exoPlayer: ExoPlayer
    private var mediaSession: MediaSession? = null
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var progressJob: Job? = null

    private var playRecordedForTrackId: String? = null
    private var appliedQueueIds: List<String> = emptyList()
    private var appliedToken = 0L

    override fun onCreate() {
        super.onCreate()

        exoPlayer =
                ExoPlayer.Builder(this)
                        .setMediaSourceFactory(DefaultMediaSourceFactory(cacheDataSourceFactory))
                        .setLoadControl(
                                DefaultLoadControl.Builder()
                                        .setBufferDurationsMs(30_000, 90_000, 2_000, 4_000)
                                        .build()
                        )
                        .setAudioAttributes(
                                AudioAttributes.Builder()
                                        .setUsage(C.USAGE_MEDIA)
                                        .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                                        .build(),
                                /* handleAudioFocus = */ true
                        )
                        .setHandleAudioBecomingNoisy(true)
                        .build()

        val riptidePlayer = RiptidePlayer(exoPlayer, playerRepository)
        val sessionActivityIntent =
                PendingIntent.getActivity(
                        this,
                        0,
                        Intent(this, MainActivity::class.java),
                        PendingIntent.FLAG_IMMUTABLE
                )

        mediaSession =
                MediaSession.Builder(this, riptidePlayer)
                        .setSessionActivity(sessionActivityIntent)
                        .setBitmapLoader(bitmapLoader)
                        .build()
                        .also { addSession(it) }

        setMediaNotificationProvider(DefaultMediaNotificationProvider(this))

        exoPlayer.addListener(
                object : Player.Listener {
                    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                        if (reason == Player.MEDIA_ITEM_TRANSITION_REASON_PLAYLIST_CHANGED) return
                        playRecordedForTrackId = null
                        playerRepository.onPlayerIndexChanged(exoPlayer.currentMediaItemIndex)
                        prefetchAhead()
                    }
                    // playWhenReady, not isPlaying: isPlaying flips to false on every buffering
                    // stall
                    override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
                        playerRepository.setIsPlaying(playWhenReady)
                    }
                    override fun onPlaybackStateChanged(state: Int) {
                        // Only reached when the whole queue is exhausted.
                        if (state == Player.STATE_ENDED) playerRepository.onQueueEnded()
                    }
                }
        )

        serviceScope.launch { playerRepository.state.collect(::reconcile) }
        serviceScope.launch { playerRepository.seekRequests.collect { exoPlayer.seekTo(it) } }
        startProgressTicker()
    }

    private fun reconcile(s: PlayerState) {
        val player = exoPlayer
        val current = s.currentTrack
        if (current == null) {
            if (player.mediaItemCount > 0) {
                player.stop()
                player.clearMediaItems()
            }
            appliedQueueIds = emptyList()
            return
        }

        player.repeatMode =
                when (s.mode) {
                    PlayMode.LOOP_SONG -> Player.REPEAT_MODE_ONE
                    PlayMode.LOOP_PLAYLIST -> Player.REPEAT_MODE_ALL
                    else -> Player.REPEAT_MODE_OFF
                }

        val ids = s.queue.map { it.id }
        val tokenChanged = s.playToken != appliedToken
        appliedToken = s.playToken
        val sameCurrent = player.currentMediaItem?.mediaId == current.id
        val hardLoad = player.mediaItemCount == 0 || (ids != appliedQueueIds && !sameCurrent)

        if (hardLoad) {
            player.setMediaItems(s.queue.map { it.toMediaItem() }, s.currentIndex, 0L)
            player.prepare()
        } else {
            // Queue edited around the playing track: patch without interrupting it.
            if (ids != appliedQueueIds) patchAround(s)
            if (tokenChanged || player.currentMediaItemIndex != s.currentIndex) {
                player.seekToDefaultPosition(s.currentIndex)
            }
        }
        appliedQueueIds = ids

        // Queue finished and the user hits play: start over
        if (s.isPlaying && player.playbackState == Player.STATE_ENDED)
                player.seekToDefaultPosition(0)
        player.playWhenReady = s.isPlaying
        if (hardLoad) prefetchAhead()
    }

    private fun patchAround(s: PlayerState) {
        val p = exoPlayer
        val cur = p.currentMediaItemIndex
        if (cur + 1 < p.mediaItemCount) p.removeMediaItems(cur + 1, p.mediaItemCount)
        if (cur > 0) p.removeMediaItems(0, cur)
        val before = s.queue.subList(0, s.currentIndex).map { it.toMediaItem() }
        val after = s.queue.subList(s.currentIndex + 1, s.queue.size).map { it.toMediaItem() }
        if (before.isNotEmpty()) p.addMediaItems(0, before)
        if (after.isNotEmpty()) p.addMediaItems(after)
        prefetchAhead()
    }

    /** Pull the next two tracks fully onto disk. */
    private fun prefetchAhead() {
        val p = exoPlayer
        val count = p.mediaItemCount
        val cur = p.currentMediaItemIndex
        if (count == 0 || p.repeatMode == Player.REPEAT_MODE_ONE) return
        for (k in 1..2) {
            val i = cur + k
            val idx =
                    if (i < count) i
                    else if (p.repeatMode == Player.REPEAT_MODE_ALL) i % count else break
            if (idx != cur) prefetcher.prefetch(p.getMediaItemAt(idx).mediaId)
        }
    }

    private fun startProgressTicker() {
        progressJob =
                serviceScope.launch {
                    while (true) {
                        delay(500)
                        if (exoPlayer.isPlaying) {
                            val pos = exoPlayer.currentPosition
                            playerRepository.updatePosition(pos)
                            checkPlayRecording(pos)
                        }
                    }
                }
    }

    private fun checkPlayRecording(positionMs: Long) {
        val track = playerRepository.state.value.currentTrack ?: return
        if (playRecordedForTrackId == track.id) return
        if (positionMs >= track.durationSeconds * 1000L * 0.1) {
            playRecordedForTrackId = track.id
            serviceScope.launch { runCatching { statsRepository.recordPlay(track.id) } }
        }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? =
            mediaSession

    override fun onDestroy() {
        progressJob?.cancel()
        serviceScope.cancel()
        mediaSession?.run {
            removeSession(this)
            player.release()
            release()
            mediaSession = null
        }
        super.onDestroy()
    }
}
