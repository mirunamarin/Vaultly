package com.marinmiruna.vaultly.ui.screens

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.marinmiruna.vaultly.R
import com.marinmiruna.vaultly.domain.model.FileItem
import com.marinmiruna.vaultly.ui.components.ConfirmDeleteDialog
import com.marinmiruna.vaultly.ui.components.HeaderButton
import com.marinmiruna.vaultly.ui.components.ScreenHeader
import com.marinmiruna.vaultly.viewmodel.FilesViewModel
import com.marinmiruna.vaultly.ui.components.VaultlyTextField

@Composable
fun FilesListScreen(
    onBack: () -> Unit,
    onTrustedSystemActivityStarted: () -> Unit,
    viewModel: FilesViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val openChooserTitle = stringResource(R.string.files_open_chooser_title)
    val noAppAvailableMessage = stringResource(R.string.files_no_app_available)
    val errorMessage = uiState.errorMessage
    val successMessage = uiState.successMessage

    var showImportExitDialog by remember { mutableStateOf(false) }
    var selectedFileIds by remember { mutableStateOf<Set<Long>>(emptySet()) }
    var showDeleteSelectedDialog by remember { mutableStateOf(false) }

    val isSelectionMode = selectedFileIds.isNotEmpty()

    fun toggleFileSelection(fileId: Long) {
        selectedFileIds = if (fileId in selectedFileIds) {
            selectedFileIds - fileId
        } else {
            selectedFileIds + fileId
        }
    }

    fun clearSelection() {
        selectedFileIds = emptySet()
    }

    fun handleBack() {
        when {
            isSelectionMode -> clearSelection()
            uiState.isImporting -> showImportExitDialog = true
            else -> onBack()
        }
    }

    fun openFile(file: FileItem) {
        viewModel.createShareUriForFile(file) { shareUri ->
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(shareUri, file.mimeType)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            runCatching {
                onTrustedSystemActivityStarted()
                context.startActivity(
                    Intent.createChooser(intent, openChooserTitle)
                )
            }.onFailure {
                android.widget.Toast.makeText(
                    context,
                    noAppAvailableMessage,
                    android.widget.Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    LaunchedEffect(Unit) {
        if (!viewModel.isFilesSessionValid()) {
            onBack()
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

    if (showImportExitDialog) {
        ConfirmDeleteDialog(
            title = stringResource(R.string.import_exit_dialog_title),
            message = stringResource(R.string.import_exit_dialog_message),
            confirmText = stringResource(R.string.import_exit_dialog_confirm),
            onConfirm = {
                showImportExitDialog = false
            },
            onDismiss = {
                showImportExitDialog = false
            }
        )
    }

    if (showDeleteSelectedDialog) {
        val selectedFiles = uiState.files.filter { file ->
            file.id in selectedFileIds
        }
        ConfirmDeleteDialog(
            title = stringResource(R.string.files_delete_selected_dialog_title),
            message = stringResource(R.string.files_delete_selected_dialog_message),
            onConfirm = {
                showDeleteSelectedDialog = false
                viewModel.deleteFiles(
                    files = selectedFiles,
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

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris: List<Uri> ->
        uris.forEach { uri ->
            val displayName = context.resolveFileDisplayName(uri)
            val mimeType = context.contentResolver.getType(uri) ?: "application/octet-stream"

            viewModel.importFile(
                uri = uri,
                displayName = displayName,
                mimeType = mimeType
            )
        }
    }

    Scaffold(
        modifier = Modifier.safeDrawingPadding(),
        floatingActionButton = {
            if (!isSelectionMode) {
                FloatingActionButton(
                    onClick = {
                        if (!uiState.isImporting) {
                            onTrustedSystemActivityStarted()
                            filePickerLauncher.launch(
                                arrayOf(
                                    "application/pdf",
                                    "text/plain",
                                    "application/msword",
                                    "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                                    "application/vnd.ms-excel",
                                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                                    "application/vnd.ms-powerpoint",
                                    "application/vnd.openxmlformats-officedocument.presentationml.presentation"
                                )
                            )
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
                        stringResource(R.string.selection_count_format, selectedFileIds.size)
                    } else {
                        stringResource(R.string.files_title)
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
                VaultlyTextField(
                    value = uiState.searchQuery,
                    onValueChange = viewModel::onSearchQueryChange,
                    label = stringResource(R.string.files_search_label),
                    modifier = Modifier.padding(top = 20.dp)
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
                            text = stringResource(R.string.files_importing),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                if (uiState.files.isEmpty()) {
                    EmptyFilesState(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 40.dp)
                    )
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = 20.dp),
                        contentPadding = PaddingValues(bottom = 96.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(
                            items = uiState.files,
                            key = { file -> file.id }
                        ) { file ->
                            FileCard(
                                file = file,
                                previewBitmap = uiState.filePreviewBitmaps[file.id],
                                onLoadPreview = {
                                    viewModel.loadPreviewForFile(file)
                                },
                                isOpening = uiState.openingFileId == file.id,
                                isSelected = file.id in selectedFileIds,
                                onClick = {
                                    if (isSelectionMode) {
                                        toggleFileSelection(file.id)
                                    } else {
                                        openFile(file)
                                    }
                                },
                                onLongClick = {
                                    toggleFileSelection(file.id)
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
private fun FileCard(
    file: FileItem,
    previewBitmap: Bitmap?,
    onLoadPreview: () -> Unit,
    isOpening: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    LaunchedEffect(file.id, file.previewEncryptedFilePath) {
        onLoadPreview()
    }
    Card(
        modifier = Modifier
            .fillMaxWidth()
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
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surface)
            ) {
                when {
                    previewBitmap != null -> {
                        Image(
                            bitmap = previewBitmap.asImageBitmap(),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit
                        )
                    }
                    file.previewEncryptedFilePath != null -> {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .size(28.dp)
                                .align(Alignment.Center),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    else -> {
                        Text(
                            text = file.fileTypeLabel(),
                            modifier = Modifier.align(Alignment.Center),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
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
            Text(
                text = file.displayName,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            if (isOpening) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = stringResource(R.string.files_preparing_open),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyFilesState(
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
                text = stringResource(R.string.files_empty_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = stringResource(R.string.files_empty_message),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun android.content.Context.resolveFileDisplayName(uri: Uri): String {
    val fallbackName = getString(R.string.files_fallback_name_prefix) +
            "_${System.currentTimeMillis()}"

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

private fun FileItem.fileTypeLabel(): String {
    return when (mimeType) {
        "application/pdf" -> "PDF"
        "text/plain" -> "TXT"
        "application/msword",
        "application/vnd.openxmlformats-officedocument.wordprocessingml.document" -> "DOC"

        "application/vnd.ms-excel",
        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet" -> "XLS"

        "application/vnd.ms-powerpoint",
        "application/vnd.openxmlformats-officedocument.presentationml.presentation" -> "PPT"

        else -> "FILE"
    }
}