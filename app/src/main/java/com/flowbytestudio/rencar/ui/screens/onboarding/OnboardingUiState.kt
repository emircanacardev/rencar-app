package com.flowbytestudio.rencar.ui.screens.onboarding

import androidx.annotation.StringRes
import com.flowbytestudio.rencar.R

enum class OnboardingIllustration {
    MAP_PIN_WITH_CAR,
    PHONE_VERIFICATION,
    MOVING_CAR,
}

data class OnboardingPage(
    val illustration: OnboardingIllustration,
    @StringRes val title: Int,
    @StringRes val description: Int,
)

val OnboardingPages = listOf(
    OnboardingPage(
        illustration = OnboardingIllustration.MAP_PIN_WITH_CAR,
        title = R.string.onboarding_page1_title,
        description = R.string.onboarding_page1_description,
    ),
    OnboardingPage(
        illustration = OnboardingIllustration.PHONE_VERIFICATION,
        title = R.string.onboarding_page2_title,
        description = R.string.onboarding_page2_description,
    ),
    OnboardingPage(
        illustration = OnboardingIllustration.MOVING_CAR,
        title = R.string.onboarding_page3_title,
        description = R.string.onboarding_page3_description,
    ),
)

data class OnboardingUiState(
    val currentPage: Int = 0,
) {
    val isLastPage: Boolean get() = currentPage == OnboardingPages.lastIndex
}
