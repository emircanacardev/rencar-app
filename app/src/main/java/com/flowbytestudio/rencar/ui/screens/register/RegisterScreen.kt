package com.flowbytestudio.rencar.ui.screens.register

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.PersonAddAlt
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.flowbytestudio.rencar.data.auth.AuthConstants
import com.flowbytestudio.rencar.ui.screens.login.AuthFooterText
import com.flowbytestudio.rencar.ui.screens.login.PhoneNumberInput
import com.flowbytestudio.rencar.ui.screens.login.PrimaryAuthButton
import com.flowbytestudio.rencar.ui.theme.Background
import com.flowbytestudio.rencar.ui.theme.BgLight
import com.flowbytestudio.rencar.ui.theme.BorderColor
import com.flowbytestudio.rencar.ui.theme.Danger
import com.flowbytestudio.rencar.ui.theme.Primary
import com.flowbytestudio.rencar.ui.theme.Surface as SurfaceColor
import com.flowbytestudio.rencar.ui.theme.TextPrimary
import com.flowbytestudio.rencar.ui.theme.TextSecondary

@Composable
fun RegisterScreen(
    onRegistered: () -> Unit,
    onNavigateToLogin: () -> Unit,
    viewModel: RegisterViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState.isRegistered) {
        if (uiState.isRegistered) onRegistered()
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
            IconButton(
                onClick = onNavigateToLogin,
                modifier = Modifier
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
            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Hesap oluştur",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Rencar'ı kullanmaya başlamak için birkaç bilgiye ihtiyacımız var.",
                fontSize = 15.sp,
                color = TextSecondary,
                lineHeight = 22.sp
            )
            Spacer(modifier = Modifier.height(32.dp))

            OutlinedTextField(
                value = uiState.fullName,
                onValueChange = viewModel::onFullNameChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Ad Soyad") },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Primary,
                    unfocusedBorderColor = BorderColor,
                    focusedContainerColor = SurfaceColor,
                    unfocusedContainerColor = SurfaceColor
                )
            )
            Spacer(modifier = Modifier.height(14.dp))

            OutlinedTextField(
                value = uiState.email,
                onValueChange = viewModel::onEmailChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("E-posta") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Primary,
                    unfocusedBorderColor = BorderColor,
                    focusedContainerColor = SurfaceColor,
                    unfocusedContainerColor = SurfaceColor
                )
            )
            Spacer(modifier = Modifier.height(14.dp))

            OutlinedTextField(
                value = uiState.password,
                onValueChange = viewModel::onPasswordChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Parola") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Primary,
                    unfocusedBorderColor = BorderColor,
                    focusedContainerColor = SurfaceColor,
                    unfocusedContainerColor = SurfaceColor
                )
            )
            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "Telefon numarası",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextSecondary
            )
            Spacer(modifier = Modifier.height(9.dp))
            PhoneNumberInput(
                value = uiState.phone,
                onValueChange = viewModel::onPhoneChange
            )
            Spacer(modifier = Modifier.height(14.dp))

            OutlinedTextField(
                value = uiState.referralCode,
                onValueChange = viewModel::onReferralCodeChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Davet kodu (opsiyonel)") },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Primary,
                    unfocusedBorderColor = BorderColor,
                    focusedContainerColor = SurfaceColor,
                    unfocusedContainerColor = SurfaceColor
                )
            )

            if (uiState.error != null) {
                Text(
                    text = uiState.error ?: "",
                    color = Danger,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(top = 12.dp)
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            PrimaryAuthButton(
                text = "Kayıt Ol",
                icon = Icons.Outlined.PersonAddAlt,
                isLoading = uiState.isLoading,
                enabled = uiState.fullName.isNotBlank() &&
                    uiState.email.isNotBlank() &&
                    uiState.password.length >= AuthConstants.MIN_PASSWORD_LENGTH &&
                    uiState.phone.length == AuthConstants.PHONE_DIGIT_COUNT &&
                    !uiState.isLoading,
                onClick = viewModel::onRegister
            )

            AuthFooterText(
                mainText = "Zaten hesabın var mı? ",
                actionText = "Giriş yap",
                onClick = onNavigateToLogin
            )
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
