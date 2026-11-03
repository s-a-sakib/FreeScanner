package com.example.ocr

import android.content.Context
import android.graphics.Bitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class LanguagePackManager(private val context: Context) {
    val tessdataDir: File
        get() {
            val dir = File(context.filesDir, "tessdata")
            if (!dir.exists()) dir.mkdirs()
            return dir
        }

    fun getInstalledLanguages(): List<String> {
        val langs = mutableListOf("eng")
        val files = tessdataDir.listFiles { _, name -> name.endsWith(".traineddata") }
        files?.forEach { file ->
            val langCode = file.name.removeSuffix(".traineddata")
            if (!langs.contains(langCode)) {
                langs.add(langCode)
            }
        }
        return langs
    }

    suspend fun importLanguagePack(sourceFile: File, langCode: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val targetFile = File(tessdataDir, "$langCode.traineddata")
            sourceFile.copyTo(targetFile, overwrite = true)
            true
        } catch (e: Exception) {
            false
        }
    }
}

class OcrEngine {
    // The native Tesseract runtime is not bundled yet. Never return invented OCR text.
    suspend fun extractText(
        @Suppress("UNUSED_PARAMETER") bitmap: Bitmap,
        @Suppress("UNUSED_PARAMETER") lang: String = "eng"
    ): String? = withContext(Dispatchers.Default) { null }
}
