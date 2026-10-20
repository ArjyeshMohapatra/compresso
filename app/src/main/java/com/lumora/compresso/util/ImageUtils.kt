package com.lumora.compresso.util

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.OpenableColumns
import com.lumora.compresso.model.SelectedImage

fun getImageMetadata(context: Context, uri: Uri): SelectedImage? {
    val contentResolver = context.contentResolver
    var name = "Unknown"
    var size = 0L

    try {
        // 1. Get file name and size using ContentResolver
        contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)

                if (nameIndex != -1) name = cursor.getString(nameIndex)
                if (sizeIndex != -1) size = cursor.getLong(sizeIndex)
            }
        }

        // 2. Get dimensions using BitmapFactory (Just bounds, not the whole image)
        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        contentResolver.openInputStream(uri)?.use { inputStream ->
            BitmapFactory.decodeStream(inputStream, null, options)
        }

        return SelectedImage(
            uri = uri,
            name = name,
            sizeBytes = size,
            width = options.outWidth,
            height = options.outHeight
        )
    } catch (e: Exception) {
        e.printStackTrace()
        return null
    }
}