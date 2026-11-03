package com.example.adr_nhom_da.ui.screens.pos

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.adr_nhom_da.data.model.MenuItem
import com.example.adr_nhom_da.data.model.OrderItem
import com.example.adr_nhom_da.data.model.RestaurantTable
import com.example.adr_nhom_da.data.model.TableStatus
import com.example.adr_nhom_da.data.repository.RestaurantRepository
import com.example.adr_nhom_da.ui.components.formatVnd
import com.example.adr_nhom_da.ui.screens.menu.getCategoryMeta
import com.example.adr_nhom_da.ui.theme.PrimaryRed

@Composable
fun CustomerQrOrderDialog(
    table: RestaurantTable,
    repository: RestaurantRepository,
    onOrderCompleted: (RestaurantTable) -> Unit,
    onProceedToCheckout: (RestaurantTable) -> Unit,
    onDismiss: () -> Unit
) {
    val allMenuItems = remember { repository.getAllMenuItems().filter { it.isAvailable } }
    val categories = listOf("Tất cả", "Khai vị", "Món chính", "Đồ uống", "Tráng miệng")
    var selectedCategory by remember { mutableStateOf("Tất cả") }
    var searchQuery by remember { mutableStateOf("") }

    // Existing active orders for this table
    var currentTableOrders by remember {
        mutableStateOf(repository.getActiveOrderForTable(table.id).toMutableList())
    }

    // Newly added items in this session
    var newlyAddedCart by remember { mutableStateOf(mutableMapOf<Long, Pair<MenuItem, Int>>()) }
    var itemNotes by remember { mutableStateOf(mutableMapOf<Long, String>()) }

    var isOrderSentSuccess by remember { mutableStateOf(false) }

    val filteredItems = remember(selectedCategory, searchQuery, allMenuItems) {
        allMenuItems.filter { item ->
            val matchCategory = selectedCategory == "Tất cả" || item.category == selectedCategory
            val matchSearch = searchQuery.isBlank() || item.name.contains(searchQuery, ignoreCase = true)
            matchCategory && matchSearch
        }
    }

    val newItemsCount = newlyAddedCart.values.sumOf { it.second }
    val newItemsTotal = newlyAddedCart.values.sumOf { it.first.price * it.second }
    val existingTotal = currentTableOrders.sumOf { it.totalPrice }

    fun sendOrderToKitchen() {
        if (newlyAddedCart.isEmpty()) return

        val updatedList = currentTableOrders.toMutableList()
        newlyAddedCart.forEach { (menuId, pair) ->
            val (dish, qty) = pair
            val note = itemNotes[menuId]?.trim() ?: ""
            val existingIndex = updatedList.indexOfFirst { it.menuItemId == menuId && it.note == note }
            if (existingIndex != -1) {
                val old = updatedList[existingIndex]
                updatedList[existingIndex] = old.copy(quantity = old.quantity + qty)
            } else {
                updatedList.add(
                    OrderItem(
                        menuItemId = dish.id,
                        menuItemName = dish.name,
                        price = dish.price,
                        quantity = qty,
                        note = note
                    )
                )
            }
        }

        repository.saveActiveOrderForTable(table.id, updatedList)
        repository.updateTableStatus(table.id, TableStatus.DANG_PHUC_VU)
        currentTableOrders = updatedList
        newlyAddedCart = mutableMapOf()
        itemNotes = mutableMapOf()
        isOrderSentSuccess = true
        onOrderCompleted(table)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.92f)
                .padding(vertical = 8.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(PrimaryRed)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.QrCodeScanner, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Gọi Món Tại: ${table.name}",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }
                        Text(
                            text = "${table.floor} • Khách tự gọi món qua mã QR",
                            color = Color.White.copy(alpha = 0.85f),
                            fontSize = 11.sp
                        )
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Đóng", tint = Color.White)
                    }
                }

                // Success confirmation banner
                AnimatedVisibility(visible = isOrderSentSuccess) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)),
                        shape = RoundedCornerShape(0.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF2E7D32))
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text("GỬI MÓN THÀNH CÔNG!", fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32), fontSize = 13.sp)
                                    Text("Bếp đã tiếp nhận đơn của ${table.name}", fontSize = 11.sp, color = Color.DarkGray)
                                }
                            }
                            Button(
                                onClick = {
                                    onDismiss()
                                    onProceedToCheckout(table)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text("Xem Hóa Đơn", fontSize = 11.sp)
                            }
                        }
                    }
                }

                // Search & Category Filters
                Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Tìm món ăn, đồ uống...") },
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

                    // Categories scroll
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(categories) { cat ->
                            FilterChip(
                                selected = selectedCategory == cat,
                                onClick = { selectedCategory = cat },
                                label = { Text(cat, fontSize = 12.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = PrimaryRed.copy(alpha = 0.15f),
                                    selectedLabelColor = PrimaryRed
                                )
                            )
                        }
                    }
                }

                HorizontalDivider(color = Color(0xFFEEEEEE))

                // Menu Items List
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 14.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (currentTableOrders.isNotEmpty()) {
                        item {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 4.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F8E9)),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.CheckCircle,
                                                contentDescription = null,
                                                tint = Color(0xFF2E7D32),
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                "Món đang phục vụ tại bàn (${currentTableOrders.size}):",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp,
                                                color = Color(0xFF2E7D32)
                                            )
                                        }
                                        Text(
                                            formatVnd(existingTotal),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = Color(0xFF2E7D32)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    currentTableOrders.forEach { itm ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 1.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                "• ${itm.menuItemName} x${itm.quantity}${if (itm.note.isNotBlank()) " (${itm.note})" else ""}",
                                                fontSize = 11.sp,
                                                color = Color.DarkGray
                                            )
                                            Text(formatVnd(itm.totalPrice), fontSize = 11.sp, color = Color.DarkGray)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    items(filteredItems) { dish ->
                        val currentQtyInNewCart = newlyAddedCart[dish.id]?.second ?: 0
                        val (dishEmoji, _, dishBgColor) = getCategoryMeta(dish.category)

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .shadow(2.dp, RoundedCornerShape(14.dp)),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(44.dp)
                                            .background(dishBgColor, CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(dishEmoji, fontSize = 22.sp)
                                    }

                                    Spacer(modifier = Modifier.width(10.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(dish.name, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color(0xFF1E1E2C))
                                        Text(dish.category, fontSize = 11.sp, color = Color.Gray)
                                        if (dish.description.isNotBlank()) {
                                            Text(dish.description, fontSize = 11.sp, color = Color.DarkGray, maxLines = 1)
                                        }
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            formatVnd(dish.price),
                                            fontWeight = FontWeight.Black,
                                            color = PrimaryRed,
                                            fontSize = 14.sp
                                        )
                                    }

                                    // Add / Remove controls
                                    if (currentQtyInNewCart == 0) {
                                        Button(
                                            onClick = {
                                                val copy = newlyAddedCart.toMutableMap()
                                                copy[dish.id] = Pair(dish, 1)
                                                newlyAddedCart = copy
                                                isOrderSentSuccess = false
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryRed),
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                        ) {
                                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Thêm", fontSize = 12.sp)
                                        }
                                    } else {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier
                                                .background(Color(0xFFF5F5F5), RoundedCornerShape(8.dp))
                                                .padding(horizontal = 4.dp, vertical = 2.dp)
                                        ) {
                                            IconButton(
                                                onClick = {
                                                    val copy = newlyAddedCart.toMutableMap()
                                                    if (currentQtyInNewCart > 1) {
                                                        copy[dish.id] = Pair(dish, currentQtyInNewCart - 1)
                                                    } else {
                                                        copy.remove(dish.id)
                                                    }
                                                    newlyAddedCart = copy
                                                },
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Icon(Icons.Default.Remove, contentDescription = "Trừ", modifier = Modifier.size(16.dp))
                                            }

                                            Text(
                                                text = "$currentQtyInNewCart",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp,
                                                modifier = Modifier.padding(horizontal = 6.dp)
                                            )

                                            IconButton(
                                                onClick = {
                                                    val copy = newlyAddedCart.toMutableMap()
                                                    copy[dish.id] = Pair(dish, currentQtyInNewCart + 1)
                                                    newlyAddedCart = copy
                                                },
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Icon(Icons.Default.Add, contentDescription = "Cộng", modifier = Modifier.size(16.dp), tint = PrimaryRed)
                                            }
                                        }
                                    }
                                }

                                // Quick Note Input when item is added
                                if (currentQtyInNewCart > 0) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    OutlinedTextField(
                                        value = itemNotes[dish.id] ?: "",
                                        onValueChange = {
                                            val notesCopy = itemNotes.toMutableMap()
                                            notesCopy[dish.id] = it
                                            itemNotes = notesCopy
                                        },
                                        placeholder = { Text("Ghi chú món (VD: Không hành, ít cay...)", fontSize = 11.sp) },
                                        modifier = Modifier.fillMaxWidth(),
                                        singleLine = true,
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Bottom Cart Summary & Submit Button
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shadowElevation = 8.dp,
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = if (newItemsCount > 0) "Món mới chọn: $newItemsCount món" else "Đang có: ${currentTableOrders.size} món tại bàn",
                                    fontSize = 12.sp,
                                    color = Color.Gray
                                )
                                Text(
                                    text = "Tổng bàn: ${formatVnd(existingTotal + newItemsTotal)}",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PrimaryRed
                                )
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                if (currentTableOrders.isNotEmpty()) {
                                    OutlinedButton(
                                        onClick = {
                                            onDismiss()
                                            onProceedToCheckout(table)
                                        },
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Text("Hóa Đơn", fontSize = 12.sp)
                                    }
                                }

                                Button(
                                    onClick = { sendOrderToKitchen() },
                                    enabled = newItemsCount > 0,
                                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryRed),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Gửi Món (${formatVnd(newItemsTotal)})", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
