package com.example.imaging

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import kotlin.math.max
import kotlin.math.min

class FilterProcessor {

    fun applyFilter(bitmap: Bitmap, filter: FilterType): Bitmap {
        return when (filter) {
            FilterType.ORIGINAL -> bitmap.copy(bitmap.config ?: Bitmap.Config.ARGB_8888, true)
            FilterType.AUTO -> applyAuto(bitmap)
            FilterType.MAGIC_COLOR -> applyMagicColor(bitmap)
            FilterType.BW -> applyAdaptiveBlackAndWhite(bitmap)
            FilterType.GRAYSCALE -> applyGrayscale(bitmap)
        }
    }

    /**
     * Auto: shadow removal & adaptive contrast balancing
     */
    private fun applyAuto(bitmap: Bitmap): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)

        // Apply gentle high-key contrast + brightness boost to whiten shadows
        val cm = ColorMatrix(
            floatArrayOf(
                1.15f, 0f, 0f, 0f, 18f,
                0f, 1.15f, 0f, 0f, 18f,
                0f, 0f, 1.15f, 0f, 18f,
                0f, 0f, 0f, 1f, 0f
            )
        )
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            colorFilter = ColorMatrixColorFilter(cm)
        }
        canvas.drawBitmap(bitmap, 0f, 0f, paint)
        return output
    }

    /**
     * Magic Color: Rich vibrant text & high contrast while whitening paper background
     */
    private fun applyMagicColor(bitmap: Bitmap): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)

        val cm = ColorMatrix().apply {
            setSaturation(1.4f)
        }
        // Increase contrast
        val scale = 1.3f
        val translate = (-0.5f * scale + 0.5f) * 255f + 15f
        val contrastMatrix = ColorMatrix(
            floatArrayOf(
                scale, 0f, 0f, 0f, translate,
                0f, scale, 0f, 0f, translate,
                0f, 0f, scale, 0f, translate,
                0f, 0f, 0f, 1f, 0f
            )
        )
        cm.postConcat(contrastMatrix)

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            colorFilter = ColorMatrixColorFilter(cm)
        }
        canvas.drawBitmap(bitmap, 0f, 0f, paint)
        return output
    }

    /**
     * Crisp high-readability Black & White document thresholding
     */
    private fun applyAdaptiveBlackAndWhite(bitmap: Bitmap): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        // Calculate average document luminance
        var sumLuma = 0L
        for (c in pixels) {
            val r = (c shr 16) and 0xFF
            val g = (c shr 8) and 0xFF
            val b = c and 0xFF
            sumLuma += (0.299 * r + 0.587 * g + 0.114 * b).toLong()
        }
        val avgLuma = (sumLuma / pixels.size).toInt()
        val threshold = (avgLuma * 0.90f).toInt().coerceIn(80, 180)

        val outPixels = IntArray(width * height)
        val black = Color.BLACK
        val white = Color.WHITE

        for (i in pixels.indices) {
            val c = pixels[i]
            val r = (c shr 16) and 0xFF
            val g = (c shr 8) and 0xFF
            val b = c and 0xFF
            val luma = (0.299 * r + 0.587 * g + 0.114 * b).toInt()
            outPixels[i] = if (luma < threshold) black else white
        }

        output.setPixels(outPixels, 0, width, 0, 0, width, height)
        return output
    }

    /**
     * Grayscale transformation
     */
    private fun applyGrayscale(bitmap: Bitmap): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)

        val cm = ColorMatrix().apply {
            setSaturation(0f)
        }
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            colorFilter = ColorMatrixColorFilter(cm)
        }
        canvas.drawBitmap(bitmap, 0f, 0f, paint)
        return output
    }

    fun rotateBitmap(bitmap: Bitmap, degrees: Int): Bitmap {
        if (degrees % 360 == 0) return bitmap
        val matrix = Matrix().apply { postRotate(degrees.toFloat()) }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    fun adjustBrightnessAndContrast(bitmap: Bitmap, brightness: Float, contrast: Float): Bitmap {
        val output = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)

        val c = contrast.coerceIn(0.5f, 2.0f)
        val b = brightness.coerceIn(-100f, 100f)

        val translate = (-0.5f * c + 0.5f) * 255f + b
        val cm = ColorMatrix(
            floatArrayOf(
                c, 0f, 0f, 0f, translate,
                0f, c, 0f, 0f, translate,
                0f, 0f, c, 0f, translate,
                0f, 0f, 0f, 1f, 0f
            )
        )
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            colorFilter = ColorMatrixColorFilter(cm)
        }
        canvas.drawBitmap(bitmap, 0f, 0f, paint)
        return output
    }
}
