package com.flowbytestudio.rencar.ui.screens.wallet

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CardGiftcard
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.flowbytestudio.rencar.R
import com.flowbytestudio.rencar.data.wallet.WalletLimits
import com.flowbytestudio.rencar.ui.common.formatTl
import com.flowbytestudio.rencar.ui.theme.Background
import com.flowbytestudio.rencar.ui.theme.BgLight
import com.flowbytestudio.rencar.ui.theme.BorderColor
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

private val VisaBlue = Color(0xFF1A1F71)
private val MastercardRed = Color(0xFFEB001B)
private val MastercardOrange = Color(0xFFF79E1B)

@Composable
fun WalletScreen(viewModel: WalletViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Bottom nav ile bu sekmeye her dönüşte bakiye + son işlemleri tazele:
    // ödeme başka bir ekranda (TripSummary) alınmış olabilir, ViewModel ise
    // navigasyon boyunca canlı kalıp init{} bir daha çalışmadığı için elle
    // tetiklemek gerekiyor.
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentViewModel by rememberUpdatedState(viewModel)
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                currentViewModel.refresh()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Silme onayı için seçili kart (yerel; onaydan sonra ViewModel'e iletilir).
    var cardPendingDelete by remember { mutableStateOf<WalletCardUiModel?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
    ) {
        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = stringResource(R.string.common_wallet),
            fontSize = 29.5.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary,
        )

        Spacer(modifier = Modifier.height(20.dp))

        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 48.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(color = Primary)
                }
            }
            uiState.errorMessage != null -> {
                WalletErrorState(
                    message = uiState.errorMessage?.let { stringResource(it) } ?: stringResource(R.string.wallet_error_load_fallback),
                    onRetry = viewModel::retry,
                )
            }
            else -> {
                BalanceCard(
                    balance = stringResource(R.string.common_amount_tl, formatTl(uiState.balance)),
                    onAddFunds = viewModel::openTopupSheet,
                )

                Spacer(modifier = Modifier.height(24.dp))

                SectionHeader(
                    title = stringResource(R.string.wallet_saved_cards_title),
                    actionLabel = stringResource(R.string.wallet_add_card_action),
                    onAction = viewModel::openAddCardSheet,
                )

                Spacer(modifier = Modifier.height(Dimens.SpaceXs))

                val cardActionError = uiState.cardActionError
                if (cardActionError != null) {
                    Text(
                        text = stringResource(cardActionError),
                        fontSize = 13.5.sp,
                        color = Danger,
                        modifier = Modifier.padding(bottom = 8.dp),
                    )
                }

                if (uiState.cards.isEmpty()) {
                    EmptyCard(text = stringResource(R.string.wallet_empty_cards))
                } else {
                    SavedCardsCard {
                        uiState.cards.forEachIndexed { index, card ->
                            SavedCardItem(
                                card = card,
                                actionsEnabled = !uiState.cardActionInProgress,
                                onMakeDefault = { viewModel.setDefaultCard(card.id) },
                                onDelete = { cardPendingDelete = card },
                            )
                            if (index != uiState.cards.lastIndex) {
                                CardDivider()
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                SectionHeader(
                    title = stringResource(R.string.wallet_recent_transactions_title),
                    actionLabel = null,
                    onAction = {},
                )

                Spacer(modifier = Modifier.height(Dimens.SpaceXs))

                if (uiState.transactions.isEmpty()) {
                    Text(
                        text = stringResource(R.string.wallet_empty_transactions),
                        fontSize = 14.5.sp,
                        color = TextSecondary,
                    )
                } else {
                    TransactionsCard {
                        uiState.transactions.forEachIndexed { index, transaction ->
                            TransactionItem(transaction = transaction)
                            if (index != uiState.transactions.lastIndex) {
                                CardDivider()
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }

    if (uiState.showTopupSheet) {
        TopupSheet(
            isSubmitting = uiState.isToppingUp,
            errorMessage = uiState.topupError,
            onDismiss = viewModel::dismissTopupSheet,
            onSubmit = viewModel::topup,
        )
    }

    if (uiState.showAddCardSheet) {
        AddCardSheet(
            isSubmitting = uiState.isAddingCard,
            errorMessage = uiState.addCardError,
            onDismiss = viewModel::dismissAddCardSheet,
            onSubmit = viewModel::addCard,
        )
    }

    cardPendingDelete?.let { card ->
        DeleteCardDialog(
            card = card,
            onConfirm = {
                viewModel.deleteCard(card.id)
                cardPendingDelete = null
            },
            onDismiss = { cardPendingDelete = null },
        )
    }
}

@Composable
private fun WalletErrorState(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = message,
            fontSize = 14.5.sp,
            color = Danger,
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = onRetry,
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Primary, contentColor = Color.White),
        ) {
            Text(text = stringResource(R.string.common_retry), fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun BalanceCard(
    balance: String,
    onAddFunds: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(Color(0xFF3B82F6), Color(0xFF1D4ED8)),
                )
            )
            .padding(horizontal = 24.dp, vertical = 28.dp),
    ) {
        // Dekoratif daire sağ üst
        Box(
            modifier = Modifier
                .size(160.dp)
                .clip(RoundedCornerShape(80.dp))
                .background(Color.White.copy(alpha = 0.07f))
                .align(Alignment.TopEnd)
        )

        Column {
            Text(
                text = stringResource(R.string.wallet_balance_label),
                fontSize = 15.5.sp,
                color = Color.White.copy(alpha = 0.80f),
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = balance,
                fontSize = 39.5.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = onAddFunds,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White.copy(alpha = 0.20f),
                    contentColor = Color.White,
                ),
            ) {
                Text(
                    text = stringResource(R.string.wallet_topup_button),
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.5.sp,
                    modifier = Modifier.padding(vertical = 2.dp),
                )
            }
        }
    }
}

@Composable
private fun SectionHeader(
    title: String,
    actionLabel: String?,
    onAction: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            fontSize = 17.5.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextPrimary,
        )
        if (actionLabel != null) {
            Text(
                text = actionLabel,
                fontSize = 15.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = Primary,
                modifier = Modifier.clickable(onClick = onAction),
            )
        }
    }
}

@Composable
private fun EmptyCard(text: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = Surface,
        tonalElevation = 0.dp,
    ) {
        Text(
            text = text,
            fontSize = 14.5.sp,
            color = TextSecondary,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 20.dp),
        )
    }
}

