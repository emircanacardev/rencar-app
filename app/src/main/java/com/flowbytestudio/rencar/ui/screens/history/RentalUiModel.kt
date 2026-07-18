package com.flowbytestudio.rencar.ui.screens.history

import androidx.annotation.StringRes
import com.flowbytestudio.rencar.R
import com.flowbytestudio.rencar.data.rentals.RentalStatus

/** [RentalStatus]'un bu ekrandaki görsel karşılığı (renk/etiket seçimi burada yapılır). */
@StringRes
fun RentalStatus.displayLabelRes(): Int? = when (this) {
    RentalStatus.PREPARING -> R.string.history_status_preparing
    RentalStatus.ACTIVE -> R.string.history_status_active
    RentalStatus.COMPLETED -> R.string.history_status_completed
    RentalStatus.CANCELLED -> R.string.history_status_cancelled
    RentalStatus.UNKNOWN -> null
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
    @StringRes val statusLabel: Int?,
    val rawStatusLabel: String,
    // COMPLETED && UNPAID ise ödenmedi rozeti gösterilir.
    val isUnpaidCompleted: Boolean,
)
