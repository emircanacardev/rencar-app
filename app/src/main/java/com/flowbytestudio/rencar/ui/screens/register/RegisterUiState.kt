package com.flowbytestudio.rencar.ui.screens.register

import androidx.annotation.StringRes

data class RegisterUiState(
    val fullName: String = "",
    val email: String = "",
    val password: String = "",
    val phone: String = "",
    val referralCode: String = "",
    val isLoading: Boolean = false,
    @StringRes val error: Int? = null,
    val errorText: String? = null,
    val isRegistered: Boolean = false,
)
