package com.example.adr_nhom_da.data.remote

import com.example.adr_nhom_da.data.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

object RestaurantApiClient {
    // URL Web API máy chủ đang kết nối (Tự động chuyển đổi giữa Local và Cloud)
    var baseUrl: String = "https://dafront-1.onrender.com"
    var localUrl: String = "http://192.168.0.172:5276"
    var cloudUrl: String = "https://dafront-1.onrender.com"
    var emulatorUrl: String = "http://10.0.2.2:5276"
    var radminUrl: String = "http://26.38.230.253:5276"
    var activeServerLabel: String = "🌐 Cloud Web API (https://dafront-1.onrender.com)"
    var authToken: String? = null

    fun cleanUrl(rawUrl: String): String {
        var cleaned = rawUrl.trim()
        while (cleaned.endsWith("/")) {
            cleaned = cleaned.dropLast(1).trim()
        }
        if (cleaned.endsWith("/api")) {
            cleaned = cleaned.removeSuffix("/api").trim()
        }
        while (cleaned.endsWith("/")) {
            cleaned = cleaned.dropLast(1).trim()
        }
        return cleaned
    }

    fun updateBaseUrl(rawUrl: String) {
        baseUrl = cleanUrl(rawUrl)
    }

    fun saveConfig(context: android.content.Context, newCloudUrl: String, newLocalUrl: String) {
        val cleanedCloud = cleanUrl(newCloudUrl)
        val cleanedLocal = cleanUrl(newLocalUrl)
        if (cleanedCloud.isNotBlank()) {
            cloudUrl = cleanedCloud
            baseUrl = cleanedCloud
        }
        if (cleanedLocal.isNotBlank()) localUrl = cleanedLocal

        context.getSharedPreferences("app_api_config", android.content.Context.MODE_PRIVATE)
            .edit()
            .putString("cloud_url", cleanedCloud)
            .putString("local_url", cleanedLocal)
            .apply()
    }

    fun init(context: android.content.Context) {
        try {
            val prefs = context.getSharedPreferences("app_api_config", android.content.Context.MODE_PRIVATE)
            var savedCloud = prefs.getString("cloud_url", "") ?: ""
            val savedLocal = prefs.getString("local_url", "") ?: ""

            // Tự động nâng cấp URL cũ lên endpoint deploy mới trên Render
            if (savedCloud.isBlank() || savedCloud.contains("da-restaurant-api.onrender.com")) {
                savedCloud = "https://dafront-1.onrender.com"
                prefs.edit().putString("cloud_url", savedCloud).apply()
            }

            if (savedCloud.isNotBlank()) {
                cloudUrl = cleanUrl(savedCloud)
                baseUrl = cloudUrl
            }
            if (savedLocal.isNotBlank()) localUrl = cleanUrl(savedLocal)

            context.assets.open(".env").bufferedReader().useLines { lines ->
                for (line in lines) {
                    val trimmed = line.trim()
                    if (trimmed.startsWith("#") || trimmed.isBlank()) continue

                    if (trimmed.startsWith("BASE_URL=") && savedCloud.isBlank()) {
                        val v = trimmed.substringAfter("BASE_URL=").trim()
                        if (v.isNotBlank()) updateBaseUrl(v)
                    } else if (trimmed.startsWith("LOCAL_URL=") && savedLocal.isBlank()) {
                        val v = trimmed.substringAfter("LOCAL_URL=").trim()
                        if (v.isNotBlank()) localUrl = cleanUrl(v)
                    } else if (trimmed.startsWith("CLOUD_URL=") && savedCloud.isBlank()) {
                        val v = trimmed.substringAfter("CLOUD_URL=").trim()
                        if (v.isNotBlank()) cloudUrl = cleanUrl(v)
                    } else if (trimmed.startsWith("EMULATOR_URL=")) {
                        val v = trimmed.substringAfter("EMULATOR_URL=").trim()
                        if (v.isNotBlank()) emulatorUrl = cleanUrl(v)
                    } else if (trimmed.startsWith("RADMIN_URL=")) {
                        val v = trimmed.substringAfter("RADMIN_URL=").trim()
                        if (v.isNotBlank()) radminUrl = cleanUrl(v)
                    }
                }
            }

            if (cloudUrl.isNotBlank()) {
                baseUrl = cloudUrl
            }
        } catch (e: Exception) {
            // Keep default baseUrl if .env is missing
        }
    }

