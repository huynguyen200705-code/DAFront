package com.example.adr_nhom_da.data.remote

import android.content.Context
import android.net.wifi.WifiManager
import android.os.Build
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.net.*
import java.util.UUID

object RealtimeSyncManager {
    private const val TAG = "RealtimeSyncManager"
    const val SYNC_TOPIC = "goldenspoon_pos/realtime_sync"
    const val LOCAL_UDP_PORT = 9876

    val deviceId: String = UUID.randomUUID().toString().take(8)
    val deviceName: String = "${Build.MANUFACTURER.replaceFirstChar { it.uppercase() }} ${Build.MODEL}"

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val _syncNotification = MutableSharedFlow<String>(extraBufferCapacity = 32)
    val syncNotification: SharedFlow<String> = _syncNotification

    private val _isCloudConnected = MutableStateFlow(false)
    val isCloudConnected: StateFlow<Boolean> = _isCloudConnected

    private val _onlineDevices = MutableStateFlow<Set<String>>(emptySet())
    val onlineDevices: StateFlow<Set<String>> = _onlineDevices

    var onRemoteEventReceived: ((type: String, payload: JSONObject, message: String) -> Unit)? = null

    private var mqttSocket: Socket? = null
    private var mqttOutputStream: OutputStream? = null
    private var isRunning = false
    private var multicastLock: WifiManager.MulticastLock? = null

    fun start(context: Context) {
        if (isRunning) return
        isRunning = true

        // Acquire Wi-Fi Multicast Lock
        try {
            val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
            multicastLock = wifiManager?.createMulticastLock("RestaurantSyncLock")?.apply {
                setReferenceCounted(true)
                acquire()
            }
        } catch (e: Exception) {
            Log.w(TAG, "MulticastLock warning: ${e.message}")
        }

        // 1. Start Cloud Real-time MQTT connection (Works on 4G, Wi-Fi, anywhere)
        scope.launch {
            runCloudMqttLoop()
        }

        // 2. Start Local UDP Broadcast listener (Backup for offline local networks)
        scope.launch {
            runLocalUdpListener()
        }

        // Broadcast join ping after a brief delay
        scope.launch {
            delay(1500)
            sendPing()
        }
    }

    fun stop() {
        isRunning = false
        _isCloudConnected.value = false
        try {
            mqttSocket?.close()
            mqttSocket = null
            multicastLock?.let { if (it.isHeld) it.release() }
        } catch (e: Exception) {
            Log.w(TAG, "Error stopping RealtimeSyncManager", e)
        }
    }

    /**
     * Broadcast an action event to all phones via Cloud MQTT + Local UDP
     */
    fun broadcast(type: String, payload: JSONObject, message: String) {
        scope.launch {
            try {
                val json = JSONObject().apply {
                    put("senderId", deviceId)
                    put("senderName", deviceName)
                    put("type", type)
                    put("message", message)
                    put("payload", payload)
                    put("timestamp", System.currentTimeMillis())
                }
                val jsonString = json.toString()

                // A. Send via Cloud MQTT (Instant worldwide delivery)
                sendMqttPublish(SYNC_TOPIC, jsonString)

                // B. Send via Local UDP (Local Wi-Fi fallback)
                sendLocalUdpBroadcast(jsonString)

                Log.d(TAG, "⚡ Broadcasted [$type]: $message")
            } catch (e: Exception) {
                Log.w(TAG, "Broadcast error: ${e.message}")
            }
        }
    }

    fun sendPing() {
        val payload = JSONObject().apply { put("ping", true) }
        broadcast("PING", payload, "Thiết bị $deviceName đã tham gia đồng bộ")
    }

