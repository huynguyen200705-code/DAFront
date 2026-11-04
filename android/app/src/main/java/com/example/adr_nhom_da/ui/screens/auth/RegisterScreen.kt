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
import com.example.adr_nhom_da.data.model.UserRole
import com.example.adr_nhom_da.data.repository.RestaurantRepository
import com.example.adr_nhom_da.ui.theme.PrimaryRed
import com.example.adr_nhom_da.ui.theme.SecondaryDarkGold
import com.example.adr_nhom_da.ui.theme.SecondaryGold

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(
    repository: RestaurantRepository,
    onRegisterSuccess: () -> Unit,
    onNavigateBackToLogin: () -> Unit
) {
    var fullName by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }
    var selectedRole by remember { mutableStateOf(UserRole.USER) }
    var message by remember { mutableStateOf<String?>(null) }
    var showFaceIdEnrollment by remember { mutableStateOf(false) }
    var newlyRegisteredUser by remember { mutableStateOf<User?>(null) }

    if (showFaceIdEnrollment && newlyRegisteredUser != null) {
        com.example.adr_nhom_da.ui.components.FaceIdEnrollmentDialog(
            username = newlyRegisteredUser!!.username,
            fullName = newlyRegisteredUser!!.fullName,
            repository = repository,
            onEnrolled = {
                showFaceIdEnrollment = false
                onRegisterSuccess()
            },
            onDismiss = {
                showFaceIdEnrollment = false
                onRegisterSuccess()
            }
        )
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
                    .size(68.dp)
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
                    Icons.Default.PersonAdd,
                    contentDescription = null,
                    tint = Color(0xFF2D0C10),
                    modifier = Modifier.size(36.dp)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "GOLDEN SPOON RESTAURANT",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                color = SecondaryGold
            )

            Spacer(modifier = Modifier.height(16.dp))

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
                        text = "ĐĂNG KÝ TÀI KHOẢN",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E1E1E)
                    )
                    Text(
                        text = "Tạo tài khoản phân quyền quản lý & nhân sự",
                        fontSize = 12.sp,
                        color = Color.Gray,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    OutlinedTextField(
                        value = fullName,
                        onValueChange = {
                            fullName = it
                            message = null
                        },
                        label = { Text("Họ và tên") },
                        leadingIcon = {
                            Icon(Icons.Default.Badge, contentDescription = null, tint = PrimaryRed)
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = phone,
                        onValueChange = {
                            phone = it
                            message = null
                        },
                        label = { Text("Số điện thoại") },
                        leadingIcon = {
                            Icon(Icons.Default.Phone, contentDescription = null, tint = PrimaryRed)
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = username,
                        onValueChange = {
                            username = it
                            message = null
                        },
                        label = { Text("Tên đăng nhập") },
                        leadingIcon = {
                            Icon(Icons.Default.AccountCircle, contentDescription = null, tint = PrimaryRed)
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = password,
                        onValueChange = {
                            password = it
                            message = null
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

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        "Phân quyền tài khoản:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.DarkGray,
                        modifier = Modifier.align(Alignment.Start)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Role Cards Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Staff Option
                        val isUser = selectedRole == UserRole.USER
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { selectedRole = UserRole.USER },
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(
                                if (isUser) 2.dp else 1.dp,
                                if (isUser) SecondaryDarkGold else Color(0xFFDDDDDD)
                            ),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isUser) SecondaryGold.copy(alpha = 0.1f) else Color(0xFFFAFAFA)
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("👨‍🍳", fontSize = 20.sp)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    "Nhân Viên",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = if (isUser) SecondaryDarkGold else Color.Gray
                                )
                                Text("Phục vụ / Thu ngân", fontSize = 10.sp, color = Color.Gray)
                            }
                        }

                        // Admin Option
                        val isAdmin = selectedRole == UserRole.ADMIN
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { selectedRole = UserRole.ADMIN },
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(
                                if (isAdmin) 2.dp else 1.dp,
                                if (isAdmin) PrimaryRed else Color(0xFFDDDDDD)
                            ),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isAdmin) PrimaryRed.copy(alpha = 0.1f) else Color(0xFFFAFAFA)
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("👑", fontSize = 20.sp)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    "Quản Trị",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = if (isAdmin) PrimaryRed else Color.Gray
                                )
                                Text("Toàn quyền hệ thống", fontSize = 10.sp, color = Color.Gray)
                            }
                        }
                    }

                    message?.let {
                        Spacer(modifier = Modifier.height(10.dp))
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

                    Spacer(modifier = Modifier.height(18.dp))

                    Button(
                        onClick = {
                            if (fullName.isBlank() || username.isBlank() || password.isBlank()) {
                                message = "Vui lòng điền đầy đủ các thông tin!"
                                return@Button
                            }
                            val newUser = User(
                                fullName = fullName,
                                phone = phone,
                                username = username,
                                password = password,
                                role = selectedRole
                            )
                            val success = repository.register(newUser)
                            if (success) {
                                newlyRegisteredUser = newUser
                                showFaceIdEnrollment = true
                            } else {
                                message = "Tên đăng nhập đã tồn tại trong hệ thống!"
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryRed),
                        shape = RoundedCornerShape(12.dp),
                        elevation = ButtonDefaults.buttonElevation(4.dp)
                    ) {
                        Text("XÁC NHẬN ĐĂNG KÝ", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    TextButton(onClick = onNavigateBackToLogin) {
                        Text("Đã có tài khoản? Quay lại Đăng nhập", color = PrimaryRed, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}
