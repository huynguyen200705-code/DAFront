package com.example.adr_nhom_da.ui.screens.pos

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.adr_nhom_da.data.model.Order
import com.example.adr_nhom_da.ui.components.formatDateTime
import com.example.adr_nhom_da.ui.components.formatVnd
import com.example.adr_nhom_da.ui.theme.PrimaryRed

@Composable
fun BillReceiptDialog(
    order: Order,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .wrapContentHeight()
                .padding(vertical = 16.dp)
                .shadow(8.dp, RoundedCornerShape(20.dp)),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header icon
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(Color(0xFFE8F5E9), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = Color(0xFF2E7D32),
                        modifier = Modifier.size(28.dp)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "GOLDEN SPOON RESTAURANT",
                    fontWeight = FontWeight.Black,
                    fontSize = 17.sp,
                    color = Color(0xFF1E1E2C),
                    letterSpacing = 1.sp
                )
                Text(
                    text = "123 Đường Ẩm Thực, Quận 1 • Hotline: 1900 6868",
                    fontSize = 11.sp,
                    color = Color.Gray
                )
                Text(
                    text = "--- HÓA ĐƠN THANH TOÁN ---",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = PrimaryRed,
                    modifier = Modifier.padding(top = 4.dp)
                )
                Text(
                    text = "Mã HĐ: #${order.id.toString().padStart(5, '0')} • ${formatDateTime(order.timestamp)}",
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    color = Color.DarkGray
                )

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = Color(0xFFE0E0E0), thickness = 1.dp)
                Spacer(modifier = Modifier.height(8.dp))

                // Meta Info Box
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFF9F9FB), RoundedCornerShape(8.dp))
                        .padding(10.dp)
                ) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Bàn phục vụ:", fontSize = 12.sp, color = Color.Gray)
                        Text(order.tableName, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                    Spacer(modifier = Modifier.height(3.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Khách hàng:", fontSize = 12.sp, color = Color.Gray)
                        Text("${order.customerName} (${order.customerTierName})", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                    Spacer(modifier = Modifier.height(3.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Thu ngân:", fontSize = 12.sp, color = Color.Gray)
                        Text(order.employeeName, fontSize = 12.sp)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Chi tiết món ăn:",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    modifier = Modifier.align(Alignment.Start)
                )
                Spacer(modifier = Modifier.height(6.dp))

                // Items list
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 220.dp)
                ) {
                    items(order.items) { item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(item.menuItemName, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                if (item.note.isNotBlank()) {
                                    Text("(${item.note})", fontSize = 10.sp, color = Color.Gray)
                                }
                            }
                            Text(
                                text = "x${item.quantity}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )
                            Text(
                                text = formatVnd(item.totalPrice),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider(color = Color(0xFFE0E0E0), thickness = 1.dp)
                Spacer(modifier = Modifier.height(10.dp))

                // Breakdown
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Tạm tính:", fontSize = 13.sp, color = Color.DarkGray)
                    Text(formatVnd(order.subtotal), fontSize = 13.sp)
                }

                if (order.discountPercent > 0) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Ưu đãi (${order.customerTierName}):", color = PrimaryRed, fontSize = 13.sp)
                        Text("-${formatVnd(order.discountAmount)}", color = PrimaryRed, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("TỔNG TIỀN:", fontWeight = FontWeight.Black, fontSize = 16.sp)
                    Text(
                        formatVnd(order.finalTotal),
                        fontWeight = FontWeight.Black,
                        fontSize = 20.sp,
                        color = PrimaryRed
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Hình thức:", fontSize = 12.sp, color = Color.Gray)
                    Text(order.paymentMethod, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Simulated QR Code on Receipt for scanning & re-opening
                Box(
                    modifier = Modifier
                        .size(90.dp)
                        .border(1.dp, Color(0xFFE0E0E0), RoundedCornerShape(8.dp))
                        .background(Color.White)
                        .padding(6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    com.example.adr_nhom_da.ui.screens.employee.SimulatedQrMatrixCanvas(
                        seed = (order.id * 179 + 53).toInt(),
                        modifier = Modifier.size(76.dp)
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = order.getQrCodeContent(),
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = Color.DarkGray
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "CẢM ƠN QUÝ KHÁCH VÀ HẸN GẶP LẠI!",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Gray,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(14.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryRed),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(vertical = 12.dp)
                ) {
                    Text("Hoàn Tất & Đóng", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
        }
    }
}
