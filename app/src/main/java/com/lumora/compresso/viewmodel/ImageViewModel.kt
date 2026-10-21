package com.lumora.compresso.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lumora.compresso.image.ImageProcessor
import com.lumora.compresso.model.*
import com.lumora.compresso.util.getImageMetadata
import kotlinx.coroutines.Dispatchers
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

    // Helper to clear stale results whenever settings or images change
    private fun invalidateResults() {
        _results.value = emptyList()
    }

    fun addImages(context: Context, uris: List<Uri>) {
        viewModelScope.launch {
            val newImages = withContext(Dispatchers.IO) {
                uris.mapNotNull { uri ->
                    if (_selectedImages.value.any { it.uri == uri }) null
                    else getImageMetadata(context, uri)
                }
            }
            _selectedImages.value = _selectedImages.value + newImages
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

    // --- Settings Updates (All invalidate previous results) ---
    fun updateCompressionMode(mode: CompressionMode) {
        _settings.value = _settings.value.copy(compressionMode = mode)
        invalidateResults()
    }

    fun updateQuality(quality: Int) {
        _settings.value = _settings.value.copy(quality = quality)
        invalidateResults()
    }

    fun updateTargetSizeKb(sizeKb: Int) {
        _settings.value = _settings.value.copy(targetSizeKb = sizeKb)
        invalidateResults()
    }

    fun updateResize(enabled: Boolean, width: Int, height: Int) {
        _settings.value = _settings.value.copy(resizeEnabled = enabled, targetWidth = width, targetHeight = height)
        invalidateResults()
    }

    fun updatePreserveAspectRatio(preserve: Boolean) {
        _settings.value = _settings.value.copy(preserveAspectRatio = preserve)
        invalidateResults()
    }

    fun updateOutputFormat(format: ImageOutputFormat) {
        _settings.value = _settings.value.copy(outputFormat = format)
        invalidateResults()
    }

    // --- Processing ---
    fun compressImages(context: Context) {
        val imagesToProcess = _selectedImages.value
        val currentSettings = _settings.value
        if (imagesToProcess.isEmpty() || _isProcessing.value) return

        _isProcessing.value = true

        viewModelScope.launch {
            val newResults = mutableListOf<ProcessingResult>()

            try {
                for (image in imagesToProcess) {
                    val result = ImageProcessor.processImage(context, image, currentSettings)

                    if (result.isSuccess) {
                        newResults.add(result.getOrThrow())
                    } else {
                        // Explicitly capture failures instead of silently dropping them
                        newResults.add(
                            ProcessingResult(
                                originalImage = image,
                                compressedSizeBytes = 0L,
                                compressedWidth = 0,
                                compressedHeight = 0,
                                processedBytes = null,
                                status = ProcessingStatus.FAILED,
                                errorMessage = result.exceptionOrNull()?.message ?: "Unknown error"
                            )
                        )
                    }
                }
                _results.value = newResults
            } finally {
                // This guarantees the spinner stops, even if something catastrophic happens
                _isProcessing.value = false
            }
        }
    }
}