package com.flowbytestudio.rencar.ui.screens.login

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.PhonelinkLock
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.flowbytestudio.rencar.ui.theme.Background
import com.flowbytestudio.rencar.ui.theme.BgLight
import com.flowbytestudio.rencar.ui.theme.BorderColor
import com.flowbytestudio.rencar.ui.theme.Danger
import com.flowbytestudio.rencar.ui.theme.Primary
import com.flowbytestudio.rencar.ui.theme.Surface as SurfaceColor
import com.flowbytestudio.rencar.ui.theme.TextPrimary
import com.flowbytestudio.rencar.ui.theme.TextSecondary

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
                .padding(horizontal = 24.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            if (uiState.step == LoginStep.OTP) {
                AuthBackButton(
                    modifier = Modifier.padding(start = 8.dp, top = 4.dp),
                    onClick = { viewModel.onChangePhone() }
                )
            }
            Spacer(modifier = Modifier.height(24.dp))

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
            text = "Tekrar hoş geldin",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Telefon numaranı gir, SMS ile doğrulama kodu gönderelim.",
            fontSize = 15.sp,
            color = TextSecondary,
            lineHeight = 22.sp
        )
        Spacer(modifier = Modifier.height(32.dp))

        PhoneNumberInput(
            value = uiState.phone,
            onValueChange = viewModel::onPhoneChange
        )

        Spacer(modifier = Modifier.height(16.dp))
        AuthInfoText(text = "6 haneli kodu bu numaraya göndereceğiz. SMS ücreti operatörüne bağlıdır.")

        if (uiState.error != null) {
            Text(
                text = uiState.error,
                color = Danger,
                fontSize = 13.sp,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        PrimaryAuthButton(
            text = "Kod Gönder",
            icon = Icons.Outlined.ChatBubbleOutline,
            isLoading = uiState.isLoading,
            enabled = uiState.phone.length == 10 && !uiState.isLoading,
            onClick = viewModel::onRequestOtp
        )

        AuthFooterText(
            mainText = "Hesabın yok mu? ",
            actionText = "Kayıt ol",
            onClick = onNavigateToRegister
        )
        Spacer(modifier = Modifier.height(32.dp))
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
                .clip(RoundedCornerShape(18.dp))
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
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "Telefonunu doğrula",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )
        Spacer(modifier = Modifier.height(12.dp))
        
        val formattedPhone = formatPhoneNumber(uiState.phone)
        Text(
            text = buildAnnotatedString {
                append("+90 $formattedPhone")
                addStyle(style = SpanStyle(fontWeight = FontWeight.Bold, color = TextPrimary), start = 0, end = length)
                append(" numarasına gönderdiğimiz 6 haneli kodu gir.")
            },
            fontSize = 15.sp,
            color = TextSecondary,
            textAlign = TextAlign.Center,
            lineHeight = 22.sp
        )
        
        Spacer(modifier = Modifier.height(32.dp))

        OtpInputFields(
            value = uiState.code,
            onValueChange = viewModel::onCodeChange
        )

        Spacer(modifier = Modifier.height(24.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Outlined.Timer,
                contentDescription = null,
                tint = TextSecondary,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = if (uiState.canResendOtp) "Kodu tekrar gönder" else "Kodu tekrar gönder · 0:${uiState.timerSeconds.toString().padStart(2, '0')}",
                fontSize = 14.sp,
                color = if (uiState.canResendOtp) Primary else TextSecondary,
                modifier = Modifier.clickable(enabled = uiState.canResendOtp) { viewModel.onRequestOtp() }
            )
        }

        if (uiState.error != null) {
            Text(
                text = uiState.error,
                color = Danger,
                fontSize = 13.sp,
                modifier = Modifier.padding(top = 16.dp)
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        PrimaryAuthButton(
            text = "Doğrula ve Devam Et",
            isLoading = uiState.isLoading,
            enabled = uiState.code.length == 6 && !uiState.isLoading,
            onClick = viewModel::onVerifyOtp
        )

        AuthFooterText(
            mainText = "Numara yanlış mı? ",
            actionText = "Değiştir",
            onClick = { viewModel.onChangePhone() }
        )
        Spacer(modifier = Modifier.height(32.dp))
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
            .clip(RoundedCornerShape(12.dp))
            .background(BgLight)
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = "Geri",
            tint = TextPrimary,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
fun PhoneNumberInput(value: String, onValueChange: (String) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .height(56.dp)
                .width(80.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(BgLight),
            contentAlignment = Alignment.Center
        ) {
            Text(text = "TR  +90", fontSize = 15.sp, fontWeight = FontWeight.Medium, color = TextPrimary)
        }
        Spacer(modifier = Modifier.width(12.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.weight(1f),
            placeholder = { Text("532 000 00 00", color = TextSecondary.copy(alpha = 0.5f)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            visualTransformation = PhoneVisualTransformation(),
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Primary,
                unfocusedBorderColor = BorderColor,
                focusedContainerColor = SurfaceColor,
                unfocusedContainerColor = SurfaceColor
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
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                repeat(6) { index ->
                    val char = value.getOrNull(index)?.toString() ?: ""
                    val isFocused = value.length == index
                    Box(
                        modifier = Modifier
                            .size(width = 48.dp, height = 56.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .border(
                                width = 2.dp,
                                color = if (isFocused) Primary else BorderColor,
                                shape = RoundedCornerShape(14.dp)
                            )
                            .background(SurfaceColor),
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
            .height(56.dp)
            .shadow(if (enabled) 8.dp else 0.dp, RoundedCornerShape(18.dp)),
        enabled = enabled,
        shape = RoundedCornerShape(18.dp),
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
                    Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(text = text, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun AuthInfoText(text: String) {
    Row(verticalAlignment = Alignment.Top, modifier = Modifier.padding(horizontal = 4.dp)) {
        Icon(
            imageVector = Icons.Outlined.Info,
            contentDescription = null,
            tint = TextSecondary,
            modifier = Modifier.size(16.dp).padding(top = 2.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
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
            .padding(top = 24.dp)
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
        val trimmed = if (text.text.length >= 10) text.text.substring(0, 10) else text.text
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
