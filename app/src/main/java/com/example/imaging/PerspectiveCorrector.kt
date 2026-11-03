package com.example.imaging

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import kotlin.math.hypot
import kotlin.math.max

class PerspectiveCorrector {

    /**
     * Warps a quadrilateral region defined by [corners] from [sourceBitmap] into a
     * flat rectangular document bitmap.
     */
    fun correctPerspective(sourceBitmap: Bitmap, corners: CornerPoints): Bitmap {
        val tl = corners.topLeft
        val tr = corners.topRight
        val br = corners.bottomRight
        val bl = corners.bottomLeft

        // Compute output dimensions
        val widthTop = hypot((tr.x - tl.x).toDouble(), (tr.y - tl.y).toDouble())
        val widthBottom = hypot((br.x - bl.x).toDouble(), (br.y - bl.y).toDouble())
        val targetWidth = max(widthTop, widthBottom).toInt().coerceIn(300, 4000)

        val heightLeft = hypot((bl.x - tl.x).toDouble(), (bl.y - tl.y).toDouble())
        val heightRight = hypot((br.x - tr.x).toDouble(), (br.y - tr.y).toDouble())
        val targetHeight = max(heightLeft, heightRight).toInt().coerceIn(400, 5000)

        val srcPoints = floatArrayOf(
            tl.x, tl.y,
            tr.x, tr.y,
            br.x, br.y,
            bl.x, bl.y
        )

        val dstPoints = floatArrayOf(
            0f, 0f,
            targetWidth.toFloat(), 0f,
            targetWidth.toFloat(), targetHeight.toFloat(),
            0f, targetHeight.toFloat()
        )

        val matrix = Matrix()
        val success = matrix.setPolyToPoly(srcPoints, 0, dstPoints, 0, 4)

        val resultBitmap = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(resultBitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

        if (success) {
            val invertMatrix = Matrix()
            if (matrix.invert(invertMatrix)) {
                // Draw source using matrix
                val forward = Matrix()
                matrix.invert(forward)
                canvas.drawBitmap(sourceBitmap, matrix, paint)
            } else {
                canvas.drawBitmap(sourceBitmap, matrix, paint)
            }
        } else {
            // Fallback: draw directly scaled
            canvas.drawBitmap(
                Bitmap.createScaledBitmap(sourceBitmap, targetWidth, targetHeight, true),
                0f, 0f, paint
            )
        }

        return resultBitmap
    }
}
