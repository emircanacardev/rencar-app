package com.flowbytestudio.rencar.ui.screens.license

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flowbytestudio.rencar.R
import com.flowbytestudio.rencar.data.auth.AuthSession
import com.flowbytestudio.rencar.data.license.LicenseRepository
import com.flowbytestudio.rencar.ui.common.toErrorRes
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import retrofit2.HttpException

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
            _uiState.update { it.copy(error = R.string.license_error_select_front_back) }
            return
        }
        _uiState.update { it.copy(step = LicenseUploadStep.SELFIE, error = null) }
    }

    fun onNextFromSelfieStep() {
        val state = _uiState.value
        if (!state.hasSelfie) {
            _uiState.update { it.copy(error = R.string.license_error_select_selfie) }
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
            _uiState.update { it.copy(error = R.string.license_error_select_front_back) }
            return
        }
        if (selfie == null) {
            _uiState.update { it.copy(error = R.string.license_error_select_selfie) }
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

                        val errorMessage = throwable.toErrorRes(
                            fallback = R.string.license_error_upload_failed,
                            overrides = mapOf(
                                409 to R.string.license_error_already_submitted,
                                413 to R.string.license_error_file_too_large,
                                400 to R.string.license_error_invalid_file_type,
                            ),
                        )
                        _uiState.update { it.copy(isSubmitting = false, error = errorMessage) }
                    }
            } catch (e: Exception) {
                Log.e(TAG, "Upload Exception", e)
                _uiState.update { it.copy(isSubmitting = false, error = R.string.common_error_connection) }
            } finally {
                _uiState.update { it.copy(isSubmitting = false) }
            }
        }
    }
}
