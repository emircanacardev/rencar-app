package com.flowbytestudio.rencar.ui.screens.tripsummary

import android.graphics.Bitmap
import android.webkit.WebView
import android.webkit.WebViewClient
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.flowbytestudio.rencar.R
import com.flowbytestudio.rencar.data.cards.CardDto
import com.flowbytestudio.rencar.ui.common.formatTl
import com.flowbytestudio.rencar.ui.theme.Background
import com.flowbytestudio.rencar.ui.theme.BorderColor
import com.flowbytestudio.rencar.ui.theme.BorderLight
import com.flowbytestudio.rencar.ui.theme.Danger
import com.flowbytestudio.rencar.ui.theme.Primary
import com.flowbytestudio.rencar.ui.theme.PrimaryLight
import com.flowbytestudio.rencar.ui.theme.Success
import com.flowbytestudio.rencar.ui.theme.SuccessLight
import com.flowbytestudio.rencar.ui.theme.Surface
import com.flowbytestudio.rencar.ui.theme.TextPrimary
import com.flowbytestudio.rencar.ui.theme.TextSecondary
import com.flowbytestudio.rencar.ui.theme.Dimens
import java.net.URI
import java.util.Locale
import org.json.JSONTokener

@Composable
fun TripSummaryScreen(
    rentalId: String,
    onDone: () -> Unit,
) {
    val viewModel: TripSummaryViewModel = viewModel(
        factory = viewModelFactory {
            initializer { TripSummaryViewModel(rentalId) }
        },
    )
    val uiState by viewModel.uiState.collectAsState()

    Box(modifier = Modifier.fillMaxSize().background(Background)) {
        when {
            uiState.isLoading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Primary)
                }
            }
            uiState.loadError != null -> {
                val loadError = uiState.loadError ?: return@Box
                Column(
                    modifier = Modifier.fillMaxSize().padding(Dimens.SpaceXl),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(text = stringResource(loadError), color = Danger, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(Dimens.SpaceS))
                    Button(onClick = viewModel::load) {
                        Text(stringResource(R.string.common_retry))
                    }
                }
            }
            else -> {
                Column(modifier = Modifier.fillMaxSize()) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = Dimens.SpaceL),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Spacer(modifier = Modifier.height(Dimens.SpaceXxl))

                        Box(
                            modifier = Modifier
                                .size(52.dp)
                                .clip(CircleShape)
                                .background(Success),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Check,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(Dimens.IconSizeL),
                            )
                        }

                        Spacer(modifier = Modifier.height(Dimens.SpaceM))

                        Text(
                            text = stringResource(R.string.trip_summary_title),
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                        )

                        Spacer(modifier = Modifier.height(Dimens.SpaceXxs))

                        uiState.rental?.vehicle?.let { vehicle ->
                            Text(
                                text = "${vehicle.brand} ${vehicle.model} · ${vehicle.plate}",
                                fontSize = 13.sp,
                                color = TextSecondary,
                            )
                        }

                        Spacer(modifier = Modifier.height(Dimens.SpaceL))

                        val emDash = stringResource(R.string.common_em_dash)
                        Row(horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceS)) {
                            SummaryTile(
                                label = stringResource(R.string.common_duration_label),
                                value = uiState.rental?.let { formatDuration(it.durationMinutes) } ?: emDash,
                                modifier = Modifier.weight(1f),
                            )
                            SummaryTile(
                                label = stringResource(R.string.common_distance_label),
                                value = uiState.rental?.let { formatKm(it.distanceKm) } ?: emDash,
                                modifier = Modifier.weight(1f),
                            )
                        }

                        Spacer(modifier = Modifier.height(Dimens.SpaceM))

                        BreakdownCard(uiState = uiState)

                        if (uiState.isPaid) {
                            Spacer(modifier = Modifier.height(Dimens.SpaceM))
                            PaidReceiptCard(uiState = uiState)
                        } else if (uiState.isPayable) {
                            Spacer(modifier = Modifier.height(Dimens.SpaceM))
                            PaymentSection(
                                uiState = uiState,
                                onMethodSelect = viewModel::onMethodSelect,
                                onCardSelect = viewModel::onCardSelect,
                                onDiscountCodeChange = viewModel::onDiscountCodeChange,
                                onIyzicoSubMethodSelect = viewModel::onIyzicoSubMethodSelect,
                                onIyzicoCardHolderNameChange = viewModel::onIyzicoCardHolderNameChange,
                                onIyzicoCardNumberChange = viewModel::onIyzicoCardNumberChange,
                                onIyzicoExpireMonthChange = viewModel::onIyzicoExpireMonthChange,
                                onIyzicoExpireYearChange = viewModel::onIyzicoExpireYearChange,
                                onIyzicoCvcChange = viewModel::onIyzicoCvcChange,
                            )
                        }

                        Spacer(modifier = Modifier.height(Dimens.SpaceL))
                    }

                    BottomBar(
                        uiState = uiState,
                        onPay = viewModel::pay,
                        onDone = onDone,
                    )
                }
            }
        }

        uiState.iyzicoCheckoutUrl?.let { url ->
            IyzicoCheckoutDialog(
                url = url,
                onReturnedToBackend = viewModel::onIyzicoWebViewReturnedToBackend,
                onDismiss = viewModel::onIyzicoCheckoutCancelled,
            )
        }

        uiState.iyzicoCheckoutHtml?.let { html ->
            Iyzico3dsDialog(
                html = html,
                onReturnedToBackend = viewModel::onIyzico3dsWebViewReturnedToBackend,
                onDismiss = viewModel::onIyzicoCheckoutCancelled,
            )
        }
    }
}

