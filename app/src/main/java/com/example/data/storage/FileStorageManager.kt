package com.example.data.storage

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

class FileStorageManager(private val context: Context) {

    private val baseDocumentsDir: File
        get() {
            val dir = File(context.filesDir, "documents")
            if (!dir.exists()) dir.mkdirs()
            return dir
        }

    fun getDocDir(docId: Long): File {
        val docDir = File(baseDocumentsDir, docId.toString())
        if (!docDir.exists()) docDir.mkdirs()
        return docDir
    }

    suspend fun saveBitmap(file: File, bitmap: Bitmap, quality: Int = 90): File = withContext(Dispatchers.IO) {
        file.parentFile?.mkdirs()
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, out)
        }
        file
    }

    suspend fun copyUriToFile(uri: Uri, destFile: File): File = withContext(Dispatchers.IO) {
        destFile.parentFile?.mkdirs()
        context.contentResolver.openInputStream(uri)?.use { input ->
            FileOutputStream(destFile).use { output ->
                input.copyTo(output)
            }
        }
        destFile
    }

    suspend fun loadBitmap(filePath: String): Bitmap? = withContext(Dispatchers.IO) {
        val file = File(filePath)
        if (!file.exists()) return@withContext null
        BitmapFactory.decodeFile(file.absolutePath)
    }

    suspend fun deleteDocumentFiles(docId: Long) = withContext(Dispatchers.IO) {
        val dir = File(baseDocumentsDir, docId.toString())
        if (dir.exists()) {
            dir.deleteRecursively()
        }
    }

    /**
     * Creates a clean sample document bitmap with professional document formatting
     * for demo and initial testing.
     */
    suspend fun createSampleDocBitmap(title: String, subtitle: String, body: String): Bitmap = withContext(Dispatchers.Default) {
        val width = 1200
        val height = 1600
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Clean white document surface
        canvas.drawColor(Color.WHITE)

        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        // Top decorative header banner
        paint.color = Color.parseColor("#059669")
        canvas.drawRect(Rect(0, 0, width, 40), paint)

        // Document Title
        paint.color = Color.parseColor("#0F172A")
        paint.textSize = 58f
        paint.isFakeBoldText = true
        canvas.drawText(title, 90f, 150f, paint)

        // Subtitle / Date
        paint.color = Color.parseColor("#64748B")
        paint.textSize = 32f
        paint.isFakeBoldText = false
        canvas.drawText(subtitle, 90f, 210f, paint)

        // Divider
        paint.color = Color.parseColor("#E2E8F0")
        paint.strokeWidth = 3f
        canvas.drawLine(90f, 250f, width - 90f, 250f, paint)

        // Body text paragraphs
        paint.color = Color.parseColor("#334155")
        paint.textSize = 30f
        var y = 330f
        val lines = body.split("\n")
        for (line in lines) {
            canvas.drawText(line, 90f, y, paint)
            y += 50f
        }

        // Bottom footer
        paint.color = Color.parseColor("#94A3B8")
        paint.textSize = 24f
        canvas.drawText("Scanned with FreeScanner • Fully Offline • Open Source", 90f, height - 70f, paint)

        bitmap
    }
}
