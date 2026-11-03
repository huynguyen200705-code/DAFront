package com.example.adr_nhom_da.ui.screens.menu

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.adr_nhom_da.data.model.MenuItem
import com.example.adr_nhom_da.data.repository.RestaurantRepository
import com.example.adr_nhom_da.ui.components.ConfirmDeleteDialog
import com.example.adr_nhom_da.ui.components.formatVnd
import com.example.adr_nhom_da.ui.theme.PrimaryRed
import com.example.adr_nhom_da.ui.theme.SecondaryDarkGold
import com.example.adr_nhom_da.ui.theme.SecondaryGold

fun getCategoryMeta(categoryName: String): Triple<String, Color, Color> {
    return when {
        categoryName.contains("Khai vị", ignoreCase = true) -> Triple("🥗", Color(0xFF2E7D32), Color(0xFFE8F5E9))
        categoryName.contains("Món chính", ignoreCase = true) -> Triple("🥩", PrimaryRed, Color(0xFFFFEBEE))
        categoryName.contains("Đồ uống", ignoreCase = true) -> Triple("🍹", Color(0xFF0288D1), Color(0xFFE1F5FE))
        categoryName.contains("Tráng miệng", ignoreCase = true) -> Triple("🍨", Color(0xFFF57C00), Color(0xFFFFF3E0))
        else -> Triple("🍲", Color(0xFF5E35B1), Color(0xFFEDE7F6))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MenuManagementScreen(
    repository: RestaurantRepository,
    isAdmin: Boolean = true
) {
    var menuList by remember { mutableStateOf(repository.getAllMenuItems()) }
    var selectedCategory by remember { mutableStateOf("Tất cả") }
    var availabilityFilter by remember { mutableStateOf("ALL") } // ALL, AVAILABLE, OUT_OF_STOCK
    var searchQuery by remember { mutableStateOf("") }
    val categories = listOf("Tất cả", "Khai vị", "Món chính", "Đồ uống", "Tráng miệng")

    var showAddEditDialog by remember { mutableStateOf(false) }
    var itemToEdit by remember { mutableStateOf<MenuItem?>(null) }
    var itemToDelete by remember { mutableStateOf<MenuItem?>(null) }

    fun refreshData() {
        menuList = repository.getAllMenuItems()
    }

    val availableCount = remember(menuList) { menuList.count { it.isAvailable } }
    val outOfStockCount = remember(menuList) { menuList.count { !it.isAvailable } }
    val averagePrice = remember(menuList) {
        if (menuList.isNotEmpty()) menuList.map { it.price }.average() else 0.0
    }

    val filteredList = remember(selectedCategory, availabilityFilter, searchQuery, menuList) {
        menuList.filter { item ->
            val matchCat = selectedCategory == "Tất cả" || item.category.equals(selectedCategory, ignoreCase = true)
            val matchAvailability = when (availabilityFilter) {
                "AVAILABLE" -> item.isAvailable
                "OUT_OF_STOCK" -> !item.isAvailable
                else -> true
            }
            val matchSearch = searchQuery.isBlank() ||
                    item.name.contains(searchQuery, ignoreCase = true) ||
                    item.description.contains(searchQuery, ignoreCase = true)
            matchCat && matchAvailability && matchSearch
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Quản Lý Thực Đơn & Món Ăn", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text(
                            "Golden Spoon Restaurant Culinary Menu",
                            fontSize = 11.sp,
                            color = Color.Gray
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { refreshData() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Tải lại", tint = PrimaryRed)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        floatingActionButton = {
            if (isAdmin) {
                ExtendedFloatingActionButton(
                    onClick = {
                        itemToEdit = null
                        showAddEditDialog = true
                    },
                    containerColor = PrimaryRed,
                    contentColor = Color.White,
                    icon = { Icon(Icons.Default.RestaurantMenu, contentDescription = null) },
                    text = { Text("Thêm Món Mới", fontWeight = FontWeight.Bold) }
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp, vertical = 6.dp)
        ) {
            // 4 Top Menu Metric Cards
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MenuKpiCard(
                    modifier = Modifier.weight(1f),
                    title = "Tổng Món",
                    value = "${menuList.size} món",
                    subtitle = "4 Nhóm món",
                    accentColor = PrimaryRed,
                    icon = Icons.AutoMirrored.Filled.MenuBook
                )
                MenuKpiCard(
                    modifier = Modifier.weight(1f),
                    title = "Đang Phục Vụ",
                    value = "$availableCount món",
                    subtitle = "Sẵn sàng chế biến",
                    accentColor = Color(0xFF2E7D32),
                    icon = Icons.Default.CheckCircle
                )
                MenuKpiCard(
                    modifier = Modifier.weight(1f),
                    title = "Tạm Hết",
                    value = "$outOfStockCount món",
                    subtitle = "Chờ nhập liệu",
                    accentColor = Color(0xFFE65100),
                    icon = Icons.Default.Block
                )
                MenuKpiCard(
                    modifier = Modifier.weight(1f),
                    title = "Giá TB",
                    value = formatVnd(averagePrice),
                    subtitle = "Mức giá trung bình",
                    accentColor = SecondaryDarkGold,
                    icon = Icons.Default.MonetizationOn
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Tìm theo tên món ăn, đồ uống, hương vị...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray) },
                trailingIcon = {
                    if (searchQuery.isNotBlank()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Xóa")
                        }
                    }
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Category Filter Chips
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(categories) { cat ->
                    val isSelected = selectedCategory == cat
                    val count = if (cat == "Tất cả") menuList.size else menuList.count { it.category.equals(cat, ignoreCase = true) }
                    val (emoji, accentColor, _) = if (cat == "Tất cả") Triple("🌟", PrimaryRed, Color.White) else getCategoryMeta(cat)

                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedCategory = cat },
                        label = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(emoji, fontSize = 12.sp)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "$cat ($count)",
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 12.sp
                                )
                            }
                        },
                        shape = RoundedCornerShape(10.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = PrimaryRed.copy(alpha = 0.12f),
                            selectedLabelColor = PrimaryRed
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = isSelected,
                            borderColor = if (isSelected) PrimaryRed else Color(0xFFE0E0E0)
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Availability Quick Filter (Tất cả / Đang phục vụ / Tạm hết)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilterChip(
                    selected = availabilityFilter == "ALL",
                    onClick = { availabilityFilter = "ALL" },
                    label = { Text("Tất cả trạng thái", fontSize = 11.sp) }
                )
                FilterChip(
                    selected = availabilityFilter == "AVAILABLE",
                    onClick = { availabilityFilter = "AVAILABLE" },
                    label = { Text("🟢 Còn phục vụ ($availableCount)", fontSize = 11.sp) }
                )
                FilterChip(
                    selected = availabilityFilter == "OUT_OF_STOCK",
                    onClick = { availabilityFilter = "OUT_OF_STOCK" },
                    label = { Text("⚪ Tạm hết ($outOfStockCount)", fontSize = 11.sp) }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Menu Items List
            if (filteredList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("🍽️", fontSize = 42.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Không tìm thấy món ăn nào phù hợp",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = Color.DarkGray
                        )
                        Text(
                            "Vui lòng thử tìm kiếm với từ khóa hoặc danh mục khác",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredList) { item ->
                        ModernMenuItemCard(
                            item = item,
                            isAdmin = isAdmin,
                            onToggleAvailable = { isAvailable ->
                                repository.updateMenuItem(item.copy(isAvailable = isAvailable))
                                refreshData()
                            },
                            onEdit = {
                                itemToEdit = item
                                showAddEditDialog = true
                            },
                            onDelete = {
                                itemToDelete = item
                            }
                        )
                    }

                    item {
                        Spacer(modifier = Modifier.height(72.dp))
                    }
                }
            }
        }
    }

    // Modal Add / Edit Dish Dialog
    if (showAddEditDialog) {
        AddEditMenuDialog(
            item = itemToEdit,
            onDismiss = { showAddEditDialog = false },
            onSave = { updatedItem ->
                if (itemToEdit == null) {
                    repository.addMenuItem(updatedItem)
                } else {
                    repository.updateMenuItem(updatedItem)
                }
                refreshData()
                showAddEditDialog = false
            }
        )
    }

    // Confirm Delete Dialog
    itemToDelete?.let { item ->
        ConfirmDeleteDialog(
            title = "Xác Nhận Xoá Món Ăn",
            message = "Bạn có chắc chắn muốn xóa món '${item.name}' khỏi thực đơn của nhà hàng không?",
            onConfirm = {
                repository.deleteMenuItem(item.id)
                refreshData()
                itemToDelete = null
            },
            onDismiss = { itemToDelete = null }
        )
    }
}

@Composable
fun MenuKpiCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    subtitle: String,
    accentColor: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier.size(18.dp)
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = value,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1E1E1E),
                maxLines = 1
            )

            Text(
                text = title,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                color = Color.DarkGray,
                maxLines = 1
            )
        }
    }
}

