package com.example.adr_nhom_da.data.model

enum class TableStatus(val displayName: String) {
    TRONG("Trống"),
    DANG_PHUC_VU("Đang phục vụ"),
    DA_DAT("Đã đặt");

    companion object {
        fun fromString(value: String?): TableStatus {
            if (value.isNullOrBlank()) return TRONG
            return when (value.uppercase().trim()) {
                "DANG_PHUC_VU", "DANGPHUCVU", "OCCUPIED", "SERVING", "BUSY" -> DANG_PHUC_VU
                "DA_DAT", "DADAT", "RESERVED", "BOOKED" -> DA_DAT
                else -> TRONG
            }
        }
    }
}

data class RestaurantTable(
    val id: Long = 0,
    val name: String,
    val floor: String = "Tầng 1", // Tầng 1, Tầng 2, Phòng VIP
    val seats: Int = 4,
    val status: TableStatus = TableStatus.TRONG,
    val currentOrderId: Long? = null
) {
    fun getQrCodeContent(): String = "REST-TABLE-${id}-${name.replace(" ", "")}"
    fun getOrderUrl(): String = "https://goldenspoon.vn/order?tableId=${id}"
}
