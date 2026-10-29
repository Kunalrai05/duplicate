package com.example.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.ui.theme.RaiGold
import com.example.ui.theme.RaiNavyDark
import com.example.ui.theme.RaiWaterBlue
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

@Composable
fun CameraCaptureScreen(
    onPhotoCaptured: (File) -> Unit,
    onClose: () -> Unit,
    isHindi: Boolean = true,
    modifier: Modifier = Modifier
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
        if (!isGranted) {
            Toast.makeText(
                context,
                if (isHindi) "फोटो खींचने के लिए कैमरा अनुमति आवश्यक है" else "Camera permission is required",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    // Gallery Picker Fallback
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val scansDir = File(context.filesDir, "scans")
                if (!scansDir.exists()) scansDir.mkdirs()
                val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                val destFile = File(scansDir, "SCAN_${timeStamp}.jpg")

                context.contentResolver.openInputStream(uri)?.use { input ->
                    destFile.outputStream().use { output -> input.copyTo(output) }
                }
                onPhotoCaptured(destFile)
            } catch (e: Exception) {
                Toast.makeText(context, "गैलरी से लोड करने में त्रुटि: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Camera state
    var lensFacing by remember { mutableStateOf(CameraSelector.LENS_FACING_BACK) }
    var isFlashOn by remember { mutableStateOf(false) }
    var isCapturing by remember { mutableStateOf(false) }
    var camera by remember { mutableStateOf<Camera?>(null) }
    var cameraProvider by remember { mutableStateOf<ProcessCameraProvider?>(null) }
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    var cameraBindError by remember { mutableStateOf<String?>(null) }

    val cameraExecutor: ExecutorService = remember { Executors.newSingleThreadExecutor() }

    DisposableEffect(Unit) {
        onDispose {
            cameraExecutor.shutdown()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        if (hasCameraPermission && cameraBindError == null) {
            // Live Camera Viewfinder
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
                            val provider = cameraProviderFuture.get()
                            cameraProvider = provider

                            val preview = Preview.Builder().build().also {
                                it.setSurfaceProvider(previewView.surfaceProvider)
                            }

                            val capture = ImageCapture.Builder()
                                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                                .build()
                            imageCapture = capture

                            val cameraSelector = CameraSelector.Builder()
                                .requireLensFacing(lensFacing)
                                .build()

                            provider.unbindAll()
                            camera = provider.bindToLifecycle(
                                lifecycleOwner,
                                cameraSelector,
                                preview,
                                capture
                            )
                        } catch (e: Exception) {
                            cameraBindError = e.message ?: "Camera initialization error"
                        }
                    }, ContextCompat.getMainExecutor(ctx))

                    previewView
                },
                modifier = Modifier.fillMaxSize()
            )
        } else {
            // Permission or Camera Unavailable Fallback Screen
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    Icons.Default.CameraAlt,
                    contentDescription = null,
                    tint = RaiGold,
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = if (isHindi) "कैमरा स्क्रीन (Camera Viewfinder)" else "Camera Viewfinder",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = if (hasCameraPermission)
                        (if (isHindi) "इम्यूलेटर/डिवाइस कैमरा व्यस्त है। आप गैलरी से रजिस्टर फोटो चुन सकते हैं।" else "Camera unavailable in emulator. Please pick sheet photo from gallery.")
                    else
                        (if (isHindi) "फोटो खींचने हेतु कैमरा अनुमति आवश्यक है।" else "Camera permission is required to capture photos."),
                    fontSize = 13.sp,
                    color = Color(0xFFC7D7EC),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(24.dp))

                if (!hasCameraPermission) {
                    Button(
                        onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                        colors = ButtonDefaults.buttonColors(containerColor = RaiGold),
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        Text(if (isHindi) "कैमरा अनुमति दें (Grant Permission)" else "Grant Permission", color = RaiNavyDark, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                Button(
                    onClick = { galleryLauncher.launch("image/*") },
                    colors = ButtonDefaults.buttonColors(containerColor = RaiWaterBlue),
                    modifier = Modifier.fillMaxWidth().height(48.dp).testTag("btn_camera_gallery_fallback")
                ) {
                    Icon(Icons.Default.Image, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (isHindi) "गैलरी से फोटो चुनें (Pick Gallery)" else "Pick Photo From Gallery", fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedButton(
                    onClick = onClose,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth().height(46.dp)
                ) {
                    Text(if (isHindi) "वापस जाएं (Go Back)" else "Go Back")
                }
            }
        }

        // Top Control Overlay Bar
        Surface(
            color = Color.Black.copy(alpha = 0.45f),
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onClose,
                    modifier = Modifier.testTag("btn_close_camera")
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Close",
                        tint = Color.White
                    )
                }

                Text(
                    text = if (isHindi) "रजिस्टर शीट फोटो खींचें" else "Scan Register Sheet",
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontSize = 15.sp
                )

                // Flash toggle
                IconButton(
                    onClick = {
                        val newFlash = !isFlashOn
                        isFlashOn = newFlash
                        camera?.cameraControl?.enableTorch(newFlash)
                    }
                ) {
                    Icon(
                        if (isFlashOn) Icons.Default.FlashOn else Icons.Default.FlashOff,
                        contentDescription = "Flash",
                        tint = if (isFlashOn) RaiGold else Color.White
                    )
                }
            }
        }

        // Guidelines Grid overlay (Center reticle for register sheet alignment)
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Transparent),
            border = BorderStroke(1.5.dp, RaiGold.copy(alpha = 0.6f)),
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .height(380.dp)
                .align(Alignment.Center)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                Text(
                    text = if (isHindi) "शीट को फ्रेम के अंदर रखें" else "Align Register Sheet inside frame",
                    color = RaiGold,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 12.dp)
                        .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }

        // Bottom Capture Controls Bar
        Surface(
            color = Color.Black.copy(alpha = 0.55f),
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 18.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Gallery Shortcut
                IconButton(
                    onClick = { galleryLauncher.launch("image/*") },
                    modifier = Modifier
                        .size(48.dp)
                        .background(Color.White.copy(alpha = 0.15f), CircleShape)
                        .testTag("btn_camera_open_gallery")
                ) {
                    Icon(Icons.Default.Image, contentDescription = "Gallery", tint = Color.White)
                }

                // Shutter Button
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .border(3.5.dp, Color.White, CircleShape)
                        .clickable(enabled = !isCapturing) {
                            val capture = imageCapture
                            if (capture != null) {
                                isCapturing = true
                                val scansDir = File(context.filesDir, "scans")
                                if (!scansDir.exists()) scansDir.mkdirs()
                                val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                                val photoFile = File(scansDir, "SCAN_${timeStamp}.jpg")

                                val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()
                                capture.takePicture(
                                    outputOptions,
                                    ContextCompat.getMainExecutor(context),
                                    object : ImageCapture.OnImageSavedCallback {
                                        override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                                            isCapturing = false
                                            Toast.makeText(context, if (isHindi) "फोटो कैप्चर हो गई!" else "Photo captured!", Toast.LENGTH_SHORT).show()
                                            onPhotoCaptured(photoFile)
                                        }

                                        override fun onError(exception: ImageCaptureException) {
                                            isCapturing = false
                                            Toast.makeText(context, "कैप्चर त्रुटि: ${exception.message}", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                )
                            } else {
                                // If camera not bound (e.g. emulator without camera hardware), fallback to gallery or sample
                                Toast.makeText(context, if (isHindi) "कैमरा उपलब्ध नहीं, कृपया गैलरी चुनें" else "Camera not ready, opening gallery", Toast.LENGTH_SHORT).show()
                                galleryLauncher.launch("image/*")
                            }
                        }
                        .padding(5.dp)
                        .background(if (isCapturing) Color.Gray else RaiGold, CircleShape)
                        .testTag("btn_camera_shutter"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.CameraAlt,
                        contentDescription = "Capture",
                        tint = RaiNavyDark,
                        modifier = Modifier.size(32.dp)
                    )
                }

                // Flip Camera
                IconButton(
                    onClick = {
                        lensFacing = if (lensFacing == CameraSelector.LENS_FACING_BACK) {
                            CameraSelector.LENS_FACING_FRONT
                        } else {
                            CameraSelector.LENS_FACING_BACK
                        }
                        // Rebind camera
                        try {
                            cameraProvider?.unbindAll()
                            val capture = imageCapture ?: ImageCapture.Builder().build().also { imageCapture = it }
                            val preview = Preview.Builder().build()
                            val selector = CameraSelector.Builder().requireLensFacing(lensFacing).build()
                            camera = cameraProvider?.bindToLifecycle(lifecycleOwner, selector, preview, capture)
                        } catch (_: Exception) {}
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .background(Color.White.copy(alpha = 0.15f), CircleShape)
                ) {
                    Icon(Icons.Default.Cameraswitch, contentDescription = "Flip Camera", tint = Color.White)
                }
            }
        }
    }
}
