package com.example.adr_nhom_da.data.model

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class AttendanceType(val displayName: String) {
    CHECK_IN("Vào ca"),
    CHECK_OUT("Tan ca")
}

enum class AttendanceMethod(val displayName: String) {
    NFC("Chạm thẻ NFC"),
    QR_CODE("Quét mã QR"),
    FACE_ID("Nhận diện FaceID")
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
