package com.lumora.compresso.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.lumora.compresso.model.ProcessingResult
import com.lumora.compresso.model.ImageOutputFormat

object StorageManager {

    fun takePersistablePermission(context: Context, uri: Uri) {
        val takeFlags: Int = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        context.contentResolver.takePersistableUriPermission(uri, takeFlags)
    }

    fun getFolderName(context: Context, uri: Uri?): String {
        if (uri == null) return "Not set"
        return try {
            DocumentFile.fromTreeUri(context, uri)?.name ?: "Unknown Folder"
        } catch (e: Exception) {
            "Invalid Folder"
        }
    }

    fun saveCompressedImage(
        context: Context,
        folderUri: Uri,
        result: ProcessingResult,
        format: ImageOutputFormat
    ): Result<Uri> {
        return try {
            val directory = DocumentFile.fromTreeUri(context, folderUri)
                ?: return Result.failure(Exception("Cannot access folder"))

            if (!directory.canWrite()) return Result.failure(Exception("No write permission"))

            val bytes = result.processedBytes ?: return Result.failure(Exception("No bytes"))

            val originalName = result.originalImage.name.substringBeforeLast(".")
            val ext = when (format) {
                ImageOutputFormat.JPEG -> "jpg"
                ImageOutputFormat.PNG -> "png"
                ImageOutputFormat.WEBP -> "webp"
            }
            val mimeType = when (format) {
                ImageOutputFormat.JPEG -> "image/jpeg"
                ImageOutputFormat.PNG -> "image/png"
                ImageOutputFormat.WEBP -> "image/webp"
            }

            val newFileName = "${originalName}_compressed.$ext"

            val newFile = directory.createFile(mimeType, newFileName)
                ?: return Result.failure(Exception("Failed to create file"))

            context.contentResolver.openOutputStream(newFile.uri)?.use { outputStream ->
                outputStream.write(bytes)
            } ?: return Result.failure(Exception("Failed to open stream"))

            Result.success(newFile.uri)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}