package com.flowbytestudio.rencar.data.vehicles

/** `VehicleDto.status` alanının backend sözleşmesi. */
enum class VehicleStatus {
    AVAILABLE,
    RESERVED,
    RENTED,
    MAINTENANCE,
    UNKNOWN,
    ;

    companion object {
        fun from(raw: String?): VehicleStatus =
            entries.firstOrNull { it.name.equals(raw, ignoreCase = true) } ?: UNKNOWN
    }
}

val VehicleDto.vehicleStatus: VehicleStatus
    get() = VehicleStatus.from(status)
