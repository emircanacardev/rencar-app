package com.flowbytestudio.rencar.ui.screens.activerental

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flowbytestudio.rencar.R
import com.flowbytestudio.rencar.data.rentals.RentalRepository
import com.flowbytestudio.rencar.data.rentals.RentalStatus
import com.flowbytestudio.rencar.data.rentals.RideLocationClient
import com.flowbytestudio.rencar.data.rentals.rentalStatus
import com.flowbytestudio.rencar.data.vehicles.VehicleRepository
import com.flowbytestudio.rencar.ui.common.toErrorRes
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class ActiveRentalViewModel(
    private val rentalId: String,
    private val rentalRepository: RentalRepository = RentalRepository(),
    private val vehicleRepository: VehicleRepository = VehicleRepository(),
    private val rideLocationClient: RideLocationClient = RideLocationClient(),
) : ViewModel() {

    private val _uiState = MutableStateFlow(ActiveRentalUiState())
    val uiState: StateFlow<ActiveRentalUiState> = _uiState.asStateFlow()

    private var pollJob: Job? = null
    private var tickerJob: Job? = null
    private var locationJob: Job? = null

    // En son sunucu ölçümü ve alındığı an (millis); tıklayıcı bunun üstüne
    // geçen yerel saniyeyi ekleyerek sayaç değerini akıcı tutar.
    private var serverElapsedSeconds: Long = 0L
    private var serverElapsedAtMillis: Long = 0L

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, loadError = null) }
            rentalRepository.getRental(rentalId)
                .onSuccess { rental ->
                    when (rental.rentalStatus) {
                        // Zaten bitmiş/iptal: TripSummary'ye devret.
                        RentalStatus.COMPLETED, RentalStatus.CANCELLED -> {
                            _uiState.update {
                                it.copy(isLoading = false, rental = rental, ended = true)
                            }
                            return@onSuccess
                        }
                        // Süre işlemiyor: sayaç/yoklama başlatma, yalnız bilgilendir.
                        RentalStatus.PREPARING -> {
                            _uiState.update {
                                it.copy(isLoading = false, rental = rental, isPreparing = true)
                            }
                        }
                        // ACTIVE: canlı durumu sunucudan yokla.
                        else -> {
                            _uiState.update {
                                it.copy(isLoading = false, rental = rental, isPreparing = false)
                            }
                            startTicker()
                            startPolling()
                            startLocationStream()
                        }
                    }
                    // Harita marker'ının koordinatları özet DTO'da yok; aracı ayrıca çek.
                    // v2 görünürlük kuralı: aktif kiralamanın sahibi RENTED aracı görebilir.
                    vehicleRepository.getVehicle(rental.vehicleId).onSuccess { vehicle ->
                        _uiState.update { it.copy(vehicle = vehicle) }
                    }
                }
                .onFailure {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            loadError = R.string.active_rental_load_error,
                        )
                    }
                }
        }
    }

    private fun startPolling() {
        if (pollJob != null) return
        pollJob = viewModelScope.launch {
            var consecutiveErrors = 0
            while (isActive) {
                rentalRepository.getActiveRental()
                    .onSuccess { active ->
                        consecutiveErrors = 0
                        if (active == null) {
                            // 404: aktif kiralama yok → yolculuk başka yerden bitmiş.
                            _uiState.update { it.copy(pollError = null, ended = true) }
                            stopLoops()
                            return@launch
                        }
                        serverElapsedSeconds = active.elapsedSeconds ?: serverElapsedSeconds
                        serverElapsedAtMillis = System.currentTimeMillis()
                        _uiState.update {
                            it.copy(
                                rental = active,
                                elapsedSeconds = active.elapsedSeconds ?: it.elapsedSeconds,
                                currentCost = active.currentCost,
                                distanceKm = active.distanceKm,
                                pollError = null,
                            )
                        }
                    }
                    .onFailure {
                        // Geçici hata: son durumu koru, ısrar ederse ince uyarı göster.
                        consecutiveErrors++
                        if (consecutiveErrors >= 2) {
                            _uiState.update {
                                it.copy(pollError = R.string.active_rental_poll_slow_connection)
                            }
                        }
                    }
                delay(5_000)
            }
        }
    }

    // Socket.IO 'my-vehicle' akışı: aktif kiralamadaki aracın canlı konumu.
    // Bağlantı/parse hatası ölümcül değildir; marker son REST konumunda kalır.
    private fun startLocationStream() {
        if (locationJob != null) return
        locationJob = viewModelScope.launch {
            rideLocationClient.vehiclePositionStream()
                .catch { /* soket hatası yut: harita REST konumuyla çizmeye devam eder */ }
                .collect { point ->
                    _uiState.update { it.copy(livePosition = point) }
                }
        }
    }

    private fun startTicker() {
        if (tickerJob != null) return
        tickerJob = viewModelScope.launch {
            while (isActive) {
                if (serverElapsedAtMillis > 0L) {
                    val extra = ((System.currentTimeMillis() - serverElapsedAtMillis) / 1000)
                        .coerceAtLeast(0)
                    _uiState.update { it.copy(elapsedSeconds = serverElapsedSeconds + extra) }
                }
                delay(1_000)
            }
        }
    }

    fun endRental() {
        if (_uiState.value.isEnding) return
        viewModelScope.launch {
            _uiState.update { it.copy(isEnding = true, endError = null) }
            // finish TÜM planlarda çalışır (return YALNIZ DAILY için, kullanılmaz).
            rentalRepository.finishRental(rentalId)
                .onSuccess {
                    stopLoops()
                    _uiState.update { it.copy(isEnding = false, ended = true) }
                }
                .onFailure { throwable ->
                    _uiState.update {
                        it.copy(isEnding = false, endError = throwable.toEndErrorMessage())
                    }
                }
        }
    }

    private fun stopLoops() {
        pollJob?.cancel()
        pollJob = null
        tickerJob?.cancel()
        tickerJob = null
        locationJob?.cancel()
        locationJob = null
    }

    override fun onCleared() {
        stopLoops()
        super.onCleared()
    }
}

private fun Throwable.toEndErrorMessage(): Int = toErrorRes(
    fallback = R.string.active_rental_end_error_generic,
    overrides = mapOf(
        409 to R.string.active_rental_end_error_not_active,
        404 to R.string.common_error_rental_not_found,
        401 to R.string.common_error_session_expired,
    ),
)
