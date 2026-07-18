package com.flowbytestudio.rencar.ui.screens.handover

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import coil.compose.AsyncImage
import com.flowbytestudio.rencar.R
import com.flowbytestudio.rencar.ui.common.rememberCameraCapture
import com.flowbytestudio.rencar.ui.theme.Background
import com.flowbytestudio.rencar.ui.theme.BgLight
import com.flowbytestudio.rencar.ui.theme.BorderLight
import com.flowbytestudio.rencar.ui.theme.Danger
import com.flowbytestudio.rencar.ui.theme.Dimens
import com.flowbytestudio.rencar.ui.theme.Primary
import com.flowbytestudio.rencar.ui.theme.PrimaryLight
import com.flowbytestudio.rencar.ui.theme.Success
import com.flowbytestudio.rencar.ui.theme.SuccessLight
import com.flowbytestudio.rencar.ui.theme.Surface
import com.flowbytestudio.rencar.ui.theme.TextPrimary
import com.flowbytestudio.rencar.ui.theme.TextSecondary

private val WarningAmber = Color(0xFFF59E0B)

@Composable
fun HandoverScreen(
    rentalId: String,
    onBack: () -> Unit,
    onCancelled: () -> Unit,
    onRentalStarted: (rentalId: String) -> Unit,
) {
    val viewModel: HandoverViewModel = viewModel(
        factory = viewModelFactory {
            initializer { HandoverViewModel(rentalId) }
        },
    )
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState.startedRentalId) {
        uiState.startedRentalId?.let(onRentalStarted)
    }
    LaunchedEffect(uiState.cancelled) {
        if (uiState.cancelled) onCancelled()
    }

    // Her yön için ayrı kamera başlatıcısı; ham kare ViewModel'e verilir,
    // küçültme arka planda (Dispatchers.IO) yapılır — ana thread bloklanmaz.
    val cameraFront = rememberCameraCapture { file -> viewModel.onPhotoCaptured(PhotoSide.ON, file) }
    val cameraBack = rememberCameraCapture { file -> viewModel.onPhotoCaptured(PhotoSide.ARKA, file) }
    val cameraLeft = rememberCameraCapture { file -> viewModel.onPhotoCaptured(PhotoSide.SOL, file) }
    val cameraRight = rememberCameraCapture { file -> viewModel.onPhotoCaptured(PhotoSide.SAG, file) }

    Column(modifier = Modifier.fillMaxSize().background(Background)) {
        HandoverHeader(onBack = onBack)

        when {
            uiState.isLoading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Primary)
                }
            }
            uiState.loadError != null -> {
                val loadError = uiState.loadError ?: return@Column
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
                val vehicle = uiState.vehicle ?: return@Column

                Column(modifier = Modifier.weight(1f).padding(horizontal = 20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "${vehicle.brand} ${vehicle.model} · ${vehicle.plate}",
                            fontSize = 13.sp,
                            color = TextSecondary,
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            text = stringResource(
                                R.string.handover_photos_taken_progress,
                                uiState.uploadedCount,
                                PhotoSide.entries.size,
                            ),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Primary,
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    val nextSide = PhotoSide.entries.firstOrNull { it !in uiState.photos.keys }

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        PhotoSlot(
                            side = PhotoSide.ON,
                            imageUrl = uiState.photos[PhotoSide.ON],
                            uploading = PhotoSide.ON in uiState.uploadingSides,
                            highlighted = nextSide == PhotoSide.ON,
                            onCapture = cameraFront::capture,
                            modifier = Modifier.weight(1f),
                        )
                        PhotoSlot(
                            side = PhotoSide.ARKA,
                            imageUrl = uiState.photos[PhotoSide.ARKA],
                            uploading = PhotoSide.ARKA in uiState.uploadingSides,
                            highlighted = nextSide == PhotoSide.ARKA,
                            onCapture = cameraBack::capture,
                            modifier = Modifier.weight(1f),
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        PhotoSlot(
                            side = PhotoSide.SOL,
                            imageUrl = uiState.photos[PhotoSide.SOL],
                            uploading = PhotoSide.SOL in uiState.uploadingSides,
                            highlighted = nextSide == PhotoSide.SOL,
                            onCapture = cameraLeft::capture,
                            modifier = Modifier.weight(1f),
                        )
                        PhotoSlot(
                            side = PhotoSide.SAG,
                            imageUrl = uiState.photos[PhotoSide.SAG],
                            uploading = PhotoSide.SAG in uiState.uploadingSides,
                            highlighted = nextSide == PhotoSide.SAG,
                            onCapture = cameraRight::capture,
                            modifier = Modifier.weight(1f),
                        )
                    }

                    val uploadError = uiState.uploadError
                    if (uploadError != null) {
                        Spacer(modifier = Modifier.height(Dimens.SpaceS))
                        Text(text = stringResource(uploadError), color = Danger, fontSize = 13.sp)
                    }
                }

                HandoverBottomBar(
                    isStarting = uiState.isStarting,
                    canStart = uiState.canStart,
                    startError = uiState.startError,
                    startErrorArg = uiState.startErrorArg,
                    cancelError = uiState.cancelError,
                    onStart = viewModel::startRental,
                    onCancel = viewModel::onCancelClicked,
                )
            }
        }
    }

    if (uiState.showCancelDialog) {
        AlertDialog(
            onDismissRequest = viewModel::onDismissCancelDialog,
            containerColor = Surface,
            title = {
                Text(text = stringResource(R.string.handover_cancel_dialog_title), fontWeight = FontWeight.Bold, color = TextPrimary)
            },
            text = {
                Text(
                    text = stringResource(R.string.handover_cancel_dialog_message),
                    color = TextSecondary,
                    fontSize = 14.sp,
                )
            },
            confirmButton = {
                TextButton(onClick = viewModel::confirmCancel, enabled = !uiState.isCancelling) {
                    if (uiState.isCancelling) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = Danger,
                            strokeWidth = 2.dp,
                        )
                    } else {
                        Text(text = stringResource(R.string.handover_cancel_dialog_confirm_button), color = Danger, fontWeight = FontWeight.SemiBold)
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::onDismissCancelDialog, enabled = !uiState.isCancelling) {
                    Text(text = stringResource(R.string.common_cancel), color = TextSecondary)
                }
            },
        )
    }
}

