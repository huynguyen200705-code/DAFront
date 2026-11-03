package com.example.adr_nhom_da.ui.screens.pos

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
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
import com.example.adr_nhom_da.data.model.*
import com.example.adr_nhom_da.data.repository.RestaurantRepository
import com.example.adr_nhom_da.ui.components.CustomerTierBadge
import com.example.adr_nhom_da.ui.components.TableStatusBadge
import com.example.adr_nhom_da.ui.components.formatVnd
import com.example.adr_nhom_da.ui.screens.employee.SimulatedQrMatrixCanvas
import com.example.adr_nhom_da.ui.theme.PrimaryRed
import com.example.adr_nhom_da.ui.theme.SecondaryDarkGold
import com.example.adr_nhom_da.ui.theme.SecondaryGold

enum class PosTab(val title: String) {
    MENU("Thực Đơn Gọi Món"),
    BILL("Hoá Đơn & Thanh Toán")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderCheckoutScreen(
    repository: RestaurantRepository,
    currentUser: User,
    initialTableId: Long? = null
) {
    val tables = remember { repository.getAllTables() }
    val allMenuItems = remember { repository.getAllMenuItems().filter { it.isAvailable } }
    val customers = remember { repository.getAllCustomers() }

    var selectedTable by remember {
        mutableStateOf(tables.find { it.id == initialTableId } ?: tables.firstOrNull())
    }
    var selectedCustomer by remember { mutableStateOf<Customer?>(null) }
    var orderItems by remember { mutableStateOf(mutableListOf<OrderItem>()) }

    val syncVersion by repository.syncVersion.collectAsState()
    LaunchedEffect(syncVersion, selectedTable) {
        selectedTable?.let { table ->
            orderItems = repository.getActiveOrderForTable(table.id).toMutableList()
        }
    }
    var paymentMethod by remember { mutableStateOf("VietQR") }
    var cashGivenInput by remember { mutableStateOf("") }

    var selectedPosTab by remember { mutableStateOf(PosTab.MENU) }

    // Dialog states
    var showTablePickerSheet by remember { mutableStateOf(false) }
    var showCustomerPickerSheet by remember { mutableStateOf(false) }
    var showQrScannerDialog by remember { mutableStateOf(false) }
    var showCustomerMenuDialog by remember { mutableStateOf(false) }
    var completedOrderForBill by remember { mutableStateOf<Order?>(null) }
    var saveSuccessMessage by remember { mutableStateOf<String?>(null) }

    // Menu search & filter
    var menuSearchQuery by remember { mutableStateOf("") }
    val categories = listOf("Tất cả", "Khai vị", "Món chính", "Đồ uống", "Tráng miệng")
    var selectedCategory by remember { mutableStateOf("Tất cả") }

    // Item note editing
    var itemToEditNote by remember { mutableStateOf<OrderItem?>(null) }
    var noteInputText by remember { mutableStateOf("") }

    LaunchedEffect(initialTableId) {
        if (initialTableId != null) {
            val matchedTable = tables.find { it.id == initialTableId }
            if (matchedTable != null) {
                selectedTable = matchedTable
            }
        }
    }

    LaunchedEffect(selectedTable?.id) {
        selectedTable?.let { table ->
            orderItems = repository.getActiveOrderForTable(table.id).toMutableList()
        }
    }

    val filteredMenuItems = remember(selectedCategory, menuSearchQuery, allMenuItems) {
        allMenuItems.filter { item ->
            val matchCategory = selectedCategory == "Tất cả" || item.category.equals(selectedCategory, ignoreCase = true)
            val matchSearch = menuSearchQuery.isBlank() || item.name.contains(menuSearchQuery, ignoreCase = true)
            matchCategory && matchSearch
        }
    }

    val totalItemsCount = orderItems.sumOf { it.quantity }
    val subtotal = orderItems.sumOf { it.totalPrice }
    val discountPercent = selectedCustomer?.discountPercent ?: 0.0
    val discountAmount = subtotal * (discountPercent / 100.0)
    val finalTotal = subtotal - discountAmount

    val cashGiven = cashGivenInput.toDoubleOrNull() ?: 0.0
    val changeAmount = if (cashGiven >= finalTotal) cashGiven - finalTotal else 0.0

    fun addDishToOrder(dish: MenuItem) {
        val currentList = orderItems.toMutableList()
        val existingIndex = currentList.indexOfFirst { it.menuItemId == dish.id && it.note.isBlank() }
        if (existingIndex != -1) {
            val existing = currentList[existingIndex]
            currentList[existingIndex] = existing.copy(quantity = existing.quantity + 1)
        } else {
            currentList.add(
                OrderItem(
                    menuItemId = dish.id,
                    menuItemName = dish.name,
                    price = dish.price,
                    quantity = 1,
                    note = ""
                )
            )
        }
        orderItems = currentList
        selectedTable?.let { table ->
            repository.saveActiveOrderForTable(table.id, currentList)
        }
    }

    fun removeDishFromOrder(dishId: Long) {
        val currentList = orderItems.toMutableList()
        val existingIndex = currentList.indexOfFirst { it.menuItemId == dishId }
        if (existingIndex != -1) {
            val existing = currentList[existingIndex]
            if (existing.quantity > 1) {
                currentList[existingIndex] = existing.copy(quantity = existing.quantity - 1)
            } else {
                currentList.removeAt(existingIndex)
            }
            orderItems = currentList
            selectedTable?.let { table ->
                repository.saveActiveOrderForTable(table.id, currentList)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "POS & Gọi Món Tại Bàn",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Text(
                            "Golden Spoon Restaurant & Lounge 4.0",
                            fontSize = 11.sp,
                            color = Color.Gray
                        )
                    }
                },
                actions = {
                    // Quick QR Table Scanner
                    IconButton(onClick = { showQrScannerDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.QrCodeScanner,
                            contentDescription = "Quét QR Bàn",
                            tint = PrimaryRed
                        )
                    }
                    // Customer Digital Menu Preview
                    selectedTable?.let {
                        IconButton(onClick = { showCustomerMenuDialog = true }) {
                            Icon(
                                imageVector = Icons.Default.RestaurantMenu,
                                contentDescription = "Menu Bàn",
                                tint = Color(0xFF2E7D32)
                            )
                        }
                    }
                    // Refresh data
                    IconButton(onClick = {
                        selectedTable?.let { table ->
                            orderItems = repository.getActiveOrderForTable(table.id).toMutableList()
                        }
                    }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Làm mới", tint = Color.Gray)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Compact Table & Customer Selection Banner
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF9F9FB)),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEEEEEE))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Table Selection Pill
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color.White)
                            .border(1.dp, PrimaryRed.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                            .clickable { showTablePickerSheet = true }
                            .padding(horizontal = 10.dp, vertical = 8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.TableBar, contentDescription = null, tint = PrimaryRed, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        selectedTable?.name ?: "Chọn Bàn",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                }
                                Text(
                                    "${selectedTable?.floor ?: ""} • ${selectedTable?.seats ?: 4} chỗ",
                                    fontSize = 11.sp,
                                    color = Color.Gray
                                )
                            }
                            selectedTable?.let { TableStatusBadge(status = it.status) }
                        }
                    }

                    // Customer VIP Selection Pill
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color.White)
                            .border(1.dp, SecondaryDarkGold.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                            .clickable { showCustomerPickerSheet = true }
                            .padding(horizontal = 10.dp, vertical = 8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Diamond, contentDescription = null, tint = SecondaryDarkGold, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        selectedCustomer?.name ?: "Khách vãng lai",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        maxLines = 1
                                    )
                                }
                                Text(
                                    if (discountPercent > 0) "Giảm giá VIP: -${discountPercent.toInt()}%" else "Không áp dụng giảm giá",
                                    fontSize = 11.sp,
                                    color = if (discountPercent > 0) PrimaryRed else Color.Gray
                                )
                            }
                            selectedCustomer?.let { CustomerTierBadge(tier = it.tier) }
                        }
                    }
                }
            }

            // Segmented POS Tabs: [ Thực Đơn Gọi Món ] vs [ Hoá Đơn & Thanh Toán (N) ]
            TabRow(
                selectedTabIndex = selectedPosTab.ordinal,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = PrimaryRed
            ) {
                PosTab.values().forEach { tab ->
                    val isSelected = selectedPosTab == tab
                    Tab(
                        selected = isSelected,
                        onClick = { selectedPosTab = tab },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (tab == PosTab.MENU) Icons.Default.RestaurantMenu else Icons.AutoMirrored.Filled.ReceiptLong,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (tab == PosTab.BILL && totalItemsCount > 0) {
                                        "${tab.title} ($totalItemsCount)"
                                    } else {
                                        tab.title
                                    },
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    )
                }
            }

            // Tab Content
            when (selectedPosTab) {
                PosTab.MENU -> {
                    // Menu & 1-Tap Calling Tab
                    Box(modifier = Modifier.fillMaxSize()) {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            item {
                                Spacer(modifier = Modifier.height(6.dp))
                                // Search Bar
                                OutlinedTextField(
                                    value = menuSearchQuery,
                                    onValueChange = { menuSearchQuery = it },
                                    placeholder = { Text("Tìm món khai vị, lẩu, món chính, đồ uống...") },
                                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray) },
                                    trailingIcon = {
                                        if (menuSearchQuery.isNotBlank()) {
                                            IconButton(onClick = { menuSearchQuery = "" }) {
                                                Icon(Icons.Default.Clear, contentDescription = "Xóa")
                                            }
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp)
                                )
                            }

                            // Category Filter Chips
                            item {
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    items(categories) { cat ->
                                        val isSelected = selectedCategory == cat
                                        val icon = when (cat) {
                                            "Khai vị" -> "🥗 "
                                            "Món chính" -> "🥩 "
                                            "Đồ uống" -> "🍹 "
                                            "Tráng miệng" -> "🍨 "
                                            else -> "🌟 "
                                        }
                                        FilterChip(
                                            selected = isSelected,
                                            onClick = { selectedCategory = cat },
                                            label = { Text("$icon$cat", fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium) },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = PrimaryRed.copy(alpha = 0.12f),
                                                selectedLabelColor = PrimaryRed
                                            )
                                        )
                                    }
                                }
                            }

                            // Menu Dish Cards
                            items(filteredMenuItems) { dish ->
                                val dishQtyInOrder = orderItems.filter { it.menuItemId == dish.id }.sumOf { it.quantity }

                                PosDishCard(
                                    dish = dish,
                                    currentQuantity = dishQtyInOrder,
                                    onAdd = { addDishToOrder(dish) },
                                    onRemove = { removeDishFromOrder(dish.id) }
                                )
                            }

                            // Bottom space for floating cart
                            item {
                                Spacer(modifier = Modifier.height(80.dp))
                            }
                        }

                        // Floating Sticky Order Summary Bar
                        if (totalItemsCount > 0) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .fillMaxWidth()
                                    .background(
                                        Brush.verticalGradient(
                                            listOf(Color.Transparent, Color.White.copy(alpha = 0.95f), Color.White)
                                        )
                                    )
                                    .padding(16.dp)
                            ) {
                                Button(
                                    onClick = { selectedPosTab = PosTab.BILL },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(52.dp),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryRed),
                                    elevation = ButtonDefaults.buttonElevation(6.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(
                                                modifier = Modifier
                                                    .size(28.dp)
                                                    .clip(CircleShape)
                                                    .background(Color.White.copy(alpha = 0.25f)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text("$totalItemsCount", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp)
                                            }
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Text("Xem Hoá Đơn & Báo Bếp", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        }

                                        Text(
                                            formatVnd(finalTotal),
                                            fontWeight = FontWeight.Black,
                                            fontSize = 15.sp,
                                            color = SecondaryGold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                PosTab.BILL -> {
                    // Live Bill & Checkout Tab
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Empty Order Notice
                        if (orderItems.isEmpty()) {
                            item {
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 32.dp),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFAFAFA))
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(24.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text("🍽️", fontSize = 42.sp)
                                        Spacer(modifier = Modifier.height(10.dp))
                                        Text(
                                            "Bàn ${selectedTable?.name ?: ""} chưa có món nào",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 16.sp
                                        )
                                        Text(
                                            "Hãy chuyển sang tab 'Thực Đơn' để chọn món phục vụ khách",
                                            fontSize = 12.sp,
                                            color = Color.Gray,
                                            textAlign = TextAlign.Center
                                        )
                                        Spacer(modifier = Modifier.height(16.dp))
                                        Button(
                                            onClick = { selectedPosTab = PosTab.MENU },
                                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryRed),
                                            shape = RoundedCornerShape(10.dp)
                                        ) {
                                            Text("Mở Thực Đơn Gọi Món")
                                        }
                                    }
                                }
                            }
                        } else {
                            // Order Items Header
                            item {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        "Món Đã Gọi Cho Bàn (${orderItems.size} món)",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                    TextButton(onClick = { selectedPosTab = PosTab.MENU }) {
                                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp), tint = PrimaryRed)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Gọi thêm món", color = PrimaryRed, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    }
                                }
                            }

                            // Itemized Dishes
                            items(orderItems) { item ->
                                BillItemCard(
                                    item = item,
                                    onIncrease = {
                                        val list = orderItems.toMutableList()
                                        val idx = list.indexOf(item)
                                        if (idx != -1) {
                                            list[idx] = item.copy(quantity = item.quantity + 1)
                                            orderItems = list
                                            selectedTable?.let { repository.saveActiveOrderForTable(it.id, list) }
                                        }
                                    },
                                    onDecrease = {
                                        val list = orderItems.toMutableList()
                                        val idx = list.indexOf(item)
                                        if (idx != -1) {
                                            if (item.quantity > 1) {
                                                list[idx] = item.copy(quantity = item.quantity - 1)
                                            } else {
                                                list.removeAt(idx)
                                            }
                                            orderItems = list
                                            selectedTable?.let { repository.saveActiveOrderForTable(it.id, list) }
                                        }
                                    },
                                    onDelete = {
                                        val list = orderItems.toMutableList()
                                        list.remove(item)
                                        orderItems = list
                                        selectedTable?.let { repository.saveActiveOrderForTable(it.id, list) }
                                    },
                                    onEditNote = {
                                        itemToEditNote = item
                                        noteInputText = item.note
                                    }
                                )
                            }

                            // Billing Summary Card
                            item {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color.White),
                                    elevation = CardDefaults.cardElevation(2.dp)
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text("Tổng tiền món (Tạm tính):", fontSize = 13.sp, color = Color.Gray)
                                            Text(formatVnd(subtotal), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                        }

                                        if (discountPercent > 0) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text(
                                                    "Chiết khấu VIP (${selectedCustomer?.tier?.displayName} - ${discountPercent.toInt()}%):",
                                                    fontSize = 13.sp,
                                                    color = PrimaryRed
                                                )
                                                Text("-${formatVnd(discountAmount)}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = PrimaryRed)
                                            }
                                        }

                                        HorizontalDivider(color = Color(0xFFEEEEEE))

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column {
                                                Text("TỔNG THANH TOÁN:", fontWeight = FontWeight.Black, fontSize = 15.sp)
                                                Text("Đã bao gồm thuế GTGT & phục vụ", fontSize = 10.sp, color = Color.Gray)
                                            }
                                            Text(
                                                text = formatVnd(finalTotal),
                                                fontWeight = FontWeight.Black,
                                                fontSize = 20.sp,
                                                color = PrimaryRed
                                            )
                                        }
                                    }
                                }
                            }

                            // Payment Methods
                            item {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color.White),
                                    elevation = CardDefaults.cardElevation(2.dp)
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp)
                                    ) {
                                        Text("Phương thức thanh toán:", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        Spacer(modifier = Modifier.height(8.dp))

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            PaymentMethodChip(
                                                modifier = Modifier.weight(1f),
                                                title = "📱 VietQR 24/7",
                                                subtitle = "Chuyển khoản tức thì",
                                                isSelected = paymentMethod == "VietQR",
                                                onClick = { paymentMethod = "VietQR" }
                                            )
                                            PaymentMethodChip(
                                                modifier = Modifier.weight(1f),
                                                title = "💵 Tiền Mặt",
                                                subtitle = "Thanh toán tại quầy",
                                                isSelected = paymentMethod == "Tiền mặt",
                                                onClick = { paymentMethod = "Tiền mặt" }
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(12.dp))

                                        if (paymentMethod == "Tiền mặt") {
                                            OutlinedTextField(
                                                value = cashGivenInput,
                                                onValueChange = { cashGivenInput = it },
                                                label = { Text("Tiền khách đưa (VNĐ)") },
                                                trailingIcon = { Text("đ", fontWeight = FontWeight.Bold, modifier = Modifier.padding(end = 12.dp)) },
                                                shape = RoundedCornerShape(10.dp),
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text("Tiền trả lại khách:", fontSize = 12.sp, color = Color.Gray)
                                                Text(formatVnd(changeAmount), fontWeight = FontWeight.Bold, color = PrimaryRed, fontSize = 14.sp)
                                            }
                                        } else {
                                            // VietQR Card
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clip(RoundedCornerShape(12.dp))
                                                    .background(Color(0xFFF0F4F8))
                                                    .border(1.dp, Color(0xFFBBDEFB), RoundedCornerShape(12.dp))
                                                    .padding(12.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(68.dp)
                                                        .clip(RoundedCornerShape(8.dp))
                                                        .background(Color.White)
                                                        .border(1.dp, Color(0xFFE0E0E0), RoundedCornerShape(8.dp))
                                                        .padding(4.dp),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    SimulatedQrMatrixCanvas(
                                                        seed = finalTotal.toInt(),
                                                        modifier = Modifier.size(60.dp)
                                                    )
                                                }

                                                Spacer(modifier = Modifier.width(12.dp))

                                                Column {
                                                    Text("Quét mã VietQR thanh toán:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1565C0))
                                                    Text("MB Bank • STK: 1903888888", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                                    Text("Chủ TK: GOLDEN SPOON RESTAURANT", fontSize = 10.sp, color = Color.Gray)
                                                    Text("Số tiền: ${formatVnd(finalTotal)}", fontSize = 13.sp, fontWeight = FontWeight.Black, color = PrimaryRed)
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            // Action Buttons Footer
                            item {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    // Save & Serve Button
                                    OutlinedButton(
                                        onClick = {
                                            selectedTable?.let { table ->
                                                if (orderItems.isNotEmpty()) {
                                                    repository.saveActiveOrderForTable(table.id, orderItems)
                                                    repository.updateTableStatus(table.id, TableStatus.DANG_PHUC_VU)
                                                    saveSuccessMessage = "Đã lưu đơn thành công cho ${table.name}! Bếp đã nhận được thông tin món ăn."
                                                }
                                            }
                                        },
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(50.dp),
                                        shape = RoundedCornerShape(12.dp),
                                        border = androidx.compose.foundation.BorderStroke(1.5.dp, PrimaryRed),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = PrimaryRed)
                                    ) {
                                        Icon(Icons.Default.SoupKitchen, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("BÁO BẾP", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                    }

                                    // Complete & Checkout Button
                                    Button(
                                        onClick = {
                                            selectedTable?.let { table ->
                                                if (orderItems.isNotEmpty()) {
                                                    val order = Order(
                                                        tableId = table.id,
                                                        tableName = table.name,
                                                        customerId = selectedCustomer?.id,
                                                        customerName = selectedCustomer?.name ?: "Khách vãng lai",
                                                        customerTierName = selectedCustomer?.tier?.displayName ?: "Bậc Đồng",
                                                        employeeId = currentUser.id,
                                                        employeeName = currentUser.fullName,
                                                        items = orderItems,
                                                        subtotal = subtotal,
                                                        discountPercent = discountPercent,
                                                        paymentMethod = paymentMethod,
                                                        status = OrderStatus.COMPLETED
                                                    )
                                                    val orderId = repository.checkoutOrder(order)
                                                    completedOrderForBill = order.copy(id = orderId)
                                                    orderItems = mutableListOf()
                                                }
                                            }
                                        },
                                        modifier = Modifier
                                            .weight(1.3f)
                                            .height(50.dp),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryRed),
                                        elevation = ButtonDefaults.buttonElevation(4.dp)
                                    ) {
                                        Icon(Icons.Default.Payment, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("THANH TOÁN", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                                Spacer(modifier = Modifier.height(20.dp))
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal Table Picker Dialog
    if (showTablePickerSheet) {
        AlertDialog(
            onDismissRequest = { showTablePickerSheet = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.TableBar, contentDescription = null, tint = PrimaryRed)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Chọn Bàn Ăn", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(350.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(tables) { table ->
                        val isSelected = selectedTable?.id == table.id
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedTable = table
                                    showTablePickerSheet = false
                                },
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) PrimaryRed.copy(alpha = 0.1f) else Color(0xFFF9F9F9)
                            ),
                            border = if (isSelected) androidx.compose.foundation.BorderStroke(1.5.dp, PrimaryRed) else null
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(table.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text("${table.floor} • ${table.seats} chỗ", fontSize = 11.sp, color = Color.Gray)
                                }
                                TableStatusBadge(status = table.status)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showTablePickerSheet = false }) { Text("Đóng") }
            }
        )
    }

    // Modal Customer Picker Dialog
    if (showCustomerPickerSheet) {
        AlertDialog(
            onDismissRequest = { showCustomerPickerSheet = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Diamond, contentDescription = null, tint = SecondaryDarkGold)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Chọn Khách Hàng Thành Viên", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(350.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Option 0: Guest
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedCustomer = null
                                    showCustomerPickerSheet = false
                                },
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (selectedCustomer == null) PrimaryRed.copy(alpha = 0.1f) else Color(0xFFF9F9F9)
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("Khách vãng lai", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text("Không áp dụng tích điểm & giảm giá", fontSize = 11.sp, color = Color.Gray)
                                }
                            }
                        }
                    }

                    items(customers) { cust ->
                        val isSelected = selectedCustomer?.id == cust.id
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedCustomer = cust
                                    showCustomerPickerSheet = false
                                },
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) PrimaryRed.copy(alpha = 0.1f) else Color(0xFFF9F9F9)
                            ),
                            border = if (isSelected) androidx.compose.foundation.BorderStroke(1.5.dp, PrimaryRed) else null
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(cust.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text("SĐT: ${cust.phone} • Giảm ${cust.discountPercent.toInt()}%", fontSize = 11.sp, color = Color.Gray)
                                }
                                CustomerTierBadge(tier = cust.tier)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showCustomerPickerSheet = false }) { Text("Đóng") }
            }
        )
    }

    // Modal Edit Dish Cooking Note
    itemToEditNote?.let { item ->
        AlertDialog(
            onDismissRequest = { itemToEditNote = null },
            title = { Text("Ghi Chú Chế Biến", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Món: ${item.menuItemName}", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = noteInputText,
                        onValueChange = { noteInputText = it },
                        placeholder = { Text("VD: Ít cay, không hành, uống lạnh...") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val list = orderItems.toMutableList()
                        val idx = list.indexOf(item)
                        if (idx != -1) {
                            list[idx] = item.copy(note = noteInputText)
                            orderItems = list
                            selectedTable?.let { repository.saveActiveOrderForTable(it.id, list) }
                        }
                        itemToEditNote = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryRed)
                ) {
                    Text("Lưu Ghi Chú")
                }
            },
            dismissButton = {
                TextButton(onClick = { itemToEditNote = null }) { Text("Hủy") }
            }
        )
    }

    // Save Order Confirmation Alert
    saveSuccessMessage?.let { msg ->
        AlertDialog(
            onDismissRequest = { saveSuccessMessage = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("🍳", fontSize = 20.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Đã Báo Món Vào Bếp", fontWeight = FontWeight.Bold)
                }
            },
            text = { Text(msg) },
            confirmButton = {
                Button(
                    onClick = { saveSuccessMessage = null },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryRed)
                ) {
                    Text("Xác Nhận")
                }
            }
        )
    }

    // Thermal Receipt Dialog
    completedOrderForBill?.let { billOrder ->
        BillReceiptDialog(
            order = billOrder,
            onDismiss = { completedOrderForBill = null }
        )
    }

    // QR Order Scanner Dialog
    if (showQrScannerDialog) {
        QrOrderScannerDialog(
            repository = repository,
            onTableScanned = { table ->
                showQrScannerDialog = false
                selectedTable = table
                orderItems = repository.getActiveOrderForTable(table.id).toMutableList()
            },
            onOrderScanned = { order ->
                showQrScannerDialog = false
                completedOrderForBill = order
            },
            onDismiss = { showQrScannerDialog = false }
        )
    }

    // Customer Digital Menu Ordering Dialog
    if (showCustomerMenuDialog && selectedTable != null) {
        CustomerQrOrderDialog(
            table = selectedTable!!,
            repository = repository,
            onOrderCompleted = { table ->
                orderItems = repository.getActiveOrderForTable(table.id).toMutableList()
            },
            onProceedToCheckout = { table ->
                showCustomerMenuDialog = false
                orderItems = repository.getActiveOrderForTable(table.id).toMutableList()
                selectedPosTab = PosTab.BILL
            },
            onDismiss = {
                showCustomerMenuDialog = false
                selectedTable?.let { table ->
                    orderItems = repository.getActiveOrderForTable(table.id).toMutableList()
                }
            }
        )
    }
}

