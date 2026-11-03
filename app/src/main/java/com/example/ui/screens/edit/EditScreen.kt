package com.example.ui.screens.edit

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.FreeScannerApp
import com.example.imaging.FilterType
import com.example.ui.viewmodel.ScannerViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun EditScreen(
    docId: Long,
    pageIndex: Int,
    viewModel: ScannerViewModel,
    onNavigateBack: () -> Unit,
    onAddAnotherPage: () -> Unit,
    onSaveDone: (Long) -> Unit
) {
    val context = LocalContext.current
    val app = context.applicationContext as FreeScannerApp

    var originalBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var displayedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var selectedFilter by remember { mutableStateOf(FilterType.MAGIC_COLOR) }
    var brightness by remember { mutableFloatStateOf(0f) }
    var contrast by remember { mutableFloatStateOf(1f) }
    var showAdjustSliders by remember { mutableStateOf(false) }

    LaunchedEffect(docId, pageIndex) {
        val pages = app.documentRepository.getPagesListForDocument(docId)
        val page = pages.getOrNull(pageIndex)
        if (page != null) {
            val bmp = app.fileStorageManager.loadBitmap(page.originalPath)
            originalBitmap = bmp
            selectedFilter = FilterType.fromName(page.filter)
            if (bmp != null) {
                displayedBitmap = app.filterProcessor.applyFilter(bmp, selectedFilter)
            }
        }
    }

    // Reapply filter & adjustments when parameters change
    fun updatePreview(filter: FilterType, bright: Float, cont: Float) {
        val orig = originalBitmap ?: return
        val filtered = app.filterProcessor.applyFilter(orig, filter)
        displayedBitmap = if (bright != 0f || cont != 1f) {
            app.filterProcessor.adjustBrightnessAndContrast(filtered, bright, cont)
        } else {
            filtered
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A))
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // Top Toolbar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onNavigateBack) {
                Icon(imageVector = Icons.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            Text(
                text = "Document Filters",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Row {
                IconButton(onClick = { showAdjustSliders = !showAdjustSliders }) {
                    Icon(
                        imageVector = Icons.Filled.Tune,
                        contentDescription = "Adjust Brightness/Contrast",
                        tint = if (showAdjustSliders) MaterialTheme.colorScheme.primary else Color.White
                    )
                }
                IconButton(onClick = {
                    displayedBitmap?.let { bmp ->
                        displayedBitmap = app.filterProcessor.rotateBitmap(bmp, 90)
                    }
                }) {
                    Icon(imageVector = Icons.Filled.RotateRight, contentDescription = "Rotate", tint = Color.White)
                }
            }
        }

        // Preview Canvas Area
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            displayedBitmap?.let { bmp ->
                Card(
                    shape = RoundedCornerShape(8.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Image(
                        bitmap = bmp.asImageBitmap(),
                        contentDescription = "Filtered Document Preview",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }

        // Optional Brightness & Contrast adjustment panel
        if (showAdjustSliders) {
            Surface(
                color = Color(0xFF1E293B),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Brightness", color = Color.White, fontSize = 12.sp, modifier = Modifier.width(80.dp))
                        Slider(
                            value = brightness,
                            onValueChange = {
                                brightness = it
                                updatePreview(selectedFilter, brightness, contrast)
                            },
                            valueRange = -50f..50f,
                            colors = SliderDefaults.colors(thumbColor = MaterialTheme.colorScheme.primary),
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Contrast", color = Color.White, fontSize = 12.sp, modifier = Modifier.width(80.dp))
                        Slider(
                            value = contrast,
                            onValueChange = {
                                contrast = it
                                updatePreview(selectedFilter, brightness, contrast)
                            },
                            valueRange = 0.5f..1.8f,
                            colors = SliderDefaults.colors(thumbColor = MaterialTheme.colorScheme.primary),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // Filter Strip Bottom Bar
        Surface(
            color = Color(0xFF1E293B),
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Filter chips
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(FilterType.entries) { filter ->
                        val isSelected = selectedFilter == filter
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) MaterialTheme.colorScheme.primary else Color(0xFF334155))
                                .clickable {
                                    selectedFilter = filter
                                    updatePreview(filter, brightness, contrast)
                                }
                                .padding(horizontal = 16.dp, vertical = 10.dp)
                                .testTag("filter_${filter.name}")
                        ) {
                            Text(
                                text = filter.displayName,
                                color = Color.White,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 13.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Action Buttons: Add Page & Save
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = onAddAnotherPage,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(imageVector = Icons.Filled.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add Page")
                    }

                    Button(
                        onClick = {
                            val bmp = displayedBitmap
                            if (bmp != null) {
                                val pageFile = File(app.fileStorageManager.getDocDir(docId), "proc_page_$pageIndex.jpg")
                                bmp.compress(Bitmap.CompressFormat.JPEG, 90, pageFile.outputStream())
                                viewModel.showMessage("Filter applied & saved")
                            }
                            onSaveDone(docId)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("btn_save_edit")
                    ) {
                        Icon(imageVector = Icons.Filled.Check, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Save Document", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
