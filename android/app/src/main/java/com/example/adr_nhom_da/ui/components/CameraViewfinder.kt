package com.example.adr_nhom_da.ui.components

import android.Manifest
import android.content.pm.PackageManager
import android.util.Log
import android.view.ViewGroup
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.adr_nhom_da.ui.theme.PrimaryRed
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import java.util.concurrent.Executors

@OptIn(ExperimentalGetImage::class)
@Composable
fun CameraViewfinder(
    modifier: Modifier = Modifier,
    lensFacing: Int = CameraSelector.LENS_FACING_BACK,
    isTorchEnabled: Boolean = false,
    enableBarcodeScanner: Boolean = false,
    enableFaceDetection: Boolean = false,
    onBarcodeScanned: ((String) -> Unit)? = null,
    onFaceDetected: ((List<Face>) -> Unit)? = null,
    onCameraUnavailable: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    var cameraError by remember { mutableStateOf<String?>(null) }
    var activeCamera by remember { mutableStateOf<Camera?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
        if (!isGranted) {
            cameraError = "Quyền truy cập camera bị từ chối."
            onCameraUnavailable?.invoke()
        }
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    // Toggle torch on active camera
    LaunchedEffect(isTorchEnabled, activeCamera) {
        try {
            if (activeCamera?.cameraInfo?.hasFlashUnit() == true) {
                activeCamera?.cameraControl?.enableTorch(isTorchEnabled)
            }
        } catch (e: Exception) {
            Log.w("CameraViewfinder", "Could not toggle torch: ${e.message}")
        }
    }

    Box(
        modifier = modifier.background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        if (!hasCameraPermission) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.CameraAlt,
                    contentDescription = null,
                    tint = Color.LightGray,
                    modifier = Modifier.size(36.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Cần cấp quyền Camera",
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Vui lòng cho phép ứng dụng sử dụng Camera để quét",
                    color = Color.Gray,
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(10.dp))
                Button(
                    onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryRed),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text("Cấp Quyền", fontSize = 12.sp)
                }
            }
        } else if (cameraError != null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = Color(0xFFFFB74D),
                    modifier = Modifier.size(36.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Camera không khả dụng",
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = cameraError ?: "Không thể kết nối đến camera thiết bị",
                    color = Color.LightGray,
                    fontSize = 10.sp,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            // Live CameraX Preview
            val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
            val barcodeScanner = remember {
                if (enableBarcodeScanner) BarcodeScanning.getClient() else null
            }
            val faceDetector = remember {
                if (enableFaceDetection) {
                    val options = FaceDetectorOptions.Builder()
                        .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
                        .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
                        .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
                        .build()
                    FaceDetection.getClient(options)
                } else null
            }

            DisposableEffect(Unit) {
                onDispose {
                    cameraExecutor.shutdown()
                    barcodeScanner?.close()
                    faceDetector?.close()
                }
            }

            AndroidView(
                modifier = Modifier.fillMaxSize(),
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
                                it.setSurfaceProvider(previewView.surfaceProvider)
                            }

                            val cameraSelector = CameraSelector.Builder()
                                .requireLensFacing(lensFacing)
                                .build()

                            // Image analysis for QR / Face detection
                            val imageAnalysisBuilder = ImageAnalysis.Builder()
                                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)

                            var imageAnalysis: ImageAnalysis? = null
                            if (enableBarcodeScanner || enableFaceDetection) {
                                imageAnalysis = imageAnalysisBuilder.build().also { analysis ->
                                    analysis.setAnalyzer(cameraExecutor) { imageProxy ->
                                        val mediaImage = imageProxy.image
                                        if (mediaImage != null) {
                                            val image = InputImage.fromMediaImage(
                                                mediaImage,
                                                imageProxy.imageInfo.rotationDegrees
                                            )

                                            if (enableBarcodeScanner && barcodeScanner != null) {
                                                barcodeScanner.process(image)
                                                    .addOnSuccessListener { barcodes ->
                                                        for (barcode in barcodes) {
                                                            val rawValue = barcode.rawValue
                                                                ?: barcode.displayValue
                                                            if (!rawValue.isNullOrBlank()) {
                                                                ContextCompat.getMainExecutor(ctx).execute {
                                                                    onBarcodeScanned?.invoke(rawValue)
                                                                }
                                                                break
                                                            }
                                                        }
                                                    }
                                                    .addOnCompleteListener {
                                                        // If not also detecting face, close image
                                                        if (!enableFaceDetection) {
                                                            imageProxy.close()
                                                        }
                                                    }
                                            }

                                            if (enableFaceDetection && faceDetector != null) {
                                                faceDetector.process(image)
                                                    .addOnSuccessListener { faces ->
                                                        ContextCompat.getMainExecutor(ctx).execute {
                                                            onFaceDetected?.invoke(faces)
                                                        }
                                                    }
                                                    .addOnCompleteListener {
                                                        imageProxy.close()
                                                    }
                                            }
                                        } else {
                                            imageProxy.close()
                                        }
                                    }
                                }
                            }

                            cameraProvider.unbindAll()
                            val useCases = mutableListOf<androidx.camera.core.UseCase>(preview)
                            if (imageAnalysis != null) {
                                useCases.add(imageAnalysis)
                            }

                            val camera = cameraProvider.bindToLifecycle(
                                lifecycleOwner,
                                cameraSelector,
                                *useCases.toTypedArray()
                            )
                            activeCamera = camera

                        } catch (e: Exception) {
                            Log.e("CameraViewfinder", "Use case binding failed", e)
                            cameraError = e.localizedMessage ?: "Không thể mở camera thiết bị"
                            onCameraUnavailable?.invoke()
                        }
                    }, ContextCompat.getMainExecutor(ctx))

                    previewView
                }
            )
        }
    }
}

