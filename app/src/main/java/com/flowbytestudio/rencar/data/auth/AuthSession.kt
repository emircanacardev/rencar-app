package com.flowbytestudio.rencar.data.auth

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Açılışta kayıtlı oturum kontrol edilene kadar UNKNOWN kalır (splash bekletir). */
enum class SessionState { UNKNOWN, LOGGED_IN, LOGGED_OUT }

object AuthSession {

    var accessToken: String? = null
        private set
    var refreshToken: String? = null
        private set

    private val _currentUser = MutableStateFlow<UserResponse?>(null)
    val currentUser: StateFlow<UserResponse?> = _currentUser.asStateFlow()

    private val _sessionState = MutableStateFlow(SessionState.UNKNOWN)
    val sessionState: StateFlow<SessionState> = _sessionState.asStateFlow()

    var justRegistered: Boolean = false
        private set

    private var appContext: Context? = null

    // Tek şeritli dispatcher: rotasyonda eski refresh token'ın yeni kaydın üzerine
    // yazılmaması için persist işleri sıraya girer.
    @OptIn(ExperimentalCoroutinesApi::class)
    private val persistScope =
        CoroutineScope(SupervisorJob() + Dispatchers.IO.limitedParallelism(1))

    fun init(context: Context) {
        if (appContext != null) return
        appContext = context.applicationContext
    }

    fun onAuthenticated(response: AuthResponse, isNewRegistration: Boolean = false) {
        accessToken = response.accessToken
        refreshToken = response.refreshToken
        _currentUser.value = response.user
        _sessionState.value = SessionState.LOGGED_IN
        justRegistered = isNewRegistration
        persist()
    }

    // Cihazda kayıtlı oturumu belleğe alır; geçerliliği SessionManager.bootstrap doğrular.
    fun restore(saved: TokenStorage.Persisted) {
        accessToken = saved.accessToken
        refreshToken = saved.refreshToken
        _currentUser.value = saved.user
    }

    // Bootstrap'ta refresh ağ hatasıyla başarısız olursa eldeki token'larla devam edilir;
    // Authenticator ağ gelince tazeler.
    fun markSessionValid() {
        _sessionState.value = SessionState.LOGGED_IN
    }

    fun markLoggedOut() {
        _sessionState.value = SessionState.LOGGED_OUT
    }

    fun consumeJustRegistered() {
        justRegistered = false
    }

    fun updateCurrentUser(user: UserResponse) {
        _currentUser.value = user
        persist()
    }

    fun clear() {
        accessToken = null
        refreshToken = null
        _currentUser.value = null
        _sessionState.value = SessionState.LOGGED_OUT
        justRegistered = false
        appContext?.let { ctx -> persistScope.launch { TokenStorage.clear(ctx) } }
    }

    private fun persist() {
        val ctx = appContext ?: return
        val access = accessToken ?: return
        val refresh = refreshToken ?: return
        val user = _currentUser.value
        persistScope.launch { TokenStorage.save(ctx, access, refresh, user) }
    }
}