@Composable
private fun SavedCardsCard(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = Surface,
        tonalElevation = 0.dp,
    ) {
        Column(content = content)
    }
}

@Composable
private fun SavedCardItem(
    card: WalletCardUiModel,
    actionsEnabled: Boolean,
    onMakeDefault: () -> Unit,
    onDelete: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CardBrandLogo(brand = card.brand)

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.wallet_card_masked_number, card.last4),
                fontSize = 16.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = stringResource(R.string.wallet_card_expiry_label, card.expiry),
                fontSize = 14.5.sp,
                color = TextSecondary,
            )
        }

        if (card.isDefault) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(SuccessLight)
                    .padding(horizontal = 10.dp, vertical = 5.dp),
            ) {
                Text(
                    text = stringResource(R.string.wallet_default_card_badge),
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Success,
                )
            }
        } else {
            IconButton(
                onClick = onMakeDefault,
                enabled = actionsEnabled,
                modifier = Modifier.size(36.dp),
            ) {
                Icon(
                    imageVector = Icons.Outlined.SwapHoriz,
                    contentDescription = stringResource(R.string.wallet_make_default_content_desc),
                    tint = if (actionsEnabled) Primary else TextSecondary,
                    modifier = Modifier.size(20.dp),
                )
            }
        }

        IconButton(
            onClick = onDelete,
            enabled = actionsEnabled,
            modifier = Modifier.size(36.dp),
        ) {
            Icon(
                imageVector = Icons.Outlined.DeleteOutline,
                contentDescription = stringResource(R.string.wallet_delete_card),
                tint = if (actionsEnabled) Danger else TextSecondary,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
private fun CardBrandLogo(brand: String) {
    val isVisa = brand.equals("VISA", ignoreCase = true)
    Box(
        modifier = Modifier
            .size(width = 48.dp, height = 32.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(if (isVisa) VisaBlue else Color(0xFF252525)),
        contentAlignment = Alignment.Center,
    ) {
        if (isVisa) {
            Text(
                text = stringResource(R.string.wallet_visa_logo_text),
                fontSize = 12.5.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                letterSpacing = 1.sp,
            )
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy((-6).dp)) {
                Box(
                    modifier = Modifier
                        .size(18.dp)
                        .clip(RoundedCornerShape(9.dp))
                        .background(MastercardRed),
                )
                Box(
                    modifier = Modifier
                        .size(18.dp)
                        .clip(RoundedCornerShape(9.dp))
                        .background(MastercardOrange.copy(alpha = 0.9f)),
                )
            }
        }
    }
}

@Composable
private fun TransactionsCard(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = Surface,
        tonalElevation = 0.dp,
    ) {
        Column(content = content)
    }
}

@Composable
private fun TransactionItem(transaction: WalletTransactionUiModel) {
    val icon: ImageVector
    val iconBg: Color
    val iconTint: Color
    when (transaction.kind) {
        WalletTransactionKind.TOPUP -> {
            icon = Icons.Outlined.KeyboardArrowUp
            iconBg = SuccessLight
            iconTint = Success
        }
        WalletTransactionKind.REFERRAL_BONUS -> {
            icon = Icons.Outlined.CardGiftcard
            iconBg = SuccessLight
            iconTint = Success
        }
        WalletTransactionKind.RENTAL_PAYMENT -> {
            icon = Icons.Outlined.DirectionsCar
            iconBg = DangerLight
            iconTint = Danger
        }
        WalletTransactionKind.OTHER -> {
            icon = Icons.Outlined.DirectionsCar
            iconBg = BgLight
            iconTint = TextSecondary
        }
    }

    val isCredit = transaction.amount >= 0
    val amountText = stringResource(
        R.string.wallet_transaction_amount_format,
        if (isCredit) "+" else "-",
        formatTl(kotlin.math.abs(transaction.amount)),
    )
    val amountColor = if (isCredit) Success else Danger
    val title = transaction.titleRes?.let { stringResource(it) } ?: transaction.rawTitle.orEmpty()
    val subtitle = if (transaction.subtitleDescription != null && transaction.subtitleDescription != title) {
        stringResource(R.string.wallet_transaction_subtitle_format, transaction.subtitleDescription, transaction.subtitleDate)
    } else {
        transaction.subtitleDate
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(iconBg),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(20.dp),
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 16.5.sp,
                fontWeight = FontWeight.Medium,
                color = TextPrimary,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                fontSize = 14.5.sp,
                color = TextSecondary,
            )
        }

        Text(
            text = amountText,
            fontSize = 16.5.sp,
            fontWeight = FontWeight.SemiBold,
            color = amountColor,
        )
    }
}

