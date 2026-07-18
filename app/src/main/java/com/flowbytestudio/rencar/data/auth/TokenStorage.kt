package com.flowbytestudio.rencar.data.auth

import android.content.Context
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json

// Bozuk dosya (yarım yazma/elektrik kesintisi) her açılışta CorruptionException
// fırlatıp crash döngüsü yaratmasın: dosya boş tercihlerle değiştirilir, kullanıcı
// en kötü ihtimalle bir kez yeniden giriş yapar.
private val Context.authDataStore by preferencesDataStore(
    name = "auth_prefs",
    corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() },
)

/**
 * Oturum token'larını uygulamaya özel DataStore'da kalıcı tutar. Refresh token
 * rotasyonlu olduğundan her yenilemede kayıt hemen güncellenmelidir; eski token
 * sunucuda "reuse" sayılır ve tüm oturum ailesini iptal eder.
 */
object TokenStorage {

    private val ACCESS_TOKEN_KEY = stringPreferencesKey("access_token")
    private val REFRESH_TOKEN_KEY = stringPreferencesKey("refresh_token")
    private val USER_JSON_KEY = stringPreferencesKey("user_json")

    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    data class Persisted(
        val accessToken: String,
        val refreshToken: String,
        val user: UserResponse?,
    )

    suspend fun read(context: Context): Persisted? {
        val prefs = context.authDataStore.data.first()
        val access = prefs[ACCESS_TOKEN_KEY] ?: return null
        val refresh = prefs[REFRESH_TOKEN_KEY] ?: return null
        val user = prefs[USER_JSON_KEY]?.let { raw ->
            runCatching { json.decodeFromString(UserResponse.serializer(), raw) }.getOrNull()
        }
        return Persisted(accessToken = access, refreshToken = refresh, user = user)
    }

    suspend fun save(
        context: Context,
        accessToken: String,
        refreshToken: String,
        user: UserResponse?,
    ) {
        context.authDataStore.edit { prefs ->
            prefs[ACCESS_TOKEN_KEY] = accessToken
            prefs[REFRESH_TOKEN_KEY] = refreshToken
            if (user != null) {
                prefs[USER_JSON_KEY] = json.encodeToString(UserResponse.serializer(), user)
            }
        }
    }

    suspend fun clear(context: Context) {
        context.authDataStore.edit { it.clear() }
    }
}
