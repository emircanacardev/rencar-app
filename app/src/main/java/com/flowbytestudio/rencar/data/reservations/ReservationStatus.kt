package com.flowbytestudio.rencar.data.reservations

/** `ReservationResponse.status` alanının backend sözleşmesi. */
enum class ReservationStatus {
    ACTIVE,
    CONVERTED,
    CANCELLED,
    EXPIRED,
    UNKNOWN,
    ;

    companion object {
        fun from(raw: String?): ReservationStatus = entries.firstOrNull { it.name == raw } ?: UNKNOWN
    }
}

val ReservationResponse.reservationStatus: ReservationStatus
    get() = ReservationStatus.from(status)
