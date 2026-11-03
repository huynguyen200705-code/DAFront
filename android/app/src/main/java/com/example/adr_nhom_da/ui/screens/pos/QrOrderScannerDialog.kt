package com.example.adr_nhom_da.ui.screens.pos

import androidx.camera.core.CameraSelector
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.adr_nhom_da.data.model.Order
import com.example.adr_nhom_da.data.model.RestaurantTable
import com.example.adr_nhom_da.data.repository.RestaurantRepository
import com.example.adr_nhom_da.ui.components.CameraViewfinder
import com.example.adr_nhom_da.ui.components.formatVnd
import com.example.adr_nhom_da.ui.theme.PrimaryRed
import kotlinx.coroutines.delay

enum class QrScanMode(val title: String) {
    TABLE("Mã QR Bàn Ăn"),
    ORDER("Mã QR Đơn Món")
}

@Composable
fun QrOrderScannerDialog(
    repository: RestaurantRepository,
    onTableScanned: (RestaurantTable) -> Unit,
    onOrderScanned: ((Order) -> Unit)? = null,
    onDismiss: () -> Unit
) {
    val tables = remember { repository.getAllTables() }
    val completedOrders = remember { repository.getAllCompletedOrders() }

    var scanMode by remember { mutableStateOf(QrScanMode.TABLE) }
    var selectedTargetTable by remember { mutableStateOf(tables.firstOrNull()) }
    var selectedTargetOrder by remember { mutableStateOf(completedOrders.firstOrNull()) }
    var manualQrInput by remember { mutableStateOf("") }

    var isScanning by remember { mutableStateOf(true) }
    var scannedSuccessTable by remember { mutableStateOf<RestaurantTable?>(null) }
    var scannedSuccessOrder by remember { mutableStateOf<Order?>(null) }
    var isTorchOn by remember { mutableStateOf(false) }
    var lastScannedRawText by remember { mutableStateOf<String?>(null) }

    // Laser Animation
    val infiniteTransition = rememberInfiniteTransition(label = "scanner_laser_transition")
    val laserY by infiniteTransition.animateFloat(
        initialValue = 0.05f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1400, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "laser_y"
    )

    val laserAuraPulse by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 700, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "laser_pulse"
    )

    // Trigger Success Table Scan
    fun triggerTableScanSuccess(table: RestaurantTable) {
        isScanning = false
        scannedSuccessTable = table
        scannedSuccessOrder = null
    }

    // Trigger Success Order Scan
    fun triggerOrderScanSuccess(order: Order) {
        isScanning = false
        scannedSuccessOrder = order
        scannedSuccessTable = null
    }

    // Handle real camera barcode/QR code scanned
    fun handleBarcodeScanned(rawCode: String) {
        if (!isScanning) return
        val code = rawCode.trim()
        lastScannedRawText = code

        val isOrderPattern = code.contains("ORDER", ignoreCase = true)
        val isTablePattern = code.contains("TABLE", ignoreCase = true) || code.contains("tableId", ignoreCase = true)

        if (scanMode == QrScanMode.ORDER || (isOrderPattern && !isTablePattern)) {
            val matchedOrder = completedOrders.firstOrNull { order ->
                code.contains(order.getQrCodeContent(), ignoreCase = true) ||
                code.contains("ORDER-${order.id}", ignoreCase = true) ||
                code == order.id.toString()
            } ?: completedOrders.firstOrNull()

            if (matchedOrder != null) {
                triggerOrderScanSuccess(matchedOrder)
                return
            }
        }

        // Match table
        val matchedTable = tables.firstOrNull { table ->
            code.contains(table.getQrCodeContent(), ignoreCase = true) ||
            code.contains("TABLE-${table.id}", ignoreCase = true) ||
            code.contains("tableId=${table.id}", ignoreCase = true) ||
            code.contains(table.name, ignoreCase = true) ||
            code == table.id.toString()
        } ?: selectedTargetTable ?: tables.firstOrNull()

        if (matchedTable != null) {
            triggerTableScanSuccess(matchedTable)
        }
    }

    // Instantaneous scan resolution in tick tack (tích tắc)
    LaunchedEffect(scannedSuccessTable) {
        scannedSuccessTable?.let { table ->
            delay(850)
            onTableScanned(table)
        }
    }

    LaunchedEffect(scannedSuccessOrder) {
        scannedSuccessOrder?.let { order ->
            delay(850)
            if (onOrderScanned != null) {
                onOrderScanned(order)
            } else {
                val matchTable = tables.firstOrNull { it.id == order.tableId }
                if (matchTable != null) {
                    onTableScanned(matchTable)
                } else {
                    onDismiss()
                }
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .wrapContentHeight()
                .padding(vertical = 12.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF101018)),
            elevation = CardDefaults.cardElevation(12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header Bar: Camera Máy Quét Mã QR
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(Color(0xFFFF1744).copy(alpha = 0.15f), CircleShape)
                                .border(1.dp, Color(0xFFFF1744).copy(alpha = 0.4f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.QrCodeScanner,
                                contentDescription = null,
                                tint = Color(0xFFFF1744),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Camera Quét Mã QR Trực Tiếp",
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Tích hợp Camera thiết bị • ML Kit Scanner",
                                color = Color(0xFFFF8A80),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = { isTorchOn = !isTorchOn },
                            modifier = Modifier.size(34.dp)
                        ) {
                            Icon(
                                imageVector = if (isTorchOn) Icons.Default.FlashOn else Icons.Default.FlashOff,
                                contentDescription = "Đèn Flash",
                                tint = if (isTorchOn) Color(0xFFFFD600) else Color.Gray,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        IconButton(onClick = onDismiss, modifier = Modifier.size(34.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "Đóng", tint = Color.LightGray)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Scan Mode Selector (Bàn Ăn vs Đơn Món)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF1E1E2C), RoundedCornerShape(12.dp))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    QrScanMode.values().forEach { mode ->
                        val isSelected = scanMode == mode
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) PrimaryRed else Color.Transparent)
                                .clickable {
                                    scanMode = mode
                                    isScanning = true
                                    scannedSuccessTable = null
                                    scannedSuccessOrder = null
                                    lastScannedRawText = null
                                }
                                .padding(vertical = 7.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (mode == QrScanMode.TABLE) Icons.Default.TableRestaurant else Icons.AutoMirrored.Filled.ReceiptLong,
                                    contentDescription = null,
                                    tint = if (isSelected) Color.White else Color.Gray,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = mode.title,
                                    color = if (isSelected) Color.White else Color.Gray,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = if (scanMode == QrScanMode.TABLE)
                        "Hướng Camera vào mã QR standee bàn ăn để nhận diện tự động"
                    else
                        "Hướng Camera vào mã QR trên hóa đơn/phiếu order để tra cứu",
                    color = Color.LightGray,
                    fontSize = 11.5.sp,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Camera Viewfinder với khung ngắm định vị & Camera thiết bị thật
                Box(
                    modifier = Modifier
                        .size(220.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color.Black)
                        .border(1.5.dp, Color(0xFF2E2E44), RoundedCornerShape(20.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    // CameraX live preview
                    CameraViewfinder(
                        modifier = Modifier.fillMaxSize(),
                        lensFacing = CameraSelector.LENS_FACING_BACK,
                        isTorchEnabled = isTorchOn,
                        enableBarcodeScanner = isScanning,
                        onBarcodeScanned = { code ->
                            handleBarcodeScanned(code)
                        }
                    )

                    if (scannedSuccessTable != null || scannedSuccessOrder != null) {
                        // Trạng thái nhận diện thành công
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color(0xE6101018)),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(16.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(56.dp)
                                        .background(Color(0xFF00E676).copy(alpha = 0.15f), CircleShape)
                                        .border(2.dp, Color(0xFF00E676), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = Color(0xFF00E676),
                                        modifier = Modifier.size(36.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Text(
                                    text = "ĐÃ NHẬN DIỆN THÀNH CÔNG!",
                                    color = Color(0xFF00E676),
                                    fontWeight = FontWeight.Black,
                                    fontSize = 12.sp,
                                    letterSpacing = 0.5.sp
                                )

                                if (scannedSuccessTable != null) {
                                    Text(
                                        text = "${scannedSuccessTable?.name} • ${scannedSuccessTable?.floor}",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp
                                    )
                                    Text(
                                        text = "Đang mở thực đơn gọi món trong tích tắc...",
                                        color = Color.LightGray,
                                        fontSize = 11.sp,
                                        textAlign = TextAlign.Center
                                    )
                                } else if (scannedSuccessOrder != null) {
                                    Text(
                                        text = "Đơn #${scannedSuccessOrder?.id.toString().padStart(5, '0')} • ${scannedSuccessOrder?.tableName}",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                    Text(
                                        text = "Tổng: ${formatVnd(scannedSuccessOrder?.finalTotal ?: 0.0)}",
                                        color = Color(0xFFFF8A80),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                    Text(
                                        text = "Đang tiếp nhận đơn món...",
                                        color = Color.LightGray,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }
                    } else {
                        // Viewfinder Overlay over Camera Feed
                        Box(
                            modifier = Modifier
                                .size(175.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .border(1.dp, Color(0x33FF1744), RoundedCornerShape(14.dp))
                        ) {
                            // 4 Corner Brackets
                            val cornerSize = 24.dp
                            val cornerBorder = 3.5.dp
                            val cornerColor = Color(0xFFFF1744)

                            Box(
                                modifier = Modifier
                                    .size(cornerSize)
                                    .align(Alignment.TopStart)
                                    .border(cornerBorder, cornerColor, RoundedCornerShape(topStart = 8.dp))
                            )
                            Box(
                                modifier = Modifier
                                    .size(cornerSize)
                                    .align(Alignment.TopEnd)
                                    .border(cornerBorder, cornerColor, RoundedCornerShape(topEnd = 8.dp))
                            )
                            Box(
                                modifier = Modifier
                                    .size(cornerSize)
                                    .align(Alignment.BottomStart)
                                    .border(cornerBorder, cornerColor, RoundedCornerShape(bottomStart = 8.dp))
                            )
                            Box(
                                modifier = Modifier
                                    .size(cornerSize)
                                    .align(Alignment.BottomEnd)
                                    .border(cornerBorder, cornerColor, RoundedCornerShape(bottomEnd = 8.dp))
                            )

                            // Crosshair Target
                            Box(
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .size(24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .width(18.dp)
                                        .height(1.5.dp)
                                        .background(Color(0xFFFF5252).copy(alpha = 0.5f))
                                )
                                Box(
                                    modifier = Modifier
                                        .height(18.dp)
                                        .width(1.5.dp)
                                        .background(Color(0xFFFF5252).copy(alpha = 0.5f))
                                )
                                Box(
                                    modifier = Modifier
                                        .size(5.dp)
                                        .background(Color(0xFFFF1744), CircleShape)
                                )
                            }

                            // Glowing Laser Line
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(28.dp)
                                    .offset(y = ((laserY * 170) - 14).dp)
                                    .background(
                                        Brush.verticalGradient(
                                            listOf(
                                                Color.Transparent,
                                                Color(0x33FF1744).copy(alpha = laserAuraPulse * 0.4f),
                                                Color(0x66FF1744).copy(alpha = laserAuraPulse * 0.6f),
                                                Color.Transparent
                                            )
                                        )
                                    )
                            )

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(3.dp)
                                    .offset(y = (laserY * 170).dp)
                                    .shadow(6.dp, RoundedCornerShape(2.dp), spotColor = Color(0xFFFF1744), ambientColor = Color(0xFFFF1744))
                                    .background(
                                        Brush.horizontalGradient(
                                            listOf(
                                                Color.Transparent,
                                                Color(0xFFFF1744).copy(alpha = laserAuraPulse),
                                                Color(0xFFFF5252),
                                                Color.White,
                                                Color(0xFFFF5252),
                                                Color(0xFFFF1744).copy(alpha = laserAuraPulse),
                                                Color.Transparent
                                            )
                                        )
                                    )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Mock / Quick test selection
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (scanMode == QrScanMode.TABLE) "Chọn nhanh bàn (Kiểm thử):" else "Chọn nhanh đơn (Kiểm thử):",
                        color = Color.LightGray,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = if (scanMode == QrScanMode.TABLE) "${tables.size} bàn" else "${completedOrders.size} đơn",
                        color = Color.Gray,
                        fontSize = 11.sp
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                if (scanMode == QrScanMode.TABLE) {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(tables) { table ->
                            val isSelected = selectedTargetTable?.id == table.id
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedTargetTable = table },
                                label = { Text(table.name, fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    containerColor = Color(0xFF1E1E2C),
                                    labelColor = Color.White,
                                    selectedContainerColor = PrimaryRed,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }
                } else {
                    if (completedOrders.isEmpty()) {
                        Text(
                            text = "Chưa có đơn món hoàn tất nào để quét mẫu.",
                            color = Color.Gray,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    } else {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(completedOrders) { order ->
                                val isSelected = selectedTargetOrder?.id == order.id
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { selectedTargetOrder = order },
                                    label = { Text("#${order.id} • ${order.tableName}", fontSize = 11.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        containerColor = Color(0xFF1E1E2C),
                                        labelColor = Color.White,
                                        selectedContainerColor = PrimaryRed,
                                        selectedLabelColor = Color.White
                                    )
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Ô nhập mã QR thủ công
                OutlinedTextField(
                    value = manualQrInput,
                    onValueChange = { manualQrInput = it },
                    placeholder = {
                        Text(
                            text = if (scanMode == QrScanMode.TABLE) "Nhập chuỗi QR (VD: REST-TABLE-1)" else "Nhập mã đơn (VD: REST-ORDER-00001)",
                            fontSize = 11.sp,
                            color = Color.Gray
                        )
                    },
                    trailingIcon = {
                        if (manualQrInput.isNotBlank()) {
                            IconButton(onClick = {
                                handleBarcodeScanned(manualQrInput.trim())
                            }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Quét", tint = Color(0xFFFF1744))
                            }
                        }
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFF1A1A26),
                        unfocusedContainerColor = Color(0xFF14141E),
                        focusedBorderColor = Color(0xFFFF1744),
                        unfocusedBorderColor = Color(0xFF2E2E44),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Nút kích hoạt nhận diện ngay
                Button(
                    onClick = {
                        if (scanMode == QrScanMode.TABLE) {
                            selectedTargetTable?.let { triggerTableScanSuccess(it) }
                        } else {
                            selectedTargetOrder?.let { triggerOrderScanSuccess(it) }
                        }
                    },
                    enabled = isScanning && (if (scanMode == QrScanMode.TABLE) selectedTargetTable != null else selectedTargetOrder != null),
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryRed),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(vertical = 11.dp)
                ) {
                    Icon(Icons.Default.QrCodeScanner, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (scanMode == QrScanMode.TABLE)
                            "Nhận diện tức thì: ${selectedTargetTable?.name ?: ""}"
                        else
                            "Nhận diện tức thì: #${selectedTargetOrder?.id ?: ""}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.5.sp
                    )
                }
            }
        }
    }
}