@Composable
private fun BottomBar(
    uiState: TripSummaryUiState,
    onPay: () -> Unit,
    onDone: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Surface,
        shadowElevation = 12.dp,
    ) {
        Column(modifier = Modifier.padding(horizontal = Dimens.SpaceL, vertical = Dimens.SpaceS)) {
            if (uiState.isPayable) {
                val payError = uiState.payError
                if (payError != null) {
                    Text(text = stringResource(payError), color = Danger, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(Dimens.SpaceXs))
                }
                Button(
                    onClick = onPay,
                    enabled = uiState.canPay && !uiState.isPaying,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    shape = RoundedCornerShape(Dimens.CornerL),
                    colors = ButtonDefaults.buttonColors(containerColor = Primary),
                ) {
                    if (uiState.isPaying) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            color = Color.White,
                            strokeWidth = 2.dp,
                        )
                    } else {
                        Text(
                            text = uiState.payableAmount?.let {
                                stringResource(R.string.trip_summary_pay_with_amount_button, formatTl(it))
                            } ?: stringResource(R.string.trip_summary_pay_button),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            } else {
                Button(
                    onClick = onDone,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    shape = RoundedCornerShape(Dimens.CornerL),
                    colors = ButtonDefaults.buttonColors(containerColor = Primary),
                ) {
                    Text(text = stringResource(R.string.trip_summary_done_button), fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun SummaryTile(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(Dimens.CornerCard),
        color = Surface,
        shadowElevation = 2.dp,
    ) {
        Column(
            modifier = Modifier.padding(vertical = Dimens.SpaceS),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(text = label, fontSize = 12.sp, color = TextSecondary)
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = value, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
        }
    }
}

@Composable
private fun BreakdownCard(uiState: TripSummaryUiState) {
    val rental = uiState.rental
    val emDash = stringResource(R.string.common_em_dash)
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(Dimens.CornerL),
        color = Surface,
        shadowElevation = 2.dp,
    ) {
        Column(modifier = Modifier.padding(Dimens.SpaceM)) {
            BreakdownRow(
                label = stringResource(R.string.common_usage_fee_label),
                value = uiState.usageFee?.let { stringResource(R.string.common_amount_tl, formatTl(it)) } ?: emDash,
            )

            rental?.startFee?.takeIf { it > 0.0 }?.let { fee ->
                Spacer(modifier = Modifier.height(Dimens.SpaceXs))
                BreakdownRow(
                    label = stringResource(R.string.trip_summary_start_fee_label),
                    value = stringResource(R.string.common_amount_tl, formatTl(fee)),
                )
            }

            rental?.serviceFee?.takeIf { it > 0.0 }?.let { fee ->
                Spacer(modifier = Modifier.height(Dimens.SpaceXs))
                BreakdownRow(
                    label = stringResource(R.string.trip_summary_service_fee_label),
                    value = stringResource(R.string.common_amount_tl, formatTl(fee)),
                )
            }

            Spacer(modifier = Modifier.height(Dimens.SpaceS))
            HorizontalDivider(color = Background, thickness = 1.dp)
            Spacer(modifier = Modifier.height(Dimens.SpaceS))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    text = stringResource(R.string.trip_summary_total_label),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary,
                )
                Text(
                    text = uiState.totalAmount?.let { stringResource(R.string.common_amount_tl, formatTl(it)) } ?: emDash,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                )
            }
        }
    }
}

@Composable
private fun BreakdownRow(
    label: String,
    value: String,
    color: Color = Color.Unspecified,
) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(
            text = label,
            fontSize = 14.sp,
            fontWeight = if (color == Color.Unspecified) FontWeight.Normal else FontWeight.SemiBold,
            color = if (color == Color.Unspecified) TextSecondary else color,
        )
        Text(
            text = value,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (color == Color.Unspecified) TextPrimary else color,
        )
    }
}

