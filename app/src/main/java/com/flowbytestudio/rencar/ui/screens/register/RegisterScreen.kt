package com.flowbytestudio.rencar.ui.screens.register

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.PersonAddAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.flowbytestudio.rencar.R
import com.flowbytestudio.rencar.data.auth.AuthConstants
import com.flowbytestudio.rencar.ui.screens.login.AuthFooterText
import com.flowbytestudio.rencar.ui.screens.login.PhoneNumberInput
import com.flowbytestudio.rencar.ui.screens.login.PrimaryAuthButton
import com.flowbytestudio.rencar.ui.theme.*

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
                .padding(horizontal = Dimens.SpaceXl)
        ) {
            Spacer(modifier = Modifier.height(Dimens.SpaceM))
            IconButton(
                onClick = onNavigateToLogin,
                modifier = Modifier
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
            Spacer(modifier = Modifier.height(Dimens.SpaceXl))

            Text(
                text = stringResource(R.string.register_title),
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(Dimens.SpaceXs))
            Text(
                text = stringResource(R.string.register_subtitle),
                fontSize = 15.sp,
                color = TextSecondary,
                lineHeight = 22.sp
            )
            Spacer(modifier = Modifier.height(Dimens.SpaceXxl))

            OutlinedTextField(
                value = uiState.fullName,
                onValueChange = viewModel::onFullNameChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.register_full_name_label)) },
                singleLine = true,
                shape = RoundedCornerShape(Dimens.CornerCard),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Primary,
                    unfocusedBorderColor = BorderColor,
                    focusedContainerColor = Surface,
                    unfocusedContainerColor = Surface
                )
            )
            Spacer(modifier = Modifier.height(14.dp))

            OutlinedTextField(
                value = uiState.email,
                onValueChange = viewModel::onEmailChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.register_email_label)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                shape = RoundedCornerShape(Dimens.CornerCard),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Primary,
                    unfocusedBorderColor = BorderColor,
                    focusedContainerColor = Surface,
                    unfocusedContainerColor = Surface
                )
            )
            Spacer(modifier = Modifier.height(14.dp))

            OutlinedTextField(
                value = uiState.password,
                onValueChange = viewModel::onPasswordChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.register_password_label)) },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                shape = RoundedCornerShape(Dimens.CornerCard),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Primary,
                    unfocusedBorderColor = BorderColor,
                    focusedContainerColor = Surface,
                    unfocusedContainerColor = Surface
                )
            )
            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = stringResource(R.string.register_phone_label),
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
                label = { Text(stringResource(R.string.register_referral_code_label)) },
                singleLine = true,
                shape = RoundedCornerShape(Dimens.CornerCard),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Primary,
                    unfocusedBorderColor = BorderColor,
                    focusedContainerColor = Surface,
                    unfocusedContainerColor = Surface
                )
            )

            val errorText = uiState.errorText ?: uiState.error?.let { stringResource(it) }
            if (errorText != null) {
                Text(
                    text = errorText,
                    color = Danger,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(top = Dimens.SpaceS)
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            PrimaryAuthButton(
                text = stringResource(R.string.register_submit_button),
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
                mainText = stringResource(R.string.register_footer_have_account),
                actionText = stringResource(R.string.common_login_action),
                onClick = onNavigateToLogin
            )
            Spacer(modifier = Modifier.height(Dimens.SpaceXxl))
        }
    }
}
