package com.flowbytestudio.rencar.ui.screens.reservation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flowbytestudio.rencar.data.rentals.RentalPlan
import com.flowbytestudio.rencar.data.rentals.RentalRepository
import com.flowbytestudio.rencar.data.rentals.RentalStatus
import com.flowbytestudio.rencar.data.rentals.rentalStatus
import com.flowbytestudio.rencar.data.reservations.ReservationRepository
import com.flowbytestudio.rencar.data.reservations.ReservationResponse
import com.flowbytestudio.rencar.data.reservations.ReservationStatus
import com.flowbytestudio.rencar.data.reservations.ReservationVehicleSummary
import com.flowbytestudio.rencar.data.reservations.reservationStatus
import com.flowbytestudio.rencar.data.vehicles.VehicleDto
import com.flowbytestudio.rencar.data.vehicles.VehicleRepository
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import retrofit2.HttpException

class ReservationViewModel(
    private val vehicleId: String,
    private val vehicleRepository: VehicleRepository = VehicleRepository(),
    private val reservationRepository: ReservationRepository = ReservationRepository(),
    private val rentalRepository: RentalRepository = RentalRepository(),
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReservationUiState())
    val uiState: StateFlow<ReservationUiState> = _uiState.asStateFlow()

    // Geri sayım tetiği; aktif rezervasyon süresini yerel olarak azaltır.
    private var tickerJob: Job? = null

    // Quote istekleri için sıra numarası; eski cevaplar durumu ezmez.
    private var quoteRequestId = 0

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, loadError = null) }

            // Aktif rezervasyon ÖNCE kontrol edilir: rezerve edilmiş araç RESERVED
            // olduğundan GET /vehicles/{id} görünürlük kuralı gereği 404 döner
            // (busy araç yalnız aktif KİRALAMA sahibine görünür). Bu yüzden aracı
            // önce çekip 404'te düşmek, rezervasyon devralmayı imkânsız kılıyordu.
            val active = reservationRepository.getActiveReservation().getOrNull()
                ?.takeIf { it.reservationStatus == ReservationStatus.ACTIVE }

            when {
                active != null && active.vehicleId == vehicleId -> {
                    // Bu araçta aktif rezervasyon: detay 404 dönebilir, özetten türet.
                    val vehicle = vehicleRepository.getVehicle(vehicleId).getOrNull()
                        ?: active.vehicle?.toVehicleDto()
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            vehicle = vehicle,
                            selectedPlan = defaultPlanFor(vehicle),
                        )
                    }
                    enterReservationActive(active)
                }

                active != null -> {
                    // Başka araçta aktif rezervasyon: engelleyici bildirim.
                    // Bu araç AVAILABLE ise özet kartı için detayını da göster.
                    val vehicle = vehicleRepository.getVehicle(vehicleId).getOrNull()
                    val label = active.vehicle?.let { "${it.brand} ${it.model} · ${it.plate}" }
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            vehicle = vehicle,
                            selectedPlan = defaultPlanFor(vehicle),
                            blockingReservationId = active.id,
                            blockingVehicleLabel = label,
                        )
                    }
                }

                else -> {
                    // Aktif rezervasyon yok: araç AVAILABLE olmalı, normal yükle.
                    val vehicle = vehicleRepository.getVehicle(vehicleId).getOrElse { throwable ->
                        _uiState.update {
                            it.copy(isLoading = false, loadError = throwable.toLoadErrorMessage())
                        }
                        return@launch
                    }
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            vehicle = vehicle,
                            selectedPlan = defaultPlanFor(vehicle),
                        )
                    }
                    refreshQuote()
                }
            }
        }
    }

    // Fiyatı API'den gelen ilk plan seçili başlar.
    private fun defaultPlanFor(vehicle: VehicleDto?): RentalPlan = when {
        vehicle?.pricePerMinute != null -> RentalPlan.DAKIKALIK
        vehicle?.pricePerHour != null -> RentalPlan.SAATLIK
        else -> RentalPlan.GUNLUK
    }

    fun onPlanSelect(plan: RentalPlan) {
        if (_uiState.value.selectedPlan == plan) return
        _uiState.update { it.copy(selectedPlan = plan) }
        refreshQuote()
    }

    fun onMinuteEstimateChange(minutes: Int) {
        val clamped = minutes.coerceIn(MINUTE_ESTIMATE_MIN, MINUTE_ESTIMATE_MAX)
        if (clamped == _uiState.value.minuteEstimate) return
        _uiState.update { it.copy(minuteEstimate = clamped) }
        refreshQuote()
    }

    fun onHoursChange(hours: Int) {
        val clamped = hours.coerceIn(HOURS_MIN, HOURS_MAX)
        if (clamped == _uiState.value.hours) return
        _uiState.update { it.copy(hours = clamped) }
        refreshQuote()
    }

    fun onDaysChange(days: Int) {
        val clamped = days.coerceIn(DAYS_MIN, DAYS_MAX)
        if (clamped == _uiState.value.days) return
        _uiState.update { it.copy(days = clamped) }
        refreshQuote()
    }

    fun onTermsToggle(accepted: Boolean) {
        _uiState.update { it.copy(termsAccepted = accepted) }
    }

    fun refreshQuote() {
        val state = _uiState.value
        // Rezervasyon aktifken araç RESERVED; GET /quote görünürlük kuralı gereği
        // 404 döner. Fiyat rezervasyon öncesinde çekilir; aktifken çağrılmaz.
        if (state.isReservationActive) return
        val vehicle = state.vehicle ?: return
        val plan = state.selectedPlan.apiValue
        val minutes = state.quoteMinutes
        val requestId = ++quoteRequestId
        viewModelScope.launch {
            _uiState.update { it.copy(isQuoteLoading = true, quoteError = null) }
            val result = vehicleRepository.getQuote(vehicle.id, plan, minutes)
            // Yalnız en güncel istek durumu güncelleyebilir.
            if (requestId != quoteRequestId) return@launch
            result
                .onSuccess { quote ->
                    _uiState.update { it.copy(isQuoteLoading = false, quote = quote, quoteError = null) }
                }
                .onFailure { throwable ->
                    _uiState.update {
                        it.copy(
                            isQuoteLoading = false,
                            quote = null,
                            quoteError = throwable.toQuoteErrorMessage(),
                        )
                    }
                }
        }
    }

    // Seçim durumu: 15 dk ücretsiz rezervasyon oluştur.
    fun reserve() {
        val state = _uiState.value
        if (!state.canReserve) return
        viewModelScope.launch {
            _uiState.update { it.copy(isReserving = true, reserveError = null, notice = null) }
            reservationRepository.createReservation(vehicleId)
                .onSuccess { reservation -> enterReservationActive(reservation) }
                .onFailure { throwable ->
                    _uiState.update {
                        it.copy(isReserving = false, reserveError = throwable.toReserveErrorMessage())
                    }
                }
        }
    }

    // Aktif rezervasyonu iptal et; seçim durumuna dön.
    fun cancelReservation() {
        val reservation = _uiState.value.reservation ?: return
        if (_uiState.value.isCancellingReservation) return
        viewModelScope.launch {
            _uiState.update { it.copy(isCancellingReservation = true, actionError = null) }
            reservationRepository.cancelReservation(reservation.id)
                .onSuccess { revertToSelection(notice = null) }
                .onFailure { throwable ->
                    // 409/404: rezervasyon zaten yok — yine de seçim durumuna dön.
                    if (throwable is HttpException && (throwable.code() == 409 || throwable.code() == 404)) {
                        revertToSelection(notice = "Rezervasyon zaten sona ermiş.")
                    } else {
                        _uiState.update {
                            it.copy(
                                isCancellingReservation = false,
                                actionError = throwable.toCancelErrorMessage(),
                            )
                        }
                    }
                }
        }
    }

    // "Kilidi Aç": rezervasyonu kiralamaya çevir.
    fun unlock() {
        val state = _uiState.value
        if (!state.canUnlock) return
        val plan = state.selectedPlan
        // endDate YALNIZ DAILY planda gönderilir.
        val endDate = if (plan == RentalPlan.GUNLUK) isoEndDate(state.days) else null
        viewModelScope.launch {
            _uiState.update { it.copy(isUnlocking = true, actionError = null) }
            rentalRepository.createRental(vehicleId, plan.apiValue, endDate)
                .onSuccess { rental ->
                    stopTicker()
                    // PER_MINUTE/HOURLY -> PREPARING (foto akışı); DAILY -> ACTIVE.
                    if (rental.rentalStatus == RentalStatus.PREPARING) {
                        _uiState.update {
                            it.copy(isUnlocking = false, navigateToHandoverRentalId = rental.id)
                        }
                    } else {
                        _uiState.update {
                            it.copy(isUnlocking = false, navigateToActiveRentalId = rental.id)
                        }
                    }
                }
                .onFailure { throwable ->
                    _uiState.update {
                        it.copy(isUnlocking = false, actionError = throwable.toRentalErrorMessage())
                    }
                }
        }
    }

    // Engelleyen (başka araçtaki) rezervasyonu iptal et; bu araç için seçime geç.
    fun cancelBlockingReservation() {
        val id = _uiState.value.blockingReservationId ?: return
        if (_uiState.value.isCancellingBlocking) return
        viewModelScope.launch {
            _uiState.update { it.copy(isCancellingBlocking = true, blockingError = null) }
            reservationRepository.cancelReservation(id)
                .onSuccess { clearBlockingAndReload() }
                .onFailure { throwable ->
                    if (throwable is HttpException && (throwable.code() == 409 || throwable.code() == 404)) {
                        clearBlockingAndReload()
                    } else {
                        _uiState.update {
                            it.copy(
                                isCancellingBlocking = false,
                                blockingError = throwable.toCancelErrorMessage(),
                            )
                        }
                    }
                }
        }
    }

    private fun clearBlockingAndReload() {
        _uiState.update {
            it.copy(
                isCancellingBlocking = false,
                blockingReservationId = null,
                blockingVehicleLabel = null,
                blockingError = null,
            )
        }
        // Tam yeniden yükleme: engelleyici kalkınca aracı yeniden çek ve fiyatı tazele
        // (araç yüklemede müsait değilse ekran boş kalmasın diye refreshQuote yetmez).
        load()
    }

    private fun enterReservationActive(reservation: ReservationResponse) {
        val remaining = reservation.remainingSeconds
        if (remaining <= 0) {
            revertToSelection(notice = "Rezervasyon süresi doldu. Aracı yeniden rezerve edebilirsiniz.")
            return
        }
        _uiState.update {
            it.copy(
                isReserving = false,
                reservation = reservation,
                remainingSeconds = remaining,
                reserveError = null,
                actionError = null,
                notice = null,
            )
        }
        startTicker()
    }

    private fun revertToSelection(notice: String?) {
        stopTicker()
        _uiState.update {
            it.copy(
                reservation = null,
                remainingSeconds = 0,
                isReserving = false,
                isCancellingReservation = false,
                isUnlocking = false,
                actionError = null,
                notice = notice,
            )
        }
    }

    private fun startTicker() {
        tickerJob?.cancel()
        tickerJob = viewModelScope.launch {
            while (true) {
                delay(1000)
                val next = _uiState.value.remainingSeconds - 1
                if (next <= 0) {
                    revertToSelection(
                        notice = "Rezervasyon süresi doldu. Aracı yeniden rezerve edebilirsiniz.",
                    )
                    break
                }
                _uiState.update { it.copy(remainingSeconds = next) }
            }
        }
    }

    private fun stopTicker() {
        tickerJob?.cancel()
        tickerJob = null
    }

    override fun onCleared() {
        super.onCleared()
        stopTicker()
    }
}

