package com.example.weathersnap.data.util

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import java.io.ByteArrayOutputStream
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ImageCompressor @Inject constructor() {

    companion object {
        const val MAX_FILE_SIZE_BYTES = 5 * 1024 * 1024L // 5 MB
        const val MAX_DIMENSION = 1920
    }

    fun compressAndValidate(filePath: String): ByteArray {
        val file = File(filePath)
        if (!file.exists() || file.length() == 0L) {
            throw IllegalArgumentException("Photo file does not exist or is empty.")
        }

        // 1. Decode bounds to determine sampling ratio
        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        BitmapFactory.decodeFile(file.absolutePath, options)
        val origWidth = options.outWidth
        val origHeight = options.outHeight

        if (origWidth <= 0 || origHeight <= 0) {
            throw IllegalArgumentException("Invalid image file format.")
        }

        // 2. Calculate inSampleSize
        var inSampleSize = 1
        val maxOriginalDimension = maxOf(origWidth, origHeight)
        if (maxOriginalDimension > MAX_DIMENSION) {
            inSampleSize = 2
            while ((maxOriginalDimension / inSampleSize) > MAX_DIMENSION) {
                inSampleSize *= 2
            }
        }

        val decodeOptions = BitmapFactory.Options().apply {
            this.inSampleSize = inSampleSize
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }

        var bitmap = BitmapFactory.decodeFile(file.absolutePath, decodeOptions)
            ?: throw IllegalStateException("Failed to decode image from disk.")

        // 3. Fix EXIF orientation if needed
        try {
            val exif = ExifInterface(file.absolutePath)
            val orientation = exif.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            )
            val rotationDegrees = when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                else -> 0f
            }
            if (rotationDegrees != 0f) {
                val matrix = Matrix().apply { postRotate(rotationDegrees) }
                val rotated = Bitmap.createBitmap(
                    bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true
                )
                if (rotated != bitmap) {
                    bitmap.recycle()
                    bitmap = rotated
                }
            }
        } catch (_: Exception) {
            // Orientation fallback if EXIF unreadable
        }

        // 4. Compress to JPEG
        var quality = 85
        var stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, stream)
        var bytes = stream.toByteArray()

        // Progressive downscale if still exceeds 5 MB
        while (bytes.size > MAX_FILE_SIZE_BYTES && quality > 30) {
            quality -= 15
            stream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, stream)
            bytes = stream.toByteArray()
        }

        bitmap.recycle()

        if (bytes.size > MAX_FILE_SIZE_BYTES) {
            throw IllegalStateException("Compressed photo exceeds maximum allowed size of 5 MB.")
        }

        return bytes
    }
}
