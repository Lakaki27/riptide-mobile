package com.evertide.riptide.player

import android.app.PendingIntent
import android.content.Intent
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.evertide.riptide.MainActivity
import com.evertide.riptide.data.repository.PlayerRepository
import com.evertide.riptide.data.repository.StatsRepository
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@UnstableApi
@AndroidEntryPoint
class PlaybackService : MediaSessionService() {

    @Inject lateinit var playerRepository: PlayerRepository
    @Inject lateinit var statsRepository: StatsRepository
    @Inject lateinit var bitmapLoader: CoilBitmapLoader

    private lateinit var exoPlayer: ExoPlayer
    private var mediaSession: MediaSession? = null

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var progressJob: Job? = null

    private var loadedUrl: String? = null
    private var playRecordedForTrackId: String? = null

    override fun onCreate() {
        super.onCreate()

        exoPlayer = ExoPlayer.Builder(this).build()
        val riptidePlayer = RiptidePlayer(exoPlayer, playerRepository)

        val sessionActivityIntent =
                PendingIntent.getActivity(
                        this,
                        0,
                        Intent(this, MainActivity::class.java),
                        PendingIntent.FLAG_IMMUTABLE,
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
                    override fun onPlaybackStateChanged(playbackState: Int) {
                        if (playbackState == Player.STATE_ENDED) {
                            playerRepository.next()
                        }
                    }

                    override fun onIsPlayingChanged(isPlaying: Boolean) {
                        playerRepository.setIsPlaying(isPlaying)
                    }
                }
        )

        observeRepositoryState()
        observeSeekRequests()
        startProgressTicker()
    }

    private fun observeRepositoryState() {
        serviceScope.launch {
            playerRepository.state.collect { state ->
                val track = state.currentTrack

                if (state.audioUrl != null && state.audioUrl != loadedUrl) {
                    loadedUrl = state.audioUrl
                    playRecordedForTrackId = null

                    val metadata =
                            MediaMetadata.Builder()
                                    .setTitle(track?.title)
                                    .setArtist(track?.artist?.name)
                                    .setArtworkUri(track?.thumbnailUrl?.let { Uri.parse(it) })
                                    .build()

                    val item =
                            MediaItem.Builder()
                                    .setUri(state.audioUrl)
                                    .setMediaMetadata(metadata)
                                    .build()

                    mediaSession?.player?.let { sessionPlayer ->
                        sessionPlayer.setMediaItem(item)
                        sessionPlayer.prepare()
                        if (state.isPlaying) {
                            sessionPlayer.play()
                        }
                    }
                } else if (state.audioUrl == null) {
                    exoPlayer.stop()
                    loadedUrl = null
                }

                when {
                    state.isPlaying &&
                            !exoPlayer.isPlaying &&
                            exoPlayer.playbackState != Player.STATE_IDLE -> exoPlayer.play()
                    !state.isPlaying && exoPlayer.isPlaying -> exoPlayer.pause()
                }
            }
        }
    }

    private fun observeSeekRequests() {
        serviceScope.launch {
            playerRepository.seekRequests.collect { positionMs -> exoPlayer.seekTo(positionMs) }
        }
    }

    private fun startProgressTicker() {
        progressJob =
                serviceScope.launch {
                    while (true) {
                        delay(500)
                        if (exoPlayer.isPlaying) {
                            val positionMs = exoPlayer.currentPosition
                            playerRepository.updatePosition(positionMs)
                            checkPlayRecording(positionMs)
                        }
                    }
                }
    }

    private fun checkPlayRecording(positionMs: Long) {
        val track = playerRepository.state.value.currentTrack ?: return
        if (playRecordedForTrackId == track.id) return
        val thresholdMs = track.durationSeconds * 1000L * 0.1
        if (positionMs >= thresholdMs) {
            playRecordedForTrackId = track.id
            serviceScope.launch { runCatching { statsRepository.recordPlay(track.id) } }
        }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? =
            mediaSession

    override fun onDestroy() {
        progressJob?.cancel()
        mediaSession?.run {
            removeSession(this)
            player.release()
            release()
            mediaSession = null
        }
        super.onDestroy()
    }
}
