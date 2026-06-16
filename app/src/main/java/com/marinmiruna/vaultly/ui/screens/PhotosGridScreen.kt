package com.marinmiruna.vaultly.ui.screens

import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.marinmiruna.vaultly.R
import com.marinmiruna.vaultly.domain.model.PhotoItem
import com.marinmiruna.vaultly.ui.components.ConfirmDeleteDialog
import com.marinmiruna.vaultly.ui.components.EncryptedImageLoader
import com.marinmiruna.vaultly.ui.components.HeaderButton
import com.marinmiruna.vaultly.ui.components.ScreenHeader
import com.marinmiruna.vaultly.viewmodel.PhotosViewModel

@Composable
fun PhotosGridScreen(
    onOpenPhoto: (Long) -> Unit,
    onBack: () -> Unit,
    onTrustedSystemActivityStarted: () -> Unit,
    viewModel: PhotosViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var selectedPhotoIds by remember { mutableStateOf<Set<Long>>(emptySet()) }
    val isSelectionMode = selectedPhotoIds.isNotEmpty()

    fun togglePhotoSelection(photoId: Long) {
        selectedPhotoIds = if (photoId in selectedPhotoIds) {
            selectedPhotoIds - photoId
        } else {
            selectedPhotoIds + photoId
        }
    }

    fun clearSelection() {
        selectedPhotoIds = emptySet()
    }

    var showImportExitDialog by remember { mutableStateOf(false) }
    var showDeleteSelectedDialog by remember { mutableStateOf(false) }

    val errorMessage = uiState.errorMessage
    val successMessage = uiState.successMessage

    LaunchedEffect(Unit) {
        if (!viewModel.isPhotosSessionValid()) {
            onBack()
        }
    }

    fun handleBack() {
        when {
            isSelectionMode -> clearSelection()
            uiState.isImporting -> showImportExitDialog = true
            else -> onBack()
        }
    }

    BackHandler {
        handleBack()
    }

    LaunchedEffect(errorMessage) {
        if (errorMessage != null) {
            android.widget.Toast.makeText(
                context,
                errorMessage,
                android.widget.Toast.LENGTH_LONG
            ).show()
            viewModel.clearErrorMessage()
        }
    }

    LaunchedEffect(successMessage) {
        if (successMessage != null) {
            android.widget.Toast.makeText(
                context,
                successMessage,
                android.widget.Toast.LENGTH_SHORT
            ).show()
            viewModel.clearSuccessMessage()
        }
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        uris.forEach { uri ->
            val displayName = context.resolveDisplayName(uri)
            val mimeType = context.contentResolver.getType(uri) ?: "image/*"

            viewModel.importPhoto(
                uri = uri,
                displayName = displayName,
                mimeType = mimeType
            )
        }
    }

    if (showImportExitDialog) {
        ConfirmDeleteDialog(
            title = stringResource(R.string.import_exit_dialog_title),
            message = stringResource(R.string.import_exit_dialog_message),
            confirmText = stringResource(R.string.import_exit_dialog_confirm),
            onConfirm = {
                showImportExitDialog = false
                onBack()
            },
            onDismiss = {
                showImportExitDialog = false
            }
        )
    }

    if (showDeleteSelectedDialog) {
        val selectedPhotos = uiState.photos.filter { photo ->
            photo.id in selectedPhotoIds
        }
        ConfirmDeleteDialog(
            title = stringResource(R.string.photos_delete_selected_dialog_title),
            message = stringResource(R.string.photos_delete_selected_dialog_message),
            onConfirm = {
                showDeleteSelectedDialog = false
                viewModel.deletePhotos(
                    photos = selectedPhotos,
                    onDeleted = {
                        clearSelection()
                    }
                )
            },
            onDismiss = {
                showDeleteSelectedDialog = false
            }
        )
    }

    Scaffold(
        modifier = Modifier.safeDrawingPadding(),
        floatingActionButton = {
            if (!isSelectionMode) {
                FloatingActionButton(
                    onClick = {
                        if (!uiState.isImporting) {
                            onTrustedSystemActivityStarted()
                            photoPickerLauncher.launch("image/*")
                        }
                    },
                    shape = RoundedCornerShape(8.dp),
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    if (uiState.isImporting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Text(
                            text = "+",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp, vertical = 24.dp)
            ) {
                ScreenHeader(
                    title = if (isSelectionMode) {
                        stringResource(R.string.selection_count_format, selectedPhotoIds.size)
                    } else {
                        stringResource(R.string.photos_title)
                    },
                    titleStyle = if (isSelectionMode) {
                        MaterialTheme.typography.titleLarge
                    } else {
                        MaterialTheme.typography.headlineMedium
                    },
                    onBack = { handleBack() },
                    actions = {
                        if (isSelectionMode) {
                            HeaderButton(
                                text = stringResource(R.string.common_delete),
                                onClick = {
                                    showDeleteSelectedDialog = true
                                },
                                containerColor = MaterialTheme.colorScheme.error,
                                contentColor = MaterialTheme.colorScheme.onError
                            )
                        }
                        HeaderButton(
                            text = stringResource(R.string.common_back),
                            onClick = { handleBack() }
                        )
                    }
                )
                if (uiState.isImporting) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = stringResource(R.string.photos_importing),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                if (uiState.photos.isEmpty()) {
                    EmptyPhotosState(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 40.dp)
                    )
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = 32.dp),
                        contentPadding = PaddingValues(bottom = 96.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(
                            items = uiState.photos,
                            key = { photo -> photo.id }
                        ) { photo ->
                            PhotoTile(
                                photo = photo,
                                decryptImage = {
                                    viewModel.decryptPhotoThumbnail(photo)
                                },
                                isSelected = photo.id in selectedPhotoIds,
                                onClick = {
                                    if (isSelectionMode) {
                                        togglePhotoSelection(photo.id)
                                    } else {
                                        onOpenPhoto(photo.id)
                                    }
                                },
                                onLongClick = {
                                    togglePhotoSelection(photo.id)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PhotoTile(
    photo: PhotoItem,
    decryptImage: suspend () -> ByteArray,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = if (isSelected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.outlineVariant
            }
        ),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            EncryptedImageLoader(
                encryptedFilePath = photo.thumbnailEncryptedFilePath,
                contentDescription = photo.displayName,
                decryptImage = {
                    decryptImage()
                },
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                targetSizePx = 320
            )
            if (isSelected) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp),
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Text(
                        text = "✓",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyPhotosState(
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant
        ),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = stringResource(R.string.photos_empty_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = stringResource(R.string.photos_empty_message),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun android.content.Context.resolveDisplayName(uri: Uri): String {
    val fallbackName = getString(R.string.photos_fallback_name_prefix) +
            "_${System.currentTimeMillis()}" +
            getString(R.string.photos_fallback_name_extension)

    val cursor = contentResolver.query(
        uri,
        arrayOf(OpenableColumns.DISPLAY_NAME),
        null,
        null,
        null
    ) ?: return fallbackName

    cursor.use {
        if (!it.moveToFirst()) {
            return fallbackName
        }

        val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        if (nameIndex < 0) {
            return fallbackName
        }

        return it.getString(nameIndex) ?: fallbackName
    }
}