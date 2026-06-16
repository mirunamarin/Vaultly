package com.marinmiruna.vaultly.viewmodel

import android.app.Application
import com.marinmiruna.vaultly.R
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.marinmiruna.vaultly.data.repository.PhotoRepository
import com.marinmiruna.vaultly.domain.model.PhotoItem
import com.marinmiruna.vaultly.session.SensitiveSessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PhotosViewModel @Inject constructor(
    private val photoRepository: PhotoRepository,
    private val sensitiveSessionManager: SensitiveSessionManager,
    private val application: Application
) : ViewModel() {

    private val errorMessage = MutableStateFlow<String?>(null)
    private val successMessage = MutableStateFlow<String?>(null)

    private val isImporting = MutableStateFlow(false)

    private val _authState = MutableStateFlow(
        PhotosAuthState(isAuthenticated = sensitiveSessionManager.isPhotosSessionValid())
    )
    val authState: StateFlow<PhotosAuthState> = _authState

    val uiState: StateFlow<PhotosUiState> = photoRepository.observePhotos()
        .catch { exception ->
            errorMessage.value = UserMessageMapper.photoOperationMessage(
                context = application,
                exception = exception
            )
            emit(emptyList())
        }
        .combine(errorMessage) { photos, error ->
            photos to error
        }
        .combine(successMessage) { (photos, error), success ->
            Triple(photos, error, success)
        }
        .combine(isImporting) { (photos, error, success), importing ->
            PhotosUiState(
                photos = photos,
                errorMessage = error,
                successMessage = success,
                isImporting = importing
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = PhotosUiState()
        )

    private val _viewerState = MutableStateFlow(PhotoViewerUiState())
    val viewerState: StateFlow<PhotoViewerUiState> = _viewerState

    fun markAuthenticated() {
        sensitiveSessionManager.markPhotosAuthenticated()
        _authState.value = PhotosAuthState(isAuthenticated = true)
    }

    fun refreshAuthState() {
        _authState.value = PhotosAuthState(
            isAuthenticated = sensitiveSessionManager.isPhotosSessionValid()
        )
    }

    fun isPhotosSessionValid(): Boolean {
        return sensitiveSessionManager.isPhotosSessionValid()
    }

    fun importPhoto(
        uri: Uri,
        displayName: String,
        mimeType: String
    ) {
        viewModelScope.launch {
            isImporting.value = true

            runCatching {
                photoRepository.importPhoto(
                    sourceUri = uri,
                    displayName = displayName,
                    mimeType = mimeType
                )
            }.onSuccess {
                successMessage.value = application.getString(R.string.photos_import_success)
            }.onFailure { exception ->
                errorMessage.value = UserMessageMapper.photoOperationMessage(
                    context = application,
                    exception = exception
                )
            }

            isImporting.value = false
        }
    }

    fun loadPhoto(photoId: Long) {
        viewModelScope.launch {
            photoRepository.observePhoto(photoId)
                .catch { exception ->
                    _viewerState.value = _viewerState.value.copy(
                        errorMessage = UserMessageMapper.photoOperationMessage(
                            context = application,
                            exception = exception
                        )
                    )
                }
                .collect { photo ->
                    if (photo == null) {
                        _viewerState.value = PhotoViewerUiState(
                            errorMessage = application.getString(R.string.photo_not_found)
                        )
                    } else {
                        _viewerState.value = PhotoViewerUiState(photo = photo)
                    }
                }
        }
    }

    suspend fun decryptPhoto(photo: PhotoItem): ByteArray {
        return runCatching {
            photoRepository.decryptPhoto(photo)
        }.onFailure { exception ->
            errorMessage.value = UserMessageMapper.photoOperationMessage(
                context = application,
                exception = exception
            )
        }.getOrDefault(ByteArray(0))
    }

    suspend fun decryptPhotoThumbnail(photo: PhotoItem): ByteArray {
        return runCatching {
            photoRepository.decryptPhotoThumbnail(photo)
        }.onFailure { exception ->
            errorMessage.value = UserMessageMapper.photoOperationMessage(
                context = application,
                exception = exception
            )
        }.getOrDefault(ByteArray(0))
    }

    suspend fun decryptPhotoPreview(photo: PhotoItem): ByteArray {
        return runCatching {
            photoRepository.decryptPhotoPreview(photo)
        }.onFailure { exception ->
            errorMessage.value = UserMessageMapper.photoOperationMessage(
                context = application,
                exception = exception
            )
        }.getOrDefault(ByteArray(0))
    }

    fun deleteCurrentPhoto(onDeleted: () -> Unit) {
        val photo = _viewerState.value.photo ?: return

        viewModelScope.launch {
            runCatching {
                photoRepository.deletePhoto(photo)
            }.onSuccess {
                onDeleted()
            }.onFailure { exception ->
                _viewerState.value = _viewerState.value.copy(
                    errorMessage = UserMessageMapper.photoOperationMessage(
                        context = application,
                        exception = exception
                    )
                )
            }
        }
    }

    fun deletePhotos(
        photos: List<PhotoItem>,
        onDeleted: () -> Unit
    ) {
        if (photos.isEmpty()) {
            return
        }

        viewModelScope.launch {
            runCatching {
                photos.forEach { photo ->
                    photoRepository.deletePhoto(photo)
                }
            }.onSuccess {
                onDeleted()
            }.onFailure { exception ->
                errorMessage.value = UserMessageMapper.photoOperationMessage(
                    context = application,
                    exception = exception
                )
            }
        }
    }

    fun clearErrorMessage() {
        errorMessage.value = null
    }

    fun clearSuccessMessage() {
        successMessage.value = null
    }
}

data class PhotosAuthState(
    val isAuthenticated: Boolean = false
)

data class PhotosUiState(
    val photos: List<PhotoItem> = emptyList(),
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val isImporting: Boolean = false
)

data class PhotoViewerUiState(
    val photo: PhotoItem? = null,
    val errorMessage: String? = null
)