@Composable
fun ModernMenuItemCard(
    item: MenuItem,
    isAdmin: Boolean,
    onToggleAvailable: (Boolean) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val (emoji, accentColor, bgColor) = getCategoryMeta(item.category)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp),
        border = if (!item.isAvailable) BorderStroke(1.dp, Color(0xFFE0E0E0)) else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Category Emoji Box
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(bgColor)
                    .border(1.dp, accentColor.copy(alpha = 0.25f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(emoji, fontSize = 26.sp)
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Dish Info
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = item.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = if (item.isAvailable) Color(0xFF1E1E2C) else Color.Gray
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(bgColor)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = item.category,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = accentColor
                        )
                    }
                }

                if (item.description.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        item.description,
                        fontSize = 11.sp,
                        color = Color.Gray,
                        maxLines = 1
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = formatVnd(item.price),
                        fontWeight = FontWeight.Black,
                        color = if (item.isAvailable) PrimaryRed else Color.Gray,
                        fontSize = 14.sp
                    )

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(
                                if (item.isAvailable) Color(0xFFE8F5E9) else Color(0xFFF5F5F5)
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = if (item.isAvailable) "● Còn phục vụ" else "○ Tạm hết món",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (item.isAvailable) Color(0xFF2E7D32) else Color.Gray
                        )
                    }
                }
            }

            // Actions & Toggle
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.Center
            ) {
                Switch(
                    checked = item.isAvailable,
                    onCheckedChange = onToggleAvailable
                )

                if (isAdmin) {
                    Row {
                        IconButton(onClick = onEdit, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Default.Edit, contentDescription = "Sửa", tint = Color.Gray, modifier = Modifier.size(16.dp))
                        }
                        IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Default.Delete, contentDescription = "Xóa", tint = PrimaryRed, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }
    }
}

