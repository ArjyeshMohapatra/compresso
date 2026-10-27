package com.lumora.compresso.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lumora.compresso.image.ImageProcessor
import com.lumora.compresso.model.*
import com.lumora.compresso.util.getImageMetadata
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ImageViewModel : ViewModel() {

    private val _selectedImages = MutableStateFlow<List<SelectedImage>>(emptyList())
    val selectedImages: StateFlow<List<SelectedImage>> = _selectedImages.asStateFlow()

    private val _settings = MutableStateFlow(ImageSettings())
    val settings: StateFlow<ImageSettings> = _settings.asStateFlow()

    private val _results = MutableStateFlow<List<ProcessingResult>>(emptyList())
    val results: StateFlow<List<ProcessingResult>> = _results.asStateFlow()

    // Using the generic ProcessingState
    private val _uiState = MutableStateFlow<ProcessingState>(ProcessingState.Idle)
    val uiState: StateFlow<ProcessingState> = _uiState.asStateFlow()

    private var processingJob: Job? = null

    private fun invalidateResults() {
        _results.value = emptyList()
        _uiState.value = ProcessingState.Idle
    }

    fun addImages(context: Context, uris: List<Uri>) {
        viewModelScope.launch {
            val currentList = _selectedImages.value
            val remainingSlots = 10 - currentList.size

            if (remainingSlots <= 0) return@launch

            val newImages = withContext(Dispatchers.IO) {
                uris.take(remainingSlots).mapNotNull { uri ->
                    if (currentList.any { it.uri == uri }) null
                    else getImageMetadata(context, uri)
                }
            }
            _selectedImages.value = currentList + newImages
            invalidateResults()
        }
    }

    fun removeImage(uri: Uri) {
        _selectedImages.value = _selectedImages.value.filter { it.uri != uri }
        invalidateResults()
    }

    fun clearAll() {
        _selectedImages.value = emptyList()
        invalidateResults()
    }

    fun updateCompressionMode(mode: CompressionMode) { _settings.value = _settings.value.copy(compressionMode = mode); invalidateResults() }
    fun updateQuality(quality: Int) { _settings.value = _settings.value.copy(quality = quality); invalidateResults() }
    fun updateTargetSizeKb(sizeKb: Int) { _settings.value = _settings.value.copy(targetSizeKb = sizeKb); invalidateResults() }
    fun updateResize(enabled: Boolean, width: Int, height: Int) { _settings.value = _settings.value.copy(resizeEnabled = enabled, targetWidth = width, targetHeight = height); invalidateResults() }
    fun updatePreserveAspectRatio(preserve: Boolean) { _settings.value = _settings.value.copy(preserveAspectRatio = preserve); invalidateResults() }
    fun updateOutputFormat(format: ImageOutputFormat) { _settings.value = _settings.value.copy(outputFormat = format); invalidateResults() }

    fun cancelProcessing() {
        processingJob?.cancel()
        _uiState.value = ProcessingState.Idle
    }

    fun resetUiState() {
        _uiState.value = ProcessingState.Idle
    }

    fun compressImages(context: Context) {
        val imagesToProcess = _selectedImages.value
        val currentSettings = _settings.value
        if (imagesToProcess.isEmpty() || _uiState.value is ProcessingState.Processing) return

        _uiState.value = ProcessingState.Processing

        processingJob = viewModelScope.launch {
            val newResults = mutableListOf<ProcessingResult>()
            try {
                for (image in imagesToProcess) {
                    val result = ImageProcessor.processImage(context, image, currentSettings)
                    if (result.isSuccess) {
                        newResults.add(result.getOrThrow())
                    } else {
                        newResults.add(
                            ProcessingResult(
                                originalImage = image, compressedSizeBytes = 0L, compressedWidth = 0, compressedHeight = 0,
                                processedBytes = null, status = ProcessingStatus.FAILED,
                                errorMessage = result.exceptionOrNull()?.message ?: "Unknown error"
                            )
                        )
                    }
                }
                _results.value = newResults
                _uiState.value = ProcessingState.Done
            } catch (e: CancellationException) {
                _uiState.value = ProcessingState.Idle
            } catch (e: Exception) {
                _uiState.value = ProcessingState.Error(e.message ?: "An unexpected error occurred")
            }
        }
    }
}