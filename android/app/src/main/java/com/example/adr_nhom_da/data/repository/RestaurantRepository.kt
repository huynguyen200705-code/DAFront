package com.example.adr_nhom_da.data.repository

import android.content.ContentValues
import android.content.Context
import android.util.Log
import com.example.adr_nhom_da.data.local.RestaurantDbHelper
import com.example.adr_nhom_da.data.model.*
import com.example.adr_nhom_da.data.remote.RealtimeSyncManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.json.JSONArray
import org.json.JSONObject

class RestaurantRepository(context: Context) {
    private val dbHelper = RestaurantDbHelper(context)

    // Reactive StateFlow: Updates every time data is modified locally or from another device
    private val _syncVersion = MutableStateFlow(0L)
    val syncVersion: StateFlow<Long> = _syncVersion

    fun notifyDataChanged(reason: String = "") {
        _syncVersion.value = System.currentTimeMillis()
    }

    init {
        // Start Realtime Sync Engine (P2P Wi-Fi UDP Multicast listener)
        RealtimeSyncManager.start(context)
        RealtimeSyncManager.onRemoteEventReceived = { type, payload, message ->
            handleRemoteSyncEvent(type, payload, message)
        }
    }

    // --- AUTHENTICATION ---
    fun login(usernameInput: String, passwordInput: String): User? {
        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery(
            "SELECT * FROM ${RestaurantDbHelper.TABLE_USERS} WHERE username=? AND password=?",
            arrayOf(usernameInput, passwordInput)
        )
        var user: User? = null
        if (cursor.moveToFirst()) {
            user = User(
                id = cursor.getLong(cursor.getColumnIndexOrThrow("id")),
                username = cursor.getString(cursor.getColumnIndexOrThrow("username")),
                password = cursor.getString(cursor.getColumnIndexOrThrow("password")),
                fullName = cursor.getString(cursor.getColumnIndexOrThrow("full_name")),
                phone = cursor.getString(cursor.getColumnIndexOrThrow("phone")) ?: "",
                role = UserRole.valueOf(cursor.getString(cursor.getColumnIndexOrThrow("role")))
            )
        }
        cursor.close()
        return user
    }

