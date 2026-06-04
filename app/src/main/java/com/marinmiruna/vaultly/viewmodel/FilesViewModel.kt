package com.marinmiruna.vaultly.viewmodel

import android.app.Application
import com.marinmiruna.vaultly.R
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.marinmiruna.vaultly.data.repository.FileRepository
import com.marinmiruna.vaultly.domain.model.FileItem
import com.marinmiruna.vaultly.session.SensitiveSessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import com.marinmiruna.vaultly.data.sharing.DecryptedFileSharer

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class FilesViewModel @Inject constructor(
    private val fileRepository: FileRepository,
    private val sensitiveSessionManager: SensitiveSessionManager,
    private val decryptedFileSharer: DecryptedFileSharer,
    private val application: Application
) : ViewModel() {

    private val searchQuery = MutableStateFlow("")
    private val errorMessage = MutableStateFlow<String?>(null)
    private val successMessage = MutableStateFlow<String?>(null)

    private val isImporting = MutableStateFlow(false)
    private val openingFileId = MutableStateFlow<Long?>(null)

    val authState: StateFlow<FilesAuthState> = MutableStateFlow(
        FilesAuthState(
            isAuthenticated = sensitiveSessionManager.isFilesSessionValid()
        )
    )

    val uiState: StateFlow<FilesUiState> = searchQuery
        .flatMapLatest { query ->
            fileRepository.observeFiles(query)
        }
        .combine(searchQuery) { files, query ->
            files to query
        }
        .combine(errorMessage) { (files, query), error ->
            Triple(files, query, error)
        }
        .combine(successMessage) { fileState, success ->
            Quadruple(
                fileState.first,
                fileState.second,
                fileState.third,
                success
            )
        }
        .combine(isImporting) { fileState, importing ->
            Quintuple(
                fileState.first,
                fileState.second,
                fileState.third,
                fileState.fourth,
                importing
            )
        }
        .combine(openingFileId) { fileState, openingId ->
            FilesUiState(
                files = fileState.first,
                searchQuery = fileState.second,
                errorMessage = fileState.third,
                successMessage = fileState.fourth,
                isImporting = fileState.fifth,
                openingFileId = openingId
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = FilesUiState()
        )

    fun markAuthenticated() {
        sensitiveSessionManager.markFilesAuthenticated()
        (authState as MutableStateFlow).value = FilesAuthState(isAuthenticated = true)
    }

    fun refreshAuthState() {
        (authState as MutableStateFlow).value = FilesAuthState(
            isAuthenticated = sensitiveSessionManager.isFilesSessionValid()
        )
    }

    fun isFilesSessionValid(): Boolean {
        return sensitiveSessionManager.isFilesSessionValid()
    }

    fun onSearchQueryChange(query: String) {
        searchQuery.value = query
    }

    fun importFile(
        uri: Uri,
        displayName: String,
        mimeType: String
    ) {
        viewModelScope.launch {
            isImporting.value = true

            runCatching {
                fileRepository.importFile(
                    sourceUri = uri,
                    displayName = displayName,
                    mimeType = mimeType
                )
            }.onSuccess {
                successMessage.value = application.getString(R.string.files_import_success)
            }.onFailure { exception ->
                errorMessage.value = UserMessageMapper.fileOperationMessage(
                    context = application,
                    exception = exception
                )
            }

            isImporting.value = false
        }
    }

    fun deleteFile(fileItem: FileItem) {
        viewModelScope.launch {
            runCatching {
                fileRepository.deleteFile(fileItem)
            }.onSuccess {
                successMessage.value = application.getString(R.string.files_delete_success)
            }
                .onFailure { exception ->
                errorMessage.value = UserMessageMapper.fileOperationMessage(
                    context = application,
                    exception = exception
                )
            }
        }
    }

    fun createShareUriForFile(
        fileItem: FileItem,
        onUriReady: (Uri) -> Unit
    ) {
        viewModelScope.launch {
            openingFileId.value = fileItem.id

            runCatching {
                decryptedFileSharer.createShareUri(
                    fileItem = fileItem
                ) { outputFile ->
                    fileRepository.decryptFileTo(
                        fileItem = fileItem,
                        outputFile = outputFile
                    )
                }
            }.onSuccess { uri ->
                successMessage.value = application.getString(R.string.files_open_ready)
                onUriReady(uri)
            }.onFailure { exception ->
                errorMessage.value = UserMessageMapper.fileOperationMessage(
                    context = application,
                    exception = exception
                )
            }

            openingFileId.value = null
        }
    }

    fun clearTemporaryDecryptedFiles() {
        decryptedFileSharer.clearTemporaryDecryptedFiles()
    }

    fun clearErrorMessage() {
        errorMessage.value = null
    }

    fun clearSuccessMessage() {
        successMessage.value = null
    }
}

data class FilesAuthState(
    val isAuthenticated: Boolean = false
)

data class FilesUiState(
    val files: List<FileItem> = emptyList(),
    val searchQuery: String = "",
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val isImporting: Boolean = false,
    val openingFileId: Long? = null
)

private data class Quadruple<A, B, C, D>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D
)

private data class Quintuple<A, B, C, D, E>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D,
    val fifth: E
)