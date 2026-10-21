package com.lumora.compresso.model

enum class ImageOutputFormat {
    JPEG,
    PNG,
    WEBP
}

enum class CompressionMode {
    QUALITY,
    TARGET_SIZE
}

data class ImageSettings(
    val compressionMode: CompressionMode = CompressionMode.QUALITY,
    val quality: Int = 80,
    val targetSizeKb: Int = 100,
    val resizeEnabled: Boolean = false,
    val targetWidth: Int = 0,
    val targetHeight: Int = 0,
    val preserveAspectRatio: Boolean = true,
    val outputFormat: ImageOutputFormat = ImageOutputFormat.JPEG
)