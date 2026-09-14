package com.evertide.riptide.ui.home

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.evertide.riptide.data.repository.DownloadsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody

enum class AddMusicMode {
    URL,
    FILE
}

enum class AddMusicStep {
    INPUT,
    LOADING,
    REVIEW
}

data class PickedFile(val uri: Uri, val name: String, val sizeBytes: Long)

data class AddMusicUiState(
        val mode: AddMusicMode = AddMusicMode.URL,
        val url: String = "",
        val step: AddMusicStep = AddMusicStep.INPUT,
        val previewTitle: String = "",
        val previewArtist: String = "",
        val previewThumbnail: String? = null,
        val error: String? = null,
        val selectedFiles: List<PickedFile> = emptyList(),
        val uploading: Boolean = false,
)

@HiltViewModel
class AddMusicViewModel
@Inject
constructor(
        private val downloadsRepository: DownloadsRepository,
        @ApplicationContext private val context: Context,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddMusicUiState())
    val uiState: StateFlow<AddMusicUiState> = _uiState

    fun resetState() {
        _uiState.value = AddMusicUiState()
    }

    fun setMode(mode: AddMusicMode) {
        _uiState.value = _uiState.value.copy(mode = mode)
    }

    fun onUrlChange(url: String) {
        _uiState.value = _uiState.value.copy(url = url, error = null)
    }

    fun onPreviewTitleChange(v: String) {
        _uiState.value = _uiState.value.copy(previewTitle = v)
    }

    fun onPreviewArtistChange(v: String) {
        _uiState.value = _uiState.value.copy(previewArtist = v)
    }

    fun submitUrl() {
        val url = _uiState.value.url.trim()
        if (url.isBlank()) return
        _uiState.value = _uiState.value.copy(step = AddMusicStep.LOADING, error = null)
        viewModelScope.launch {
            try {
                val preview = downloadsRepository.preview(url)
                _uiState.value =
                        _uiState.value.copy(
                                previewTitle = preview.title,
                                previewArtist = preview.artist,
                                previewThumbnail = preview.thumbnailUrl,
                                step = AddMusicStep.REVIEW,
                        )
            } catch (t: Throwable) {
                _uiState.value =
                        _uiState.value.copy(
                                error = "Could not fetch video info",
                                step = AddMusicStep.INPUT
                        )
            }
        }
    }

    fun backToInput() {
        _uiState.value = _uiState.value.copy(step = AddMusicStep.INPUT, error = null)
    }

    fun confirmUrlDownload(onDone: () -> Unit) {
        val state = _uiState.value
        _uiState.value = state.copy(uploading = true, error = null)
        viewModelScope.launch {
            val success =
                    downloadsRepository.start(state.url, state.previewTitle, state.previewArtist)
            if (success) {
                resetState()
                onDone()
            } else {
                _uiState.value =
                        _uiState.value.copy(
                                uploading = false,
                                step = AddMusicStep.REVIEW,
                                error = "Failed to start download. Please try again."
                        )
            }
        }
    }

    fun addFiles(uris: List<Uri>) {
        viewModelScope.launch {
            val incoming = withContext(Dispatchers.IO) { uris.mapNotNull { resolveFileInfo(it) } }
            val existingKeys = _uiState.value.selectedFiles.map { it.name to it.sizeBytes }.toSet()
            val deduped = incoming.filterNot { (it.name to it.sizeBytes) in existingKeys }
            _uiState.value =
                    _uiState.value.copy(
                            selectedFiles = _uiState.value.selectedFiles + deduped,
                            error = null
                    )
        }
    }

    private fun resolveFileInfo(uri: Uri): PickedFile? {
        var name = "audio file"
        var size = 0L
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
            if (cursor.moveToFirst()) {
                if (nameIndex >= 0) name = cursor.getString(nameIndex) ?: name
                if (sizeIndex >= 0) size = cursor.getLong(sizeIndex)
            }
        }
        return PickedFile(uri, name, size)
    }

    fun removeFile(index: Int) {
        _uiState.value =
                _uiState.value.copy(
                        selectedFiles =
                                _uiState.value.selectedFiles.filterIndexed { i, _ -> i != index }
                )
    }

    fun uploadFiles(onDone: () -> Unit) {
        val files = _uiState.value.selectedFiles
        if (files.isEmpty()) return
        _uiState.value = _uiState.value.copy(uploading = true, error = null)
        viewModelScope.launch {
            try {
                val parts =
                        withContext(Dispatchers.IO) {
                            files.map { file ->
                                val bytes =
                                        context.contentResolver.openInputStream(file.uri)?.use {
                                            it.readBytes()
                                        }
                                                ?: error("Could not read file")
                                val body = bytes.toRequestBody("audio/*".toMediaTypeOrNull())
                                MultipartBody.Part.createFormData("files", file.name, body)
                            }
                        }
                val success = downloadsRepository.uploadFiles(parts, files.map { it.name })
                if (success) {
                    resetState()
                    onDone()
                } else {
                    _uiState.value =
                            _uiState.value.copy(
                                    uploading = false,
                                    error = "Upload failed. Please try again."
                            )
                }
            } catch (t: Throwable) {
                _uiState.value =
                        _uiState.value.copy(
                                uploading = false,
                                error = "Upload failed. Please try again."
                        )
            }
        }
    }

    fun formatSize(bytes: Long): String =
            if (bytes < 1024 * 1024) "${bytes / 1024} KB"
            else "%.1f MB".format(bytes / (1024f * 1024f))
}
