package com.evertide.riptide.data.repository

import com.evertide.riptide.data.remote.ApiService
import com.evertide.riptide.util.NotificationHelper
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

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
) {
    // App-lifetime scope: polling survives navigating away from Settings or backgrounding the app,
    // same pattern as DownloadsRepository. Only stops if the process itself is killed.
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val _consumeProgress = MutableStateFlow(LibraryJobProgress())
    val consumeProgress: StateFlow<LibraryJobProgress> = _consumeProgress

    private val _resyncProgress = MutableStateFlow(LibraryJobProgress())
    val resyncProgress: StateFlow<LibraryJobProgress> = _resyncProgress

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
            val job = runCatching { api.getConsumeStatus(jobId) }.getOrNull() ?: break
            if (job.status == "done" || job.status == "failed") {
                val total = job.total ?: 0
                _consumeProgress.value =
                        LibraryJobProgress(running = false, processed = total, total = total)
                notificationHelper.notifyConsumeFinished(
                        success = job.status == "done",
                        processed = total
                )
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

    fun startResync() {
        if (_resyncProgress.value.running) return
        scope.launch {
            val jobId =
                    runCatching {
                                api.startResync(
                                                com.evertide.riptide.data.model.ResyncRequest(false)
                                        )
                                        .jobId
                            }
                            .getOrNull()
                            ?: return@launch
            _resyncProgress.value = LibraryJobProgress(running = true)
            pollResync(jobId)
        }
    }

    private suspend fun pollResync(jobId: String) {
        while (true) {
            val job = runCatching { api.getResyncStatus(jobId) }.getOrNull() ?: break
            if (job.status == "done" || job.status == "failed") {
                val total = job.total ?: 0
                _resyncProgress.value =
                        LibraryJobProgress(running = false, processed = total, total = total)
                notificationHelper.notifyResyncFinished(
                        success = job.status == "done",
                        processed = total
                )
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
}
