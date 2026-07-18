package com.flowbytestudio.rencar.ui.screens.activerental

import androidx.annotation.StringRes
import com.flowbytestudio.rencar.data.rentals.RentalDto
import com.flowbytestudio.rencar.data.rentals.VehiclePoint
import com.flowbytestudio.rencar.data.vehicles.VehicleDto

data class ActiveRentalUiState(
    val isLoading: Boolean = true,
    @StringRes val loadError: Int? = null,
    val rental: RentalDto? = null,
    // Harita marker'ının ilk konumu; koordinat özet DTO'da (rental.vehicle) yok,
    // bu yüzden araç ayrıca rental.vehicleId ile çekilir. Sonra canlı konumla güncellenir.
    val vehicle: VehicleDto? = null,
    // Socket.IO 'my-vehicle' akışından gelen canlı araç konumu (varsa marker'ı bu sürer).
    val livePosition: VehiclePoint? = null,
    // Sunucudan gelen elapsedSeconds ile tohumlanır, yoklamalar arasında
    // yerel 1sn'lik tıklayıcıyla akıcı tutulur.
    val elapsedSeconds: Long = 0L,
    // GET /rentals/active — şu an bitirilse ödenecek tahmini tutar.
    val currentCost: Double? = null,
    // GET /rentals/active — biriken mesafe (km).
    val distanceKm: Double? = null,
    // PREPARING: süre işlemiyor; foto akışı tamamlanıp start çağrılmalı.
    val isPreparing: Boolean = false,
    val isEnding: Boolean = false,
    @StringRes val endError: Int? = null,
    // Yoklama sırasında oluşan geçici (ölümcül olmayan) bağlantı hatası.
    @StringRes val pollError: Int? = null,
    // finish başarılı / 404 (başka yerden bitmiş) / baştan COMPLETED → onEnded().
    val ended: Boolean = false,
)
