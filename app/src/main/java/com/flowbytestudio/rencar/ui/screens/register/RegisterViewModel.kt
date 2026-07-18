package com.flowbytestudio.rencar.ui.screens.register

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flowbytestudio.rencar.R
import com.flowbytestudio.rencar.data.auth.AuthRepository
import com.flowbytestudio.rencar.data.network.backendMessage
import com.flowbytestudio.rencar.data.auth.AuthConstants
import com.flowbytestudio.rencar.ui.common.toErrorRes
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import retrofit2.HttpException

class RegisterViewModel(
    private val repository: AuthRepository = AuthRepository(),
) : ViewModel() {

    private val _uiState = MutableStateFlow(RegisterUiState())
    val uiState: StateFlow<RegisterUiState> = _uiState.asStateFlow()

    fun onFullNameChange(value: String) {
        _uiState.update { it.copy(fullName = value, error = null, errorText = null) }
    }

    fun onEmailChange(value: String) {
        _uiState.update { it.copy(email = value, error = null, errorText = null) }
    }

    fun onPasswordChange(value: String) {
        _uiState.update { it.copy(password = value, error = null, errorText = null) }
    }

    fun onPhoneChange(value: String) {
        val digitsOnly = value.filter { it.isDigit() }
        if (digitsOnly.length <= AuthConstants.PHONE_DIGIT_COUNT) {
            _uiState.update { it.copy(phone = digitsOnly, error = null, errorText = null) }
        }
    }

    fun onReferralCodeChange(value: String) {
        _uiState.update { it.copy(referralCode = value.uppercase(), error = null, errorText = null) }
    }

    fun onRegister() {
        val state = _uiState.value

        if (state.fullName.isBlank() || state.email.isBlank() || state.phone.length != AuthConstants.PHONE_DIGIT_COUNT) {
            _uiState.update { it.copy(error = R.string.register_error_fill_all_fields) }
            return
        }
        if (state.password.length < AuthConstants.MIN_PASSWORD_LENGTH) {
            _uiState.update { it.copy(error = R.string.register_error_password_min_length) }
            return
        }

        val fullPhone = "+90${state.phone}"

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null, errorText = null) }
            try {
                repository.register(
                    email = state.email,
                    password = state.password,
                    fullName = state.fullName,
                    phone = fullPhone,
                    referralCode = state.referralCode.ifBlank { null },
                )
                    .onSuccess {
                        Log.d("RencarAuth", "Register Success: phone=$fullPhone")
                        _uiState.update { it.copy(isLoading = false, isRegistered = true) }
                    }
                    .onFailure { throwable ->
                        val httpException = throwable as? HttpException
                        val statusCode = httpException?.code()
                        val backendMessage = httpException?.backendMessage()
                        Log.e(
                            "RencarAuth",
                            "Register Failure: phone=$fullPhone, status=$statusCode, message=$backendMessage",
                            throwable,
                        )

                        if (statusCode == 409 && backendMessage != null) {
                            _uiState.update { it.copy(isLoading = false, errorText = backendMessage) }
                        } else {
                            val errorRes = throwable.toErrorRes(
                                fallback = R.string.register_error_generic,
                                overrides = buildMap {
                                    put(409, R.string.register_error_already_registered)
                                    if (state.referralCode.isNotBlank()) {
                                        put(400, R.string.register_error_invalid_referral_code)
                                    }
                                },
                            )
                            _uiState.update { it.copy(isLoading = false, error = errorRes) }
                        }
                    }
            } catch (e: Exception) {
                Log.e("RencarAuth", "Register Exception: phone=$fullPhone", e)
                _uiState.update { it.copy(isLoading = false, error = R.string.common_error_connection) }
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }
}
