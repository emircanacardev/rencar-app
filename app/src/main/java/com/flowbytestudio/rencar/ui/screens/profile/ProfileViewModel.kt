package com.flowbytestudio.rencar.ui.screens.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flowbytestudio.rencar.data.auth.AuthRepository
import com.flowbytestudio.rencar.data.auth.AuthSession
import com.flowbytestudio.rencar.data.auth.userRole
import com.flowbytestudio.rencar.data.license.LicenseRepository
import com.flowbytestudio.rencar.data.license.licenseStatusEnum
import com.flowbytestudio.rencar.data.rentals.RentalRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ProfileViewModel(
    private val authRepository: AuthRepository = AuthRepository(),
    private val licenseRepository: LicenseRepository = LicenseRepository(),
    private val rentalRepository: RentalRepository = RentalRepository(),
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            AuthSession.currentUser.collect { user ->
                _uiState.update {
                    it.copy(
                        name = user?.fullName.orEmpty(),
                        phone = user?.phone.orEmpty(),
                        avatarUrl = user?.avatarUrl,
                        role = user?.userRole,
                        referralCode = user?.referralCode,
                    )
                }
            }
        }
        // referralCode /auth/me çağrısında üretilir; oturuma yansır ve yukarıdaki collector alır.
        viewModelScope.launch { authRepository.me() }
        loadStats()
        loadLicenseStatus()
    }

    private fun loadStats() {
        viewModelScope.launch {
            // PENDING kullanıcıda 403 döner; sessizce geçilir, kart gösterilmez.
            rentalRepository.getStats().onSuccess { stats ->
                _uiState.update {
                    it.copy(
                        stats = ProfileStats(
                            tripCount = stats.tripCount,
                            totalSpent = stats.totalSpent,
                            totalMinutes = stats.totalMinutes,
                            totalKm = stats.totalKm,
                        ),
                    )
                }
            }
        }
    }

    private fun loadLicenseStatus() {
        viewModelScope.launch {
            licenseRepository.getStatus().onSuccess { status ->
                _uiState.update {
                    it.copy(licenseStatus = status.licenseStatusEnum, rejectReason = status.rejectReason)
                }
            }
        }
    }

    // Ehliyet onayı sonrası yeniden giriş yapmadan CUSTOMER token'ı almak için.
    fun onRefreshSession() {
        if (_uiState.value.isRefreshingSession) return
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshingSession = true) }
            authRepository.refreshSession()
            // Oturum yenilenince stats artık erişilebilir olabilir.
            loadStats()
            _uiState.update { it.copy(isRefreshingSession = false) }
        }
    }

    fun onLogoutClick() {
        _uiState.update { it.copy(isLoggingOut = true) }
        // Sunucuda oturumu iptal eder; başarısız olsa da yerel oturumu temizler.
        viewModelScope.launch { authRepository.logout() }
    }
}
