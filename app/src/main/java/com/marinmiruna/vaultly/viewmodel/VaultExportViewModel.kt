package com.marinmiruna.vaultly.viewmodel

import com.marinmiruna.vaultly.R
import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.marinmiruna.vaultly.data.export.VaultExportRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class VaultExportViewModel @Inject constructor(
    private val vaultExportRepository: VaultExportRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(VaultExportUiState())
    val uiState: StateFlow<VaultExportUiState> = _uiState

    fun onExportPasswordChange(password: String) {
        _uiState.value = _uiState.value.copy(exportPassword = password)
    }

    fun onExportPasswordConfirmationChange(passwordConfirmation: String) {
        _uiState.value = _uiState.value.copy(
            exportPasswordConfirmation = passwordConfirmation
        )
    }

    fun prepareExport(): Boolean {
        val state = _uiState.value

        val errorMessage = when {
            state.exportPassword.isBlank() -> {
                context.getString(R.string.export_error_password_empty)
            }
            state.exportPassword.length < MIN_EXPORT_PASSWORD_LENGTH -> {
                context.getString(R.string.export_error_password_too_short)
            }
            state.exportPassword != state.exportPasswordConfirmation -> {
                context.getString(R.string.export_error_password_mismatch)
            }
            else -> null
        }

        if (errorMessage != null) {
            _uiState.value = state.copy(errorMessage = errorMessage)
            return false
        }

        _uiState.value = state.copy(
            errorMessage = null,
            successMessage = null
        )
        return true
    }

    fun createSuggestedExportFileName(): String {
        return "vaultly_export_${System.currentTimeMillis()}.vaultly"
    }

    fun exportVaultToUri(uri: Uri) {
        val exportPassword = _uiState.value.exportPassword

        if (exportPassword.isBlank()) {
            _uiState.value = _uiState.value.copy(
                errorMessage = context.getString(R.string.export_error_password_empty)
            )
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isExporting = true,
                errorMessage = null,
                successMessage = null
            )

            runCatching {
                val exportBytes = vaultExportRepository.exportVault(
                    exportPassword = exportPassword.toCharArray()
                )

                val outputStream = context.contentResolver.openOutputStream(uri)
                    ?: throw IllegalArgumentException(
                        context.getString(R.string.export_error_file_open_failed)
                    )

                outputStream.use { stream ->
                    stream.write(exportBytes)
                    stream.flush()
                }
            }.onSuccess {
                _uiState.value = VaultExportUiState(
                    successMessage = context.getString(R.string.export_success)
                )
            }.onFailure { exception ->
                _uiState.value = _uiState.value.copy(
                    isExporting = false,
                    errorMessage = exception.message ?: context.getString(R.string.export_error_failed)
                )
            }
        }
    }

    fun clearMessages() {
        _uiState.value = _uiState.value.copy(
            errorMessage = null,
            successMessage = null
        )
    }

    companion object {
        private const val MIN_EXPORT_PASSWORD_LENGTH = 8
    }
}

data class VaultExportUiState(
    val exportPassword: String = "",
    val exportPasswordConfirmation: String = "",
    val isExporting: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null
)