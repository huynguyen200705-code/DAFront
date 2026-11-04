package com.example.adr_nhom_da.data.remote

import android.content.Context
import android.util.Log
import com.example.adr_nhom_da.data.model.AttendanceRecord
import com.example.adr_nhom_da.data.model.AttendanceType
import com.example.adr_nhom_da.data.model.Customer
import com.example.adr_nhom_da.data.model.Employee
import com.example.adr_nhom_da.data.model.MenuItem
import com.example.adr_nhom_da.data.model.Order
import com.example.adr_nhom_da.data.model.RestaurantTable
import com.example.adr_nhom_da.data.model.User
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
 * giữa App Android và Web API (https://dafront-1.onrender.com & Local API).
 *
 * Chạy 3 cấp độ:
 * 1. Tự động đồng bộ toàn bộ (Upload & Download 2 chiều) ngay khi khởi động
 * 2. Tự động đồng bộ định kỳ mỗi 15 giây (Background Worker)
 * 3. Tự động đồng bộ tức thì theo từng thao tác (Action-triggered)
 */
object WebApiSyncEngine {
    private const val TAG = "WebApiSyncEngine"
    private var isEngineRunning = false
    private var syncWorkerThread: Thread? = null

    // Danh sách URL song song: Cloud Render đặt trước, sau đó là Local Wi-Fi PC
    val candidateBaseUrls: List<String>
        get() = listOfNotNull(
            "https://dafront-1.onrender.com",
            RestaurantApiClient.cloudUrl,
            RestaurantApiClient.baseUrl,
            RestaurantApiClient.localUrl,
            "http://192.168.0.172:5276",
            "http://10.168.222.213:5276",
            RestaurantApiClient.emulatorUrl,
            "http://10.0.2.2:5276",
            RestaurantApiClient.radminUrl,
            "http://26.38.230.253:5276",
            "http://127.0.0.1:5276"
        ).map { RestaurantApiClient.cleanUrl(it) }.distinct().filter { it.isNotBlank() }

    fun start(context: Context, repository: RestaurantRepository) {
        if (isEngineRunning) return
        isEngineRunning = true

        // 1. Đồng bộ toàn bộ dữ liệu 2 chiều ngay lập tức khi mở app
        syncAll(context, repository, null)

        // 2. Chạy vòng lặp đồng bộ định kỳ mỗi 15 giây
        syncWorkerThread = thread(start = true, isDaemon = true, name = "WebApiSyncEngineThread") {
            while (isEngineRunning) {
                try {
                    Thread.sleep(15000)
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
     * Đồng bộ toàn bộ dữ liệu 2 chiều (App SQLite <-> Web API)
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
                val users = repository.getAllUsers()

                // 1. Chuẩn bị JSON Upload
                val tablesArr = JSONArray()
                tables.forEach { t ->
                    tablesArr.put(JSONObject().apply {
                        put("id", t.id)
                        put("name", t.name)
                        put("floor", t.floor)
                        put("seats", t.seats)
                        put("capacity", t.seats)
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

                val usersArr = JSONArray()
                users.forEach { u ->
                    usersArr.put(JSONObject().apply {
                        put("username", u.username)
                        put("password", u.password)
                        put("fullName", u.fullName)
                        put("phone", u.phone)
                        put("role", u.role.name)
                        put("faceIdData", u.faceIdData ?: "")
                    })
                }

                val payload = JSONObject().apply {
                    put("tables", tablesArr)
                    put("menuItems", menuArr)
                    put("customers", custArr)
                    put("employees", empArr)
                    put("attendance", attArr)
                    put("orders", ordersArr)
                    put("users", usersArr)
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
                            connectTimeout = 4000
                            readTimeout = 4000
                        }
                        if (pingConn.responseCode == 200) {
                            connectedUrl = baseUrl
                            RestaurantApiClient.baseUrl = baseUrl
                            RestaurantApiClient.activeServerLabel = when {
                                baseUrl.startsWith("https://") -> "🌐 Cloud Web API ($baseUrl)"
                                baseUrl.contains("10.0.2.2") -> "💻 Android Emulator ($baseUrl)"
                                else -> "🏠 Local Web API ($baseUrl)"
                            }
                            Log.i(TAG, "Đã liên kết song song Web API: ${RestaurantApiClient.activeServerLabel}")
                            break
                        }
                    } catch (ignored: Exception) {}
                }

                if (connectedUrl == null) {
                    onResult?.invoke(false, "Không thể kết nối Web API (kiểm tra mạng Internet hoặc server Render)")
                    return@thread
                }

                // 2. Gửi Upload đồng thời lên TẤT CẢ máy chủ (cả Cloud Render và Local PC)
                var uploadSuccessCount = 0
                for (targetUrl in urlsToTry) {
                    try {
                        val uploadUrl = URL("$targetUrl/api/sync/upload")
                        val uploadConn = (uploadUrl.openConnection() as HttpURLConnection).apply {
                            requestMethod = "POST"
                            connectTimeout = 6000
                            readTimeout = 15000
                            setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                            setRequestProperty("Accept", "application/json")
                            doOutput = true
                        }
                        OutputStreamWriter(uploadConn.outputStream, "UTF-8").use { writer ->
                            writer.write(payloadString)
                            writer.flush()
                        }
                        if (uploadConn.responseCode in 200..299) {
                            uploadSuccessCount++
                            Log.i(TAG, "Đã upload toàn bộ CSDL lên: $targetUrl")
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "Không thể upload tới $targetUrl: ${e.message}")
                    }
                }

                val code = if (uploadSuccessCount > 0) 200 else 500
                if (code in 200..299) {
                    Log.i(TAG, "✓ Upload dữ liệu lên Web API thành công!")

                    // 3. Gọi GET /api/sync/all để đồng bộ ngược lại các thay đổi mới nhất từ Web API vào app SQLite
                    try {
                        val getAllUrl = URL("$connectedUrl/api/sync/all")
                        val getConn = (getAllUrl.openConnection() as HttpURLConnection).apply {
                            requestMethod = "GET"
                            connectTimeout = 6000
                            readTimeout = 12000
                        }
                        if (getConn.responseCode == 200) {
                            val resText = BufferedReader(InputStreamReader(getConn.inputStream)).use { it.readText() }
                            val getJson = JSONObject(resText)
                            repository.applyFullSyncSnapshot(getJson, notify = true)
                            Log.i(TAG, "✓ Đã gộp dữ liệu từ $connectedUrl vào CSDL nội bộ!")
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "Lỗi lấy dữ liệu /api/sync/all từ Web API: ${e.message}")
                    }

                    // 4. Đồng bộ thêm danh sách User từ /api/restaurant-auth/users nếu cần
                    try {
                        val usersUrl = URL("$connectedUrl/api/restaurant-auth/users")
                        val uConn = (usersUrl.openConnection() as HttpURLConnection).apply {
                            requestMethod = "GET"
                            connectTimeout = 4000
                            readTimeout = 8000
                        }
                        if (uConn.responseCode == 200) {
                            val uText = BufferedReader(InputStreamReader(uConn.inputStream)).use { it.readText() }
                            val uJson = JSONObject(uText)
                            if (uJson.optBoolean("success", false)) {
                                val uArr = uJson.optJSONArray("data")
                                if (uArr != null && uArr.length() > 0) {
                                    repository.applyFullSyncSnapshot(JSONObject().put("users", uArr), notify = false)
                                }
                            }
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "Lỗi lấy users từ Web API: ${e.message}")
                    }

                    // 5. Đồng bộ thêm danh sách Chấm công từ /api/attendance nếu có dữ liệu mới
                    try {
                        val attUrl = URL("$connectedUrl/api/attendance")
                        val aConn = (attUrl.openConnection() as HttpURLConnection).apply {
                            requestMethod = "GET"
                            connectTimeout = 4000
                            readTimeout = 8000
                        }
                        if (aConn.responseCode == 200) {
                            val aText = BufferedReader(InputStreamReader(aConn.inputStream)).use { it.readText() }
                            val aJson = JSONObject(aText)
                            if (aJson.optBoolean("success", false)) {
                                val aArr = aJson.optJSONArray("data")
                                if (aArr != null && aArr.length() > 0) {
                                    repository.applyFullSyncSnapshot(JSONObject().put("attendance", aArr), notify = false)
                                }
                            }
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "Lỗi lấy attendance từ Web API: ${e.message}")
                    }

                    onResult?.invoke(true, "Đồng bộ thành công song song với Web API ($connectedUrl)")
                } else {
                    Log.w(TAG, "Lỗi HTTP $code từ Web API")
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
            put("vatPercent", 0.0)
            put("finalAmount", order.finalTotal)
            put("paymentMethod", order.paymentMethod)
            put("staffName", order.employeeName)
            put("createdAt", order.timestamp)
        })
    }

    fun syncUserAsync(user: User) {
        postRecordAsync("/api/sync/user", JSONObject().apply {
            put("username", user.username)
            put("password", user.password)
            put("fullName", user.fullName)
            put("phone", user.phone)
            put("role", user.role.name)
            put("faceIdData", user.faceIdData ?: "")
        })
    }

    fun syncAttendanceAsync(record: AttendanceRecord) {
        postRecordAsync("/api/attendance/record", JSONObject().apply {
            put("employeeId", record.employeeId)
            put("employeeName", record.employeeName)
            put("type", record.type.name)
            put("method", record.method.name)
            put("timestamp", record.timestamp)
            put("durationHours", record.durationHours)
            put("note", record.note)
        })
    }

    private fun postRecordAsync(endpoint: String, body: JSONObject) {
        thread(start = true, isDaemon = true) {
            try {
                val urls = (listOf(RestaurantApiClient.baseUrl) + candidateBaseUrls).distinct()
                val jsonStr = body.toString()

                // Đồng bộ thay đổi tức thời tới TẤT CẢ máy chủ đang bật (cả Cloud Render và Local)
                for (baseUrl in urls) {
                    try {
                        val url = URL("$baseUrl$endpoint")
                        val conn = (url.openConnection() as HttpURLConnection).apply {
                            requestMethod = "POST"
                            connectTimeout = 4000
                            readTimeout = 6000
                            setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                            doOutput = true
                        }
                        OutputStreamWriter(conn.outputStream, "UTF-8").use { it.write(jsonStr) }
                        val code = conn.responseCode
                        if (code in 200..299) {
                            Log.d(TAG, "Đồng bộ record thành công tới: $baseUrl$endpoint")
                        }
                    } catch (ignored: Exception) {}
                }
            } catch (ignored: Exception) {}
        }
    }
}
