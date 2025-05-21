package com.example.lamb0693.testcompose

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import androidx.core.graphics.scale
import java.io.File
import java.io.OutputStream

object ImageUtils {
    fun resizeAndSave(bitmapFile: File, context: Context) {
        val bitmap = BitmapFactory.decodeFile(bitmapFile.absolutePath)
        val resized = bitmap.scale(320, 200)

        val filename = "resized_${System.currentTimeMillis()}.jpg"

        val resolver = context.contentResolver
        val imageCollection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        } else {
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        }

        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, filename)
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
        }

        val imageUri = resolver.insert(imageCollection, contentValues)

        if (imageUri != null) {
            try {
                val outputStream: OutputStream? = resolver.openOutputStream(imageUri)
                if (outputStream != null) {
                    resized.compress(Bitmap.CompressFormat.JPEG, 90, outputStream)
                    outputStream.close()

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        contentValues.clear()
                        contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
                        resolver.update(imageUri, contentValues, null, null)
                    }

                    Log.d(">>>>", "Image saved to gallery: $imageUri")
                }
            } catch (e: Exception) {
                Log.e(">>>>", "Failed to save image to gallery", e)
            }
        } else {
            Log.e(">>>>", "Failed to create new MediaStore record.")
        }
    }
}