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
import java.io.ByteArrayInputStream
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

            val originalBytes = resolver.openInputStream(image.uri)?.use { it.readBytes() }
                ?: throw IOException("Cannot read original image")

            val mime = image.mimeType?.lowercase() ?: ""
            val name = image.name.lowercase()
            val isSameFormat = when (settings.outputFormat) {
                ImageOutputFormat.JPEG -> mime == "image/jpeg" || mime == "image/jpg" || name.endsWith(".jpg") || name.endsWith(".jpeg")
                ImageOutputFormat.PNG -> mime == "image/png" || name.endsWith(".png")
                ImageOutputFormat.WEBP -> mime == "image/webp" || name.endsWith(".webp")
            }

            val isSourceJpeg = mime == "image/jpeg" || mime == "image/jpg" || name.endsWith(".jpg") || name.endsWith(".jpeg")

            // FIX: Only skip JPEG to PNG if the user is in TARGET_SIZE mode.
            if (isSourceJpeg && settings.outputFormat == ImageOutputFormat.PNG &&
                !settings.resizeEnabled && settings.compressionMode == CompressionMode.TARGET_SIZE) {

                return@withContext Result.success(
                    ProcessingResult(
                        originalImage = image,
                        compressedSizeBytes = originalBytes.size.toLong(),
                        compressedWidth = image.width,
                        compressedHeight = image.height,
                        processedBytes = originalBytes,
                        status = ProcessingStatus.SKIPPED,
                        errorMessage = "Skipped: Converting JPEG to PNG increases file size."
                    )
                )
            }

            // Early skip / validation for same-format files in target size mode
            if (settings.compressionMode == CompressionMode.TARGET_SIZE && !settings.resizeEnabled && isSameFormat) {
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
                } else if (settings.outputFormat == ImageOutputFormat.PNG) {
                    return@withContext Result.success(
                        ProcessingResult(
                            originalImage = image,
                            compressedSizeBytes = originalBytes.size.toLong(),
                            compressedWidth = image.width,
                            compressedHeight = image.height,
                            processedBytes = originalBytes,
                            status = ProcessingStatus.TARGET_NOT_MET,
                            errorMessage = "PNG is lossless and cannot meet target size without resizing."
                        )
                    )
                }
            }

            val orientation = ByteArrayInputStream(originalBytes).use { stream ->
                ExifInterface(stream).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
            }

            var bitmap = BitmapFactory.decodeByteArray(originalBytes, 0, originalBytes.size)
                ?: throw IOException("Cannot decode image")

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

                if (settings.resizeEnabled) {
                    var targetWidth = settings.targetWidth
                    var targetHeight = settings.targetHeight
                    if (settings.preserveAspectRatio) {
                        val scale = min(
                            1f,
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

                if (settings.compressionMode == CompressionMode.TARGET_SIZE && format != Bitmap.CompressFormat.PNG) {
                    var low = 5
                    var high = 100
                    var bestBytes: ByteArray? = null
                    var fallbackBytes: ByteArray? = null
                    val testStream = ByteArrayOutputStream()

                    while (low <= high) {
                        val mid = (low + high) / 2
                        testStream.reset()
                        if (!bitmap.compress(format, mid, testStream)) throw IOException("Compression failed")

                        if (testStream.size() <= requestedMaxBytes) {
                            bestBytes = testStream.toByteArray()
                            low = mid + 1
                        } else {
                            if (mid == 5 || fallbackBytes == null) {
                                fallbackBytes = testStream.toByteArray()
                            }
                            high = mid - 1
                        }
                    }

                    if (bestBytes != null) {
                        processedBytes = bestBytes
                    } else {
                        processedBytes = fallbackBytes ?: testStream.toByteArray()
                        status = ProcessingStatus.TARGET_NOT_MET
                    }
                } else {
                    processedBytes = ByteArrayOutputStream().use { output ->
                        if (!bitmap.compress(format, settings.quality, output)) throw IOException("Compression failed")
                        output.toByteArray()
                    }
                    if (settings.compressionMode == CompressionMode.TARGET_SIZE && processedBytes.size > requestedMaxBytes) {
                        status = ProcessingStatus.TARGET_NOT_MET
                    }
                }

                // Anti-inflation (only if we didn't resize)
                if (!settings.resizeEnabled && isSameFormat && processedBytes.size > originalBytes.size) {
                    processedBytes = originalBytes
                    if (status != ProcessingStatus.TARGET_NOT_MET) {
                        status = ProcessingStatus.SKIPPED
                    }
                }

                val errorMessage = if (status == ProcessingStatus.TARGET_NOT_MET) {
                    if (settings.outputFormat == ImageOutputFormat.PNG) {
                        "Target size cannot be met for PNG without resizing further."
                    } else {
                        "Target size of ${settings.targetSizeKb} KB could not be reached even at minimum quality."
                    }
                } else null

                val finalWidth = if (processedBytes === originalBytes) image.width else bitmap.width
                val finalHeight = if (processedBytes === originalBytes) image.height else bitmap.height

                Result.success(
                    ProcessingResult(
                        originalImage = image,
                        compressedSizeBytes = processedBytes.size.toLong(),
                        compressedWidth = finalWidth,
                        compressedHeight = finalHeight,
                        processedBytes = processedBytes,
                        status = status,
                        errorMessage = errorMessage
                    )
                )
            } finally {
                if (!bitmap.isRecycled) bitmap.recycle()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (t: Throwable) {
            Result.failure(if (t is Exception) t else Exception("Image processing failed due to memory or system error: ${t.localizedMessage}", t))
        }
    }
}