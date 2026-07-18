package com.flowbytestudio.rencar.data.auth

import android.content.Context
import com.flowbytestudio.rencar.data.network.NetworkModule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import retrofit2.HttpException

/**
 * Oturum yaşam döngüsü: açılışta kayıtlı oturumu geri yükleyip doğrular, access
 * token süresi dolduğunda rotasyonlu refresh'i tek kanaldan çalıştırır. Eşzamanlı
 * iki refresh aynı token'ı gönderirse sunucu bunu "reuse" sayıp tüm oturumu iptal
 * ettiği için mutex ile serileştirme zorunlu.
 */
object SessionManager {

    // Yavaş/asılı ağda splash süresiz kalmasın; süre dolarsa eldeki token'larla
    // devam edilir, rotasyon NonCancellable olduğundan arka planda yine tamamlanır.
    private const val BOOTSTRAP_REFRESH_TIMEOUT_MS = 5_000L

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
        // Depolama hatası açılışta çökme ya da splash kilidi yaratmasın:
        // okunamayan oturum "yok" sayılır.
        val saved = runCatching { TokenStorage.read(context) }.getOrNull()
        if (saved == null) {
            AuthSession.markLoggedOut()
            return
        }
        AuthSession.restore(saved)
        val result = withTimeoutOrNull(BOOTSTRAP_REFRESH_TIMEOUT_MS) { refresh() }
        if (result == null) {
            // Zaman aşımı anında rotasyon 401'le bitip clear() çalışmış olabilir;
            // token hâlâ duruyorsa oturum geçerli sayılır.
            if (AuthSession.refreshToken != null) AuthSession.markSessionValid()
            return
        }
        result.onFailure { throwable ->
            val sessionDead = throwable is HttpException &&
                (throwable.code() == 401 || throwable.code() == 403)
            // Oturum öldüyse performRefreshLocked zaten clear() ile LOGGED_OUT'a
            // çekti; ağ/sunucu hatasında kullanıcı login'e düşürülmez.
            if (!sessionDead) AuthSession.markSessionValid()
        }
    }

    suspend fun refresh(): Result<AuthResponse> = refreshMutex.withLock {
        performRefreshLocked()
    }

    // Authenticator yolu: eşzamanlı 401'lerde ilk giren rotasyonu yapar; mutex'te
    // bekleyenler içeri girince token'ın çoktan yenilendiğini görüp ağı hiç
    // kullanmadan onu alır (N istek → 1 rotasyon).
    suspend fun refreshedAccessToken(failedAccessToken: String?): String? =
        refreshMutex.withLock {
            AuthSession.accessToken?.takeIf { it != failedAccessToken }
                ?: performRefreshLocked().getOrNull()?.accessToken
        }

    private suspend fun performRefreshLocked(): Result<AuthResponse> {
        // Token mutex İÇİNDE okunur: sırada bekleyen ikinci çağrı, ilkinin
        // rotasyonla ürettiği yeni token'ı kullanır.
        val token = AuthSession.refreshToken
            ?: return Result.failure(IllegalStateException("Oturum bulunamadı"))
        // NonCancellable: sunucu rotasyonu yaptıktan sonra çağıranın scope iptali
        // yanıtı yarıda keserse yeni token kaybolur, eldeki eski token bir sonraki
        // denemede "reuse" sayılıp tüm oturumu iptal ettirirdi.
        return withContext(NonCancellable) {
            val result = runCatching {
                NetworkModule.refreshAuthApi.refresh(RefreshTokenRequest(refreshToken = token))
            }
            // Yanıt beklenirken logout/yeni login oturumu değiştirdiyse bayat sonuç
            // ne uygulanır ne de oturumu kapatır.
            if (AuthSession.refreshToken != token) {
                return@withContext Result.failure(IllegalStateException("Oturum değişti"))
            }
            result.onSuccess { response ->
                AuthSession.onAuthenticated(response)
                // Yeni refresh token diske inmeden rotasyon tamam sayılmaz; süreç
                // bu aralıkta ölürse sıradaki açılış reuse detection'a takılırdı.
                AuthSession.flushPersist()
            }.onFailure { throwable ->
                if (throwable is HttpException && (throwable.code() == 401 || throwable.code() == 403)) {
                    // Refresh token ölü (süresi doldu/reuse/iptal) → oturum biter.
                    AuthSession.clear()
                }
            }
        }
    }
}
