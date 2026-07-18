package com.flowbytestudio.rencar.ui.screens.tripsummary

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flowbytestudio.rencar.data.cards.CardRepository
import com.flowbytestudio.rencar.data.iyzico.IyzicoCardRequest
import com.flowbytestudio.rencar.data.iyzico.IyzicoRepository
import com.flowbytestudio.rencar.data.rentals.PaymentStatus
import com.flowbytestudio.rencar.data.rentals.RentalRepository
import com.flowbytestudio.rencar.data.rentals.RentalStatus
import com.flowbytestudio.rencar.data.rentals.rentalPaymentStatus
import com.flowbytestudio.rencar.data.rentals.rentalStatus
import com.flowbytestudio.rencar.data.wallet.WalletRepository
import java.util.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import retrofit2.HttpException

class TripSummaryViewModel(
    private val rentalId: String,
    private val rentalRepository: RentalRepository = RentalRepository(),
    private val walletRepository: WalletRepository = WalletRepository(),
    private val cardRepository: CardRepository = CardRepository(),
    private val iyzicoRepository: IyzicoRepository = IyzicoRepository(),
) : ViewModel() {

    private val _uiState = MutableStateFlow(TripSummaryUiState())
    val uiState: StateFlow<TripSummaryUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, loadError = null) }
            rentalRepository.getRental(rentalId)
                .onSuccess { rental ->
                    _uiState.update { it.copy(isLoading = false, rental = rental) }
                    // Ödeme alınabilir durumdaysa cüzdan bakiyesini ve kayıtlı kartları getir.
                    if (rental.rentalStatus == RentalStatus.COMPLETED && rental.rentalPaymentStatus == PaymentStatus.UNPAID) {
                        loadPaymentSources()
                    }
                }
                .onFailure { throwable ->
                    _uiState.update {
                        it.copy(isLoading = false, loadError = throwable.toLoadErrorMessage())
                    }
                }
        }
    }

    private suspend fun loadPaymentSources() {
        val wallet = walletRepository.getWallet().getOrNull()
        val cards = cardRepository.getCards().getOrNull().orEmpty()
        val defaultCard = cards.firstOrNull { it.isDefault } ?: cards.firstOrNull()
        val payable = _uiState.value.payableAmount
        val insufficient = wallet != null && payable != null && wallet.balance < payable
        // Bakiye yetersiz ama kayıtlı kart varsa, kartı öntanımlı yöntem olarak seç.
        val defaultMethod = if (insufficient && defaultCard != null) {
            PaymentMethodOption.CARD
        } else {
            PaymentMethodOption.WALLET
        }
        _uiState.update {
            it.copy(
                walletBalance = wallet?.balance,
                cards = cards,
                selectedCardId = defaultCard?.id,
                selectedMethod = defaultMethod,
            )
        }
    }

    fun onMethodSelect(method: PaymentMethodOption) {
        _uiState.update { it.copy(selectedMethod = method, payError = null) }
    }

    fun onCardSelect(cardId: String) {
        _uiState.update { it.copy(selectedCardId = cardId, payError = null) }
    }

    fun onDiscountCodeChange(code: String) {
        _uiState.update { it.copy(discountCode = code) }
    }

    fun onIyzicoSubMethodSelect(subMethod: IyzicoSubMethod) {
        _uiState.update { it.copy(iyzicoSubMethod = subMethod, payError = null) }
    }

    fun onIyzicoCardHolderNameChange(value: String) {
        _uiState.update { it.copy(iyzicoCardHolderName = value, payError = null) }
    }

    fun onIyzicoCardNumberChange(value: String) {
        _uiState.update { it.copy(iyzicoCardNumber = value, payError = null) }
    }

    fun onIyzicoExpireMonthChange(value: String) {
        _uiState.update { it.copy(iyzicoExpireMonth = value, payError = null) }
    }

    fun onIyzicoExpireYearChange(value: String) {
        _uiState.update { it.copy(iyzicoExpireYear = value, payError = null) }
    }

    fun onIyzicoCvcChange(value: String) {
        _uiState.update { it.copy(iyzicoCvc = value, payError = null) }
    }

    fun pay() {
        val state = _uiState.value
        if (state.isPaying || !state.isPayable || !state.canPay) return
        when (state.selectedMethod) {
            PaymentMethodOption.IYZICO -> when (state.iyzicoSubMethod) {
                IyzicoSubMethod.HOSTED_PAGE -> startIyzicoHostedPage()
                IyzicoSubMethod.CARD_3DS -> startIyzicoCardPayment(threeDs = true)
                IyzicoSubMethod.CARD_DIRECT -> startIyzicoCardPayment(threeDs = false)
            }
            PaymentMethodOption.WALLET, PaymentMethodOption.CARD -> payDirect()
        }
    }

    private fun payDirect() {
        val state = _uiState.value
        val method = state.selectedMethod
        val cardId = if (method == PaymentMethodOption.CARD) state.selectedCardId else null
        val discountCode = state.discountCode.takeIf { it.isNotBlank() }
        val walletInsufficient = state.walletInsufficient
        viewModelScope.launch {
            _uiState.update { it.copy(isPaying = true, payError = null) }
            rentalRepository.payRental(
                id = rentalId,
                method = method.name,
                cardId = cardId,
                discountCode = discountCode,
            )
                .onSuccess { receipt ->
                    _uiState.update { it.copy(isPaying = false, receipt = receipt) }
                }
                .onFailure { throwable ->
                    val message = throwable.toPayErrorMessage(
                        discountProvided = discountCode != null,
                        walletInsufficient = walletInsufficient,
                    )
                    _uiState.update { it.copy(isPaying = false, payError = message) }
                }
        }
    }

    // --- İyzico Hazır Sayfa (Checkout Form) akışı -------------------------------

    private fun startIyzicoHostedPage() {
        val amount = _uiState.value.payableAmount ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isPaying = true, payError = null) }
            iyzicoRepository.initializeCheckoutForm(rentalId, amount)
                .onSuccess { response ->
                    val url = response.paymentPageUrl
                    if (url == null) {
                        _uiState.update {
                            it.copy(isPaying = false, payError = "Ödeme sayfası açılamadı. Lütfen tekrar dene.")
                        }
                        return@onSuccess
                    }
                    _uiState.update {
                        it.copy(iyzicoCheckoutUrl = url, iyzicoToken = response.token)
                    }
                }
                .onFailure {
                    _uiState.update {
                        it.copy(isPaying = false, payError = "Ödeme sayfası açılamadı. Lütfen tekrar dene.")
                    }
                }
        }
    }

    // WebView, İyzico'nun barındırdığı sayfadan ayrılıp backend'in
    // (rencarv2.halitkalayci.com/iyzico/checkout-form/callback) host'una döndüğünde
    // ekran bunu çağırır: ödeme akışı tamamlanmış demektir, sonucu sorgularız.
    fun onIyzicoWebViewReturnedToBackend() {
        val token = _uiState.value.iyzicoToken ?: return
        _uiState.update { it.copy(iyzicoCheckoutUrl = null) }
        viewModelScope.launch {
            iyzicoRepository.getCheckoutFormResult(token)
                .onSuccess { result ->
                    if (result.paymentStatus == "SUCCESS" && result.paymentId != null) {
                        finalizeIyzicoPayment(result.paymentId)
                    } else {
                        _uiState.update {
                            it.copy(isPaying = false, iyzicoToken = null, payError = "Ödeme tamamlanamadı.")
                        }
                    }
                }
                .onFailure {
                    _uiState.update {
                        it.copy(isPaying = false, iyzicoToken = null, payError = "Ödeme sonucu doğrulanamadı.")
                    }
                }
        }
    }

    fun onIyzicoCheckoutCancelled() {
        _uiState.update {
            it.copy(isPaying = false, iyzicoCheckoutUrl = null, iyzicoCheckoutHtml = null, iyzicoToken = null)
        }
    }

    // --- İyzico kart formu (3-D Secure ve doğrudan tahsilat ortak akışı) --------

    private fun startIyzicoCardPayment(threeDs: Boolean) {
        val state = _uiState.value
        val amount = state.payableAmount ?: return
        if (!state.iyzicoCardFormValid) return
        val card = IyzicoCardRequest(
            cardHolderName = state.iyzicoCardHolderName.trim(),
            cardNumber = state.iyzicoCardNumber.replace(" ", ""),
            expireMonth = state.iyzicoExpireMonth.trim(),
            expireYear = state.iyzicoExpireYear.trim(),
            cvc = state.iyzicoCvc.trim(),
        )
        if (threeDs) {
            startThreedsPayment(amount, card)
        } else {
            startDirectCardPayment(amount, card)
        }
    }

    // Kart bilgisi bu ekranda toplanır, 3-D Secure adımı yoktur; İyzico sonucu senkron döner.
    private fun startDirectCardPayment(amount: Double, card: IyzicoCardRequest) {
        viewModelScope.launch {
            _uiState.update { it.copy(isPaying = true, payError = null) }
            iyzicoRepository.createPayment(rentalId, amount, card)
                .onSuccess { response ->
                    if (response.status == "success" && response.paymentId != null) {
                        finalizeIyzicoPayment(response.paymentId)
                    } else {
                        _uiState.update {
                            it.copy(isPaying = false, payError = "Kart tahsilatı reddedildi. Bilgileri kontrol et.")
                        }
                    }
                }
                .onFailure {
                    _uiState.update {
                        it.copy(isPaying = false, payError = "Kart tahsilatı reddedildi. Bilgileri kontrol et.")
                    }
                }
        }
    }

    // Kart bilgisi bu ekranda toplanır; banka onayı SMS ile doğrulanır.
    private fun startThreedsPayment(amount: Double, card: IyzicoCardRequest) {
        viewModelScope.launch {
            _uiState.update { it.copy(isPaying = true, payError = null) }
            iyzicoRepository.initializeThreeds(rentalId, amount, card)
                .onSuccess { response ->
                    val html = response.threeDSHtmlContentDecoded
                    if (response.status != "success" || html == null) {
                        _uiState.update {
                            it.copy(isPaying = false, payError = "3D Secure doğrulaması başlatılamadı.")
                        }
                        return@onSuccess
                    }
                    // WebView bu HTML'i doğrudan render eder: banka formu otomatik submit olur,
                    // kullanıcı SMS kodunu girer, İyzico son adımda backend host'umuza
                    // (rencarv2.halitkalayci.com/iyzico/payments/threeds/callback) döner.
                    _uiState.update { it.copy(iyzicoCheckoutHtml = html) }
                }
                .onFailure {
                    _uiState.update {
                        it.copy(isPaying = false, payError = "3D Secure doğrulaması başlatılamadı.")
                    }
                }
        }
    }

    // WebView backend'in 3DS callback host'una dönünce, sayfanın kendi HTML'i (backend'in
    // döndürdüğü "Ödeme başarılı" sayfası) taranıp paymentId çıkarılır — bu callback JSON
    // döndürmüyor, doğrudan kullanıcıya gösterilecek bir sonuç sayfası döndürüyor.
    fun onIyzico3dsWebViewReturnedToBackend(pageHtml: String) {
        _uiState.update { it.copy(iyzicoCheckoutHtml = null) }
        val paymentId = IYZICO_PAYMENT_ID_REGEX.find(pageHtml)?.groupValues?.get(1)
        if (paymentId == null) {
            _uiState.update { it.copy(isPaying = false, payError = "Ödeme sonucu okunamadı.") }
            return
        }
        viewModelScope.launch { finalizeIyzicoPayment(paymentId) }
    }

    private suspend fun finalizeIyzicoPayment(iyzicoPaymentId: String) {
        rentalRepository.payRental(
            id = rentalId,
            method = PaymentMethodOption.IYZICO.name,
            iyzicoPaymentId = iyzicoPaymentId,
        )
            .onSuccess { receipt ->
                _uiState.update { it.copy(isPaying = false, iyzicoToken = null, receipt = receipt) }
            }
            .onFailure { throwable ->
                _uiState.update {
                    it.copy(
                        isPaying = false,
                        iyzicoToken = null,
                        payError = throwable.toPayErrorMessage(discountProvided = false, walletInsufficient = false),
                    )
                }
            }
    }
}

