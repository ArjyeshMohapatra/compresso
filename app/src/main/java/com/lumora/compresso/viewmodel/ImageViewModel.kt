package com.lumora.compresso.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lumora.compresso.model.SelectedImage
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

    fun addImages(context: Context, uris: List<Uri>) {
        viewModelScope.launch {
            // Process metadata in the background so the UI doesn't freeze
            val newImages = withContext(Dispatchers.IO) {
                uris.mapNotNull { uri ->
                    // Don't add duplicates
                    if (_selectedImages.value.any { it.uri == uri }) null
                    else getImageMetadata(context, uri)
                }
            }
            _selectedImages.value = _selectedImages.value + newImages
        }
    }

    fun removeImage(uri: Uri) {
        _selectedImages.value = _selectedImages.value.filter { it.uri != uri }
    }

    fun clearAll() {
        _selectedImages.value = emptyList()
    }
}