package com.example.adr_nhom_da.ui.components

import androidx.camera.core.CameraSelector
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.adr_nhom_da.data.local.FaceBiometricHelper
import com.example.adr_nhom_da.data.repository.RestaurantRepository
import com.example.adr_nhom_da.ui.theme.PrimaryRed
import com.example.adr_nhom_da.ui.theme.SecondaryDarkGold
import com.example.adr_nhom_da.ui.theme.SecondaryGold
import kotlinx.coroutines.delay

@Composable
fun FaceIdEnrollmentDialog(
    username: String,
    fullName: String,
    repository: RestaurantRepository,
    onEnrolled: () -> Unit,
    onDismiss: () -> Unit
) {
    var lensFacing by remember { mutableStateOf(CameraSelector.LENS_FACING_FRONT) }
    var collectedSignatures by remember { mutableStateOf(listOf<String>()) }
    var isCompleted by remember { mutableStateOf(false) }
    var statusText by remember { mutableStateOf("Vui lòng nhìn thẳng vào camera để cài đặt Face ID...") }

    // Laser scan animation
    val infiniteTransition = rememberInfiniteTransition(label = "face_enroll_anim")
    val laserY by infiniteTransition.animateFloat(
        initialValue = 0.1f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1300, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "laser_pos"
    )

    // Complete enrollment when enough frames collected (instantaneous 2-frame average)
    LaunchedEffect(collectedSignatures.size) {
        if (collectedSignatures.size >= 2 && !isCompleted) {
            isCompleted = true
            statusText = "✓ Đã thu thập đặc trưng khuôn mặt! Đang lưu..."
            val finalSignature = FaceBiometricHelper.averageSignatures(collectedSignatures)
                ?: collectedSignatures.first()
            repository.updateUserFaceId(username, finalSignature, broadcast = true)
            delay(400)
            onEnrolled()
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
                            .size(36.dp)
                            .background(PrimaryRed.copy(alpha = 0.15f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Face,
                            contentDescription = null,
                            tint = PrimaryRed,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Tạo Face ID Cho Tài Khoản",
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp
                    )
                }

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
                        contentDescription = "Đổi Camera",
                        tint = Color.Gray,
                        modifier = Modifier.size(20.dp)
                    )
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
                // User badge banner
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
                            text = fullName,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = PrimaryRed
                        )
                        Text(
                            text = "Tài khoản: @$username • Đang liên kết Face ID cá nhân",
                            fontSize = 11.5.sp,
                            color = Color.DarkGray
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Camera viewfinder frame
                Box(
                    modifier = Modifier
                        .size(190.dp)
                        .border(
                            width = 3.dp,
                            brush = Brush.sweepGradient(
                                listOf(PrimaryRed, SecondaryGold, Color(0xFF00E676), PrimaryRed)
                            ),
                            shape = CircleShape
                        )
                        .shadow(8.dp, CircleShape)
                        .clip(CircleShape)
                        .background(Color.Black),
                    contentAlignment = Alignment.Center
                ) {
                    CameraViewfinder(
                        modifier = Modifier.fillMaxSize(),
                        lensFacing = lensFacing,
                        enableFaceDetection = !isCompleted,
                        onFaceDetected = { faces ->
                            if (!isCompleted && faces.isNotEmpty()) {
                                val face = faces.first()
                                val sig = FaceBiometricHelper.extractFaceSignature(face)
                                if (sig != null) {
                                    val current = collectedSignatures.toMutableList()
                                    current.add(sig)
                                    collectedSignatures = current
                                    statusText = "Đang quét khuôn mặt: ${current.size}/3..."
                                }
                            }
                        }
                    )

                    // Laser beam
                    if (!isCompleted) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(2.5.dp)
                                .offset(y = ((laserY - 0.5f) * 160).dp)
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(Color.Transparent, Color(0xFF00E676), Color.White, Color(0xFF00E676), Color.Transparent)
                                    )
                                )
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.65f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = Color(0xFF00E676),
                                    modifier = Modifier.size(54.dp)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "HOÀN TẤT!",
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Progress Bar
                LinearProgressIndicator(
                    progress = { (collectedSignatures.size / 3f).coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth(0.8f)
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = Color(0xFF00E676),
                    trackColor = Color(0xFFE0E0E0)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = statusText,
                    fontSize = 12.sp,
                    color = if (isCompleted) Color(0xFF2E7D32) else Color.DarkGray,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Fast Setup / Mock Button for quick dev testing or camera-less devices
                OutlinedButton(
                    onClick = {
                        val mockSig = FaceBiometricHelper.createMockSignature(username)
                        repository.updateUserFaceId(username, mockSig, broadcast = true)
                        isCompleted = true
                        statusText = "✓ Đã gán Face ID thành công cho @$username!"
                        onEnrolled()
                    },
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Fingerprint,
                        contentDescription = null,
                        tint = SecondaryDarkGold,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Lưu Face ID ngay (Kiểm thử 1 chạm)",
                        fontSize = 11.5.sp,
                        color = SecondaryDarkGold,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Để sau / Bỏ qua", color = Color.Gray)
            }
        }
    )
}

