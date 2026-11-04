package com.example.adr_nhom_da.ui.screens.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.adr_nhom_da.data.model.User
import com.example.adr_nhom_da.data.repository.RestaurantRepository
import com.example.adr_nhom_da.ui.theme.PrimaryRed
import com.example.adr_nhom_da.ui.theme.SecondaryDarkGold
import com.example.adr_nhom_da.ui.theme.SecondaryGold

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    repository: RestaurantRepository,
    onLoginSuccess: (User) -> Unit,
    onNavigateToRegister: () -> Unit
) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showFaceIdLoginDialog by remember { mutableStateOf(false) }
    var faceIdTargetUser by remember { mutableStateOf<String?>(null) }

    if (showFaceIdLoginDialog) {
        com.example.adr_nhom_da.ui.components.FaceIdLoginDialog(
            repository = repository,
            targetUsername = faceIdTargetUser,
            onLoginSuccess = { user ->
                showFaceIdLoginDialog = false
                onLoginSuccess(user)
            },
            onDismiss = {
                showFaceIdLoginDialog = false
            }
        )
    }

    fun performLogin(u: String, p: String) {
        val user = repository.login(u, p)
        if (user != null) {
            errorMessage = null
            onLoginSuccess(user)
        } else {
            errorMessage = "Tên đăng nhập hoặc mật khẩu không chính xác!"
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF2D0C10),
                        Color(0xFF190608),
                        Color(0xFF0F0405)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Restaurant Brand Emblem
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            listOf(SecondaryGold, SecondaryDarkGold)
                        )
                    )
                    .border(2.dp, Color.White.copy(alpha = 0.5f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Restaurant,
                    contentDescription = null,
                    tint = Color(0xFF2D0C10),
                    modifier = Modifier.size(44.dp)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "GOLDEN SPOON",
                fontSize = 24.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 2.sp,
                color = SecondaryGold
            )
            Text(
                text = "RESTAURANT & LOUNGE • POS 4.0",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.sp,
                color = Color.White.copy(alpha = 0.75f)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Elegant Card Container
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, SecondaryGold.copy(alpha = 0.25f), RoundedCornerShape(20.dp)),
                elevation = CardDefaults.cardElevation(10.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "ĐĂNG NHẬP HỆ THỐNG",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E1E1E)
                    )
                    Text(
                        text = "Vui lòng nhập tài khoản để vào ca làm việc",
                        fontSize = 12.sp,
                        color = Color.Gray,
                        modifier = Modifier.padding(bottom = 20.dp)
                    )

                    OutlinedTextField(
                        value = username,
                        onValueChange = {
                            username = it
                            errorMessage = null
                        },
                        label = { Text("Tên đăng nhập") },
                        leadingIcon = {
                            Icon(Icons.Default.AccountCircle, contentDescription = null, tint = PrimaryRed)
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = password,
                        onValueChange = {
                            password = it
                            errorMessage = null
                        },
                        label = { Text("Mật khẩu") },
                        leadingIcon = {
                            Icon(Icons.Default.Lock, contentDescription = null, tint = PrimaryRed)
                        },
                        trailingIcon = {
                            IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                                Icon(
                                    if (isPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = if (isPasswordVisible) "Ẩn mật khẩu" else "Hiện mật khẩu",
                                    tint = Color.Gray
                                )
                            }
                        },
                        singleLine = true,
                        visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    errorMessage?.let {
                        Spacer(modifier = Modifier.height(8.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFFFFEBEE))
                                .padding(8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "⚠️ $it",
                                color = PrimaryRed,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Button(
                        onClick = { performLogin(username, password) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryRed),
                        shape = RoundedCornerShape(12.dp),
                        elevation = ButtonDefaults.buttonElevation(4.dp)
                    ) {
                        Text("VÀO HỆ THỐNG", fontSize = 15.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Face ID Login Button
                    OutlinedButton(
                        onClick = {
                            faceIdTargetUser = if (username.isNotBlank()) username.trim() else null
                            showFaceIdLoginDialog = true
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(
                            1.5.dp,
                            Brush.horizontalGradient(listOf(PrimaryRed, SecondaryGold))
                        ),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF2D0C10))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Face,
                            contentDescription = "Face ID",
                            tint = PrimaryRed,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (username.isNotBlank()) "QUÉT FACE ID CHO @${username.trim()}" else "ĐĂNG NHẬP BẰNG FACE ID",
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF2D0C10)
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    HorizontalDivider(color = Color(0xFFEEEEEE))

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        "Đăng nhập nhanh (1-chạm thử nghiệm):",
                        fontSize = 12.sp,
                        color = Color.Gray,
                        fontWeight = FontWeight.Medium
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Admin Fast Login
                        OutlinedButton(
                            onClick = {
                                username = "admin"
                                password = "123456"
                                performLogin("admin", "123456")
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, PrimaryRed),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = PrimaryRed)
                        ) {
                            Text("👑 Admin", fontWeight = FontWeight.Bold)
                        }

                        // Staff Fast Login
                        OutlinedButton(
                            onClick = {
                                username = "staff"
                                password = "123456"
                                performLogin("staff", "123456")
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, SecondaryDarkGold),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = SecondaryDarkGold)
                        ) {
                            Text("👨‍🍳 Staff", fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    TextButton(onClick = onNavigateToRegister) {
                        Text("Chưa có tài khoản? Đăng ký tại đây", color = PrimaryRed, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            val isCloudOnline by com.example.adr_nhom_da.data.remote.RealtimeSyncManager.isCloudConnected.collectAsState()

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.White.copy(alpha = 0.12f))
                    .clickable {
                        com.example.adr_nhom_da.data.remote.RealtimeSyncManager.sendPing()
                        com.example.adr_nhom_da.data.remote.RealtimeSyncManager.broadcast(
                            "REQUEST_FULL_SYNC",
                            org.json.JSONObject(),
                            "Đồng bộ tài khoản đăng nhập"
                        )
                    }
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(if (isCloudOnline) Color(0xFF00E676) else Color(0xFFFFB300))
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    if (isCloudOnline) "Cloud Realtime: Đã kết nối (Chạm đồng bộ)" else "Cloud Realtime: Đang kết nối...",
                    fontSize = 11.sp,
                    color = Color.White.copy(alpha = 0.85f)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    Icons.Default.Sync,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.7f),
                    modifier = Modifier.size(13.dp)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                "Golden Spoon POS • Đồng bộ đa thiết bị tức thì",
                fontSize = 11.sp,
                color = Color.White.copy(alpha = 0.5f)
            )
        }
    }
}