@Composable
private fun CardDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = 16.dp),
        color = Background,
        thickness = 1.dp,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TopupSheet(
    isSubmitting: Boolean,
    @androidx.annotation.StringRes errorMessage: Int?,
    onDismiss: () -> Unit,
    onSubmit: (String) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var amount by remember { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Surface,
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 20.dp)
                .padding(bottom = 8.dp)
                .navigationBarsPadding()
                .imePadding(),
        ) {
            Text(
                text = stringResource(R.string.wallet_topup_sheet_title),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.wallet_topup_range_hint),
                fontSize = 13.5.sp,
                color = TextSecondary,
            )

            Spacer(modifier = Modifier.height(20.dp))

            OutlinedTextField(
                value = amount,
                onValueChange = { input -> amount = input.filter { it.isDigit() }.take(5) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text(stringResource(R.string.wallet_topup_amount_field_label)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Primary,
                    unfocusedBorderColor = BorderColor,
                    focusedContainerColor = Surface,
                    unfocusedContainerColor = Surface,
                ),
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                WalletLimits.QUICK_TOPUP_AMOUNTS.forEach { chip ->
                    AmountChip(
                        label = stringResource(R.string.wallet_amount_chip_format, chip),
                        onClick = { amount = chip.toString() },
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            if (errorMessage != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(text = stringResource(errorMessage), fontSize = 13.5.sp, color = Danger)
            }

            Spacer(modifier = Modifier.height(20.dp))

            SheetPrimaryButton(
                text = stringResource(R.string.wallet_topup_submit_button),
                isLoading = isSubmitting,
                enabled = amount.isNotBlank() && !isSubmitting,
                onClick = { onSubmit(amount) },
            )

            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Composable
private fun AmountChip(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(PrimaryLight)
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = Primary,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddCardSheet(
    isSubmitting: Boolean,
    @androidx.annotation.StringRes errorMessage: Int?,
    onDismiss: () -> Unit,
    onSubmit: (brand: String, last4: String, expMonth: String, expYear: String) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var brand by remember { mutableStateOf("VISA") }
    var last4 by remember { mutableStateOf("") }
    var expMonth by remember { mutableStateOf("") }
    var expYear by remember { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Surface,
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 20.dp)
                .padding(bottom = 8.dp)
                .navigationBarsPadding()
                .imePadding(),
        ) {
            Text(
                text = stringResource(R.string.wallet_add_card_sheet_title),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.wallet_add_card_privacy_note),
                fontSize = 13.5.sp,
                color = TextSecondary,
            )

            Spacer(modifier = Modifier.height(20.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                BrandOption(
                    label = stringResource(R.string.common_brand_visa),
                    selected = brand == "VISA",
                    onClick = { brand = "VISA" },
                    modifier = Modifier.weight(1f),
                )
                BrandOption(
                    label = stringResource(R.string.common_brand_mastercard),
                    selected = brand == "MASTERCARD",
                    onClick = { brand = "MASTERCARD" },
                    modifier = Modifier.weight(1f),
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = last4,
                onValueChange = { input -> last4 = input.filter { it.isDigit() }.take(4) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text(stringResource(R.string.wallet_card_last4_field_label)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                shape = RoundedCornerShape(14.dp),
                colors = sheetFieldColors(),
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = expMonth,
                    onValueChange = { input -> expMonth = input.filter { it.isDigit() }.take(2) },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    label = { Text(stringResource(R.string.wallet_expiry_month_field_label)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(14.dp),
                    colors = sheetFieldColors(),
                )
                OutlinedTextField(
                    value = expYear,
                    onValueChange = { input -> expYear = input.filter { it.isDigit() }.take(4) },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    label = { Text(stringResource(R.string.wallet_expiry_year_field_label)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(14.dp),
                    colors = sheetFieldColors(),
                )
            }

            if (errorMessage != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(text = stringResource(errorMessage), fontSize = 13.5.sp, color = Danger)
            }

            Spacer(modifier = Modifier.height(20.dp))

            SheetPrimaryButton(
                text = stringResource(R.string.wallet_add_card_submit_button),
                isLoading = isSubmitting,
                enabled = last4.length == 4 && expMonth.isNotBlank() && expYear.length == 4 && !isSubmitting,
                onClick = { onSubmit(brand, last4, expMonth, expYear) },
            )

            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Composable
private fun BrandOption(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) PrimaryLight else BgLight)
            .border(
                width = if (selected) 1.5.dp else 1.dp,
                color = if (selected) Primary else BorderColor,
                shape = RoundedCornerShape(12.dp),
            )
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (selected) Primary else TextPrimary,
        )
    }
}

@Composable
private fun SheetPrimaryButton(
    text: String,
    isLoading: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Primary,
            contentColor = Color.White,
            disabledContainerColor = Primary.copy(alpha = 0.5f),
        ),
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(22.dp),
                color = Color.White,
                strokeWidth = 2.dp,
            )
        } else {
            Text(text = text, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun DeleteCardDialog(
    card: WalletCardUiModel,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Surface,
        title = {
            Text(text = stringResource(R.string.wallet_delete_card), fontWeight = FontWeight.Bold, color = TextPrimary)
        },
        text = {
            Text(
                text = stringResource(R.string.wallet_delete_card_confirm_message, card.last4),
                color = TextSecondary,
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(text = stringResource(R.string.wallet_delete_confirm_button), color = Danger, fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.common_cancel), color = TextSecondary)
            }
        },
    )
}

@Composable
private fun sheetFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = Primary,
    unfocusedBorderColor = BorderColor,
    focusedContainerColor = Surface,
    unfocusedContainerColor = Surface,
)
