package com.flowbytestudio.rencar.ui.screens.reservation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.LockOpen
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import coil.compose.AsyncImage
import com.flowbytestudio.rencar.R
import com.flowbytestudio.rencar.data.rentals.RentalPlan
import com.flowbytestudio.rencar.data.vehicles.QuoteResponse
import com.flowbytestudio.rencar.data.vehicles.VehicleDto
import com.flowbytestudio.rencar.data.vehicles.VehicleStatus
import com.flowbytestudio.rencar.data.vehicles.vehicleStatus
import com.flowbytestudio.rencar.ui.screens.map.colors
import com.flowbytestudio.rencar.ui.common.formatTl
import com.flowbytestudio.rencar.ui.screens.map.VehicleSegment
import com.flowbytestudio.rencar.ui.screens.map.VehicleType
import com.flowbytestudio.rencar.ui.theme.Background
import com.flowbytestudio.rencar.ui.theme.BgLight
import com.flowbytestudio.rencar.ui.theme.BorderLight
import com.flowbytestudio.rencar.ui.theme.Danger
import com.flowbytestudio.rencar.ui.theme.DangerLight
import com.flowbytestudio.rencar.ui.theme.Dimens
import com.flowbytestudio.rencar.ui.theme.Primary
import com.flowbytestudio.rencar.ui.theme.PrimaryLight
import com.flowbytestudio.rencar.ui.theme.Success
import com.flowbytestudio.rencar.ui.theme.SuccessLight
import com.flowbytestudio.rencar.ui.theme.Surface
import com.flowbytestudio.rencar.ui.theme.TextPrimary
import com.flowbytestudio.rencar.ui.theme.TextSecondary
import java.util.Locale

@Composable
fun ReservationScreen(
    vehicleId: String,
    onBack: () -> Unit,
    onNavigateToHandover: (rentalId: String) -> Unit,
    onNavigateToActiveRental: (rentalId: String) -> Unit,
) {
    val viewModel: ReservationViewModel = viewModel(
        factory = viewModelFactory {
            initializer { ReservationViewModel(vehicleId) }
        },
    )
    val uiState by viewModel.uiState.collectAsState()

    // Kilidi açılan yolculuk: dk/sa -> foto akışı, DAILY -> aktif kiralama.
    LaunchedEffect(uiState.navigateToHandoverRentalId) {
        uiState.navigateToHandoverRentalId?.let(onNavigateToHandover)
    }
    LaunchedEffect(uiState.navigateToActiveRentalId) {
        uiState.navigateToActiveRentalId?.let(onNavigateToActiveRental)
    }

    Column(modifier = Modifier.fillMaxSize().background(Background)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = stringResource(R.string.common_back), tint = TextPrimary)
            }
            Text(
                text = if (uiState.isReservationActive) stringResource(R.string.reservation_title_active) else stringResource(R.string.reservation_title),
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary,
            )
        }

        Box(modifier = Modifier.fillMaxSize()) {
            when {
                uiState.isLoading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Primary)
                    }
                }
                uiState.loadError != null -> {
                    val loadError = uiState.loadError ?: return@Box
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(Dimens.SpaceXl),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Text(text = stringResource(loadError), color = Danger, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(Dimens.SpaceS))
                        Button(
                            onClick = viewModel::load,
                            colors = ButtonDefaults.buttonColors(containerColor = Primary),
                        ) {
                            Text(stringResource(R.string.common_retry))
                        }
                    }
                }
                uiState.blockingReservationId != null -> {
                    BlockingReservationNotice(
                        vehicleLabel = uiState.blockingVehicleLabel,
                        isCancelling = uiState.isCancellingBlocking,
                        errorMessage = uiState.blockingError,
                        onCancel = viewModel::cancelBlockingReservation,
                    )
                }
                uiState.vehicle != null -> {
                    ReservationContent(
                        vehicle = uiState.vehicle!!,
                        uiState = uiState,
                        onPlanSelect = viewModel::onPlanSelect,
                        onMinuteChange = viewModel::onMinuteEstimateChange,
                        onHoursChange = viewModel::onHoursChange,
                        onDaysChange = viewModel::onDaysChange,
                        onRetryQuote = viewModel::refreshQuote,
                        onTermsToggle = viewModel::onTermsToggle,
                        onReserve = viewModel::reserve,
                        onCancelReservation = viewModel::cancelReservation,
                        onUnlock = viewModel::unlock,
                    )
                }
            }
        }
    }
}

