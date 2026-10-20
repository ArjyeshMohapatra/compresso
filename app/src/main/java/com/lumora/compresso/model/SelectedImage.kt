package com.lumora.compresso.model

import android.net.Uri

data class SelectedImage(
    val uri: Uri,
    val name: String,
    val sizeBytes: Long,
    val width: Int,
    val height: Int
) {
    // Helper to format bytes into KB or MB for the UI
    fun getFormattedSize(): String {
        return when {
            sizeBytes >= 1024 * 1024 -> String.format("%.2f MB", sizeBytes / (1024.0 * 1024.0))
            sizeBytes >= 1024 -> String.format("%.1f KB", sizeBytes / 1024.0)
            else -> "$sizeBytes B"
        }
    }
}