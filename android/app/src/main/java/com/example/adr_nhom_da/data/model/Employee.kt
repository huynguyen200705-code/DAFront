package com.example.adr_nhom_da.data.model

enum class EmployeeTier(val displayName: String) {
    THU_VIEC("Thử việc"),
    CHINH_THUC("Chính thức"),
    QUAN_LY("Quản lý"),
    GIAM_DOC("Giám đốc");

    companion object {
        fun fromString(value: String?): EmployeeTier {
            if (value.isNullOrBlank()) return CHINH_THUC
            return when (value.uppercase().trim()) {
                "THU_VIEC", "THUVIEC", "PROBATION" -> THU_VIEC
                "CHINH_THUC", "CHINHTHUC", "OFFICIAL" -> CHINH_THUC
                "QUAN_LY", "QUANLY", "MANAGER" -> QUAN_LY
                "GIAM_DOC", "GIAMDOC", "DIRECTOR" -> GIAM_DOC
                else -> CHINH_THUC
            }
        }
    }
}

enum class ContractType(val displayName: String) {
    HOURLY("Theo giờ"),
    MONTHLY("Theo tháng");

    companion object {
        fun fromString(value: String?): ContractType {
            if (value.isNullOrBlank()) return MONTHLY
            return when (value.uppercase().trim()) {
                "HOURLY", "THEO_GIO", "PART_TIME" -> HOURLY
                "MONTHLY", "THEO_THANG", "FULL_TIME" -> MONTHLY
                else -> MONTHLY
            }
        }
    }
}

data class Employee(
    val id: Long = 0,
    val name: String,
    val phone: String,
    val position: String,
    val tier: EmployeeTier = EmployeeTier.CHINH_THUC,
    val contractType: ContractType = ContractType.MONTHLY,
    val hourlyRate: Double = 25000.0,      // 25,000 VNĐ / giờ
    val monthlySalary: Double = 8000000.0, // 8,000,000 VNĐ / tháng
    val workedHours: Double = 0.0,
    val workedDays: Double = 0.0,
    val bonus: Double = 0.0,
    val penalty: Double = 0.0
) {
    fun calculateSalary(): Double {
        val baseAmount = when (contractType) {
            ContractType.HOURLY -> hourlyRate * workedHours
            ContractType.MONTHLY -> monthlySalary * (workedDays / 26.0)
        }
        val total = baseAmount + bonus - penalty
        return if (total < 0) 0.0 else total
    }

    fun getNfcTagCode(): String = "NFC-EMP-${id.toString().padStart(4, '0')}"
    fun getQrCodeContent(): String = "REST-EMP-${id}-${phone.takeLast(4)}"
}
