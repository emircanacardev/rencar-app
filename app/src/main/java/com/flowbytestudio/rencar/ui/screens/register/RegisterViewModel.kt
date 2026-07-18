package com.flowbytestudio.rencar.ui.screens.register

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flowbytestudio.rencar.data.auth.AuthConstants
import com.flowbytestudio.rencar.data.auth.AuthRepository
import com.flowbytestudio.rencar.data.network.backendMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException

class RegisterViewModel(
    private val repository: AuthRepository = AuthRepository(),
) : ViewModel() {

    companion object {
        private const val TAG = "RencarAuth"
    }

    private val _uiState = MutableStateFlow(RegisterUiState())
    val uiState: StateFlow<RegisterUiState> = _uiState.asStateFlow()

    fun onFullNameChange(value: String) {
        _uiState.update { it.copy(fullName = value, error = null) }
    }

    fun onEmailChange(value: String) {
        _uiState.update { it.copy(email = value, error = null) }
    }

    fun onPasswordChange(value: String) {
        _uiState.update { it.copy(password = value, error = null) }
    }

    fun onPhoneChange(value: String) {
        val digitsOnly = value.filter { it.isDigit() }
        if (digitsOnly.length <= AuthConstants.PHONE_DIGIT_COUNT) {
            _uiState.update { it.copy(phone = digitsOnly, error = null) }
        }
    }

    fun onReferralCodeChange(value: String) {
        _uiState.update { it.copy(referralCode = value.uppercase(), error = null) }
    }

    fun onRegister() {
        val state = _uiState.value

        if (state.fullName.isBlank() || state.email.isBlank() ||
            state.phone.length != AuthConstants.PHONE_DIGIT_COUNT
        ) {
            _uiState.update { it.copy(error = "Lütfen tüm alanları eksiksiz doldurun.") }
            return
        }
        if (state.password.length < AuthConstants.MIN_PASSWORD_LENGTH) {
            _uiState.update {
                it.copy(error = "Parola en az ${AuthConstants.MIN_PASSWORD_LENGTH} karakter olmalı.")
            }
            return
        }

        val fullPhone = "+90${state.phone}"

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                repository.register(
                    email = state.email,
                    password = state.password,
                    fullName = state.fullName,
                    phone = fullPhone,
                    referralCode = state.referralCode.ifBlank { null },
                )
                    .onSuccess {
                        Log.d(TAG, "Register Success")
                        _uiState.update { it.copy(isLoading = false, isRegistered = true) }
                    }
                    .onFailure { throwable ->
                        val httpException = throwable as? HttpException
                        val statusCode = httpException?.code()
                        val backendMessage = httpException?.backendMessage()
                        Log.e(
                            TAG,
                            "Register Failure: status=$statusCode, message=$backendMessage",
                            throwable,
                        )

                        val errorMessage = when {
                            throwable is IOException -> "Bağlantı hatası oluştu."
                            statusCode == 409 -> backendMessage ?: "Bu e-posta veya telefon numarası zaten kayıtlı."
                            statusCode == 400 && state.referralCode.isNotBlank() -> "Girilen davet kodu geçersiz."
                            else -> "Kayıt oluşturulamadı. Bilgilerinizi kontrol edin."
                        }
                        _uiState.update { it.copy(isLoading = false, error = errorMessage) }
                    }
            } catch (e: Exception) {
                Log.e(TAG, "Register Exception", e)
                _uiState.update { it.copy(isLoading = false, error = "Bağlantı hatası oluştu.") }
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }
}
