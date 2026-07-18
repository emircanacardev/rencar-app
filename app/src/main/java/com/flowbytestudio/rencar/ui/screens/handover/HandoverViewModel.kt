package com.flowbytestudio.rencar.ui.screens.handover

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flowbytestudio.rencar.R
import com.flowbytestudio.rencar.data.rentals.RentalPhotosState
import com.flowbytestudio.rencar.data.rentals.RentalRepository
import com.flowbytestudio.rencar.data.rentals.RentalStatus
import com.flowbytestudio.rencar.data.rentals.rentalStatus
import com.flowbytestudio.rencar.ui.common.ImageFiles
import com.flowbytestudio.rencar.ui.common.toErrorRes
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.HttpException

class HandoverViewModel(
    private val rentalId: String,
    private val rentalRepository: RentalRepository = RentalRepository(),
) : ViewModel() {

    private val _uiState = MutableStateFlow(HandoverUiState())
    val uiState: StateFlow<HandoverUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, loadError = null) }
            val rental = rentalRepository.getRental(rentalId).getOrElse {
                _uiState.update { s ->
                    s.copy(
                        isLoading = false,
                        loadError = R.string.handover_load_error,
                    )
                }
                return@launch
            }
            // Uygulama başlamış bir yolculukla yeniden açıldıysa doğrudan aktif ekrana yönlendir.
            if (rental.rentalStatus == RentalStatus.ACTIVE) {
                _uiState.update {
                    it.copy(isLoading = false, vehicle = rental.vehicle, startedRentalId = rentalId)
                }
                return@launch
            }
            // Yarım kalan foto akışını sunucudan devral (uygulama yeniden açılmış olabilir).
            val photos = rentalRepository.getPhotos(rentalId).getOrNull()
            _uiState.update {
                it.copy(isLoading = false, vehicle = rental.vehicle).withPhotos(photos)
            }
        }
    }

    // Ekran ham kareyi verir; küçültme (bitmap decode + döndürme + JPEG) ANR'a yol
    // açmamak için ana thread yerine Dispatchers.IO'da yapılır, sonra yüklenir.
    fun onPhotoCaptured(side: PhotoSide, rawFile: File) {
        viewModelScope.launch {
            _uiState.update { it.copy(uploadingSides = it.uploadingSides + side, uploadError = null) }
            val file = withContext(Dispatchers.IO) { ImageFiles.compressForUpload(rawFile) }
            rentalRepository.uploadPhoto(rentalId, side.apiSide, file)
                .onSuccess { photos ->
                    _uiState.update {
                        it.copy(uploadingSides = it.uploadingSides - side).withPhotos(photos)
                    }
                }
                .onFailure { throwable ->
                    _uiState.update {
                        it.copy(
                            uploadingSides = it.uploadingSides - side,
                            uploadError = throwable.toUploadErrorMessage(),
                        )
                    }
                }
        }
    }

    fun startRental() {
        if (!_uiState.value.canStart) return
        viewModelScope.launch {
            _uiState.update { it.copy(isStarting = true, startError = null, startErrorArg = null) }
            rentalRepository.startRental(rentalId)
                .onSuccess {
                    _uiState.update { it.copy(isStarting = false, startedRentalId = rentalId) }
                }
                .onFailure { throwable -> handleStartFailure(throwable) }
        }
    }

    // 409: fotoğraflar eksik ("N foto kaldı") ya da yolculuk zaten başladı/bitti.
    // Kalan sayıyı sunucudan güncel foto durumunu çekerek gösteriyoruz.
    private suspend fun handleStartFailure(throwable: Throwable) {
        if (throwable is HttpException && throwable.code() == 409) {
            val photos = rentalRepository.getPhotos(rentalId).getOrNull()
            _uiState.update {
                if (photos != null && !photos.photosComplete) {
                    it.withPhotos(photos).copy(
                        isStarting = false,
                        startError = R.string.handover_photos_remaining_error,
                        startErrorArg = photos.remainingSides.size,
                    )
                } else {
                    it.withPhotos(photos).copy(
                        isStarting = false,
                        startError = R.string.handover_start_conflict_error,
                        startErrorArg = null,
                    )
                }
            }
            return
        }
        _uiState.update {
            it.copy(isStarting = false, startError = throwable.toStartErrorMessage(), startErrorArg = null)
        }
    }

    fun onCancelClicked() {
        _uiState.update { it.copy(showCancelDialog = true, cancelError = null) }
    }

    fun onDismissCancelDialog() {
        if (_uiState.value.isCancelling) return
        _uiState.update { it.copy(showCancelDialog = false) }
    }

    fun confirmCancel() {
        if (_uiState.value.isCancelling) return
        viewModelScope.launch {
            _uiState.update { it.copy(isCancelling = true, cancelError = null) }
            rentalRepository.cancelRental(rentalId)
                .onSuccess {
                    _uiState.update {
                        it.copy(isCancelling = false, showCancelDialog = false, cancelled = true)
                    }
                }
                .onFailure { throwable ->
                    _uiState.update {
                        it.copy(
                            isCancelling = false,
                            showCancelDialog = false,
                            cancelError = throwable.toCancelErrorMessage(),
                        )
                    }
                }
        }
    }
}

// Sunucu foto durumunu UI state'e uygular; null gelirse mevcut durumu korur.
private fun HandoverUiState.withPhotos(state: RentalPhotosState?): HandoverUiState {
    if (state == null) return this
    val photoMap = state.photos.mapNotNull { photo ->
        PhotoSide.fromApi(photo.side)?.let { side -> side to photo.imageUrl }
    }.toMap()
    return copy(
        photos = photoMap,
        uploadedCount = state.uploadedCount,
        photosComplete = state.photosComplete,
    )
}

private fun Throwable.toUploadErrorMessage(): Int = toErrorRes(
    fallback = R.string.handover_upload_error_generic,
    overrides = mapOf(
        413 to R.string.handover_upload_error_file_too_large,
        400 to R.string.handover_upload_error_invalid_file,
        409 to R.string.handover_upload_error_not_preparing,
        404 to R.string.handover_error_rental_not_found,
        401 to R.string.common_error_session_expired,
    ),
)

private fun Throwable.toStartErrorMessage(): Int = toErrorRes(
    fallback = R.string.handover_start_error_generic,
    overrides = mapOf(
        404 to R.string.handover_error_rental_not_found,
        401 to R.string.common_error_session_expired,
    ),
)

private fun Throwable.toCancelErrorMessage(): Int = toErrorRes(
    fallback = R.string.handover_cancel_error_generic,
    overrides = mapOf(
        409 to R.string.handover_cancel_error_not_preparing,
        404 to R.string.handover_error_rental_not_found,
        401 to R.string.common_error_session_expired,
    ),
)