@Composable
private fun PaymentSection(
    uiState: TripSummaryUiState,
    onMethodSelect: (PaymentMethodOption) -> Unit,
    onCardSelect: (String) -> Unit,
    onDiscountCodeChange: (String) -> Unit,
    onIyzicoSubMethodSelect: (IyzicoSubMethod) -> Unit,
    onIyzicoCardHolderNameChange: (String) -> Unit,
    onIyzicoCardNumberChange: (String) -> Unit,
    onIyzicoExpireMonthChange: (String) -> Unit,
    onIyzicoExpireYearChange: (String) -> Unit,
    onIyzicoCvcChange: (String) -> Unit,
) {
    val emDash = stringResource(R.string.common_em_dash)
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(Dimens.CornerL),
        color = Surface,
        shadowElevation = 2.dp,
    ) {
        Column(modifier = Modifier.padding(Dimens.SpaceM)) {
            Text(
                text = stringResource(R.string.trip_summary_payment_method_title),
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary,
            )

            Spacer(modifier = Modifier.height(Dimens.SpaceS))

            Row(horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceS)) {
                MethodChip(
                    icon = Icons.Outlined.AccountBalanceWallet,
                    title = stringResource(R.string.common_wallet),
                    subtitle = uiState.walletBalance?.let { stringResource(R.string.common_amount_tl, formatTl(it)) } ?: emDash,
                    subtitleColor = if (uiState.walletInsufficient) Danger else TextSecondary,
                    selected = uiState.selectedMethod == PaymentMethodOption.WALLET,
                    onClick = { onMethodSelect(PaymentMethodOption.WALLET) },
                    modifier = Modifier.weight(1f),
                )
                MethodChip(
                    icon = Icons.Outlined.CreditCard,
                    title = stringResource(R.string.trip_summary_method_card),
                    subtitle = if (uiState.cards.isEmpty()) {
                        stringResource(R.string.trip_summary_no_cards_subtitle)
                    } else {
                        stringResource(R.string.trip_summary_card_count_subtitle, uiState.cards.size)
                    },
                    subtitleColor = TextSecondary,
                    selected = uiState.selectedMethod == PaymentMethodOption.CARD,
                    onClick = { onMethodSelect(PaymentMethodOption.CARD) },
                    modifier = Modifier.weight(1f),
                )
                MethodChip(
                    icon = Icons.Outlined.Language,
                    title = stringResource(R.string.trip_summary_method_iyzico),
                    subtitle = stringResource(R.string.trip_summary_iyzico_secure_subtitle),
                    subtitleColor = TextSecondary,
                    selected = uiState.selectedMethod == PaymentMethodOption.IYZICO,
                    onClick = { onMethodSelect(PaymentMethodOption.IYZICO) },
                    modifier = Modifier.weight(1f),
                )
            }

            if (uiState.selectedMethod == PaymentMethodOption.WALLET && uiState.walletInsufficient) {
                Spacer(modifier = Modifier.height(Dimens.SpaceXs))
                Text(
                    text = stringResource(R.string.trip_summary_wallet_insufficient_warning),
                    fontSize = 12.sp,
                    color = Danger,
                )
            }

            if (uiState.selectedMethod == PaymentMethodOption.CARD) {
                Spacer(modifier = Modifier.height(Dimens.SpaceS))
                if (uiState.cards.isEmpty()) {
                    Text(
                        text = stringResource(R.string.trip_summary_no_saved_cards_hint),
                        fontSize = 13.sp,
                        color = TextSecondary,
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(Dimens.SpaceXs)) {
                        uiState.cards.forEach { card ->
                            CardRow(
                                card = card,
                                selected = card.id == uiState.selectedCardId,
                                onClick = { onCardSelect(card.id) },
                            )
                        }
                    }
                }
            }

            if (uiState.selectedMethod == PaymentMethodOption.IYZICO) {
                Spacer(modifier = Modifier.height(Dimens.SpaceS))
                IyzicoSubMethodSection(
                    uiState = uiState,
                    onSubMethodSelect = onIyzicoSubMethodSelect,
                    onCardHolderNameChange = onIyzicoCardHolderNameChange,
                    onCardNumberChange = onIyzicoCardNumberChange,
                    onExpireMonthChange = onIyzicoExpireMonthChange,
                    onExpireYearChange = onIyzicoExpireYearChange,
                    onCvcChange = onIyzicoCvcChange,
                )
            }

            // İyzico ödemesinde indirim kodu desteklenmiyor (spec: "IYZICO'da kullanılamaz").
            if (uiState.selectedMethod != PaymentMethodOption.IYZICO) {
                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = Background, thickness = 1.dp)
                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = stringResource(R.string.trip_summary_discount_code_label),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary,
                )
                Spacer(modifier = Modifier.height(Dimens.SpaceXs))
                OutlinedTextField(
                    value = uiState.discountCode,
                    onValueChange = onDiscountCodeChange,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text(stringResource(R.string.trip_summary_discount_code_placeholder), color = TextSecondary.copy(alpha = 0.5f)) },
                    singleLine = true,
                    shape = RoundedCornerShape(Dimens.CornerCard),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Primary,
                        unfocusedBorderColor = BorderColor,
                        focusedContainerColor = Surface,
                        unfocusedContainerColor = Surface,
                    ),
                )
            }
        }
    }
}

