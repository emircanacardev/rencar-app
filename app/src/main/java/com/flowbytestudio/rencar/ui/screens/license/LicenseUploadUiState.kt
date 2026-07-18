package com.flowbytestudio.rencar.ui.screens.license

import android.net.Uri
import androidx.annotation.StringRes

enum class LicenseUploadStep { LICENSE, SELFIE, CONFIRM }

data class LicenseUploadUiState(
    val step: LicenseUploadStep = LicenseUploadStep.LICENSE,
    val frontUri: Uri? = null,
    val backUri: Uri? = null,
    val selfieUri: Uri? = null,
    val selfieUrl: String? = null,
    val isSubmitting: Boolean = false,
    val isSubmitted: Boolean = false,
    @StringRes val error: Int? = null,
) {
    val hasSelfie: Boolean get() = selfieUri != null || selfieUrl != null
}
