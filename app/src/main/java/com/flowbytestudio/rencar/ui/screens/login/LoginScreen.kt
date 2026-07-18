package com.flowbytestudio.rencar.ui.screens.login

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.flowbytestudio.rencar.R
import com.flowbytestudio.rencar.data.auth.AuthConstants
import com.flowbytestudio.rencar.ui.theme.*

@Composable
fun LoginScreen(
    onLoggedIn: () -> Unit,
    onNavigateToRegister: () -> Unit,
    viewModel: LoginViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState.isLoggedIn) {
        if (uiState.isLoggedIn) onLoggedIn()
    }

    // Android sistem geri tuşu/hareketi yönetimi
    BackHandler(enabled = uiState.step == LoginStep.OTP) {
        viewModel.onChangePhone()
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = Dimens.SpaceXl)
        ) {
            Spacer(modifier = Modifier.height(Dimens.SpaceM))
            if (uiState.step == LoginStep.OTP) {
                AuthBackButton(
                    modifier = Modifier.padding(start = Dimens.SpaceXs, top = Dimens.SpaceXxs),
                    onClick = { viewModel.onChangePhone() }
                )
            }
            Spacer(modifier = Modifier.height(Dimens.SpaceXl))

            if (uiState.step == LoginStep.PHONE) {
                PhoneStepContent(uiState, viewModel, onNavigateToRegister)
            } else {
                OtpStepContent(uiState, viewModel)
            }
        }
    }
}

@Composable
private fun PhoneStepContent(
    uiState: LoginUiState,
    viewModel: LoginViewModel,
    onNavigateToRegister: () -> Unit
) {
    Column {
        Text(
            text = stringResource(R.string.login_welcome_title),
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )
        Spacer(modifier = Modifier.height(Dimens.SpaceXs))
        Text(
            text = stringResource(R.string.login_phone_subtitle),
            fontSize = 15.sp,
            color = TextSecondary,
            lineHeight = 22.sp
        )
        Spacer(modifier = Modifier.height(Dimens.SpaceXxl))

        PhoneNumberInput(
            value = uiState.phone,
            onValueChange = viewModel::onPhoneChange
        )

        Spacer(modifier = Modifier.height(Dimens.SpaceM))
        AuthInfoText(text = stringResource(R.string.login_phone_sms_info))

        if (uiState.error != null) {
            Text(
                text = stringResource(uiState.error),
                color = Danger,
                fontSize = 13.sp,
                modifier = Modifier.padding(top = Dimens.SpaceXs)
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        PrimaryAuthButton(
            text = stringResource(R.string.login_send_code_button),
            icon = Icons.Outlined.ChatBubbleOutline,
            isLoading = uiState.isLoading,
            enabled = uiState.phone.length == AuthConstants.PHONE_DIGIT_COUNT && !uiState.isLoading,
            onClick = viewModel::onRequestOtp
        )

        AuthFooterText(
            mainText = stringResource(R.string.login_no_account_prompt),
            actionText = stringResource(R.string.login_register_action),
            onClick = onNavigateToRegister
        )
        Spacer(modifier = Modifier.height(Dimens.SpaceXxl))
    }
}

@Composable
private fun OtpStepContent(
    uiState: LoginUiState,
    viewModel: LoginViewModel
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(RoundedCornerShape(Dimens.CornerButton))
                .background(BgLight),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.PhonelinkLock,
                contentDescription = null,
                tint = Primary,
                modifier = Modifier.size(32.dp)
            )
        }
        Spacer(modifier = Modifier.height(Dimens.SpaceXl))
        Text(
            text = stringResource(R.string.login_otp_title),
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )
        Spacer(modifier = Modifier.height(Dimens.SpaceS))

        val formattedPhone = formatPhoneNumber(uiState.phone)
        val otpInstruction = stringResource(R.string.login_otp_instruction, formattedPhone)
        Text(
            text = buildAnnotatedString {
                append(otpInstruction)
                addStyle(style = SpanStyle(fontWeight = FontWeight.Bold, color = TextPrimary), start = 0, end = "+90 $formattedPhone".length)
            },
            fontSize = 15.sp,
            color = TextSecondary,
            textAlign = TextAlign.Center,
            lineHeight = 22.sp
        )
        
        Spacer(modifier = Modifier.height(Dimens.SpaceXxl))

        OtpInputFields(
            value = uiState.code,
            onValueChange = viewModel::onCodeChange
        )

        Spacer(modifier = Modifier.height(Dimens.SpaceXl))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Outlined.Timer,
                contentDescription = null,
                tint = TextSecondary,
                modifier = Modifier.size(Dimens.IconSizeS)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = if (uiState.canResendOtp) stringResource(R.string.login_resend_code_action) else stringResource(R.string.login_resend_code_countdown, uiState.timerSeconds.toString().padStart(2, '0')),
                fontSize = 14.sp,
                color = if (uiState.canResendOtp) Primary else TextSecondary,
                modifier = Modifier.clickable(enabled = uiState.canResendOtp) { viewModel.onRequestOtp() }
            )
        }

        if (uiState.error != null) {
            Text(
                text = stringResource(uiState.error),
                color = Danger,
                fontSize = 13.sp,
                modifier = Modifier.padding(top = Dimens.SpaceM)
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        PrimaryAuthButton(
            text = stringResource(R.string.login_verify_continue_button),
            isLoading = uiState.isLoading,
            enabled = uiState.code.length == AuthConstants.OTP_DIGIT_COUNT && !uiState.isLoading,
            onClick = viewModel::onVerifyOtp
        )

        AuthFooterText(
            mainText = stringResource(R.string.login_wrong_number_prompt),
            actionText = stringResource(R.string.login_change_number_action),
            onClick = { viewModel.onChangePhone() }
        )
        Spacer(modifier = Modifier.height(Dimens.SpaceXxl))
    }
}

