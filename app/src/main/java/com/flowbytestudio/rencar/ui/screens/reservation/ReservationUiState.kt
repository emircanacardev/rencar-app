package com.flowbytestudio.rencar.ui.screens.reservation

import com.flowbytestudio.rencar.data.rentals.RentalPlan
import com.flowbytestudio.rencar.data.reservations.ReservationResponse
import com.flowbytestudio.rencar.data.vehicles.QuoteResponse
import com.flowbytestudio.rencar.data.vehicles.VehicleDto

// Rezervasyon ücretsiz tutma süresi (RESERVATION_TTL_MIN varsayılanı).
const val FREE_RESERVATION_MINUTES = 15

// Dakikalık plan tahmini süre seçicisinin sınırları/adımı (dk).
const val MINUTE_ESTIMATE_STEP = 15
const val MINUTE_ESTIMATE_MIN = 15
const val MINUTE_ESTIMATE_MAX = 240

// Saatlik/günlük seçicilerin sınırları.
const val HOURS_MIN = 1
const val HOURS_MAX = 24
const val DAYS_MIN = 1
const val DAYS_MAX = 30

data class ReservationUiState(
    val isLoading: Boolean = true,
    val vehicle: VehicleDto? = null,
    val loadError: String? = null,

    // Plan + süre seçimi
    val selectedPlan: RentalPlan = RentalPlan.GUNLUK,
    val minuteEstimate: Int = 30,
    val hours: Int = 1,
    val days: Int = 1,

    // GET /vehicles/{id}/quote — "Tahmini ücret" dökümü
    val quote: QuoteResponse? = null,
    val isQuoteLoading: Boolean = false,
    val quoteError: String? = null,

    val termsAccepted: Boolean = false,

    // Seçim durumu: "Rezerve Et" aksiyonu
    val isReserving: Boolean = false,
    val reserveError: String? = null,

    // Aktif rezervasyon durumu (bu araç için); null ise seçim durumundayız.
    val reservation: ReservationResponse? = null,
    val remainingSeconds: Long = 0,
    val isCancellingReservation: Boolean = false,
    val isUnlocking: Boolean = false,
    // Aktif durumda iptal/kilidi-aç aksiyonlarının hatası
    val actionError: String? = null,

    // Başka bir araçta aktif rezervasyon varsa engelleyici bildirim
    val blockingReservationId: String? = null,
    val blockingVehicleLabel: String? = null,
    val isCancellingBlocking: Boolean = false,
    val blockingError: String? = null,

    // Bilgilendirme (ör. "Rezervasyon süresi doldu")
    val notice: String? = null,

    // Tek seferlik navigasyon tetikleyicileri
    val navigateToHandoverRentalId: String? = null,
    val navigateToActiveRentalId: String? = null,
) {
    // Bu ekran aktif rezervasyon durumunda mı?
    val isReservationActive: Boolean
        get() = reservation != null

    // Quote için sorulacak süre (dk): plan ve seçime göre.
    val quoteMinutes: Int
        get() = when (selectedPlan) {
            RentalPlan.DAKIKALIK -> minuteEstimate
            RentalPlan.SAATLIK -> hours * 60
            RentalPlan.GUNLUK -> days * 1440
        }

    val canReserve: Boolean
        get() = vehicle != null && termsAccepted && !isReserving && blockingReservationId == null

    val canUnlock: Boolean
        get() = reservation != null && termsAccepted && !isUnlocking && !isCancellingReservation
}
