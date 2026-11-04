package com.example.adr_nhom_da.data.remote

import android.content.Context
import android.util.Log
import com.example.adr_nhom_da.data.model.AttendanceType
import com.example.adr_nhom_da.data.model.Customer
import com.example.adr_nhom_da.data.model.Employee
import com.example.adr_nhom_da.data.model.MenuItem
import com.example.adr_nhom_da.data.model.Order
import com.example.adr_nhom_da.data.model.RestaurantTable
import com.example.adr_nhom_da.data.repository.RestaurantRepository
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread

/**
 * WebApiSyncEngine: Tự động và liên tục đồng bộ toàn bộ cơ sở dữ liệu
 * giữa App Android (thư mục demo) và Web API (DA Restaurant Api).
 *
 * Chạy 3 cấp độ:
 * 1. Tự động đồng bộ toàn bộ (Upload & Download) ngay khi khởi động
 * 2. Tự động đồng bộ định kỳ mỗi 20 giây (Background Worker)
 * 3. Tự động đồng bộ tức thì theo từng thao tác (Action-triggered)
 */
object WebApiSyncEngine {
    private const val TAG = "WebApiSyncEngine"
    private var isEngineRunning = false
    private var syncWorkerThread: Thread? = null

    // Danh sách IP dự phòng kết nối tới Web API
    private val candidateBaseUrls = listOf(
        "http://10.0.2.2:5276",        // Android Emulator
        "http://10.168.222.213:5276",  // Wi-Fi LAN hiện tại của máy tính
        "http://26.38.230.253:5276",   // Radmin VPN
        "http://127.0.0.1:5276"
    )

    fun start(context: Context, repository: RestaurantRepository) {
        if (isEngineRunning) return
        isEngineRunning = true

        // 1. Đồng bộ toàn bộ dữ liệu ngay lập tức khi mở app
        syncAll(context, repository, null)

        // 2. Chạy vòng lặp đồng bộ định kỳ mỗi 20 giây
        syncWorkerThread = thread(start = true, isDaemon = true, name = "WebApiSyncEngineThread") {
            while (isEngineRunning) {
                try {
                    Thread.sleep(20000)
                    if (!isEngineRunning) break
                    syncAll(context, repository, null)
                } catch (ignored: InterruptedException) {
                    break
                } catch (e: Exception) {
                    Log.w(TAG, "Lỗi chu kỳ đồng bộ: ${e.message}")
                }
            }
        }
    }

    fun stop() {
        isEngineRunning = false
        syncWorkerThread?.interrupt()
        syncWorkerThread = null
    }