@Composable
fun PosDishCard(
    dish: MenuItem,
    currentQuantity: Int,
    onAdd: () -> Unit,
    onRemove: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp),
        border = if (currentQuantity > 0) androidx.compose.foundation.BorderStroke(1.dp, PrimaryRed.copy(alpha = 0.4f)) else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Category Emoji Box
            val emoji = when (dish.category) {
                "Khai vị" -> "🥗"
                "Món chính" -> "🥩"
                "Đồ uống" -> "🍹"
                "Tráng miệng" -> "🍨"
                else -> "🍲"
            }
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFFFFF3E0)),
                contentAlignment = Alignment.Center
            ) {
                Text(emoji, fontSize = 24.sp)
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Dish Info
            Column(modifier = Modifier.weight(1f)) {
                Text(dish.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                if (dish.description.isNotBlank()) {
                    Text(dish.description, fontSize = 11.sp, color = Color.Gray, maxLines = 1)
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(formatVnd(dish.price), fontWeight = FontWeight.Bold, color = PrimaryRed, fontSize = 13.sp)
            }

            // Quick Stepper / Add button
            if (currentQuantity > 0) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(PrimaryRed.copy(alpha = 0.1f))
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    IconButton(onClick = onRemove, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Remove, contentDescription = null, tint = PrimaryRed, modifier = Modifier.size(16.dp))
                    }
                    Text(
                        "$currentQuantity",
                        fontWeight = FontWeight.Bold,
                        color = PrimaryRed,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                    IconButton(onClick = onAdd, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = PrimaryRed, modifier = Modifier.size(16.dp))
                    }
                }
            } else {
                FilledTonalButton(
                    onClick = onAdd,
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Thêm", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
fun BillItemCard(
    item: OrderItem,
    onIncrease: () -> Unit,
    onDecrease: () -> Unit,
    onDelete: () -> Unit,
    onEditNote: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(1.5.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(item.menuItemName, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text(
                        "${formatVnd(item.price)} x ${item.quantity} = ${formatVnd(item.totalPrice)}",
                        fontSize = 12.sp,
                        color = Color.DarkGray
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onDecrease, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Remove, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(18.dp))
                    }
                    Text(
                        "${item.quantity}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(horizontal = 6.dp)
                    )
                    IconButton(onClick = onIncrease, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = PrimaryRed, modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = null, tint = Color.LightGray, modifier = Modifier.size(18.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Cooking note badge / button
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xFFF5F5F5))
                    .clickable { onEditNote() }
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Icon(Icons.Default.EditNote, contentDescription = null, tint = PrimaryRed, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = if (item.note.isNotBlank()) "Ghi chú: ${item.note}" else "Thêm ghi chú chế biến...",
                    fontSize = 11.sp,
                    color = if (item.note.isNotBlank()) Color(0xFF1E1E1E) else Color.Gray,
                    fontWeight = if (item.note.isNotBlank()) FontWeight.Medium else FontWeight.Normal
                )
            }
        }
    }
}

@Composable
fun PaymentMethodChip(
    modifier: Modifier = Modifier,
    title: String,
    subtitle: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier.clickable { onClick() },
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) PrimaryRed.copy(alpha = 0.1f) else Color(0xFFFAFAFA)
        ),
        border = androidx.compose.foundation.BorderStroke(
            if (isSelected) 1.5.dp else 1.dp,
            if (isSelected) PrimaryRed else Color(0xFFE0E0E0)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            Text(title, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = if (isSelected) PrimaryRed else Color.Black)
            Text(subtitle, fontSize = 10.sp, color = Color.Gray)
        }
    }
}
