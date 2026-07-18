package com.flowbytestudio.rencar.ui.screens.profile

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.CardGiftcard
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.HourglassEmpty
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.flowbytestudio.rencar.R
import com.flowbytestudio.rencar.data.license.LicenseStatus
import com.flowbytestudio.rencar.ui.common.formatTl
import com.flowbytestudio.rencar.ui.theme.Background
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

private val WarningAmber = Color(0xFFF59E0B)
private val WarningAmberLight = Color(0x1AF59E0B)

@Composable
fun ProfileScreen(
    onNavigateToSettings: () -> Unit = {},
    onNavigateToLicenseUpload: () -> Unit = {},
    onNavigateToReferral: () -> Unit = {},
    onNavigateToPaymentMethods: () -> Unit = {},
    viewModel: ProfileViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    ProfileContent(
        uiState = uiState,
        onEditClick = {},
        onPaymentMethodsClick = onNavigateToPaymentMethods,
        onSettingsClick = onNavigateToSettings,
        onSupportClick = {},
        onLicenseActionClick = onNavigateToLicenseUpload,
        onReferralClick = onNavigateToReferral,
        onRefreshSessionClick = viewModel::onRefreshSession,
        onLogoutClick = viewModel::onLogoutClick,
    )
}

@Composable
private fun ProfileContent(
    uiState: ProfileUiState,
    onEditClick: () -> Unit,
    onPaymentMethodsClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onSupportClick: () -> Unit,
    onLicenseActionClick: () -> Unit,
    onReferralClick: () -> Unit,
    onRefreshSessionClick: () -> Unit,
    onLogoutClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
    ) {
        Spacer(modifier = Modifier.height(12.dp))

        UserHeaderCard(
            name = uiState.name,
            phone = uiState.phone,
            avatarUrl = uiState.avatarUrl,
            onEditClick = onEditClick,
        )

        Spacer(modifier = Modifier.height(12.dp))

        uiState.stats?.let { stats ->
            StatsCard(stats = stats)
            Spacer(modifier = Modifier.height(12.dp))
        }

        LicenseSection(
            uiState = uiState,
            onLicenseActionClick = onLicenseActionClick,
            onRefreshSessionClick = onRefreshSessionClick,
        )

        Spacer(modifier = Modifier.height(12.dp))

        ReferralCard(onClick = onReferralClick)

        Spacer(modifier = Modifier.height(12.dp))

        MenuCard {
            MenuItem(
                icon = Icons.Outlined.CreditCard,
                label = stringResource(R.string.profile_menu_payment_methods),
                onClick = onPaymentMethodsClick,
            )
            MenuDivider()
            MenuItem(
                icon = Icons.Outlined.Settings,
                label = stringResource(R.string.common_settings),
                onClick = onSettingsClick,
            )
            MenuDivider()
            MenuItem(
                icon = Icons.AutoMirrored.Outlined.HelpOutline,
                label = stringResource(R.string.profile_menu_help_support),
                onClick = onSupportClick,
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        LogoutCard(onClick = onLogoutClick)

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
private fun UserHeaderCard(
    name: String,
    phone: String,
    avatarUrl: String?,
    onEditClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(Color(0xFFE8E0F0)),
            contentAlignment = Alignment.Center,
        ) {
            if (avatarUrl != null) {
                AsyncImage(
                    model = avatarUrl,
                    contentDescription = name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                Icon(
                    imageVector = Icons.Outlined.Person,
                    contentDescription = null,
                    tint = Color(0xFF9C72CB),
                    modifier = Modifier.size(34.dp),
                )
            }
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = name,
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = phone,
                fontSize = 15.sp,
                color = TextSecondary,
            )
        }

        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Surface)
                .clickable(onClick = onEditClick),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Outlined.Edit,
                contentDescription = stringResource(R.string.profile_edit_button_content_description),
                tint = TextSecondary,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

@Composable
private fun StatsCard(stats: ProfileStats) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = Surface,
        tonalElevation = 0.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(PrimaryLight),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Outlined.Insights,
                    contentDescription = null,
                    tint = Primary,
                    modifier = Modifier.size(22.dp),
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.profile_stats_trips_this_month, stats.tripCount),
                    fontSize = 16.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary,
                )
                Text(
                    text = stringResource(R.string.profile_stats_total_spent, formatTl(stats.totalSpent)),
                    fontSize = 14.5.sp,
                    color = TextSecondary,
                )
                if (stats.totalMinutes > 0 || stats.totalKm > 0.0) {
                    Spacer(modifier = Modifier.height(Dimens.SpaceXs))
                    Row(horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceXs)) {
                        StatChip(text = stringResource(R.string.common_minutes_short, stats.totalMinutes))
                        StatChip(text = stringResource(R.string.common_distance_km, "%.1f".format(stats.totalKm)))
                    }
                }
            }
        }
    }
}

@Composable
private fun StatChip(text: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(Background)
            .padding(horizontal = 8.dp, vertical = 3.dp),
    ) {
        Text(
            text = text,
            fontSize = 12.5.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextSecondary,
        )
    }
}

