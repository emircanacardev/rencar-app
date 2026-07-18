package com.flowbytestudio.rencar.ui.screens.map

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AirlineSeatReclineNormal
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.LocalGasStation
import androidx.compose.material.icons.outlined.NearMe
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.flowbytestudio.rencar.R
import com.flowbytestudio.rencar.data.vehicles.VehicleDto
import com.flowbytestudio.rencar.data.vehicles.VehicleStatus
import com.flowbytestudio.rencar.data.vehicles.vehicleStatus
import com.flowbytestudio.rencar.ui.common.formatTl
import com.flowbytestudio.rencar.ui.theme.BgLight
import com.flowbytestudio.rencar.ui.theme.BorderLight
import com.flowbytestudio.rencar.ui.theme.Dimens
import com.flowbytestudio.rencar.ui.theme.Primary
import com.flowbytestudio.rencar.ui.theme.PrimaryLight
import com.flowbytestudio.rencar.ui.theme.Success
import com.flowbytestudio.rencar.ui.theme.Surface
import com.flowbytestudio.rencar.ui.theme.TextPrimary
import com.flowbytestudio.rencar.ui.theme.TextSecondary

private const val MINUTES_PER_DAY = 1440.0
private const val HOURS_PER_DAY = 24.0

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VehicleDetailSheet(
    vehicle: VehicleDto,
    distanceLabel: String?,
    canReserve: Boolean,
    sheetState: SheetState,
    onDismiss: () -> Unit,
    onReserve: () -> Unit,
) {
    val typeColor = VehicleType.colorFor(vehicle.type)
    val status = vehicle.vehicleStatus
    val isAvailable = status == VehicleStatus.AVAILABLE
    val statusLabel = when (status) {
        VehicleStatus.AVAILABLE -> stringResource(R.string.map_vehicle_status_available)
        VehicleStatus.RESERVED -> stringResource(R.string.map_vehicle_status_reserved)
        VehicleStatus.RENTED -> stringResource(R.string.map_vehicle_status_rented)
        VehicleStatus.MAINTENANCE -> stringResource(R.string.map_vehicle_status_maintenance)
        VehicleStatus.UNKNOWN -> vehicle.status
    }
    val (statusColor, statusBg) = status.colors().let { it.foreground to it.background }
    val segmentLabelRes = VehicleSegment.labelFor(vehicle.segment)
    val segmentLabel = segmentLabelRes?.let { stringResource(it) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Surface,
    ) {
        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "${vehicle.brand} ${vehicle.model}",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    modifier = Modifier.weight(1f),
                )
                if (segmentLabel != null) {
                    Chip(text = segmentLabel, textColor = Primary, background = PrimaryLight)
                    Spacer(modifier = Modifier.width(6.dp))
                }
                Chip(text = statusLabel, textColor = statusColor, background = statusBg)
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = if (distanceLabel != null) {
                    stringResource(R.string.map_vehicle_plate_with_distance, vehicle.plate, distanceLabel)
                } else {
                    vehicle.plate
                },
                fontSize = 13.sp,
                color = TextSecondary,
            )

            Spacer(modifier = Modifier.height(16.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        Brush.linearGradient(colors = listOf(typeColor, typeColor.copy(alpha = 0.7f))),
                    ),
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
                    Icon(
                        imageVector = Icons.Outlined.DirectionsCar,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.height(72.dp),
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SpecTile(
                    icon = Icons.Outlined.LocalGasStation,
                    label = stringResource(R.string.map_spec_fuel_label),
                    value = vehicle.fuelPercent?.let { stringResource(R.string.map_spec_fuel_percent_value, it) } ?: "—",
                    fuelProgress = vehicle.fuelPercent?.let { it / 100f },
                    modifier = Modifier.weight(1f),
                )
                SpecTile(
                    icon = Icons.Outlined.NearMe,
                    label = stringResource(R.string.map_spec_range_label),
                    value = vehicle.rangeKm?.let { stringResource(R.string.map_spec_range_value, it) } ?: "—",
                    caption = vehicle.rangeKm?.let { stringResource(R.string.map_spec_range_caption_full_tank) },
                    modifier = Modifier.weight(1f),
                )
            }

            Spacer(modifier = Modifier.height(Dimens.SpaceXs))

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SpecTile(
                    icon = Icons.Outlined.Settings,
                    label = stringResource(R.string.map_spec_transmission_label),
                    value = vehicle.transmission ?: "—",
                    modifier = Modifier.weight(1f),
                )
                SpecTile(
                    icon = Icons.Outlined.AirlineSeatReclineNormal,
                    label = stringResource(R.string.map_spec_seats_label),
                    value = vehicle.seats?.let { stringResource(R.string.common_seat_count, it) } ?: "—",
                    modifier = Modifier.weight(1f),
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // TODO: API şu an sadece pricePerDay döndürüyor; dakikalık/saatlik ücret
            // burada client-side türetiliyor. Backend gerçek pricePerMinute/pricePerHour
            // dönmeye başlarsa bu türetme kaldırılıp gerçek alanlar kullanılmalı.
            val derivedPricePerMinute = vehicle.pricePerDay / MINUTES_PER_DAY
            val derivedPricePerHour = vehicle.pricePerDay / HOURS_PER_DAY

            Row(verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = stringResource(R.string.common_amount_tl, formatTl(derivedPricePerMinute)),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                    )
                    Text(
                        text = stringResource(R.string.map_price_per_minute_unit),
                        fontSize = 14.sp,
                        color = TextSecondary,
                        modifier = Modifier.padding(bottom = 3.dp),
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = stringResource(R.string.map_price_per_hour_label, formatTl(derivedPricePerHour)),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextSecondary,
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            if (isAvailable) {
                // v2: doğrudan kilit açma yok; kiralama yalnız rezervasyon sonrası açılır.
                Button(
                    onClick = onReserve,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    enabled = canReserve,
                    shape = RoundedCornerShape(Dimens.CornerL),
                    colors = ButtonDefaults.buttonColors(containerColor = Primary),
                ) {
                    Text(text = stringResource(R.string.map_reserve_button), fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                }
            } else {
                // Meşgul araçlarda aksiyon yok; kullanıcı yalnızca detayı görür.
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(Dimens.CornerL))
                        .background(BgLight)
                        .padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = stringResource(R.string.map_vehicle_unavailable_message),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextSecondary,
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun Chip(text: String, textColor: Color, background: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(background)
            .padding(horizontal = 10.dp, vertical = 4.dp),
    ) {
        Text(text = text, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = textColor)
    }
}

@Composable
private fun SpecTile(
    icon: ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    caption: String? = null,
    fuelProgress: Float? = null,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(BgLight)
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = TextSecondary,
                modifier = Modifier.size(13.dp),
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(text = label, fontSize = 11.sp, color = TextSecondary)
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = value,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary,
        )

        if (caption != null) {
            Text(text = caption, fontSize = 10.sp, color = TextSecondary)
        }

        if (fuelProgress != null) {
            Spacer(modifier = Modifier.height(6.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(BorderLight),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(fuelProgress.coerceIn(0f, 1f))
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Success),
                )
            }
        }
    }
}
