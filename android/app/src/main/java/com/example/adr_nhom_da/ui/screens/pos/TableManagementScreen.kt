package com.example.adr_nhom_da.ui.screens.pos

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Chair
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.TableRestaurant
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.adr_nhom_da.data.model.RestaurantTable
import com.example.adr_nhom_da.data.model.TableStatus
import com.example.adr_nhom_da.data.repository.RestaurantRepository
import com.example.adr_nhom_da.ui.components.TableStatusBadge
import com.example.adr_nhom_da.ui.components.formatVnd
import com.example.adr_nhom_da.ui.theme.PrimaryRed

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TableManagementScreen(
    repository: RestaurantRepository,
    onOpenOrder: (RestaurantTable) -> Unit
) {
    var tables by remember { mutableStateOf(repository.getAllTables()) }
    var selectedFloor by remember { mutableStateOf("Tất cả") }
    val floors = listOf("Tất cả", "Tầng 1", "Tầng 2", "Phòng VIP")

    var showAddDialog by remember { mutableStateOf(false) }
    var selectedTableForAction by remember { mutableStateOf<RestaurantTable?>(null) }

    // QR Code Ordering Dialog States
    var showQrScannerDialog by remember { mutableStateOf(false) }
    var selectedTableForQrStandee by remember { mutableStateOf<RestaurantTable?>(null) }
    var selectedTableForCustomerOrder by remember { mutableStateOf<RestaurantTable?>(null) }

    fun refreshData() {
        tables = repository.getAllTables()
    }

    val filteredTables = if (selectedFloor == "Tất cả") tables else tables.filter { it.floor == selectedFloor }

    // Statistics counts
    val emptyCount = tables.count { it.status == TableStatus.TRONG }
    val servingCount = tables.count { it.status == TableStatus.DANG_PHUC_VU }
    val reservedCount = tables.count { it.status == TableStatus.DA_DAT }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Sơ Đồ Bàn Ăn Nhà Hàng", fontWeight = FontWeight.Black, fontSize = 18.sp)
                        Text("${tables.size} bàn • Quản lý & Gọi món qua mã QR", fontSize = 11.sp, color = Color.Gray)
                    }
                },
                actions = {
                    FilledTonalButton(
                        onClick = { showQrScannerDialog = true },
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = PrimaryRed.copy(alpha = 0.12f),
                            contentColor = PrimaryRed
                        ),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Icon(Icons.Default.QrCodeScanner, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Quét QR Bàn", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = PrimaryRed,
                elevation = FloatingActionButtonDefaults.elevation(6.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Thêm bàn", tint = Color.White)
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // Live Status Overview Banner
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(3.dp, RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp, horizontal = 14.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    StatusMetricItem(
                        count = emptyCount,
                        label = "Trống",
                        color = Color(0xFF2E7D32),
                        bgColor = Color(0xFFE8F5E9)
                    )
                    VerticalDivider(modifier = Modifier.height(28.dp), color = Color(0xFFEEEEEE))
                    StatusMetricItem(
                        count = servingCount,
                        label = "Đang phục vụ",
                        color = Color(0xFFD84315),
                        bgColor = Color(0xFFFBE9E7)
                    )
                    VerticalDivider(modifier = Modifier.height(28.dp), color = Color(0xFFEEEEEE))
                    StatusMetricItem(
                        count = reservedCount,
                        label = "Đã đặt",
                        color = Color(0xFF1565C0),
                        bgColor = Color(0xFFE3F2FD)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Modern Floor Filter Chips
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(floors) { floor ->
                    val isSelected = selectedFloor == floor
                    val count = if (floor == "Tất cả") tables.size else tables.count { it.floor == floor }

                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedFloor = floor },
                        label = {
                            Text(
                                text = "$floor ($count)",
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 12.sp
                            )
                        },
                        shape = RoundedCornerShape(20.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = PrimaryRed,
                            selectedLabelColor = Color.White,
                            containerColor = Color.White
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = isSelected,
                            borderColor = if (isSelected) PrimaryRed else Color(0xFFE0E0E0)
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Tables Grid
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(filteredTables) { table ->
                    val activeItems = repository.getActiveOrderForTable(table.id)
                    val activeTotal = activeItems.sumOf { it.totalPrice }

                    TableCard(
                        table = table,
                        activeItemCount = activeItems.size,
                        activeOrderTotal = activeTotal,
                        onClick = {
                            selectedTableForAction = table
                        },
                        onViewQr = {
                            selectedTableForQrStandee = table
                        }
                    )
                }
            }
        }
    }

    // Add Table Dialog
    if (showAddDialog) {
        var tableName by remember { mutableStateOf("") }
        var floor by remember { mutableStateOf("Tầng 1") }
        var seats by remember { mutableStateOf("4") }

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Thêm Bàn Ăn Mới", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    OutlinedTextField(
                        value = tableName,
                        onValueChange = { tableName = it },
                        label = { Text("Tên bàn (VD: Bàn 104)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = floor,
                        onValueChange = { floor = it },
                        label = { Text("Khu vực / Tầng") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = seats,
                        onValueChange = { seats = it },
                        label = { Text("Số ghế") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (tableName.isNotBlank()) {
                            repository.addTable(
                                RestaurantTable(
                                    name = tableName,
                                    floor = floor,
                                    seats = seats.toIntOrNull() ?: 4
                                )
                            )
                            refreshData()
                            showAddDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryRed)
                ) {
                    Text("Lưu")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) { Text("Hủy") }
            }
        )
    }

    // Status Action Dialog for Tables
    selectedTableForAction?.let { table ->
        AlertDialog(
            onDismissRequest = { selectedTableForAction = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.TableRestaurant, contentDescription = null, tint = PrimaryRed)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(table.name, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column {
                    Text("${table.floor} • ${table.seats} ghế", fontSize = 13.sp, color = Color.Gray)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Trạng thái: ", fontSize = 13.sp)
                        TableStatusBadge(status = table.status)
                    }
                    Spacer(modifier = Modifier.height(14.dp))

                    // QR Standee Button
                    OutlinedButton(
                        onClick = {
                            selectedTableForQrStandee = table
                            selectedTableForAction = null
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.QrCode, contentDescription = null, tint = Color(0xFF1976D2), modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("📱 Xem Standee Mã QR Đặt Bàn", color = Color(0xFF1976D2), fontWeight = FontWeight.SemiBold)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Customer QR Ordering button
                    Button(
                        onClick = {
                            selectedTableForCustomerOrder = table
                            selectedTableForAction = null
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.QrCodeScanner, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("🍽️ Khách Quét QR Tự Gọi Món")
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    Text("Thao tác nghiệp vụ bàn ăn:", fontWeight = FontWeight.Medium, fontSize = 13.sp, color = Color.DarkGray)
                    Spacer(modifier = Modifier.height(8.dp))

                    when (table.status) {
                        TableStatus.TRONG -> {
                            Button(
                                onClick = {
                                    repository.updateTableStatus(table.id, TableStatus.DANG_PHUC_VU)
                                    refreshData()
                                    selectedTableForAction = null
                                    onOpenOrder(table.copy(status = TableStatus.DANG_PHUC_VU))
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryRed),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("🍽️ Mở Bàn Gọi Món (Đang Phục Vụ)")
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            OutlinedButton(
                                onClick = {
                                    repository.updateTableStatus(table.id, TableStatus.DA_DAT)
                                    refreshData()
                                    selectedTableForAction = null
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("📅 Đặt Bàn Trước")
                            }
                        }
                        TableStatus.DA_DAT -> {
                            Button(
                                onClick = {
                                    repository.updateTableStatus(table.id, TableStatus.DANG_PHUC_VU)
                                    refreshData()
                                    selectedTableForAction = null
                                    onOpenOrder(table.copy(status = TableStatus.DANG_PHUC_VU))
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryRed),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("✅ Khách Nhận Bàn (Mở Phục Vụ)")
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            OutlinedButton(
                                onClick = {
                                    repository.updateTableStatus(table.id, TableStatus.TRONG)
                                    refreshData()
                                    selectedTableForAction = null
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("❌ Hủy Đặt Bàn (Về Trống)")
                            }
                        }
                        TableStatus.DANG_PHUC_VU -> {
                            Button(
                                onClick = {
                                    selectedTableForAction = null
                                    onOpenOrder(table)
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryRed),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("📝 Xem/Gọi Món & Thanh Toán")
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            OutlinedButton(
                                onClick = {
                                    repository.clearActiveOrderForTable(table.id)
                                    repository.updateTableStatus(table.id, TableStatus.TRONG)
                                    refreshData()
                                    selectedTableForAction = null
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("🧹 Hủy Bàn (Trả Về Bàn Trống)")
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { selectedTableForAction = null }) { Text("Đóng") }
            }
        )
    }

    // QR Standee Table Tent Dialog
    selectedTableForQrStandee?.let { table ->
        TableQrStandeeDialog(
            table = table,
            onStartOrder = { scannedTable ->
                selectedTableForQrStandee = null
                selectedTableForCustomerOrder = scannedTable
            },
            onDismiss = { selectedTableForQrStandee = null }
        )
    }

    // QR Order Scanner Viewfinder Dialog
    if (showQrScannerDialog) {
        QrOrderScannerDialog(
            repository = repository,
            onTableScanned = { scannedTable ->
                showQrScannerDialog = false
                selectedTableForCustomerOrder = scannedTable
            },
            onOrderScanned = { scannedOrder ->
                showQrScannerDialog = false
                val matched = tables.firstOrNull { it.id == scannedOrder.tableId }
                if (matched != null) {
                    onOpenOrder(matched)
                }
            },
            onDismiss = { showQrScannerDialog = false }
        )
    }

    // Customer QR Digital Menu Ordering Dialog
    selectedTableForCustomerOrder?.let { table ->
        CustomerQrOrderDialog(
            table = table,
            repository = repository,
            onOrderCompleted = { refreshData() },
            onProceedToCheckout = { checkedTable ->
                selectedTableForCustomerOrder = null
                onOpenOrder(checkedTable)
            },
            onDismiss = {
                selectedTableForCustomerOrder = null
                refreshData()
            }
        )
    }
}

@Composable
fun StatusMetricItem(
    count: Int,
    label: String,
    color: Color,
    bgColor: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .background(bgColor, RoundedCornerShape(8.dp))
                .padding(horizontal = 10.dp, vertical = 2.dp)
        ) {
            Text(
                text = "$count",
                fontWeight = FontWeight.Black,
                fontSize = 17.sp,
                color = color
            )
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            color = Color.Gray,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun TableCard(
    table: RestaurantTable,
    activeItemCount: Int,
    activeOrderTotal: Double,
    onClick: () -> Unit,
    onViewQr: () -> Unit
) {
    val borderColor = when (table.status) {
        TableStatus.TRONG -> Color(0xFFC8E6C9)
        TableStatus.DANG_PHUC_VU -> Color(0xFFFFCCBC)
        TableStatus.DA_DAT -> Color(0xFFBBDEFB)
    }

    val iconColor = when (table.status) {
        TableStatus.TRONG -> Color(0xFF2E7D32)
        TableStatus.DANG_PHUC_VU -> PrimaryRed
        TableStatus.DA_DAT -> Color(0xFF1565C0)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(3.dp, RoundedCornerShape(16.dp))
            .border(1.2.dp, borderColor, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            // QR shortcut button on top right of card with nice touch circle
            IconButton(
                onClick = onViewQr,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp)
                    .size(32.dp)
                    .background(Color(0xFFF0F4F8), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.QrCode,
                    contentDescription = "Mã QR Bàn",
                    tint = Color(0xFF1976D2),
                    modifier = Modifier.size(18.dp)
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Table visual seating graphic
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .background(iconColor.copy(alpha = 0.08f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.TableRestaurant,
                        contentDescription = null,
                        modifier = Modifier.size(28.dp),
                        tint = iconColor
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))
                Text(table.name, fontWeight = FontWeight.Black, fontSize = 16.sp, color = Color(0xFF1E1E2C))
                Text("${table.floor} • ${table.seats} ghế", fontSize = 11.sp, color = Color.Gray)

                Spacer(modifier = Modifier.height(8.dp))
                TableStatusBadge(status = table.status)

                // If currently serving, show live order summary pill
                if (table.status == TableStatus.DANG_PHUC_VU && activeItemCount > 0) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFFFF3E0),
                        border = androidx.compose.foundation.BorderStroke(0.8.dp, Color(0xFFFFB74D))
                    ) {
                        Text(
                            text = "🍽️ $activeItemCount món • ${formatVnd(activeOrderTotal)}",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFE65100),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }
    }
}