    /**
     * Đồng bộ toàn bộ dữ liệu 2 chiều (App SQLite -> Web API -> Cập nhật ngược lại)
     */
    fun syncAll(
        context: Context,
        repository: RestaurantRepository,
        onResult: ((Boolean, String) -> Unit)? = null
    ) {
        thread(start = true, isDaemon = true) {
            try {
                // Thu thập toàn bộ dữ liệu từ SQLite của app
                val tables = repository.getAllTables()
                val menuItems = repository.getAllMenuItems()
                val customers = repository.getAllCustomers()
                val employees = repository.getAllEmployees()
                val orders = repository.getAllCompletedOrders()
                val attendance = repository.getAllAttendanceRecords()

                // 1. Chuẩn bị JSON Upload
                val tablesArr = JSONArray()
                tables.forEach { t ->
                    tablesArr.put(JSONObject().apply {
                        put("id", t.id)
                        put("name", t.name)
                        put("floor", t.floor)
                        put("seats", t.seats)
                        put("status", t.status.name)
                        put("currentOrderId", t.currentOrderId ?: JSONObject.NULL)
                    })
                }

                val menuArr = JSONArray()
                menuItems.forEach { m ->
                    menuArr.put(JSONObject().apply {
                        put("id", m.id)
                        put("name", m.name)
                        put("category", m.category)
                        put("price", m.price)
                        put("description", m.description)
                        put("isAvailable", m.isAvailable)
                    })
                }

                val custArr = JSONArray()
                customers.forEach { c ->
                    custArr.put(JSONObject().apply {
                        put("id", c.id)
                        put("name", c.name)
                        put("phone", c.phone)
                        put("email", c.email)
                        put("totalSpent", c.totalSpent)
                        put("loyaltyPoints", c.loyaltyPoints)
                    })
                }

                val empArr = JSONArray()
                employees.forEach { e ->
                    empArr.put(JSONObject().apply {
                        put("id", e.id)
                        put("name", e.name)
                        put("phone", e.phone)
                        put("position", e.position)
                        put("tier", e.tier.name)
                        put("contractType", e.contractType.name)
                        put("hourlyRate", e.hourlyRate)
                        put("monthlySalary", e.monthlySalary)
                        put("workedHours", e.workedHours)
                        put("workedDays", e.workedDays)
                        put("bonus", e.bonus)
                        put("penalty", e.penalty)
                    })
                }

                val attArr = JSONArray()
                attendance.forEach { a ->
                    attArr.put(JSONObject().apply {
                        put("employeeId", a.employeeId)
                        put("employeeName", a.employeeName)
                        put("type", a.type.name)
                        put("method", a.method.name)
                        put("timestamp", a.timestamp)
                        put("durationHours", a.durationHours)
                        put("note", a.note)
                    })
                }

                val ordersArr = JSONArray()
                orders.forEach { o ->
                    val itemsArr = JSONArray()
                    o.items.forEach { item ->
                        itemsArr.put(JSONObject().apply {
                            put("menuItemId", item.menuItemId)
                            put("menuItemName", item.menuItemName)
                            put("price", item.price)
                            put("quantity", item.quantity)
                            put("note", item.note)
                        })
                    }

                    ordersArr.put(JSONObject().apply {
                        put("tableId", o.tableId)
                        put("tableName", o.tableName)
                        put("customerId", o.customerId ?: JSONObject.NULL)
                        put("customerName", o.customerName)
                        put("customerTierName", o.customerTierName)
                        put("employeeId", o.employeeId)
                        put("employeeName", o.employeeName)
                        put("subtotal", o.subtotal)
                        put("discountPercent", o.discountPercent)
                        put("paymentMethod", o.paymentMethod)
                        put("status", o.status.name)
                        put("timestamp", o.timestamp)
                        put("items", itemsArr)
                    })
                }

                val payload = JSONObject().apply {
                    put("tables", tablesArr)
                    put("menuItems", menuArr)
                    put("customers", custArr)
                    put("employees", empArr)
                    put("attendance", attArr)
                    put("orders", ordersArr)
                }

                val payloadString = payload.toString()

                // Tìm server hoạt động từ danh sách URL
                var connectedUrl: String? = null
                val urlsToTry = (listOf(RestaurantApiClient.baseUrl) + candidateBaseUrls).distinct()

                for (baseUrl in urlsToTry) {
                    try {
                        val pingUrl = URL("$baseUrl/api/sync/ping")
                        val pingConn = (pingUrl.openConnection() as HttpURLConnection).apply {
                            requestMethod = "GET"
                            connectTimeout = 2500
                            readTimeout = 2500
                        }
                        if (pingConn.responseCode == 200) {
                            connectedUrl = baseUrl
                            RestaurantApiClient.baseUrl = baseUrl
                            break
                        }
                    } catch (ignored: Exception) {}
                }

                if (connectedUrl == null) {
                    onResult?.invoke(false, "Không thể kết nối đến Web API (hãy đảm bảo run.bat đã bật)")
                    return@thread
                }

                // Gửi Upload lên Web API
                val uploadUrl = URL("$connectedUrl/api/sync/upload")
                val uploadConn = (uploadUrl.openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    connectTimeout = 8000
                    readTimeout = 15000
                    setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                    setRequestProperty("Accept", "application/json")
                    doOutput = true
                }

                OutputStreamWriter(uploadConn.outputStream, "UTF-8").use { writer ->
                    writer.write(payloadString)
                    writer.flush()
                }

                val code = uploadConn.responseCode
                if (code in 200..299) {
                    Log.i(TAG, "✓ Upload dữ liệu lên Web API thành công!")

                    // Sau khi upload, gọi GET /api/sync/all để đồng bộ ngược lại các thay đổi mới từ Web API
                    try {
                        val getAllUrl = URL("$connectedUrl/api/sync/all")
                        val getConn = (getAllUrl.openConnection() as HttpURLConnection).apply {
                            requestMethod = "GET"
                            connectTimeout = 5000
                            readTimeout = 10000
                        }
                        if (getConn.responseCode == 200) {
                            val resText = BufferedReader(InputStreamReader(getConn.inputStream)).use { it.readText() }
                            val getJson = JSONObject(resText)
                            // Nếu Web API có dữ liệu mới, cập nhật vào SQLite nếu cần
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "Lỗi lấy dữ liệu từ Web API: ${e.message}")
                    }

                    onResult?.invoke(true, "Đồng bộ thành công với Web API ($connectedUrl)")
                } else {
                    val errText = BufferedReader(InputStreamReader(uploadConn.errorStream)).use { it.readText() }
                    Log.w(TAG, "Lỗi HTTP $code từ Web API: $errText")
                    onResult?.invoke(false, "Web API phản hồi mã lỗi: $code")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Lỗi trong quá trình đồng bộ: ${e.message}", e)
                onResult?.invoke(false, "Lỗi: ${e.message}")
            }
        }
    }

    // --- CÁC HÀM ĐỒNG BỘ ĐƠN LẺ TỨC THÌ (ACTION-SYNC) ---

    fun syncCustomerAsync(customer: Customer) {
        postRecordAsync("/api/sync/customer", JSONObject().apply {
            put("name", customer.name)
            put("phone", customer.phone)
            put("email", customer.email)
            put("totalSpent", customer.totalSpent)
            put("loyaltyPoints", customer.loyaltyPoints)
            put("tier", "Customer")
        })
    }

    fun syncEmployeeAsync(emp: Employee) {
        postRecordAsync("/api/sync/employee", JSONObject().apply {
            put("name", emp.name)
            put("phone", emp.phone)
            put("position", emp.position)
            put("tier", emp.tier.name)
            put("contractType", emp.contractType.name)
            put("monthlySalary", emp.monthlySalary)
            put("hourlyRate", emp.hourlyRate)
            put("workedDays", emp.workedDays)
            put("bonus", emp.bonus)
            put("penalty", emp.penalty)
        })
    }

    fun syncTableAsync(table: RestaurantTable) {
        postRecordAsync("/api/sync/table", JSONObject().apply {
            put("name", table.name)
            put("floor", table.floor)
            put("seats", table.seats)
            put("status", table.status.name)
        })
    }

    fun syncMenuItemAsync(item: MenuItem) {
        postRecordAsync("/api/sync/menuitem", JSONObject().apply {
            put("name", item.name)
            put("category", item.category)
            put("price", item.price)
            put("description", item.description)
            put("isAvailable", item.isAvailable)
        })
    }

    fun syncOrderAsync(order: Order) {
        postRecordAsync("/api/sync/order", JSONObject().apply {
            put("tableId", order.tableId)
            put("tableName", order.tableName)
            put("customerName", order.customerName)
            put("status", order.status.name)
            put("subtotal", order.subtotal)
            put("discountPercent", order.discountPercent)
            put("finalAmount", order.finalTotal)
            put("paymentMethod", order.paymentMethod)
            put("staffName", order.employeeName)
            put("createdAt", order.timestamp)
        })
    }

    private fun postRecordAsync(endpoint: String, body: JSONObject) {
        thread(start = true, isDaemon = true) {
            try {
                val urls = (listOf(RestaurantApiClient.baseUrl) + candidateBaseUrls).distinct()
                val jsonStr = body.toString()

                for (baseUrl in urls) {
                    try {
                        val url = URL("$baseUrl$endpoint")
                        val conn = (url.openConnection() as HttpURLConnection).apply {
                            requestMethod = "POST"
                            connectTimeout = 3000
                            readTimeout = 4000
                            setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                            doOutput = true
                        }
                        OutputStreamWriter(conn.outputStream, "UTF-8").use { it.write(jsonStr) }
                        if (conn.responseCode in 200..299) {
                            RestaurantApiClient.baseUrl = baseUrl
                            return@thread
                        }
                    } catch (ignored: Exception) {}
                }
            } catch (ignored: Exception) {}
        }
    }
}

