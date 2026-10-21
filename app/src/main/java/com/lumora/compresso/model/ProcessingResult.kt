package com.lumora.compresso.model

enum class ProcessingStatus {
    SUCCESS,
    SKIPPED,
    TARGET_NOT_MET,
    FAILED
}

data class ProcessingResult(
    val originalImage: SelectedImage,
    val compressedSizeBytes: Long,
    val compressedWidth: Int,
    val compressedHeight: Int,
    val processedBytes: ByteArray?, // Null if failed
    val status: ProcessingStatus,
    val errorMessage: String? = null
)