@Composable
private fun MethodChip(
    icon: ImageVector,
    title: String,
    subtitle: String,
    subtitleColor: Color,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(Dimens.CornerM)
    Column(
        modifier = modifier
            .clip(shape)
            .border(
                width = if (selected) 1.5.dp else 1.dp,
                color = if (selected) Primary else BorderLight,
                shape = shape,
            )
            .background(if (selected) PrimaryLight else Surface)
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = Dimens.SpaceXs),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (selected) Primary else TextSecondary,
            modifier = Modifier.size(Dimens.IconSizeM),
        )
        Spacer(modifier = Modifier.height(Dimens.SpaceXxs))
        Text(
            text = title,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (selected) Primary else TextPrimary,
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(text = subtitle, fontSize = 12.sp, color = subtitleColor)
    }
}

@Composable
private fun CardRow(
    card: CardDto,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(Dimens.CornerM)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .border(
                width = if (selected) 1.5.dp else 1.dp,
                color = if (selected) Primary else BorderLight,
                shape = shape,
            )
            .background(if (selected) PrimaryLight else Surface)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .width(40.dp)
                .height(28.dp)
                .clip(RoundedCornerShape(Dimens.CornerXs))
                .background(brandColor(card.brand)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Outlined.CreditCard,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(Dimens.IconSizeS),
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.trip_summary_card_masked, brandLabel(card.brand), card.last4),
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary,
            )
            val expiry = String.format(Locale.US, "%02d/%02d", card.expMonth, card.expYear % 100)
            Text(
                text = if (card.isDefault) {
                    stringResource(R.string.trip_summary_card_expiry_default, expiry)
                } else {
                    stringResource(R.string.trip_summary_card_expiry, expiry)
                },
                fontSize = 12.sp,
                color = TextSecondary,
            )
        }

        if (selected) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = null,
                tint = Primary,
                modifier = Modifier.size(Dimens.IconSizeM),
            )
        }
    }
}

