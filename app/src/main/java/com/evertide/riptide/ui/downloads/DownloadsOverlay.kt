package com.evertide.riptide.ui.downloads

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.evertide.riptide.data.repository.DownloadStatus

@Composable
fun DownloadsOverlay(viewModel: DownloadsOverlayViewModel = hiltViewModel()) {
    val jobs by viewModel.jobs.collectAsState()
    if (jobs.isEmpty()) return

    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomEnd) {
        Column(
                modifier = Modifier.padding(bottom = 96.dp, end = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            jobs.forEach { job ->
                Row(
                        modifier =
                                Modifier.width(280.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(MaterialTheme.colorScheme.surface)
                                        .padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                                "Running download job",
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                                statusLabel(job.status) + (job.error?.let { ": $it" } ?: ""),
                                style = MaterialTheme.typography.bodyMedium,
                                color =
                                        if (job.status == DownloadStatus.FAILED)
                                                MaterialTheme.colorScheme.error
                                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    if (job.status != DownloadStatus.DONE && job.status != DownloadStatus.FAILED) {
                        CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp
                        )
                    } else {
                        IconButton(
                                onClick = { viewModel.dismiss(job.id) },
                                modifier = Modifier.size(20.dp)
                        ) { Icon(Icons.Filled.Close, contentDescription = "Dismiss") }
                    }
                }
            }
        }
    }
}

private fun statusLabel(status: DownloadStatus): String =
        when (status) {
            DownloadStatus.DOWNLOADING -> "Downloading"
            DownloadStatus.PROCESSING -> "Processing"
            DownloadStatus.UPLOADING -> "Uploading"
            DownloadStatus.DONE -> "Added to Library"
            DownloadStatus.FAILED -> "Failed"
        }