@Composable
private fun ReservationContent(
    vehicle: VehicleDto,
    uiState: ReservationUiState,
    onPlanSelect: (RentalPlan) -> Unit,
    onMinuteChange: (Int) -> Unit,
    onHoursChange: (Int) -> Unit,
    onDaysChange: (Int) -> Unit,
    onRetryQuote: () -> Unit,
    onTermsToggle: (Boolean) -> Unit,
    onReserve: () -> Unit,
    onCancelReservation: () -> Unit,
    onUnlock: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            VehicleSummaryCard(vehicle = vehicle)

            if (uiState.isReservationActive) {
                Spacer(modifier = Modifier.height(16.dp))
                CountdownBanner(remainingSeconds = uiState.remainingSeconds)
            } else {
                val notice = uiState.notice
                if (notice != null) {
                    Spacer(modifier = Modifier.height(16.dp))
                    NoticeBanner(message = stringResource(notice))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            PlanSelector(
                selected = uiState.selectedPlan,
                pricePerMinute = vehicle.pricePerMinute,
                pricePerHour = vehicle.pricePerHour,
                pricePerDay = vehicle.pricePerDay,
                onSelect = onPlanSelect,
            )

            Spacer(modifier = Modifier.height(16.dp))

            PlanDetailsCard(
                plan = uiState.selectedPlan,
                minuteEstimate = uiState.minuteEstimate,
                hours = uiState.hours,
                days = uiState.days,
                quote = uiState.quote,
                isQuoteLoading = uiState.isQuoteLoading,
                quoteError = uiState.quoteError,
                onMinuteChange = onMinuteChange,
                onHoursChange = onHoursChange,
                onDaysChange = onDaysChange,
                onRetryQuote = onRetryQuote,
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(verticalAlignment = Alignment.Top) {
                Checkbox(
                    checked = uiState.termsAccepted,
                    onCheckedChange = onTermsToggle,
                    colors = CheckboxDefaults.colors(checkedColor = Primary),
                )
                Text(
                    text = stringResource(R.string.reservation_terms_checkbox_label),
                    fontSize = 13.sp,
                    color = TextSecondary,
                    modifier = Modifier
                        .padding(top = 14.dp)
                        .clickable { onTermsToggle(!uiState.termsAccepted) },
                )
            }

            Spacer(modifier = Modifier.height(20.dp))
        }

        if (uiState.isReservationActive) {
            ReservationActiveBottomBar(
                canUnlock = uiState.canUnlock,
                isUnlocking = uiState.isUnlocking,
                isCancelling = uiState.isCancellingReservation,
                errorMessage = uiState.actionError,
                onUnlock = onUnlock,
                onCancel = onCancelReservation,
            )
        } else {
            SelectionBottomBar(
                enabled = uiState.canReserve,
                isReserving = uiState.isReserving,
                errorMessage = uiState.reserveError,
                onReserve = onReserve,
            )
        }
    }
}

@Composable
private fun SelectionBottomBar(
    enabled: Boolean,
    isReserving: Boolean,
    @androidx.annotation.StringRes errorMessage: Int?,
    onReserve: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Surface,
        shadowElevation = 12.dp,
    ) {
        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp)) {
            if (errorMessage != null) {
                Text(text = stringResource(errorMessage), color = Danger, fontSize = 13.sp)
                Spacer(modifier = Modifier.height(8.dp))
            }
            Button(
                onClick = onReserve,
                enabled = enabled,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(Dimens.CornerL),
                colors = ButtonDefaults.buttonColors(containerColor = Primary),
            ) {
                if (isReserving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        color = Color.White,
                        strokeWidth = 2.dp,
                    )
                } else {
                    Text(
                        text = stringResource(R.string.reservation_reserve_button, FREE_RESERVATION_MINUTES),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
    }
}

@Composable
private fun ReservationActiveBottomBar(
    canUnlock: Boolean,
    isUnlocking: Boolean,
    isCancelling: Boolean,
    @androidx.annotation.StringRes errorMessage: Int?,
    onUnlock: () -> Unit,
    onCancel: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Surface,
        shadowElevation = 12.dp,
    ) {
        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp)) {
            if (errorMessage != null) {
                Text(text = stringResource(errorMessage), color = Danger, fontSize = 13.sp)
                Spacer(modifier = Modifier.height(8.dp))
            }
            Button(
                onClick = onUnlock,
                enabled = canUnlock,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(Dimens.CornerL),
                colors = ButtonDefaults.buttonColors(containerColor = Primary),
            ) {
                if (isUnlocking) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        color = Color.White,
                        strokeWidth = 2.dp,
                    )
                } else {
                    Icon(Icons.Outlined.LockOpen, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = stringResource(R.string.reservation_unlock_button), fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            TextButton(
                onClick = onCancel,
                enabled = !isCancelling && !isUnlocking,
                modifier = Modifier.fillMaxWidth().height(46.dp),
                colors = ButtonDefaults.textButtonColors(contentColor = Danger),
            ) {
                if (isCancelling) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Danger,
                        strokeWidth = 2.dp,
                    )
                } else {
                    Text(text = stringResource(R.string.reservation_cancel_button), fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun CountdownBanner(remainingSeconds: Long) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = PrimaryLight,
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Timer, contentDescription = null, tint = Primary, modifier = Modifier.size(22.dp))
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = stringResource(R.string.reservation_countdown_title),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = formatCountdown(remainingSeconds),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Primary,
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = stringResource(R.string.reservation_countdown_hint),
                fontSize = 12.sp,
                color = TextSecondary,
            )
        }
    }
}

