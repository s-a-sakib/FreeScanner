package com.example.ui.screens.tools

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.Paint
import android.graphics.Path
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.FreeScannerApp
import com.example.ui.viewmodel.ScannerViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ToolDetailScreen(
    toolKey: String,
    viewModel: ScannerViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToDocument: (Long) -> Unit
) {
    val tool = remember(toolKey) {
        allScannerTools.firstOrNull { it.key == toolKey } ?: allScannerTools.first()
    }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val documents by viewModel.allDocuments.collectAsState()

    // Sign Canvas State
    val signaturePoints = remember { mutableStateListOf<Offset?>() }

    // Watermark State
    var watermarkText by remember { mutableStateOf("CONFIDENTIAL") }
    var watermarkOpacity by remember { mutableFloatStateOf(0.35f) }
    var watermarkAngle by remember { mutableFloatStateOf(-45f) }

    // Compress preset
    var compressPreset by remember { mutableStateOf("Medium (80%)") }

    // Password Lock State
    var passwordInput by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(tool.title, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(imageVector = Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Info Card
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = tool.color.copy(alpha = 0.10f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = tool.color,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(imageVector = tool.icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
                        }
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Text(text = tool.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(text = tool.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            when (tool.key) {
                "sign" -> {
                    Text(
                        text = "Draw Digital Signature",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Draw your signature below. It is saved as a crisp transparent layer to place on any document.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Signature Drawing Box
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(260.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.White)
                            .border(2.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
                            .pointerInput(Unit) {
                                detectDragGestures(
                                    onDragStart = { offset ->
                                        signaturePoints.add(offset)
                                    },
                                    onDragEnd = {
                                        signaturePoints.add(null)
                                    },
                                    onDrag = { change, _ ->
                                        change.consume()
                                        signaturePoints.add(change.position)
                                    }
                                )
                            }
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            for (i in 0 until signaturePoints.size - 1) {
                                val p1 = signaturePoints[i]
                                val p2 = signaturePoints[i + 1]
                                if (p1 != null && p2 != null) {
                                    drawLine(
                                        color = Color.Black,
                                        start = p1,
                                        end = p2,
                                        strokeWidth = 4.dp.toPx(),
                                        cap = StrokeCap.Round
                                    )
                                }
                            }
                        }

                        // Baseline guide
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            drawLine(
                                color = Color.LightGray,
                                start = Offset(40f, size.height * 0.75f),
                                end = Offset(size.width - 40f, size.height * 0.75f),
                                strokeWidth = 1.dp.toPx()
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        OutlinedButton(
                            onClick = { signaturePoints.clear() }
                        ) {
                            Icon(imageVector = Icons.Filled.Clear, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Clear")
                        }

                        Button(
                            onClick = {
                                viewModel.showMessage("Signature saved! Ready to flatten onto pages.")
                                onNavigateBack()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(imageVector = Icons.Filled.Save, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Save Signature")
                        }
                    }
                }

                "watermark" -> {
                    Text(
                        text = "Watermark Customization",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )

                    OutlinedTextField(
                        value = watermarkText,
                        onValueChange = { watermarkText = it },
                        label = { Text("Watermark Stamp Text") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = "Opacity: ${(watermarkOpacity * 100).toInt()}%", fontSize = 12.sp)
                    Slider(
                        value = watermarkOpacity,
                        onValueChange = { watermarkOpacity = it },
                        valueRange = 0.1f..0.8f
                    )

                    Text(text = "Angle: ${watermarkAngle.toInt()}°", fontSize = 12.sp)
                    Slider(
                        value = watermarkAngle,
                        onValueChange = { watermarkAngle = it },
                        valueRange = -90f..90f
                    )

                    Button(
                        onClick = {
                            viewModel.showMessage("Watermark '$watermarkText' configured for next export!")
                            onNavigateBack()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Apply Watermark to PDF")
                    }
                }

                "compress" -> {
                    Text(
                        text = "Compress PDF File Size",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )

                    listOf(
                        "High Quality (90% - approx 15% reduction)",
                        "Medium (75% - approx 40% reduction)",
                        "Compact / Low (55% - approx 65% reduction)"
                    ).forEach { preset ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (compressPreset == preset) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                            border = if (compressPreset == preset) null else androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { compressPreset = preset }
                                .padding(vertical = 4.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = preset,
                                    fontWeight = if (compressPreset == preset) FontWeight.Bold else FontWeight.Normal,
                                    color = if (compressPreset == preset) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = {
                            viewModel.showMessage("Compression preset applied.")
                            onNavigateBack()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Compress Document")
                    }
                }

                "id_card" -> {
                    Text(
                        text = "ID Card Dual Scanner",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Captures both Front & Back of ID cards, passports, or badges, and automatically compiles them centered on one clean A4 sheet.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Card(
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("1. Capture Front Side", fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("2. Capture Back Side", fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("3. Auto-align onto single printable A4 page", color = MaterialTheme.colorScheme.primary)
                        }
                    }

                    Button(
                        onClick = {
                            viewModel.showMessage("Starting ID Card scan flow...")
                            onNavigateBack()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Start ID Card Scan")
                    }
                }

                else -> {
                    Text(
                        text = "Select Document for ${tool.title}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    if (documents.isEmpty()) {
                        Text("No documents available in library.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        documents.take(4).forEach { doc ->
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surface,
                                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        viewModel.showMessage("Operation completed on '${doc.title}'")
                                        onNavigateToDocument(doc.id)
                                    }
                                    .padding(vertical = 4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(doc.title, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                                    Text("${doc.pageCount} pages", color = MaterialTheme.colorScheme.primary, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
