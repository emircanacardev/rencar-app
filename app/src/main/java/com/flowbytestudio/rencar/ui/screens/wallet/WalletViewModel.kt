package com.flowbytestudio.rencar.ui.screens.wallet

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flowbytestudio.rencar.R
import com.flowbytestudio.rencar.data.cards.CardDto
import com.flowbytestudio.rencar.data.cards.CardRepository
import com.flowbytestudio.rencar.data.wallet.WalletLimits
import com.flowbytestudio.rencar.data.wallet.WalletRepository
import com.flowbytestudio.rencar.data.wallet.WalletResponse
import com.flowbytestudio.rencar.data.wallet.WalletTransactionDto
import com.flowbytestudio.rencar.ui.common.toErrorRes
import java.time.OffsetDateTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import retrofit2.HttpException

class WalletViewModel(
    private val walletRepository: WalletRepository = WalletRepository(),
    private val cardRepository: CardRepository = CardRepository(),
) : ViewModel() {

    private val _uiState = MutableStateFlow(WalletUiState(isLoading = true))
    val uiState: StateFlow<WalletUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun retry() = load()

    // Kullanıcı bu ekrana her döndüğünde (ör. bir yolculuğu ödedikten sonra)
    // bakiyeyi ve son işlemleri sessizce tazeler; init{} navigasyon boyunca
    // ViewModel canlı kaldığı için yalnızca ilk açılışta çalışır.
    fun refresh() {
        viewModelScope.launch { refreshQuietly() }
    }

    private fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            fetchWalletAndCards()
                .onSuccess { (wallet, cards) ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = null,
                            balance = wallet.balance,
                            transactions = wallet.transactions.toUiModels(),
                            cards = cards.map { card -> card.toUiModel() },
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(isLoading = false, errorMessage = error.toLoadErrorMessage())
                    }
                }
        }
    }

    // --- Bakiye yükleme ------------------------------------------------------

    fun openTopupSheet() {
        _uiState.update { it.copy(showTopupSheet = true, topupError = null) }
    }

    fun dismissTopupSheet() {
        if (_uiState.value.isToppingUp) return
        _uiState.update { it.copy(showTopupSheet = false, topupError = null) }
    }

    fun topup(amountText: String) {
        val amount = amountText.trim().replace(',', '.').toDoubleOrNull()
        if (amount == null || amount < WalletLimits.MIN_TOPUP_AMOUNT || amount > WalletLimits.MAX_TOPUP_AMOUNT) {
            _uiState.update { it.copy(topupError = topupRangeErrorMessage()) }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isToppingUp = true, topupError = null) }
            walletRepository.topup(amount)
                .onSuccess { wallet ->
                    // topup güncel cüzdanı döner; bakiye + hareketleri buradan tazeleriz.
                    _uiState.update {
                        it.copy(
                            isToppingUp = false,
                            showTopupSheet = false,
                            balance = wallet.balance,
                            transactions = wallet.transactions.toUiModels(),
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(isToppingUp = false, topupError = error.toTopupErrorMessage())
                    }
                }
        }
    }

    // --- Kart ekleme ---------------------------------------------------------

    fun openAddCardSheet() {
        _uiState.update { it.copy(showAddCardSheet = true, addCardError = null) }
    }

    fun dismissAddCardSheet() {
        if (_uiState.value.isAddingCard) return
        _uiState.update { it.copy(showAddCardSheet = false, addCardError = null) }
    }

    fun addCard(brand: String, last4: String, expMonthText: String, expYearText: String) {
        val cleanLast4 = last4.trim()
        val month = expMonthText.trim().toIntOrNull()
        val yearText = expYearText.trim()
        val year = yearText.toIntOrNull()
        val validationError = when {
            cleanLast4.length != 4 || cleanLast4.any { !it.isDigit() } ->
                R.string.wallet_last4_validation_error
            month == null || month !in 1..12 -> R.string.wallet_expiry_month_validation_error
            year == null || yearText.length != 4 -> R.string.wallet_expiry_year_validation_error
            isExpiryPast(month, year) -> R.string.wallet_card_expired_error
            else -> null
        }
        if (validationError != null) {
            _uiState.update { it.copy(addCardError = validationError) }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isAddingCard = true, addCardError = null) }
            val created = cardRepository.createCard(brand, cleanLast4, month!!, year!!).getOrElse { e ->
                _uiState.update {
                    it.copy(isAddingCard = false, addCardError = e.toCreateCardErrorMessage())
                }
                return@launch
            }
            // Kart oluşturuldu: önce iyimser olarak listeye ekle (ilk kart otomatik
            // öntanımlı olur). Böylece sonraki tazeleme başarısız olsa bile kart görünür
            // kalır ve kullanıcı yanıltıcı "eklenemedi" mesajıyla mükerrer kart oluşturmaz.
            _uiState.update { state ->
                val others = if (created.isDefault) {
                    state.cards.map { it.copy(isDefault = false) }
                } else {
                    state.cards
                }
                state.copy(
                    isAddingCard = false,
                    showAddCardSheet = false,
                    addCardError = null,
                    cards = others + created.toUiModel(),
                )
            }
            // Sunucu gerçeğiyle (sıralama/öntanımlı) sessizce senkronla; hata yutulur.
            refreshQuietly()
        }
    }

    // --- Kart aksiyonları ----------------------------------------------------

    fun setDefaultCard(id: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(cardActionInProgress = true, cardActionError = null) }
            val result = cardRepository.setDefault(id)
            if (result.isFailure) {
                _uiState.update {
                    it.copy(
                        cardActionInProgress = false,
                        cardActionError = result.exceptionOrNull().toCardActionErrorMessage(),
                    )
                }
                return@launch
            }
            refreshInto { it.copy(cardActionInProgress = false, cardActionError = null) }
        }
    }

    fun deleteCard(id: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(cardActionInProgress = true, cardActionError = null) }
            val result = cardRepository.deleteCard(id)
            if (result.isFailure) {
                _uiState.update {
                    it.copy(
                        cardActionInProgress = false,
                        cardActionError = result.exceptionOrNull().toCardActionErrorMessage(),
                    )
                }
                return@launch
            }
            refreshInto { it.copy(cardActionInProgress = false, cardActionError = null) }
        }
    }

    // --- Ortak yardımcılar ---------------------------------------------------

    // Bir mutasyon sonrası cüzdanı + kartları yeniden çeker, ekranı tazeler.
    // Veri gelmezse (extra ile sıfırlanan) bayrakları koruyup hata mesajı gösterir.
    private suspend fun refreshInto(extra: (WalletUiState) -> WalletUiState) {
        fetchWalletAndCards()
            .onSuccess { (wallet, cards) ->
                _uiState.update {
                    extra(it).copy(
                        balance = wallet.balance,
                        transactions = wallet.transactions.toUiModels(),
                        cards = cards.map { card -> card.toUiModel() },
                    )
                }
            }
            .onFailure { error ->
                _uiState.update {
                    extra(it).copy(cardActionError = error.toLoadErrorMessage())
                }
            }
    }

    // Cüzdanı + kartları yeniden çeker; başarısız olursa mevcut durumu korur ve
    // HATA GÖSTERMEZ (çağıran zaten iyimser güncelleme yapmıştır).
    private suspend fun refreshQuietly() {
        fetchWalletAndCards().onSuccess { (wallet, cards) ->
            _uiState.update {
                it.copy(
                    balance = wallet.balance,
                    transactions = wallet.transactions.toUiModels(),
                    cards = cards.map { card -> card.toUiModel() },
                )
            }
        }
    }

    private suspend fun fetchWalletAndCards(): Result<Pair<WalletResponse, List<CardDto>>> {
        val wallet = walletRepository.getWallet().getOrElse { return Result.failure(it) }
        val cards = cardRepository.getCards().getOrElse { return Result.failure(it) }
        return Result.success(wallet to cards)
    }
}

