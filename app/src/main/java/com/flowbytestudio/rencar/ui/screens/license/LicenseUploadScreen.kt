package com.flowbytestudio.rencar.ui.screens.license

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.flowbytestudio.rencar.ui.theme.Background
import com.flowbytestudio.rencar.ui.theme.BgLight
import com.flowbytestudio.rencar.ui.theme.BorderColor
import com.flowbytestudio.rencar.ui.theme.Danger
import com.flowbytestudio.rencar.ui.theme.Primary
import com.flowbytestudio.rencar.ui.theme.Success
import com.flowbytestudio.rencar.ui.theme.SuccessLight
import com.flowbytestudio.rencar.ui.theme.Surface as SurfaceColor
import com.flowbytestudio.rencar.ui.theme.TextPrimary
import com.flowbytestudio.rencar.ui.theme.TextSecondary
import java.io.File

@Composable
fun LicenseUploadScreen(
    canGoBack: Boolean,
    onFinished: () -> Unit,
    onSkip: () -> Unit,
    viewModel: LicenseUploadViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(uiState.isSubmitted) {
        if (uiState.isSubmitted) onFinished()
    }

    val isFirstStep = uiState.step == LicenseUploadStep.LICENSE
    val onBack: () -> Unit = {
        if (isFirstStep) onSkip() else viewModel.onBackToPreviousStep()
    }
    val showBackButton = canGoBack || !isFirstStep

    BackHandler(enabled = showBackButton) { onBack() }

    Surface(modifier = Modifier.fillMaxSize(), color = Background) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 22.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (showBackButton) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(13.dp))
                            .background(SurfaceColor)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Geri",
                            tint = TextPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                }
                Column {
                    Text(
                        text = "Ehliyet doğrulama",
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "Kiralamadan önce tek seferlik",
                        fontSize = 12.5.sp,
                        color = TextSecondary
                    )
                }
            }

            StepProgressBar(step = uiState.step, modifier = Modifier.padding(horizontal = 22.dp, vertical = 10.dp))

            when (uiState.step) {
                LicenseUploadStep.LICENSE -> LicenseStepContent(
                    uiState = uiState,
                    context = context,
                    onFrontPicked = viewModel::onFrontSelected,
                    onBackPicked = viewModel::onBackSelected,
                    onNext = viewModel::onNextFromLicenseStep,
                    onSkip = onSkip,
                )
                LicenseUploadStep.SELFIE -> SelfieStepContent(
                    uiState = uiState,
                    context = context,
                    onSelfiePicked = viewModel::onSelfieSelected,
                    onNext = viewModel::onNextFromSelfieStep,
                    onSkip = onSkip,
                )
                LicenseUploadStep.CONFIRM -> ConfirmStepContent(
                    uiState = uiState,
                    onSubmit = { viewModel.onSubmit(context) },
                    onSkip = onSkip,
                )
            }
        }
    }
}

@Composable
private fun StepProgressBar(step: LicenseUploadStep, modifier: Modifier = Modifier) {
    Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        StepIndicator(index = 1, label = "Ehliyet", isActive = step.ordinal >= 0, isCurrent = step == LicenseUploadStep.LICENSE)
        StepConnector()
        StepIndicator(index = 2, label = "Selfie", isActive = step.ordinal >= 1, isCurrent = step == LicenseUploadStep.SELFIE)
        StepConnector()
        StepIndicator(index = 3, label = "Onay", isActive = step.ordinal >= 2, isCurrent = step == LicenseUploadStep.CONFIRM)
    }
}

@Composable
private fun RowScope.StepIndicator(index: Int, label: String, isActive: Boolean, isCurrent: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(if (isActive) Primary else BgLight),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = index.toString(),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = if (isActive) Color.White else TextSecondary,
            )
        }
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = label,
            fontSize = 11.5.sp,
            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.SemiBold,
            color = if (isActive) TextPrimary else TextSecondary,
        )
    }
}

@Composable
private fun RowScope.StepConnector() {
    Spacer(
        modifier = Modifier
            .weight(1f)
            .height(2.dp)
            .background(BorderColor)
    )
}