@Composable
private fun PaidReceiptCard(uiState: TripSummaryUiState) {
    val receipt = uiState.receipt
    val rental = uiState.rental
    val method = receipt?.method ?: rental?.paymentMethod
    val total = receipt?.totalPrice ?: rental?.totalPrice
    val discount = receipt?.discountAmount ?: rental?.discountAmount ?: 0.0
    val paid = receipt?.paidAmount ?: total?.let { (it - discount).coerceAtLeast(0.0) }

    val emDash = stringResource(R.string.common_em_dash)
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(Dimens.CornerL),
        color = Surface,
        shadowElevation = 2.dp,
    ) {
        Column(modifier = Modifier.padding(Dimens.SpaceM)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(Dimens.CornerS))
                        .background(SuccessLight)
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                ) {
                    Text(
                        text = stringResource(R.string.trip_summary_paid_badge),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Success,
                    )
                }
                Spacer(modifier = Modifier.width(Dimens.SpaceXs))
                Text(
                    text = stringResource(PaymentMethodOption.from(method).displayLabelRes()),
                    fontSize = 13.sp,
                    color = TextSecondary,
                )
            }

            Spacer(modifier = Modifier.height(Dimens.SpaceS))

            if (discount > 0.0) {
                BreakdownRow(
                    label = stringResource(R.string.trip_summary_discount_label),
                    value = "−" + stringResource(R.string.common_amount_tl, formatTl(discount)),
                    color = Success,
                )
                Spacer(modifier = Modifier.height(Dimens.SpaceXs))
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    text = stringResource(R.string.trip_summary_paid_amount_label),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary,
                )
                Text(
                    text = paid?.let { stringResource(R.string.common_amount_tl, formatTl(it)) } ?: emDash,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                )
            }

            receipt?.walletBalance?.let { balance ->
                Spacer(modifier = Modifier.height(Dimens.SpaceS))
                HorizontalDivider(color = Background, thickness = 1.dp)
                Spacer(modifier = Modifier.height(Dimens.SpaceS))
                BreakdownRow(
                    label = stringResource(R.string.trip_summary_remaining_balance_label),
                    value = stringResource(R.string.common_amount_tl, formatTl(balance)),
                )
            }

            receipt?.card?.let { card ->
                Spacer(modifier = Modifier.height(Dimens.SpaceS))
                HorizontalDivider(color = Background, thickness = 1.dp)
                Spacer(modifier = Modifier.height(Dimens.SpaceS))
                BreakdownRow(
                    label = stringResource(R.string.trip_summary_method_card),
                    value = stringResource(R.string.trip_summary_card_masked, brandLabel(card.brand), card.last4),
                )
            }
        }
    }
}

private fun brandLabel(brand: String): String = when (brand.uppercase()) {
    "VISA" -> "Visa"
    "MASTERCARD" -> "Mastercard"
    else -> brand
}

private fun brandColor(brand: String): Color = when (brand.uppercase()) {
    "VISA" -> Color(0xFF1A1F71)
    "MASTERCARD" -> Color(0xFFEB001B)
    else -> Color(0xFF334155)
}

@Composable
private fun formatDuration(minutes: Int): String {
    val hours = minutes / 60
    val remaining = minutes % 60
    return when {
        hours <= 0 -> stringResource(R.string.common_minutes_short, minutes)
        remaining == 0 -> stringResource(R.string.trip_summary_duration_hours, hours)
        else -> stringResource(R.string.trip_summary_duration_hours_minutes, hours, remaining)
    }
}

@Composable
private fun formatKm(km: Double): String {
    val turkish = Locale("tr", "TR")
    return if (km % 1.0 == 0.0) {
        stringResource(R.string.trip_summary_distance_km_whole, km.toInt())
    } else {
        stringResource(R.string.common_distance_km, String.format(turkish, "%.1f", km))
    }
}

// İyzico'nun barındırdığı checkout sayfasını (paymentPageUrl) gösteren WebView diyaloğu.
// Kart bilgisi bu ekranda hiç toplanmaz; kullanıcı kartını doğrudan İyzico'nun sayfasına
// girer. Ödeme bitince WebView backend'in callback host'una (BACKEND_HOST) döner —
// bu, akışın tamamlandığının tek güvenilir sinyalidir.
private const val BACKEND_HOST = "rencarv2.halitkalayci.com"

