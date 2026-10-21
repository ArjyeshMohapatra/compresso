package com.lumora.compresso.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lumora.compresso.image.ImageProcessor
import com.lumora.compresso.model.*
import com.lumora.compresso.util.getImageMetadata
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

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()

    private var processingJob: Job? = null

    private fun invalidateResults() {
        _results.value = emptyList()
    }

    fun addImages(context: Context, uris: List<Uri>) {
        viewModelScope.launch {
            val currentList = _selectedImages.value
            val remainingSlots = 10 - currentList.size // MAX 10 IMAGES

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
        _isProcessing.value = false
    }

    fun compressImages(context: Context) {
        val imagesToProcess = _selectedImages.value
        val currentSettings = _settings.value
        if (imagesToProcess.isEmpty() || _isProcessing.value) return

        _isProcessing.value = true

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
            } catch (e: Exception) {
                // Ignore exceptions caused by cancellation
            } finally {
                _isProcessing.value = false
            }
        }
    }
}