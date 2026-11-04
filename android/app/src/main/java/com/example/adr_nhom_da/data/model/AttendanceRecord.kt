package com.example.adr_nhom_da.data.model

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class AttendanceType(val displayName: String) {
    CHECK_IN("Vào ca"),
    CHECK_OUT("Tan ca");

    companion object {
        fun fromString(value: String?): AttendanceType {
            if (value.isNullOrBlank()) return CHECK_IN
            return when (value.uppercase().trim()) {
                "CHECK_OUT", "CHECKOUT", "OUT", "TAN_CA" -> CHECK_OUT
                else -> CHECK_IN
            }
        }
    }
}

enum class AttendanceMethod(val displayName: String) {
    NFC("Chạm thẻ NFC"),
    NFC_CARD("Chạm thẻ NFC"),
    QR_CODE("Quét mã QR"),
    FACE_ID("Nhận diện FaceID");

    companion object {
        fun fromString(value: String?): AttendanceMethod {
            if (value.isNullOrBlank()) return FACE_ID
            return when (value.uppercase().trim()) {
                "NFC", "NFC_CARD", "CARD", "THE_NFC" -> NFC
                "QR_CODE", "QR", "QRCODE", "MA_QR" -> QR_CODE
                "FACE_ID", "FACEID", "FACE", "KHUON_MAT" -> FACE_ID
                else -> FACE_ID
            }
        }
    }
}

data class AttendanceRecord(
    val id: Long = 0,
    val employeeId: Long,
    val employeeName: String,
    val type: AttendanceType,
    val method: AttendanceMethod,
    val timestamp: Long = System.currentTimeMillis(),
    val durationHours: Double = 0.0,
    val note: String = ""
) {
    val formattedTime: String
        get() = SimpleDateFormat("HH:mm - dd/MM/yyyy", Locale.getDefault()).format(Date(timestamp))

    val formattedHourOnly: String
        get() = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(timestamp))
}