@Composable
private fun IyzicoCheckoutDialog(
    url: String,
    onReturnedToBackend: () -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Column(modifier = Modifier.fillMaxSize().background(Surface)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Dimens.SpaceM, vertical = Dimens.SpaceS),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = stringResource(R.string.trip_summary_iyzico_dialog_title),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary,
                )
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = stringResource(R.string.trip_summary_close_content_description),
                    tint = TextSecondary,
                    modifier = Modifier.clickable(onClick = onDismiss),
                )
            }
            HorizontalDivider(color = Background, thickness = 1.dp)

            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { context ->
                    WebView(context).apply {
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        webViewClient = object : WebViewClient() {
                            override fun onPageStarted(
                                view: WebView?,
                                loadedUrl: String?,
                                favicon: Bitmap?,
                            ) {
                                super.onPageStarted(view, loadedUrl, favicon)
                                val host = loadedUrl?.let { runCatching { URI(it).host }.getOrNull() }
                                if (host == BACKEND_HOST) {
                                    onReturnedToBackend()
                                }
                            }
                        }
                        loadUrl(url)
                    }
                },
            )
        }
    }
}

// İyzico üzerinden üç tahsilat yolu sunulur: banka onayı istemcide toplanmayan
// barındırılan ödeme sayfası, kart bilgisiyle 3-D Secure doğrulamalı tahsilat ve
// kart bilgisiyle doğrudan (3-D Secure'suz) tahsilat.
@Composable
private fun IyzicoSubMethodSection(
    uiState: TripSummaryUiState,
    onSubMethodSelect: (IyzicoSubMethod) -> Unit,
    onCardHolderNameChange: (String) -> Unit,
    onCardNumberChange: (String) -> Unit,
    onExpireMonthChange: (String) -> Unit,
    onExpireYearChange: (String) -> Unit,
    onCvcChange: (String) -> Unit,
) {
    Column {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            SubMethodTab(
                label = stringResource(R.string.trip_summary_iyzico_hosted_page_tab),
                selected = uiState.iyzicoSubMethod == IyzicoSubMethod.HOSTED_PAGE,
                onClick = { onSubMethodSelect(IyzicoSubMethod.HOSTED_PAGE) },
                modifier = Modifier.weight(1f),
            )
            SubMethodTab(
                label = stringResource(R.string.trip_summary_iyzico_threeds_tab),
                selected = uiState.iyzicoSubMethod == IyzicoSubMethod.CARD_3DS,
                onClick = { onSubMethodSelect(IyzicoSubMethod.CARD_3DS) },
                modifier = Modifier.weight(1f),
            )
            SubMethodTab(
                label = stringResource(R.string.trip_summary_iyzico_direct_card_tab),
                selected = uiState.iyzicoSubMethod == IyzicoSubMethod.CARD_DIRECT,
                onClick = { onSubMethodSelect(IyzicoSubMethod.CARD_DIRECT) },
                modifier = Modifier.weight(1f),
            )
        }

        Spacer(modifier = Modifier.height(Dimens.SpaceS))

        when (uiState.iyzicoSubMethod) {
            IyzicoSubMethod.HOSTED_PAGE -> {
                Text(
                    text = stringResource(R.string.trip_summary_iyzico_hosted_page_info),
                    fontSize = 12.sp,
                    color = TextSecondary,
                )
            }
            IyzicoSubMethod.CARD_3DS, IyzicoSubMethod.CARD_DIRECT -> {
                IyzicoCardForm(
                    uiState = uiState,
                    onCardHolderNameChange = onCardHolderNameChange,
                    onCardNumberChange = onCardNumberChange,
                    onExpireMonthChange = onExpireMonthChange,
                    onExpireYearChange = onExpireYearChange,
                    onCvcChange = onCvcChange,
                )
                Spacer(modifier = Modifier.height(Dimens.SpaceXs))
                Text(
                    text = if (uiState.iyzicoSubMethod == IyzicoSubMethod.CARD_3DS) {
                        stringResource(R.string.trip_summary_iyzico_threeds_info)
                    } else {
                        stringResource(R.string.trip_summary_iyzico_direct_info)
                    },
                    fontSize = 12.sp,
                    color = TextSecondary,
                )
            }
        }
    }
}

