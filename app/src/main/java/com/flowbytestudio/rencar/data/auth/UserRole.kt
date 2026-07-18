package com.flowbytestudio.rencar.data.auth

/** `UserResponse.role` alanının backend sözleşmesi. */
enum class UserRole {
    PENDING,
    CUSTOMER,
    ADMIN,
    UNKNOWN,
    ;

    companion object {
        fun from(raw: String?): UserRole = entries.firstOrNull { it.name == raw } ?: UNKNOWN
    }
}

val UserResponse.userRole: UserRole
    get() = UserRole.from(role)
