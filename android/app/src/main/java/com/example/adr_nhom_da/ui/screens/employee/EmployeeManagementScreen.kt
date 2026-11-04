package com.example.adr_nhom_da.ui.screens.employee

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Login
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.adr_nhom_da.data.model.AttendanceMethod
import com.example.adr_nhom_da.data.model.AttendanceRecord
import com.example.adr_nhom_da.data.model.AttendanceType
import com.example.adr_nhom_da.data.model.ContractType
import com.example.adr_nhom_da.data.model.Employee
import com.example.adr_nhom_da.data.model.EmployeeTier
import com.example.adr_nhom_da.data.repository.RestaurantRepository
import com.example.adr_nhom_da.ui.components.ConfirmDeleteDialog
import com.example.adr_nhom_da.ui.components.EmployeeTierBadge
import com.example.adr_nhom_da.ui.components.FaceIdVerificationDialog
import com.example.adr_nhom_da.ui.components.formatVnd
import com.example.adr_nhom_da.ui.theme.PrimaryRed
import com.example.adr_nhom_da.ui.theme.SecondaryDarkGold
import com.example.adr_nhom_da.ui.theme.SecondaryGold
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class EmployeeScreenTab(val title: String) {
    STAFF_LIST("Nhân Sự"),
    ATTENDANCE_STATION("Trạm Chấm Công"),
    ATTENDANCE_LOGS("Nhật Ký Điểm Danh")
}