// Rezerve araç detayı 404 döndüğünde rezervasyon özetinden asgari VehicleDto türetilir.
private fun ReservationVehicleSummary.toVehicleDto(): VehicleDto = VehicleDto(
    id = id,
    plate = plate,
    brand = brand,
    model = model,
    type = type,
    pricePerDay = 0.0,
    status = "RESERVED",
    latitude = latitude,
    longitude = longitude,
    pricePerMinute = pricePerMinute,
)

// endDate: şu andan itibaren `days` gün sonrası, ISO UTC (YALNIZ DAILY).
private fun isoEndDate(days: Int): String {
    val millis = System.currentTimeMillis() + days * 24L * 60 * 60 * 1000
    val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }
    return format.format(Date(millis))
}

private fun Throwable.toLoadErrorMessage(): String = when {
    this is HttpException && code() == 404 -> "Bu araç artık müsait değil."
    this is HttpException && code() == 403 -> "Hesabınız onay bekliyor. Ehliyetiniz onaylandıktan sonra araç kiralayabilirsiniz."
    this is HttpException && code() == 401 -> "Oturumun sona ermiş. Lütfen tekrar giriş yap."
    else -> "Araç bilgileri yüklenemedi. Lütfen tekrar dene."
}

private fun Throwable.toQuoteErrorMessage(): String = when {
    this is HttpException && code() == 404 -> "Bu araç artık müsait değil."
    this is HttpException && code() == 403 -> "Hesabınız onay bekliyor."
    this is HttpException && code() == 401 -> "Oturumun sona ermiş. Lütfen tekrar giriş yap."
    else -> "Tahmini ücret hesaplanamadı."
}

