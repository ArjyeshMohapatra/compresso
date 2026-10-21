package com.lumora.compresso.image

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.os.Build
import androidx.exifinterface.media.ExifInterface
import com.lumora.compresso.model.*
import kotlinx.coroutines.CancellationException
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
            if (settings.compressionMode == CompressionMode.TARGET_SIZE) {
                require(settings.targetSizeKb > 0) { "Target size must be greater than zero" }
            }

            val resolver = context.contentResolver
            val requestedMaxBytes = settings.targetSizeKb * 1024L

            val isSameFormat = when (settings.outputFormat) {
                ImageOutputFormat.JPEG -> image.mimeType == "image/jpeg"
                ImageOutputFormat.PNG -> image.mimeType == "image/png"
                ImageOutputFormat.WEBP -> image.mimeType == "image/webp"
            }

            // ROBUST SKIP CHECK
            if (settings.compressionMode == CompressionMode.TARGET_SIZE &&
                !settings.resizeEnabled && isSameFormat) {

                val originalBytes = resolver.openInputStream(image.uri)?.use { it.readBytes() }
                    ?: throw IOException("Cannot read original image")

                if (originalBytes.size.toLong() <= requestedMaxBytes) {
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
            }

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

                // Bounding Box Resize (Never upscale)
                if (settings.resizeEnabled) {
                    var targetWidth = settings.targetWidth
                    var targetHeight = settings.targetHeight
                    if (settings.preserveAspectRatio) {
                        val scale = min(
                            1f, // Prevents upscaling
                            min(settings.targetWidth.toFloat() / bitmap.width, settings.targetHeight.toFloat() / bitmap.height)
                        )
                        targetWidth = (bitmap.width * scale).roundToInt().coerceAtLeast(1)
                        targetHeight = (bitmap.height * scale).roundToInt().coerceAtLeast(1)
                    }
                    if (targetWidth != bitmap.width || targetHeight != bitmap.height) {
                        val resizedBitmap = Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight, true)
                        if (resizedBitmap !== bitmap) { bitmap.recycle(); bitmap = resizedBitmap }
                    }
                }

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

                var processedBytes: ByteArray
                var status = ProcessingStatus.SUCCESS

                // YOUR RULE: Never inflate. Cap target at original size.
                val maxAllowedBytes = minOf(requestedMaxBytes, image.sizeBytes)

                if (settings.compressionMode == CompressionMode.TARGET_SIZE && format != Bitmap.CompressFormat.PNG) {
                    var low = 5
                    var high = 100
                    var bestBytes: ByteArray? = null

                    while (low <= high) {
                        val mid = (low + high) / 2
                        val testStream = ByteArrayOutputStream()
                        if (!bitmap.compress(format, mid, testStream)) throw IOException("Compression failed")

                        val size = testStream.size()
                        if (size <= maxAllowedBytes) {
                            bestBytes = testStream.toByteArray()
                            low = mid + 1
                        } else {
                            high = mid - 1
                        }
                    }

                    if (bestBytes == null) {
                        val fallbackStream = ByteArrayOutputStream()
                        if (!bitmap.compress(format, 5, fallbackStream)) throw IOException("Compression failed")
                        bestBytes = fallbackStream.toByteArray()
                        if (bestBytes.size > maxAllowedBytes) status = ProcessingStatus.TARGET_NOT_MET
                    }
                    processedBytes = bestBytes
                } else {
                    processedBytes = ByteArrayOutputStream().use { output ->
                        if (!bitmap.compress(format, settings.quality, output)) throw IOException("Compression failed")
                        output.toByteArray()
                    }
                    if (settings.compressionMode == CompressionMode.TARGET_SIZE && processedBytes.size > maxAllowedBytes) {
                        status = ProcessingStatus.TARGET_NOT_MET
                    }
                }

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
        } catch (e: CancellationException) {
            // MUST rethrow so the Cancel button actually stops the coroutine
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}