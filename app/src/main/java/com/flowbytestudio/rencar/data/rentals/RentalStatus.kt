package com.flowbytestudio.rencar.data.rentals

/**
 * `RentalDto.status` alanının backend sözleşmesini temsil eder (bkz. api-openapi_v2.json,
 * AdminRentalController status enum'u). API'den gelen ham string'i tek yerden ayrıştırıp
 * çağıranların birbirinden bağımsız string karşılaştırması yapmasını önler.
 */
enum class RentalStatus {
    PREPARING,
    ACTIVE,
    COMPLETED,
    CANCELLED,
    // Backend yeni bir durum eklerse (ör. geriye dönük uyumluluk) UI kırılmaz.
    UNKNOWN,
    ;

    companion object {
        fun from(raw: String?): RentalStatus = entries.firstOrNull { it.name == raw } ?: UNKNOWN
    }
}

/** `RentalDto.paymentStatus` alanının backend sözleşmesi. */
enum class PaymentStatus {
    UNPAID,
    PAID,
    UNKNOWN,
    ;

    companion object {
        fun from(raw: String?): PaymentStatus = entries.firstOrNull { it.name == raw } ?: UNKNOWN
    }
}

val RentalDto.rentalStatus: RentalStatus
    get() = RentalStatus.from(status)

val RentalDto.rentalPaymentStatus: PaymentStatus
    get() = PaymentStatus.from(paymentStatus)
