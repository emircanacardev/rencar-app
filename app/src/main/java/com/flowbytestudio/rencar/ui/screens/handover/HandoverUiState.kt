package com.flowbytestudio.rencar.ui.screens.handover

import androidx.annotation.StringRes
import com.flowbytestudio.rencar.R
import com.flowbytestudio.rencar.data.rentals.RentalVehicleSummary

// UI etiketi Türkçe, API yönü İngilizce: ON=FRONT, ARKA=BACK, SOL=LEFT, SAG=RIGHT.
enum class PhotoSide(@StringRes val label: Int, val apiSide: String) {
    ON(R.string.handover_side_front, "FRONT"),
    ARKA(R.string.handover_side_back, "BACK"),
    SOL(R.string.handover_side_left, "LEFT"),
    SAG(R.string.handover_side_right, "RIGHT");

    companion object {
        fun fromApi(apiSide: String): PhotoSide? = entries.firstOrNull { it.apiSide == apiSide }
    }
}

data class HandoverUiState(
    val isLoading: Boolean = true,
    @StringRes val loadError: Int? = null,
    val vehicle: RentalVehicleSummary? = null,
    // Yön -> sunucuda yüklü fotoğrafın URL'i (Coil ile gösterilir).
    val photos: Map<PhotoSide, String> = emptyMap(),
    val uploadedCount: Int = 0,
    val photosComplete: Boolean = false,
    // Şu an yükleniyor olan yönler (slot üzerinde spinner).
    val uploadingSides: Set<PhotoSide> = emptySet(),
    @StringRes val uploadError: Int? = null,
    val isStarting: Boolean = false,
    @StringRes val startError: Int? = null,
    val startErrorArg: Int? = null,
    val startedRentalId: String? = null,
    val showCancelDialog: Boolean = false,
    val isCancelling: Boolean = false,
    @StringRes val cancelError: Int? = null,
    val cancelled: Boolean = false,
) {
    val remainingCount: Int
        get() = PhotoSide.entries.size - uploadedCount

    val canStart: Boolean
        get() = photosComplete && !isStarting && !isCancelling && uploadingSides.isEmpty()
}