@Composable
private fun LicenseSection(
    uiState: ProfileUiState,
    onLicenseActionClick: () -> Unit,
    onRefreshSessionClick: () -> Unit,
) {
    when (uiState.licenseStatus) {
        LicenseStatus.APPROVED -> ApprovedLicenseCard(
            licenseClass = uiState.licenseClass,
            showRefresh = uiState.canRefreshSession,
            isRefreshing = uiState.isRefreshingSession,
            onRefreshSessionClick = onRefreshSessionClick,
        )
        LicenseStatus.UNDER_REVIEW -> LicenseStatusCard(
            icon = Icons.Outlined.HourglassEmpty,
            iconTint = WarningAmber,
            iconBg = WarningAmberLight,
            title = stringResource(R.string.profile_license_under_review_title),
            subtitle = stringResource(R.string.profile_license_under_review_subtitle),
            badgeText = stringResource(R.string.profile_license_under_review_badge),
            badgeColor = WarningAmber,
            badgeBg = WarningAmberLight,
        )
        LicenseStatus.REJECTED -> LicenseStatusCard(
            icon = Icons.Outlined.WarningAmber,
            iconTint = Danger,
            iconBg = DangerLight,
            title = stringResource(R.string.profile_license_rejected_title),
            subtitle = uiState.rejectReason ?: stringResource(R.string.profile_license_rejected_subtitle_fallback),
            badgeText = stringResource(R.string.profile_license_rejected_badge_reupload),
            badgeColor = Primary,
            badgeBg = PrimaryLight,
            onClick = onLicenseActionClick,
        )
        LicenseStatus.NOT_SUBMITTED -> LicenseStatusCard(
            icon = Icons.Outlined.Shield,
            iconTint = Primary,
            iconBg = PrimaryLight,
            title = stringResource(R.string.profile_license_not_submitted_title),
            subtitle = stringResource(R.string.profile_license_not_submitted_subtitle),
            badgeText = stringResource(R.string.profile_license_not_submitted_badge_verify),
            badgeColor = Primary,
            badgeBg = PrimaryLight,
            onClick = onLicenseActionClick,
        )
        // UNKNOWN (henüz yüklenmedi/hata) ve beklenmeyen durumlar: kart gizli kalır,
        // böylece durum bilinmeden yanıltıcı "doğrula" istemi gösterilmez.
        LicenseStatus.UNKNOWN -> Unit
    }
}

@Composable
private fun ApprovedLicenseCard(
    @androidx.annotation.StringRes licenseClass: Int,
    showRefresh: Boolean,
    isRefreshing: Boolean,
    onRefreshSessionClick: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = Surface,
        tonalElevation = 0.dp,
    ) {
        Column {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(SuccessLight),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Shield,
                        contentDescription = null,
                        tint = Success,
                        modifier = Modifier.size(22.dp),
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.profile_license_approved_title),
                        fontSize = 16.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary,
                    )
                    Text(
                        text = stringResource(licenseClass),
                        fontSize = 14.5.sp,
                        color = TextSecondary,
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(SuccessLight)
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                ) {
                    Text(
                        text = stringResource(R.string.profile_license_approved_badge),
                        fontSize = 14.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Success,
                    )
                }
            }

            if (showRefresh) {
                HorizontalDivider(color = Background, thickness = 1.dp)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = !isRefreshing, onClick = onRefreshSessionClick)
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    if (isRefreshing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = Primary,
                            strokeWidth = 2.dp,
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Outlined.Refresh,
                            contentDescription = null,
                            tint = Primary,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                    Text(
                        text = stringResource(R.string.profile_refresh_session_button),
                        fontSize = 14.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Primary,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun LicenseStatusCard(
    icon: ImageVector,
    iconTint: Color,
    iconBg: Color,
    title: String,
    subtitle: String,
    badgeText: String,
    badgeColor: Color,
    badgeBg: Color,
    onClick: (() -> Unit)? = null,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = Surface,
        tonalElevation = 0.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(iconBg),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(22.dp),
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 16.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary,
                )
                Text(
                    text = subtitle,
                    fontSize = 14.5.sp,
                    color = TextSecondary,
                    maxLines = 2,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(badgeBg)
                    .padding(horizontal = 10.dp, vertical = 5.dp),
            ) {
                Text(
                    text = badgeText,
                    fontSize = 14.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = badgeColor,
                )
            }
        }
    }
}

@Composable
private fun ReferralCard(onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        color = Surface,
        tonalElevation = 0.dp,
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(PrimaryLight),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Outlined.CardGiftcard,
                    contentDescription = null,
                    tint = Primary,
                    modifier = Modifier.size(22.dp),
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.profile_referral_title),
                    fontSize = 16.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary,
                )
                Text(
                    text = stringResource(R.string.profile_referral_subtitle),
                    fontSize = 13.5.sp,
                    color = TextSecondary,
                )
            }

            Icon(
                imageVector = Icons.Outlined.ChevronRight,
                contentDescription = null,
                tint = TextSecondary,
            )
        }
    }
}

@Composable
private fun MenuCard(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = Surface,
        tonalElevation = 0.dp,
    ) {
        Column(content = content)
    }
}

@Composable
private fun MenuItem(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    tint: Color = TextSecondary,
    labelColor: Color = TextPrimary,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(22.dp),
        )
        Text(
            text = label,
            fontSize = 16.5.sp,
            color = labelColor,
            modifier = Modifier.weight(1f),
        )
        Icon(
            imageVector = Icons.Outlined.ChevronRight,
            contentDescription = null,
            tint = BorderLight,
            modifier = Modifier.size(20.dp),
        )
    }
}

@Composable
private fun MenuDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(start = 50.dp),
        color = Background,
        thickness = 1.dp,
    )
}

@Composable
private fun LogoutCard(onClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = Surface,
        tonalElevation = 0.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(vertical = 18.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.Logout,
                contentDescription = null,
                tint = Danger,
                modifier = Modifier.size(20.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.profile_logout_button),
                fontSize = 16.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = Danger,
            )
        }
    }
}
