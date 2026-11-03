package com.example.ui.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.FreeScannerApp
import com.example.data.entity.Document
import com.example.data.entity.Folder
import com.example.data.entity.Page
import com.example.imaging.CornerPoints
import com.example.imaging.FilterType
import com.example.pdf.PdfPageSize
import com.example.pdf.WatermarkConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

enum class SortOrder {
    DATE_DESC,
    DATE_ASC,
    NAME_ASC,
    NAME_DESC
}

class ScannerViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as FreeScannerApp
    private val docRepo = app.documentRepository
    private val folderRepo = app.folderRepository
    private val fileStorage = app.fileStorageManager
    private val pdfService = app.pdfToolService
    private val edgeDetector = app.edgeDetector
    private val perspectiveCorrector = app.perspectiveCorrector
    private val filterProcessor = app.filterProcessor
    private val ocrEngine = app.ocrEngine

    // Search and Filtering
    val searchQuery = MutableStateFlow("")
    val selectedFolderId = MutableStateFlow<Long?>(null)
    val sortOrder = MutableStateFlow(SortOrder.DATE_DESC)
    val isGridView = MutableStateFlow(false)
    val selectedDocIds = MutableStateFlow<Set<Long>>(emptySet())

    // App Preferences
    val defaultFilter = MutableStateFlow(FilterType.MAGIC_COLOR)
    val defaultPageSize = MutableStateFlow(PdfPageSize.A4)
    val imageQuality = MutableStateFlow(85)
    val appTheme = MutableStateFlow("system") // system, light, dark

    val folders: StateFlow<List<Folder>> = folderRepo.getAllFolders()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentDocuments: StateFlow<List<Document>> = docRepo.getRecentDocuments(5)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val allDocuments: StateFlow<List<Document>> = searchQuery
        .flatMapLatest { query ->
            if (query.isBlank()) {
                docRepo.getAllDocuments()
            } else {
                docRepo.searchDocuments(query.trim())
            }
        }
        .combine(selectedFolderId) { docs, folderId ->
            if (folderId == null) docs else docs.filter { it.folderId == folderId }
        }
        .combine(sortOrder) { docs, sort ->
            when (sort) {
                SortOrder.DATE_DESC -> docs.sortedByDescending { it.updatedAt }
                SortOrder.DATE_ASC -> docs.sortedBy { it.updatedAt }
                SortOrder.NAME_ASC -> docs.sortedBy { it.title.lowercase() }
                SortOrder.NAME_DESC -> docs.sortedByDescending { it.title.lowercase() }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Active viewer document & pages
    val currentViewerDoc = MutableStateFlow<Document?>(null)
    val currentViewerPages = MutableStateFlow<List<Page>>(emptyList())

    // Status Message / Toast feedback
    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    fun showMessage(msg: String) {
        _statusMessage.value = msg
    }

    fun clearMessage() {
        _statusMessage.value = null
    }

    // Load document for viewer
    fun loadDocumentForViewer(docId: Long) {
        viewModelScope.launch {
            val doc = docRepo.getDocument(docId)
            currentViewerDoc.value = doc
            docRepo.getPagesForDocument(docId).collect { pages ->
                currentViewerPages.value = pages
            }
        }
    }

    // Toggle item selection in library
    fun toggleDocSelection(docId: Long) {
        val current = selectedDocIds.value.toMutableSet()
        if (current.contains(docId)) {
            current.remove(docId)
        } else {
            current.add(docId)
        }
        selectedDocIds.value = current
    }

    fun clearSelection() {
        selectedDocIds.value = emptySet()
    }

    fun deleteSelectedDocuments() {
        val ids = selectedDocIds.value.toList()
        viewModelScope.launch {
            docRepo.deleteDocuments(ids)
            clearSelection()
            showMessage("Deleted ${ids.size} document(s)")
        }
    }

    fun deleteDocument(docId: Long) {
        viewModelScope.launch {
            docRepo.deleteDocument(docId)
            showMessage("Document deleted")
        }
    }

    fun renameDocument(docId: Long, newTitle: String) {
        if (newTitle.isBlank()) return
        viewModelScope.launch {
            docRepo.renameDocument(docId, newTitle.trim())
            currentViewerDoc.value = currentViewerDoc.value?.copy(title = newTitle.trim())
            showMessage("Renamed to '$newTitle'")
        }
    }

    fun moveDocumentToFolder(docId: Long, folderId: Long?) {
        viewModelScope.launch {
            docRepo.moveDocumentToFolder(docId, folderId)
            showMessage("Document moved")
        }
    }

    fun toggleLock(docId: Long) {
        viewModelScope.launch {
            docRepo.toggleLock(docId)
            val updated = docRepo.getDocument(docId)
            currentViewerDoc.value = updated
            showMessage(if (updated?.isLocked == true) "Document locked" else "Document unlocked")
        }
    }

    fun createFolder(name: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            folderRepo.createFolder(name.trim())
            showMessage("Folder '$name' created")
        }
    }

    fun deleteFolder(folderId: Long) {
        viewModelScope.launch {
            folderRepo.deleteFolder(folderId)
            if (selectedFolderId.value == folderId) {
                selectedFolderId.value = null
            }
            showMessage("Folder deleted")
        }
    }

    fun deletePage(page: Page) {
        viewModelScope.launch {
            docRepo.deletePage(page)
            showMessage("Page deleted")
        }
    }

    fun reorderPages(pages: List<Page>) {
        val docId = currentViewerDoc.value?.id ?: return
        viewModelScope.launch {
            docRepo.reorderPages(docId, pages)
            currentViewerPages.value = pages
        }
    }

    // Capture and save new document from Camera or imported bitmap
    fun saveScannedPage(bitmap: Bitmap, title: String? = null, onComplete: (Long) -> Unit) {
        viewModelScope.launch {
            val docTitle = title ?: ("Scan " + java.text.SimpleDateFormat("MMM dd, HH:mm", java.util.Locale.getDefault()).format(java.util.Date()))
            val docId = docRepo.createDocument(docTitle, selectedFolderId.value)

            // Auto edge detect and rectify
            val corners = edgeDetector.detectCorners(bitmap)
            val rectified = if (corners != null) {
                perspectiveCorrector.correctPerspective(bitmap, corners)
            } else {
                bitmap
            }

            // Apply default filter
            val filtered = filterProcessor.applyFilter(rectified, defaultFilter.value)

            docRepo.addPageToDocument(docId, filtered, defaultFilter.value.name)
            showMessage("Document scanned and saved")
            onComplete(docId)
        }
    }

    // Add another page to an existing document
    fun addPageToActiveDocument(docId: Long, bitmap: Bitmap) {
        viewModelScope.launch {
            val corners = edgeDetector.detectCorners(bitmap)
            val rectified = if (corners != null) {
                perspectiveCorrector.correctPerspective(bitmap, corners)
            } else {
                bitmap
            }
            val filtered = filterProcessor.applyFilter(rectified, defaultFilter.value)
            docRepo.addPageToDocument(docId, filtered, defaultFilter.value.name)
            showMessage("Page added to document")
        }
    }

    // Import images from URI picker
    fun importImages(uris: List<Uri>, onComplete: (Long) -> Unit) {
        if (uris.isEmpty()) return
        viewModelScope.launch(Dispatchers.IO) {
            val docTitle = "Imported " + java.text.SimpleDateFormat("MMM dd, HH:mm", java.util.Locale.getDefault()).format(java.util.Date())
            val docId = docRepo.createDocument(docTitle, selectedFolderId.value)

            for (uri in uris) {
                val input = getApplication<Application>().contentResolver.openInputStream(uri)
                val bitmap = BitmapFactory.decodeStream(input)
                input?.close()

                if (bitmap != null) {
                    val corners = edgeDetector.detectCorners(bitmap)
                    val rectified = if (corners != null) perspectiveCorrector.correctPerspective(bitmap, corners) else bitmap
                    val filtered = filterProcessor.applyFilter(rectified, defaultFilter.value)
                    docRepo.addPageToDocument(docId, filtered, defaultFilter.value.name)
                }
            }
            withContext(Dispatchers.Main) {
                showMessage("Imported ${uris.size} image(s)")
                onComplete(docId)
            }
        }
    }

    // Import PDF file from URI picker
    fun importPdf(uri: Uri, onComplete: (Long) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val context = getApplication<Application>()
            val tempFile = File(context.cacheDir, "import_temp.pdf")
            fileStorage.copyUriToFile(uri, tempFile)

            val bitmaps = pdfService.renderPdfPagesToBitmaps(tempFile)
            tempFile.delete()

            if (bitmaps.isNotEmpty()) {
                val docTitle = "PDF Import " + java.text.SimpleDateFormat("MMM dd, HH:mm", java.util.Locale.getDefault()).format(java.util.Date())
                val docId = docRepo.createDocument(docTitle, selectedFolderId.value)

                bitmaps.forEach { bmp ->
                    docRepo.addPageToDocument(docId, bmp, FilterType.ORIGINAL.name)
                }

                withContext(Dispatchers.Main) {
                    showMessage("Imported ${bitmaps.size} PDF page(s)")
                    onComplete(docId)
                }
            } else {
                withContext(Dispatchers.Main) {
                    showMessage("Could not parse PDF pages")
                }
            }
        }
    }

    // Export document to PDF file
    suspend fun exportDocumentToPdf(
        docId: Long,
        watermarkText: String? = null,
        pageSize: PdfPageSize = defaultPageSize.value
    ): File? = withContext(Dispatchers.IO) {
        val pages = docRepo.getPagesListForDocument(docId)
        if (pages.isEmpty()) return@withContext null

        val bitmaps = pages.mapNotNull { page ->
            fileStorage.loadBitmap(page.processedPath)
        }
        if (bitmaps.isEmpty()) return@withContext null

        val doc = docRepo.getDocument(docId)
        val safeName = (doc?.title ?: "FreeScanner_Doc")
            .replace("[^a-zA-Z0-9_-]".toRegex(), "_")
        val exportFile = File(getApplication<Application>().cacheDir, "${safeName}.pdf")

        val wm = if (!watermarkText.isNullOrBlank()) WatermarkConfig(watermarkText) else null
        pdfService.createPdfFromBitmaps(
            bitmaps = bitmaps,
            outputFile = exportFile,
            pageSize = pageSize,
            watermark = wm,
            quality = imageQuality.value
        )
    }

    // Run OCR on page
    fun runOcrOnPage(page: Page, onResult: (String) -> Unit) {
        viewModelScope.launch {
            val bitmap = fileStorage.loadBitmap(page.processedPath)
            if (bitmap != null) {
                val text = ocrEngine.extractText(bitmap)
                docRepo.updatePage(page.copy(ocrText = text))
                onResult(text)
            } else {
                onResult("Unable to load page bitmap for OCR.")
            }
        }
    }
}
