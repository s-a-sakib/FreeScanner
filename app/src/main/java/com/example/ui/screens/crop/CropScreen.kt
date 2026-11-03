package com.example.ui.screens.crop

import android.graphics.Bitmap
import android.graphics.PointF
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.FreeScannerApp
import com.example.imaging.CornerPoints
import com.example.ui.viewmodel.ScannerViewModel

@Composable
fun CropScreen(
    docId: Long,
    pageIndex: Int,
    viewModel: ScannerViewModel,
    onNavigateBack: () -> Unit,
    onDoneCrop: (Long, Int) -> Unit
) {
    val context = LocalContext.current
    val app = context.applicationContext as FreeScannerApp

    var currentBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var rotationAngle by remember { mutableFloatStateOf(0f) }

    var pTopLeft by remember { mutableStateOf(Offset(80f, 120f)) }
    var pTopRight by remember { mutableStateOf(Offset(400f, 120f)) }
    var pBottomRight by remember { mutableStateOf(Offset(400f, 600f)) }
    var pBottomLeft by remember { mutableStateOf(Offset(80f, 600f)) }

    var activeCorner by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(docId, pageIndex) {
        val pages = app.documentRepository.getPagesListForDocument(docId)
        val page = pages.getOrNull(pageIndex)
        if (page != null) {
            val bmp = app.fileStorageManager.loadBitmap(page.originalPath)
            currentBitmap = bmp
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
                text = "Crop & Rectify",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            IconButton(
                onClick = {
                    rotationAngle = (rotationAngle + 90f) % 360f
                }
            ) {
                Icon(imageVector = Icons.Filled.RotateRight, contentDescription = "Rotate 90 degrees", tint = Color.White)
            }
        }

        // Draggable Crop Area
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            val maxW = constraints.maxWidth.toFloat()
            val maxH = constraints.maxHeight.toFloat()

            LaunchedEffect(maxW, maxH) {
                pTopLeft = Offset(maxW * 0.12f, maxH * 0.10f)
                pTopRight = Offset(maxW * 0.88f, maxH * 0.10f)
                pBottomRight = Offset(maxW * 0.88f, maxH * 0.90f)
                pBottomLeft = Offset(maxW * 0.12f, maxH * 0.90f)
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragStart = { offset ->
                                val hitRadius = 70f
                                activeCorner = when {
                                    (offset - pTopLeft).getDistance() < hitRadius -> "TL"
                                    (offset - pTopRight).getDistance() < hitRadius -> "TR"
                                    (offset - pBottomRight).getDistance() < hitRadius -> "BR"
                                    (offset - pBottomLeft).getDistance() < hitRadius -> "BL"
                                    else -> null
                                }
                            },
                            onDragEnd = { activeCorner = null },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                when (activeCorner) {
                                    "TL" -> pTopLeft = Offset(
                                        (pTopLeft.x + dragAmount.x).coerceIn(0f, maxW),
                                        (pTopLeft.y + dragAmount.y).coerceIn(0f, maxH)
                                    )
                                    "TR" -> pTopRight = Offset(
                                        (pTopRight.x + dragAmount.x).coerceIn(0f, maxW),
                                        (pTopRight.y + dragAmount.y).coerceIn(0f, maxH)
                                    )
                                    "BR" -> pBottomRight = Offset(
                                        (pBottomRight.x + dragAmount.x).coerceIn(0f, maxW),
                                        (pBottomRight.y + dragAmount.y).coerceIn(0f, maxH)
                                    )
                                    "BL" -> pBottomLeft = Offset(
                                        (pBottomLeft.x + dragAmount.x).coerceIn(0f, maxW),
                                        (pBottomLeft.y + dragAmount.y).coerceIn(0f, maxH)
                                    )
                                }
                            }
                        )
                    }
            ) {
                // Background Document Image
                currentBitmap?.let { bmp ->
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawImage(
                            image = bmp.asImageBitmap(),
                            dstSize = androidx.compose.ui.unit.IntSize(maxW.toInt(), maxH.toInt())
                        )
                    }
                }

                // Interactive Crop Polygon & Handles
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val cropPath = Path().apply {
                        moveTo(pTopLeft.x, pTopLeft.y)
                        lineTo(pTopRight.x, pTopRight.y)
                        lineTo(pBottomRight.x, pBottomRight.y)
                        lineTo(pBottomLeft.x, pBottomLeft.y)
                        close()
                    }

                    // Translucent darkened mask outside
                    drawPath(
                        path = cropPath,
                        color = Color(0x3310B981)
                    )

                    // Polygon contour line
                    drawPath(
                        path = cropPath,
                        color = Color(0xFF10B981),
                        style = Stroke(width = 3.dp.toPx())
                    )

                    // 4 Corner Handles
                    val handleRadius = 14.dp.toPx()
                    val handleStroke = 3.dp.toPx()

                    listOf(pTopLeft, pTopRight, pBottomRight, pBottomLeft).forEach { pt ->
                        drawCircle(color = Color.White, radius = handleRadius, center = pt)
                        drawCircle(color = Color(0xFF0EA5E9), radius = handleRadius, center = pt, style = Stroke(handleStroke))
                    }
                }

                // Magnifier Lens indicator
                activeCorner?.let { corner ->
                    Surface(
                        shape = CircleShape,
                        color = Color.Black.copy(alpha = 0.85f),
                        shadowElevation = 8.dp,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(16.dp)
                            .size(80.dp)
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(imageVector = Icons.Filled.ZoomIn, contentDescription = null, tint = Color(0xFF10B981))
                            Text(text = corner, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Bottom Controls
        Surface(
            color = Color(0xFF1E293B),
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = {
                        // Reset to default rectangle
                        pTopLeft = Offset(80f, 120f)
                        pTopRight = Offset(400f, 120f)
                        pBottomRight = Offset(400f, 600f)
                        pBottomLeft = Offset(80f, 600f)
                    },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                ) {
                    Text("Auto Detect")
                }

                Button(
                    onClick = {
                        onDoneCrop(docId, pageIndex)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("btn_done_crop")
                ) {
                    Icon(imageVector = Icons.Filled.Check, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Done Rectifying", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
