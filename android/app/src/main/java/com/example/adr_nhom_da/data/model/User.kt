package com.example.adr_nhom_da.data.model

enum class UserRole {
    ADMIN,
    USER;

    companion object {
        fun fromString(value: String?): UserRole {
            if (value.isNullOrBlank()) return USER
            return when (value.uppercase().trim()) {
                "ADMIN", "MANAGER", "QUAN_LY", "QUANTRIVIEN", "QUAN_TRI" -> ADMIN
                else -> USER
            }
        }
    }
}

data class User(
    val id: Long = 0,
    val username: String,
    val password: String,
    val fullName: String,
    val phone: String = "",
    val role: UserRole = UserRole.USER,
    val faceIdData: String? = null
)
