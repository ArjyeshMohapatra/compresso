package com.lumora.compresso.image

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.os.Build
import androidx.exifinterface.media.ExifInterface
import com.lumora.compresso.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.IOException
import kotlin.math.min
import kotlin.math.roundToInt

object ImageProcessor {

    suspend fun processImage(
        context: Context,
        image: SelectedImage,
        settings: ImageSettings
    ): Result<ProcessingResult> = withContext(Dispatchers.IO) {
        try {
            require(settings.quality in 1..100) { "Quality must be between 1 and 100" }
            if (settings.resizeEnabled) {
                require(settings.targetWidth > 0 && settings.targetHeight > 0) { "Target width and height must be positive" }
            }

            val resolver = context.contentResolver

            // 1. STRICT SKIP CHECK (Format + Size)
            val isSameFormat = when (settings.outputFormat) {
                ImageOutputFormat.JPEG -> image.mimeType == "image/jpeg"
                ImageOutputFormat.PNG -> image.mimeType == "image/png"
                ImageOutputFormat.WEBP -> image.mimeType == "image/webp"
            }

            if (settings.compressionMode == CompressionMode.TARGET_SIZE &&
                (settings.targetSizeKb * 1024L) >= image.sizeBytes &&
                !settings.resizeEnabled &&
                isSameFormat &&
                image.sizeBytes > 0) {

                val originalBytes = resolver.openInputStream(image.uri)?.use { it.readBytes() }
                    ?: throw IOException("Cannot read original image")

                return@withContext Result.success(
                    ProcessingResult(
                        originalImage = image,
                        compressedSizeBytes = originalBytes.size.toLong(),
                        compressedWidth = image.width,
                        compressedHeight = image.height,
                        processedBytes = originalBytes,
                        status = ProcessingStatus.SKIPPED
                    )
                )
            }

            // 2. Decode and Fix EXIF
            val orientation = resolver.openInputStream(image.uri)?.use { stream ->
                ExifInterface(stream).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
            } ?: throw IOException("Cannot read image orientation")

            var bitmap = resolver.openInputStream(image.uri)?.use { stream ->
                BitmapFactory.decodeStream(stream)
            } ?: throw IOException("Cannot decode image")

            try {
                val matrix = Matrix()
                when (orientation) {
                    ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.setScale(-1f, 1f)
                    ExifInterface.ORIENTATION_ROTATE_180 -> matrix.setRotate(180f)
                    ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.setScale(1f, -1f)
                    ExifInterface.ORIENTATION_TRANSPOSE -> { matrix.setRotate(90f); matrix.postScale(-1f, 1f) }
                    ExifInterface.ORIENTATION_ROTATE_90 -> matrix.setRotate(90f)
                    ExifInterface.ORIENTATION_TRANSVERSE -> { matrix.setRotate(-90f); matrix.postScale(-1f, 1f) }
                    ExifInterface.ORIENTATION_ROTATE_270 -> matrix.setRotate(-90f)
                }

                if (!matrix.isIdentity) {
                    val orientedBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
                    if (orientedBitmap !== bitmap) { bitmap.recycle(); bitmap = orientedBitmap }
                }

                // 3. Resize
                if (settings.resizeEnabled) {
                    var targetWidth = settings.targetWidth
                    var targetHeight = settings.targetHeight
                    if (settings.preserveAspectRatio) {
                        val scale = min(settings.targetWidth.toFloat() / bitmap.width, settings.targetHeight.toFloat() / bitmap.height)
                        targetWidth = (bitmap.width * scale).roundToInt().coerceAtLeast(1)
                        targetHeight = (bitmap.height * scale).roundToInt().coerceAtLeast(1)
                    }
                    if (targetWidth != bitmap.width || targetHeight != bitmap.height) {
                        val resizedBitmap = Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight, true)
                        if (resizedBitmap !== bitmap) { bitmap.recycle(); bitmap = resizedBitmap }
                    }
                }

                // 4. Format & Transparency handling
                val format = when (settings.outputFormat) {
                    ImageOutputFormat.JPEG -> Bitmap.CompressFormat.JPEG
                    ImageOutputFormat.PNG -> Bitmap.CompressFormat.PNG
                    ImageOutputFormat.WEBP -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) Bitmap.CompressFormat.WEBP_LOSSY else @Suppress("DEPRECATION") Bitmap.CompressFormat.WEBP
                }

                if (settings.outputFormat == ImageOutputFormat.JPEG && bitmap.hasAlpha()) {
                    val opaqueBitmap = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
                    val canvas = android.graphics.Canvas(opaqueBitmap)
                    canvas.drawColor(android.graphics.Color.WHITE)
                    canvas.drawBitmap(bitmap, 0f, 0f, null)
                    bitmap.recycle()
                    bitmap = opaqueBitmap
                }

                // 5. Compression Logic
                var processedBytes: ByteArray
                var status = ProcessingStatus.SUCCESS
                val requestedMaxBytes = settings.targetSizeKb * 1024L
                val maxAllowedBytes = minOf(requestedMaxBytes, if (image.sizeBytes > 0) image.sizeBytes else requestedMaxBytes)

                if (settings.compressionMode == CompressionMode.TARGET_SIZE && format != Bitmap.CompressFormat.PNG) {
                    // Binary Search for JPEG/WEBP
                    var low = 5
                    var high = 100
                    var bestBytes: ByteArray? = null

                    while (low <= high) {
                        val mid = (low + high) / 2
                        val testStream = ByteArrayOutputStream()
                        val success = bitmap.compress(format, mid, testStream)
                        if (!success) throw IOException("Compression failed at quality $mid")

                        val size = testStream.size()
                        if (size <= maxAllowedBytes) {
                            bestBytes = testStream.toByteArray()
                            low = mid + 1
                        } else {
                            high = mid - 1
                        }
                    }

                    // If even 5% is too big, we output it but flag the status
                    if (bestBytes == null) {
                        val fallbackStream = ByteArrayOutputStream()
                        if (!bitmap.compress(format, 5, fallbackStream)) throw IOException("Compression failed entirely")
                        bestBytes = fallbackStream.toByteArray()
                        if (bestBytes.size > maxAllowedBytes) status = ProcessingStatus.TARGET_NOT_MET
                    }
                    processedBytes = bestBytes
                } else {
                    // PNG or Quality Mode (Standard fixed compression)
                    processedBytes = ByteArrayOutputStream().use { output ->
                        val success = bitmap.compress(format, settings.quality, output)
                        if (!success) throw IOException("Image compression failed")
                        output.toByteArray()
                    }
                    if (settings.compressionMode == CompressionMode.TARGET_SIZE && processedBytes.size > maxAllowedBytes) {
                        status = ProcessingStatus.TARGET_NOT_MET
                    }
                }

                if (processedBytes.isEmpty()) throw IOException("Compression produced an empty file")

                Result.success(
                    ProcessingResult(
                        originalImage = image,
                        compressedSizeBytes = processedBytes.size.toLong(),
                        compressedWidth = bitmap.width,
                        compressedHeight = bitmap.height,
                        processedBytes = processedBytes,
                        status = status
                    )
                )
            } finally {
                if (!bitmap.isRecycled) bitmap.recycle()
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}