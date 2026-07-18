package com.flowbytestudio.rencar.data.auth

import android.content.Context
import com.flowbytestudio.rencar.data.network.NetworkModule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import retrofit2.HttpException

/**
 * Oturum yaşam döngüsü: açılışta kayıtlı oturumu geri yükleyip doğrular, access
 * token süresi dolduğunda rotasyonlu refresh'i tek kanaldan çalıştırır. Eşzamanlı
 * iki refresh aynı token'ı gönderirse sunucu bunu "reuse" sayıp tüm oturumu iptal
 * ettiği için mutex ile serileştirme zorunlu.
 */
object SessionManager {

    private val refreshMutex = Mutex()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @Volatile
    private var started = false

    fun init(context: Context) {
        if (started) return
        started = true
        AuthSession.init(context)
        scope.launch { bootstrap(context.applicationContext) }
    }

    private suspend fun bootstrap(context: Context) {
        val saved = TokenStorage.read(context)
        if (saved == null) {
            AuthSession.markLoggedOut()
            return
        }
        AuthSession.restore(saved)
        refresh().onFailure { throwable ->
            val sessionDead = throwable is HttpException &&
                (throwable.code() == 401 || throwable.code() == 403)
            // Oturum öldüyse refresh() zaten clear() ile LOGGED_OUT'a çekti.
            // Ağ/sunucu hatasında kullanıcıyı login'e düşürmeyiz; Authenticator
            // ilk 401'de yeniden dener.
            if (!sessionDead) AuthSession.markSessionValid()
        }
    }

    suspend fun refresh(): Result<AuthResponse> = refreshMutex.withLock {
        // Token mutex İÇİNDE okunur: sırada bekleyen ikinci çağrı, ilkinin
        // rotasyonla ürettiği yeni token'ı kullanır.
        val token = AuthSession.refreshToken
            ?: return@withLock Result.failure(IllegalStateException("Oturum bulunamadı"))
        runCatching {
            NetworkModule.refreshAuthApi.refresh(RefreshTokenRequest(refreshToken = token))
        }.onSuccess { response ->
            AuthSession.onAuthenticated(response)
        }.onFailure { throwable ->
            if (throwable is HttpException && (throwable.code() == 401 || throwable.code() == 403)) {
                // Refresh token ölü (süresi doldu/reuse/iptal) → oturum biter.
                AuthSession.clear()
            }
        }
    }
}
