package com.example.ui.screens.viewer

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import com.example.data.entity.Page
import com.example.ui.viewmodel.ScannerViewModel
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentViewerScreen(
    docId: Long,
    viewModel: ScannerViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToCrop: (Long, Int) -> Unit,
    onNavigateToEdit: (Long, Int) -> Unit,
    onAddPage: (Long) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val clipboardManager = LocalClipboardManager.current

    val document by viewModel.currentViewerDoc.collectAsState()
    val pages by viewModel.currentViewerPages.collectAsState()

    var isExporting by remember { mutableStateOf(false) }
    var showOcrDialog by remember { mutableStateOf(false) }
    var ocrTextContent by remember { mutableStateOf("") }
    var showWatermarkDialog by remember { mutableStateOf(false) }
    var watermarkInput by remember { mutableStateOf("") }

    LaunchedEffect(docId) {
        viewModel.loadDocumentForViewer(docId)
    }

    val pagerState = rememberPagerState(pageCount = { pages.size })

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = document?.title ?: "Document",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (pages.isNotEmpty()) {
                            Text(
                                text = "Page ${pagerState.currentPage + 1} of ${pages.size}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(imageVector = Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    // OCR Extract Text Button
                    IconButton(
                        onClick = {
                            val curPage = pages.getOrNull(pagerState.currentPage)
                            if (curPage != null) {
                                viewModel.runOcrOnPage(curPage) { text ->
                                    ocrTextContent = text
                                    showOcrDialog = true
                                }
                            }
                        },
                        modifier = Modifier.testTag("viewer_btn_ocr")
                    ) {
                        Icon(imageVector = Icons.Filled.TextFields, contentDescription = "Extract Text")
                    }

                    // Share PDF Button
                    IconButton(
                        onClick = {
                            scope.launch {
                                isExporting = true
                                val pdfFile = viewModel.exportDocumentToPdf(docId)
                                isExporting = false
                                if (pdfFile != null && pdfFile.exists()) {
                                    val uri = FileProvider.getUriForFile(
                                        context,
                                        "${context.packageName}.fileprovider",
                                        pdfFile
                                    )
                                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = "application/pdf"
                                        putExtra(Intent.EXTRA_STREAM, uri)
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    context.startActivity(Intent.createChooser(shareIntent, "Share Document PDF"))
                                }
                            }
                        },
                        modifier = Modifier.testTag("viewer_btn_share")
                    ) {
                        Icon(imageVector = Icons.Filled.Share, contentDescription = "Share PDF")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        bottomBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Crop Button
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.clickable {
                            onNavigateToCrop(docId, pagerState.currentPage)
                        }
                    ) {
                        Icon(imageVector = Icons.Filled.Crop, contentDescription = "Crop", tint = MaterialTheme.colorScheme.primary)
                        Text("Crop", style = MaterialTheme.typography.labelSmall)
                    }

                    // Edit / Filters Button
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.clickable {
                            onNavigateToEdit(docId, pagerState.currentPage)
                        }
                    ) {
                        Icon(imageVector = Icons.Filled.Edit, contentDescription = "Filters", tint = MaterialTheme.colorScheme.primary)
                        Text("Filters", style = MaterialTheme.typography.labelSmall)
                    }

                    // Add Page Button
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.clickable {
                            onAddPage(docId)
                        }
                    ) {
                        Icon(imageVector = Icons.Filled.Add, contentDescription = "Add Page", tint = MaterialTheme.colorScheme.primary)
                        Text("Add Page", style = MaterialTheme.typography.labelSmall)
                    }

                    // Watermark / Export Button
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.clickable {
                            showWatermarkDialog = true
                        }
                    ) {
                        Icon(imageVector = Icons.Filled.PictureAsPdf, contentDescription = "Export PDF", tint = MaterialTheme.colorScheme.primary)
                        Text("Export", style = MaterialTheme.typography.labelSmall)
                    }

                    // Delete Page Button
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.clickable {
                            val cur = pages.getOrNull(pagerState.currentPage)
                            if (cur != null) {
                                viewModel.deletePage(cur)
                            }
                        }
                    ) {
                        Icon(imageVector = Icons.Filled.Delete, contentDescription = "Delete Page", tint = MaterialTheme.colorScheme.error)
                        Text("Delete", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color(0xFF0F172A)),
            contentAlignment = Alignment.Center
        ) {
            if (pages.isEmpty()) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Filled.Description,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.5f),
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Document has no pages",
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            } else {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize()
                ) { pageIdx ->
                    val page = pages[pageIdx]
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Card(
                            shape = RoundedCornerShape(8.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            AsyncImage(
                                model = File(page.processedPath),
                                contentDescription = "Page ${pageIdx + 1}",
                                contentScale = ContentScale.Fit,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.White)
                            )
                        }
                    }
                }
            }

            if (isExporting) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color.Black.copy(alpha = 0.8f),
                    modifier = Modifier.padding(24.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(16.dp))
                        Text("Generating PDF...", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    // OCR Extract Text Dialog
    if (showOcrDialog) {
        AlertDialog(
            onDismissRequest = { showOcrDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Filled.TextFields, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Extracted Text")
                }
            },
            text = {
                OutlinedTextField(
                    value = ocrTextContent,
                    onValueChange = { ocrTextContent = it },
                    label = { Text("OCR Output") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        clipboardManager.setText(AnnotatedString(ocrTextContent))
                        viewModel.showMessage("Text copied to clipboard")
                        showOcrDialog = false
                    }
                ) {
                    Icon(imageVector = Icons.Filled.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Copy")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        val sendIntent = Intent().apply {
                            action = Intent.ACTION_SEND
                            putExtra(Intent.EXTRA_TEXT, ocrTextContent)
                            type = "text/plain"
                        }
                        context.startActivity(Intent.createChooser(sendIntent, "Share Extracted Text"))
                        showOcrDialog = false
                    }
                ) {
                    Text("Share .txt")
                }
            }
        )
    }

    // Watermark & Export Dialog
    if (showWatermarkDialog) {
        AlertDialog(
            onDismissRequest = { showWatermarkDialog = false },
            title = { Text("Export Document PDF") },
            text = {
                Column {
                    Text(
                        text = "Add optional text watermark (e.g. 'CONFIDENTIAL', 'SAMPLE', 'COPY'):",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = watermarkInput,
                        onValueChange = { watermarkInput = it },
                        placeholder = { Text("Leave blank for no watermark") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showWatermarkDialog = false
                        scope.launch {
                            isExporting = true
                            val pdfFile = viewModel.exportDocumentToPdf(
                                docId = docId,
                                watermarkText = watermarkInput.ifBlank { null }
                            )
                            isExporting = false
                            if (pdfFile != null && pdfFile.exists()) {
                                viewModel.showMessage("PDF exported to ${pdfFile.name}")
                                val uri = FileProvider.getUriForFile(
                                    context,
                                    "${context.packageName}.fileprovider",
                                    pdfFile
                                )
                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "application/pdf"
                                    putExtra(Intent.EXTRA_STREAM, uri)
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(Intent.createChooser(shareIntent, "Save or Share PDF"))
                            }
                        }
                    }
                ) {
                    Text("Export PDF")
                }
            },
            dismissButton = {
                TextButton(onClick = { showWatermarkDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