@Composable
private fun ColumnScope.LicenseStepContent(
    uiState: LicenseUploadUiState,
    context: Context,
    onFrontPicked: (Uri) -> Unit,
    onBackPicked: (Uri) -> Unit,
    onNext: () -> Unit,
    onSkip: () -> Unit,
) {
    Column(
        modifier = Modifier
            .weight(1f)
            .padding(horizontal = 18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        PhotoPickerCard(
            label = "Ehliyet ön yüz",
            uri = uiState.frontUri,
            imageUrl = null,
            context = context,
            onImagePicked = onFrontPicked,
        )
        PhotoPickerCard(
            label = "Ehliyet arka yüz",
            uri = uiState.backUri,
            imageUrl = null,
            context = context,
            onImagePicked = onBackPicked,
        )

        InfoBanner()

        if (uiState.error != null) {
            Text(text = uiState.error, color = Danger, fontSize = 13.sp, modifier = Modifier.padding(top = 4.dp))
        }
    }

    BottomActionArea(
        primaryText = "Devam Et",
        primaryEnabled = uiState.frontUri != null && uiState.backUri != null,
        onPrimaryClick = onNext,
        secondaryText = "Daha sonra tamamla",
        onSecondaryClick = onSkip,
    )
}

@Composable
private fun ColumnScope.SelfieStepContent(
    uiState: LicenseUploadUiState,
    context: Context,
    onSelfiePicked: (Uri) -> Unit,
    onNext: () -> Unit,
    onSkip: () -> Unit,
) {
    Column(
        modifier = Modifier
            .weight(1f)
            .padding(horizontal = 18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = "Selfie",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary,
        )
        SelfiePickerAvatar(
            uri = uiState.selfieUri,
            imageUrl = uiState.selfieUrl,
            context = context,
            onImagePicked = onSelfiePicked,
            modifier = Modifier.fillMaxWidth(),
        )

        if (uiState.selfieUrl != null && uiState.selfieUri == null) {
            Text(
                text = "Profil fotoğrafın otomatik olarak kullanıldı. Değiştirmek için dokun.",
                fontSize = 12.5.sp,
                color = TextSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        InfoBanner()

        if (uiState.error != null) {
            Text(text = uiState.error, color = Danger, fontSize = 13.sp, modifier = Modifier.padding(top = 4.dp))
        }
    }

    BottomActionArea(
        primaryText = "Devam Et",
        primaryEnabled = uiState.hasSelfie,
        onPrimaryClick = onNext,
        secondaryText = "Daha sonra tamamla",
        onSecondaryClick = onSkip,
    )
}

@Composable
private fun ColumnScope.ConfirmStepContent(
    uiState: LicenseUploadUiState,
    onSubmit: () -> Unit,
    onSkip: () -> Unit,
) {
    Column(
        modifier = Modifier
            .weight(1f)
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(SuccessLight),
            contentAlignment = Alignment.Center,
        ) {
            Icon(imageVector = Icons.Filled.Check, contentDescription = null, tint = Success, modifier = Modifier.size(34.dp))
        }
        Spacer(modifier = Modifier.height(20.dp))
        Text(
            text = "Göndermeye hazırsın",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Ehliyet ön/arka yüz ve selfie fotoğrafların incelemeye gönderilecek.",
            fontSize = 14.sp,
            color = TextSecondary,
            lineHeight = 20.sp,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(20.dp))
        InfoBanner()

        if (uiState.error != null) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(text = uiState.error, color = Danger, fontSize = 13.sp, textAlign = TextAlign.Center)
        }
    }

    BottomActionArea(
        primaryText = "Ehliyeti Gönder",
        primaryEnabled = !uiState.isSubmitting,
        isLoading = uiState.isSubmitting,
        onPrimaryClick = onSubmit,
        secondaryText = "Daha sonra tamamla",
        onSecondaryClick = onSkip,
    )
}

@Composable
private fun InfoBanner() {
    Row(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(BgLight)
            .padding(horizontal = 13.dp, vertical = 12.dp),
    ) {
        Icon(
            imageVector = Icons.Outlined.Info,
            contentDescription = null,
            tint = Primary,
            modifier = Modifier.size(18.dp),
        )
        Text(
            text = buildString {
                append("Bilgilerin güvenle saklanır. Doğrulama genelde birkaç dakika sürer.")
            },
            fontSize = 12.sp,
            color = TextSecondary,
            lineHeight = 18.sp,
        )
    }
}

@Composable
private fun ColumnScope.BottomActionArea(
    primaryText: String,
    primaryEnabled: Boolean,
    onPrimaryClick: () -> Unit,
    isLoading: Boolean = false,
    secondaryText: String? = null,
    onSecondaryClick: (() -> Unit)? = null,
) {
    Column(
        modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Button(
            onClick = onPrimaryClick,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            enabled = primaryEnabled,
            shape = RoundedCornerShape(18.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Primary),
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White, strokeWidth = 2.dp)
            } else {
                Text(text = primaryText, fontSize = 16.5.sp, fontWeight = FontWeight.Bold)
            }
        }

        if (secondaryText != null && onSecondaryClick != null) {
            Text(
                text = secondaryText,
                fontSize = 14.5.sp,
                color = TextSecondary,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onSecondaryClick)
                    .padding(vertical = 6.dp),
                textAlign = TextAlign.Center,
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
    }
}

private class ImagePickerState(
    val showPicker: MutableState<Boolean>,
    val onCameraClick: () -> Unit,
    val onGalleryClick: () -> Unit,
)