@Composable
fun AuthBackButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        modifier = modifier
            .size(44.dp)
            .clip(RoundedCornerShape(Dimens.CornerM))
            .background(BgLight)
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = stringResource(R.string.common_back),
            tint = TextPrimary,
            modifier = Modifier.size(Dimens.IconSizeM)
        )
    }
}

@Composable
fun PhoneNumberInput(value: String, onValueChange: (String) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .height(Dimens.ControlHeight)
                .width(80.dp)
                .clip(RoundedCornerShape(Dimens.CornerCard))
                .background(BgLight),
            contentAlignment = Alignment.Center
        ) {
            Text(text = stringResource(R.string.login_country_code_label), fontSize = 15.sp, fontWeight = FontWeight.Medium, color = TextPrimary)
        }
        Spacer(modifier = Modifier.width(Dimens.SpaceS))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.weight(1f),
            placeholder = { Text(stringResource(R.string.login_phone_placeholder), color = TextSecondary.copy(alpha = 0.5f)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            visualTransformation = PhoneVisualTransformation(),
            shape = RoundedCornerShape(Dimens.CornerCard),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Primary,
                unfocusedBorderColor = BorderColor,
                focusedContainerColor = Surface,
                unfocusedContainerColor = Surface
            )
        )
    }
}

@Composable
fun OtpInputFields(value: String, onValueChange: (String) -> Unit) {
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        decorationBox = {
            Row(horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceXs)) {
                repeat(AuthConstants.OTP_DIGIT_COUNT) { index ->
                    val char = value.getOrNull(index)?.toString() ?: ""
                    val isFocused = value.length == index
                    Box(
                        modifier = Modifier
                            .size(width = 48.dp, height = 56.dp)
                            .clip(RoundedCornerShape(Dimens.CornerCard))
                            .border(
                                width = 2.dp,
                                color = if (isFocused) Primary else BorderColor,
                                shape = RoundedCornerShape(Dimens.CornerCard)
                            )
                            .background(Surface),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = char,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }
                }
            }
        }
    )
}

@Composable
fun PrimaryAuthButton(
    text: String,
    icon: ImageVector? = null,
    isLoading: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(Dimens.ControlHeight)
            .shadow(if (enabled) 8.dp else 0.dp, RoundedCornerShape(Dimens.CornerButton)),
        enabled = enabled,
        shape = RoundedCornerShape(Dimens.CornerButton),
        colors = ButtonDefaults.buttonColors(
            containerColor = Primary,
            disabledContainerColor = Primary.copy(alpha = 0.5f)
        )
    ) {
        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White, strokeWidth = 2.dp)
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (icon != null) {
                    Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(Dimens.IconSizeM))
                    Spacer(modifier = Modifier.width(Dimens.SpaceXs))
                }
                Text(text = text, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun AuthInfoText(text: String) {
    Row(verticalAlignment = Alignment.Top, modifier = Modifier.padding(horizontal = Dimens.SpaceXxs)) {
        Icon(
            imageVector = Icons.Outlined.Info,
            contentDescription = null,
            tint = TextSecondary,
            modifier = Modifier.size(Dimens.IconSizeS).padding(top = 2.dp)
        )
        Spacer(modifier = Modifier.width(Dimens.SpaceXs))
        Text(text = text, fontSize = 12.sp, color = TextSecondary, lineHeight = 18.sp)
    }
}

@Composable
fun AuthFooterText(mainText: String, actionText: String, onClick: () -> Unit) {
    Text(
        text = buildAnnotatedString {
            append(mainText)
            withStyle(style = SpanStyle(color = Primary, fontWeight = FontWeight.Bold)) {
                append(actionText)
            }
        },
        fontSize = 14.sp,
        color = TextSecondary,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = Dimens.SpaceXl)
            .clickable { onClick() },
        textAlign = TextAlign.Center
    )
}

private fun formatPhoneNumber(phone: String): String {
    return buildString {
        phone.forEachIndexed { index, c ->
            append(c)
            if (index == 2 || index == 5 || index == 7) append(" ")
        }
    }.trim()
}

class PhoneVisualTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val trimmed = if (text.text.length >= AuthConstants.PHONE_DIGIT_COUNT) text.text.substring(0, AuthConstants.PHONE_DIGIT_COUNT) else text.text
        var out = ""
        for (i in trimmed.indices) {
            out += trimmed[i]
            if (i == 2 || i == 5 || i == 7) out += " "
        }

        val offsetMapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                if (offset <= 2) return offset
                if (offset <= 5) return offset + 1
                if (offset <= 7) return offset + 2
                if (offset <= 10) return offset + 3
                return 13
            }

            override fun transformedToOriginal(offset: Int): Int {
                if (offset <= 3) return offset
                if (offset <= 7) return offset - 1
                if (offset <= 10) return offset - 2
                if (offset <= 13) return offset - 3
                return 10
            }
        }
        return TransformedText(AnnotatedString(out), offsetMapping)
    }
}
