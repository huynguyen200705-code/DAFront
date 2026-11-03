package com.example.adr_nhom_da.ui.screens.customer

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.adr_nhom_da.data.model.Customer
import com.example.adr_nhom_da.data.model.CustomerTier
import com.example.adr_nhom_da.data.repository.RestaurantRepository
import com.example.adr_nhom_da.ui.components.ConfirmDeleteDialog
import com.example.adr_nhom_da.ui.components.CustomerTierBadge
import com.example.adr_nhom_da.ui.components.formatVnd
import com.example.adr_nhom_da.ui.theme.PrimaryRed

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerManagementScreen(
    repository: RestaurantRepository
) {
    var customers by remember { mutableStateOf(repository.getAllCustomers()) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedTierFilter by remember { mutableStateOf<CustomerTier?>(null) }

    var showAddEditDialog by remember { mutableStateOf(false) }
    var customerToEdit by remember { mutableStateOf<Customer?>(null) }
    var customerToDelete by remember { mutableStateOf<Customer?>(null) }

    fun refreshData() {
        customers = repository.getAllCustomers()
    }

    val syncVersion by repository.syncVersion.collectAsState()
    LaunchedEffect(syncVersion) {
        refreshData()
    }

    val filteredList = customers.filter { cust ->
        val matchesSearch = cust.name.contains(searchQuery, ignoreCase = true) || cust.phone.contains(searchQuery)
        val matchesTier = selectedTierFilter == null || cust.tier == selectedTierFilter
        matchesSearch && matchesTier
    }

    val vipCount = customers.count { it.tier == CustomerTier.VANG || it.tier == CustomerTier.BACH_KIM || it.tier == CustomerTier.KIM_CUONG }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Quản Lý 5 Bậc Khách Hàng", fontWeight = FontWeight.Black, fontSize = 18.sp)
                        Text("${customers.size} thành viên • $vipCount khách VIP", fontSize = 11.sp, color = Color.Gray)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    customerToEdit = null
                    showAddEditDialog = true
                },
                containerColor = PrimaryRed,
                elevation = FloatingActionButtonDefaults.elevation(6.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Thêm khách hàng", tint = Color.White)
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Tìm theo tên hoặc số điện thoại...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray) },
                trailingIcon = {
                    if (searchQuery.isNotBlank()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Xóa")
                        }
                    }
                },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                shape = RoundedCornerShape(12.dp)
            )

            // Modern Tier Filter Chips
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                item {
                    val isAll = selectedTierFilter == null
                    FilterChip(
                        selected = isAll,
                        onClick = { selectedTierFilter = null },
                        label = {
                            Text(
                                text = "Tất cả (${customers.size})",
                                fontWeight = if (isAll) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 12.sp
                            )
                        },
                        shape = RoundedCornerShape(20.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = PrimaryRed,
                            selectedLabelColor = Color.White,
                            containerColor = Color.White
                        )
                    )
                }

                items(CustomerTier.values()) { tier ->
                    val isSelected = selectedTierFilter == tier
                    val count = customers.count { it.tier == tier }

                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedTierFilter = tier },
                        label = {
                            Text(
                                text = "${tier.displayName} ($count)",
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 12.sp
                            )
                        },
                        shape = RoundedCornerShape(20.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = PrimaryRed,
                            selectedLabelColor = Color.White,
                            containerColor = Color.White
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Customer List
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredList) { cust ->
                    CustomerCard(
                        customer = cust,
                        onEdit = {
                            customerToEdit = cust
                            showAddEditDialog = true
                        },
                        onDelete = {
                            customerToDelete = cust
                        }
                    )
                }
            }
        }
    }

    // Add / Edit Dialog
    if (showAddEditDialog) {
        var name by remember { mutableStateOf(customerToEdit?.name ?: "") }
        var phone by remember { mutableStateOf(customerToEdit?.phone ?: "") }
        var totalSpentText by remember { mutableStateOf(customerToEdit?.totalSpent?.toInt()?.toString() ?: "0") }
        var pointsText by remember { mutableStateOf(customerToEdit?.loyaltyPoints?.toString() ?: "0") }

        AlertDialog(
            onDismissRequest = { showAddEditDialog = false },
            title = { Text(if (customerToEdit == null) "Thêm Khách Hàng Mới" else "Cập Nhật Khách Hàng", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Họ và tên") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("Số điện thoại") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = totalSpentText,
                        onValueChange = { totalSpentText = it },
                        label = { Text("Tổng chi tiêu tích lũy (VNĐ)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = pointsText,
                        onValueChange = { pointsText = it },
                        label = { Text("Điểm tích lũy (Points)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (name.isNotBlank()) {
                            val spent = totalSpentText.toDoubleOrNull() ?: 0.0
                            val points = pointsText.toIntOrNull() ?: 0

                            val newCust = Customer(
                                id = customerToEdit?.id ?: 0,
                                name = name,
                                phone = phone,
                                email = customerToEdit?.email ?: "",
                                totalSpent = spent,
                                loyaltyPoints = points
                            )
                            if (customerToEdit == null) {
                                repository.addCustomer(newCust)
                            } else {
                                repository.updateCustomer(newCust)
                            }
                            refreshData()
                            showAddEditDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryRed)
                ) {
                    Text("Lưu")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddEditDialog = false }) { Text("Hủy") }
            }
        )
    }

    // Confirm Delete Dialog
    customerToDelete?.let { cust ->
        ConfirmDeleteDialog(
            message = "Bạn có chắc chắn muốn xóa khách hàng '${cust.name}'?",
            onConfirm = {
                repository.deleteCustomer(cust.id)
                refreshData()
                customerToDelete = null
            },
            onDismiss = { customerToDelete = null }
        )
    }
}

@Composable
fun CustomerCard(
    customer: Customer,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val nextTier = CustomerTier.getNextTier(customer.tier)
    val progress = if (nextTier != null) {
        (customer.totalSpent / nextTier.minSpend).coerceIn(0.0, 1.0).toFloat()
    } else 1.0f

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(2.dp, RoundedCornerShape(14.dp)),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Avatar circle
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(PrimaryRed.copy(alpha = 0.08f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = customer.name.take(1).uppercase(),
                        fontWeight = FontWeight.Black,
                        fontSize = 18.sp,
                        color = PrimaryRed
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(customer.name, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color(0xFF1E1E2C))
                        Spacer(modifier = Modifier.width(8.dp))
                        CustomerTierBadge(tier = customer.tier)
                    }
                    Text("SĐT: ${customer.phone} • Tích lũy: ${customer.loyaltyPoints} điểm", fontSize = 11.sp, color = Color.Gray)
                    Text("Chi tiêu: ${formatVnd(customer.totalSpent)}", fontWeight = FontWeight.Bold, color = PrimaryRed, fontSize = 13.sp)
                }

                Row {
                    IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Edit, contentDescription = "Sửa", tint = Color.Gray, modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = "Xóa", tint = PrimaryRed, modifier = Modifier.size(18.dp))
                    }
                }
            }

            // Tier Progress Bar
            if (nextTier != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFF9F9FB), RoundedCornerShape(8.dp))
                        .padding(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Tiến độ thăng hạng ${nextTier.displayName}:", fontSize = 11.sp, color = Color.Gray)
                        Text("${formatVnd(customer.totalSpent)} / ${formatVnd(nextTier.minSpend)}", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color.DarkGray)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = PrimaryRed,
                        trackColor = Color(0xFFE0E0E0)
                    )
                }
            }
        }
    }
}
