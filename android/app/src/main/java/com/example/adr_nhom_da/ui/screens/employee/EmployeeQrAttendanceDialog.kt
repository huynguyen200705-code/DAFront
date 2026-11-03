package com.example.adr_nhom_da.ui.screens.employee

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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.adr_nhom_da.data.model.Employee
import com.example.adr_nhom_da.ui.components.CameraViewfinder
import com.example.adr_nhom_da.ui.theme.PrimaryRed
import kotlinx.coroutines.delay

@Composable
fun EmployeeQrAttendanceDialog(
    employees: List<Employee>,
    initialSelectedEmployee: Employee? = null,
    onEmployeeScanned: (Employee) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedEmployee by remember { mutableStateOf(initialSelectedEmployee ?: employees.firstOrNull()) }
    var isScanning by remember { mutableStateOf(true) }
    var scannedSuccessEmployee by remember { mutableStateOf<Employee?>(null) }
    var isTorchOn by remember { mutableStateOf(false) }

    // Laser Animation
    val infiniteTransition = rememberInfiniteTransition(label = "emp_qr_anim")
    val laserY by infiniteTransition.animateFloat(
        initialValue = 0.05f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1300, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "laser_pos"
    )

    fun triggerSuccess(emp: Employee) {
        if (!isScanning) return
        isScanning = false
        scannedSuccessEmployee = emp
    }

    LaunchedEffect(scannedSuccessEmployee) {
        scannedSuccessEmployee?.let { emp ->
            delay(850)
            onEmployeeScanned(emp)
        }
    }

    fun handleBarcodeScanned(rawCode: String) {
        if (!isScanning) return
        val code = rawCode.trim()

        val matchedEmp = employees.firstOrNull { emp ->
            code.contains("EMP-${emp.id}", ignoreCase = true) ||
            code.contains(emp.getQrCodeContent(), ignoreCase = true) ||
            code.contains(emp.phone, ignoreCase = true) ||
            code.contains(emp.name, ignoreCase = true) ||
            code == emp.id.toString()
        } ?: selectedEmployee ?: employees.firstOrNull()

        if (matchedEmp != null) {
            triggerSuccess(matchedEmp)
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
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .background(Color(0xFF388E3C).copy(alpha = 0.2f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.QrCodeScanner,
                                contentDescription = null,
                                tint = Color(0xFF00E676),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Quét QR Điểm Danh",
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Hướng Camera vào Thẻ nhân viên",
                                color = Color(0xFFA5D6A7),
                                fontSize = 11.sp
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = { isTorchOn = !isTorchOn },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = if (isTorchOn) Icons.Default.FlashOn else Icons.Default.FlashOff,
                                contentDescription = "Flash",
                                tint = if (isTorchOn) Color(0xFFFFD600) else Color.Gray,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "Đóng", tint = Color.LightGray)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Camera Viewfinder Box
                Box(
                    modifier = Modifier
                        .size(200.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color.Black)
                        .border(1.5.dp, Color(0xFF2E2E44), RoundedCornerShape(18.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    CameraViewfinder(
                        modifier = Modifier.fillMaxSize(),
                        lensFacing = CameraSelector.LENS_FACING_BACK,
                        isTorchEnabled = isTorchOn,
                        enableBarcodeScanner = isScanning,
                        onBarcodeScanned = { code ->
                            handleBarcodeScanned(code)
                        }
                    )

                    if (scannedSuccessEmployee != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color(0xE6101018)),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = Color(0xFF00E676),
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "ĐIỂM DANH THÀNH CÔNG!",
                                    color = Color(0xFF00E676),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                                Text(
                                    text = scannedSuccessEmployee?.name ?: "",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = scannedSuccessEmployee?.position ?: "",
                                    color = Color.LightGray,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    } else {
                        // Viewfinder Reticle & Laser
                        Box(
                            modifier = Modifier
                                .size(160.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .border(1.dp, Color(0x3300E676), RoundedCornerShape(12.dp))
                        ) {
                            // Laser
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(2.5.dp)
                                    .offset(y = (laserY * 155).dp)
                                    .background(
                                        Brush.horizontalGradient(
                                            listOf(
                                                Color.Transparent,
                                                Color(0xFF00E676),
                                                Color.White,
                                                Color(0xFF00E676),
                                                Color.Transparent
                                            )
                                        )
                                    )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Quick employee selection chips
                Text(
                    text = "Hoặc chọn nhân viên kiểm thử:",
                    color = Color.LightGray,
                    fontSize = 11.sp,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(4.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(employees) { emp ->
                        val isSelected = selectedEmployee?.id == emp.id
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedEmployee = emp },
                            label = { Text(emp.name, fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = Color(0xFF1E1E2C),
                                labelColor = Color.White,
                                selectedContainerColor = Color(0xFF2E7D32),
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = {
                        selectedEmployee?.let { triggerSuccess(it) }
                    },
                    enabled = isScanning && selectedEmployee != null,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.QrCodeScanner, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Điểm danh: ${selectedEmployee?.name ?: ""}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

