package com.flowbytestudio.rencar.ui.screens.map

import androidx.compose.runtime.Composable
import com.flowbytestudio.rencar.data.vehicles.VehicleStatus
import com.flowbytestudio.rencar.ui.theme.BgLight
import com.flowbytestudio.rencar.ui.theme.Danger
import com.flowbytestudio.rencar.ui.theme.DangerLight
import com.flowbytestudio.rencar.ui.theme.Primary
import com.flowbytestudio.rencar.ui.theme.PrimaryLight
import com.flowbytestudio.rencar.ui.theme.Success
import com.flowbytestudio.rencar.ui.theme.SuccessLight
import com.flowbytestudio.rencar.ui.theme.TextSecondary

/** [VehicleStatus] rozetinin rengi + arka planı — harita detayı ve rezervasyon
 * ekranı aynı renk paletini paylaşır, yalnız etiket metninin büyük/küçük harfi farklıdır. */
data class VehicleStatusColors(val foreground: androidx.compose.ui.graphics.Color, val background: androidx.compose.ui.graphics.Color)

@Composable
fun VehicleStatus.colors(): VehicleStatusColors = when (this) {
    VehicleStatus.AVAILABLE -> VehicleStatusColors(Success, SuccessLight)
    VehicleStatus.RESERVED -> VehicleStatusColors(Primary, PrimaryLight)
    VehicleStatus.RENTED -> VehicleStatusColors(Danger, DangerLight)
    VehicleStatus.MAINTENANCE, VehicleStatus.UNKNOWN -> VehicleStatusColors(TextSecondary, BgLight)
}
