package com.evertide.riptide.data.repository

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.evertide.riptide.data.model.Music
import com.evertide.riptide.player.PlaybackService
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

enum class PlayMode {
    NORMAL,
    LOOP_SONG,
    LOOP_PLAYLIST,
    RANDOMIZE
}

data class PlayerState(
        val queue: List<Music> = emptyList(),
        val originalQueue: List<Music> = emptyList(),
        val currentIndex: Int = -1,
        val isPlaying: Boolean = false,
        val mode: PlayMode = PlayMode.NORMAL,
        val sourceId: String? = null,
        val playToken: Long =
                0, // bumped on every explicit "play this index" so the service restarts/seeks
) {
    val currentTrack: Music?
        get() = queue.getOrNull(currentIndex)
}

@Singleton
@OptIn(androidx.media3.common.util.UnstableApi::class)
class PlayerRepository
@Inject
constructor(
        @ApplicationContext private val appContext: Context,
) {
    private val _state = MutableStateFlow(PlayerState())
    val state: StateFlow<PlayerState> = _state

    private val _currentPositionMs = MutableStateFlow(0L)
    val currentPositionMs: StateFlow<Long> = _currentPositionMs

    private val _seekRequests = MutableSharedFlow<Long>(extraBufferCapacity = 1)
    val seekRequests: SharedFlow<Long> = _seekRequests

    private fun <T> shuffle(items: List<T>): List<T> = items.shuffled()

    private fun ensureServiceStarted() {
        runCatching {
            ContextCompat.startForegroundService(
                    appContext,
                    Intent(appContext, PlaybackService::class.java)
            )
        }
    }

    private fun PlayerState.jump(index: Int) =
            copy(currentIndex = index, isPlaying = true, playToken = playToken + 1)

    private fun playIndex(index: Int, startService: Boolean = true) {
        if (_state.value.queue.getOrNull(index) == null) return
        if (startService) ensureServiceStarted()
        _currentPositionMs.value = 0L
        _state.update { it.jump(index) }
    }

    private fun advance() {
        val s = _state.value
        val next = s.currentIndex + 1
        when {
            next < s.queue.size -> playIndex(next, startService = false)
            s.mode == PlayMode.LOOP_PLAYLIST -> playIndex(0, startService = false)
            s.mode == PlayMode.RANDOMIZE -> reshuffleAndRestart()
            else -> _state.update { it.copy(isPlaying = false) }
        }
    }

    private fun reshuffleAndRestart() {
        _currentPositionMs.value = 0L
        _state.update { it.copy(queue = shuffle(it.queue)).jump(0) }
    }

    fun setQueue(tracks: List<Music>, startIndex: Int = 0, sourceId: String? = null) {
        if (tracks.isEmpty()) return
        ensureServiceStarted()
        _currentPositionMs.value = 0L
        _state.update { s ->
            val queue = if (s.mode == PlayMode.RANDOMIZE) shuffle(tracks) else tracks
            val startId = tracks.getOrNull(startIndex)?.id
            val idx = queue.indexOfFirst { it.id == startId }.coerceAtLeast(0)
            s.copy(queue = queue, originalQueue = tracks, sourceId = sourceId).jump(idx)
        }
    }

    fun playFromList(tracks: List<Music>, track: Music, sourceId: String) {
        val s = _state.value
        if (s.sourceId == sourceId) {
            val existingIndex = s.queue.indexOfFirst { it.id == track.id }
            if (existingIndex >= 0) {
                playIndex(existingIndex)
                return
            }
        }
        val index = tracks.indexOfFirst { it.id == track.id }
        if (index >= 0) setQueue(tracks, index, sourceId)
    }

    fun playFromSearch(track: Music) {
        ensureServiceStarted()
        _currentPositionMs.value = 0L
        _state.update { s ->
            val existing = s.queue.indexOfFirst { it.id == track.id }
            val current = s.currentTrack
            when {
                existing >= 0 -> s.jump(existing)
                current == null ->
                        s.copy(
                                        queue = listOf(track) + s.queue,
                                        originalQueue = listOf(track) + s.originalQueue,
                                )
                                .jump(0)
                else -> {
                    val queue = s.queue.toMutableList().apply { add(s.currentIndex + 1, track) }
                    val oi = s.originalQueue.indexOfFirst { it.id == current.id }
                    val original =
                            s.originalQueue.toMutableList().apply {
                                add(if (oi >= 0) oi + 1 else size, track)
                            }
                    s.copy(queue = queue, originalQueue = original).jump(s.currentIndex + 1)
                }
            }
        }
    }

    fun addToQueue(track: Music) {
        _state.update {
            if (it.queue.any { existing -> existing.id == track.id }) return@update it
            it.copy(queue = it.queue + track, originalQueue = it.originalQueue + track)
        }
    }

    fun playNext(track: Music) {
        _state.update { s ->
            val currentTrack = s.currentTrack
            val withoutTrack = s.queue.filterNot { it.id == track.id }.toMutableList()
            val newCurrentIndex =
                    currentTrack?.let { ct -> withoutTrack.indexOfFirst { it.id == ct.id } } ?: -1
            val insertAt = newCurrentIndex + 1
            withoutTrack.add(insertAt, track)
            s.copy(
                    queue = withoutTrack,
                    currentIndex = if (newCurrentIndex >= 0) newCurrentIndex else s.currentIndex
            )
        }
    }

    fun sortQueue(tracks: List<Music>, sourceId: String) {
        val s = _state.value
        if (s.mode == PlayMode.RANDOMIZE || s.sourceId != sourceId) return
        val currentTrack = s.currentTrack ?: return
        val idx = tracks.indexOfFirst { it.id == currentTrack.id }
        if (idx < 0) return
        _state.update { it.copy(queue = tracks, originalQueue = tracks, currentIndex = idx) }
    }

    fun jumpTo(index: Int) = playIndex(index)

    fun next() {
        val s = _state.value
        if (s.mode == PlayMode.LOOP_SONG) {
            playIndex(s.currentIndex)
            return
        }
        advance()
    }

    fun skipNext() = advance()

    fun previous() {
        val s = _state.value
        if (s.currentIndex > 0) playIndex(s.currentIndex - 1)
    }

    fun setMode(mode: PlayMode) {
        _state.update { s ->
            when {
                mode == PlayMode.RANDOMIZE -> {
                    val currentTrack = s.currentTrack
                    val shuffled = shuffle(s.queue)
                    val foundIndex =
                            currentTrack?.let { ct -> shuffled.indexOfFirst { it.id == ct.id } }
                                    ?: -1
                    s.copy(
                            mode = mode,
                            queue = shuffled,
                            currentIndex = if (foundIndex == -1) 0 else foundIndex
                    )
                }
                s.mode == PlayMode.RANDOMIZE -> {
                    val currentTrack = s.currentTrack
                    val foundIndex =
                            currentTrack?.let { ct ->
                                s.originalQueue.indexOfFirst { it.id == ct.id }
                            }
                                    ?: -1
                    s.copy(
                            mode = mode,
                            queue = s.originalQueue,
                            currentIndex = if (foundIndex == -1) s.currentIndex else foundIndex
                    )
                }
                else -> s.copy(mode = mode)
            }
        }
    }

    /**
     * Called by PlaybackService when ExoPlayer's actual isPlaying state changes (hardware buttons,
     * notification, etc).
     */
    fun setIsPlaying(isPlaying: Boolean) {
        _state.update { it.copy(isPlaying = isPlaying) }
    }

    fun togglePlay() {
        _state.update { it.copy(isPlaying = !it.isPlaying) }
    }

    /** Called by PlaybackService's progress ticker to keep UI position in sync. */
    fun updatePosition(positionMs: Long) {
        _currentPositionMs.value = positionMs
    }

    /**
     * Called by the UI (e.g. dragging the seek bar); PlaybackService listens and applies it to
     * ExoPlayer.
     */
    fun seekTo(positionMs: Long) {
        _currentPositionMs.value = positionMs
        _seekRequests.tryEmit(positionMs)
    }

    fun onPlayerIndexChanged(index: Int) {
        _currentPositionMs.value = 0L
        _state.update { if (it.currentIndex == index) it else it.copy(currentIndex = index) }
    }

    fun onQueueEnded() {
        if (_state.value.mode == PlayMode.RANDOMIZE) reshuffleAndRestart()
        else _state.update { it.copy(isPlaying = false) }
    }
}
