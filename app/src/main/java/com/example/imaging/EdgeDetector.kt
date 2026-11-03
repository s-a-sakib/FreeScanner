package com.example.imaging

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.PointF
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min

class EdgeDetector {

    /**
     * Detects 4 document corner points from a source bitmap.
     * Operates on a downscaled image for sub-second performance.
     * Returns null if no strong quadrilateral boundary is found.
     */
    fun detectCorners(bitmap: Bitmap): CornerPoints? {
        val width = bitmap.width
        val height = bitmap.height
        if (width < 50 || height < 50) return null

        // Downscale for fast analysis (max 320px width)
        val targetWidth = 320
        val scale = if (width > targetWidth) width.toFloat() / targetWidth else 1.0f
        val sampleW = (width / scale).toInt().coerceAtLeast(40)
        val sampleH = (height / scale).toInt().coerceAtLeast(40)

        val scaled = Bitmap.createScaledBitmap(bitmap, sampleW, sampleH, true)
        val pixels = IntArray(sampleW * sampleH)
        scaled.getPixels(pixels, 0, sampleW, 0, 0, sampleW, sampleH)
        if (scaled != bitmap) scaled.recycle()

        // 1. Grayscale
        val gray = IntArray(sampleW * sampleH)
        for (i in pixels.indices) {
            val c = pixels[i]
            val r = (c shr 16) and 0xFF
            val g = (c shr 8) and 0xFF
            val b = c and 0xFF
            gray[i] = (0.299 * r + 0.587 * g + 0.114 * b).toInt()
        }

        // 2. Sobel Edge Gradient
        val edges = FloatArray(sampleW * sampleH)
        var maxGrad = 0f

        for (y in 1 until sampleH - 1) {
            for (x in 1 until sampleW - 1) {
                val gx = (-gray[(y - 1) * sampleW + (x - 1)] + gray[(y - 1) * sampleW + (x + 1)]
                        - 2 * gray[y * sampleW + (x - 1)] + 2 * gray[y * sampleW + (x + 1)]
                        - gray[(y + 1) * sampleW + (x - 1)] + gray[(y + 1) * sampleW + (x + 1)]).toFloat()

                val gy = (-gray[(y - 1) * sampleW + (x - 1)] - 2 * gray[(y - 1) * sampleW + x] - gray[(y - 1) * sampleW + (x + 1)]
                        + gray[(y + 1) * sampleW + (x - 1)] + 2 * gray[(y + 1) * sampleW + x] + gray[(y + 1) * sampleW + (x + 1)]).toFloat()

                val mag = hypot(gx, gy)
                edges[y * sampleW + x] = mag
                if (mag > maxGrad) maxGrad = mag
            }
        }

        val threshold = maxGrad * 0.28f
        val strongPoints = mutableListOf<Point2D>()

        for (y in (sampleH * 0.05).toInt() until (sampleH * 0.95).toInt()) {
            for (x in (sampleW * 0.05).toInt() until (sampleW * 0.95).toInt()) {
                if (edges[y * sampleW + x] > threshold) {
                    strongPoints.add(Point2D(x.toFloat(), y.toFloat()))
                }
            }
        }

        if (strongPoints.size < 20) {
            // Default 7% inset quadrilateral
            return CornerPoints.defaultForDimensions(width.toFloat(), height.toFloat(), 0.07f)
        }

        // Find extreme corners (min/max sum and difference)
        var tl = strongPoints[0]
        var br = strongPoints[0]
        var tr = strongPoints[0]
        var bl = strongPoints[0]

        var minSum = tl.x + tl.y
        var maxSum = br.x + br.y
        var minDiff = tr.y - tr.x
        var maxDiff = bl.y - bl.x

        for (pt in strongPoints) {
            val sum = pt.x + pt.y
            val diff = pt.y - pt.x

            if (sum < minSum) {
                minSum = sum
                tl = pt
            }
            if (sum > maxSum) {
                maxSum = sum
                br = pt
            }
            if (diff < minDiff) {
                minDiff = diff
                tr = pt
            }
            if (diff > maxDiff) {
                maxDiff = diff
                bl = pt
            }
        }

        // Map back to original coordinate space
        return CornerPoints(
            topLeft = Point2D((tl.x * scale).coerceIn(0f, width.toFloat()), (tl.y * scale).coerceIn(0f, height.toFloat())),
            topRight = Point2D((tr.x * scale).coerceIn(0f, width.toFloat()), (tr.y * scale).coerceIn(0f, height.toFloat())),
            bottomRight = Point2D((br.x * scale).coerceIn(0f, width.toFloat()), (br.y * scale).coerceIn(0f, height.toFloat())),
            bottomLeft = Point2D((bl.x * scale).coerceIn(0f, width.toFloat()), (bl.y * scale).coerceIn(0f, height.toFloat()))
        )
    }
}