    // ==========================================
    // CLOUD REAL-TIME ENGINE (MQTT 3.1.1)
    // ==========================================
    private suspend fun runCloudMqttLoop() = withContext(Dispatchers.IO) {
        val brokers = listOf("broker.hivemq.com", "broker.emqx.io")
        var brokerIndex = 0

        while (isRunning) {
            val host = brokers[brokerIndex % brokers.size]
            try {
                Log.i(TAG, "Connecting to Cloud Realtime Broker: $host:1883...")
                val socket = Socket()
                socket.connect(InetSocketAddress(host, 1883), 7000)
                socket.soTimeout = 40000
                mqttSocket = socket
                val out = socket.getOutputStream()
                val input = socket.getInputStream()
                mqttOutputStream = out

                // Send CONNECT
                sendMqttConnect(out, "pos_${deviceId}_${System.currentTimeMillis() % 10000}")

                // Read CONNACK
                val connackHeader = input.read()
                if (connackHeader == 0x20) {
                    val remaining = input.read()
                    val flags = input.read()
                    val returnCode = input.read()
                    if (returnCode == 0) {
                        Log.i(TAG, "✓ Cloud Real-time Broker CONNECTED! Subscribing to $SYNC_TOPIC...")
                        _isCloudConnected.value = true

                        // Send SUBSCRIBE
                        sendMqttSubscribe(out, SYNC_TOPIC)

                        // Start keep-alive ping loop
                        val pingJob = launch {
                            while (isActive && isRunning) {
                                delay(25000)
                                try {
                                    synchronized(out) {
                                        out.write(byteArrayOf(0xC0.toByte(), 0x00)) // PINGREQ
                                        out.flush()
                                    }
                                } catch (e: Exception) {
                                    break
                                }
                            }
                        }

                        // Read incoming packets
                        try {
                            readMqttPackets(input)
                        } finally {
                            pingJob.cancel()
                        }
                    }
                }
            } catch (e: Exception) {
                if (!isRunning) break
                Log.w(TAG, "Cloud broker disconnected (${host}): ${e.message}. Reconnecting in 3s...")
                _isCloudConnected.value = false
                brokerIndex++
            } finally {
                try { mqttSocket?.close() } catch (ignored: Exception) {}
                mqttSocket = null
                mqttOutputStream = null
                _isCloudConnected.value = false
            }

            if (isRunning) {
                delay(3000)
            }
        }
    }

    private fun readMqttPackets(input: InputStream) {
        while (isRunning) {
            val header = input.read()
            if (header == -1) break

            val packetType = (header shr 4) and 0x0F
            val length = readMqttRemainingLength(input)
            val body = ByteArray(length)
            var totalRead = 0
            while (totalRead < length) {
                val r = input.read(body, totalRead, length - totalRead)
                if (r == -1) break
                totalRead += r
            }

            if (packetType == 3) { // PUBLISH packet
                // Topic length (2 bytes)
                if (body.size >= 2) {
                    val topicLen = ((body[0].toInt() and 0xFF) shl 8) or (body[1].toInt() and 0xFF)
                    val payloadOffset = 2 + topicLen
                    if (payloadOffset <= body.size) {
                        val payloadText = String(body, payloadOffset, body.size - payloadOffset, Charsets.UTF_8)
                        handleIncomingPayload(payloadText)
                    }
                }
            } else if (packetType == 13) { // PINGRESP
                // Server acknowledged keep-alive ping
            }
        }
    }

    private fun handleIncomingPayload(jsonText: String) {
        try {
            val json = JSONObject(jsonText)
            val senderId = json.optString("senderId")
            val senderName = json.optString("senderName", "Máy khác")
            val type = json.optString("type")
            val message = json.optString("message", "")
            val payload = json.optJSONObject("payload") ?: JSONObject()

            // Ignore our own broadcast reflections
            if (senderId == deviceId) return

            Log.i(TAG, "⚡ Received Cloud Real-time Event: [$type] from $senderName")

            // Track connected peer
            val current = _onlineDevices.value.toMutableSet()
            current.add("$senderName ($senderId)")
            _onlineDevices.value = current

            if (type == "PING") {
                scope.launch {
                    val pongPayload = JSONObject().apply { put("pong", true) }
                    broadcast("PONG", pongPayload, "")
                }
                return
            }

            // Deliver event to repository
            onRemoteEventReceived?.invoke(type, payload, message)

            // Notify UI
            if (message.isNotBlank()) {
                _syncNotification.tryEmit("⚡ [Đồng bộ từ $senderName]: $message")
            }

        } catch (e: Exception) {
            Log.w(TAG, "Error handling payload: ${e.message}")
        }
    }

    private fun sendMqttConnect(out: OutputStream, clientId: String) {
        val cidBytes = clientId.toByteArray(Charsets.UTF_8)
        val payload = ByteArrayOutputStream().apply {
            write(0); write(4)
            write("MQTT".toByteArray(Charsets.ISO_8859_1))
            write(4)  // level 3.1.1
            write(2)  // clean session
            write(0); write(60) // keepalive 60s
            write((cidBytes.size shr 8) and 0xFF)
            write(cidBytes.size and 0xFF)
            write(cidBytes)
        }

        val packet = ByteArrayOutputStream().apply {
            write(0x10) // CONNECT
            writeRemainingLength(this, payload.size())
            write(payload.toByteArray())
        }
        synchronized(out) {
            out.write(packet.toByteArray())
            out.flush()
        }
    }

