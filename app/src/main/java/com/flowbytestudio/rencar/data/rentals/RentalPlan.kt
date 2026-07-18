package com.flowbytestudio.rencar.data.rentals

/** `RentalDto.plan` alanının backend sözleşmesi + ekranlarda gösterilen Türkçe etiket. */
enum class RentalPlan(val apiValue: String, val label: String) {
    DAKIKALIK("PER_MINUTE", "Dakikalık"),
    SAATLIK("HOURLY", "Saatlik"),
    GUNLUK("DAILY", "Günlük"),
    ;

    companion object {
        fun from(apiValue: String?): RentalPlan? = entries.firstOrNull { it.apiValue == apiValue }
    }
}

val RentalDto.rentalPlan: RentalPlan?
    get() = RentalPlan.from(plan)
