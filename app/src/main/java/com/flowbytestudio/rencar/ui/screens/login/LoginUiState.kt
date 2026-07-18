package com.flowbytestudio.rencar.ui.screens.login

import androidx.annotation.StringRes
import com.flowbytestudio.rencar.data.auth.AuthConstants

enum class LoginStep { PHONE, OTP }

data class LoginUiState(
    val step: LoginStep = LoginStep.PHONE,
    val phone: String = "",
    val code: String = "",
    val isLoading: Boolean = false,
    @StringRes val error: Int? = null,
    val isLoggedIn: Boolean = false,
    val timerSeconds: Int = AuthConstants.OTP_RESEND_COOLDOWN_SECONDS,
    val canResendOtp: Boolean = false
)