@Composable
private fun NoticeBanner(message: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = BgLight,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Outlined.Info, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = message, fontSize = 13.sp, color = TextSecondary)
        }
    }
}

@Composable
private fun BlockingReservationNotice(
    vehicleLabel: String?,
    isCancelling: Boolean,
    @androidx.annotation.StringRes errorMessage: Int?,
    onCancel: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(Dimens.SpaceXl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            Icons.Outlined.WarningAmber,
            contentDescription = null,
            tint = Danger,
            modifier = Modifier.size(48.dp),
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.reservation_blocking_title),
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextPrimary,
        )
        if (vehicleLabel != null) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(text = vehicleLabel, fontSize = 14.sp, color = TextSecondary)
        }
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = stringResource(R.string.reservation_blocking_body),
            fontSize = 14.sp,
            color = TextSecondary,
        )
        if (errorMessage != null) {
            Spacer(modifier = Modifier.height(Dimens.SpaceS))
            Text(text = stringResource(errorMessage), color = Danger, fontSize = 13.sp)
        }
        Spacer(modifier = Modifier.height(20.dp))
        Button(
            onClick = onCancel,
            enabled = !isCancelling,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(Dimens.CornerL),
            colors = ButtonDefaults.buttonColors(containerColor = Danger),
        ) {
            if (isCancelling) {
                CircularProgressIndicator(
                    modifier = Modifier.size(22.dp),
                    color = Color.White,
                    strokeWidth = 2.dp,
                )
            } else {
                Text(text = stringResource(R.string.reservation_cancel_button), fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun VehicleSummaryCard(vehicle: VehicleDto) {
    val typeColor = VehicleType.colorFor(vehicle.type)
    val status = vehicle.vehicleStatus
    val statusLabel = when (status) {
        VehicleStatus.AVAILABLE -> stringResource(R.string.reservation_status_available)
        VehicleStatus.RESERVED -> stringResource(R.string.reservation_status_reserved)
        VehicleStatus.RENTED -> stringResource(R.string.reservation_status_rented)
        VehicleStatus.MAINTENANCE -> stringResource(R.string.reservation_status_maintenance)
        VehicleStatus.UNKNOWN -> vehicle.status
    }
    val (statusColor, statusBg) = status.colors().let { it.foreground to it.background }
    val typeLabelRes = VehicleType.labelFor(vehicle.type)
    val segmentLabelRes = VehicleSegment.labelFor(vehicle.segment)
    val subtitle = listOfNotNull(
        vehicle.plate,
        vehicle.transmission ?: typeLabelRes?.let { stringResource(it) },
        vehicle.seats?.let { stringResource(R.string.common_seat_count, it) },
        segmentLabelRes?.let { stringResource(it) },
    ).joinToString(" · ")

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = Surface,
        shadowElevation = 2.dp,
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .width(76.dp)
                    .height(56.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Brush.linearGradient(colors = listOf(typeColor, typeColor.copy(alpha = 0.7f)))),
                contentAlignment = Alignment.Center,
            ) {
                if (vehicle.imageUrl != null) {
                    AsyncImage(
                        model = vehicle.imageUrl,
                        contentDescription = "${vehicle.brand} ${vehicle.model}",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    Icon(Icons.Outlined.DirectionsCar, contentDescription = null, tint = Color.White, modifier = Modifier.size(30.dp))
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(text = "${vehicle.brand} ${vehicle.model}", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                Spacer(modifier = Modifier.height(2.dp))
                Text(text = subtitle, fontSize = 13.sp, color = TextSecondary)
                if (vehicle.fuelPercent != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(SuccessLight)
                            .padding(horizontal = 8.dp, vertical = 3.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.reservation_fuel_percent, vehicle.fuelPercent),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Success,
                        )
                    }
                }
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(statusBg)
                    .padding(horizontal = 10.dp, vertical = 5.dp),
            ) {
                Text(text = statusLabel, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = statusColor)
            }
        }
    }
}

@Composable
private fun PlanSelector(
    selected: RentalPlan,
    pricePerMinute: Double?,
    pricePerHour: Double?,
    pricePerDay: Double,
    onSelect: (RentalPlan) -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = Surface,
        shadowElevation = 2.dp,
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = stringResource(R.string.reservation_plan_section_title),
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary,
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                PlanChip(
                    title = stringResource(R.string.common_plan_per_minute),
                    priceLabel = pricePerMinute?.let { stringResource(R.string.common_price_per_minute, formatTl(it)) },
                    selected = selected == RentalPlan.DAKIKALIK,
                    onClick = { onSelect(RentalPlan.DAKIKALIK) },
                    modifier = Modifier.weight(1f),
                )
                PlanChip(
                    title = stringResource(R.string.common_plan_hourly),
                    priceLabel = pricePerHour?.let { stringResource(R.string.reservation_plan_price_per_hour, formatTl(it)) },
                    selected = selected == RentalPlan.SAATLIK,
                    onClick = { onSelect(RentalPlan.SAATLIK) },
                    modifier = Modifier.weight(1f),
                )
                PlanChip(
                    title = stringResource(R.string.common_plan_daily),
                    priceLabel = stringResource(R.string.common_amount_tl, formatTl(pricePerDay)),
                    selected = selected == RentalPlan.GUNLUK,
                    onClick = { onSelect(RentalPlan.GUNLUK) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun PlanChip(
    title: String,
    priceLabel: String?,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Fiyatı API'den gelmeyen planlar veri gelene kadar seçilemez.
    val enabled = priceLabel != null
    val shape = RoundedCornerShape(12.dp)

    Column(
        modifier = modifier
            .clip(shape)
            .border(
                width = if (selected) 1.5.dp else 1.dp,
                color = if (selected) Primary else BorderLight,
                shape = shape,
            )
            .background(if (selected) PrimaryLight else Surface)
            .then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier.alpha(0.45f))
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = title,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (selected) Primary else TextPrimary,
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = priceLabel ?: "—",
            fontSize = 12.sp,
            color = if (selected) Primary else TextSecondary,
        )
    }
}

@Composable
private fun PlanDetailsCard(
    plan: RentalPlan,
    minuteEstimate: Int,
    hours: Int,
    days: Int,
    quote: QuoteResponse?,
    isQuoteLoading: Boolean,
    @androidx.annotation.StringRes quoteError: Int?,
    onMinuteChange: (Int) -> Unit,
    onHoursChange: (Int) -> Unit,
    onDaysChange: (Int) -> Unit,
    onRetryQuote: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = Surface,
        shadowElevation = 2.dp,
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = if (plan == RentalPlan.DAKIKALIK) stringResource(R.string.reservation_duration_estimated_label) else stringResource(R.string.common_duration_label),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary,
                )
                when (plan) {
                    RentalPlan.DAKIKALIK -> DurationStepper(
                        valueText = stringResource(R.string.common_minutes_short, minuteEstimate),
                        onDecrement = { onMinuteChange(minuteEstimate - MINUTE_ESTIMATE_STEP) },
                        onIncrement = { onMinuteChange(minuteEstimate + MINUTE_ESTIMATE_STEP) },
                        decrementEnabled = minuteEstimate > MINUTE_ESTIMATE_MIN,
                        incrementEnabled = minuteEstimate < MINUTE_ESTIMATE_MAX,
                    )
                    RentalPlan.SAATLIK -> DurationStepper(
                        valueText = if (hours == 1) stringResource(R.string.reservation_duration_hour_one) else stringResource(R.string.reservation_duration_hours, hours),
                        onDecrement = { onHoursChange(hours - 1) },
                        onIncrement = { onHoursChange(hours + 1) },
                        decrementEnabled = hours > HOURS_MIN,
                        incrementEnabled = hours < HOURS_MAX,
                    )
                    RentalPlan.GUNLUK -> DurationStepper(
                        valueText = if (days == 1) stringResource(R.string.reservation_duration_day_one) else stringResource(R.string.reservation_duration_days, days),
                        onDecrement = { onDaysChange(days - 1) },
                        onIncrement = { onDaysChange(days + 1) },
                        decrementEnabled = days > DAYS_MIN,
                        incrementEnabled = days < DAYS_MAX,
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = Background, thickness = 1.dp)
            Spacer(modifier = Modifier.height(12.dp))

            when {
                quote != null -> {
                    PriceRow(label = stringResource(R.string.common_usage_fee_label), value = stringResource(R.string.common_amount_tl, formatTl(quote.usageFee)))
                    if (quote.startFee != 0.0) {
                        Spacer(modifier = Modifier.height(8.dp))
                        PriceRow(label = stringResource(R.string.reservation_price_start_fee_label), value = stringResource(R.string.common_amount_tl, formatTl(quote.startFee)))
                    }
                    if (quote.serviceFee != 0.0) {
                        Spacer(modifier = Modifier.height(8.dp))
                        PriceRow(label = stringResource(R.string.reservation_price_service_fee_label), value = stringResource(R.string.common_amount_tl, formatTl(quote.serviceFee)))
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    HorizontalDivider(color = Background, thickness = 1.dp)
                    Spacer(modifier = Modifier.height(10.dp))
                    PriceRow(
                        label = stringResource(R.string.reservation_price_estimated_total_label),
                        value = stringResource(R.string.reservation_price_estimated_total_value, formatTl(quote.estimatedTotal)),
                        emphasize = true,
                    )
                    if (isQuoteLoading) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = stringResource(R.string.reservation_quote_updating), fontSize = 12.sp, color = TextSecondary)
                    }
                }
                isQuoteLoading -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Primary, strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(text = stringResource(R.string.reservation_quote_calculating), fontSize = 13.sp, color = TextSecondary)
                    }
                }
                quoteError == null -> {
                    // Rezervasyon aktifken quote çekilmez; ücret kullanıma göre sonda oluşur.
                    Text(
                        text = stringResource(R.string.reservation_price_calculated_after_trip),
                        fontSize = 13.sp,
                        color = TextSecondary,
                    )
                }
                else -> {
                    Text(
                        text = stringResource(quoteError),
                        fontSize = 13.sp,
                        color = Danger,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    TextButton(
                        onClick = onRetryQuote,
                        colors = ButtonDefaults.textButtonColors(contentColor = Primary),
                    ) {
                        Text(text = stringResource(R.string.common_retry), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

@Composable
private fun DurationStepper(
    valueText: String,
    onDecrement: () -> Unit,
    onIncrement: () -> Unit,
    decrementEnabled: Boolean,
    incrementEnabled: Boolean,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        StepperButton(icon = Icons.Outlined.Remove, enabled = decrementEnabled, onClick = onDecrement)
        Text(
            text = valueText,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextPrimary,
            modifier = Modifier.width(64.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
        StepperButton(icon = Icons.Outlined.Add, enabled = incrementEnabled, onClick = onIncrement)
    }
}

@Composable
private fun StepperButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(10.dp)
    Box(
        modifier = Modifier
            .size(34.dp)
            .clip(shape)
            .border(width = 1.dp, color = BorderLight, shape = shape)
            .background(Surface)
            .then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier.alpha(0.4f)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (enabled) Primary else TextSecondary,
            modifier = Modifier.size(18.dp),
        )
    }
}

@Composable
private fun PriceRow(label: String, value: String, emphasize: Boolean = false) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(
            text = label,
            fontSize = if (emphasize) 15.sp else 14.sp,
            fontWeight = if (emphasize) FontWeight.SemiBold else FontWeight.Normal,
            color = if (emphasize) TextPrimary else TextSecondary,
        )
        Text(
            text = value,
            fontSize = if (emphasize) 18.sp else 14.sp,
            fontWeight = if (emphasize) FontWeight.Bold else FontWeight.SemiBold,
            color = if (emphasize) Primary else TextPrimary,
        )
    }
}

// remainingSeconds -> mm:ss
private fun formatCountdown(totalSeconds: Long): String {
    val safe = totalSeconds.coerceAtLeast(0)
    val minutes = safe / 60
    val seconds = safe % 60
    return String.format(Locale.US, "%02d:%02d", minutes, seconds)
}
