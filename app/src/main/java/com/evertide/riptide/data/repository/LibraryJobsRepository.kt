package com.evertide.riptide.data.repository

import coil3.ImageLoader
import com.evertide.riptide.data.image.ImageCacheManager
import com.evertide.riptide.data.model.ResyncRequest
import com.evertide.riptide.data.remote.ApiService
import com.evertide.riptide.util.NotificationHelper
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class LibraryJobProgress(
        val running: Boolean = false,
        val processed: Int = 0,
        val total: Int = 0
)

@Singleton
class LibraryJobsRepository
@Inject
constructor(
        private val api: ApiService,
        private val notificationHelper: NotificationHelper,
        private val imageCacheManager: ImageCacheManager,
        private val imageLoader: ImageLoader,
) {
    // App-lifetime scope: polling survives navigating away from Settings or backgrounding the app.
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val _consumeProgress = MutableStateFlow(LibraryJobProgress())
    val consumeProgress: StateFlow<LibraryJobProgress> = _consumeProgress

    private val _resyncProgress = MutableStateFlow(LibraryJobProgress())
    val resyncProgress: StateFlow<LibraryJobProgress> = _resyncProgress

    /**
     * Emits when the library list should be reloaded (after a resync, or after the local thumbnail
     * cache was cleared).
     */
    private val _libraryChanged = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val libraryChanged: SharedFlow<Unit> = _libraryChanged

    // ---------------- Consume ----------------

    fun startConsume() {
        if (_consumeProgress.value.running) return
        scope.launch {
            val jobId = runCatching { api.startConsume().jobId }.getOrNull() ?: return@launch
            _consumeProgress.value = LibraryJobProgress(running = true)
            pollConsume(jobId)
        }
    }

    private suspend fun pollConsume(jobId: String) {
        while (true) {
            val job = runCatching { api.getConsumeStatus(jobId) }.getOrNull()
            if (job == null) {
                // status call failed: don't leave the UI stuck on "running"
                _consumeProgress.value = LibraryJobProgress(running = false)
                break
            }
            if (job.status == "done" || job.status == "failed") {
                val total = job.total ?: 0
                _consumeProgress.value =
                        LibraryJobProgress(running = false, processed = total, total = total)
                notificationHelper.notifyConsumeFinished(
                        success = job.status == "done",
                        processed = total
                )
                if (job.status == "done") _libraryChanged.tryEmit(Unit)
                break
            }
            _consumeProgress.value =
                    LibraryJobProgress(
                            running = true,
                            processed = job.processed ?: 0,
                            total = job.total ?: 0
                    )
            delay(1000)
        }
    }

    // ---------------- Resync ----------------

    fun startResync(regenerateThumbnails: Boolean = false) {
        if (_resyncProgress.value.running) return
        scope.launch {
            val jobId =
                    runCatching { api.startResync(ResyncRequest(regenerateThumbnails)).jobId }
                            .getOrNull()
                            ?: return@launch
            _resyncProgress.value = LibraryJobProgress(running = true)
            pollResync(jobId, regenerateThumbnails)
        }
    }

    private suspend fun pollResync(jobId: String, regenerated: Boolean) {
        while (true) {
            val job = runCatching { api.getResyncStatus(jobId) }.getOrNull()
            if (job == null) {
                _resyncProgress.value = LibraryJobProgress(running = false)
                break
            }
            if (job.status == "done" || job.status == "failed") {
                val total = job.total ?: 0
                val success = job.status == "done"
                // Clear BEFORE flipping running=false and before telling the UI to reload
                if (success && regenerated) {
                    runCatching { clearImageCaches() }
                }
                _resyncProgress.value =
                        LibraryJobProgress(running = false, processed = total, total = total)
                notificationHelper.notifyResyncFinished(success = success, processed = total)
                if (success) _libraryChanged.tryEmit(Unit)
                break
            }
            _resyncProgress.value =
                    LibraryJobProgress(
                            running = true,
                            processed = job.processed ?: 0,
                            total = job.total ?: 0
                    )
            delay(1000)
        }
    }

    // ---------------- Local thumbnail refetch ----------------

    /** Wipes local thumbnail caches (disk, memory, Room) and asks the UI to reload fresh URLs. */
    fun refetchThumbnails() {
        scope.launch {
            runCatching { clearImageCaches() }
            _libraryChanged.tryEmit(Unit)
        }
    }

    private suspend fun clearImageCaches() =
            withContext(Dispatchers.IO) { imageCacheManager.clearAll(imageLoader) }
}
