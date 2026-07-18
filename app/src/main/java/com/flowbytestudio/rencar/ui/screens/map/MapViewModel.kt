package com.flowbytestudio.rencar.ui.screens.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flowbytestudio.rencar.R
import com.flowbytestudio.rencar.data.geocoding.GeocodingRepository
import com.flowbytestudio.rencar.data.geocoding.GeocodingResult
import com.flowbytestudio.rencar.data.rentals.RentalRepository
import com.flowbytestudio.rencar.data.rentals.RentalStatus
import com.flowbytestudio.rencar.data.rentals.rentalStatus
import com.flowbytestudio.rencar.data.reservations.ReservationRepository
import com.flowbytestudio.rencar.data.vehicles.VehicleDto
import com.flowbytestudio.rencar.data.vehicles.VehicleRepository
import com.flowbytestudio.rencar.ui.common.toErrorRes
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MapViewModel(
    private val repository: VehicleRepository = VehicleRepository(),
    private val rentalRepository: RentalRepository = RentalRepository(),
    private val reservationRepository: ReservationRepository = ReservationRepository(),
    private val geocodingRepository: GeocodingRepository = GeocodingRepository(),
) : ViewModel() {

    private val _uiState = MutableStateFlow(MapUiState())
    val uiState: StateFlow<MapUiState> = _uiState.asStateFlow()

    init {
        loadVehicles()
        startReservationTicker()
    }

    fun loadVehicles() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            // includeBusy=true: RENTED/RESERVED araçlar da döner (haritadaki gri marker'lar).
            repository.getVehicles(segment = _uiState.value.selectedSegment, includeBusy = true)
                .onSuccess { vehicles ->
                    _uiState.update { it.copy(isLoading = false, vehicles = vehicles) }
                }
                .onFailure { throwable ->
                    _uiState.update {
                        it.copy(isLoading = false, error = throwable.toVehicleLoadErrorMessage())
                    }
                }
        }
        loadBanners()
    }

    fun onSegmentSelected(segment: String?) {
        if (_uiState.value.selectedSegment == segment) return
        // Segment değişince sunucudan yeniden yüklenir; tip filtresi sıfırlanır (stale seçim kalmasın).
        _uiState.update { it.copy(selectedSegment = segment, selectedType = null, focusedVehicleId = null) }
        loadVehicles()
    }

    // Banner önceliği: aktif kiralama > hazırlıktaki (PREPARING) kiralama > aktif rezervasyon.
    // Uygulama kapansa bile kullanıcı devam eden akışına haritadan geri dönebilir.
    private fun loadBanners() {
        viewModelScope.launch {
            // /rentals/active teorik olarak yalnız ACTIVE (süren) yolculuğu döner, ama
            // status'ü burada da doğrulamak PREPARING bir kiralamanın (ör. foto akışından
            // geri tuşuyla çıkılmış) yanlışlıkla "Kiralama aktif" bannerına düşmesini engeller.
            val active = rentalRepository.getActiveRental().getOrNull()
                ?.takeIf { it.rentalStatus == RentalStatus.ACTIVE }
            if (active != null) {
                _uiState.update {
                    it.copy(
                        activeRental = active,
                        preparingRental = null,
                        activeReservation = null,
                        reservationRemainingSeconds = null,
                    )
                }
                return@launch
            }

            val preparing = rentalRepository.getMyRentals().getOrNull()
                ?.firstOrNull { it.rentalStatus == RentalStatus.PREPARING }
            if (preparing != null) {
                _uiState.update {
                    it.copy(
                        activeRental = null,
                        preparingRental = preparing,
                        activeReservation = null,
                        reservationRemainingSeconds = null,
                    )
                }
                return@launch
            }

            val reservation = reservationRepository.getActiveReservation().getOrNull()
            _uiState.update {
                it.copy(
                    activeRental = null,
                    preparingRental = null,
                    activeReservation = reservation,
                    reservationRemainingSeconds = reservation?.remainingSeconds,
                )
            }
        }
    }

    // Rezervasyon banner'ındaki kalan süre saniye saniye yerelde azaltılır; her poll'da
    // loadBanners() sunucudan gelen taze değerle yeniden eşitler.
    private fun startReservationTicker() {
        viewModelScope.launch {
            while (true) {
                delay(1_000L)
                _uiState.update { state ->
                    val remaining = state.reservationRemainingSeconds
                    if (state.activeReservation != null && remaining != null && remaining > 0) {
                        state.copy(reservationRemainingSeconds = remaining - 1)
                    } else {
                        state
                    }
                }
            }
        }
    }

    fun onTypeSelected(type: String?) {
        _uiState.update { it.copy(selectedType = type, focusedVehicleId = null) }
    }

    fun onVehicleFocused(vehicleId: String?) {
        _uiState.update { it.copy(focusedVehicleId = vehicleId) }
    }

    fun findNearestVehicle(userLat: Double, userLon: Double): VehicleDto? {
        val nearest = _uiState.value.availableFilteredVehicles.minByOrNull { vehicle ->
            haversineMeters(userLat, userLon, vehicle.latitude, vehicle.longitude)
        }
        _uiState.update { it.copy(focusedVehicleId = nearest?.id) }
        return nearest
    }

    // --- Yer arama (üst arama çubuğu, ör. "Bursa") -----------------------------

    private var searchJob: Job? = null

    fun onSearchQueryChange(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        searchJob?.cancel()
        if (query.isBlank()) {
            _uiState.update { it.copy(searchResults = emptyList(), isSearching = false) }
            return
        }
        searchJob = viewModelScope.launch {
            delay(SEARCH_DEBOUNCE_MS)
            _uiState.update { it.copy(isSearching = true) }
            geocodingRepository.search(query)
                .onSuccess { results ->
                    _uiState.update { it.copy(isSearching = false, searchResults = results) }
                }
                .onFailure {
                    _uiState.update { it.copy(isSearching = false, searchResults = emptyList()) }
                }
        }
    }

    // Bir arama sonucu seçildiğinde çubuğu ve sonuç listesini temizler.
    fun onSearchResultSelected(result: GeocodingResult) {
        searchJob?.cancel()
        _uiState.update {
            it.copy(searchQuery = "", searchResults = emptyList(), isSearching = false)
        }
    }

    fun clearSearch() {
        searchJob?.cancel()
        _uiState.update { it.copy(searchQuery = "", searchResults = emptyList(), isSearching = false) }
    }
}

private const val SEARCH_DEBOUNCE_MS = 450L

private fun Throwable.toVehicleLoadErrorMessage(): Int = toErrorRes(
    fallback = R.string.map_error_vehicles_load_failed,
    overrides = mapOf(
        403 to R.string.map_error_account_not_approved,
        401 to R.string.common_error_session_expired,
    ),
)

fun haversineMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val earthRadiusMeters = 6371000.0
    val dLat = Math.toRadians(lat2 - lat1)
    val dLon = Math.toRadians(lon2 - lon1)
    val a = sin(dLat / 2) * sin(dLat / 2) +
        cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2) * sin(dLon / 2)
    val c = 2 * atan2(sqrt(a), sqrt(1 - a))
    return earthRadiusMeters * c
}
