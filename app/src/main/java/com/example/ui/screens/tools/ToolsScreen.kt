package com.example.ui.screens.tools

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Compress
import androidx.compose.material.icons.filled.Draw
import androidx.compose.material.icons.filled.DynamicFeed
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LowPriority
import androidx.compose.material.icons.filled.MergeType
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.viewmodel.ScannerViewModel

data class ScannerToolItem(
    val key: String,
    val title: String,
    val description: String,
    val icon: ImageVector,
    val color: Color
)

val allScannerTools = listOf(
    ScannerToolItem("merge", "Merge PDFs", "Combine multiple documents into one single PDF", Icons.Filled.MergeType, Color(0xFF059669)),
    ScannerToolItem("extract", "Extract Pages", "Export selected pages to a separate file", Icons.Filled.DynamicFeed, Color(0xFF0EA5E9)),
    ScannerToolItem("reorder", "Reorder Pages", "Rearrange page ordering visually", Icons.Filled.LowPriority, Color(0xFF6366F1)),
    ScannerToolItem("pdf_to_images", "PDF to Images", "Render PDF pages into high-res JPG/PNG", Icons.Filled.Image, Color(0xFF8B5CF6)),
    ScannerToolItem("compress", "Compress PDF", "Reduce PDF file size with preset quality", Icons.Filled.Compress, Color(0xFFEC4899)),
    ScannerToolItem("lock", "Lock / Password", "Protect document with AES encryption", Icons.Filled.Lock, Color(0xFFF59E0B)),
    ScannerToolItem("sign", "Sign Document", "Draw digital signature and stamp on page", Icons.Filled.Draw, Color(0xFF10B981)),
    ScannerToolItem("watermark", "Add Watermark", "Apply text watermark with custom opacity & angle", Icons.Filled.WaterDrop, Color(0xFF38BDF8)),
    ScannerToolItem("id_card", "ID Card Scan", "Scan front & back onto one standard A4 sheet", Icons.Filled.Badge, Color(0xFF3B82F6)),
    ScannerToolItem("ocr", "Extract Text (OCR)", "On-device text extraction to copy or share", Icons.Filled.TextFields, Color(0xFF14B8A6))
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ToolsScreen(
    viewModel: ScannerViewModel,
    onNavigateToToolAction: (String) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("PDF & Scan Tools", fontWeight = FontWeight.Bold)
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(allScannerTools) { tool ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { onNavigateToToolAction(tool.key) }
                        .testTag("tool_${tool.key}"),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = tool.color.copy(alpha = 0.15f),
                            modifier = Modifier.size(46.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = tool.icon,
                                    contentDescription = null,
                                    tint = tool.color,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = tool.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = tool.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            lineHeight = 16.sp
                        )
                    }
                }
            }
        }
    }
}