@Composable
private fun rememberImagePickerState(
    context: Context,
    onImagePicked: (Uri) -> Unit,
): ImagePickerState {
    val showPicker = remember { mutableStateOf(false) }
    var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
    ) { pickedUri -> pickedUri?.let(onImagePicked) }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture(),
    ) { success -> if (success) pendingCameraUri?.let(onImagePicked) }

    fun launchCamera() {
        val photoFile = File.createTempFile("license_camera", ".jpg", context.cacheDir)
        val photoUri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", photoFile)
        pendingCameraUri = photoUri
        cameraLauncher.launch(photoUri)
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted -> if (granted) launchCamera() }

    return ImagePickerState(
        showPicker = showPicker,
        onCameraClick = {
            showPicker.value = false
            val hasCameraPermission = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA,
            ) == PackageManager.PERMISSION_GRANTED
            if (hasCameraPermission) {
                launchCamera()
            } else {
                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        },
        onGalleryClick = {
            showPicker.value = false
            galleryLauncher.launch("image/*")
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ImagePickerBottomSheet(state: ImagePickerState) {
    if (state.showPicker.value) {
        ModalBottomSheet(onDismissRequest = { state.showPicker.value = false }) {
            Column(modifier = Modifier.padding(bottom = 24.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = state.onCameraClick)
                        .padding(horizontal = 24.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(imageVector = Icons.Outlined.CameraAlt, contentDescription = null, tint = Primary)
                    Spacer(modifier = Modifier.width(14.dp))
                    Text(text = "Kamera ile çek", fontSize = 16.sp, color = TextPrimary)
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = state.onGalleryClick)
                        .padding(horizontal = 24.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(imageVector = Icons.Outlined.PhotoLibrary, contentDescription = null, tint = Primary)
                    Spacer(modifier = Modifier.width(14.dp))
                    Text(text = "Galeriden seç", fontSize = 16.sp, color = TextPrimary)
                }
            }
        }
    }
}

@Composable
private fun PhotoPickerCard(
    label: String,
    uri: Uri?,
    imageUrl: String?,
    context: Context,
    onImagePicked: (Uri) -> Unit,
    heightDp: Int = 130,
) {
    val pickerState = rememberImagePickerState(context = context, onImagePicked = onImagePicked)
    val displayUri = uri
    val hasImage = displayUri != null || imageUrl != null

    Column {
        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary,
            modifier = Modifier.padding(bottom = 8.dp),
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(heightDp.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(SurfaceColor)
                .border(
                    width = if (!hasImage) 2.dp else 0.dp,
                    color = BorderColor,
                    shape = RoundedCornerShape(18.dp),
                )
                .clickable { pickerState.showPicker.value = true },
            contentAlignment = Alignment.Center,
        ) {
            if (hasImage) {
                AsyncImage(
                    model = displayUri ?: imageUrl,
                    contentDescription = label,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(18.dp)),
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .clip(RoundedCornerShape(7.dp))
                        .background(Success)
                        .padding(horizontal = 8.dp, vertical = 3.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Outlined.CheckCircle,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(12.dp),
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "Yüklendi", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Outlined.PhotoLibrary,
                        contentDescription = null,
                        tint = Primary,
                        modifier = Modifier.size(28.dp),
                    )
                    Spacer(modifier = Modifier.height(7.dp))
                    Text(text = "Çek veya yükle", fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold, color = TextSecondary)
                }
            }
        }
    }

    ImagePickerBottomSheet(state = pickerState)
}

@Composable
private fun SelfiePickerAvatar(
    uri: Uri?,
    imageUrl: String?,
    context: Context,
    onImagePicked: (Uri) -> Unit,
    modifier: Modifier = Modifier,
) {
    val pickerState = rememberImagePickerState(context = context, onImagePicked = onImagePicked)
    val displayUri = uri
    val hasImage = displayUri != null || imageUrl != null

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .size(148.dp)
                .clip(CircleShape)
                .background(SurfaceColor)
                .border(
                    width = if (!hasImage) 2.dp else 0.dp,
                    color = BorderColor,
                    shape = CircleShape,
                )
                .clickable { pickerState.showPicker.value = true },
            contentAlignment = Alignment.Center,
        ) {
            if (hasImage) {
                AsyncImage(
                    model = displayUri ?: imageUrl,
                    contentDescription = "Selfie",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize().clip(CircleShape),
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .clip(CircleShape)
                        .background(Success)
                        .size(32.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.CheckCircle,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp),
                    )
                }
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Outlined.CameraAlt,
                        contentDescription = null,
                        tint = Primary,
                        modifier = Modifier.size(30.dp),
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(text = "Çek veya yükle", fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold, color = TextSecondary)
                }
            }
        }
    }

    ImagePickerBottomSheet(state = pickerState)
}