@Composable
private fun HandoverHeader(onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(shape = CircleShape, color = Surface, shadowElevation = 2.dp) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = stringResource(R.string.common_back), tint = TextPrimary)
            }
        }
        Spacer(modifier = Modifier.width(Dimens.SpaceS))
        Column {
            Text(
                text = stringResource(R.string.handover_header_title),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
            )
            Text(
                text = stringResource(R.string.handover_header_subtitle, PhotoSide.entries.size),
                fontSize = 13.sp,
                color = TextSecondary,
            )
        }
    }
}

@Composable
private fun PhotoSlot(
    side: PhotoSide,
    imageUrl: String?,
    uploading: Boolean,
    highlighted: Boolean,
    onCapture: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val captured = imageUrl != null
    val shape = RoundedCornerShape(16.dp)
    val borderColor = BorderLight

    Box(
        modifier = modifier
            .height(128.dp)
            .clip(shape)
            .background(if (captured) SuccessLight else Surface)
            .then(
                if (captured || uploading) {
                    Modifier
                } else {
                    Modifier.drawBehind {
                        drawRoundRect(
                            color = borderColor,
                            style = Stroke(
                                width = 1.5.dp.toPx(),
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 12f)),
                            ),
                            cornerRadius = CornerRadius(16.dp.toPx()),
                        )
                    }
                },
            )
            .clickable(enabled = !uploading) { onCapture() },
    ) {
        // Yüklü fotoğrafı Coil ile göster; üzerine etiket/rozet biner.
        val sideLabel = stringResource(side.label)
        if (captured) {
            AsyncImage(
                model = imageUrl,
                contentDescription = stringResource(R.string.handover_photo_image_content_description, sideLabel),
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize().clip(shape),
            )
        }

        // Sol üst köşe yön etiketi
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(10.dp)
                .clip(RoundedCornerShape(Dimens.CornerS))
                .background(if (captured) TextPrimary else BgLight)
                .padding(horizontal = 10.dp, vertical = 4.dp),
        ) {
            Text(
                text = sideLabel,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (captured) Background else TextSecondary,
            )
        }

        when {
            uploading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.25f)),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(28.dp),
                        color = Color.White,
                        strokeWidth = 2.5.dp,
                    )
                }
            }
            captured -> {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(10.dp)
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(Success),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = stringResource(R.string.handover_photo_taken_content_description, sideLabel),
                        tint = Color.White,
                        modifier = Modifier.size(14.dp),
                    )
                }
            }
            else -> {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (highlighted) Primary else PrimaryLight),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.PhotoCamera,
                            contentDescription = stringResource(R.string.handover_take_photo_content_description, sideLabel),
                            tint = if (highlighted) Color.White else Primary,
                            modifier = Modifier.size(22.dp),
                        )
                    }
                    Spacer(modifier = Modifier.height(Dimens.SpaceXs))
                    Text(text = stringResource(R.string.handover_take_photo_label), fontSize = 12.sp, color = TextSecondary)
                }
            }
        }
    }
}

@Composable
private fun HandoverBottomBar(
    isStarting: Boolean,
    canStart: Boolean,
    @androidx.annotation.StringRes startError: Int?,
    startErrorArg: Int?,
    @androidx.annotation.StringRes cancelError: Int?,
    onStart: () -> Unit,
    onCancel: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Surface,
        shadowElevation = 12.dp,
    ) {
        Column(modifier = Modifier.padding(horizontal = Dimens.SpaceL, vertical = Dimens.SpaceS)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.WarningAmber,
                    contentDescription = null,
                    tint = WarningAmber,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = stringResource(R.string.handover_damage_photo_tip),
                    fontSize = 12.sp,
                    color = TextSecondary,
                )
            }

            if (startError != null) {
                Spacer(modifier = Modifier.height(Dimens.SpaceXs))
                val message = if (startErrorArg != null) {
                    stringResource(startError, startErrorArg)
                } else {
                    stringResource(startError)
                }
                Text(text = message, color = Danger, fontSize = 13.sp)
            }
            if (cancelError != null) {
                Spacer(modifier = Modifier.height(Dimens.SpaceXs))
                Text(text = stringResource(cancelError), color = Danger, fontSize = 13.sp)
            }

            Spacer(modifier = Modifier.height(Dimens.SpaceS))

            Button(
                onClick = onStart,
                enabled = canStart,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(Dimens.CornerL),
                colors = ButtonDefaults.buttonColors(containerColor = Primary),
            ) {
                if (isStarting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        color = Color.White,
                        strokeWidth = 2.dp,
                    )
                } else {
                    Text(
                        text = stringResource(R.string.handover_start_trip_button),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            TextButton(
                onClick = onCancel,
                enabled = !isStarting,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(Dimens.CornerL),
            ) {
                Text(
                    text = stringResource(R.string.handover_cancel_trip_button),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Danger,
                )
            }
        }
    }
}
