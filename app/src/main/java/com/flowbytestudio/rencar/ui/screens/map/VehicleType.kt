package com.flowbytestudio.rencar.ui.screens.map

import androidx.annotation.StringRes
import androidx.compose.ui.graphics.Color
import com.flowbytestudio.rencar.R

enum class VehicleType(val apiValue: String, @StringRes val label: Int, val color: Color) {
    SEDAN("SEDAN", R.string.map_vehicle_type_sedan, Color(0xFFF59E0B)),
    SUV("SUV", R.string.common_suv, Color(0xFF8B5CF6)),
    HATCHBACK("HATCHBACK", R.string.map_vehicle_type_hatchback, Color(0xFF14B8A6)),
    STATION("STATION", R.string.map_vehicle_type_station, Color(0xFFEAB308)),
    MINIVAN("MINIVAN", R.string.map_vehicle_type_minivan, Color(0xFFEF4444));

    companion object {
        fun fromApiValue(value: String): VehicleType? = entries.find { it.apiValue == value }

        fun colorFor(apiValue: String): Color = fromApiValue(apiValue)?.color ?: Color(0xFF6B7280)

        @StringRes
        fun labelFor(apiValue: String): Int? = fromApiValue(apiValue)?.label
    }
}

// Fiyat segmenti (ECONOMY/COMFORT/SUV) — karoseri tipinden (VehicleType) BAĞIMSIZDIR.
// Haritadaki Tümü/Ekonomik/Konfor/SUV sekmeleri (sunucu ?segment paramı) ve araç
// kartındaki segment rozeti bunu kullanır.
enum class VehicleSegment(val apiValue: String, @StringRes val label: Int) {
    ECONOMY("ECONOMY", R.string.common_segment_economy),
    COMFORT("COMFORT", R.string.common_segment_comfort),
    SUV("SUV", R.string.common_suv);

    companion object {
        @StringRes
        fun labelFor(apiValue: String?): Int? =
            apiValue?.let { value -> entries.find { it.apiValue == value }?.label }
    }
}