fun getRoleEmoji(position: String, tier: EmployeeTier): String {
    return when {
        tier == EmployeeTier.GIAM_DOC || position.contains("Giám đốc", ignoreCase = true) -> "👑"
        tier == EmployeeTier.QUAN_LY || position.contains("Quản lý", ignoreCase = true) -> "💼"
        position.contains("Bếp", ignoreCase = true) -> "👨‍🍳"
        position.contains("Thu ngân", ignoreCase = true) -> "💳"
        position.contains("Phục vụ", ignoreCase = true) -> "🤵"
        position.contains("Pha chế", ignoreCase = true) -> "🍹"
        else -> "👤"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmployeeManagementScreen(
    repository: RestaurantRepository
) {
    var employees by remember { mutableStateOf(repository.getAllEmployees()) }
    var attendanceRecords by remember { mutableStateOf(repository.getAllAttendanceRecords()) }
    var selectedTab by remember { mutableStateOf(EmployeeScreenTab.STAFF_LIST) }

    // Dialog states
    var showAddEditDialog by remember { mutableStateOf(false) }
    var employeeToEdit by remember { mutableStateOf<Employee?>(null) }
    var employeeToDelete by remember { mutableStateOf<Employee?>(null) }
    var selectedEmployeeForPayslip by remember { mutableStateOf<Employee?>(null) }
    var selectedEmployeeForBadge by remember { mutableStateOf<Employee?>(null) }

    // FaceID Verification State
    var isFaceIdEnabled by remember { mutableStateOf(true) }
    var pendingFaceAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    var faceActionTitle by remember { mutableStateOf("") }
    var faceActionDesc by remember { mutableStateOf("") }

    // Search & Filter
    var searchQuery by remember { mutableStateOf("") }
    var shiftFilter by remember { mutableStateOf("ALL") } // ALL, IN_SHIFT, OFF_SHIFT, MANAGERS

    fun refreshData() {
        employees = repository.getAllEmployees()
        attendanceRecords = repository.getAllAttendanceRecords()
    }

    val syncVersion by repository.syncVersion.collectAsState()
    LaunchedEffect(syncVersion) {
        refreshData()
    }

    fun executeWithFaceId(title: String, desc: String, action: () -> Unit) {
        if (isFaceIdEnabled) {
            faceActionTitle = title
            faceActionDesc = desc
            pendingFaceAction = action
        } else {
            action()
        }
    }

    val activeCount = remember(employees, attendanceRecords) {
        employees.count { repository.getLastAttendanceForEmployee(it.id)?.type == AttendanceType.CHECK_IN }
    }
    val totalSalaryBudget = employees.sumOf { it.calculateSalary() }

    val filteredEmployees = remember(employees, searchQuery, shiftFilter, attendanceRecords) {
        employees.filter { emp ->
            val isCheckedIn = repository.getLastAttendanceForEmployee(emp.id)?.type == AttendanceType.CHECK_IN
            val matchesSearch = searchQuery.isBlank() ||
                    emp.name.contains(searchQuery, ignoreCase = true) ||
                    emp.phone.contains(searchQuery, ignoreCase = true) ||
                    emp.position.contains(searchQuery, ignoreCase = true)

            val matchesFilter = when (shiftFilter) {
                "IN_SHIFT" -> isCheckedIn
                "OFF_SHIFT" -> !isCheckedIn
                "MANAGERS" -> emp.tier == EmployeeTier.QUAN_LY || emp.tier == EmployeeTier.GIAM_DOC
                else -> true
            }

            matchesSearch && matchesFilter
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Quản Lý Nhân Sự & Chấm Công", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(if (isFaceIdEnabled) Color(0xFF2E7D32) else Color.Gray)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = if (isFaceIdEnabled) "Face ID: Bảo vệ quyền Quản trị 4.0" else "Face ID: Đã tắt",
                                fontSize = 11.sp,
                                color = if (isFaceIdEnabled) Color(0xFF2E7D32) else Color.Gray
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { isFaceIdEnabled = !isFaceIdEnabled }) {
                        Icon(
                            imageVector = Icons.Default.Face,
                            contentDescription = "Chuyển chế độ Face ID",
                            tint = if (isFaceIdEnabled) PrimaryRed else Color.Gray
                        )
                    }
                    IconButton(onClick = { refreshData() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Tải lại", tint = PrimaryRed)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        floatingActionButton = {
            if (selectedTab == EmployeeScreenTab.STAFF_LIST) {
                ExtendedFloatingActionButton(
                    onClick = {
                        executeWithFaceId(
                            title = "Xác Thực Thêm Nhân Viên",
                            desc = "Vui lòng xác minh Face ID Quản trị viên trước khi tạo hồ sơ nhân sự mới"
                        ) {
                            employeeToEdit = null
                            showAddEditDialog = true
                        }
                    },
                    containerColor = PrimaryRed,
                    contentColor = Color.White,
                    icon = { Icon(Icons.Default.PersonAdd, contentDescription = null) },
                    text = { Text("Thêm Nhân Viên", fontWeight = FontWeight.Bold) }
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Modern Segmented Navigation Tabs
            TabRow(
                selectedTabIndex = selectedTab.ordinal,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = PrimaryRed
            ) {
                EmployeeScreenTab.values().forEach { tab ->
                    Tab(
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = when (tab) {
                                        EmployeeScreenTab.STAFF_LIST -> Icons.Default.People
                                        EmployeeScreenTab.ATTENDANCE_STATION -> Icons.Default.Nfc
                                        EmployeeScreenTab.ATTENDANCE_LOGS -> Icons.Default.History
                                    },
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(tab.title, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            }
                        }
                    )
                }
            }

            when (selectedTab) {
                EmployeeScreenTab.STAFF_LIST -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // 4 Top KPI Metric Cards
                        item {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    StaffKpiCard(
                                        modifier = Modifier.weight(1f),
                                        title = "Tổng Nhân Sự",
                                        value = "${employees.size} người",
                                        subtitle = "4 Cấp bậc chức danh",
                                        accentColor = PrimaryRed,
                                        icon = Icons.Default.Badge
                                    )
                                    StaffKpiCard(
                                        modifier = Modifier.weight(1f),
                                        title = "Đang Trong Ca",
                                        value = "$activeCount người",
                                        subtitle = "${employees.size - activeCount} chưa vào ca",
                                        accentColor = Color(0xFF2E7D32),
                                        icon = Icons.Default.Timer
                                    )
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    StaffKpiCard(
                                        modifier = Modifier.weight(1f),
                                        title = "Quỹ Lương Kỳ Này",
                                        value = formatVnd(totalSalaryBudget),
                                        subtitle = "Đã tính thưởng & phạt",
                                        accentColor = SecondaryDarkGold,
                                        icon = Icons.Default.MonetizationOn
                                    )
                                    StaffKpiCard(
                                        modifier = Modifier.weight(1f),
                                        title = "Bảo Mật Quản Trị",
                                        value = if (isFaceIdEnabled) "Face ID Bật" else "Face ID Tắt",
                                        subtitle = "Chống sửa trái phép",
                                        accentColor = Color(0xFF1976D2),
                                        icon = Icons.Default.Security
                                    )
                                }
                            }
                        }

                        // Search Bar
                        item {
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                placeholder = { Text("Tìm theo tên, SĐT, vị trí...") },
                                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray) },
                                trailingIcon = {
                                    if (searchQuery.isNotBlank()) {
                                        IconButton(onClick = { searchQuery = "" }) {
                                            Icon(Icons.Default.Clear, contentDescription = "Xóa tìm kiếm")
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp)
                            )
                        }

                        // Filter Chips Row
                        item {
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                item {
                                    FilterChip(
                                        selected = shiftFilter == "ALL",
                                        onClick = { shiftFilter = "ALL" },
                                        label = { Text("Tất cả (${employees.size})") }
                                    )
                                }
                                item {
                                    FilterChip(
                                        selected = shiftFilter == "IN_SHIFT",
                                        onClick = { shiftFilter = "IN_SHIFT" },
                                        label = { Text("🟢 Trong ca ($activeCount)") }
                                    )
                                }
                                item {
                                    FilterChip(
                                        selected = shiftFilter == "OFF_SHIFT",
                                        onClick = { shiftFilter = "OFF_SHIFT" },
                                        label = { Text("⚪ Chưa vào ca (${employees.size - activeCount})") }
                                    )
                                }
                                item {
                                    FilterChip(
                                        selected = shiftFilter == "MANAGERS",
                                        onClick = { shiftFilter = "MANAGERS" },
                                        label = { Text("👑 Cấp Quản lý") }
                                    )
                                }
                            }
                        }

                        // Staff List Items
                        if (filteredEmployees.isEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 32.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        "Không tìm thấy nhân sự phù hợp điều kiện lọc",
                                        color = Color.Gray,
                                        fontSize = 13.sp
                                    )
                                }
                            }
                        } else {
                            items(filteredEmployees) { emp ->
                                val lastAtt = repository.getLastAttendanceForEmployee(emp.id)
                                val isCheckedIn = lastAtt?.type == AttendanceType.CHECK_IN

                                ModernEmployeeCard(
                                    employee = emp,
                                    isCheckedIn = isCheckedIn,
                                    lastAttendance = lastAtt,
                                    onEdit = {
                                        executeWithFaceId(
                                            title = "Xác Thực Sửa Nhân Viên",
                                            desc = "Xác minh Face ID quản trị viên để sửa '${emp.name}'"
                                        ) {
                                            employeeToEdit = emp
                                            showAddEditDialog = true
                                        }
                                    },
                                    onDelete = {
                                        executeWithFaceId(
                                            title = "Xác Thực XÓA Nhân Viên",
                                            desc = "Cảnh báo: Hành động xóa '${emp.name}' yêu cầu Face ID Quản trị"
                                        ) {
                                            employeeToDelete = emp
                                        }
                                    },
                                    onViewPayslip = {
                                        selectedEmployeeForPayslip = emp
                                    },
                                    onViewBadge = {
                                        selectedEmployeeForBadge = emp
                                    }
                                )
                            }
                        }

                        item {
                            Spacer(modifier = Modifier.height(64.dp))
                        }
                    }
                }

                EmployeeScreenTab.ATTENDANCE_STATION -> {
                    AttendanceStationView(
                        employees = employees,
                        repository = repository,
                        onAttendanceChanged = { refreshData() },
                        onViewBadge = { selectedEmployeeForBadge = it }
                    )
                }

                EmployeeScreenTab.ATTENDANCE_LOGS -> {
                    AttendanceLogsView(
                        records = attendanceRecords,
                        onRefresh = { refreshData() }
                    )
                }
            }
        }
    }

    // Modal Add / Edit Employee
    if (showAddEditDialog) {
        AddEditEmployeeDialog(
            employee = employeeToEdit,
            onDismiss = { showAddEditDialog = false },
            onSave = { updatedEmp ->
                if (employeeToEdit == null) {
                    repository.addEmployee(updatedEmp)
                } else {
                    repository.updateEmployee(updatedEmp)
                }
                refreshData()
                showAddEditDialog = false
            }
        )
    }

    // Confirm Delete Dialog
    employeeToDelete?.let { emp ->
        ConfirmDeleteDialog(
            title = "Xóa Hồ Sơ Nhân Sự?",
            message = "Bạn có chắc chắn muốn xóa nhân viên '${emp.name}' (${emp.position}) khỏi hệ thống nhà hàng không?",
            onConfirm = {
                repository.deleteEmployee(emp.id)
                refreshData()
                employeeToDelete = null
            },
            onDismiss = { employeeToDelete = null }
        )
    }

    // Smart Badge Dialog
    selectedEmployeeForBadge?.let { emp ->
        EmployeeBadgeDialog(
            employee = emp,
            repository = repository,
            onAttendanceUpdated = { refreshData() },
            onDismiss = { selectedEmployeeForBadge = null }
        )
    }

    // Payslip Dialog
    selectedEmployeeForPayslip?.let { emp ->
        PayslipDialog(
            employee = emp,
            onDismiss = { selectedEmployeeForPayslip = null }
        )
    }

    // Face ID Verification
    if (pendingFaceAction != null) {
        FaceIdVerificationDialog(
            actionTitle = faceActionTitle,
            actionDescription = faceActionDesc,
            onSuccess = {
                val action = pendingFaceAction
                pendingFaceAction = null
                action?.invoke()
            },
            onDismiss = { pendingFaceAction = null }
        )
    }
}

