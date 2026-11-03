package com.example

import android.app.Application
import com.example.data.db.AppDatabase
import com.example.data.repository.DocumentRepository
import com.example.data.repository.FolderRepository
import com.example.data.storage.FileStorageManager
import com.example.imaging.EdgeDetector
import com.example.imaging.FilterProcessor
import com.example.imaging.PerspectiveCorrector
import com.example.ocr.LanguagePackManager
import com.example.ocr.OcrEngine
import com.example.pdf.PdfToolService

class FreeScannerApp : Application() {

    lateinit var appDatabase: AppDatabase
        private set

    lateinit var fileStorageManager: FileStorageManager
        private set

    lateinit var documentRepository: DocumentRepository
        private set

    lateinit var folderRepository: FolderRepository
        private set

    lateinit var pdfToolService: PdfToolService
        private set

    lateinit var edgeDetector: EdgeDetector
        private set

    lateinit var perspectiveCorrector: PerspectiveCorrector
        private set

    lateinit var filterProcessor: FilterProcessor
        private set

    lateinit var ocrEngine: OcrEngine
        private set

    lateinit var languagePackManager: LanguagePackManager
        private set

    override fun onCreate() {
        super.onCreate()

        appDatabase = AppDatabase.getDatabase(this)
        fileStorageManager = FileStorageManager(this)
        documentRepository = DocumentRepository(
            appDatabase.documentDao(),
            appDatabase.pageDao(),
            fileStorageManager
        )
        folderRepository = FolderRepository(appDatabase.folderDao())
        pdfToolService = PdfToolService(this)

        edgeDetector = EdgeDetector()
        perspectiveCorrector = PerspectiveCorrector()
        filterProcessor = FilterProcessor()

        languagePackManager = LanguagePackManager(this)
        ocrEngine = OcrEngine()

    }
}