    private fun sendMqttSubscribe(out: OutputStream, topic: String) {
        val tBytes = topic.toByteArray(Charsets.UTF_8)
        val payload = ByteArrayOutputStream().apply {
            write(0); write(1) // Packet ID 1
            write((tBytes.size shr 8) and 0xFF)
            write(tBytes.size and 0xFF)
            write(tBytes)
            write(0) // QoS 0
        }

        val packet = ByteArrayOutputStream().apply {
            write(0x82) // SUBSCRIBE
            writeRemainingLength(this, payload.size())
            write(payload.toByteArray())
        }
        synchronized(out) {
            out.write(packet.toByteArray())
            out.flush()
        }
    }

    private fun sendMqttPublish(topic: String, message: String) {
        val out = mqttOutputStream ?: return
        try {
            val tBytes = topic.toByteArray(Charsets.UTF_8)
            val mBytes = message.toByteArray(Charsets.UTF_8)
            val payload = ByteArrayOutputStream().apply {
                write((tBytes.size shr 8) and 0xFF)
                write(tBytes.size and 0xFF)
                write(tBytes)
                write(mBytes)
            }

            val packet = ByteArrayOutputStream().apply {
                write(0x30) // PUBLISH QoS 0
                writeRemainingLength(this, payload.size())
                write(payload.toByteArray())
            }
            synchronized(out) {
                out.write(packet.toByteArray())
                out.flush()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Mqtt Publish failed: ${e.message}")
        }
    }

    private fun writeRemainingLength(out: ByteArrayOutputStream, length: Int) {
        var x = length
        do {
            var encodedByte = (x % 128).toByte()
            x /= 128
            if (x > 0) {
                encodedByte = (encodedByte.toInt() or 128).toByte()
            }
            out.write(encodedByte.toInt())
        } while (x > 0)
    }

    private fun readMqttRemainingLength(input: InputStream): Int {
        var multiplier = 1
        var value = 0
        var digit: Int
        do {
            digit = input.read()
            if (digit == -1) break
            value += (digit and 127) * multiplier
            multiplier *= 128
        } while ((digit and 128) != 0)
        return value
    }

    // ==========================================
    // LOCAL UDP ENGINE (Fallback for Local LAN)
    // ==========================================
    private suspend fun runLocalUdpListener() = withContext(Dispatchers.IO) {
        try {
            val socket = DatagramSocket(null).apply {
                reuseAddress = true
                broadcast = true
                bind(InetSocketAddress(LOCAL_UDP_PORT))
            }
            val buffer = ByteArray(4096)
            while (isRunning) {
                try {
                    val packet = DatagramPacket(buffer, buffer.size)
                    socket.receive(packet)
                    val text = String(packet.data, 0, packet.length, Charsets.UTF_8)
                    handleIncomingPayload(text)
                } catch (e: Exception) {
                    if (!isRunning) break
                    delay(500)
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Local UDP listener warning: ${e.message}")
        }
    }

    private fun sendLocalUdpBroadcast(jsonText: String) {
        try {
            val bytes = jsonText.toByteArray(Charsets.UTF_8)
            val socket = DatagramSocket().apply { broadcast = true }
            val targets = mutableListOf<InetAddress>()
            targets.add(InetAddress.getByName("255.255.255.255"))

            // Also add all interface broadcast addresses (e.g. 192.168.1.255, 192.168.43.255)
            val interfaces = NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val iface = interfaces.nextElement()
                if (iface.isLoopback || !iface.isUp) continue
                for (addr in iface.interfaceAddresses) {
                    addr.broadcast?.let { targets.add(it) }
                }
            }

            for (target in targets.distinct()) {
                try {
                    socket.send(DatagramPacket(bytes, bytes.size, target, LOCAL_UDP_PORT))
                } catch (ignored: Exception) {}
            }
            socket.close()
        } catch (e: Exception) {
            Log.w(TAG, "Local UDP broadcast failed: ${e.message}")
        }
    }
}