private val displayDateFormatter = DateTimeFormatter.ofPattern("d MMM · HH:mm", Locale("tr"))

private fun formatDateTime(iso: String): String = runCatching {
    OffsetDateTime.parse(iso).format(displayDateFormatter)
}.getOrDefault(iso)

private fun isExpiryPast(month: Int, year: Int): Boolean = runCatching {
    YearMonth.of(year, month).isBefore(YearMonth.now())
}.getOrDefault(false)

// Son 20 hareket, yeniden eskiye (API zaten bu sırada döner; savunmacı olarak sınırlarız).
private fun List<WalletTransactionDto>.toUiModels(): List<WalletTransactionUiModel> =
    take(20).map { it.toUiModel() }

private fun WalletTransactionDto.toUiModel(): WalletTransactionUiModel {
    val kind = when (type) {
        "TOPUP" -> WalletTransactionKind.TOPUP
        "RENTAL_PAYMENT" -> WalletTransactionKind.RENTAL_PAYMENT
        "REFERRAL_BONUS" -> WalletTransactionKind.REFERRAL_BONUS
        else -> WalletTransactionKind.OTHER
    }
    // Sabit etiketli türlerde (TOPUP/REFERRAL_BONUS) kaynak dizesi kullanılır; RENTAL_PAYMENT/OTHER
    // sunucudan gelen açıklamayı öncelikli gösterir, boşsa ekran kaynak dizesine düşer.
    val titleRes = when (kind) {
        WalletTransactionKind.TOPUP -> R.string.wallet_transaction_topup_title
        WalletTransactionKind.REFERRAL_BONUS -> R.string.wallet_transaction_referral_bonus_title
        WalletTransactionKind.RENTAL_PAYMENT -> if (description.isBlank()) R.string.wallet_transaction_rental_payment_title else null
        WalletTransactionKind.OTHER -> if (description.isBlank()) R.string.wallet_transaction_other_title else null
    }
    val date = formatDateTime(createdAt)
    return WalletTransactionUiModel(
        id = id,
        kind = kind,
        titleRes = titleRes,
        rawTitle = description.ifBlank { null },
        subtitleDate = date,
        subtitleDescription = description.ifBlank { null },
        amount = amount,
    )
}

