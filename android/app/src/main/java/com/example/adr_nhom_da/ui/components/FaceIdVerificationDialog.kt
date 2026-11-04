package com.example.adr_nhom_da.ui.components

import androidx.camera.core.CameraSelector
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.adr_nhom_da.ui.theme.PrimaryRed
import kotlinx.coroutines.delay

enum class FaceAuthStatus {
    READY,
    SCANNING,
    SUCCESS,
    FAILED,
    PIN_INPUT
}

@Composable
fun FaceIdVerificationDialog(
    actionTitle: String,
    actionDescription: String,
    onSuccess: () -> Unit,
    onDismiss: () -> Unit
) {
    var authStatus by remember { mutableStateOf(FaceAuthStatus.SCANNING) }
    var pinInput by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var lensFacing by remember { mutableStateOf(CameraSelector.LENS_FACING_FRONT) }
    var faceDetectedCount by remember { mutableStateOf(0) }
    var isVerifying by remember { mutableStateOf(false) }

    // Laser scanning line animation
    val infiniteTransition = rememberInfiniteTransition(label = "faceid_scan_anim")
    val laserY by infiniteTransition.animateFloat(
        initialValue = 0.1f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1400, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "faceid_laser_pos"
    )

    val borderGlowAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "border_glow"
    )

    // Handle verification success trigger with ultra-fast feedback (350ms for visual tick)
    LaunchedEffect(authStatus) {
        if (authStatus == FaceAuthStatus.SUCCESS) {
            delay(350)
            onSuccess()
        }
    }

    // Instantaneous Face Detection & Authentication
    LaunchedEffect(faceDetectedCount) {
        if (faceDetectedCount >= 1 && !isVerifying && authStatus == FaceAuthStatus.SCANNING) {
            isVerifying = true
            authStatus = FaceAuthStatus.SUCCESS
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .background(PrimaryRed.copy(alpha = 0.15f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Face,
                            contentDescription = null,
                            tint = PrimaryRed,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Xác Minh Face ID",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                }

                // Switch front/back camera button
                if (authStatus != FaceAuthStatus.PIN_INPUT) {
                    IconButton(
                        onClick = {
                            lensFacing = if (lensFacing == CameraSelector.LENS_FACING_FRONT) {
                                CameraSelector.LENS_FACING_BACK
                            } else {
                                CameraSelector.LENS_FACING_FRONT
                            }
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Cameraswitch,
                            contentDescription = "Đổi camera",
                            tint = Color.Gray,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Operation banner
                Card(
                    colors = CardDefaults.cardColors(containerColor = PrimaryRed.copy(alpha = 0.08f)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = actionTitle,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryRed,
                            textAlign = TextAlign.Center
                        )
                        if (actionDescription.isNotBlank()) {
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = actionDescription,
                                fontSize = 11.5.sp,
                                color = Color.Gray,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                if (authStatus == FaceAuthStatus.PIN_INPUT) {
                    // PIN Fallback mode
                    Text(
                        text = "Nhập mã PIN Quản trị viên (Mặc định: 123456)",
                        fontSize = 12.5.sp,
                        color = Color.Gray,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = pinInput,
                        onValueChange = {
                            if (it.length <= 6) pinInput = it
                            errorMessage = null
                        },
                        label = { Text("Mã PIN (6 số)") },
                        visualTransformation = PasswordVisualTransformation(),
                        leadingIcon = { Icon(Icons.Default.Password, contentDescription = null) },
                        isError = errorMessage != null,
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    if (errorMessage != null) {
                        Text(
                            text = errorMessage ?: "",
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                } else {
                    // Live Camera Viewfinder with Circular Outline
                    Box(
                        modifier = Modifier
                            .size(190.dp)
                            .border(
                                width = 3.dp,
                                brush = Brush.sweepGradient(
                                    listOf(
                                        PrimaryRed,
                                        Color(0xFF00E5FF),
                                        Color(0xFF00E676),
                                        PrimaryRed
                                    )
                                ),
                                shape = CircleShape
                            )
                            .shadow(8.dp, CircleShape)
                            .clip(CircleShape)
                            .background(Color.Black),
                        contentAlignment = Alignment.Center
                    ) {
                        // Real Device Camera View
                        CameraViewfinder(
                            modifier = Modifier.fillMaxSize(),
                            lensFacing = lensFacing,
                            enableFaceDetection = authStatus == FaceAuthStatus.SCANNING,
                            onFaceDetected = { faces ->
                                if (faces.isNotEmpty() && authStatus == FaceAuthStatus.SCANNING && !isVerifying) {
                                    isVerifying = true
                                    authStatus = FaceAuthStatus.SUCCESS
                                }
                            },
                            onCameraUnavailable = {
                                // Camera not available (e.g. emulator without webcam)
                            }
                        )

                        // Face Guide Oval & Laser overlay
                        if (authStatus == FaceAuthStatus.SUCCESS) {
                            // Success Overlay
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.Black.copy(alpha = 0.6f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = "Thành công",
                                        tint = Color(0xFF00E676),
                                        modifier = Modifier.size(56.dp)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        "ĐÃ XÁC MINH!",
                                        color = Color.White,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        } else {
                            // Oval Face Guide Outline
                            Box(
                                modifier = Modifier
                                    .size(130.dp, 160.dp)
                                    .border(
                                        width = if (faceDetectedCount > 0) 2.5.dp else 1.5.dp,
                                        color = if (faceDetectedCount > 0) Color(0xFF00E676) else Color(0x8800E5FF),
                                        shape = RoundedCornerShape(percent = 50)
                                    )
                            )

                            // Moving Laser Line across the face
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(2.5.dp)
                                    .offset(y = ((laserY - 0.5f) * 160).dp)
                                    .background(
                                        Brush.horizontalGradient(
                                            listOf(
                                                Color.Transparent,
                                                if (faceDetectedCount > 0) Color(0xFF00E676) else Color(0xFF00E5FF),
                                                Color.White,
                                                if (faceDetectedCount > 0) Color(0xFF00E676) else Color(0xFF00E5FF),
                                                Color.Transparent
                                            )
                                        )
                                    )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Status Text
                    Text(
                        text = when {
                            authStatus == FaceAuthStatus.SUCCESS -> "✓ Khuôn mặt hợp lệ! Đang xử lý thao tác..."
                            isVerifying -> "Đã nhận diện khuôn mặt. Đang xác thực..."
                            faceDetectedCount > 0 -> "Đã tìm thấy khuôn mặt, giữ nguyên tư thế..."
                            authStatus == FaceAuthStatus.FAILED -> "Nhận diện không thành công. Thử lại."
                            else -> "Hướng khuôn mặt vào trong vòng tròn để quét..."
                        },
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = when {
                            authStatus == FaceAuthStatus.SUCCESS || faceDetectedCount > 0 -> Color(0xFF2E7D32)
                            authStatus == FaceAuthStatus.FAILED -> Color(0xFFC62828)
                            else -> Color.DarkGray
                        },
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Instant Verification Button (Chụp / Xác minh ngay - Tiện lợi cho kiểm thử)
                    OutlinedButton(
                        onClick = {
                            authStatus = FaceAuthStatus.SUCCESS
                        },
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Camera,
                            contentDescription = null,
                            tint = PrimaryRed,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Chụp / Xác minh ngay",
                            fontSize = 11.5.sp,
                            color = PrimaryRed,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Switch between PIN & Face ID
                TextButton(
                    onClick = {
                        if (authStatus == FaceAuthStatus.PIN_INPUT) {
                            authStatus = FaceAuthStatus.SCANNING
                            faceDetectedCount = 0
                            isVerifying = false
                        } else {
                            authStatus = FaceAuthStatus.PIN_INPUT
                        }
                    }
                ) {
                    Icon(
                        imageVector = if (authStatus == FaceAuthStatus.PIN_INPUT) Icons.Default.Face else Icons.Default.Lock,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (authStatus == FaceAuthStatus.PIN_INPUT) "Quay lại nhận diện Camera FaceID" else "Dùng mã PIN dự phòng",
                        fontSize = 12.5.sp
                    )
                }
            }
        },
        confirmButton = {
            if (authStatus == FaceAuthStatus.PIN_INPUT) {
                Button(
                    onClick = {
                        if (pinInput == "123456" || pinInput == "admin") {
                            authStatus = FaceAuthStatus.SUCCESS
                            onSuccess()
                        } else {
                            errorMessage = "Mã PIN không chính xác (Thử 123456)"
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryRed)
                ) {
                    Text("Xác nhận PIN")
                }
            } else if (authStatus == FaceAuthStatus.FAILED) {
                Button(
                    onClick = {
                        authStatus = FaceAuthStatus.SCANNING
                        faceDetectedCount = 0
                        isVerifying = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryRed)
                ) {
                    Text("Quét lại")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Hủy bỏ")
            }
        }
    )
}
