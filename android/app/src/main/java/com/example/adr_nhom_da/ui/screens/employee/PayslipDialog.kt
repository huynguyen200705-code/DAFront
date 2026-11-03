package com.example.adr_nhom_da.ui.screens.employee

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.adr_nhom_da.data.model.ContractType
import com.example.adr_nhom_da.data.model.Employee
import com.example.adr_nhom_da.ui.components.EmployeeTierBadge
import com.example.adr_nhom_da.ui.components.formatVnd
import com.example.adr_nhom_da.ui.theme.PrimaryRed
import com.example.adr_nhom_da.ui.theme.SecondaryDarkGold
import com.example.adr_nhom_da.ui.theme.SecondaryGold
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun PayslipDialog(
    employee: Employee,
    onDismiss: () -> Unit
) {
    val totalSalary = employee.calculateSalary()
    val monthYear = remember {
        SimpleDateFormat("MM/yyyy", Locale.getDefault()).format(Date())
    }
    var isConfirmedPaid by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(SecondaryGold.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.MonetizationOn,
                        contentDescription = null,
                        tint = SecondaryDarkGold,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    "PHIẾU LƯƠNG NHÂN SỰ",
                    fontWeight = FontWeight.Black,
                    fontSize = 17.sp,
                    color = PrimaryRed,
                    letterSpacing = 1.sp
                )
                Text(
                    "GOLDEN SPOON RESTAURANT • KỲ $monthYear",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.Gray
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Employee Info Header Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF9F9F9)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEEEEEE))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(employee.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            EmployeeTierBadge(tier = employee.tier)
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "${employee.position} • SĐT: ${employee.phone} • HĐ: ${employee.contractType.displayName}",
                            fontSize = 12.sp,
                            color = Color.DarkGray
                        )
                    }
                }

                // Salary Breakdown Table
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE0E0E0))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            "CHI TIẾT CÁC KHOẢN THU NHẬP",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Gray
                        )

                        HorizontalDivider(color = Color(0xFFEEEEEE))

                        if (employee.contractType == ContractType.HOURLY) {
                            PayslipItemRow(
                                label = "Đơn giá giờ làm:",
                                detail = "${formatVnd(employee.hourlyRate)} / giờ",
                                amount = "${employee.workedHours} giờ"
                            )
                            PayslipItemRow(
                                label = "Lương cơ bản thực tế:",
                                detail = "${employee.workedHours}h x ${formatVnd(employee.hourlyRate)}",
                                amount = formatVnd(employee.hourlyRate * employee.workedHours),
                                isBold = true
                            )
                        } else {
                            PayslipItemRow(
                                label = "Lương thỏa thuận tháng:",
                                detail = "Tiêu chuẩn 26 ngày công",
                                amount = formatVnd(employee.monthlySalary)
                            )
                            PayslipItemRow(
                                label = "Ngày công thực tế ghi nhận:",
                                detail = "${employee.workedDays} / 26 ngày",
                                amount = formatVnd(employee.monthlySalary * (employee.workedDays / 26.0)),
                                isBold = true
                            )
                        }

                        HorizontalDivider(color = Color(0xFFEEEEEE))

                        // Bonus
                        PayslipItemRow(
                            label = "Thưởng hiệu suất & chuyên cần:",
                            detail = "KPI xuất sắc",
                            amount = "+${formatVnd(employee.bonus)}",
                            amountColor = Color(0xFF2E7D32)
                        )

                        // Penalty
                        PayslipItemRow(
                            label = "Khấu trừ kỷ luật / Vi phạm:",
                            detail = "Đi muộn, quy chuẩn",
                            amount = "-${formatVnd(employee.penalty)}",
                            amountColor = PrimaryRed
                        )

                        HorizontalDivider(color = Color(0xFFEEEEEE), thickness = 1.5.dp)

                        // Final Total Net Salary
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("THỰC LĨNH TỔNG KỲ:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text("Đã bao gồm thưởng & trừ phạt", fontSize = 10.sp, color = Color.Gray)
                            }
                            Text(
                                text = formatVnd(totalSalary),
                                fontWeight = FontWeight.Black,
                                fontSize = 18.sp,
                                color = PrimaryRed
                            )
                        }
                    }
                }

                // Official Approval Stamp Banner
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isConfirmedPaid) Color(0xFFE8F5E9) else Color(0xFFFFF8E1))
                        .border(
                            1.dp,
                            if (isConfirmedPaid) Color(0xFF2E7D32) else SecondaryGold,
                            RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (isConfirmedPaid) "✔ ĐÃ XÁC NHẬN CHI TRẢ LƯƠNG CHO NHÂN VIÊN" else "⏳ TRẠNG THÁI: CHỜ QUẢN LÝ DUYỆT CHI",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isConfirmedPaid) Color(0xFF2E7D32) else SecondaryDarkGold
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { isConfirmedPaid = !isConfirmedPaid },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isConfirmedPaid) Color(0xFF2E7D32) else PrimaryRed
                ),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(if (isConfirmedPaid) "Đã Duyệt Chi ✓" else "Duyệt Chi Lương")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Đóng")
            }
        }
    )
}

@Composable
private fun PayslipItemRow(
    label: String,
    detail: String,
    amount: String,
    isBold: Boolean = false,
    amountColor: Color = Color(0xFF1E1E1E)
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(label, fontSize = 12.sp, fontWeight = if (isBold) FontWeight.SemiBold else FontWeight.Normal)
            Text(detail, fontSize = 10.sp, color = Color.Gray)
        }
        Text(
            text = amount,
            fontSize = 12.sp,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.Medium,
            color = amountColor
        )
    }
}
