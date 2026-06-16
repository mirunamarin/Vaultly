package com.marinmiruna.vaultly.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.marinmiruna.vaultly.R
import com.marinmiruna.vaultly.ui.components.SecureTextField
import com.marinmiruna.vaultly.viewmodel.VaultExportViewModel
import com.marinmiruna.vaultly.ui.components.ConfirmDeleteDialog
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.marinmiruna.vaultly.ui.theme.ThemeMode
import com.marinmiruna.vaultly.ui.components.ScreenHeader

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onExportAuthRequested: (onSuccess: () -> Unit) -> Unit,
    exportViewModel: VaultExportViewModel = hiltViewModel(),
    themeMode: ThemeMode,
    onThemeModeChange: (ThemeMode) -> Unit,
) {
    val exportState by exportViewModel.uiState.collectAsStateWithLifecycle()

    var showDiscardDialog by remember { mutableStateOf(false) }

    val hasUnsavedExportInput =
        exportState.exportPassword.isNotBlank() ||
                exportState.exportPasswordConfirmation.isNotBlank()

    fun handleBack() {
        if (hasUnsavedExportInput) {
            showDiscardDialog = true
        } else {
            onBack()
        }
    }

    BackHandler {
        handleBack()
    }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri ->
        if (uri != null) {
            exportViewModel.exportVaultToUri(uri)
        }
    }

    if (showDiscardDialog) {
        ConfirmDeleteDialog(
            title = stringResource(R.string.discard_changes_dialog_title),
            message = stringResource(R.string.discard_changes_dialog_message),
            confirmText = stringResource(R.string.discard_changes_confirm),
            onConfirm = {
                showDiscardDialog = false
                onBack()
            },
            onDismiss = {
                showDiscardDialog = false
            }
        )
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ScreenHeader(
                title = stringResource(R.string.settings_title),
                onBack = { handleBack() }
            )

            ThemeModeCard(
                selectedMode = themeMode,
                onModeSelected = onThemeModeChange
            )

            ExportSettingsCard(
                exportPassword = exportState.exportPassword,
                exportPasswordConfirmation = exportState.exportPasswordConfirmation,
                isExporting = exportState.isExporting,
                errorMessage = exportState.errorMessage,
                successMessage = exportState.successMessage,
                onExportPasswordChange = exportViewModel::onExportPasswordChange,
                onExportPasswordConfirmationChange = exportViewModel::onExportPasswordConfirmationChange,
                onExportClick = {
                    if (exportViewModel.prepareExport()) {
                        onExportAuthRequested {
                            exportLauncher.launch(exportViewModel.createSuggestedExportFileName())
                        }
                    }
                }
            )
        }
    }
}
@Composable
private fun ThemeModeCard(
    selectedMode: ThemeMode,
    onModeSelected: (ThemeMode) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
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
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = stringResource(R.string.theme_mode_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ThemeModeButton(
                    text = stringResource(R.string.theme_mode_system),
                    selected = selectedMode == ThemeMode.System,
                    onClick = { onModeSelected(ThemeMode.System) },
                    modifier = Modifier.fillMaxWidth()
                )

                ThemeModeButton(
                    text = stringResource(R.string.theme_mode_light),
                    selected = selectedMode == ThemeMode.Light,
                    onClick = { onModeSelected(ThemeMode.Light) },
                    modifier = Modifier.fillMaxWidth()
                )

                ThemeModeButton(
                    text = stringResource(R.string.theme_mode_dark),
                    selected = selectedMode == ThemeMode.Dark,
                    onClick = { onModeSelected(ThemeMode.Dark) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun ThemeModeButton(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (selected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.surface
            },
            contentColor = if (selected) {
                MaterialTheme.colorScheme.onPrimary
            } else {
                MaterialTheme.colorScheme.onSurface
            }
        )
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun ExportSettingsCard(
    exportPassword: String,
    exportPasswordConfirmation: String,
    isExporting: Boolean,
    errorMessage: String?,
    successMessage: String?,
    onExportPasswordChange: (String) -> Unit,
    onExportPasswordConfirmationChange: (String) -> Unit,
    onExportClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
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
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = stringResource(R.string.export_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Text(
                    text = stringResource(R.string.export_body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            SecureTextField(
                value = exportPassword,
                onValueChange = onExportPasswordChange,
                label = stringResource(R.string.export_password_label),
                enabled = !isExporting
            )

            SecureTextField(
                value = exportPasswordConfirmation,
                onValueChange = onExportPasswordConfirmationChange,
                label = stringResource(R.string.export_password_confirmation_label),
                enabled = !isExporting
            )

            if (errorMessage != null) {
                Text(
                    text = errorMessage,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }

            if (successMessage != null) {
                Text(
                    text = successMessage,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.tertiary
                )
            }

            Button(
                onClick = onExportClick,
                modifier = Modifier.fillMaxWidth(),
                enabled = !isExporting,
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    disabledContainerColor = MaterialTheme.colorScheme.surface,
                    disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            ) {
                Text(
                    text = if (isExporting) {
                        stringResource(R.string.export_in_progress)
                    } else {
                        stringResource(R.string.export_button)
                    },
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}