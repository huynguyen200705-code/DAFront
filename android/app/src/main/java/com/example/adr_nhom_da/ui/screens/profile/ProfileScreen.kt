package com.example.adr_nhom_da.ui.screens.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.adr_nhom_da.data.model.AttendanceType
import com.example.adr_nhom_da.data.model.ContractType
import com.example.adr_nhom_da.data.model.User
import com.example.adr_nhom_da.data.model.UserRole
import com.example.adr_nhom_da.data.repository.RestaurantRepository
import com.example.adr_nhom_da.ui.components.formatVnd
import com.example.adr_nhom_da.ui.screens.employee.EmployeeBadgeDialog
import com.example.adr_nhom_da.ui.screens.employee.PayslipDialog
import com.example.adr_nhom_da.ui.theme.PrimaryRed

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    currentUser: User,
    repository: RestaurantRepository,
    onLogout: () -> Unit
) {
    var showChangePasswordDialog by remember { mutableStateOf(false) }
    var showSelfPayslipDialog by remember { mutableStateOf(false) }
    var showSelfBadgeDialog by remember { mutableStateOf(false) }

    var selfEmployee by remember(currentUser.fullName) {
        mutableStateOf(
            repository.getAllEmployees().find { it.name.contains(currentUser.fullName, ignoreCase = true) }
            ?: repository.getAllEmployees().firstOrNull()
        )
    }

    val lastAtt = selfEmployee?.let { repository.getLastAttendanceForEmployee(it.id) }
    val isCheckedIn = lastAtt?.type == AttendanceType.CHECK_IN

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Trang Cá Nhân & Thẻ Nhân Viên", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // User Avatar Box
            Box(
                modifier = Modifier
                    .size(90.dp)
                    .background(PrimaryRed, shape = CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Person,
                    contentDescription = null,
                    modifier = Modifier.size(54.dp),
                    tint = Color.White
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(currentUser.fullName, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text("Tài khoản: @${currentUser.username}", fontSize = 14.sp, color = Color.Gray)

            Spacer(modifier = Modifier.height(6.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                AssistChip(
                    onClick = {},
                    label = {
                        Text(
                            if (currentUser.role == UserRole.ADMIN) "Quản Lý (ADMIN)" else "Nhân Viên (STAFF)",
                            fontWeight = FontWeight.Bold
                        )
                    }
                )

                if (selfEmployee != null) {
                    AssistChip(
                        onClick = {},
                        leadingIcon = {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(if (isCheckedIn) Color(0xFF2E7D32) else Color.Gray)
                            )
                        },
                        label = {
                            Text(
                                if (isCheckedIn) "Đang trong ca" else "Chưa vào ca",
                                color = if (isCheckedIn) Color(0xFF2E7D32) else Color.Gray,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Staff Quick Stats Card
            if (selfEmployee != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Chức Vụ", fontSize = 11.sp, color = Color.Gray)
                            Text(selfEmployee!!.position, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                        Box(modifier = Modifier.width(1.dp).height(24.dp).background(Color(0xFFEEEEEE)))
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Thời Lượng", fontSize = 11.sp, color = Color.Gray)
                            Text(
                                if (selfEmployee!!.contractType == ContractType.HOURLY) "${selfEmployee!!.workedHours}h" else "${selfEmployee!!.workedDays} ngày",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Box(modifier = Modifier.width(1.dp).height(24.dp).background(Color(0xFFEEEEEE)))
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Lương Dự Tính", fontSize = 11.sp, color = Color.Gray)
                            Text(
                                formatVnd(selfEmployee!!.calculateSalary()),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = PrimaryRed
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
            }

            // Action Buttons Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(2.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    if (selfEmployee != null) {
                        // Smart Badge & QR Code
                        TextButton(
                            onClick = { showSelfBadgeDialog = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.QrCode, contentDescription = null, tint = Color(0xFF1976D2))
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Thẻ Nhân Viên & Mã QR / NFC", color = Color.Black, fontWeight = FontWeight.SemiBold)
                                    Text("Mở thẻ định danh thông minh & chấm công nhanh", fontSize = 11.sp, color = Color.Gray)
                                }
                            }
                        }

                        HorizontalDivider(color = Color(0xFFEEEEEE))

                        // Payslip Button
                        TextButton(
                            onClick = { showSelfPayslipDialog = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.AutoMirrored.Filled.ReceiptLong, contentDescription = null, tint = PrimaryRed)
                                Spacer(modifier = Modifier.width(12.dp))
                                Text("Xem Phiếu Lương Cá Nhân", color = Color.Black)
                            }
                        }
                        HorizontalDivider(color = Color(0xFFEEEEEE))
                    }

                    // Change Password
                    TextButton(
                        onClick = { showChangePasswordDialog = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Lock, contentDescription = null, tint = PrimaryRed)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Đổi Mật Khẩu", color = Color.Black)
                        }
                    }

                    HorizontalDivider(color = Color(0xFFEEEEEE))

                    // Logout
                    TextButton(
                        onClick = onLogout,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, tint = PrimaryRed)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Đăng Xuất", color = PrimaryRed, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    // Smart Badge & NFC/QR Dialog
    if (showSelfBadgeDialog && selfEmployee != null) {
        EmployeeBadgeDialog(
            employee = selfEmployee!!,
            repository = repository,
            onAttendanceUpdated = {
                selfEmployee = repository.getAllEmployees().find { it.id == selfEmployee!!.id }
            },
            onDismiss = { showSelfBadgeDialog = false }
        )
    }

    // Self Payslip Dialog
    if (showSelfPayslipDialog && selfEmployee != null) {
        PayslipDialog(
            employee = selfEmployee!!,
            onDismiss = { showSelfPayslipDialog = false }
        )
    }

    // Change Password Dialog
    if (showChangePasswordDialog) {
        var newPassword by remember { mutableStateOf("") }
        var successMessage by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { showChangePasswordDialog = false },
            title = { Text("Đổi Mật Khẩu") },
            text = {
                Column {
                    OutlinedTextField(
                        value = newPassword,
                        onValueChange = { newPassword = it },
                        label = { Text("Mật khẩu mới") },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth()
                    )
                    successMessage?.let {
                        Text(it, color = PrimaryRed, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp))
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newPassword.isNotBlank()) {
                            val success = repository.updateUserPassword(currentUser.id, newPassword)
                            if (success) {
                                successMessage = "Đổi mật khẩu thành công!"
                            } else {
                                successMessage = "Lỗi khi cập nhật mật khẩu!"
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryRed)
                ) {
                    Text("Cập Nhật")
                }
            },
            dismissButton = {
                TextButton(onClick = { showChangePasswordDialog = false }) { Text("Đóng") }
            }
        )
    }
}
