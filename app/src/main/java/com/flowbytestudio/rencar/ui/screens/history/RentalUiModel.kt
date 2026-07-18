package com.flowbytestudio.rencar.ui.screens.history

import com.flowbytestudio.rencar.data.rentals.RentalStatus

/** [RentalStatus]'un bu ekrandaki Türkçe görsel karşılığı (renk/etiket seçimi burada yapılır). */
fun RentalStatus.displayLabel(): String = when (this) {
    RentalStatus.PREPARING -> "Hazırlanıyor"
    RentalStatus.ACTIVE -> "Devam ediyor"
    RentalStatus.COMPLETED -> "Tamamlandı"
    RentalStatus.CANCELLED -> "İptal edildi"
    RentalStatus.UNKNOWN -> ""
}

data class RentalUiModel(
    val id: String,
    val vehicleId: String,
    // "Marka Model · Plaka" (araç yoksa vehicleId).
    val vehicleLabel: String,
    // Dakikalık / Saatlik / Günlük
    val planLabel: String,
    // startedAt biçimlenmiş; PREPARING'de yoksa "—".
    val dateLabel: String,
    // "₺X" ya da fiyat kilitlenmemişse "—".
    val priceLabel: String,
    val durationMinutes: Int,
    val distanceKm: Double,
    val status: RentalStatus,
    // Bilinen durumlar enum etiketini, bilinmeyenler ham status'u gösterir.
    val statusLabel: String,
    // COMPLETED && UNPAID ise ödenmedi rozeti gösterilir.
    val isUnpaidCompleted: Boolean,
)