@Composable
fun StaffKpiCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    subtitle: String,
    accentColor: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(accentColor.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(18.dp))
                }

                Text(
                    title,
                    fontSize = 11.sp,
                    color = Color.Gray,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = value,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1E1E1E)
            )

            Text(
                text = subtitle,
                fontSize = 10.sp,
                color = Color.Gray,
                maxLines = 1
            )
        }
    }
}

@Composable
fun ModernEmployeeCard(
    employee: Employee,
    isCheckedIn: Boolean,
    lastAttendance: AttendanceRecord?,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onViewPayslip: () -> Unit,
    onViewBadge: () -> Unit
) {
    val roleEmoji = getRoleEmoji(employee.position, employee.tier)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isCheckedIn) Color(0xFF81C784).copy(alpha = 0.6f) else Color(0xFFEEEEEE)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            // Header Row: Avatar + Info + Edit/Delete
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Avatar with Role Emoji & Live Status Ring
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(
                            if (isCheckedIn) Color(0xFFE8F5E9) else Color(0xFFF5F5F5)
                        )
                        .border(
                            2.dp,
                            if (isCheckedIn) Color(0xFF2E7D32) else Color(0xFFB0BEC5),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(roleEmoji, fontSize = 22.sp)
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Name & Position
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            employee.name,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = Color(0xFF1E1E1E)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        EmployeeTierBadge(tier = employee.tier)
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        "${employee.position} • SĐT: ${employee.phone}",
                        fontSize = 12.sp,
                        color = Color.DarkGray
                    )
                }

                // Quick Edit / Delete
                Row {
                    IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Edit, contentDescription = "Sửa", tint = Color.Gray, modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = "Xóa", tint = PrimaryRed, modifier = Modifier.size(18.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Shift Status Pill & Salary info
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFFFAFAFA))
                    .border(1.dp, Color(0xFFEEEEEE), RoundedCornerShape(10.dp))
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Live shift state
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(if (isCheckedIn) Color(0xFF2E7D32) else Color.Gray)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isCheckedIn) {
                            "Đang trực ca (${lastAttendance?.formattedHourOnly ?: ""})"
                        } else {
                            "Chưa vào ca"
                        },
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isCheckedIn) Color(0xFF2E7D32) else Color.Gray
                    )
                }

                // Salary projection
                Text(
                    text = "Lương: ${formatVnd(employee.calculateSalary())}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryRed
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Action Buttons Row: Smart ID NFC/QR + Payslip
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Smart ID Badge Button
                OutlinedButton(
                    onClick = onViewBadge,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF1976D2)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1976D2))
                ) {
                    Icon(Icons.Default.QrCode, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Thẻ Smart ID", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }

                // Payslip Button
                Button(
                    onClick = onViewPayslip,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryRed)
                ) {
                    Icon(Icons.AutoMirrored.Filled.ReceiptLong, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Phiếu Lương", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

/**
 * Interactive Attendance Station Kiosk (Trạm Chấm Công)
 */
@Composable
fun AttendanceStationView(
    employees: List<Employee>,
    repository: RestaurantRepository,
    onAttendanceChanged: () -> Unit,
    onViewBadge: (Employee) -> Unit
) {
    var selectedEmployee by remember(employees) { mutableStateOf(employees.firstOrNull()) }
    var showFaceIdDialog by remember { mutableStateOf(false) }
    var isNfcScanning by remember { mutableStateOf(false) }
    var isQrScanning by remember { mutableStateOf(false) }
    var statusFeedback by remember { mutableStateOf<String?>(null) }

    // Live clock
    var currentTimeString by remember {
        mutableStateOf(SimpleDateFormat("HH:mm:ss - EEEE, dd/MM/yyyy", Locale.forLanguageTag("vi-VN")).format(Date()))
    }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            currentTimeString = SimpleDateFormat("HH:mm:ss - EEEE, dd/MM/yyyy", Locale.forLanguageTag("vi-VN")).format(Date())
        }
    }

    val activeCount = remember(employees, repository) {
        employees.count { repository.getLastAttendanceForEmployee(it.id)?.type == AttendanceType.CHECK_IN }
    }

    fun executeCheckInOut(method: AttendanceMethod) {
        val emp = selectedEmployee ?: return
        val lastAtt = repository.getLastAttendanceForEmployee(emp.id)
        val isCheckingOut = lastAtt?.type == AttendanceType.CHECK_IN
        val targetType = if (isCheckingOut) AttendanceType.CHECK_OUT else AttendanceType.CHECK_IN

        val note = "Chấm công tại Trạm (${method.displayName})"
        val res = repository.recordAttendance(
            employeeId = emp.id,
            type = targetType,
            method = method,
            note = note
        )
        if (res != null) {
            statusFeedback = if (targetType == AttendanceType.CHECK_IN) {
                "✓ ${emp.name} VÀO CA THÀNH CÔNG (${method.displayName})"
            } else {
                "✓ ${emp.name} TAN CA THÀNH CÔNG! (+${res.durationHours}h)"
            }
            onAttendanceChanged()
        }
    }

    LaunchedEffect(isNfcScanning) {
        if (isNfcScanning) {
            delay(1000)
            isNfcScanning = false
            executeCheckInOut(AttendanceMethod.NFC)
        }
    }



    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Digital Clock Header Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E2C))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "TRẠM ĐIỂM DANH & CHẤM CÔNG THÔNG MINH 4.0",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF64B5F6),
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = currentTimeString,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    // Metrics
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Tổng nhân sự", fontSize = 11.sp, color = Color.LightGray)
                            Text("${employees.size}", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Đang trong ca", fontSize = 11.sp, color = Color(0xFF81C784))
                            Text("$activeCount", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF81C784))
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Chưa vào ca", fontSize = 11.sp, color = Color(0xFFFFB74D))
                            Text("${employees.size - activeCount}", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFFB74D))
                        }
                    }
                }
            }
        }

        // Quick employee selector chips
        item {
            Text("1. Chọn Nhân Viên Điểm Danh:", fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(6.dp))
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(employees) { emp ->
                    val isCheckedIn = repository.getLastAttendanceForEmployee(emp.id)?.type == AttendanceType.CHECK_IN
                    val isSelected = selectedEmployee?.id == emp.id

                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedEmployee = emp },
                        label = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(if (isCheckedIn) Color(0xFF2E7D32) else Color.Gray)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(emp.name, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
                            }
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = PrimaryRed.copy(alpha = 0.15f),
                            selectedLabelColor = PrimaryRed
                        )
                    )
                }
            }
        }

        // Selected Employee Action Terminal Box
        item {
            selectedEmployee?.let { emp ->
                val lastAtt = repository.getLastAttendanceForEmployee(emp.id)
                val isCurrentlyIn = lastAtt?.type == AttendanceType.CHECK_IN

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(3.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(emp.name, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                                Text("${emp.position} • Mã NFC: ${emp.getNfcTagCode()}", fontSize = 12.sp, color = Color.Gray)
                            }
                            AssistChip(
                                onClick = { onViewBadge(emp) },
                                leadingIcon = { Icon(Icons.Default.QrCode, contentDescription = null, modifier = Modifier.size(16.dp)) },
                                label = { Text("Xem Thẻ") }
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Current status banner
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = if (isCurrentlyIn) Color(0xFFE8F5E9) else Color(0xFFFFF3E0)
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (isCurrentlyIn) "Trạng thái: ĐANG TRONG CA 🟢" else "Trạng thái: CHƯA VÀO CA ⚪",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = if (isCurrentlyIn) Color(0xFF2E7D32) else Color(0xFFE65100)
                                )
                                Text(
                                    text = if (isCurrentlyIn) "Bấm quẹt để TAN CA" else "Bấm quẹt để VÀO CA",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.DarkGray
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        Text("2. Chọn Phương Thức Xác Thực:", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(10.dp))

                        // 3 Terminal Action Cards
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // NFC Tap Card
                            Card(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { isNfcScanning = true },
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isNfcScanning) Color(0xFFE3F2FD) else Color(0xFFF9F9F9)
                                ),
                                border = if (isNfcScanning) androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF1976D2)) else androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEEEEEE))
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Nfc,
                                        contentDescription = null,
                                        tint = Color(0xFF1976D2),
                                        modifier = Modifier.size(32.dp)
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text("Chạm NFC", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    Text(if (isNfcScanning) "Đang quẹt..." else "Mô phỏng", fontSize = 10.sp, color = Color.Gray)
                                }
                            }

                            // QR Scanner Card
                            Card(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { isQrScanning = true },
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isQrScanning) Color(0xFFE8F5E9) else Color(0xFFF9F9F9)
                                ),
                                border = if (isQrScanning) androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF388E3C)) else androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEEEEEE))
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.QrCodeScanner,
                                        contentDescription = null,
                                        tint = Color(0xFF388E3C),
                                        modifier = Modifier.size(32.dp)
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text("Quét QR", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    Text(if (isQrScanning) "Đang mở..." else "Camera máy", fontSize = 10.sp, color = Color(0xFF388E3C))
                                }
                            }

                            // Face ID Card
                            Card(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { showFaceIdDialog = true },
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFF9F9F9)),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEEEEEE))
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Face,
                                        contentDescription = null,
                                        tint = PrimaryRed,
                                        modifier = Modifier.size(32.dp)
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text("Face ID", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    Text("Sinh trắc học", fontSize = 10.sp, color = Color.Gray)
                                }
                            }
                        }

                        // Feedback message
                        AnimatedVisibility(visible = statusFeedback != null) {
                            statusFeedback?.let { msg ->
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 12.dp)
                                ) {
                                    Text(
                                        text = msg,
                                        color = Color(0xFF2E7D32),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(10.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showFaceIdDialog && selectedEmployee != null) {
        FaceIdVerificationDialog(
            actionTitle = "Chấm Công Bằng Face ID",
            actionDescription = "Nhận diện khuôn mặt nhân viên: ${selectedEmployee?.name}",
            onSuccess = {
                showFaceIdDialog = false
                executeCheckInOut(AttendanceMethod.FACE_ID)
            },
            onDismiss = {
                showFaceIdDialog = false
            }
        )
    }

    if (isQrScanning) {
        EmployeeQrAttendanceDialog(
            employees = employees,
            initialSelectedEmployee = selectedEmployee,
            onEmployeeScanned = { scannedEmp ->
                selectedEmployee = scannedEmp
                isQrScanning = false
                executeCheckInOut(AttendanceMethod.QR_CODE)
            },
            onDismiss = {
                isQrScanning = false
            }
        )
    }
}

/**
 * Attendance Logs View (Nhật Ký Điểm Danh)
 */
@Composable
fun AttendanceLogsView(
    records: List<AttendanceRecord>,
    onRefresh: () -> Unit
) {
    var filterType by remember { mutableStateOf<AttendanceType?>(null) }

    val filteredRecords = remember(records, filterType) {
        if (filterType == null) records
        else records.filter { it.type == filterType }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Nhật ký ra vào (${filteredRecords.size})", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            IconButton(onClick = onRefresh) {
                Icon(Icons.Default.Refresh, contentDescription = "Tải lại", tint = PrimaryRed)
            }
        }

        // Filter chips
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(vertical = 6.dp)
        ) {
            FilterChip(
                selected = filterType == null,
                onClick = { filterType = null },
                label = { Text("Tất cả (${records.size})") }
            )
            FilterChip(
                selected = filterType == AttendanceType.CHECK_IN,
                onClick = { filterType = AttendanceType.CHECK_IN },
                label = { Text("🟢 Vào ca") }
            )
            FilterChip(
                selected = filterType == AttendanceType.CHECK_OUT,
                onClick = { filterType = AttendanceType.CHECK_OUT },
                label = { Text("🔴 Tan ca") }
            )
        }

        if (filteredRecords.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text("Chưa có bản ghi điểm danh nào.", color = Color.Gray, fontSize = 14.sp)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredRecords) { rec ->
                    ModernAttendanceRecordCard(record = rec)
                }
            }
        }
    }
}