/**
 * Add / Edit Culinary Dish Dialog
 */
@Composable
fun AddEditMenuDialog(
    item: MenuItem?,
    onDismiss: () -> Unit,
    onSave: (MenuItem) -> Unit
) {
    var name by remember { mutableStateOf(item?.name ?: "") }
    var category by remember { mutableStateOf(item?.category ?: "Món chính") }
    var priceText by remember { mutableStateOf(item?.price?.toInt()?.toString() ?: "75000") }
    var desc by remember { mutableStateOf(item?.description ?: "") }
    var isAvailable by remember { mutableStateOf(item?.isAvailable ?: true) }

    val presetCategories = listOf("Khai vị", "Món chính", "Đồ uống", "Tráng miệng")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("👨‍🍳", fontSize = 22.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    if (item == null) "Thêm Món Ăn Mới" else "Cập Nhật Món Ăn",
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp
                )
            }
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
                    label = { Text("Tên món ăn") },
                    leadingIcon = { Icon(Icons.Default.Restaurant, contentDescription = null, tint = PrimaryRed) },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Danh mục ẩm thực:", fontSize = 12.sp, fontWeight = FontWeight.Bold)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    presetCategories.forEach { cat ->
                        val isSelected = category.equals(cat, ignoreCase = true)
                        val (emoji, _, _) = getCategoryMeta(cat)

                        FilterChip(
                            selected = isSelected,
                            onClick = { category = cat },
                            label = { Text("$emoji $cat", fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = PrimaryRed.copy(alpha = 0.12f),
                                selectedLabelColor = PrimaryRed
                            )
                        )
                    }
                }

                OutlinedTextField(
                    value = priceText,
                    onValueChange = { priceText = it },
                    label = { Text("Giá niêm yết (VNĐ)") },
                    leadingIcon = { Icon(Icons.Default.MonetizationOn, contentDescription = null, tint = SecondaryDarkGold) },
                    trailingIcon = { Text("đ", fontWeight = FontWeight.Bold, modifier = Modifier.padding(end = 12.dp)) },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                // Quick price suggestion chips
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    listOf(45000, 75000, 120000, 250000).forEach { p ->
                        AssistChip(
                            onClick = { priceText = p.toString() },
                            label = { Text(formatVnd(p.toDouble()), fontSize = 10.sp) }
                        )
                    }
                }

                OutlinedTextField(
                    value = desc,
                    onValueChange = { desc = it },
                    label = { Text("Mô tả hương vị & nguyên liệu") },
                    placeholder = { Text("VD: Tôm tươi, mực giòn, sốt đậm đà...") },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Sẵn sàng phục vụ ngay:", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    Switch(
                        checked = isAvailable,
                        onCheckedChange = { isAvailable = it }
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        val priceVal = priceText.toDoubleOrNull() ?: 50000.0
                        val newItem = MenuItem(
                            id = item?.id ?: 0,
                            name = name,
                            category = category,
                            price = priceVal,
                            description = desc,
                            isAvailable = isAvailable
                        )
                        onSave(newItem)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryRed),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(if (item == null) "Thêm Món" else "Lưu Thay Đổi")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Hủy") }
        }
    )
}
