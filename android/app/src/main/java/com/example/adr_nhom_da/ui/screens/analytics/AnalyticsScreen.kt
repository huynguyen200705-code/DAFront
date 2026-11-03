package com.example.adr_nhom_da.ui.screens.analytics

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.adr_nhom_da.data.model.CustomerTier
import com.example.adr_nhom_da.data.model.Order
import com.example.adr_nhom_da.data.repository.RestaurantRepository
import com.example.adr_nhom_da.ui.components.CustomerTierBadge
import com.example.adr_nhom_da.ui.components.formatVnd
import com.example.adr_nhom_da.ui.theme.PrimaryRed
import com.example.adr_nhom_da.ui.theme.SecondaryDarkGold
import com.example.adr_nhom_da.ui.theme.SecondaryGold

enum class TimePeriod(val title: String) {
    TODAY("Hôm nay"),
    WEEK("7 ngày"),
    MONTH("Tháng này"),
    ALL("Tất cả")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(
    repository: RestaurantRepository
) {
    var selectedPeriod by remember { mutableStateOf(TimePeriod.ALL) }
    var showExportDialog by remember { mutableStateOf(false) }

    // Fetch fresh data
    var allOrders by remember { mutableStateOf(repository.getAllCompletedOrders()) }
    var customers by remember { mutableStateOf(repository.getAllCustomers()) }
    var employees by remember { mutableStateOf(repository.getAllEmployees()) }

    val syncVersion by repository.syncVersion.collectAsState()
    LaunchedEffect(syncVersion) {
        allOrders = repository.getAllCompletedOrders()
        customers = repository.getAllCustomers()
        employees = repository.getAllEmployees()
    }

    // Filter orders by period
    val now = System.currentTimeMillis()
    val filteredOrders = remember(allOrders, selectedPeriod) {
        when (selectedPeriod) {
            TimePeriod.TODAY -> {
                val oneDayAgo = now - 86400000L
                allOrders.filter { it.timestamp >= oneDayAgo }
            }
            TimePeriod.WEEK -> {
                val sevenDaysAgo = now - 7 * 86400000L
                allOrders.filter { it.timestamp >= sevenDaysAgo }
            }
            TimePeriod.MONTH -> {
                val thirtyDaysAgo = now - 30 * 86400000L
                allOrders.filter { it.timestamp >= thirtyDaysAgo }
            }
            TimePeriod.ALL -> allOrders
        }
    }

    val totalRevenue = filteredOrders.sumOf { it.finalTotal }
    val totalOrdersCount = filteredOrders.size
    val averageOrderValue = if (totalOrdersCount > 0) totalRevenue / totalOrdersCount else 0.0

    val totalSalaryBudget = employees.sumOf { it.calculateSalary() }
    val estimatedProfit = (totalRevenue - (totalSalaryBudget * 0.35)).coerceAtLeast(0.0)

    // Calculate Payment methods
    val vietQrOrders = filteredOrders.filter { it.paymentMethod.contains("VietQR", ignoreCase = true) }
    val cashOrders = filteredOrders.filter { !it.paymentMethod.contains("VietQR", ignoreCase = true) }
    val vietQrRevenue = vietQrOrders.sumOf { it.finalTotal }
    val cashRevenue = cashOrders.sumOf { it.finalTotal }

    // Calculate Top Selling Dishes
    val dishSalesMap = remember(filteredOrders) {
        val map = mutableMapOf<String, Pair<Int, Double>>() // Name -> (Quantity, TotalRevenue)
        filteredOrders.forEach { order ->
            order.items.forEach { item ->
                val current = map[item.menuItemName] ?: Pair(0, 0.0)
                map[item.menuItemName] = Pair(
                    current.first + item.quantity,
                    current.second + item.totalPrice
                )
            }
        }
        map.toList().sortedByDescending { it.second.first }.take(5)
    }

    val maxDishQuantity = dishSalesMap.maxOfOrNull { it.second.first } ?: 1

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "Báo Cáo & Thống Kê Doanh Thu",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Text(
                            "Golden Spoon Restaurant & Lounge",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }
                },
                actions = {
                    IconButton(onClick = {
                        allOrders = repository.getAllCompletedOrders()
                    }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Tải lại", tint = PrimaryRed)
                    }
                    IconButton(onClick = { showExportDialog = true }) {
                        Icon(Icons.Default.Download, contentDescription = "Xuất báo cáo", tint = Color(0xFF2E7D32))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Timeframe Segmented Chips
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFF0F0F0))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    TimePeriod.values().forEach { period ->
                        val isSelected = selectedPeriod == period
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) PrimaryRed else Color.Transparent)
                                .clickable { selectedPeriod = period }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = period.title,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) Color.White else Color.DarkGray
                            )
                        }
                    }
                }
            }

            // Top 4 Executive KPI Cards (2x2 Grid)
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Card 1: Gross Revenue
                        MetricSummaryCard(
                            modifier = Modifier.weight(1f),
                            title = "Tổng Doanh Thu",
                            value = formatVnd(totalRevenue),
                            subtitle = "Thực nhận sau KM",
                            icon = Icons.Default.MonetizationOn,
                            accentColor = PrimaryRed,
                            trend = "+18.5% ↗"
                        )

                        // Card 2: Orders Count
                        MetricSummaryCard(
                            modifier = Modifier.weight(1f),
                            title = "Số Hoá Đơn",
                            value = "$totalOrdersCount đơn",
                            subtitle = "Trung bình: ${formatVnd(averageOrderValue)}",
                            icon = Icons.AutoMirrored.Filled.ReceiptLong,
                            accentColor = SecondaryDarkGold,
                            trend = "100% Hoàn tất"
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Card 3: Loyalty & Customers
                        MetricSummaryCard(
                            modifier = Modifier.weight(1f),
                            title = "Khách Thành Viên",
                            value = "${customers.size} khách",
                            subtitle = "5 bậc thành viên VIP",
                            icon = Icons.Default.Diamond,
                            accentColor = Color(0xFF673AB7),
                            trend = "Tích luỹ 4.0"
                        )

                        // Card 4: Estimated Profit
                        MetricSummaryCard(
                            modifier = Modifier.weight(1f),
                            title = "Lợi Nhuận Gộp",
                            value = formatVnd(estimatedProfit),
                            subtitle = "Trừ chi phí nhân sự",
                            icon = Icons.AutoMirrored.Filled.TrendingUp,
                            accentColor = Color(0xFF2E7D32),
                            trend = "+24.2% ↗"
                        )
                    }
                }
            }

            // Weekly Revenue Bar Chart Canvas
            item {
                WeeklyRevenueBarChartCard(
                    totalRevenue = totalRevenue,
                    orderCount = totalOrdersCount
                )
            }

            // Top 5 Best-Selling Dishes Leaderboard
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .background(SecondaryGold.copy(alpha = 0.2f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("🏆", fontSize = 16.sp)
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "Top Món Ăn Bán Chạy Nhất",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                            }
                            Text(
                                "Xếp hạng",
                                fontSize = 12.sp,
                                color = Color.Gray,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        if (dishSalesMap.isEmpty()) {
                            Text(
                                "Chưa có dữ liệu món bán trong kỳ",
                                fontSize = 13.sp,
                                color = Color.Gray,
                                modifier = Modifier.padding(vertical = 12.dp)
                            )
                        } else {
                            dishSalesMap.forEachIndexed { index, (dishName, sales) ->
                                val rankBadge = when (index) {
                                    0 -> "🥇"
                                    1 -> "🥈"
                                    2 -> "🥉"
                                    else -> "#${index + 1}"
                                }
                                val progress = sales.first.toFloat() / maxDishQuantity.toFloat()

                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 6.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text(rankBadge, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                dishName,
                                                fontWeight = FontWeight.SemiBold,
                                                fontSize = 13.sp
                                            )
                                        }

                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                "${sales.first} phần",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                color = PrimaryRed
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                formatVnd(sales.second),
                                                fontSize = 12.sp,
                                                color = Color.DarkGray
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))

                                    // Bar Progress Indicator
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(6.dp)
                                            .clip(RoundedCornerShape(3.dp))
                                            .background(Color(0xFFF0F0F0))
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth(fraction = progress.coerceIn(0.05f, 1f))
                                                .fillMaxHeight()
                                                .clip(RoundedCornerShape(3.dp))
                                                .background(
                                                    Brush.horizontalGradient(
                                                        if (index == 0) listOf(SecondaryDarkGold, SecondaryGold)
                                                        else listOf(PrimaryRed, PrimaryRed.copy(alpha = 0.7f))
                                                    )
                                                )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Payment Methods Breakdown (VietQR vs Cash)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.CreditCard, contentDescription = null, tint = PrimaryRed)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Phương Thức Thanh Toán",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        val vietQrPercent = if (totalRevenue > 0) ((vietQrRevenue / totalRevenue) * 100).toInt() else 0
                        val cashPercent = 100 - vietQrPercent

                        // Ratio Bar
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(12.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFFEEEEEE))
                        ) {
                            if (vietQrPercent > 0) {
                                Box(
                                    modifier = Modifier
                                        .weight(vietQrPercent.toFloat().coerceAtLeast(1f))
                                        .fillMaxHeight()
                                        .background(Color(0xFF1976D2))
                                )
                            }
                            if (cashPercent > 0) {
                                Box(
                                    modifier = Modifier
                                        .weight(cashPercent.toFloat().coerceAtLeast(1f))
                                        .fillMaxHeight()
                                        .background(Color(0xFF2E7D32))
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            // VietQR Pill
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .background(Color(0xFF1976D2), CircleShape)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Chuyển khoản VietQR ($vietQrPercent%): ", fontSize = 12.sp, color = Color.Gray)
                                Text(formatVnd(vietQrRevenue), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }

                            // Cash Pill
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .background(Color(0xFF2E7D32), CircleShape)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Tiền mặt ($cashPercent%): ", fontSize = 12.sp, color = Color.Gray)
                                Text(formatVnd(cashRevenue), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Customer Tiers Distribution Breakdown Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Phân Bổ 5 Bậc Khách Hàng VIP",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                            Text(
                                "${customers.size} thành viên",
                                fontSize = 12.sp,
                                color = PrimaryRed,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        val totalCustomerSpend = customers.sumOf { it.totalSpent }.coerceAtLeast(1.0)

                        CustomerTier.values().forEach { tier ->
                            val tierCustomers = customers.filter { it.tier == tier }
                            val tierSpend = tierCustomers.sumOf { it.totalSpent }
                            val spendPercent = ((tierSpend / totalCustomerSpend) * 100).toInt()

                            Column(modifier = Modifier.padding(vertical = 4.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        CustomerTierBadge(tier = tier)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            "${tierCustomers.size} khách ($spendPercent%)",
                                            fontSize = 12.sp,
                                            color = Color.Gray
                                        )
                                    }
                                    Text(
                                        formatVnd(tierSpend),
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 13.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Salary Budget Analysis Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Quỹ Lương & Nhân Lực Ca Làm",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFFE8F5E9))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    "Đang trực ca",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF2E7D32)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Tổng nhân sự toàn hệ thống:", fontSize = 13.sp, color = Color.Gray)
                            Text("${employees.size} nhân sự", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Tổng chi phí quỹ lương ước tính:", fontSize = 13.sp, color = Color.Gray)
                            Text(formatVnd(totalSalaryBudget), fontWeight = FontWeight.Bold, fontSize = 14.sp, color = PrimaryRed)
                        }
                    }
                }
            }

            // Export Báo Cáo Button
            item {
                Button(
                    onClick = { showExportDialog = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryRed)
                ) {
                    Icon(Icons.Default.Download, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Xuất Báo Cáo Tài Chính (PDF / Excel)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    // Export Financial Report Dialog
    if (showExportDialog) {
        ExportReportDialog(
            totalRevenue = totalRevenue,
            ordersCount = totalOrdersCount,
            profit = estimatedProfit,
            onDismiss = { showExportDialog = false }
        )
    }
}

@Composable
fun MetricSummaryCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accentColor: Color,
    trend: String
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
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(accentColor.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(20.dp))
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(accentColor.copy(alpha = 0.1f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        trend,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = accentColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = value,
                fontSize = 16.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFF1E1E1E),
                maxLines = 1
            )

            Text(
                text = title,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = Color.DarkGray
            )

            Spacer(modifier = Modifier.height(2.dp))

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
fun WeeklyRevenueBarChartCard(
    totalRevenue: Double,
    orderCount: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "Doanh Thu 7 Ngày Qua",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    Text(
                        "Biểu đồ cột mô phỏng theo ngày",
                        fontSize = 11.sp,
                        color = Color.Gray
                    )
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(PrimaryRed.copy(alpha = 0.1f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        "Cao điểm",
                        color = PrimaryRed,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Custom Compose Canvas Bar Chart
            val days = listOf("T2", "T3", "T4", "T5", "T6", "T7", "CN")
            val relativeWeights = listOf(0.45f, 0.6f, 0.75f, 0.55f, 0.9f, 1.0f, 0.85f)

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val canvasWidth = size.width
                    val canvasHeight = size.height - 30.dp.toPx()
                    val barWidth = (canvasWidth / days.size) * 0.45f
                    val spacing = canvasWidth / days.size

                    // Draw horizontal dotted gridline at 50% and 100%
                    drawLine(
                        color = Color(0xFFEEEEEE),
                        start = Offset(0f, canvasHeight * 0.5f),
                        end = Offset(canvasWidth, canvasHeight * 0.5f),
                        strokeWidth = 1.dp.toPx()
                    )
                    drawLine(
                        color = Color(0xFFEEEEEE),
                        start = Offset(0f, 0f),
                        end = Offset(canvasWidth, 0f),
                        strokeWidth = 1.dp.toPx()
                    )

                    // Draw bars
                    relativeWeights.forEachIndexed { i, weight ->
                        val barHeight = canvasHeight * weight
                        val x = i * spacing + (spacing - barWidth) / 2f
                        val y = canvasHeight - barHeight

                        val isPeak = i == 5 // Saturday peak
                        val barBrush = if (isPeak) {
                            Brush.verticalGradient(
                                colors = listOf(PrimaryRed, Color(0xFFFF8A80)),
                                startY = y,
                                endY = canvasHeight
                            )
                        } else {
                            Brush.verticalGradient(
                                colors = listOf(SecondaryDarkGold, SecondaryGold.copy(alpha = 0.5f)),
                                startY = y,
                                endY = canvasHeight
                            )
                        }

                        drawRoundRect(
                            brush = barBrush,
                            topLeft = Offset(x, y),
                            size = Size(barWidth, barHeight),
                            cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx())
                        )
                    }
                }

                // Days Labels Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    days.forEachIndexed { idx, day ->
                        val isPeak = idx == 5
                        Text(
                            text = day,
                            fontSize = 11.sp,
                            fontWeight = if (isPeak) FontWeight.Bold else FontWeight.Medium,
                            color = if (isPeak) PrimaryRed else Color.Gray
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ExportReportDialog(
    totalRevenue: Double,
    ordersCount: Int,
    profit: Double,
    onDismiss: () -> Unit
) {
    var isExported by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Assessment, contentDescription = null, tint = PrimaryRed)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Báo Cáo Doanh Thu POS 4.0", fontWeight = FontWeight.Bold, fontSize = 17.sp)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "GOLDEN SPOON RESTAURANT & LOUNGE",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = PrimaryRed
                )
                Text(
                    "Hệ thống đã tổng hợp báo cáo tài chính chi tiết với các chỉ số kế toán chuẩn:",
                    fontSize = 12.sp,
                    color = Color.DarkGray
                )

                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF9F9F9)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        ReportRow("Tổng doanh thu thực nhận:", formatVnd(totalRevenue))
                        ReportRow("Tổng số hoá đơn phát hành:", "$ordersCount hoá đơn")
                        ReportRow("Ước tính lợi nhuận ròng:", formatVnd(profit))
                        ReportRow("Thời gian xuất:", "Hôm nay, 19:30")
                        ReportRow("Định dạng file:", "PDF & Microsoft Excel (.xlsx)")
                    }
                }

                if (isExported) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFE8F5E9))
                            .padding(10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "✅ Đã xuất file thành công vào thư mục /Downloads!",
                            color = Color(0xFF2E7D32),
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { isExported = true },
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryRed)
            ) {
                Text(if (isExported) "Tải Lại File" else "Tải Xuống Ngay")
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
private fun ReportRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontSize = 11.sp, color = Color.Gray)
        Text(value, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}
