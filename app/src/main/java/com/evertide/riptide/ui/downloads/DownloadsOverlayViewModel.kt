package com.evertide.riptide.ui.downloads

import androidx.lifecycle.ViewModel
import com.evertide.riptide.data.repository.DownloadJob
import com.evertide.riptide.data.repository.DownloadsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.StateFlow

@HiltViewModel
class DownloadsOverlayViewModel
@Inject
constructor(
        private val downloadsRepository: DownloadsRepository,
) : ViewModel() {
    val jobs: StateFlow<List<DownloadJob>> = downloadsRepository.jobs
    fun dismiss(jobId: String) = downloadsRepository.dismiss(jobId)
}
