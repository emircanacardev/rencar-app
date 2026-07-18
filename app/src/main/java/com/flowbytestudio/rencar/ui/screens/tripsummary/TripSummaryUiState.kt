package com.flowbytestudio.rencar.ui.screens.tripsummary

import com.flowbytestudio.rencar.data.cards.CardDto
import com.flowbytestudio.rencar.data.rentals.PayRentalResponse
import com.flowbytestudio.rencar.data.rentals.PaymentStatus
import com.flowbytestudio.rencar.data.rentals.RentalDto
import com.flowbytestudio.rencar.data.rentals.RentalStatus
import com.flowbytestudio.rencar.data.rentals.rentalPaymentStatus
import com.flowbytestudio.rencar.data.rentals.rentalStatus

/** Fatura ekranındaki ödeme yöntemi seçimi. API'ye WALLET/CARD/IYZICO olarak gider. */
enum class PaymentMethodOption {
    WALLET, CARD, IYZICO;

    companion object {
        fun from(raw: String?): PaymentMethodOption? = entries.firstOrNull { it.name == raw }
    }
}

/** Makbuzda/menüde gösterilen Türkçe etiket. */
fun PaymentMethodOption?.displayLabel(): String = when (this) {
    PaymentMethodOption.WALLET -> "Cüzdan"
    PaymentMethodOption.CARD -> "Kart"
    PaymentMethodOption.IYZICO -> "İyzico"
    null -> "—"
}

/** İyzico seçilince kendi içindeki tahsilat yöntemi. */
enum class IyzicoSubMethod {
    // Kart bilgisi bu uygulamada toplanmaz; İyzico'nun barındırdığı ödeme sayfası açılır.
    HOSTED_PAGE,
    // Kart bilgisi bu ekranda toplanır; banka onayı SMS ile (3-D Secure) doğrulanır.
    CARD_3DS,
    // Kart bilgisi bu ekranda toplanır; 3-D Secure adımı yoktur, tahsilat anında sonuçlanır.
    CARD_DIRECT,
}

data class TripSummaryUiState(
    val isLoading: Boolean = true,
    val loadError: String? = null,
    val rental: RentalDto? = null,

    // Ödeme bölümü (yalnız COMPLETED + UNPAID iken doldurulur)
    val walletBalance: Double? = null,
    val cards: List<CardDto> = emptyList(),
    val selectedMethod: PaymentMethodOption = PaymentMethodOption.WALLET,
    val selectedCardId: String? = null,
    val discountCode: String = "",
    val isPaying: Boolean = false,
    val payError: String? = null,

    // İyzico alt-yöntem seçimi + kart formu (yalnız CARD_3DS/CARD_DIRECT'te doldurulur).
    val iyzicoSubMethod: IyzicoSubMethod = IyzicoSubMethod.HOSTED_PAGE,
    val iyzicoCardHolderName: String = "",
    val iyzicoCardNumber: String = "",
    val iyzicoExpireMonth: String = "",
    val iyzicoExpireYear: String = "",
    val iyzicoCvc: String = "",

    // WebView açılırken dolar (Checkout Form ya da 3DS doğrulama sayfası), sonuç gelene kadar sürer.
    val iyzicoCheckoutUrl: String? = null,
    val iyzicoCheckoutHtml: String? = null,
    val iyzicoToken: String? = null,

    // Bu oturumda ödeme alındığında dolan makbuz (paidAmount, kalan bakiye / kart).
    val receipt: PayRentalResponse? = null,
) {
    /** Kilitli toplam (indirim öncesi). PER_MINUTE/HOURLY finish sonrası dolar. */
    val totalAmount: Double?
        get() = rental?.totalPrice

    /**
     * Kullanım ücreti kalemi: finish cevabında usageFee doluysa onu, değilse
     * toplamdan açılış ve hizmet bedelini düşerek türetir.
     */
    val usageFee: Double?
        get() {
            val r = rental ?: return null
            r.usageFee?.let { return it }
            val total = r.totalPrice ?: return null
            return total - r.startFee - (r.serviceFee ?: 0.0)
        }

    /** Ödeme yapıldı mı? (bu oturumda ödendiyse ya da yüklenişte zaten PAID ise) */
    val isPaid: Boolean
        get() = receipt != null || rental?.rentalPaymentStatus == PaymentStatus.PAID

    /** Ödeme bölümü gösterilsin mi? */
    val isPayable: Boolean
        get() = receipt == null &&
            rental?.rentalStatus == RentalStatus.COMPLETED &&
            rental.rentalPaymentStatus == PaymentStatus.UNPAID

    /** Ödenecek tutar (kilitli toplam; indirim sunucuda uygulanır). */
    val payableAmount: Double?
        get() = rental?.totalPrice

    /** Cüzdan bakiyesi ödenecek tutarı karşılamıyor mu? */
    val walletInsufficient: Boolean
        get() {
            val balance = walletBalance ?: return false
            val payable = payableAmount ?: return false
            return balance < payable
        }

    /** İyzico 3DS kart formu geçerli mi (basit alan doluluk kontrolü; sunucu asıl doğrulamayı yapar). */
    val iyzicoCardFormValid: Boolean
        get() = iyzicoCardHolderName.isNotBlank() &&
            iyzicoCardNumber.replace(" ", "").length in 15..16 &&
            iyzicoExpireMonth.length == 2 &&
            iyzicoExpireYear.length == 4 &&
            iyzicoCvc.length in 3..4

    /** Ödeme tuşu aktif mi? */
    val canPay: Boolean
        get() {
            if (payableAmount == null) return false
            return when (selectedMethod) {
                PaymentMethodOption.WALLET -> walletBalance != null && !walletInsufficient
                PaymentMethodOption.CARD -> selectedCardId != null
                PaymentMethodOption.IYZICO -> when (iyzicoSubMethod) {
                    IyzicoSubMethod.HOSTED_PAGE -> true
                    IyzicoSubMethod.CARD_3DS, IyzicoSubMethod.CARD_DIRECT -> iyzicoCardFormValid
                }
            }
        }
}