private fun CardDto.toUiModel(): WalletCardUiModel = WalletCardUiModel(
    id = id,
    brand = brand,
    last4 = last4,
    expiry = String.format(Locale.US, "%02d/%02d", expMonth, expYear % 100),
    isDefault = isDefault,
)

private fun Throwable?.toLoadErrorMessage(): Int = when {
    this == null -> R.string.wallet_error_load_failed
    else -> toErrorRes(
        fallback = R.string.wallet_error_load_failed,
        overrides = mapOf(
            401 to R.string.common_error_session_expired,
            403 to R.string.wallet_error_license_required,
        ),
    )
}

private fun topupRangeErrorMessage(): Int = R.string.wallet_topup_range_error

private fun Throwable.toTopupErrorMessage(): Int = toErrorRes(
    fallback = R.string.wallet_error_topup_failed,
    overrides = mapOf(
        400 to topupRangeErrorMessage(),
        401 to R.string.common_error_session_expired,
    ),
)

private fun Throwable?.toCreateCardErrorMessage(): Int = when {
    this == null -> R.string.wallet_error_add_card_failed
    else -> toErrorRes(
        fallback = R.string.wallet_error_add_card_failed,
        overrides = mapOf(
            400 to R.string.wallet_error_card_invalid,
            401 to R.string.common_error_session_expired,
        ),
    )
}

private fun Throwable?.toCardActionErrorMessage(): Int = when {
    this == null -> R.string.wallet_error_action_failed
    else -> toErrorRes(
        fallback = R.string.wallet_error_action_failed,
        overrides = mapOf(
            404 to R.string.wallet_error_card_not_found,
            401 to R.string.common_error_session_expired,
        ),
    )
}