    suspend fun ping(): Boolean = withContext(Dispatchers.IO) {
        try {
            val url = URL("$baseUrl/api/sync/ping")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 4000
                readTimeout = 4000
            }
            conn.responseCode == 200
        } catch (e: Exception) {
            false
        }
    }

    suspend fun login(username: String, password: String): User? = withContext(Dispatchers.IO) {
        try {
            val url = URL("$baseUrl/api/restaurant-auth/login")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                setRequestProperty("Content-Type", "application/json")
                doOutput = true
                connectTimeout = 6000
                readTimeout = 6000
            }

            val body = JSONObject().apply {
                put("username", username)
                put("password", password)
            }

            OutputStreamWriter(conn.outputStream).use { it.write(body.toString()) }

            if (conn.responseCode == 200) {
                val response = readStream(conn)
                val json = JSONObject(response)
                if (json.optBoolean("success", false)) {
                    authToken = json.optString("token")
                    val userObj = json.getJSONObject("user")
                    return@withContext User(
                        id = userObj.optLong("id"),
                        username = userObj.optString("username"),
                        password = "",
                        fullName = userObj.optString("fullName"),
                        phone = userObj.optString("phone"),
                        role = UserRole.valueOf(userObj.optString("role", "USER"))
                    )
                }
            }
            null
        } catch (e: Exception) {
            null
        }
    }

    suspend fun fetchAllTables(): List<RestaurantTable>? = withContext(Dispatchers.IO) {
        try {
            val url = URL("$baseUrl/api/tables")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 6000
                readTimeout = 6000
            }
            if (conn.responseCode == 200) {
                val json = JSONObject(readStream(conn))
                val array = json.getJSONArray("data")
                val list = mutableListOf<RestaurantTable>()
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    list.add(
                        RestaurantTable(
                            id = obj.getLong("id"),
                            name = obj.getString("name"),
                            floor = obj.optString("floor", "Tầng 1"),
                            seats = obj.optInt("seats", 4),
                            status = TableStatus.valueOf(obj.getString("status")),
                            currentOrderId = if (obj.isNull("currentOrderId")) null else obj.getLong("currentOrderId")
                        )
                    )
                }
                return@withContext list
            }
            null
        } catch (e: Exception) {
            null
        }
    }

    suspend fun checkoutOrder(order: Order): Long? = withContext(Dispatchers.IO) {
        try {
            val url = URL("$baseUrl/api/orders/checkout")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                setRequestProperty("Content-Type", "application/json")
                doOutput = true
                connectTimeout = 8000
                readTimeout = 8000
            }

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

            val body = JSONObject().apply {
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
                put("items", itemsArray)
            }

            OutputStreamWriter(conn.outputStream).use { it.write(body.toString()) }

            if (conn.responseCode == 200) {
                val json = JSONObject(readStream(conn))
                if (json.optBoolean("success", false)) {
                    return@withContext json.optLong("orderId")
                }
            }
            null
        } catch (e: Exception) {
            null
        }
    }

    suspend fun recordAttendance(
        employeeId: Long,
        type: AttendanceType,
        method: AttendanceMethod,
        note: String
    ): AttendanceRecord? = withContext(Dispatchers.IO) {
        try {
            val endpoint = if (type == AttendanceType.CHECK_IN) "check-in" else "check-out"
            val url = URL("$baseUrl/api/attendance/$endpoint")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                setRequestProperty("Content-Type", "application/json")
                doOutput = true
                connectTimeout = 6000
                readTimeout = 6000
            }

            val body = JSONObject().apply {
                put("employeeId", employeeId)
                put("method", method.name)
                put("note", note)
            }

            OutputStreamWriter(conn.outputStream).use { it.write(body.toString()) }

            if (conn.responseCode == 200) {
                val json = JSONObject(readStream(conn))
                if (json.optBoolean("success", false)) {
                    val data = json.getJSONObject("data")
                    return@withContext AttendanceRecord(
                        id = data.optLong("id"),
                        employeeId = data.optLong("employeeId"),
                        employeeName = data.optString("employeeName"),
                        type = AttendanceType.valueOf(data.optString("type")),
                        method = AttendanceMethod.valueOf(data.optString("method")),
                        timestamp = data.optLong("timestamp"),
                        durationHours = data.optDouble("durationHours", 0.0),
                        note = data.optString("note", "")
                    )
                }
            }
            null
        } catch (e: Exception) {
            null
        }
    }

    private fun readStream(conn: HttpURLConnection): String {
        val stream = if (conn.responseCode in 200..299) conn.inputStream else conn.errorStream
        return BufferedReader(InputStreamReader(stream)).use { it.readText() }
    }
}
