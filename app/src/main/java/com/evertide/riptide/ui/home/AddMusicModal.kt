package com.evertide.riptide.ui.home

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil3.compose.AsyncImage
import com.evertide.riptide.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddMusicModal(
        onClose: () -> Unit,
        viewModel: AddMusicViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    var showLegalDialog by remember { mutableStateOf(false) }

    val filePickerLauncher =
            rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.OpenMultipleDocuments(),
            ) { uris -> if (uris.isNotEmpty()) viewModel.addFiles(uris) }

    ModalBottomSheet(
            onDismissRequest = {
                viewModel.resetState()
                onClose()
            }
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                            stringResource(R.string.add_music),
                            style = MaterialTheme.typography.titleMedium
                    )
                    IconButton(onClick = { showLegalDialog = true }) {
                        Icon(
                                Icons.Filled.Info,
                                contentDescription = stringResource(R.string.legal_disclaimer_view)
                        )
                    }
                }
                TextButton(
                        onClick = {
                            viewModel.resetState()
                            onClose()
                        }
                ) { Text(stringResource(R.string.close)) }
            }

            Spacer(Modifier.height(8.dp))
            SegmentedModeSwitch(state.mode, viewModel::setMode)
            Spacer(Modifier.height(12.dp))

            if (state.mode == AddMusicMode.URL) {
                UrlModeContent(state, viewModel, onClose)
            } else {
                FileModeContent(state, viewModel, onClose) {
                    filePickerLauncher.launch(arrayOf("audio/*"))
                }
            }
        }
    }

    if (showLegalDialog) {
        AlertDialog(
                onDismissRequest = { showLegalDialog = false },
                title = { Text(stringResource(R.string.legal_disclaimer_download_title)) },
                text = { Text(stringResource(R.string.legal_disclaimer_download_text)) },
                confirmButton = {
                    TextButton(onClick = { showLegalDialog = false }) {
                        Text(stringResource(R.string.got_it))
                    }
                },
        )
    }
}

@Composable
private fun SegmentedModeSwitch(mode: AddMusicMode, onSelect: (AddMusicMode) -> Unit) {
    Row(
            modifier =
                    Modifier.fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surface)
                            .padding(2.dp),
    ) {
        listOf(
                        AddMusicMode.URL to stringResource(R.string.from_url),
                        AddMusicMode.FILE to stringResource(R.string.upload_files)
                )
                .forEach { (value, label) ->
                    val selected = mode == value
                    Box(
                            modifier =
                                    Modifier.weight(1f)
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(
                                                    if (selected) MaterialTheme.colorScheme.primary
                                                    else Color.Transparent
                                            )
                                            .clickable { onSelect(value) }
                                            .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center,
                    ) {
                        Text(
                                label,
                                color =
                                        if (selected) MaterialTheme.colorScheme.onPrimary
                                        else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
    }
}

@Composable
private fun UrlModeContent(
        state: AddMusicUiState,
        viewModel: AddMusicViewModel,
        onClose: () -> Unit
) {
    when (state.step) {
        AddMusicStep.INPUT ->
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                            value = state.url,
                            onValueChange = viewModel::onUrlChange,
                            placeholder = { Text("https://...") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                    )
                    state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                    Button(onClick = viewModel::submitUrl, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.fetch_info))
                    }
                }
        AddMusicStep.LOADING ->
                Box(Modifier.fillMaxWidth().height(160.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
        AddMusicStep.REVIEW ->
                Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
                ) {
                    state.previewThumbnail?.let {
                        AsyncImage(
                                model = it,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier =
                                        Modifier.fillMaxWidth()
                                                .height(140.dp)
                                                .clip(RoundedCornerShape(8.dp)),
                        )
                    }
                    OutlinedTextField(
                            value = state.previewTitle,
                            onValueChange = viewModel::onPreviewTitleChange,
                            label = { Text(stringResource(R.string.title)) },
                            modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                            value = state.previewArtist,
                            onValueChange = viewModel::onPreviewArtistChange,
                            label = { Text(stringResource(R.string.artist)) },
                            modifier = Modifier.fillMaxWidth(),
                    )
                    state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                    Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                    ) {
                        TextButton(onClick = viewModel::backToInput) {
                            Text(stringResource(R.string.back))
                        }
                        Spacer(Modifier.weight(1f))
                        Button(
                                onClick = { viewModel.confirmUrlDownload(onClose) },
                                enabled = !state.uploading
                        ) {
                            Text(
                                    if (state.uploading) stringResource(R.string.starting)
                                    else stringResource(R.string.start_download)
                            )
                        }
                    }
                }
    }
}

@Composable
private fun FileModeContent(
        state: AddMusicUiState,
        viewModel: AddMusicViewModel,
        onClose: () -> Unit,
        onPickFiles: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(onClick = onPickFiles, modifier = Modifier.fillMaxWidth().height(100.dp)) {
            Text(stringResource(R.string.dragndrop_audio))
        }

        if (state.selectedFiles.isNotEmpty()) {
            LazyColumn(modifier = Modifier.heightIn(max = 160.dp)) {
                itemsIndexed(state.selectedFiles) { index, file ->
                    Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(file.name, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(
                                    viewModel.formatSize(file.sizeBytes),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        IconButton(onClick = { viewModel.removeFile(index) }) {
                            Icon(Icons.Filled.Close, contentDescription = "Remove")
                        }
                    }
                }
            }
        }

        state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }

        Button(
                onClick = { viewModel.uploadFiles(onClose) },
                enabled = !state.uploading && state.selectedFiles.isNotEmpty(),
                modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                    if (state.uploading) stringResource(R.string.uploading)
                    else
                            pluralStringResource(
                                    R.plurals.upload_files_count,
                                    state.selectedFiles.size,
                                    state.selectedFiles.size,
                            )
            )
        }
    }
}
