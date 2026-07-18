package com.flowbytestudio.rencar.data.wallet

/**
 * Bakiye yükleme sınırları (bkz. api-openapi_v2.json, TopupDto: "10-5000 TL aralığında").
 * ViewModel'deki validasyon ile ekrandaki bilgilendirme metni bu tek kaynaktan beslenir.
 */
object WalletLimits {
    const val MIN_TOPUP_AMOUNT = 10.0
    const val MAX_TOPUP_AMOUNT = 5000.0

    // Hızlı seçim çipleri (₺). Yalnız görsel bir öneri; sunucu tarafında karşılığı yok.
    val QUICK_TOPUP_AMOUNTS = listOf(100, 250, 500)
}