@Composable
fun ModernAttendanceRecordCard(record: AttendanceRecord) {
    val isCheckIn = record.type == AttendanceType.CHECK_IN
    val methodColor = when (record.method) {
        AttendanceMethod.NFC, AttendanceMethod.NFC_CARD -> Color(0xFF1976D2)
        AttendanceMethod.QR_CODE -> Color(0xFF388E3C)
        AttendanceMethod.FACE_ID -> PrimaryRed
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(1.5.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(
                            if (isCheckIn) Color(0xFF2E7D32).copy(alpha = 0.12f)
                            else Color(0xFFE65100).copy(alpha = 0.12f)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isCheckIn) Icons.AutoMirrored.Filled.Login else Icons.AutoMirrored.Filled.Logout,
                        contentDescription = null,
                        tint = if (isCheckIn) Color(0xFF2E7D32) else Color(0xFFE65100),
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(record.employeeName, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        "${record.type.displayName} lúc ${record.formattedTime}",
                        fontSize = 12.sp,
                        color = Color.DarkGray
                    )
                    if (record.durationHours > 0) {
                        Text(
                            "Thời lượng làm việc: +${record.durationHours} giờ",
                            fontSize = 11.sp,
                            color = Color(0xFF2E7D32),
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(methodColor.copy(alpha = 0.12f))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    record.method.displayName,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = methodColor
                )
            }
        }
    }
}

