package com.flowbytestudio.rencar.ui.screens.license

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flowbytestudio.rencar.data.auth.AuthSession
import com.flowbytestudio.rencar.data.license.LicenseRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException

class LicenseUploadViewModel(
    private val repository: LicenseRepository = LicenseRepository(),
) : ViewModel() {

    companion object {
        private const val TAG = "RencarLicense"
    }

    private val _uiState = MutableStateFlow(
        LicenseUploadUiState(selfieUrl = AuthSession.currentUser.value?.avatarUrl),
    )
    val uiState: StateFlow<LicenseUploadUiState> = _uiState.asStateFlow()

    fun onFrontSelected(uri: Uri) {
        _uiState.update { it.copy(frontUri = uri, error = null) }
    }

    fun onBackSelected(uri: Uri) {
        _uiState.update { it.copy(backUri = uri, error = null) }
    }

    fun onSelfieSelected(uri: Uri) {
        _uiState.update { it.copy(selfieUri = uri, selfieUrl = null, error = null) }
    }

    fun onNextFromLicenseStep() {
        val state = _uiState.value
        if (state.frontUri == null || state.backUri == null) {
            _uiState.update { it.copy(error = "Lütfen ön ve arka yüz fotoğraflarını seçin.") }
            return
        }
        _uiState.update { it.copy(step = LicenseUploadStep.SELFIE, error = null) }
    }

    fun onNextFromSelfieStep() {
        val state = _uiState.value
        if (!state.hasSelfie) {
            _uiState.update { it.copy(error = "Lütfen bir selfie çek veya yükle.") }
            return
        }
        _uiState.update { it.copy(step = LicenseUploadStep.CONFIRM, error = null) }
    }

    fun onBackToPreviousStep() {
        _uiState.update {
            when (it.step) {
                LicenseUploadStep.LICENSE -> it
                LicenseUploadStep.SELFIE -> it.copy(step = LicenseUploadStep.LICENSE, error = null)
                LicenseUploadStep.CONFIRM -> it.copy(step = LicenseUploadStep.SELFIE, error = null)
            }
        }
    }

    // Context, çağrının anlık ihtiyacı için Composable'dan geçirilir; ViewModel
    // tarafında saklanmaz (leak riski yok), yalnızca Uri -> dosya kopyalama işinde kullanılır.
    fun onSubmit(context: Context) {
        val state = _uiState.value
        val front = state.frontUri
        val back = state.backUri
        val selfie = state.selfieUri
        if (front == null || back == null) {
            _uiState.update { it.copy(error = "Lütfen ön ve arka yüz fotoğraflarını seçin.") }
            return
        }
        if (selfie == null) {
            _uiState.update { it.copy(error = "Lütfen bir selfie çek veya yükle.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, error = null) }
            try {
                repository.upload(context.applicationContext, front, back, selfie)
                    .onSuccess {
                        Log.d(TAG, "Upload Success")
                        _uiState.update { it.copy(isSubmitting = false, isSubmitted = true) }
                    }
                    .onFailure { throwable ->
                        val statusCode = (throwable as? HttpException)?.code()
                        Log.e(TAG, "Upload Failure: status=$statusCode", throwable)

                        val errorMessage = when {
                            throwable is IOException -> "Bağlantı hatası oluştu."
                            statusCode == 409 -> "Ehliyetiniz zaten onaylı veya incelemede."
                            statusCode == 413 -> "Dosya boyutu çok büyük (maksimum 5MB)."
                            statusCode == 400 -> "Dosya tipi geçersiz. Lütfen jpg/png seçin."
                            else -> "Yükleme başarısız oldu. Lütfen tekrar deneyin."
                        }
                        _uiState.update { it.copy(isSubmitting = false, error = errorMessage) }
                    }
            } catch (e: Exception) {
                Log.e(TAG, "Upload Exception", e)
                _uiState.update { it.copy(isSubmitting = false, error = "Bağlantı hatası oluştu.") }
            } finally {
                _uiState.update { it.copy(isSubmitting = false) }
            }
        }
    }
}
