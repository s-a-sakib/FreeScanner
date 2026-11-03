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
        ocrText: String? = null
    ): Page = withContext(Dispatchers.IO) {
        val currentCount = pageDao.getPageCount(docId)
        val docDir = fileStorage.getDocDir(docId)

        val pageOrder = currentCount
        val origFile = File(docDir, "orig_page_$pageOrder.jpg")
        val procFile = File(docDir, "proc_page_$pageOrder.jpg")

        fileStorage.saveBitmap(origFile, originalBitmap, quality = 95)
        fileStorage.saveBitmap(procFile, originalBitmap, quality = 90)

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

    suspend fun seedSampleDataIfEmpty() = withContext(Dispatchers.IO) {
        val existing = documentDao.getDocumentById(1)
        if (existing == null) {
            // Seed a sample welcome document so users immediately have interactive items
            val docId = createDocument("Welcome to FreeScanner Guide", null)
            val sampleBitmap1 = fileStorage.createSampleDocBitmap(
                title = "Welcome to FreeScanner",
                subtitle = "Fully Offline • Fast • No Tracking",
                body = "FreeScanner is your private, on-device document scanner.\n\n" +
                        "1. Scan paper receipts, notes, and book pages with CameraX.\n" +
                        "2. Smart edge detection and perspective correction flatten skewed sheets.\n" +
                        "3. Apply crisp filters: Magic Color, B&W, and Auto.\n" +
                        "4. Organize into custom folders, search by title & OCR text.\n" +
                        "5. Use PDF Tools: Merge, Extract, Sign, Watermark, and Compress.\n\n" +
                        "All processing runs strictly on your Android device."
            )
            val sampleBitmap2 = fileStorage.createSampleDocBitmap(
                title = "Offline Privacy Guarantee",
                subtitle = "No Accounts • No Internet • F-Droid Compliant",
                body = "Zero Tracking Policy:\n\n" +
                        "• No analytics, telemetry, or crash reporting SDKs.\n" +
                        "• No internet access permission in manifest.\n" +
                        "• Only camera permission is requested for scanning.\n" +
                        "• Files stay strictly inside app-private sandbox.\n" +
                        "• Exports use the secure system Storage Access Framework (SAF)."
            )
            addPageToDocument(
                docId,
                sampleBitmap1,
                filter = "MAGIC_COLOR",
                ocrText = "Welcome to FreeScanner. Fully Offline, Fast, No Tracking. Clean filters and PDF tools."
            )
            addPageToDocument(
                docId,
                sampleBitmap2,
                filter = "AUTO",
                ocrText = "Offline Privacy Guarantee. Zero tracking policy. No internet permission required."
            )

            // Seed a second sample document: Invoice & Receipt
            val receiptDocId = createDocument("Sample Purchase Receipt", null)
            val receiptBitmap = fileStorage.createSampleDocBitmap(
                title = "Official Store Receipt",
                subtitle = "Invoice #FS-9042 • Nov 2026",
                body = "ITEM                            QTY    TOTAL\n" +
                        "----------------------------------------------\n" +
                        "Document Folder Archiver         1     $14.50\n" +
                        "Matte Scanning Backdrop          1     $22.00\n" +
                        "High-Res Optical Calibration     1      $9.99\n" +
                        "----------------------------------------------\n" +
                        "SUBTOTAL:                              $46.49\n" +
                        "TAX (8%):                               $3.72\n" +
                        "GRAND TOTAL:                           $50.21\n\n" +
                        "Status: PAID IN FULL - Thank you for your business!"
            )
            addPageToDocument(
                receiptDocId,
                receiptBitmap,
                filter = "BW",
                ocrText = "Official Store Receipt Invoice #FS-9042 Document Folder Archiver TOTAL $50.21 PAID"
            )
        }
    }
}
