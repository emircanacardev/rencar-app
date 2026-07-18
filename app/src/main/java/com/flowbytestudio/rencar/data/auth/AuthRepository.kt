package com.flowbytestudio.rencar.data.auth

import com.flowbytestudio.rencar.data.network.NetworkModule

class AuthRepository(
    private val api: AuthApi = NetworkModule.authApi,
) {

    suspend fun register(
        email: String,
        password: String,
        fullName: String,
        phone: String,
        referralCode: String? = null,
    ): Result<AuthResponse> = runCatching {
        api.register(
            RegisterRequest(
                email = email,
                password = password,
                fullName = fullName,
                phone = phone,
                referralCode = referralCode?.takeIf { it.isNotBlank() },
            )
        )
    }.onSuccess { response ->
        // Kayıt: oturum açılır ve justRegistered ile ehliyet yükleme ekranına yönlendirilir.
        AuthSession.onAuthenticated(response, isNewRegistration = true)
    }

    suspend fun requestOtp(phone: String): Result<OtpRequiredResponse> = runCatching {
        api.login(LoginRequest(phone = phone))
    }

    suspend fun verifyOtp(phone: String, code: String): Result<AuthResponse> = runCatching {
        api.verifyOtp(VerifyOtpRequest(phone = phone, code = code))
    }.onSuccess { response ->
        AuthSession.onAuthenticated(response)
    }

    // Ehliyet onayı sonrası yeniden giriş yapmadan CUSTOMER token'ı almak için;
    // ayrıca canlı konum soketi süresi dolan token'ı bununla tazeler.
    // Rotasyon + reuse detection nedeniyle tüm refresh'ler SessionManager'daki
    // tek mutex üzerinden akar.
    suspend fun refreshSession(): Result<AuthResponse> = SessionManager.refresh()

    // referralCode /auth/me çağrısında üretildiği için profil/davet ekranı bu ucu kullanır.
    suspend fun me(): Result<UserResponse> = runCatching {
        api.me()
    }.onSuccess { user ->
        AuthSession.updateCurrentUser(user)
    }

    suspend fun logout(): Result<Unit> = runCatching {
        api.logout()
    }.also {
        // Sunucu çağrısı başarısız olsa da yerel oturum kapatılır.
        AuthSession.clear()
    }
}
