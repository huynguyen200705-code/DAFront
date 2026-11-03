package com.example.adr_nhom_da.ui.screens.employee

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.adr_nhom_da.data.model.AttendanceMethod
import com.example.adr_nhom_da.data.model.AttendanceType
import com.example.adr_nhom_da.data.model.Employee
import com.example.adr_nhom_da.data.repository.RestaurantRepository
import com.example.adr_nhom_da.ui.components.EmployeeTierBadge
import com.example.adr_nhom_da.ui.components.FaceIdVerificationDialog
import com.example.adr_nhom_da.ui.theme.PrimaryRed
import kotlinx.coroutines.delay

@Composable
fun EmployeeBadgeDialog(
    employee: Employee,
    repository: RestaurantRepository,
    onAttendanceUpdated: () -> Unit,
    onDismiss: () -> Unit
) {
    var isCheckingOut by remember {
        val lastAtt = repository.getLastAttendanceForEmployee(employee.id)
        mutableStateOf(lastAtt?.type == AttendanceType.CHECK_IN)
    }

    var showFaceIdDialog by remember { mutableStateOf(false) }
    var successToastMessage by remember { mutableStateOf<String?>(null) }
    var isNfcSimulating by remember { mutableStateOf(false) }
    var isQrSimulating by remember { mutableStateOf(false) }

    fun processAttendance(method: AttendanceMethod) {
        val type = if (isCheckingOut) AttendanceType.CHECK_OUT else AttendanceType.CHECK_IN
        val note = when (method) {
            AttendanceMethod.NFC -> "Chấm công qua Thẻ nhân viên NFC"
            AttendanceMethod.QR_CODE -> "Quét mã QR Thẻ nhân viên"
            AttendanceMethod.FACE_ID -> "Nhận diện Face ID sinh trắc học"
        }
        val record = repository.recordAttendance(
            employeeId = employee.id,
            type = type,
            method = method,
            note = note
        )
        if (record != null) {
            isCheckingOut = !isCheckingOut
            successToastMessage = if (type == AttendanceType.CHECK_IN) {
                "✓ VÀO CA THÀNH CÔNG (${method.displayName})"
            } else {
                "✓ TAN CA THÀNH CÔNG! Đã cộng ${record.durationHours}h làm việc"
            }
            onAttendanceUpdated()
        }
    }

    // NFC Wave animation
    val infiniteTransition = rememberInfiniteTransition(label = "nfc_pulse")
    val nfcPulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "nfc_scale"
    )
    val nfcPulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "nfc_alpha"
    )

    LaunchedEffect(isNfcSimulating) {
        if (isNfcSimulating) {
            delay(1200)
            isNfcSimulating = false
            processAttendance(AttendanceMethod.NFC)
        }
    }

    LaunchedEffect(isQrSimulating) {
        if (isQrSimulating) {
            delay(1200)
            isQrSimulating = false
            processAttendance(AttendanceMethod.QR_CODE)
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .wrapContentHeight()
                .padding(vertical = 16.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Badge,
                            contentDescription = null,
                            tint = PrimaryRed,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Thẻ Nhân Viên & NFC / QR",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Đóng", tint = Color.Gray)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Realistic Vertical Smart Badge
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(6.dp, RoundedCornerShape(16.dp)),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Top Badge Header with lanyard hole styling
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(64.dp)
                                .background(
                                    Brush.linearGradient(
                                        listOf(PrimaryRed, Color(0xFFB71C1C))
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            // Lanyard slot hole
                            Box(
                                modifier = Modifier
                                    .size(width = 40.dp, height = 7.dp)
                                    .align(Alignment.TopCenter)
                                    .offset(y = 6.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color.White.copy(alpha = 0.45f))
                            )

                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(top = 10.dp)
                            ) {
                                Text(
                                    text = "GOLDEN SPOON RESTAURANT",
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 1.sp
                                )
                                Text(
                                    text = "STAFF SMART IDENTIFICATION PASS",
                                    color = Color.White.copy(alpha = 0.85f),
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Avatar & Name info
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(60.dp)
                                    .border(2.dp, PrimaryRed, CircleShape)
                                    .clip(CircleShape)
                                    .background(PrimaryRed.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    tint = PrimaryRed,
                                    modifier = Modifier.size(38.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(14.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = employee.name,
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1E1E2C)
                                )
                                Text(
                                    text = employee.position,
                                    fontSize = 13.sp,
                                    color = Color.DarkGray,
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(modifier = Modifier.height(3.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    EmployeeTierBadge(tier = employee.tier)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "ID: EMP-${employee.id.toString().padStart(4, '0')}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.Gray
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            color = Color(0xFFEEEEEE)
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        // QR Code Graphic + NFC Tag Display
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.SpaceAround,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // QR Matrix Canvas simulation
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Box(
                                    modifier = Modifier
                                        .size(110.dp)
                                        .border(1.dp, Color(0xFFDDDDDD), RoundedCornerShape(8.dp))
                                        .background(Color.White)
                                        .padding(6.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    SimulatedQrMatrixCanvas(
                                        seed = employee.id.toInt(),
                                        modifier = Modifier.size(96.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = employee.getQrCodeContent(),
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.DarkGray
                                )
                            }

                            // NFC Contactless Tag Card
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFFF7F8FA))
                                    .border(1.dp, Color(0xFFE0E0E0), RoundedCornerShape(12.dp))
                                    .padding(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier.size(46.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isNfcSimulating) {
                                        Box(
                                            modifier = Modifier
                                                .size((46 * nfcPulseScale).dp)
                                                .border(2.dp, PrimaryRed.copy(alpha = nfcPulseAlpha), CircleShape)
                                        )
                                    }
                                    Icon(
                                        imageVector = Icons.Default.Nfc,
                                        contentDescription = "NFC Tag",
                                        tint = if (isNfcSimulating) PrimaryRed else Color(0xFF1976D2),
                                        modifier = Modifier.size(38.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Chip NFC 13.56MHz",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1976D2)
                                )
                                Text(
                                    text = employee.getNfcTagCode(),
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = Color.Gray
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Current Attendance Status Banner
                        val bannerColor = if (isCheckingOut) Color(0xFFE8F5E9) else Color(0xFFFFF3E0)
                        val textStatusColor = if (isCheckingOut) Color(0xFF2E7D32) else Color(0xFFE65100)

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(bannerColor)
                                .padding(vertical = 8.dp, horizontal = 16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (isCheckingOut) Icons.Default.CheckCircle else Icons.Default.Schedule,
                                    contentDescription = null,
                                    tint = textStatusColor,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isCheckingOut) "Trạng thái: Đang trong ca làm việc" else "Trạng thái: Chưa vào ca / Đã tan ca",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = textStatusColor
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Success Message Toast inside Dialog
                AnimatedVisibility(visible = successToastMessage != null) {
                    successToastMessage?.let { msg ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp)
                        ) {
                            Text(
                                text = msg,
                                color = Color(0xFF2E7D32),
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp)
                            )
                        }
                    }
                }

                // Interactive Simulation Buttons
                Text(
                    text = "Mô phỏng Chấm Công với Thẻ này:",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.DarkGray,
                    modifier = Modifier.align(Alignment.Start)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // NFC Tap Button
                    Button(
                        onClick = { isNfcSimulating = true },
                        enabled = !isNfcSimulating && !isQrSimulating,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1976D2)),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(vertical = 10.dp)
                    ) {
                        Icon(Icons.Default.Nfc, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (isNfcSimulating) "Đang chạm..." else "Chạm NFC", fontSize = 12.sp)
                    }

                    // QR Scan Button
                    Button(
                        onClick = { isQrSimulating = true },
                        enabled = !isNfcSimulating && !isQrSimulating,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF388E3C)),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(vertical = 10.dp)
                    ) {
                        Icon(Icons.Default.QrCode, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (isQrSimulating) "Đang quét..." else "Quét QR", fontSize = 12.sp)
                    }

                    // Face ID Button
                    Button(
                        onClick = { showFaceIdDialog = true },
                        enabled = !isNfcSimulating && !isQrSimulating,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryRed),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(vertical = 10.dp)
                    ) {
                        Icon(Icons.Default.Face, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Face ID", fontSize = 12.sp)
                    }
                }
            }
        }
    }

    // FaceID Check-in modal
    if (showFaceIdDialog) {
        FaceIdVerificationDialog(
            actionTitle = "Chấm Công Bằng Face ID",
            actionDescription = "Xác minh danh tính khuôn mặt nhân viên: ${employee.name}",
            onSuccess = {
                showFaceIdDialog = false
                processAttendance(AttendanceMethod.FACE_ID)
            },
            onDismiss = {
                showFaceIdDialog = false
            }
        )
    }
}

/**
 * Procedural Vector QR Code Canvas simulation for the badge
 */
@Composable
fun SimulatedQrMatrixCanvas(
    seed: Int,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val gridSize = 19
        val cellSize = size.width / gridSize

        // Clear white background
        drawRect(Color.White, Offset.Zero, size)

        fun drawFinderPattern(startX: Int, startY: Int) {
            // Outer 7x7 black box
            drawRect(
                color = Color.Black,
                topLeft = Offset(startX * cellSize, startY * cellSize),
                size = Size(7 * cellSize, 7 * cellSize)
            )
            // Inner 5x5 white box
            drawRect(
                color = Color.White,
                topLeft = Offset((startX + 1) * cellSize, (startY + 1) * cellSize),
                size = Size(5 * cellSize, 5 * cellSize)
            )
            // Center 3x3 black box
            drawRect(
                color = Color.Black,
                topLeft = Offset((startX + 2) * cellSize, (startY + 2) * cellSize),
                size = Size(3 * cellSize, 3 * cellSize)
            )
        }

        // Draw standard QR 3 corner finders
        drawFinderPattern(0, 0) // Top-Left
        drawFinderPattern(gridSize - 7, 0) // Top-Right
        drawFinderPattern(0, gridSize - 7) // Bottom-Left

        // Draw deterministic pseudo data modules
        for (row in 0 until gridSize) {
            for (col in 0 until gridSize) {
                val inTopLeftFinder = row < 8 && col < 8
                val inTopRightFinder = row < 8 && col >= gridSize - 8
                val inBottomLeftFinder = row >= gridSize - 8 && col < 8
                if (inTopLeftFinder || inTopRightFinder || inBottomLeftFinder) continue

                // Pseudo-hash cell fill
                val hash = (row * 31 + col * 17 + seed * 23) xor (row xor col)
                if (hash % 3 == 0 || (row % 2 == 0 && col % 3 == 1)) {
                    drawRect(
                        color = Color.Black,
                        topLeft = Offset(col * cellSize, row * cellSize),
                        size = Size(cellSize * 0.92f, cellSize * 0.92f)
                    )
                }
            }
        }
    }
}
