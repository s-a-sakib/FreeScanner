package com.example.ui.screens.camera

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.view.ViewGroup
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.Camera
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FlashAuto
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.ui.viewmodel.ScannerViewModel
import java.io.File

@Composable
fun CameraScreen(
    viewModel: ScannerViewModel,
    onNavigateBack: () -> Unit,
    onFinishedScanning: (Long) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    // Camera states
    var isFlashOn by remember { mutableStateOf(false) }
    var isBatchMode by remember { mutableStateOf(false) }
    var isCapturing by remember { mutableStateOf(false) }
    val capturedBitmaps = remember { mutableStateListOf<Bitmap>() }

    var imageCapture: ImageCapture? by remember { mutableStateOf(null) }
    var boundCamera: Camera? by remember { mutableStateOf(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ComposeColor.Black)
    ) {
        if (hasCameraPermission) {
            // CameraX Live Preview View
            AndroidView(
                factory = { ctx ->
                    val previewView = PreviewView(ctx).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        scaleType = PreviewView.ScaleType.FILL_CENTER
                    }

                    val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                    cameraProviderFuture.addListener({
                        try {
                            val cameraProvider = cameraProviderFuture.get()
                            val preview = Preview.Builder().build().also {
                                it.surfaceProvider = previewView.surfaceProvider
                            }

                            val capture = ImageCapture.Builder()
                                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                                .build()
                            imageCapture = capture

                            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                            cameraProvider.unbindAll()
                            boundCamera = cameraProvider.bindToLifecycle(
                                lifecycleOwner,
                                cameraSelector,
                                preview,
                                capture
                            )
                        } catch (e: Exception) {
                            // Camera hardware fallback handled below
                        }
                    }, ContextCompat.getMainExecutor(ctx))

                    previewView
                },
                modifier = Modifier.fillMaxSize()
            )

            // Smart Edge Overlay / Guide
            Canvas(modifier = Modifier.fillMaxSize()) {
                val canvasW = size.width
                val canvasH = size.height
                val marginX = canvasW * 0.10f
                val marginY = canvasH * 0.16f
                val rectW = canvasW - (marginX * 2)
                val rectH = canvasH - (marginY * 2)

                // Outer guide box with glowing emerald edges
                drawRect(
                    color = ComposeColor(0xFF10B981).copy(alpha = 0.8f),
                    topLeft = Offset(marginX, marginY),
                    size = Size(rectW, rectH),
                    style = Stroke(
                        width = 4.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(30f, 18f), 0f)
                    )
                )

                // Corner bracket accents
                val bracketLen = 40.dp.toPx()
                val bracketStroke = 6.dp.toPx()
                val bracketColor = ComposeColor(0xFF38BDF8)

                // Top-Left
                drawLine(bracketColor, Offset(marginX, marginY), Offset(marginX + bracketLen, marginY), bracketStroke)
                drawLine(bracketColor, Offset(marginX, marginY), Offset(marginX, marginY + bracketLen), bracketStroke)

                // Top-Right
                drawLine(bracketColor, Offset(marginX + rectW, marginY), Offset(marginX + rectW - bracketLen, marginY), bracketStroke)
                drawLine(bracketColor, Offset(marginX + rectW, marginY), Offset(marginX + rectW, marginY + bracketLen), bracketStroke)

                // Bottom-Left
                drawLine(bracketColor, Offset(marginX, marginY + rectH), Offset(marginX + bracketLen, marginY + rectH), bracketStroke)
                drawLine(bracketColor, Offset(marginX, marginY + rectH), Offset(marginX, marginY + rectH - bracketLen), bracketStroke)

                // Bottom-Right
                drawLine(bracketColor, Offset(marginX + rectW, marginY + rectH), Offset(marginX + rectW - bracketLen, marginY + rectH), bracketStroke)
                drawLine(bracketColor, Offset(marginX + rectW, marginY + rectH), Offset(marginX + rectW, marginY + rectH - bracketLen), bracketStroke)
            }
        } else {
            // Permission missing info
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.CameraAlt,
                    contentDescription = null,
                    tint = ComposeColor.White,
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Camera Permission Required",
                    color = ComposeColor.White,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "FreeScanner requires only the camera permission to scan physical documents offline.",
                    color = ComposeColor.White.copy(alpha = 0.8f),
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Grant Camera Access")
                }
            }
        }

        // Top Toolbar: Back, Flash, Auto-Capture, Batch Mode
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onNavigateBack,
                modifier = Modifier
                    .background(ComposeColor.Black.copy(alpha = 0.5f), CircleShape)
                    .testTag("camera_btn_back")
            ) {
                Icon(imageVector = Icons.Filled.ArrowBack, contentDescription = "Back", tint = ComposeColor.White)
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // Flash toggle
                Surface(
                    shape = CircleShape,
                    color = if (isFlashOn) MaterialTheme.colorScheme.primary else ComposeColor.Black.copy(alpha = 0.5f),
                    modifier = Modifier.clickable {
                        isFlashOn = !isFlashOn
                        boundCamera?.cameraControl?.enableTorch(isFlashOn)
                    }
                ) {
                    Box(modifier = Modifier.padding(10.dp)) {
                        Icon(
                            imageVector = if (isFlashOn) Icons.Filled.FlashOn else Icons.Filled.FlashOff,
                            contentDescription = "Flash",
                            tint = ComposeColor.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // Batch Mode toggle
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = if (isBatchMode) MaterialTheme.colorScheme.primary else ComposeColor.Black.copy(alpha = 0.5f),
                    modifier = Modifier.clickable { isBatchMode = !isBatchMode }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Layers,
                            contentDescription = null,
                            tint = ComposeColor.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isBatchMode) "Batch (${capturedBitmaps.size})" else "Single",
                            color = ComposeColor.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Bottom Controls: Page counter, Shutter button, Done button
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Document Framing Hint
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = ComposeColor.Black.copy(alpha = 0.65f),
                modifier = Modifier.padding(bottom = 20.dp)
            ) {
                Text(
                    text = "Align page within corners • Auto edge detection active",
                    color = ComposeColor.White,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Page counter / Cancel
                Box(
                    modifier = Modifier.size(54.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (capturedBitmaps.isNotEmpty()) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(44.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "${capturedBitmaps.size}",
                                    color = ComposeColor.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                // Big Shutter Button
                Surface(
                    shape = CircleShape,
                    color = ComposeColor.White,
                    modifier = Modifier
                        .size(80.dp)
                        .border(4.dp, ComposeColor(0xFF10B981), CircleShape)
                        .clickable {
                            val capture = imageCapture
                            if (capture == null || isCapturing) {
                                viewModel.showMessage("Camera is starting. Try again in a moment.")
                            } else {
                                isCapturing = true
                                val output = File.createTempFile("freescanner_capture_", ".jpg", context.cacheDir)
                                val options = ImageCapture.OutputFileOptions.Builder(output).build()
                                capture.takePicture(options, ContextCompat.getMainExecutor(context),
                                    object : ImageCapture.OnImageSavedCallback {
                                        override fun onImageSaved(result: ImageCapture.OutputFileResults) {
                                            val bitmap = BitmapFactory.decodeFile(output.absolutePath)
                                            output.delete()
                                            isCapturing = false
                                            if (bitmap == null) {
                                                viewModel.showMessage("Could not read captured image")
                                            } else if (isBatchMode) {
                                                capturedBitmaps.add(bitmap)
                                                viewModel.showMessage("Page ${capturedBitmaps.size} captured")
                                            } else {
                                                viewModel.saveScannedPage(bitmap, onComplete = onFinishedScanning)
                                            }
                                        }

                                        override fun onError(exception: ImageCaptureException) {
                                            output.delete()
                                            isCapturing = false
                                            viewModel.showMessage("Capture failed: ${exception.message ?: "camera error"}")
                                        }
                                    })
                            }
                        }
                        .testTag("camera_shutter_button")
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(8.dp)
                            .background(ComposeColor(0xFF10B981), CircleShape)
                    )
                }

                // Done Button (visible when pages captured)
                Box(
                    modifier = Modifier.size(54.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (capturedBitmaps.isNotEmpty() || isBatchMode) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .size(48.dp)
                                .clickable {
                                    if (capturedBitmaps.isNotEmpty() && !isCapturing) {
                                        viewModel.saveScannedPages(capturedBitmaps.toList(), onFinishedScanning)
                                    } else {
                                        onNavigateBack()
                                    }
                                }
                                .testTag("camera_btn_done")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Filled.Check,
                                    contentDescription = "Done Scanning",
                                    tint = ComposeColor.White
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