@Composable
private fun IyzicoCardForm(
    uiState: TripSummaryUiState,
    onCardHolderNameChange: (String) -> Unit,
    onCardNumberChange: (String) -> Unit,
    onExpireMonthChange: (String) -> Unit,
    onExpireYearChange: (String) -> Unit,
    onCvcChange: (String) -> Unit,
) {
    Column {
        OutlinedTextField(
            value = uiState.iyzicoCardHolderName,
            onValueChange = onCardHolderNameChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.trip_summary_card_holder_name_label)) },
            singleLine = true,
            shape = RoundedCornerShape(Dimens.CornerCard),
            colors = iyzicoFieldColors(),
        )
        Spacer(modifier = Modifier.height(Dimens.SpaceXs))
        OutlinedTextField(
            value = uiState.iyzicoCardNumber,
            onValueChange = onCardNumberChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.trip_summary_card_number_label)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number,
            ),
            shape = RoundedCornerShape(Dimens.CornerCard),
            colors = iyzicoFieldColors(),
        )
        Spacer(modifier = Modifier.height(Dimens.SpaceXs))
        Row(horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceXs)) {
            OutlinedTextField(
                value = uiState.iyzicoExpireMonth,
                onValueChange = onExpireMonthChange,
                modifier = Modifier.weight(1f),
                label = { Text(stringResource(R.string.trip_summary_expire_month_label)) },
                placeholder = { Text(stringResource(R.string.trip_summary_expire_month_placeholder)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                ),
                shape = RoundedCornerShape(Dimens.CornerCard),
                colors = iyzicoFieldColors(),
            )
            OutlinedTextField(
                value = uiState.iyzicoExpireYear,
                onValueChange = onExpireYearChange,
                modifier = Modifier.weight(1f),
                label = { Text(stringResource(R.string.trip_summary_expire_year_label)) },
                placeholder = { Text(stringResource(R.string.trip_summary_expire_year_placeholder)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                ),
                shape = RoundedCornerShape(Dimens.CornerCard),
                colors = iyzicoFieldColors(),
            )
            OutlinedTextField(
                value = uiState.iyzicoCvc,
                onValueChange = onCvcChange,
                modifier = Modifier.weight(1f),
                label = { Text(stringResource(R.string.trip_summary_cvc_label)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                ),
                shape = RoundedCornerShape(Dimens.CornerCard),
                colors = iyzicoFieldColors(),
            )
        }
    }
}

@Composable
private fun iyzicoFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = Primary,
    unfocusedBorderColor = BorderColor,
    focusedContainerColor = Surface,
    unfocusedContainerColor = Surface,
)

@Composable
private fun SubMethodTab(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(10.dp)
    Box(
        modifier = modifier
            .clip(shape)
            .border(
                width = if (selected) 1.5.dp else 1.dp,
                color = if (selected) Primary else BorderLight,
                shape = shape,
            )
            .background(if (selected) PrimaryLight else Surface)
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 4.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            fontSize = 11.5.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            softWrap = false,
            color = if (selected) Primary else TextSecondary,
        )
    }
}

// İyzico'nun banka doğrulama HTML'ini (threeDSHtmlContentDecoded) doğrudan render eden
// WebView diyaloğu. Form otomatik submit olur, kullanıcı SMS kodunu girer; İyzico son
// adımda backend host'umuza (BACKEND_HOST) döner ve orada "Ödeme başarılı" sayfasını
// gösterir — bu sayfanın HTML'i onReturnedToBackend'e taşınıp paymentId'i parse eder.
@Composable
private fun Iyzico3dsDialog(
    html: String,
    onReturnedToBackend: (pageHtml: String) -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Column(modifier = Modifier.fillMaxSize().background(Surface)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Dimens.SpaceM, vertical = Dimens.SpaceS),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = stringResource(R.string.trip_summary_threeds_dialog_title),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary,
                )
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = stringResource(R.string.trip_summary_close_content_description),
                    tint = TextSecondary,
                    modifier = Modifier.clickable(onClick = onDismiss),
                )
            }
            HorizontalDivider(color = Background, thickness = 1.dp)

            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { context ->
                    WebView(context).apply {
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        webViewClient = object : WebViewClient() {
                            override fun onPageFinished(view: WebView?, loadedUrl: String?) {
                                super.onPageFinished(view, loadedUrl)
                                val host = loadedUrl?.let { runCatching { URI(it).host }.getOrNull() }
                                if (host == BACKEND_HOST) {
                                    view?.evaluateJavascript(
                                        "document.documentElement.outerHTML",
                                    ) { rawHtml ->
                                        // evaluateJavascript sonucu JSON-string olarak gelir (kaçışlı); çöz.
                                        val decoded = runCatching {
                                            JSONTokener(rawHtml).nextValue() as String
                                        }.getOrDefault(rawHtml.orEmpty())
                                        onReturnedToBackend(decoded)
                                    }
                                }
                            }
                        }
                        loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)
                    }
                },
            )
        }
    }
}
