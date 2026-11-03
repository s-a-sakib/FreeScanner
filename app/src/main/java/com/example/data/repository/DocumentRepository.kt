package com.example.data.repository

import android.graphics.Bitmap
import com.example.data.dao.DocumentDao
import com.example.data.dao.PageDao
import com.example.data.entity.Document
import com.example.data.entity.Page
import com.example.data.storage.FileStorageManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File

class DocumentRepository(
    private val documentDao: DocumentDao,
    private val pageDao: PageDao,
    private val fileStorage: FileStorageManager
) {
    fun getAllDocuments(): Flow<List<Document>> = documentDao.getAllDocuments()

    fun getRecentDocuments(limit: Int = 10): Flow<List<Document>> = documentDao.getRecentDocuments(limit)

    fun getDocumentsByFolder(folderId: Long): Flow<List<Document>> = documentDao.getDocumentsByFolder(folderId)

    fun getDocumentFlow(id: Long): Flow<Document?> = documentDao.getDocumentFlowById(id)

    suspend fun getDocument(id: Long): Document? = documentDao.getDocumentById(id)

    fun getPagesForDocument(docId: Long): Flow<List<Page>> = pageDao.getPagesForDocument(docId)

    suspend fun getPagesListForDocument(docId: Long): List<Page> = pageDao.getPagesListForDocument(docId)

    suspend fun createDocument(title: String, folderId: Long? = null): Long = withContext(Dispatchers.IO) {
        val doc = Document(
            title = title,
            folderId = folderId,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
            pageCount = 0
        )
        documentDao.insertDocument(doc)
    }

    suspend fun addPageToDocument(
        docId: Long,
        originalBitmap: Bitmap,
        filter: String = "AUTO",
        ocrText: String? = null,
        processedBitmap: Bitmap? = null
    ): Page = withContext(Dispatchers.IO) {
        val currentCount = pageDao.getPageCount(docId)
        val docDir = fileStorage.getDocDir(docId)

        val pageOrder = currentCount
        val origFile = File(docDir, "orig_page_$pageOrder.jpg")
        val procFile = File(docDir, "proc_page_$pageOrder.jpg")

        fileStorage.saveBitmap(origFile, originalBitmap, quality = 95)
        fileStorage.saveBitmap(procFile, processedBitmap ?: originalBitmap, quality = 90)

        val page = Page(
            documentId = docId,
            orderIndex = pageOrder,
            originalPath = origFile.absolutePath,
            processedPath = procFile.absolutePath,
            filter = filter,
            rotation = 0,
            ocrText = ocrText
        )
        val pageId = pageDao.insertPage(page)

        // Update document page count & thumbnail
        val doc = documentDao.getDocumentById(docId)
        if (doc != null) {
            val thumb = if (pageOrder == 0) procFile.absolutePath else doc.thumbnailPath ?: procFile.absolutePath
            documentDao.updateDocument(
                doc.copy(
                    pageCount = currentCount + 1,
                    thumbnailPath = thumb,
                    updatedAt = System.currentTimeMillis()
                )
            )
        }

        page.copy(id = pageId)
    }

    suspend fun updatePage(page: Page) = withContext(Dispatchers.IO) {
        pageDao.updatePage(page)
        val doc = documentDao.getDocumentById(page.documentId)
        if (doc != null) {
            documentDao.updateDocument(doc.copy(updatedAt = System.currentTimeMillis()))
        }
    }

    suspend fun reorderPages(docId: Long, reorderedPages: List<Page>) = withContext(Dispatchers.IO) {
        val updated = reorderedPages.mapIndexed { index, p ->
            p.copy(orderIndex = index)
        }
        pageDao.updatePages(updated)
        val doc = documentDao.getDocumentById(docId)
        if (doc != null && updated.isNotEmpty()) {
            documentDao.updateDocument(
                doc.copy(
                    thumbnailPath = updated.first().processedPath,
                    updatedAt = System.currentTimeMillis()
                )
            )
        }
    }

    suspend fun deletePage(page: Page) = withContext(Dispatchers.IO) {
        pageDao.deletePage(page)
        val remaining = pageDao.getPagesListForDocument(page.documentId)
        val updated = remaining.mapIndexed { index, p -> p.copy(orderIndex = index) }
        pageDao.updatePages(updated)

        val doc = documentDao.getDocumentById(page.documentId)
        if (doc != null) {
            val thumb = updated.firstOrNull()?.processedPath
            documentDao.updateDocument(
                doc.copy(
                    pageCount = updated.size,
                    thumbnailPath = thumb,
                    updatedAt = System.currentTimeMillis()
                )
            )
        }
    }

    suspend fun deleteDocument(docId: Long) = withContext(Dispatchers.IO) {
        val doc = documentDao.getDocumentById(docId)
        if (doc != null) {
            documentDao.deleteDocument(doc)
            fileStorage.deleteDocumentFiles(docId)
        }
    }

    suspend fun deleteDocuments(docIds: List<Long>) = withContext(Dispatchers.IO) {
        documentDao.deleteDocumentsByIds(docIds)
        for (id in docIds) {
            fileStorage.deleteDocumentFiles(id)
        }
    }

    suspend fun renameDocument(docId: Long, newTitle: String) = withContext(Dispatchers.IO) {
        val doc = documentDao.getDocumentById(docId)
        if (doc != null) {
            documentDao.updateDocument(doc.copy(title = newTitle, updatedAt = System.currentTimeMillis()))
        }
    }

    suspend fun moveDocumentToFolder(docId: Long, folderId: Long?) = withContext(Dispatchers.IO) {
        val doc = documentDao.getDocumentById(docId)
        if (doc != null) {
            documentDao.updateDocument(doc.copy(folderId = folderId, updatedAt = System.currentTimeMillis()))
        }
    }

    suspend fun toggleLock(docId: Long) = withContext(Dispatchers.IO) {
        val doc = documentDao.getDocumentById(docId)
        if (doc != null) {
            documentDao.updateDocument(doc.copy(isLocked = !doc.isLocked, updatedAt = System.currentTimeMillis()))
        }
    }

    fun searchDocuments(query: String): Flow<List<Document>> = documentDao.searchDocuments(query)

}