/**
 * Add / Edit Employee Dialog
 */
@Composable
fun AddEditEmployeeDialog(
    employee: Employee?,
    onDismiss: () -> Unit,
    onSave: (Employee) -> Unit
) {
    var name by remember { mutableStateOf(employee?.name ?: "") }
    var phone by remember { mutableStateOf(employee?.phone ?: "") }
    var position by remember { mutableStateOf(employee?.position ?: "Phục vụ") }
    var selectedTier by remember { mutableStateOf(employee?.tier ?: EmployeeTier.CHINH_THUC) }
    var selectedContract by remember { mutableStateOf(employee?.contractType ?: ContractType.MONTHLY) }

    var hourlyRateText by remember { mutableStateOf(employee?.hourlyRate?.toInt()?.toString() ?: "25000") }
    var monthlySalaryText by remember { mutableStateOf(employee?.monthlySalary?.toInt()?.toString() ?: "8000000") }
    var workedHoursText by remember { mutableStateOf(employee?.workedHours?.toInt()?.toString() ?: "0") }
    var workedDaysText by remember { mutableStateOf(employee?.workedDays?.toInt()?.toString() ?: "26") }
    var bonusText by remember { mutableStateOf(employee?.bonus?.toInt()?.toString() ?: "0") }
    var penaltyText by remember { mutableStateOf(employee?.penalty?.toInt()?.toString() ?: "0") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (employee == null) "Thêm Nhân Viên Mới" else "Cập Nhật Hồ Sơ Nhân Viên",
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Họ và tên nhân viên") },
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = PrimaryRed) },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Số điện thoại") },
                    leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null, tint = PrimaryRed) },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = position,
                    onValueChange = { position = it },
                    label = { Text("Vị trí (VD: Phục vụ, Thu ngân, Bếp)") },
                    leadingIcon = { Icon(Icons.Default.Work, contentDescription = null, tint = PrimaryRed) },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Cấp bậc nhân sự:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    EmployeeTier.values().forEach { tier ->
                        FilterChip(
                            selected = selectedTier == tier,
                            onClick = { selectedTier = tier },
                            label = { Text(tier.displayName, fontSize = 11.sp) }
                        )
                    }
                }

                Text("Hình thức hợp đồng:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = selectedContract == ContractType.HOURLY,
                        onClick = { selectedContract = ContractType.HOURLY },
                        label = { Text("Theo Giờ (Part-time)") }
                    )
                    FilterChip(
                        selected = selectedContract == ContractType.MONTHLY,
                        onClick = { selectedContract = ContractType.MONTHLY },
                        label = { Text("Theo Tháng (Full-time)") }
                    )
                }

                if (selectedContract == ContractType.HOURLY) {
                    OutlinedTextField(
                        value = hourlyRateText,
                        onValueChange = { hourlyRateText = it },
                        label = { Text("Lương/giờ (VNĐ)") },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = workedHoursText,
                        onValueChange = { workedHoursText = it },
                        label = { Text("Số giờ làm việc") },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    OutlinedTextField(
                        value = monthlySalaryText,
                        onValueChange = { monthlySalaryText = it },
                        label = { Text("Lương thỏa thuận tháng (VNĐ)") },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = workedDaysText,
                        onValueChange = { workedDaysText = it },
                        label = { Text("Số ngày công tiêu chuẩn (/26)") },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = bonusText,
                        onValueChange = { bonusText = it },
                        label = { Text("Thưởng (+VNĐ)") },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = penaltyText,
                        onValueChange = { penaltyText = it },
                        label = { Text("Phạt (-VNĐ)") },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        val emp = Employee(
                            id = employee?.id ?: 0,
                            name = name,
                            phone = phone,
                            position = position,
                            tier = selectedTier,
                            contractType = selectedContract,
                            hourlyRate = hourlyRateText.toDoubleOrNull() ?: 25000.0,
                            monthlySalary = monthlySalaryText.toDoubleOrNull() ?: 8000000.0,
                            workedHours = workedHoursText.toDoubleOrNull() ?: 0.0,
                            workedDays = workedDaysText.toDoubleOrNull() ?: 26.0,
                            bonus = bonusText.toDoubleOrNull() ?: 0.0,
                            penalty = penaltyText.toDoubleOrNull() ?: 0.0
                        )
                        onSave(emp)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryRed),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(if (employee == null) "Thêm Mới" else "Lưu Thay Đổi")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Hủy") }
        }
    )
}
