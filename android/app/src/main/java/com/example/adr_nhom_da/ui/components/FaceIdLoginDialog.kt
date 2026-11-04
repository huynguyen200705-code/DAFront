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
import com.example.adr_nhom_da.data.model.User
import com.example.adr_nhom_da.data.repository.RestaurantRepository
import com.example.adr_nhom_da.ui.theme.PrimaryRed
import com.example.adr_nhom_da.ui.theme.SecondaryDarkGold
import com.example.adr_nhom_da.ui.theme.SecondaryGold
import kotlinx.coroutines.delay

@Composable
fun FaceIdLoginDialog(
    repository: RestaurantRepository,
    targetUsername: String? = null,
    onLoginSuccess: (User) -> Unit,
    onDismiss: () -> Unit
) {
    var lensFacing by remember { mutableStateOf(CameraSelector.LENS_FACING_FRONT) }
    var authError by remember { mutableStateOf<String?>(null) }
    var matchedUser by remember { mutableStateOf<User?>(null) }
    var isVerifying by remember { mutableStateOf(false) }
    var scanCount by remember { mutableStateOf(0) }

    // Laser scan animation
    val infiniteTransition = rememberInfiniteTransition(label = "face_login_anim")
    val laserY by infiniteTransition.animateFloat(
        initialValue = 0.1f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1300, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "laser_login_pos"
    )

    fun processFaceSignature(sig: String) {
        if (isVerifying || matchedUser != null) return
        isVerifying = true
        val result = repository.authenticateByFaceId(sig, targetUsername)
        when (result) {
            is RestaurantRepository.FaceAuthResult.Success -> {
                authError = null
                matchedUser = result.user
            }
            is RestaurantRepository.FaceAuthResult.MismatchTargetUser -> {
                authError = "⚠️ Khuôn mặt KHÔNG khớp với tài khoản @$targetUsername! Đây là Face ID của tài khoản khác (@${result.matchedUser?.username ?: "khác"})."
                isVerifying = false
            }
            is RestaurantRepository.FaceAuthResult.UnrecognizedFace -> {
                authError = "⚠️ Không nhận diện được khuôn mặt. Khuôn mặt này chưa được liên kết với tài khoản nào!"
                isVerifying = false
            }
            is RestaurantRepository.FaceAuthResult.NoRegisteredFaces -> {
                authError = "⚠️ Chưa có tài khoản nào cài đặt Face ID trong hệ thống. Vui lòng đăng ký trước!"
                isVerifying = false
            }
        }
    }

    LaunchedEffect(matchedUser) {
        if (matchedUser != null) {
            delay(900)
            onLoginSuccess(matchedUser!!)
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
                        text = "Đăng Nhập Bằng Face ID",
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
                // Header instruction
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
                            text = if (targetUsername != null) "Xác thực danh tính cho: @$targetUsername" else "Nhận diện khuôn mặt tự động",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.5.sp,
                            color = PrimaryRed
                        )
                        Text(
                            text = "Hệ thống sẽ đối chiếu với Face ID đã liên kết với tài khoản",
                            fontSize = 11.sp,
                            color = Color.DarkGray
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Camera Circular Frame
                Box(
                    modifier = Modifier
                        .size(190.dp)
                        .border(
                            width = 3.dp,
                            brush = Brush.sweepGradient(
                                if (matchedUser != null) listOf(Color(0xFF00E676), Color(0xFF00E676))
                                else if (authError != null) listOf(PrimaryRed, Color(0xFFFF1744))
                                else listOf(PrimaryRed, SecondaryGold, Color(0xFF00E5FF), PrimaryRed)
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
                        enableFaceDetection = matchedUser == null,
                        onFaceDetected = { faces ->
                            if (matchedUser == null && faces.isNotEmpty()) {
                                scanCount++
                                if (scanCount >= 2) {
                                    val sig = FaceBiometricHelper.extractFaceSignature(faces.first())
                                    if (sig != null) {
                                        processFaceSignature(sig)
                                    }
                                }
                            }
                        }
                    )

                    // Laser line or Success badge
                    if (matchedUser != null) {
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
                                    text = "XÁC NHẬN THÀNH CÔNG!",
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = matchedUser?.fullName ?: "",
                                    color = Color(0xFF00E676),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(2.5.dp)
                                .offset(y = ((laserY - 0.5f) * 160).dp)
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(Color.Transparent, Color(0xFF00E5FF), Color.White, Color(0xFF00E5FF), Color.Transparent)
                                    )
                                )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Error / Info text
                authError?.let { err ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFFFEBEE))
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = err,
                            color = PrimaryRed,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                if (authError == null && matchedUser == null) {
                    Text(
                        text = if (isVerifying) "Đang so khớp với Face ID..." else "Đang quét khuôn mặt tự động...",
                        fontSize = 12.sp,
                        color = Color.DarkGray,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Fast test simulations for testing scenarios
                Text(
                    text = "Kiểm thử nhanh tình huống:",
                    fontSize = 11.sp,
                    color = Color.Gray,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            val sig = FaceBiometricHelper.createMockSignature("admin_master_face")
                            processFaceSignature(sig)
                        },
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp)
                    ) {
                        Text("👑 Admin", fontSize = 10.5.sp)
                    }

                    OutlinedButton(
                        onClick = {
                            val sig = FaceBiometricHelper.createMockSignature("staff_kitchen_face")
                            processFaceSignature(sig)
                        },
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp)
                    ) {
                        Text("👨‍🍳 Staff", fontSize = 10.5.sp)
                    }

                    OutlinedButton(
                        onClick = {
                            // Stranger mock signature
                            val sig = FaceBiometricHelper.createMockSignature("unknown_stranger_face_999")
                            processFaceSignature(sig)
                        },
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp)
                    ) {
                        Text("❌ Người Lạ", fontSize = 10.5.sp, color = PrimaryRed)
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Dùng mật khẩu", color = Color.Gray)
            }
        }
    )
}

