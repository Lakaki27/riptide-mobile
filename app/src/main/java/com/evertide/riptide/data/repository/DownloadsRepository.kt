package com.evertide.riptide.data.repository

import com.evertide.riptide.data.model.DownloadPreviewRequest
import com.evertide.riptide.data.model.DownloadPreviewResponse
import com.evertide.riptide.data.model.DownloadStartRequest
import com.evertide.riptide.data.remote.ApiService
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
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import okhttp3.MultipartBody

enum class DownloadStatus {
    DOWNLOADING,
    PROCESSING,
    UPLOADING,
    DONE,
    FAILED
}

data class DownloadJob(
        val id: String,
        val label: String,
        val status: DownloadStatus = DownloadStatus.DOWNLOADING,
        val error: String? = null,
)

@Singleton
class DownloadsRepository
@Inject
constructor(
        private val api: ApiService,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val _jobs = MutableStateFlow<List<DownloadJob>>(emptyList())
    val jobs: StateFlow<List<DownloadJob>> = _jobs

    /**
     * Emits once per job that finishes successfully — HomeViewModel reloads the library on this.
     */
    private val _completedEvents = MutableSharedFlow<Unit>(extraBufferCapacity = 4)
    val completedEvents: SharedFlow<Unit> = _completedEvents

    suspend fun preview(url: String): DownloadPreviewResponse =
            api.previewDownload(DownloadPreviewRequest(url))

    suspend fun start(url: String, title: String? = null, artist: String? = null): Boolean {
        return try {
            val jobId = api.startDownload(DownloadStartRequest(url, title, artist)).jobId
            _jobs.update {
                it + DownloadJob(id = jobId, label = title?.takeIf { t -> t.isNotBlank() } ?: url)
            }
            scope.launch { poll(jobId) }
            true
        } catch (e: Throwable) {
            false
        }
    }

    fun trackExisting(jobId: String, label: String) {
        _jobs.update { it + DownloadJob(id = jobId, label = label) }
        scope.launch { poll(jobId) }
    }

    suspend fun uploadFiles(parts: List<MultipartBody.Part>, labels: List<String>): Boolean {
        return try {
            val jobIds = api.uploadFiles(parts).jobIds
            jobIds.forEachIndexed { i, jobId ->
                trackExisting(jobId, labels.getOrElse(i) { "Upload" })
            }
            true
        } catch (e: Throwable) {
            false
        }
    }

    fun dismiss(jobId: String) {
        _jobs.update { jobs -> jobs.filterNot { it.id == jobId } }
    }

    private suspend fun poll(jobId: String, retriesLeft: Int = 3) {
        try {
            val job = api.getDownloadStatus(jobId)
            val status =
                    when (job.status) {
                        "processing" -> DownloadStatus.PROCESSING
                        "uploading" -> DownloadStatus.UPLOADING
                        "done" -> DownloadStatus.DONE
                        "failed" -> DownloadStatus.FAILED
                        else -> DownloadStatus.DOWNLOADING
                    }
            _jobs.update { jobs ->
                jobs.map { if (it.id == jobId) it.copy(status = status, error = job.error) else it }
            }

            if (status == DownloadStatus.DONE || status == DownloadStatus.FAILED) {
                if (status == DownloadStatus.DONE) _completedEvents.tryEmit(Unit)
                scope.launch {
                    delay(4000)
                    dismiss(jobId)
                }
                return
            }
            delay(1500)
            poll(jobId, 3)
        } catch (t: Throwable) {
            if (retriesLeft > 0) {
                delay(1500)
                poll(jobId, retriesLeft - 1)
                return
            }
            _jobs.update { jobs ->
                jobs.map {
                    if (it.id == jobId)
                            it.copy(status = DownloadStatus.FAILED, error = "lost connection")
                    else it
                }
            }
        }
    }
}