// Backend'in 3DS callback sonuç sayfasındaki "Ödeme ID<b>36789054</b>" biçimini hedefler.
private val IYZICO_PAYMENT_ID_REGEX = Regex("""Ödeme ID</td><td[^>]*><b>(\d+)</b>""")

private fun Throwable.toLoadErrorMessage(): String = when {
    this is HttpException && code() == 404 -> "Kiralama bulunamadı."
    this is HttpException && code() == 401 -> "Oturumun sona ermiş. Lütfen tekrar giriş yap."
    else -> "Kiralama özeti yüklenemedi. Lütfen tekrar dene."
}

private fun Throwable.toPayErrorMessage(
    discountProvided: Boolean,
    walletInsufficient: Boolean,
): String = when {
    this is HttpException && code() == 409 -> {
        // 409 üç anlama gelebilir: yetersiz bakiye / zaten ödendi / indirim kodu limiti.
        // Önce sunucu mesajından, olmazsa yerel bağlamdan en olası nedeni seçeriz.
        val body = runCatching { response()?.errorBody()?.string() }
            .getOrNull().orEmpty().lowercase(Locale("tr", "TR"))
        when {
            body.contains("bakiye") -> "Cüzdan bakiyen yetersiz. Bakiye yükle ya da kartla öde."
            body.contains("indirim") -> "İndirim kodu geçersiz ya da kullanım limiti dolmuş."
            body.contains("zaten") -> "Bu yolculuğun ödemesi zaten alınmış."
            walletInsufficient -> "Cüzdan bakiyen yetersiz. Bakiye yükle ya da kartla öde."
            discountProvided -> "İndirim kodu geçersiz ya da kullanım limiti dolmuş."
            else -> "Ödeme alınamadı. Bu yolculuk zaten ödenmiş olabilir."
        }
    }
    this is HttpException && code() == 404 -> "İndirim kodu ya da kart bulunamadı."
    this is HttpException && code() == 400 -> "Geçersiz ödeme isteği. Bilgileri kontrol et."
    this is HttpException && code() == 401 -> "Oturumun sona ermiş. Lütfen tekrar giriş yap."
    else -> "Ödeme alınamadı. Lütfen tekrar dene."
}