    fun register(user: User): Boolean {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put("username", user.username)
            put("password", user.password)
            put("full_name", user.fullName)
            put("phone", user.phone)
            put("role", user.role.name)
        }
        val result = db.insert(RestaurantDbHelper.TABLE_USERS, null, values)
        return result != -1L
    }

    // --- CUSTOMERS ---
    fun getAllCustomers(): List<Customer> {
        val list = mutableListOf<Customer>()
        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery("SELECT * FROM ${RestaurantDbHelper.TABLE_CUSTOMERS}", null)
        if (cursor.moveToFirst()) {
            do {
                list.add(
                    Customer(
                        id = cursor.getLong(cursor.getColumnIndexOrThrow("id")),
                        name = cursor.getString(cursor.getColumnIndexOrThrow("name")),
                        phone = cursor.getString(cursor.getColumnIndexOrThrow("phone")),
                        email = cursor.getString(cursor.getColumnIndexOrThrow("email")) ?: "",
                        totalSpent = cursor.getDouble(cursor.getColumnIndexOrThrow("total_spent")),
                        loyaltyPoints = cursor.getInt(cursor.getColumnIndexOrThrow("loyalty_points"))
                    )
                )
            } while (cursor.moveToNext())
        }
        cursor.close()
        return list
    }

    fun addCustomer(customer: Customer, broadcast: Boolean = true): Boolean {
        val db = dbHelper.writableDatabase
        val cv = ContentValues().apply {
            put("name", customer.name)
            put("phone", customer.phone)
            put("email", customer.email)
            put("total_spent", customer.totalSpent)
            put("loyalty_points", customer.loyaltyPoints)
        }
        val id = db.insert(RestaurantDbHelper.TABLE_CUSTOMERS, null, cv)
        if (id != -1L) {
            notifyDataChanged("add_customer")
            if (broadcast) {
                val payload = JSONObject().apply {
                    put("id", id)
                    put("name", customer.name)
                    put("phone", customer.phone)
                    put("email", customer.email)
                    put("totalSpent", customer.totalSpent)
                    put("loyaltyPoints", customer.loyaltyPoints)
                }
                RealtimeSyncManager.broadcast("CUSTOMER_ADDED", payload, "Thêm khách hàng: ${customer.name}")
            }
            return true
        }
        return false
    }

    fun updateCustomer(customer: Customer, broadcast: Boolean = true): Boolean {
        val db = dbHelper.writableDatabase
        val cv = ContentValues().apply {
            put("name", customer.name)
            put("phone", customer.phone)
            put("email", customer.email)
            put("total_spent", customer.totalSpent)
            put("loyalty_points", customer.loyaltyPoints)
        }
        val success = db.update(RestaurantDbHelper.TABLE_CUSTOMERS, cv, "id=?", arrayOf(customer.id.toString())) > 0
        if (success) {
            notifyDataChanged("update_customer")
            if (broadcast) {
                val payload = JSONObject().apply {
                    put("id", customer.id)
                    put("name", customer.name)
                    put("phone", customer.phone)
                    put("email", customer.email)
                    put("totalSpent", customer.totalSpent)
                    put("loyaltyPoints", customer.loyaltyPoints)
                }
                RealtimeSyncManager.broadcast("CUSTOMER_UPDATED", payload, "Cập nhật khách hàng: ${customer.name}")
            }
        }
        return success
    }

    fun getCustomerById(id: Long): Customer? {
        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery("SELECT * FROM ${RestaurantDbHelper.TABLE_CUSTOMERS} WHERE id=?", arrayOf(id.toString()))
        var customer: Customer? = null
        if (cursor.moveToFirst()) {
            customer = Customer(
                id = cursor.getLong(cursor.getColumnIndexOrThrow("id")),
                name = cursor.getString(cursor.getColumnIndexOrThrow("name")),
                phone = cursor.getString(cursor.getColumnIndexOrThrow("phone")),
                email = cursor.getString(cursor.getColumnIndexOrThrow("email")) ?: "",
                totalSpent = cursor.getDouble(cursor.getColumnIndexOrThrow("total_spent")),
                loyaltyPoints = cursor.getInt(cursor.getColumnIndexOrThrow("loyalty_points"))
            )
        }
        cursor.close()
        return customer
    }

    fun deleteCustomer(id: Long, broadcast: Boolean = true): Boolean {
        val cust = getCustomerById(id)
        val db = dbHelper.writableDatabase
        val success = db.delete(RestaurantDbHelper.TABLE_CUSTOMERS, "id=?", arrayOf(id.toString())) > 0
        if (success) {
            notifyDataChanged("delete_customer")
            if (broadcast) {
                val payload = JSONObject().apply {
                    put("id", id)
                    put("name", cust?.name ?: "")
                    put("phone", cust?.phone ?: "")
                }
                RealtimeSyncManager.broadcast("CUSTOMER_DELETED", payload, "Xóa khách hàng: ${cust?.name ?: "#$id"}")
            }
        }
        return success
    }

    // --- EMPLOYEES ---
    fun getAllEmployees(): List<Employee> {
        val list = mutableListOf<Employee>()
        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery("SELECT * FROM ${RestaurantDbHelper.TABLE_EMPLOYEES}", null)
        if (cursor.moveToFirst()) {
            do {
                list.add(
                    Employee(
                        id = cursor.getLong(cursor.getColumnIndexOrThrow("id")),
                        name = cursor.getString(cursor.getColumnIndexOrThrow("name")),
                        phone = cursor.getString(cursor.getColumnIndexOrThrow("phone")),
                        position = cursor.getString(cursor.getColumnIndexOrThrow("position")),
                        tier = EmployeeTier.valueOf(cursor.getString(cursor.getColumnIndexOrThrow("tier"))),
                        contractType = ContractType.valueOf(cursor.getString(cursor.getColumnIndexOrThrow("contract_type"))),
                        hourlyRate = cursor.getDouble(cursor.getColumnIndexOrThrow("hourly_rate")),
                        monthlySalary = cursor.getDouble(cursor.getColumnIndexOrThrow("monthly_salary")),
                        workedHours = cursor.getDouble(cursor.getColumnIndexOrThrow("worked_hours")),
                        workedDays = cursor.getDouble(cursor.getColumnIndexOrThrow("worked_days")),
                        bonus = cursor.getDouble(cursor.getColumnIndexOrThrow("bonus")),
                        penalty = cursor.getDouble(cursor.getColumnIndexOrThrow("penalty"))
                    )
                )
            } while (cursor.moveToNext())
        }
        cursor.close()
        return list
    }

    fun addEmployee(emp: Employee, broadcast: Boolean = true): Boolean {
        val db = dbHelper.writableDatabase
        val cv = ContentValues().apply {
            put("name", emp.name)
            put("phone", emp.phone)
            put("position", emp.position)
            put("tier", emp.tier.name)
            put("contract_type", emp.contractType.name)
            put("hourly_rate", emp.hourlyRate)
            put("monthly_salary", emp.monthlySalary)
            put("worked_hours", emp.workedHours)
            put("worked_days", emp.workedDays)
            put("bonus", emp.bonus)
            put("penalty", emp.penalty)
        }
        val id = db.insert(RestaurantDbHelper.TABLE_EMPLOYEES, null, cv)
        if (id != -1L) {
            notifyDataChanged("add_employee")
            if (broadcast) {
                val payload = JSONObject().apply {
                    put("id", id)
                    put("name", emp.name)
                    put("phone", emp.phone)
                    put("position", emp.position)
                    put("tier", emp.tier.name)
                    put("contractType", emp.contractType.name)
                    put("hourlyRate", emp.hourlyRate)
                    put("monthlySalary", emp.monthlySalary)
                    put("workedHours", emp.workedHours)
                    put("workedDays", emp.workedDays)
                    put("bonus", emp.bonus)
                    put("penalty", emp.penalty)
                }
                RealtimeSyncManager.broadcast("EMPLOYEE_ADDED", payload, "Thêm nhân viên: ${emp.name}")
            }
            return true
        }
        return false
    }

    fun updateEmployee(emp: Employee, broadcast: Boolean = true): Boolean {
        val db = dbHelper.writableDatabase
        val cv = ContentValues().apply {
            put("name", emp.name)
            put("phone", emp.phone)
            put("position", emp.position)
            put("tier", emp.tier.name)
            put("contract_type", emp.contractType.name)
            put("hourly_rate", emp.hourlyRate)
            put("monthly_salary", emp.monthlySalary)
            put("worked_hours", emp.workedHours)
            put("worked_days", emp.workedDays)
            put("bonus", emp.bonus)
            put("penalty", emp.penalty)
        }
        val success = db.update(RestaurantDbHelper.TABLE_EMPLOYEES, cv, "id=?", arrayOf(emp.id.toString())) > 0
        if (success) {
            notifyDataChanged("update_employee")
            if (broadcast) {
                val payload = JSONObject().apply {
                    put("id", emp.id)
                    put("name", emp.name)
                    put("phone", emp.phone)
                    put("position", emp.position)
                    put("tier", emp.tier.name)
                    put("contractType", emp.contractType.name)
                    put("hourlyRate", emp.hourlyRate)
                    put("monthlySalary", emp.monthlySalary)
                    put("workedHours", emp.workedHours)
                    put("workedDays", emp.workedDays)
                    put("bonus", emp.bonus)
                    put("penalty", emp.penalty)
                }
                RealtimeSyncManager.broadcast("EMPLOYEE_UPDATED", payload, "Cập nhật nhân viên: ${emp.name}")
            }
        }
        return success
    }

    fun deleteEmployee(id: Long, broadcast: Boolean = true): Boolean {
        val emp = getEmployeeById(id)
        val db = dbHelper.writableDatabase
        val success = db.delete(RestaurantDbHelper.TABLE_EMPLOYEES, "id=?", arrayOf(id.toString())) > 0
        if (success) {
            notifyDataChanged("delete_employee")
            if (broadcast) {
                val payload = JSONObject().apply {
                    put("id", id)
                    put("name", emp?.name ?: "")
                    put("phone", emp?.phone ?: "")
                }
                RealtimeSyncManager.broadcast("EMPLOYEE_DELETED", payload, "Xóa nhân viên: ${emp?.name ?: "#$id"}")
            }
        }
        return success
    }

    // --- MENU ITEMS ---
    fun getAllMenuItems(): List<MenuItem> {
        val list = mutableListOf<MenuItem>()
        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery("SELECT * FROM ${RestaurantDbHelper.TABLE_MENU_ITEMS}", null)
        if (cursor.moveToFirst()) {
            do {
                list.add(
                    MenuItem(
                        id = cursor.getLong(cursor.getColumnIndexOrThrow("id")),
                        name = cursor.getString(cursor.getColumnIndexOrThrow("name")),
                        category = cursor.getString(cursor.getColumnIndexOrThrow("category")),
                        price = cursor.getDouble(cursor.getColumnIndexOrThrow("price")),
                        description = cursor.getString(cursor.getColumnIndexOrThrow("description")) ?: "",
                        isAvailable = cursor.getInt(cursor.getColumnIndexOrThrow("is_available")) == 1
                    )
                )
            } while (cursor.moveToNext())
        }
        cursor.close()
        return list
    }

    fun getMenuItemById(id: Long): MenuItem? {
        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery("SELECT * FROM ${RestaurantDbHelper.TABLE_MENU_ITEMS} WHERE id=?", arrayOf(id.toString()))
        var item: MenuItem? = null
        if (cursor.moveToFirst()) {
            item = MenuItem(
                id = cursor.getLong(cursor.getColumnIndexOrThrow("id")),
                name = cursor.getString(cursor.getColumnIndexOrThrow("name")),
                category = cursor.getString(cursor.getColumnIndexOrThrow("category")),
                price = cursor.getDouble(cursor.getColumnIndexOrThrow("price")),
                description = cursor.getString(cursor.getColumnIndexOrThrow("description")) ?: "",
                isAvailable = cursor.getInt(cursor.getColumnIndexOrThrow("is_available")) == 1
            )
        }
        cursor.close()
        return item
    }

    fun addMenuItem(item: MenuItem, broadcast: Boolean = true): Boolean {
        val db = dbHelper.writableDatabase
        val cv = ContentValues().apply {
            put("name", item.name)
            put("category", item.category)
            put("price", item.price)
            put("description", item.description)
            put("is_available", if (item.isAvailable) 1 else 0)
        }
        val id = db.insert(RestaurantDbHelper.TABLE_MENU_ITEMS, null, cv)
        if (id != -1L) {
            notifyDataChanged("add_menu")
            if (broadcast) {
                val payload = JSONObject().apply {
                    put("id", id)
                    put("name", item.name)
                    put("category", item.category)
                    put("price", item.price)
                    put("description", item.description)
                    put("isAvailable", item.isAvailable)
                }
                RealtimeSyncManager.broadcast("MENU_ADDED", payload, "Thêm món: ${item.name}")
            }
            return true
        }
        return false
    }

    fun updateMenuItem(item: MenuItem, broadcast: Boolean = true): Boolean {
        val db = dbHelper.writableDatabase
        val cv = ContentValues().apply {
            put("name", item.name)
            put("category", item.category)
            put("price", item.price)
            put("description", item.description)
            put("is_available", if (item.isAvailable) 1 else 0)
        }
        val success = db.update(RestaurantDbHelper.TABLE_MENU_ITEMS, cv, "id=?", arrayOf(item.id.toString())) > 0
        if (success) {
            notifyDataChanged("update_menu")
            if (broadcast) {
                val payload = JSONObject().apply {
                    put("id", item.id)
                    put("name", item.name)
                    put("category", item.category)
                    put("price", item.price)
                    put("description", item.description)
                    put("isAvailable", item.isAvailable)
                }
                RealtimeSyncManager.broadcast("MENU_UPDATED", payload, "Cập nhật món: ${item.name}")
            }
        }
        return success
    }

    fun deleteMenuItem(id: Long, broadcast: Boolean = true): Boolean {
        val item = getMenuItemById(id)
        val db = dbHelper.writableDatabase
        val success = db.delete(RestaurantDbHelper.TABLE_MENU_ITEMS, "id=?", arrayOf(id.toString())) > 0
        if (success) {
            notifyDataChanged("delete_menu")
            if (broadcast) {
                val payload = JSONObject().apply {
                    put("id", id)
                    put("name", item?.name ?: "")
                }
                RealtimeSyncManager.broadcast("MENU_DELETED", payload, "Xóa món: ${item?.name ?: "#$id"}")
            }
        }
        return success
    }

    // --- TABLES ---
    fun getAllTables(): List<RestaurantTable> {
        val list = mutableListOf<RestaurantTable>()
        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery("SELECT * FROM ${RestaurantDbHelper.TABLE_TABLES}", null)
        if (cursor.moveToFirst()) {
            do {
                val orderIdVal = cursor.getLong(cursor.getColumnIndexOrThrow("current_order_id"))
                list.add(
                    RestaurantTable(
                        id = cursor.getLong(cursor.getColumnIndexOrThrow("id")),
                        name = cursor.getString(cursor.getColumnIndexOrThrow("name")),
                        floor = cursor.getString(cursor.getColumnIndexOrThrow("floor")),
                        seats = cursor.getInt(cursor.getColumnIndexOrThrow("seats")),
                        status = TableStatus.valueOf(cursor.getString(cursor.getColumnIndexOrThrow("status"))),
                        currentOrderId = if (cursor.isNull(cursor.getColumnIndexOrThrow("current_order_id"))) null else orderIdVal
                    )
                )
            } while (cursor.moveToNext())
        }
        cursor.close()
        return list
    }

    fun getTableById(id: Long): RestaurantTable? {
        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery("SELECT * FROM ${RestaurantDbHelper.TABLE_TABLES} WHERE id=?", arrayOf(id.toString()))
        var table: RestaurantTable? = null
        if (cursor.moveToFirst()) {
            val orderIdVal = cursor.getLong(cursor.getColumnIndexOrThrow("current_order_id"))
            table = RestaurantTable(
                id = cursor.getLong(cursor.getColumnIndexOrThrow("id")),
                name = cursor.getString(cursor.getColumnIndexOrThrow("name")),
                floor = cursor.getString(cursor.getColumnIndexOrThrow("floor")),
                seats = cursor.getInt(cursor.getColumnIndexOrThrow("seats")),
                status = TableStatus.valueOf(cursor.getString(cursor.getColumnIndexOrThrow("status"))),
                currentOrderId = if (cursor.isNull(cursor.getColumnIndexOrThrow("current_order_id"))) null else orderIdVal
            )
        }
        cursor.close()
        return table
    }

    fun addTable(table: RestaurantTable, broadcast: Boolean = true): Boolean {
        val db = dbHelper.writableDatabase
        val cv = ContentValues().apply {
            put("name", table.name)
            put("floor", table.floor)
            put("seats", table.seats)
            put("status", table.status.name)
        }
        val id = db.insert(RestaurantDbHelper.TABLE_TABLES, null, cv)
        if (id != -1L) {
            notifyDataChanged("add_table")
            if (broadcast) {
                val payload = JSONObject().apply {
                    put("id", id)
                    put("name", table.name)
                    put("floor", table.floor)
                }
                RealtimeSyncManager.broadcast("TABLE_ADDED", payload, "Thêm bàn mới: ${table.name}")
            }
            return true
        }
        return false
    }

    fun updateTableStatus(tableId: Long, status: TableStatus, broadcast: Boolean = true): Boolean {
        val db = dbHelper.writableDatabase
        val cv = ContentValues().apply {
            put("status", status.name)
        }
        val success = db.update(RestaurantDbHelper.TABLE_TABLES, cv, "id=?", arrayOf(tableId.toString())) > 0
        if (success) {
            notifyDataChanged("table_status")
            if (broadcast) {
                val table = getTableById(tableId)
                val payload = JSONObject().apply {
                    put("tableId", tableId)
                    put("status", status.name)
                    put("tableName", table?.name ?: "Bàn $tableId")
                }
                RealtimeSyncManager.broadcast(
                    "TABLE_STATUS_CHANGED",
                    payload,
                    "${table?.name ?: "Bàn $tableId"} chuyển sang ${status.displayName}"
                )
            }
        }
        return success
    }

    fun deleteTable(id: Long, broadcast: Boolean = true): Boolean {
        val db = dbHelper.writableDatabase
        val success = db.delete(RestaurantDbHelper.TABLE_TABLES, "id=?", arrayOf(id.toString())) > 0
        if (success) {
            notifyDataChanged("delete_table")
            if (broadcast) {
                val payload = JSONObject().apply { put("id", id) }
                RealtimeSyncManager.broadcast("TABLE_DELETED", payload, "Xóa bàn #$id")
            }
        }
        return success
    }

    // --- ACTIVE TABLE ORDERS IN-MEMORY ---
    private val activeTableOrders = mutableMapOf<Long, MutableList<OrderItem>>()

    fun getActiveOrderForTable(tableId: Long): MutableList<OrderItem> {
        return activeTableOrders[tableId]?.toMutableList() ?: mutableListOf()
    }

    fun saveActiveOrderForTable(tableId: Long, items: List<OrderItem>, broadcast: Boolean = true) {
        val table = getTableById(tableId)
        if (items.isNotEmpty()) {
            activeTableOrders[tableId] = items.toMutableList()
            updateTableStatus(tableId, TableStatus.DANG_PHUC_VU, broadcast = false)
        } else {
            activeTableOrders.remove(tableId)
            updateTableStatus(tableId, TableStatus.TRONG, broadcast = false)
        }
        notifyDataChanged("active_order")

        if (broadcast) {
            val itemsArray = JSONArray()
            for (item in items) {
                itemsArray.put(JSONObject().apply {
                    put("menuItemId", item.menuItemId)
                    put("menuItemName", item.menuItemName)
                    put("price", item.price)
                    put("quantity", item.quantity)
                    put("note", item.note)
                })
            }
            val payload = JSONObject().apply {
                put("tableId", tableId)
                put("tableName", table?.name ?: "Bàn $tableId")
                put("items", itemsArray)
            }
            val actionMsg = if (items.isNotEmpty()) {
                "${table?.name ?: "Bàn $tableId"} vừa gọi ${items.sumOf { it.quantity }} món mới"
            } else {
                "${table?.name ?: "Bàn $tableId"} đã dọn bàn (Trống)"
            }
            RealtimeSyncManager.broadcast("ACTIVE_ORDER_UPDATED", payload, actionMsg)
        }
    }

    fun clearActiveOrderForTable(tableId: Long) {
        activeTableOrders.remove(tableId)
    }

    // --- ORDERS & CHECKOUT ---
    fun checkoutOrder(order: Order, broadcast: Boolean = true): Long {
        val db = dbHelper.writableDatabase
        db.beginTransaction()
        try {
            val cvOrder = ContentValues().apply {
                put("table_id", order.tableId)
                put("table_name", order.tableName)
                put("customer_id", order.customerId)
                put("customer_name", order.customerName)
                put("customer_tier_name", order.customerTierName)
                put("employee_id", order.employeeId)
                put("employee_name", order.employeeName)
                put("subtotal", order.subtotal)
                put("discount_percent", order.discountPercent)
                put("payment_method", order.paymentMethod)
                put("status", OrderStatus.COMPLETED.name)
                put("timestamp", order.timestamp)
            }
            val orderId = db.insert(RestaurantDbHelper.TABLE_ORDERS, null, cvOrder)

            for (item in order.items) {
                val cvItem = ContentValues().apply {
                    put("order_id", orderId)
                    put("menu_item_id", item.menuItemId)
                    put("menu_item_name", item.menuItemName)
                    put("price", item.price)
                    put("quantity", item.quantity)
                    put("note", item.note)
                }
                db.insert(RestaurantDbHelper.TABLE_ORDER_ITEMS, null, cvItem)
            }

            // Clear active table order in memory
            activeTableOrders.remove(order.tableId)

            // Update Table Status to TRONG
            val cvTable = ContentValues().apply {
                put("status", TableStatus.TRONG.name)
                put("current_order_id", null as Long?)
            }
            db.update(RestaurantDbHelper.TABLE_TABLES, cvTable, "id=?", arrayOf(order.tableId.toString()))

            // Update Customer totalSpent & loyalty points if customer selected
            order.customerId?.let { custId ->
                val custCursor = db.rawQuery("SELECT total_spent, loyalty_points FROM ${RestaurantDbHelper.TABLE_CUSTOMERS} WHERE id=?", arrayOf(custId.toString()))
                if (custCursor.moveToFirst()) {
                    val currentSpent = custCursor.getDouble(0)
                    val currentPoints = custCursor.getInt(1)
                    val newSpent = currentSpent + order.finalTotal
                    val earnedPoints = (order.finalTotal / 100000.0).toInt()
                    val cvCust = ContentValues().apply {
                        put("total_spent", newSpent)
                        put("loyalty_points", currentPoints + earnedPoints)
                    }
                    db.update(RestaurantDbHelper.TABLE_CUSTOMERS, cvCust, "id=?", arrayOf(custId.toString()))
                }
                custCursor.close()
            }

            db.setTransactionSuccessful()

            notifyDataChanged("order_checkout")

            if (broadcast) {
                val itemsArray = JSONArray()
                for (item in order.items) {
                    itemsArray.put(JSONObject().apply {
                        put("menuItemId", item.menuItemId)
                        put("menuItemName", item.menuItemName)
                        put("price", item.price)
                        put("quantity", item.quantity)
                        put("note", item.note)
                    })
                }
                val payload = JSONObject().apply {
                    put("orderId", orderId)
                    put("tableId", order.tableId)
                    put("tableName", order.tableName)
                    put("customerId", order.customerId)
                    put("customerName", order.customerName)
                    put("customerTierName", order.customerTierName)
                    put("employeeId", order.employeeId)
                    put("employeeName", order.employeeName)
                    put("subtotal", order.subtotal)
                    put("discountPercent", order.discountPercent)
                    put("paymentMethod", order.paymentMethod)
                    put("timestamp", order.timestamp)
                    put("items", itemsArray)
                }
                RealtimeSyncManager.broadcast(
                    "ORDER_CHECKOUT",
                    payload,
                    "Đơn ${order.tableName} thanh toán hoàn tất (${order.items.size} món)"
                )
            }

            return orderId
        } finally {
            db.endTransaction()
        }
    }

    fun getAllCompletedOrders(): List<Order> {
        val list = mutableListOf<Order>()
        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery("SELECT * FROM ${RestaurantDbHelper.TABLE_ORDERS} ORDER BY id DESC", null)
        if (cursor.moveToFirst()) {
            do {
                val orderId = cursor.getLong(cursor.getColumnIndexOrThrow("id"))
                val items = getOrderItems(orderId)
                list.add(
                    Order(
                        id = orderId,
                        tableId = cursor.getLong(cursor.getColumnIndexOrThrow("table_id")),
                        tableName = cursor.getString(cursor.getColumnIndexOrThrow("table_name")),
                        customerId = if (cursor.isNull(cursor.getColumnIndexOrThrow("customer_id"))) null else cursor.getLong(cursor.getColumnIndexOrThrow("customer_id")),
                        customerName = cursor.getString(cursor.getColumnIndexOrThrow("customer_name")) ?: "Khách vãng lai",
                        customerTierName = cursor.getString(cursor.getColumnIndexOrThrow("customer_tier_name")) ?: "Bậc Đồng",
                        employeeId = cursor.getLong(cursor.getColumnIndexOrThrow("employee_id")),
                        employeeName = cursor.getString(cursor.getColumnIndexOrThrow("employee_name")) ?: "Nhân viên",
                        items = items,
                        subtotal = cursor.getDouble(cursor.getColumnIndexOrThrow("subtotal")),
                        discountPercent = cursor.getDouble(cursor.getColumnIndexOrThrow("discount_percent")),
                        paymentMethod = cursor.getString(cursor.getColumnIndexOrThrow("payment_method")) ?: "Tiền mặt",
                        status = OrderStatus.valueOf(cursor.getString(cursor.getColumnIndexOrThrow("status"))),
                        timestamp = cursor.getLong(cursor.getColumnIndexOrThrow("timestamp"))
                    )
                )
            } while (cursor.moveToNext())
        }
        cursor.close()
        return list
    }

    fun getOrderItems(orderId: Long): List<OrderItem> {
        val list = mutableListOf<OrderItem>()
        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery(
            "SELECT * FROM ${RestaurantDbHelper.TABLE_ORDER_ITEMS} WHERE order_id=?",
            arrayOf(orderId.toString())
        )
        if (cursor.moveToFirst()) {
            do {
                list.add(
                    OrderItem(
                        menuItemId = cursor.getLong(cursor.getColumnIndexOrThrow("menu_item_id")),
                        menuItemName = cursor.getString(cursor.getColumnIndexOrThrow("menu_item_name")),
                        price = cursor.getDouble(cursor.getColumnIndexOrThrow("price")),
                        quantity = cursor.getInt(cursor.getColumnIndexOrThrow("quantity")),
                        note = cursor.getString(cursor.getColumnIndexOrThrow("note")) ?: ""
                    )
                )
            } while (cursor.moveToNext())
        }
        cursor.close()
        return list
    }

    fun getEmployeeById(id: Long): Employee? {
        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery(
            "SELECT * FROM ${RestaurantDbHelper.TABLE_EMPLOYEES} WHERE id=?",
            arrayOf(id.toString())
        )
        var emp: Employee? = null
        if (cursor.moveToFirst()) {
            emp = Employee(
                id = cursor.getLong(cursor.getColumnIndexOrThrow("id")),
                name = cursor.getString(cursor.getColumnIndexOrThrow("name")),
                phone = cursor.getString(cursor.getColumnIndexOrThrow("phone")),
                position = cursor.getString(cursor.getColumnIndexOrThrow("position")),
                tier = EmployeeTier.valueOf(cursor.getString(cursor.getColumnIndexOrThrow("tier"))),
                contractType = ContractType.valueOf(cursor.getString(cursor.getColumnIndexOrThrow("contract_type"))),
                hourlyRate = cursor.getDouble(cursor.getColumnIndexOrThrow("hourly_rate")),
                monthlySalary = cursor.getDouble(cursor.getColumnIndexOrThrow("monthly_salary")),
                workedHours = cursor.getDouble(cursor.getColumnIndexOrThrow("worked_hours")),
                workedDays = cursor.getDouble(cursor.getColumnIndexOrThrow("worked_days")),
                bonus = cursor.getDouble(cursor.getColumnIndexOrThrow("bonus")),
                penalty = cursor.getDouble(cursor.getColumnIndexOrThrow("penalty"))
            )
        }
        cursor.close()
        return emp
    }

    // --- ATTENDANCE (CHẤM CÔNG NFC & QR & FACE ID) ---
    fun getAllAttendanceRecords(): List<AttendanceRecord> {
        val list = mutableListOf<AttendanceRecord>()
        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery(
            "SELECT * FROM ${RestaurantDbHelper.TABLE_ATTENDANCE} ORDER BY id DESC",
            null
        )
        if (cursor.moveToFirst()) {
            do {
                list.add(
                    AttendanceRecord(
                        id = cursor.getLong(cursor.getColumnIndexOrThrow("id")),
                        employeeId = cursor.getLong(cursor.getColumnIndexOrThrow("employee_id")),
                        employeeName = cursor.getString(cursor.getColumnIndexOrThrow("employee_name")),
                        type = AttendanceType.valueOf(cursor.getString(cursor.getColumnIndexOrThrow("type"))),
                        method = AttendanceMethod.valueOf(cursor.getString(cursor.getColumnIndexOrThrow("method"))),
                        timestamp = cursor.getLong(cursor.getColumnIndexOrThrow("timestamp")),
                        durationHours = cursor.getDouble(cursor.getColumnIndexOrThrow("duration_hours")),
                        note = cursor.getString(cursor.getColumnIndexOrThrow("note")) ?: ""
                    )
                )
            } while (cursor.moveToNext())
        }
        cursor.close()
        return list
    }

    fun getLastAttendanceForEmployee(employeeId: Long): AttendanceRecord? {
        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery(
            "SELECT * FROM ${RestaurantDbHelper.TABLE_ATTENDANCE} WHERE employee_id=? ORDER BY id DESC LIMIT 1",
            arrayOf(employeeId.toString())
        )
        var record: AttendanceRecord? = null
        if (cursor.moveToFirst()) {
            record = AttendanceRecord(
                id = cursor.getLong(cursor.getColumnIndexOrThrow("id")),
                employeeId = cursor.getLong(cursor.getColumnIndexOrThrow("employee_id")),
                employeeName = cursor.getString(cursor.getColumnIndexOrThrow("employee_name")),
                type = AttendanceType.valueOf(cursor.getString(cursor.getColumnIndexOrThrow("type"))),
                method = AttendanceMethod.valueOf(cursor.getString(cursor.getColumnIndexOrThrow("method"))),
                timestamp = cursor.getLong(cursor.getColumnIndexOrThrow("timestamp")),
                durationHours = cursor.getDouble(cursor.getColumnIndexOrThrow("duration_hours")),
                note = cursor.getString(cursor.getColumnIndexOrThrow("note")) ?: ""
            )
        }
        cursor.close()
        return record
    }

    fun getLastCheckInForEmployee(employeeId: Long): AttendanceRecord? {
        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery(
            "SELECT * FROM ${RestaurantDbHelper.TABLE_ATTENDANCE} WHERE employee_id=? AND type='CHECK_IN' ORDER BY id DESC LIMIT 1",
            arrayOf(employeeId.toString())
        )
        var record: AttendanceRecord? = null
        if (cursor.moveToFirst()) {
            record = AttendanceRecord(
                id = cursor.getLong(cursor.getColumnIndexOrThrow("id")),
                employeeId = cursor.getLong(cursor.getColumnIndexOrThrow("employee_id")),
                employeeName = cursor.getString(cursor.getColumnIndexOrThrow("employee_name")),
                type = AttendanceType.valueOf(cursor.getString(cursor.getColumnIndexOrThrow("type"))),
                method = AttendanceMethod.valueOf(cursor.getString(cursor.getColumnIndexOrThrow("method"))),
                timestamp = cursor.getLong(cursor.getColumnIndexOrThrow("timestamp")),
                durationHours = cursor.getDouble(cursor.getColumnIndexOrThrow("duration_hours")),
                note = cursor.getString(cursor.getColumnIndexOrThrow("note")) ?: ""
            )
        }
        cursor.close()
        return record
    }

    fun recordAttendance(
        employeeId: Long,
        type: AttendanceType,
        method: AttendanceMethod,
        note: String = "",
        broadcast: Boolean = true
    ): AttendanceRecord? {
        val emp = getEmployeeById(employeeId) ?: return null
        val now = System.currentTimeMillis()
        var durationHours = 0.0

        if (type == AttendanceType.CHECK_OUT) {
            val lastCheckIn = getLastCheckInForEmployee(employeeId)
            if (lastCheckIn != null && lastCheckIn.timestamp < now) {
                val diffMillis = now - lastCheckIn.timestamp
                durationHours = (diffMillis / (1000.0 * 60.0 * 60.0)).coerceAtLeast(0.1)
                durationHours = Math.round(durationHours * 10.0) / 10.0

                val updatedEmp = if (emp.contractType == ContractType.HOURLY) {
                    emp.copy(workedHours = emp.workedHours + durationHours)
                } else {
                    val addDay = if (durationHours >= 4.0) 1.0 else 0.5
                    emp.copy(workedDays = (emp.workedDays + addDay).coerceAtMost(26.0))
                }
                updateEmployee(updatedEmp, broadcast = false)
            }
        }

        val db = dbHelper.writableDatabase
        val cv = ContentValues().apply {
            put("employee_id", employeeId)
            put("employee_name", emp.name)
            put("type", type.name)
            put("method", method.name)
            put("timestamp", now)
            put("duration_hours", durationHours)
            put("note", note)
        }
        val id = db.insert(RestaurantDbHelper.TABLE_ATTENDANCE, null, cv)
        if (id == -1L) return null

        val resultRecord = AttendanceRecord(
            id = id,
            employeeId = employeeId,
            employeeName = emp.name,
            type = type,
            method = method,
            timestamp = now,
            durationHours = durationHours,
            note = note
        )

        notifyDataChanged("attendance")

        if (broadcast) {
            val payload = JSONObject().apply {
                put("employeeId", employeeId)
                put("employeeName", emp.name)
                put("type", type.name)
                put("method", method.name)
                put("timestamp", now)
                put("durationHours", durationHours)
                put("note", note)
            }
            val msg = if (type == AttendanceType.CHECK_IN) {
                "${emp.name} VÀO CA (${method.displayName})"
            } else {
                "${emp.name} TAN CA (+${durationHours}h)"
            }
            RealtimeSyncManager.broadcast("ATTENDANCE_RECORD", payload, msg)
        }

        return resultRecord
    }

    // --- REALTIME REMOTE EVENT HANDLER ---
    /**
     * Executes when an instant UDP sync packet is received from another phone
     */
    private fun handleRemoteSyncEvent(type: String, payload: JSONObject, message: String) {
        try {
            when (type) {
                "TABLE_STATUS_CHANGED" -> {
                    val tableId = payload.optLong("tableId")
                    val tableName = payload.optString("tableName")
                    val statusStr = payload.optString("status")
                    val status = TableStatus.valueOf(statusStr)

                    val db = dbHelper.writableDatabase
                    val cv = ContentValues().apply { put("status", status.name) }
                    if (tableName.isNotBlank()) {
                        db.update(RestaurantDbHelper.TABLE_TABLES, cv, "id=? OR name=?", arrayOf(tableId.toString(), tableName))
                    } else {
                        db.update(RestaurantDbHelper.TABLE_TABLES, cv, "id=?", arrayOf(tableId.toString()))
                    }
                    notifyDataChanged("remote_table_status")
                }

                "ACTIVE_ORDER_UPDATED" -> {
                    val tableId = payload.optLong("tableId")
                    val tableName = payload.optString("tableName")
                    val itemsArray = payload.optJSONArray("items")
                    val items = mutableListOf<OrderItem>()

                    if (itemsArray != null) {
                        for (i in 0 until itemsArray.length()) {
                            val obj = itemsArray.getJSONObject(i)
                            items.add(
                                OrderItem(
                                    menuItemId = obj.optLong("menuItemId"),
                                    menuItemName = obj.optString("menuItemName"),
                                    price = obj.optDouble("price"),
                                    quantity = obj.optInt("quantity"),
                                    note = obj.optString("note")
                                )
                            )
                        }
                    }

                    val db = dbHelper.writableDatabase
                    if (items.isNotEmpty()) {
                        activeTableOrders[tableId] = items
                        val cv = ContentValues().apply { put("status", TableStatus.DANG_PHUC_VU.name) }
                        if (tableName.isNotBlank()) {
                            db.update(RestaurantDbHelper.TABLE_TABLES, cv, "id=? OR name=?", arrayOf(tableId.toString(), tableName))
                        } else {
                            db.update(RestaurantDbHelper.TABLE_TABLES, cv, "id=?", arrayOf(tableId.toString()))
                        }
                    } else {
                        activeTableOrders.remove(tableId)
                        val cv = ContentValues().apply { put("status", TableStatus.TRONG.name) }
                        if (tableName.isNotBlank()) {
                            db.update(RestaurantDbHelper.TABLE_TABLES, cv, "id=? OR name=?", arrayOf(tableId.toString(), tableName))
                        } else {
                            db.update(RestaurantDbHelper.TABLE_TABLES, cv, "id=?", arrayOf(tableId.toString()))
                        }
                    }
                    notifyDataChanged("remote_active_order")
                }

                "ORDER_CHECKOUT" -> {
                    val tableId = payload.optLong("tableId")
                    val tableName = payload.optString("tableName")
                    val customerName = payload.optString("customerName", "Khách vãng lai")
                    val customerTierName = payload.optString("customerTierName", "Bậc Đồng")
                    val employeeName = payload.optString("employeeName", "Nhân viên")
                    val subtotal = payload.optDouble("subtotal")
                    val discountPercent = payload.optDouble("discountPercent")
                    val paymentMethod = payload.optString("paymentMethod", "Tiền mặt")
                    val timestamp = payload.optLong("timestamp", System.currentTimeMillis())

                    val db = dbHelper.writableDatabase
                    db.beginTransaction()
                    try {
                        val cvOrder = ContentValues().apply {
                            put("table_id", tableId)
                            put("table_name", tableName)
                            put("customer_id", if (payload.has("customerId") && !payload.isNull("customerId")) payload.optLong("customerId") else null)
                            put("customer_name", customerName)
                            put("customer_tier_name", customerTierName)
                            put("employee_id", payload.optLong("employeeId", 0L))
                            put("employee_name", employeeName)
                            put("subtotal", subtotal)
                            put("discount_percent", discountPercent)
                            put("payment_method", paymentMethod)
                            put("status", OrderStatus.COMPLETED.name)
                            put("timestamp", timestamp)
                        }
                        val orderId = db.insert(RestaurantDbHelper.TABLE_ORDERS, null, cvOrder)

                        val itemsArray = payload.optJSONArray("items")
                        if (itemsArray != null && orderId != -1L) {
                            for (i in 0 until itemsArray.length()) {
                                val obj = itemsArray.getJSONObject(i)
                                val cvItem = ContentValues().apply {
                                    put("order_id", orderId)
                                    put("menu_item_id", obj.optLong("menuItemId"))
                                    put("menu_item_name", obj.optString("menuItemName"))
                                    put("price", obj.optDouble("price"))
                                    put("quantity", obj.optInt("quantity"))
                                    put("note", obj.optString("note"))
                                }
                                db.insert(RestaurantDbHelper.TABLE_ORDER_ITEMS, null, cvItem)
                            }
                        }

                        // Reset table to TRONG
                        val cvTable = ContentValues().apply {
                            put("status", TableStatus.TRONG.name)
                            put("current_order_id", null as Long?)
                        }
                        db.update(RestaurantDbHelper.TABLE_TABLES, cvTable, "id=?", arrayOf(tableId.toString()))
                        activeTableOrders.remove(tableId)

                        db.setTransactionSuccessful()
                    } finally {
                        db.endTransaction()
                    }
                    notifyDataChanged("remote_order_checkout")
                }

                "ATTENDANCE_RECORD" -> {
                    val empId = payload.optLong("employeeId")
                    val empName = payload.optString("employeeName")
                    val typeStr = payload.optString("type")
                    val methodStr = payload.optString("method")
                    val ts = payload.optLong("timestamp", System.currentTimeMillis())
                    val dur = payload.optDouble("durationHours", 0.0)
                    val note = payload.optString("note", "")

                    val db = dbHelper.writableDatabase
                    val cv = ContentValues().apply {
                        put("employee_id", empId)
                        put("employee_name", empName)
                        put("type", typeStr)
                        put("method", methodStr)
                        put("timestamp", ts)
                        put("duration_hours", dur)
                        put("note", note)
                    }
                    db.insert(RestaurantDbHelper.TABLE_ATTENDANCE, null, cv)
                    notifyDataChanged("remote_attendance")
                }

                "MENU_ADDED" -> {
                    val db = dbHelper.writableDatabase
                    val cv = ContentValues().apply {
                        put("name", payload.optString("name"))
                        put("category", payload.optString("category", "Món chính"))
                        put("price", payload.optDouble("price", 0.0))
                        put("description", payload.optString("description", ""))
                        put("is_available", if (payload.optBoolean("isAvailable", true)) 1 else 0)
                    }
                    db.insert(RestaurantDbHelper.TABLE_MENU_ITEMS, null, cv)
                    notifyDataChanged("remote_menu_added")
                }

                "MENU_UPDATED" -> {
                    val db = dbHelper.writableDatabase
                    val id = payload.optLong("id")
                    val name = payload.optString("name")
                    val cv = ContentValues().apply {
                        put("name", name)
                        put("category", payload.optString("category", "Món chính"))
                        put("price", payload.optDouble("price", 0.0))
                        put("description", payload.optString("description", ""))
                        put("is_available", if (payload.optBoolean("isAvailable", true)) 1 else 0)
                    }
                    if (id > 0) {
                        db.update(RestaurantDbHelper.TABLE_MENU_ITEMS, cv, "id=? OR name=?", arrayOf(id.toString(), name))
                    } else {
                        db.update(RestaurantDbHelper.TABLE_MENU_ITEMS, cv, "name=?", arrayOf(name))
                    }
                    notifyDataChanged("remote_menu_updated")
                }

                "MENU_DELETED" -> {
                    val db = dbHelper.writableDatabase
                    val id = payload.optLong("id")
                    val name = payload.optString("name")
                    if (id > 0) {
                        db.delete(RestaurantDbHelper.TABLE_MENU_ITEMS, "id=? OR name=?", arrayOf(id.toString(), name))
                    } else if (name.isNotBlank()) {
                        db.delete(RestaurantDbHelper.TABLE_MENU_ITEMS, "name=?", arrayOf(name))
                    }
                    notifyDataChanged("remote_menu_deleted")
                }

                "CUSTOMER_ADDED" -> {
                    val db = dbHelper.writableDatabase
                    val cv = ContentValues().apply {
                        put("name", payload.optString("name"))
                        put("phone", payload.optString("phone"))
                        put("email", payload.optString("email", ""))
                        put("total_spent", payload.optDouble("totalSpent", 0.0))
                        put("loyalty_points", payload.optInt("loyaltyPoints", 0))
                    }
                    db.insert(RestaurantDbHelper.TABLE_CUSTOMERS, null, cv)
                    notifyDataChanged("remote_customer_added")
                }

                "CUSTOMER_UPDATED" -> {
                    val db = dbHelper.writableDatabase
                    val id = payload.optLong("id")
                    val phone = payload.optString("phone")
                    val cv = ContentValues().apply {
                        put("name", payload.optString("name"))
                        put("email", payload.optString("email", ""))
                        put("total_spent", payload.optDouble("totalSpent", 0.0))
                        put("loyalty_points", payload.optInt("loyaltyPoints", 0))
                    }
                    if (id > 0) {
                        db.update(RestaurantDbHelper.TABLE_CUSTOMERS, cv, "id=? OR phone=?", arrayOf(id.toString(), phone))
                    } else {
                        db.update(RestaurantDbHelper.TABLE_CUSTOMERS, cv, "phone=?", arrayOf(phone))
                    }
                    notifyDataChanged("remote_customer_updated")
                }

                "CUSTOMER_DELETED" -> {
                    val db = dbHelper.writableDatabase
                    val id = payload.optLong("id")
                    val phone = payload.optString("phone")
                    val name = payload.optString("name")
                    if (phone.isNotBlank()) {
                        db.delete(RestaurantDbHelper.TABLE_CUSTOMERS, "id=? OR phone=? OR name=?", arrayOf(id.toString(), phone, name))
                    } else {
                        db.delete(RestaurantDbHelper.TABLE_CUSTOMERS, "id=?", arrayOf(id.toString()))
                    }
                    notifyDataChanged("remote_customer_deleted")
                }

                "EMPLOYEE_ADDED" -> {
                    val db = dbHelper.writableDatabase
                    val cv = ContentValues().apply {
                        put("name", payload.optString("name"))
                        put("phone", payload.optString("phone"))
                        put("position", payload.optString("position", "Nhân viên"))
                        put("tier", payload.optString("tier", "CHINH_THUC"))
                        put("contract_type", payload.optString("contractType", "MONTHLY"))
                        put("hourly_rate", payload.optDouble("hourlyRate", 25000.0))
                        put("monthly_salary", payload.optDouble("monthlySalary", 8000000.0))
                        put("worked_hours", payload.optDouble("workedHours", 0.0))
                        put("worked_days", payload.optDouble("workedDays", 0.0))
                        put("bonus", payload.optDouble("bonus", 0.0))
                        put("penalty", payload.optDouble("penalty", 0.0))
                    }
                    db.insert(RestaurantDbHelper.TABLE_EMPLOYEES, null, cv)
                    notifyDataChanged("remote_employee_added")
                }

                "EMPLOYEE_UPDATED" -> {
                    val db = dbHelper.writableDatabase
                    val id = payload.optLong("id")
                    val phone = payload.optString("phone")
                    val cv = ContentValues().apply {
                        put("name", payload.optString("name"))
                        put("position", payload.optString("position"))
                        put("tier", payload.optString("tier"))
                        put("contract_type", payload.optString("contractType"))
                        put("hourly_rate", payload.optDouble("hourlyRate"))
                        put("monthly_salary", payload.optDouble("monthlySalary"))
                        put("worked_hours", payload.optDouble("workedHours"))
                        put("worked_days", payload.optDouble("workedDays"))
                        put("bonus", payload.optDouble("bonus"))
                        put("penalty", payload.optDouble("penalty"))
                    }
                    if (id > 0) {
                        db.update(RestaurantDbHelper.TABLE_EMPLOYEES, cv, "id=? OR phone=?", arrayOf(id.toString(), phone))
                    } else {
                        db.update(RestaurantDbHelper.TABLE_EMPLOYEES, cv, "phone=?", arrayOf(phone))
                    }
                    notifyDataChanged("remote_employee_updated")
                }

                "EMPLOYEE_DELETED" -> {
                    val db = dbHelper.writableDatabase
                    val id = payload.optLong("id")
                    val phone = payload.optString("phone")
                    val name = payload.optString("name")
                    if (phone.isNotBlank()) {
                        db.delete(RestaurantDbHelper.TABLE_EMPLOYEES, "id=? OR phone=? OR name=?", arrayOf(id.toString(), phone, name))
                    } else {
                        db.delete(RestaurantDbHelper.TABLE_EMPLOYEES, "id=?", arrayOf(id.toString()))
                    }
                    notifyDataChanged("remote_employee_deleted")
                }

                "TABLE_ADDED" -> {
                    val db = dbHelper.writableDatabase
                    val cv = ContentValues().apply {
                        put("name", payload.optString("name"))
                        put("floor", payload.optString("floor", "Tầng 1"))
                        put("seats", payload.optInt("seats", 4))
                        put("status", payload.optString("status", "TRONG"))
                    }
                    db.insert(RestaurantDbHelper.TABLE_TABLES, null, cv)
                    notifyDataChanged("remote_table_added")
                }

                "TABLE_DELETED" -> {
                    val db = dbHelper.writableDatabase
                    val id = payload.optLong("id")
                    val name = payload.optString("name")
                    if (name.isNotBlank()) {
                        db.delete(RestaurantDbHelper.TABLE_TABLES, "id=? OR name=?", arrayOf(id.toString(), name))
                    } else {
                        db.delete(RestaurantDbHelper.TABLE_TABLES, "id=?", arrayOf(id.toString()))
                    }
                    notifyDataChanged("remote_table_deleted")
                }
            }
        } catch (e: Exception) {
            Log.e("RestaurantRepository", "Error handling remote sync event", e)
        }
    }
}
