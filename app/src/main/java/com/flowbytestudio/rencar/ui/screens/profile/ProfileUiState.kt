package com.flowbytestudio.rencar.ui.screens.profile

import androidx.annotation.StringRes
import com.flowbytestudio.rencar.R
import com.flowbytestudio.rencar.data.auth.UserRole
import com.flowbytestudio.rencar.data.license.LicenseStatus

data class ProfileUiState(
    val name: String = "",
    val phone: String = "",
    val avatarUrl: String? = null,
    val role: UserRole? = null,
    // /auth/me ile üretilir; gelene kadar null.
    val referralCode: String? = null,
    // Varsayılan UNKNOWN: getStatus başarısız olursa onaylı kullanıcıya yanlışlıkla
    // "ehliyetini doğrula" gösterilmez; durum kartı bilinene kadar gizli kalır.
    val licenseStatus: LicenseStatus = LicenseStatus.UNKNOWN,
    val rejectReason: String? = null,
    @StringRes val licenseClass: Int = R.string.profile_license_class_default,
    // Bu ayki yolculuk özeti (CUSTOMER olmayan kullanıcıda null kalır).
    val stats: ProfileStats? = null,
    val isRefreshingSession: Boolean = false,
    val isLoggingOut: Boolean = false,
) {
    // Ehliyet onayı sonrası CUSTOMER token'ı için oturum yenileme önerilir.
    val canRefreshSession: Boolean
        get() = licenseStatus == LicenseStatus.APPROVED && role != null && role != UserRole.CUSTOMER
}

data class ProfileStats(
    val tripCount: Int,
    val totalSpent: Double,
    val totalMinutes: Int,
    val totalKm: Double,
)