// POST /reservations hata eşlemesi.
private fun Throwable.toReserveErrorMessage(): String = when {
    this is HttpException && code() == 409 ->
        "Bu araç şu anda müsait değil ya da zaten aktif bir rezervasyon/kiralamanız var."
    this is HttpException && code() == 404 -> "Araç bulunamadı."
    this is HttpException && code() == 403 ->
        "Hesabınız onay bekliyor. Ehliyetiniz onaylandıktan sonra rezervasyon yapabilirsiniz."
    this is HttpException && code() == 401 -> "Oturumun sona ermiş. Lütfen tekrar giriş yap."
    else -> "Rezervasyon oluşturulamadı. Lütfen tekrar dene."
}

// POST /rentals hata eşlemesi (kilidi aç).
private fun Throwable.toRentalErrorMessage(): String = when {
    this is HttpException && code() == 409 ->
        "Kilit açılamadı. Rezervasyonunuz sona ermiş ya da zaten aktif bir kiralamanız olabilir."
    this is HttpException && code() == 404 -> "Araç bulunamadı."
    this is HttpException && code() == 403 ->
        "Hesabınız onay bekliyor. Ehliyetiniz onaylandıktan sonra kiralama yapabilirsiniz."
    this is HttpException && code() == 400 -> "Kiralama bilgileri geçersiz. Lütfen tekrar dene."
    this is HttpException && code() == 401 -> "Oturumun sona ermiş. Lütfen tekrar giriş yap."
    else -> "Kiralama başlatılamadı. Lütfen tekrar dene."
}

// DELETE /reservations/{id} hata eşlemesi.
private fun Throwable.toCancelErrorMessage(): String = when {
    this is HttpException && code() == 401 -> "Oturumun sona ermiş. Lütfen tekrar giriş yap."
    else -> "Rezervasyon iptal edilemedi. Lütfen tekrar dene."
}
