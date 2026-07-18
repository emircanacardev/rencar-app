package com.flowbytestudio.rencar.data.auth

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Açılışta kayıtlı oturum kontrol edilene kadar UNKNOWN kalır (splash bekletir). */
enum class SessionState { UNKNOWN, LOGGED_IN, LOGGED_OUT }

object AuthSession {

    private data class TokenPair(val access: String, val refresh: String)

    // Çift alan yerine tek volatile holder: authInterceptor ve persist işleri
    // farklı thread'lerden okuduğu için çiftin asla "yarım" (biri eski biri yeni)
    // görünmemesi gerekir.
    @Volatile
    private var tokenPair: TokenPair? = null

    val accessToken: String? get() = tokenPair?.access
    val refreshToken: String? get() = tokenPair?.refresh

    private val _currentUser = MutableStateFlow<UserResponse?>(null)
    val currentUser: StateFlow<UserResponse?> = _currentUser.asStateFlow()

    private val _sessionState = MutableStateFlow(SessionState.UNKNOWN)
    val sessionState: StateFlow<SessionState> = _sessionState.asStateFlow()

    var justRegistered: Boolean = false
        private set

    private var appContext: Context? = null

    // Tek şeritli persist kuyruğu: her iş anlık durumu ÇALIŞIRKEN okur, böylece
    // bayat bir kopya sonradan yazılmış rotasyon token'ının üzerine binemez.
    @OptIn(ExperimentalCoroutinesApi::class)
    private val persistScope =
        CoroutineScope(SupervisorJob() + Dispatchers.IO.limitedParallelism(1))

    @Volatile
    private var lastPersistJob: Job? = null

    fun init(context: Context) {
        if (appContext != null) return
        appContext = context.applicationContext
    }

    fun onAuthenticated(response: AuthResponse, isNewRegistration: Boolean = false) {
        tokenPair = TokenPair(response.accessToken, response.refreshToken)
        _currentUser.value = response.user
        _sessionState.value = SessionState.LOGGED_IN
        justRegistered = isNewRegistration
        enqueuePersist()
    }

    // Cihazda kayıtlı oturumu belleğe alır; geçerliliği SessionManager.bootstrap doğrular.
    fun restore(saved: TokenStorage.Persisted) {
        tokenPair = TokenPair(saved.accessToken, saved.refreshToken)
        _currentUser.value = saved.user
    }

    // Bootstrap'ta refresh ağ hatası/zaman aşımıyla sonuçlanırsa eldeki token'larla
    // devam edilir; Authenticator gerektiğinde tazeler.
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
        if (tokenPair != null) enqueuePersist()
    }

    fun clear() {
        tokenPair = null
        _currentUser.value = null
        _sessionState.value = SessionState.LOGGED_OUT
        justRegistered = false
        enqueuePersist()
    }

    // Rotasyonda yeni refresh token diske inmeden işlem "tamam" sayılmasın diye
    // SessionManager mutex'i bırakmadan önce bunu bekler (reuse detection'a karşı).
    suspend fun flushPersist() {
        lastPersistJob?.join()
    }

    private fun enqueuePersist() {
        val ctx = appContext ?: return
        lastPersistJob = persistScope.launch {
            // Disk hatası (dolu disk vb.) oturumu değil sadece kalıcılığı kaybettirir.
            runCatching {
                val pair = tokenPair
                if (pair == null) {
                    TokenStorage.clear(ctx)
                } else {
                    TokenStorage.save(ctx, pair.access, pair.refresh, _currentUser.value)
                }
            }
        }
    }
}
