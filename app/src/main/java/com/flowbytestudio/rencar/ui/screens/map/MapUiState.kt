package com.flowbytestudio.rencar.ui.screens.map

import com.flowbytestudio.rencar.data.geocoding.GeocodingResult
import com.flowbytestudio.rencar.data.rentals.RentalDto
import com.flowbytestudio.rencar.data.reservations.ReservationResponse
import com.flowbytestudio.rencar.data.vehicles.VehicleDto
import com.flowbytestudio.rencar.data.vehicles.VehicleStatus
import com.flowbytestudio.rencar.data.vehicles.vehicleStatus

data class MapUiState(
    val isLoading: Boolean = true,
    val vehicles: List<VehicleDto> = emptyList(),
    val error: String? = null,
    // Sunucu tarafı fiyat segmenti filtresi (?segment) — Tümü(null)/Ekonomik/Konfor/SUV.
    val selectedSegment: String? = null,
    // İstemci tarafı karoseri tipi filtresi; segmentin üstünde çalışır.
    val selectedType: String? = null,
    val focusedVehicleId: String? = null,
    // Banner öncelik sırası: aktif kiralama > hazırlıktaki (PREPARING) kiralama > aktif rezervasyon.
    val activeRental: RentalDto? = null,
    val preparingRental: RentalDto? = null,
    val activeReservation: ReservationResponse? = null,
    // Rezervasyon geri sayımı için yerelde saniye saniye azaltılan kalan süre.
    val reservationRemainingSeconds: Long? = null,
    // Üst aramadaki yer adı araması (ör. "Bursa") — Nominatim (OSM) ile debounce'lı çalışır.
    val searchQuery: String = "",
    val searchResults: List<GeocodingResult> = emptyList(),
    val isSearching: Boolean = false,
) {
    // Haritada gösterilen araçlar (renkli müsait + gri meşgul), tip filtresi uygulanmış.
    val filteredVehicles: List<VehicleDto>
        get() = vehicles.filter { selectedType == null || it.type == selectedType }

    // Sayaç ve "en yakın araç" yalnız MÜSAİT araçları dikkate alır.
    val availableFilteredVehicles: List<VehicleDto>
        get() = filteredVehicles.filter { it.vehicleStatus == VehicleStatus.AVAILABLE }

    val availableTypes: List<String>
        get() = vehicles.map { it.type }.distinct()

    val banner: MapBanner?
        get() = when {
            activeRental != null -> MapBanner.ActiveRental(
                rentalId = activeRental.id,
                vehicleName = activeRental.vehicle?.let { "${it.brand} ${it.model}" },
                currentCost = activeRental.currentCost ?: activeRental.totalPrice,
            )

            preparingRental != null -> MapBanner.PreparingRental(
                rentalId = preparingRental.id,
                vehicleName = preparingRental.vehicle?.let { "${it.brand} ${it.model}" },
            )

            activeReservation != null -> MapBanner.ActiveReservation(
                vehicleId = activeReservation.vehicleId,
                vehicleName = activeReservation.vehicle?.let { "${it.brand} ${it.model}" },
                remainingSeconds = (reservationRemainingSeconds ?: activeReservation.remainingSeconds)
                    .coerceAtLeast(0),
            )

            else -> null
        }
}

// Harita üstündeki durum banner'ı — öncelik MapUiState.banner'da belirlenir.
sealed interface MapBanner {
    data class ActiveRental(
        val rentalId: String,
        val vehicleName: String?,
        val currentCost: Double?,
    ) : MapBanner

    data class PreparingRental(
        val rentalId: String,
        val vehicleName: String?,
    ) : MapBanner

    data class ActiveReservation(
        val vehicleId: String,
        val vehicleName: String?,
        val remainingSeconds: Long,
    ) : MapBanner
}
