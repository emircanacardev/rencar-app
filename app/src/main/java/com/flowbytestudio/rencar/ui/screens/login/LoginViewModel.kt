package com.flowbytestudio.rencar.ui.screens.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flowbytestudio.rencar.data.auth.AuthRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException

class LoginViewModel(
    private val repository: AuthRepository = AuthRepository(),
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    private var timerJob: Job? = null

    fun onPhoneChange(phone: String) {
        val digitsOnly = phone.filter { it.isDigit() }
        if (digitsOnly.length <= 10) {
            _uiState.update { it.copy(phone = digitsOnly, error = null) }
        }
    }

    fun onCodeChange(code: String) {
        val digitsOnly = code.filter { it.isDigit() }
        if (digitsOnly.length <= 6) {
            _uiState.update { it.copy(code = digitsOnly, error = null) }
            // 6. hane girilir girilmez otomatik doğrula; buton yine de duruyor.
            // Hatalı kodda kullanıcı düzeltince (uzunluk tekrar 6 olunca) yeniden tetiklenir.
            if (digitsOnly.length == 6 && !_uiState.value.isLoading) {
                onVerifyOtp()
            }
        }
    }

    fun onRequestOtp() {
        val phoneDigits = _uiState.value.phone
        if (phoneDigits.length < 10) {
            _uiState.update { it.copy(error = "Lütfen 10 haneli telefon numaranızı girin.") }
            return
        }

        // Backend expects format: +905317452223
        val fullPhone = "+90$phoneDigits"

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                repository.requestOtp(fullPhone)
                    .onSuccess {
                        _uiState.update {
                            it.copy(
                                isLoading = false, 
                                step = LoginStep.OTP,
                                timerSeconds = 60,
                                canResendOtp = false
                            ) 
                        }
                        startTimer()
                    }
                    .onFailure { throwable ->
                        val statusCode = (throwable as? HttpException)?.code()
                        val errorMessage = when {
                            throwable is IOException -> "Bağlantı hatası oluştu."
                            statusCode == 401 -> "Bu telefon numarasına kayıtlı kullanıcı yok."
                            else -> "Kod gönderilemedi. Telefon numarasını kontrol et."
                        }
                        _uiState.update { it.copy(isLoading = false, error = errorMessage) }
                    }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, error = "Bağlantı hatası oluştu.")
                }
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (_uiState.value.timerSeconds > 0) {
                delay(1000L)
                _uiState.update { it.copy(timerSeconds = it.timerSeconds - 1) }
            }
            _uiState.update { it.copy(canResendOtp = true) }
        }
    }

    fun onVerifyOtp() {
        val phoneDigits = _uiState.value.phone
        val fullPhone = "+90$phoneDigits"
        val code = _uiState.value.code
        
        if (code.length != 6) {
            _uiState.update { it.copy(error = "6 haneli kodu eksiksiz girin.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                repository.verifyOtp(fullPhone, code)
                    .onSuccess {
                        _uiState.update { it.copy(isLoading = false, isLoggedIn = true) }
                    }
                    .onFailure { throwable ->
                        val errorMessage = if (throwable is IOException) {
                            "Bağlantı hatası oluştu."
                        } else {
                            "Kod hatalı veya süresi dolmuş."
                        }
                        _uiState.update { it.copy(isLoading = false, error = errorMessage) }
                    }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, error = "Bağlantı hatası oluştu.")
                }
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun onChangePhone() {
        timerJob?.cancel()
        _uiState.update { 
            it.copy(
                step = LoginStep.PHONE, 
                code = "", 
                error = null,
                timerSeconds = 0,
                canResendOtp = false,
                isLoading = false
            ) 
        }
    }
}
