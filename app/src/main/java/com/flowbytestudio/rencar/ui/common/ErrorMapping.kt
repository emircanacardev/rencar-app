package com.flowbytestudio.rencar.ui.common

import androidx.annotation.StringRes
import com.flowbytestudio.rencar.R
import retrofit2.HttpException
import java.io.IOException

/**
 * Ekranların ortak hata eşleme kuralı: bağlantı hatası tek mesajdan yönetilir,
 * ekrana özgü anlam taşıyan HTTP kodları [overrides] ile verilir (ör. login'de
 * 401 = "kayıtlı kullanıcı yok"), kalan her şey ekranın genel mesajına düşer.
 */
@StringRes
fun Throwable.toErrorRes(
    @StringRes fallback: Int,
    overrides: Map<Int, Int> = emptyMap(),
): Int = when {
    this is IOException -> R.string.common_error_connection
    else -> (this as? HttpException)?.code()?.let(overrides::get) ?: fallback
}
