package com.flowbytestudio.rencar.ui.screens.history

import androidx.annotation.StringRes

data class HistoryUiState(
    val rentals: List<RentalUiModel> = emptyList(),
    // GET /rentals/stats'ten gelir (bu ayın tamamlanmış yolculukları).
    val tripCountThisMonth: Int = 0,
    val totalSpentThisMonth: Double = 0.0,
    val isLoading: Boolean = false,
    @StringRes val errorMessage: Int? = null,
    @StringRes val statsErrorMessage: Int? = null,
)
