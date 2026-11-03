package com.example.pdf

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class PdfToolService(private val context: Context) {

    /**
     * Creates a standard PDF file from bitmap pages, fitting each bitmap into the chosen page size
     * and optionally applying a custom text watermark.
     */
    suspend fun createPdfFromBitmaps(
        bitmaps: List<Bitmap>,
        outputFile: File,
        pageSize: PdfPageSize = PdfPageSize.A4,
        watermark: WatermarkConfig? = null,
        quality: Int = 85
    ): File = withContext(Dispatchers.IO) {
        val pdfDocument = PdfDocument()

        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        val watermarkPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.DKGRAY
            textAlign = Paint.Align.CENTER
        }

        try {
            bitmaps.forEachIndexed { index, bitmap ->
                val pageInfo = PdfDocument.PageInfo.Builder(pageSize.widthPt, pageSize.heightPt, index + 1).create()
                val page = pdfDocument.startPage(pageInfo)
                val canvas = page.canvas

                // Clear page background
                canvas.drawColor(Color.WHITE)

                // Scale bitmap preserving aspect ratio with 15pt margin
                val margin = 20f
                val availW = pageSize.widthPt - (margin * 2)
                val availH = pageSize.heightPt - (margin * 2)

                val bmpW = bitmap.width.toFloat()
                val bmpH = bitmap.height.toFloat()
                val scale = minOf(availW / bmpW, availH / bmpH)

                val drawW = bmpW * scale
                val drawH = bmpH * scale
                val left = margin + (availW - drawW) / 2f
                val top = margin + (availH - drawH) / 2f

                canvas.drawBitmap(bitmap, null, RectF(left, top, left + drawW, top + drawH), paint)

                // Apply watermark if requested
                watermark?.let { wm ->
                    canvas.save()
                    canvas.translate(pageSize.widthPt / 2f, pageSize.heightPt / 2f)
                    canvas.rotate(wm.angle)
                    watermarkPaint.textSize = wm.textSizeSp * 1.5f
                    watermarkPaint.alpha = (wm.opacity * 255).toInt().coerceIn(20, 255)
                    canvas.drawText(wm.text, 0f, 0f, watermarkPaint)
                    canvas.restore()
                }

                pdfDocument.finishPage(page)
            }

            outputFile.parentFile?.mkdirs()
            FileOutputStream(outputFile).use { out ->
                pdfDocument.writeTo(out)
            }
        } finally {
            pdfDocument.close()
        }

        outputFile
    }

    /**
     * Renders each page of a PDF file into a high-quality Bitmap using native Android PdfRenderer.
     */
    suspend fun renderPdfPagesToBitmaps(pdfFile: File, renderDpiScale: Float = 2.0f): List<Bitmap> = withContext(Dispatchers.IO) {
        val renderedBitmaps = mutableListOf<Bitmap>()
        val pfd = ParcelFileDescriptor.open(pdfFile, ParcelFileDescriptor.MODE_READ_ONLY)
        val renderer = PdfRenderer(pfd)

        try {
            for (i in 0 until renderer.pageCount) {
                val page = renderer.openPage(i)
                val w = (page.width * renderDpiScale).toInt().coerceAtLeast(300)
                val h = (page.height * renderDpiScale).toInt().coerceAtLeast(400)
                val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(bitmap)
                canvas.drawColor(Color.WHITE)

                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                page.close()
                renderedBitmaps.add(bitmap)
            }
        } finally {
            renderer.close()
            pfd.close()
        }

        renderedBitmaps
    }

    /**
     * Places ID front and back scans onto a single standard A4 document page.
     */
    suspend fun createIdCardPage(frontBitmap: Bitmap, backBitmap: Bitmap): Bitmap = withContext(Dispatchers.Default) {
        val width = 1240
        val height = 1754
        val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        canvas.drawColor(Color.WHITE)

        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#475569")
            textSize = 28f
            textAlign = Paint.Align.CENTER
            isFakeBoldText = true
        }

        // Header
        val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#059669")
        }
        canvas.drawRect(Rect(0, 0, width, 30), headerPaint)

        // Title
        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#0F172A")
            textSize = 44f
            textAlign = Paint.Align.CENTER
            isFakeBoldText = true
        }
        canvas.drawText("Identification Document Scan", width / 2f, 110f, titlePaint)

        // Draw Front Card Box
        val cardWidth = 950f
        val cardHeight = 600f
        val marginX = (width - cardWidth) / 2f

        val topFront = 180f
        canvas.drawText("— FRONT SIDE —", width / 2f, topFront + 40f, textPaint)
        canvas.drawBitmap(frontBitmap, null, RectF(marginX, topFront + 60f, marginX + cardWidth, topFront + 60f + cardHeight), paint)

        // Draw Back Card Box
        val topBack = topFront + cardHeight + 140f
        canvas.drawText("— BACK SIDE —", width / 2f, topBack + 40f, textPaint)
        canvas.drawBitmap(backBitmap, null, RectF(marginX, topBack + 60f, marginX + cardWidth, topBack + 60f + cardHeight), paint)

        result
    }
}
