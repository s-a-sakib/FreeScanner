package com.example.data.storage

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

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

}
