package com.flowbytestudio.rencar.ui.screens.onboarding

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material.icons.outlined.TimeToLeave
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.flowbytestudio.rencar.R
import com.flowbytestudio.rencar.ui.theme.BgLight
import com.flowbytestudio.rencar.ui.theme.BorderLight
import com.flowbytestudio.rencar.ui.theme.Dimens
import com.flowbytestudio.rencar.ui.theme.Primary
import com.flowbytestudio.rencar.ui.theme.PrimaryVariant
import com.flowbytestudio.rencar.ui.theme.Success
import com.flowbytestudio.rencar.ui.theme.TextPrimary
import com.flowbytestudio.rencar.ui.theme.TextSecondary
import kotlinx.coroutines.launch

@Composable
fun OnboardingScreen(
    onStartClick: () -> Unit,
    onLoginClick: () -> Unit,
    viewModel: OnboardingViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val pagerState = rememberPagerState(pageCount = { OnboardingPages.size })
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(pagerState.currentPage) {
        viewModel.onPageChanged(pagerState.currentPage)
    }

    BackHandler(enabled = uiState.currentPage > 0) {
        if (viewModel.onGoToPreviousPage()) {
            coroutineScope.launch {
                pagerState.animateScrollToPage(uiState.currentPage - 1)
            }
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = BgLight
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(Dimens.SpaceXl),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.weight(1f))

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxWidth(),
            ) { page ->
                OnboardingPageContent(page = OnboardingPages[page])
            }

            Spacer(modifier = Modifier.height(48.dp))

            OnboardingPageIndicator(
                pageCount = OnboardingPages.size,
                currentPage = pagerState.currentPage,
            )

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = onStartClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(Dimens.ControlHeight),
                shape = RoundedCornerShape(Dimens.CornerButton),
                colors = ButtonDefaults.buttonColors(containerColor = Primary)
            ) {
                Text(
                    text = stringResource(R.string.onboarding_start_button),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(Dimens.SpaceXl))

            Text(
                text = buildAnnotatedString {
                    append(stringResource(R.string.onboarding_have_account_prefix))
                    withStyle(style = SpanStyle(color = Primary, fontWeight = FontWeight.Bold)) {
                        append(stringResource(R.string.common_login_action))
                    }
                },
                fontSize = 14.sp,
                color = TextSecondary,
                modifier = Modifier.noRippleClickable { onLoginClick() }
            )

            Spacer(modifier = Modifier.height(Dimens.SpaceM))
        }
    }
}

@Composable
private fun OnboardingPageContent(page: OnboardingPage) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(contentAlignment = Alignment.Center) {
            Box(
                modifier = Modifier
                    .size(220.dp)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(Primary.copy(alpha = 0.18f), Color.Transparent),
                        ),
                    ),
            )
            Box(
                modifier = Modifier
                    .size(112.dp)
                    .clip(RoundedCornerShape(30.dp))
                    .background(Brush.linearGradient(colors = listOf(PrimaryVariant, Primary))),
                contentAlignment = Alignment.Center,
            ) {
                when (page.illustration) {
                    OnboardingIllustration.MAP_PIN_WITH_CAR -> MapPinWithCarIllustration()
                    OnboardingIllustration.PHONE_VERIFICATION -> PhoneVerificationIllustration()
                    OnboardingIllustration.MOVING_CAR -> MovingCarIllustration()
                }
            }
        }

        Spacer(modifier = Modifier.height(Dimens.SpaceXxl))

        Text(
            text = stringResource(page.title),
            fontSize = 26.sp,
            fontWeight = FontWeight.ExtraBold,
            color = TextPrimary,
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(Dimens.SpaceS))

        Text(
            text = stringResource(page.description),
            fontSize = 16.sp,
            color = TextSecondary,
            textAlign = TextAlign.Center,
            lineHeight = 24.sp,
            modifier = Modifier.padding(horizontal = Dimens.SpaceXxl)
        )
    }
}

@Composable
private fun MapPinWithCarIllustration() {
    Icon(
        imageVector = Icons.Outlined.DirectionsCar,
        contentDescription = null,
        tint = Color.White,
        modifier = Modifier.size(56.dp),
    )
}

@Composable
private fun PhoneVerificationIllustration() {
    Box(modifier = Modifier.size(64.dp), contentAlignment = Alignment.Center) {
        Icon(
            imageVector = Icons.Outlined.PhoneAndroid,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(52.dp),
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .size(24.dp)
                .clip(CircleShape)
                .background(Success)
                .border(width = 3.dp, color = PrimaryVariant, shape = CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(14.dp),
            )
        }
    }
}

@Composable
private fun MovingCarIllustration() {
    Box(modifier = Modifier.size(64.dp), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(5.dp),
            modifier = Modifier
                .align(Alignment.CenterStart)
                .offset(x = (-2).dp),
        ) {
            Box(modifier = Modifier.size(width = 14.dp, height = 3.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.55f)))
            Box(modifier = Modifier.size(width = 20.dp, height = 3.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.75f)))
            Box(modifier = Modifier.size(width = 14.dp, height = 3.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.55f)))
        }
        Icon(
            imageVector = Icons.Outlined.TimeToLeave,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(52.dp).offset(x = 6.dp),
        )
    }
}

@Composable
private fun OnboardingPageIndicator(pageCount: Int, currentPage: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceXs)) {
        repeat(pageCount) { index ->
            Box(
                modifier = Modifier
                    .size(width = if (index == currentPage) 24.dp else 8.dp, height = 8.dp)
                    .clip(CircleShape)
                    .background(if (index == currentPage) Primary else BorderLight)
            )
        }
    }
}

fun Modifier.noRippleClickable(onClick: () -> Unit): Modifier = composed {
    this.clickable(
        interactionSource = remember { MutableInteractionSource() },
        indication = null,
        onClick = onClick
    )
}
