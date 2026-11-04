package com.example.adr_nhom_da.ui.screens.profile

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.adr_nhom_da.data.model.*
import com.example.adr_nhom_da.data.remote.RealtimeSyncManager
import com.example.adr_nhom_da.data.remote.RestaurantApiClient
import com.example.adr_nhom_da.data.remote.WebApiSyncEngine
import com.example.adr_nhom_da.data.repository.RestaurantRepository
import com.example.adr_nhom_da.ui.components.FaceIdEnrollmentDialog
import com.example.adr_nhom_da.ui.components.formatVnd
import com.example.adr_nhom_da.ui.screens.employee.EmployeeBadgeDialog
import com.example.adr_nhom_da.ui.screens.employee.PayslipDialog
import com.example.adr_nhom_da.ui.theme.PrimaryRed
import com.example.adr_nhom_da.ui.theme.SecondaryDarkGold
import com.example.adr_nhom_da.ui.theme.SecondaryGold
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    currentUser: User,
    repository: RestaurantRepository,
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    val isCloudOnline by RealtimeSyncManager.isCloudConnected.collectAsState()

    var activeUser by remember { mutableStateOf(currentUser) }
    var showChangePasswordDialog by remember { mutableStateOf(false) }
    var showSelfPayslipDialog by remember { mutableStateOf(false) }
    var showSelfBadgeDialog by remember { mutableStateOf(false) }
    var showFaceIdDialog by remember { mutableStateOf(false) }
    var showEditProfileDialog by remember { mutableStateOf(false) }
    var showAttendanceHistoryDialog by remember { mutableStateOf(false) }
    var showSystemSettingsDialog by remember { mutableStateOf(false) }
    var showAccountManagementDialog by remember { mutableStateOf(false) }
    var showDeleteFaceIdConfirmDialog by remember { mutableStateOf(false) }
    var showLogoutConfirmDialog by remember { mutableStateOf(false) }
    var showServerConfigDialog by remember { mutableStateOf(false) }
    var syncNoticeMessage by remember { mutableStateOf<String?>(null) }

    // Fetch refreshed user from repository
    LaunchedEffect(Unit) {
        val refreshed = repository.getUserById(activeUser.id)
            ?: repository.getUserByUsername(activeUser.username)
        if (refreshed != null) {
            activeUser = refreshed
        }
    }

    var selfEmployee by remember(activeUser.fullName, activeUser.username) {
        val found = repository.getAllEmployees().find {
            it.name.equals(activeUser.fullName, ignoreCase = true) ||
            it.name.equals(activeUser.username, ignoreCase = true) ||
            (activeUser.phone.isNotBlank() && it.phone == activeUser.phone)
        }
        if (found == null) {
            val empName = if (activeUser.fullName.isNotBlank()) activeUser.fullName else activeUser.username
            val newEmp = Employee(
                name = empName,
                phone = activeUser.phone.ifBlank { "0901234567" },
                position = if (activeUser.role == UserRole.ADMIN) "Quản Lý (@${activeUser.username})" else "Nhân Viên (@${activeUser.username})",
                tier = if (activeUser.role == UserRole.ADMIN) EmployeeTier.QUAN_LY else EmployeeTier.CHINH_THUC,
                contractType = ContractType.MONTHLY,
                monthlySalary = if (activeUser.role == UserRole.ADMIN) 15000000.0 else 8500000.0
            )
            repository.addEmployee(newEmp)
            mutableStateOf<Employee?>(repository.getAllEmployees().find { it.name == empName } ?: newEmp)
        } else {
            mutableStateOf<Employee?>(found)
        }
    }

    val lastAtt = selfEmployee?.let { repository.getLastAttendanceForEmployee(it.id) }
    val isCheckedIn = lastAtt?.type == AttendanceType.CHECK_IN

    val hasFaceId = !activeUser.faceIdData.isNullOrBlank()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.AccountCircle,
                            contentDescription = null,
                            tint = PrimaryRed,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Trang Cá Nhân & Hệ Thống",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    }
                },
                actions = {
                    IconButton(onClick = {
                        RealtimeSyncManager.sendPing()
                        RealtimeSyncManager.broadcast("REQUEST_FULL_SYNC", JSONObject(), "Yêu cầu đồng bộ lại toàn bộ dữ liệu")
                        syncNoticeMessage = "Đã gửi yêu cầu đồng bộ tức thì tới tất cả thiết bị!"
                    }) {
                        Icon(
                            imageVector = Icons.Default.Sync,
                            contentDescription = "Đồng bộ",
                            tint = PrimaryRed
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Notice Banner if synced
            syncNoticeMessage?.let { notice ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF2E7D32), modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(notice, fontSize = 12.sp, color = Color(0xFF2E7D32), modifier = Modifier.weight(1f))
                        IconButton(onClick = { syncNoticeMessage = null }, modifier = Modifier.size(20.dp)) {
                            Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(14.dp))
                        }
                    }
                }
            }

            // --- 1. USER PROFILE HEADER CARD ---
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(3.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Avatar with Gradient Border & Role Emblem
                    Box(
                        modifier = Modifier
                            .size(92.dp)
                            .border(
                                width = 3.dp,
                                brush = Brush.sweepGradient(listOf(PrimaryRed, SecondaryGold, Color(0xFF00E5FF), PrimaryRed)),
                                shape = CircleShape
                            )
                            .padding(4.dp)
                            .clip(CircleShape)
                            .background(PrimaryRed),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            modifier = Modifier.size(54.dp),
                            tint = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = activeUser.fullName,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E1E1E)
                    )

                    Text(
                        text = "Tài khoản: @${activeUser.username}${if (activeUser.phone.isNotBlank()) " • ${activeUser.phone}" else ""}",
                        fontSize = 13.sp,
                        color = Color.Gray
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Chips Row
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AssistChip(
                            onClick = {},
                            leadingIcon = {
                                Icon(
                                    imageVector = if (activeUser.role == UserRole.ADMIN) Icons.Default.Security else Icons.Default.Badge,
                                    contentDescription = null,
                                    tint = if (activeUser.role == UserRole.ADMIN) PrimaryRed else SecondaryDarkGold,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            label = {
                                Text(
                                    if (activeUser.role == UserRole.ADMIN) "Quản Lý (ADMIN)" else "Nhân Viên (STAFF)",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.5.sp
                                )
                            }
                        )

                        // Attendance chip
                        if (selfEmployee != null) {
                            AssistChip(
                                onClick = {},
                                leadingIcon = {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(if (isCheckedIn) Color(0xFF2E7D32) else Color(0xFFE53935))
                                    )
                                },
                                label = {
                                    Text(
                                        if (isCheckedIn) "Đang Trong Ca" else "Chưa Vào Ca",
                                        color = if (isCheckedIn) Color(0xFF2E7D32) else Color(0xFFE53935),
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 11.5.sp
                                    )
                                }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Quick Action Dual Buttons: [Chỉnh sửa hồ sơ] and [Chấm công nhanh]
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showEditProfileDialog = true },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(vertical = 8.dp)
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Sửa Hồ Sơ", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }

                        if (selfEmployee != null) {
                            Button(
                                onClick = {
                                    val newType = if (isCheckedIn) AttendanceType.CHECK_OUT else AttendanceType.CHECK_IN
                                    repository.recordAttendance(
                                        employeeId = selfEmployee!!.id,
                                        type = newType,
                                        method = AttendanceMethod.FACE_ID,
                                        note = "Chấm công nhanh từ trang cá nhân"
                                    )
                                    selfEmployee = repository.getAllEmployees().find { it.id == selfEmployee!!.id }
                                    Toast.makeText(
                                        context,
                                        if (newType == AttendanceType.CHECK_IN) "✓ Đã Vào Ca Thành Công!" else "✓ Đã Tan Ca Thành Công!",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isCheckedIn) Color(0xFFE53935) else Color(0xFF2E7D32)
                                ),
                                contentPadding = PaddingValues(vertical = 8.dp)
                            ) {
                                Icon(
                                    imageVector = if (isCheckedIn) Icons.Default.TimerOff else Icons.Default.Timer,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isCheckedIn) "Tan Ca Ngay" else "Vào Ca Ngay",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // --- 2. WORK STATS (If Linked Employee) ---
            if (selfEmployee != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(bottom = 10.dp)
                        ) {
                            Icon(Icons.Default.TrendingUp, contentDescription = null, tint = PrimaryRed, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Thống Kê Công Việc & Thu Nhập", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
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
                }
                Spacer(modifier = Modifier.height(14.dp))
            }

            // --- 3. NHÓM 1: THẺ ĐỊNH DANH & CHẤM CÔNG ---
            ProfileSectionHeader(title = "ĐỊNH DANH & CHẤM CÔNG", icon = Icons.Default.Badge)
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(2.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)) {
                    if (selfEmployee != null) {
                        ProfileMenuRow(
                            icon = Icons.Default.QrCode,
                            iconColor = Color(0xFF1976D2),
                            title = "Thẻ Nhân Viên & Mã QR / NFC",
                            subtitle = "Mở thẻ định danh cá nhân & quét mã QR",
                            onClick = { showSelfBadgeDialog = true }
                        )

                        HorizontalDivider(color = Color(0xFFF0F0F0))

                        ProfileMenuRow(
                            icon = Icons.AutoMirrored.Filled.ReceiptLong,
                            iconColor = PrimaryRed,
                            title = "Phiếu Lương & Thu Nhập",
                            subtitle = "Xem bảng kê chi tiết lương, thưởng, phạt",
                            onClick = { showSelfPayslipDialog = true }
                        )

                        HorizontalDivider(color = Color(0xFFF0F0F0))

                        ProfileMenuRow(
                            icon = Icons.Default.History,
                            iconColor = Color(0xFF00897B),
                            title = "Lịch Sử Chấm Công Cá Nhân",
                            subtitle = "Xem các lần vào ca, tan ca và số giờ làm",
                            onClick = { showAttendanceHistoryDialog = true }
                        )
                    } else {
                        ProfileMenuRow(
                            icon = Icons.Default.PersonAdd,
                            iconColor = PrimaryRed,
                            title = "Tạo Hồ Sơ Nhân Viên Liên Kết",
                            subtitle = "Liên kết tài khoản này với hồ sơ nhân sự nhà hàng",
                            onClick = {
                                val newEmp = Employee(
                                    name = activeUser.fullName,
                                    phone = activeUser.phone.ifBlank { "0900000000" },
                                    position = if (activeUser.role == UserRole.ADMIN) "Quản lý" else "Nhân viên",
                                    tier = EmployeeTier.CHINH_THUC,
                                    contractType = ContractType.MONTHLY,
                                    monthlySalary = 10000000.0
                                )
                                repository.addEmployee(newEmp)
                                selfEmployee = repository.getAllEmployees().find { it.name == activeUser.fullName }
                                Toast.makeText(context, "Đã liên kết hồ sơ nhân viên thành công!", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // --- 4. NHÓM 2: BẢO MẬT & SINH TRẮC HỌC ---
            ProfileSectionHeader(title = "BẢO MẬT & SINH TRẮC HỌC", icon = Icons.Default.Security)
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(2.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)) {
                    // Face ID
                    ProfileMenuRow(
                        icon = Icons.Default.Face,
                        iconColor = if (hasFaceId) Color(0xFF2E7D32) else PrimaryRed,
                        title = if (hasFaceId) "Cập Nhật Face ID Khuôn Mặt" else "Cài Đặt Face ID Khuôn Mặt",
                        subtitle = if (hasFaceId) "Đã liên kết Face ID cá nhân • Bấm để quét lại" else "Chưa cài đặt • Bấm để liên kết khuôn mặt",
                        trailingBadge = if (hasFaceId) "ĐÃ BẬT ✓" else "CHƯA CÓ",
                        badgeColor = if (hasFaceId) Color(0xFF2E7D32) else Color.Gray,
                        onClick = { showFaceIdDialog = true }
                    )

                    if (hasFaceId) {
                        HorizontalDivider(color = Color(0xFFF0F0F0))
                        ProfileMenuRow(
                            icon = Icons.Default.PersonRemove,
                            iconColor = Color(0xFFE53935),
                            title = "Hủy Liên Kết Face ID",
                            subtitle = "Xóa dữ liệu khuôn mặt khỏi tài khoản này",
                            onClick = { showDeleteFaceIdConfirmDialog = true }
                        )
                    }

                    HorizontalDivider(color = Color(0xFFF0F0F0))

                    // Change Password
                    ProfileMenuRow(
                        icon = Icons.Default.Lock,
                        iconColor = Color(0xFFF57C00),
                        title = "Đổi Mật Khẩu Đăng Nhập",
                        subtitle = "Bảo mật tài khoản với mật khẩu mới",
                        onClick = { showChangePasswordDialog = true }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // --- 5. NHÓM 3: HỆ THỐNG & ĐỒNG BỘ ĐA MÁY ---
            ProfileSectionHeader(title = "HỆ THỐNG & ĐỒNG BỘ ĐA MÁY", icon = Icons.Default.CloudSync)
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(2.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)) {
                    // Manual Multi-Device Sync
                    ProfileMenuRow(
                        icon = Icons.Default.Sync,
                        iconColor = Color(0xFF00B0FF),
                        title = "Đồng Bộ Toàn Bộ Dữ Liệu Ngay",
                        subtitle = "Tải toàn bộ Bàn, Món, Khách, Đơn lên Web API và các máy khác",
                        trailingBadge = if (isCloudOnline) "ONLINE" else "LOCAL",
                        badgeColor = if (isCloudOnline) Color(0xFF2E7D32) else Color(0xFFF57C00),
                        onClick = {
                            RealtimeSyncManager.sendPing()
                            RealtimeSyncManager.broadcast("REQUEST_FULL_SYNC", JSONObject(), "Yêu cầu đồng bộ lại toàn bộ dữ liệu")
                            WebApiSyncEngine.syncAll(context, repository) { success, msg ->
                                syncNoticeMessage = if (success) "✓ $msg" else "⚠ $msg"
                            }
                            syncNoticeMessage = "Đang đồng bộ dữ liệu với Web API và Cloud Real-time..."
                        }
                    )

                    HorizontalDivider(color = Color(0xFFF0F0F0))
                    ProfileMenuRow(
                        icon = Icons.Default.Cloud,
                        iconColor = Color(0xFF1976D2),
                        title = "Cấu Hình Server Web API (Local & Cloud Deploy)",
                        subtitle = "Nhập link Render / Cloud để App đồng bộ trực tiếp lên Web API",
                        trailingBadge = if (RestaurantApiClient.baseUrl.startsWith("https://")) "CLOUD" else "LOCAL",
                        badgeColor = if (RestaurantApiClient.baseUrl.startsWith("https://")) Color(0xFF1976D2) else Color(0xFF2E7D32),
                        onClick = { showServerConfigDialog = true }
                    )

                    // System Settings (Admin only)
                    if (activeUser.role == UserRole.ADMIN) {
                        HorizontalDivider(color = Color(0xFFF0F0F0))
                        ProfileMenuRow(
                            icon = Icons.Default.ManageAccounts,
                            iconColor = Color(0xFFE65100),
                            title = "Quản Lý Toàn Bộ Tài Khoản Hệ Thống",
                            subtitle = "Phân quyền Quản trị / Nhân viên, Face ID & đồng bộ toàn máy",
                            onClick = { showAccountManagementDialog = true }
                        )
                        HorizontalDivider(color = Color(0xFFF0F0F0))
                        ProfileMenuRow(
                            icon = Icons.Default.Settings,
                            iconColor = Color(0xFF6A1B9A),
                            title = "Cài Đặt Nhà Hàng & VAT/Wifi",
                            subtitle = "Thông tin nhà hàng, thuế VAT, mật khẩu Wifi",
                            onClick = { showSystemSettingsDialog = true }
                        )
                    }

                    HorizontalDivider(color = Color(0xFFF0F0F0))

                    // Device info row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.PhoneAndroid, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Mã Thiết Bị Hiện Tại", fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold)
                            Text("${RealtimeSyncManager.deviceName} (${RealtimeSyncManager.deviceId})", fontSize = 11.sp, color = Color.Gray)
                        }
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(if (isCloudOnline) Color(0xFF00E676) else Color(0xFFFFB300))
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // --- 6. NHÓM 4: ĐĂNG XUẤT ---
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(2.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                ProfileMenuRow(
                    icon = Icons.AutoMirrored.Filled.Logout,
                    iconColor = PrimaryRed,
                    title = "Đăng Xuất Khỏi Thiết Bị",
                    subtitle = "Thoát phiên làm việc của tài khoản @${activeUser.username}",
                    onClick = { showLogoutConfirmDialog = true }
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Golden Spoon Restaurant POS v4.0.2\nĐồng bộ Cloud Realtime & Face ID AI",
                fontSize = 11.sp,
                color = Color.Gray,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                lineHeight = 16.sp
            )

            Spacer(modifier = Modifier.height(10.dp))
        }
    }

    // --- DIALOGS ---

    // 1. Face ID Enrollment Dialog
    if (showFaceIdDialog) {
        FaceIdEnrollmentDialog(
            username = activeUser.username,
            fullName = activeUser.fullName,
            repository = repository,
            onEnrolled = {
                showFaceIdDialog = false
                val refreshed = repository.getUserById(activeUser.id) ?: repository.getUserByUsername(activeUser.username)
                if (refreshed != null) activeUser = refreshed
                Toast.makeText(context, "✓ Đã cài đặt Face ID thành công!", Toast.LENGTH_SHORT).show()
            },
            onDismiss = { showFaceIdDialog = false }
        )
    }

    // 2. Edit Profile Dialog
    if (showEditProfileDialog) {
        var editName by remember { mutableStateOf(activeUser.fullName) }
        var editPhone by remember { mutableStateOf(activeUser.phone) }

        AlertDialog(
            onDismissRequest = { showEditProfileDialog = false },
            title = { Text("Chỉnh Sửa Hồ Sơ Cá Nhân", fontWeight = FontWeight.Bold, fontSize = 17.sp) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = editName,
                        onValueChange = { editName = it },
                        label = { Text("Họ và tên") },
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editPhone,
                        onValueChange = { editPhone = it },
                        label = { Text("Số điện thoại") },
                        leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (editName.isNotBlank()) {
                            repository.updateUserProfile(activeUser.id, editName, editPhone)
                            val refreshed = repository.getUserById(activeUser.id) ?: repository.getUserByUsername(activeUser.username)
                            if (refreshed != null) activeUser = refreshed
                            showEditProfileDialog = false
                            Toast.makeText(context, "Đã cập nhật hồ sơ thành công!", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryRed)
                ) {
                    Text("Lưu Thay Đổi")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditProfileDialog = false }) { Text("Hủy") }
            }
        )
    }

    // 3. Smart Badge & NFC/QR Dialog
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

    // 4. Self Payslip Dialog
    if (showSelfPayslipDialog && selfEmployee != null) {
        PayslipDialog(
            employee = selfEmployee!!,
            onDismiss = { showSelfPayslipDialog = false }
        )
    }

    // 5. Attendance History Dialog
    if (showAttendanceHistoryDialog && selfEmployee != null) {
        val records = remember(selfEmployee!!.id) {
            repository.getAllAttendanceRecords().filter { it.employeeId == selfEmployee!!.id }.take(15)
        }
        val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

        AlertDialog(
            onDismissRequest = { showAttendanceHistoryDialog = false },
            title = { Text("Lịch Sử Chấm Công Cá Nhân", fontWeight = FontWeight.Bold, fontSize = 17.sp) },
            text = {
                if (records.isEmpty()) {
                    Text("Chưa có lịch sử chấm công nào.", color = Color.Gray, fontSize = 13.sp)
                } else {
                    LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 300.dp)) {
                        items(records) { rec ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(if (rec.type == AttendanceType.CHECK_IN) Color(0xFF2E7D32) else Color(0xFFE53935))
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = if (rec.type == AttendanceType.CHECK_IN) "Vào ca (${rec.method})" else "Tan ca (${rec.method})",
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 12.5.sp
                                        )
                                        Text(dateFormat.format(Date(rec.timestamp)), fontSize = 10.5.sp, color = Color.Gray)
                                    }
                                }
                                if (rec.durationHours > 0) {
                                    Text("${rec.durationHours}h", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = PrimaryRed)
                                }
                            }
                            HorizontalDivider(color = Color(0xFFF5F5F5))
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showAttendanceHistoryDialog = false }) { Text("Đóng") }
            }
        )
    }

    // 6. Change Password Dialog
    if (showChangePasswordDialog) {
        var newPassword by remember { mutableStateOf("") }
        var confirmPassword by remember { mutableStateOf("") }
        var pwdError by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { showChangePasswordDialog = false },
            title = { Text("Đổi Mật Khẩu", fontWeight = FontWeight.Bold, fontSize = 17.sp) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = newPassword,
                        onValueChange = { newPassword = it; pwdError = null },
                        label = { Text("Mật khẩu mới") },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = confirmPassword,
                        onValueChange = { confirmPassword = it; pwdError = null },
                        label = { Text("Xác nhận mật khẩu") },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth()
                    )
                    pwdError?.let {
                        Text(it, color = PrimaryRed, fontSize = 12.sp)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newPassword.isBlank()) {
                            pwdError = "Mật khẩu không được để trống!"
                        } else if (newPassword != confirmPassword) {
                            pwdError = "Xác nhận mật khẩu không khớp!"
                        } else {
                            val success = repository.updateUserPassword(activeUser.id, newPassword)
                            if (success) {
                                showChangePasswordDialog = false
                                Toast.makeText(context, "Đã đổi mật khẩu thành công!", Toast.LENGTH_SHORT).show()
                            } else {
                                pwdError = "Lỗi khi cập nhật mật khẩu!"
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryRed)
                ) {
                    Text("Cập Nhật")
                }
            },
            dismissButton = {
                TextButton(onClick = { showChangePasswordDialog = false }) { Text("Hủy") }
            }
        )
    }

    // 7. System Settings Dialog (Admin)
    if (showSystemSettingsDialog) {
        val currentSettings = remember { repository.getSystemSettings().toMutableMap() }
        var restName by remember { mutableStateOf(currentSettings["restaurant_name"] ?: "Golden Spoon") }
        var restAddress by remember { mutableStateOf(currentSettings["address"] ?: "") }
        var restPhone by remember { mutableStateOf(currentSettings["phone"] ?: "") }
        var restVat by remember { mutableStateOf(currentSettings["vat_percent"] ?: "8.0") }
        var restWifiName by remember { mutableStateOf(currentSettings["wifi_name"] ?: "") }
        var restWifiPass by remember { mutableStateOf(currentSettings["wifi_pass"] ?: "") }

        AlertDialog(
            onDismissRequest = { showSystemSettingsDialog = false },
            title = { Text("Cài Đặt Hệ Thống Nhà Hàng", fontWeight = FontWeight.Bold, fontSize = 17.sp) },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(value = restName, onValueChange = { restName = it }, label = { Text("Tên nhà hàng") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = restAddress, onValueChange = { restAddress = it }, label = { Text("Địa chỉ") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = restPhone, onValueChange = { restPhone = it }, label = { Text("Hotline") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = restVat, onValueChange = { restVat = it }, label = { Text("Thuế VAT (%)") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = restWifiName, onValueChange = { restWifiName = it }, label = { Text("Tên Wi-Fi") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = restWifiPass, onValueChange = { restWifiPass = it }, label = { Text("Mật khẩu Wi-Fi") }, modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val map = mapOf(
                            "restaurant_name" to restName,
                            "address" to restAddress,
                            "phone" to restPhone,
                            "vat_percent" to restVat,
                            "wifi_name" to restWifiName,
                            "wifi_pass" to restWifiPass
                        )
                        repository.updateSystemSettings(map)
                        showSystemSettingsDialog = false
                        Toast.makeText(context, "Đã lưu cài đặt hệ thống!", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryRed)
                ) {
                    Text("Lưu Cài Đặt")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSystemSettingsDialog = false }) { Text("Hủy") }
            }
        )
    }

    // Server Config Dialog (Local & Cloud Deploy)
    if (showServerConfigDialog) {
        var inputCloudUrl by remember { mutableStateOf<String>(RestaurantApiClient.cloudUrl) }
        var inputLocalUrl by remember { mutableStateOf<String>(RestaurantApiClient.localUrl) }
        var testResultMsg by remember { mutableStateOf<String?>(null) }
        var isTesting by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showServerConfigDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Cloud, contentDescription = null, tint = Color(0xFF1976D2), modifier = Modifier.size(26.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Cấu Hình Server Web API", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                }
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
                    Text("Máy chủ hiện tại:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color.Gray)
                    Text(
                        RestaurantApiClient.activeServerLabel,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1976D2)
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Text("🌐 URL Cloud Deploy (Render.com / Railway):", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    OutlinedTextField(
                        value = inputCloudUrl,
                        onValueChange = { newVal: String ->
                            inputCloudUrl = newVal
                            testResultMsg = null
                        },
                        placeholder = { Text("https://da-restaurant-api.onrender.com") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text("Dán URL Web Service Render của bạn vào đây.", fontSize = 11.sp, color = Color.Gray)

                    Spacer(modifier = Modifier.height(12.dp))

                    Text("🏠 URL Local Web API (Wi-Fi máy tính):", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    OutlinedTextField(
                        value = inputLocalUrl,
                        onValueChange = { newVal: String ->
                            inputLocalUrl = newVal
                            testResultMsg = null
                        },
                        placeholder = { Text("http://192.168.0.172:5276") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    testResultMsg?.let { msg ->
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(msg, fontSize = 12.sp, color = if (msg.startsWith("✓")) Color(0xFF2E7D32) else Color.Red)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        isTesting = true
                        RestaurantApiClient.saveConfig(context, inputCloudUrl, inputLocalUrl)
                        WebApiSyncEngine.syncAll(context, repository) { success, msg ->
                            isTesting = false
                            testResultMsg = if (success) "✓ Đã kết nối và đồng bộ toàn bộ dữ liệu lên Web API!" else "⚠ $msg"
                            if (success) {
                                Toast.makeText(context, "Đã lưu và đồng bộ thành công!", Toast.LENGTH_SHORT).show()
                                showServerConfigDialog = false
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1976D2))
                ) {
                    Text(if (isTesting) "Đang kết nối..." else "Lưu & Đồng Bộ Ngay")
                }
            },
            dismissButton = {
                TextButton(onClick = { showServerConfigDialog = false }) {
                    Text("Đóng")
                }
            }
        )
    }

    // 7b. System Accounts Management Dialog (Admin only)
    if (showAccountManagementDialog) {
        val syncVersion by repository.syncVersion.collectAsState()
        var allUsers by remember(syncVersion) { mutableStateOf(repository.getAllUsers()) }
        var showCreateUserDialog by remember { mutableStateOf(false) }
        var userToDelete by remember { mutableStateOf<User?>(null) }

        AlertDialog(
            onDismissRequest = { showAccountManagementDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.ManageAccounts, contentDescription = null, tint = PrimaryRed, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text("Quản Lý Tài Khoản Hệ Thống", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                        Text(
                            "${allUsers.size} tài khoản (${allUsers.count { it.role == UserRole.ADMIN }} Quản trị, ${allUsers.count { it.role == UserRole.USER }} Nhân viên)",
                            fontSize = 11.5.sp,
                            color = Color.Gray
                        )
                    }
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 450.dp)
                ) {
                    // Quick Action Row: Add User & Full Cloud Sync
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { showCreateUserDialog = true },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryRed),
                            contentPadding = PaddingValues(vertical = 8.dp)
                        ) {
                            Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Thêm Tài Khoản", fontSize = 11.5.sp)
                        }

                        OutlinedButton(
                            onClick = {
                                repository.syncRetainedCloudState()
                                RealtimeSyncManager.broadcast("REQUEST_FULL_SYNC", JSONObject(), "Đồng bộ toàn bộ tài khoản")
                                Toast.makeText(context, "⚡ Đã đẩy đồng bộ toàn bộ tài khoản lên toàn hệ thống!", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.weight(1f),
                            border = BorderStroke(1.dp, Color(0xFF00B0FF)),
                            contentPadding = PaddingValues(vertical = 8.dp)
                        ) {
                            Icon(Icons.Default.Sync, contentDescription = null, tint = Color(0xFF00B0FF), modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Đồng Bộ Toàn Máy", fontSize = 11.5.sp, color = Color(0xFF00B0FF))
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    HorizontalDivider(color = Color(0xFFEEEEEE))
                    Spacer(modifier = Modifier.height(6.dp))

                    LazyColumn(
                        modifier = Modifier.weight(1f, fill = false),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(allUsers, key = { it.id }) { user ->
                            val isAdmin = user.role == UserRole.ADMIN
                            val hasFace = !user.faceIdData.isNullOrBlank()

                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFF9F9F9)),
                                border = BorderStroke(1.dp, if (isAdmin) PrimaryRed.copy(alpha = 0.3f) else Color(0xFFE0E0E0))
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        // Emoji Avatar
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .clip(CircleShape)
                                                .background(if (isAdmin) PrimaryRed.copy(alpha = 0.12f) else SecondaryGold.copy(alpha = 0.15f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(if (isAdmin) "👑" else "👨‍🍳", fontSize = 18.sp)
                                        }

                                        Spacer(modifier = Modifier.width(10.dp))

                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(user.fullName.ifBlank { user.username }, fontWeight = FontWeight.Bold, fontSize = 13.5.sp)
                                            Text("@${user.username} • ${user.phone.ifBlank { "Chưa có SĐT" }}", fontSize = 11.5.sp, color = Color.Gray)
                                        }

                                        // Role Chip
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(if (isAdmin) PrimaryRed else SecondaryDarkGold)
                                                .padding(horizontal = 7.dp, vertical = 3.dp)
                                        ) {
                                            Text(
                                                if (isAdmin) "QUẢN TRỊ" else "NHÂN VIÊN",
                                                color = Color.White,
                                                fontSize = 9.5.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        // Face ID status
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(
                                                modifier = Modifier
                                                    .size(6.dp)
                                                    .clip(CircleShape)
                                                    .background(if (hasFace) Color(0xFF2E7D32) else Color.Gray)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                if (hasFace) "Face ID: Đã kích hoạt ✓" else "Face ID: Chưa đăng ký",
                                                fontSize = 11.sp,
                                                color = if (hasFace) Color(0xFF2E7D32) else Color.Gray,
                                                fontWeight = if (hasFace) FontWeight.SemiBold else FontWeight.Normal
                                            )
                                        }

                                        // Action buttons
                                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                            // Toggle Role Button
                                            TextButton(
                                                onClick = {
                                                    val newRole = if (isAdmin) UserRole.USER else UserRole.ADMIN
                                                    repository.updateUserRole(user.id, newRole)
                                                    allUsers = repository.getAllUsers()
                                                    Toast.makeText(context, "Đã chuyển quyền @${user.username} thành ${newRole.name}!", Toast.LENGTH_SHORT).show()
                                                },
                                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Text(if (isAdmin) "Hạ thành NV" else "Lên Quản Trị", fontSize = 10.5.sp, color = PrimaryRed, fontWeight = FontWeight.Bold)
                                            }

                                            // Delete User (if not self)
                                            if (user.username != activeUser.username) {
                                                TextButton(
                                                    onClick = { userToDelete = user },
                                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                                                ) {
                                                    Text("Xóa", fontSize = 10.5.sp, color = Color.Red)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showAccountManagementDialog = false }) {
                    Text("Đóng")
                }
            }
        )

        // Sub-dialog: Create User
        if (showCreateUserDialog) {
            var newUname by remember { mutableStateOf("") }
            var newPass by remember { mutableStateOf("") }
            var newFname by remember { mutableStateOf("") }
            var newPhone by remember { mutableStateOf("") }
            var newRole by remember { mutableStateOf(UserRole.USER) }

            AlertDialog(
                onDismissRequest = { showCreateUserDialog = false },
                title = { Text("Tạo Tài Khoản Mới", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = newFname,
                            onValueChange = { newFname = it },
                            label = { Text("Họ và tên") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = newUname,
                            onValueChange = { newUname = it },
                            label = { Text("Tên đăng nhập") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = newPass,
                            onValueChange = { newPass = it },
                            label = { Text("Mật khẩu") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = newPhone,
                            onValueChange = { newPhone = it },
                            label = { Text("Số điện thoại") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Vai trò: ", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            FilterChip(
                                selected = newRole == UserRole.USER,
                                onClick = { newRole = UserRole.USER },
                                label = { Text("Nhân Viên") }
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            FilterChip(
                                selected = newRole == UserRole.ADMIN,
                                onClick = { newRole = UserRole.ADMIN },
                                label = { Text("Quản Trị") }
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (newUname.isNotBlank() && newPass.isNotBlank()) {
                                val success = repository.register(
                                    User(
                                        username = newUname.trim(),
                                        password = newPass.trim(),
                                        fullName = newFname.trim(),
                                        phone = newPhone.trim(),
                                        role = newRole
                                    )
                                )
                                if (success) {
                                    allUsers = repository.getAllUsers()
                                    showCreateUserDialog = false
                                    Toast.makeText(context, "Đã tạo tài khoản @$newUname và đồng bộ toàn máy!", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "Tên đăng nhập đã tồn tại!", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryRed)
                    ) {
                        Text("Tạo & Đồng Bộ")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showCreateUserDialog = false }) { Text("Hủy") }
                }
            )
        }

        // Sub-dialog: Confirm Delete User
        if (userToDelete != null) {
            AlertDialog(
                onDismissRequest = { userToDelete = null },
                title = { Text("Xác Nhận Xóa Tài Khoản") },
                text = { Text("Bạn có chắc muốn xóa tài khoản '${userToDelete!!.fullName}' (@${userToDelete!!.username}) khỏi hệ thống toàn bộ các máy?") },
                confirmButton = {
                    Button(
                        onClick = {
                            repository.deleteUser(userToDelete!!.id)
                            allUsers = repository.getAllUsers()
                            userToDelete = null
                            Toast.makeText(context, "Đã xóa tài khoản và đồng bộ tới toàn bộ thiết bị!", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryRed)
                    ) {
                        Text("Xóa Ngay")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { userToDelete = null }) { Text("Hủy") }
                }
            )
        }
    }

    // 8. Delete Face ID Confirm Dialog
    if (showDeleteFaceIdConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteFaceIdConfirmDialog = false },
            title = { Text("Hủy Liên Kết Face ID?") },
            text = { Text("Bạn có chắc chắn muốn xóa dữ liệu Face ID của tài khoản @${activeUser.username}? Sau khi xóa, bạn sẽ không thể đăng nhập bằng khuôn mặt.") },
            confirmButton = {
                Button(
                    onClick = {
                        repository.deleteUserFaceId(activeUser.username)
                        val refreshed = repository.getUserById(activeUser.id) ?: repository.getUserByUsername(activeUser.username)
                        if (refreshed != null) activeUser = refreshed
                        showDeleteFaceIdConfirmDialog = false
                        Toast.makeText(context, "Đã hủy liên kết Face ID!", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryRed)
                ) {
                    Text("Xác Nhận Hủy")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteFaceIdConfirmDialog = false }) { Text("Hủy") }
            }
        )
    }

    // 9. Logout Confirm Dialog
    if (showLogoutConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutConfirmDialog = false },
            title = { Text("Xác Nhận Đăng Xuất") },
            text = { Text("Bạn có muốn đăng xuất khỏi tài khoản @${activeUser.username} trên thiết bị này không?") },
            confirmButton = {
                Button(
                    onClick = {
                        showLogoutConfirmDialog = false
                        onLogout()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryRed)
                ) {
                    Text("Đăng Xuất")
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutConfirmDialog = false }) { Text("Ở Lại") }
            }
        )
    }
}

@Composable
fun ProfileSectionHeader(title: String, icon: ImageVector) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 4.dp, bottom = 6.dp)
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = PrimaryRed, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = title,
            fontSize = 11.5.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Gray,
            letterSpacing = 0.8.sp
        )
    }
}

@Composable
fun ProfileMenuRow(
    icon: ImageVector,
    iconColor: Color,
    title: String,
    subtitle: String,
    trailingBadge: String? = null,
    badgeColor: Color = Color.Gray,
    onClick: () -> Unit
) {
    TextButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(iconColor.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, color = Color(0xFF1E1E1E), fontWeight = FontWeight.SemiBold, fontSize = 13.5.sp)
                Text(subtitle, fontSize = 11.sp, color = Color.Gray)
            }
            if (trailingBadge != null) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = badgeColor.copy(alpha = 0.12f),
                    modifier = Modifier.padding(end = 4.dp)
                ) {
                    Text(
                        text = trailingBadge,
                        color = badgeColor,
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
            Icon(Icons.AutoMirrored.Filled.ArrowForwardIos, contentDescription = null, tint = Color.LightGray, modifier = Modifier.size(12.dp))
        }
    }
}
