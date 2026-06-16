package com.marinmiruna.vaultly.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.marinmiruna.vaultly.R
import com.marinmiruna.vaultly.domain.security.PasswordSecurityIssue
import com.marinmiruna.vaultly.domain.security.PasswordSecurityReport
import com.marinmiruna.vaultly.ui.components.ConfirmDeleteDialog
import com.marinmiruna.vaultly.ui.components.PasswordGeneratorSheet
import com.marinmiruna.vaultly.ui.components.ScreenHeader
import com.marinmiruna.vaultly.ui.components.SecureTextField
import com.marinmiruna.vaultly.viewmodel.PasswordsViewModel
import com.marinmiruna.vaultly.ui.components.VaultlyTextField

@Composable
fun PasswordDetailScreen(
    passwordId: Long,
    onBack: () -> Unit,
    viewModel: PasswordsViewModel = hiltViewModel()
) {
    val detailState by viewModel.detailState.collectAsStateWithLifecycle()
    val errorMessage = detailState.errorMessage
    val context = LocalContext.current
    val passwordCopiedMessage = stringResource(R.string.password_copied)
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showGeneratorSheet by remember { mutableStateOf(false) }
    var showDiscardDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (!viewModel.isPasswordSessionValid()) {
            onBack()
        }
    }

    LaunchedEffect(passwordId) {
        viewModel.loadPassword(passwordId)
    }

    if (showDeleteDialog) {
        ConfirmDeleteDialog(
            title = stringResource(R.string.password_delete_dialog_title),
            message = stringResource(R.string.password_delete_dialog_message),
            onConfirm = {
                showDeleteDialog = false
                viewModel.deleteCurrentPassword(onDeleted = onBack)
            },
            onDismiss = {
                showDeleteDialog = false
            }
        )
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

    fun handleBack() {
        if (detailState.hasUnsavedChanges) {
            showDiscardDialog = true
        } else {
            onBack()
        }
    }

    BackHandler {
        handleBack()
    }

    if (showGeneratorSheet) {
        PasswordGeneratorSheet(
            generatedPassword = detailState.generatedPassword,
            length = detailState.generatorLength,
            includeUppercase = detailState.generatorUppercase,
            includeDigits = detailState.generatorDigits,
            includeSymbols = detailState.generatorSymbols,
            onLengthChange = viewModel::onGeneratorLengthChange,
            onIncludeUppercaseChange = viewModel::onGeneratorUppercaseChange,
            onIncludeDigitsChange = viewModel::onGeneratorDigitsChange,
            onIncludeSymbolsChange = viewModel::onGeneratorSymbolsChange,
            onGenerate = viewModel::generatePassword,
            onUsePassword = {
                viewModel.useGeneratedPassword()
                showGeneratorSheet = false
            },
            onDismiss = {
                showGeneratorSheet = false
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
                title = if (passwordId == 0L) {
                    stringResource(R.string.password_new_title)
                } else {
                    stringResource(R.string.password_edit_title)
                },
                onBack = { handleBack() }
            )
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
                    VaultlyTextField(
                        value = detailState.service,
                        onValueChange = viewModel::onServiceChange,
                        label = stringResource(R.string.password_service_label)
                    )
                    VaultlyTextField(
                        value = detailState.username,
                        onValueChange = viewModel::onUsernameChange,
                        label = stringResource(R.string.password_username_label)
                    )
                    SecureTextField(
                        value = detailState.password,
                        onValueChange = viewModel::onPasswordChange,
                        label = stringResource(R.string.password_label)
                    )
                    PasswordSecurityWarnings(
                        securityReport = detailState.securityReport
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                showGeneratorSheet = true
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.surface,
                                contentColor = MaterialTheme.colorScheme.onSurface
                            )
                        ) {
                            Text(
                                text = stringResource(R.string.password_generator_button),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Button(
                            onClick = {
                                viewModel.copyPasswordToClipboard(detailState.password)
                                android.widget.Toast.makeText(
                                    context,
                                    passwordCopiedMessage,
                                    android.widget.Toast.LENGTH_SHORT
                                ).show()
                            },
                            modifier = Modifier.weight(1f),
                            enabled = detailState.password.isNotBlank(),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary,
                                disabledContainerColor = MaterialTheme.colorScheme.surface,
                                disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        ) {
                            Text(
                                text = stringResource(R.string.password_copy_button),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                    VaultlyTextField(
                        value = detailState.url,
                        onValueChange = viewModel::onUrlChange,
                        label = stringResource(R.string.password_url_label)
                    )
                    VaultlyTextField(
                        value = detailState.note,
                        onValueChange = viewModel::onNoteChange,
                        label = stringResource(R.string.password_note_label),
                        modifier = Modifier.heightIn(min = 120.dp),
                        singleLine = false
                    )
                    if (errorMessage != null) {
                        Text(
                            text = errorMessage,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
            Button(
                onClick = {
                    viewModel.savePassword(onSaved = onBack)
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Text(
                    text = stringResource(R.string.password_save_button),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
            }
            if (detailState.canDelete) {
                TextButton(
                    onClick = {
                        showDeleteDialog = true
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = stringResource(R.string.password_delete_button),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
private fun PasswordSecurityWarnings(
    securityReport: PasswordSecurityReport?
) {
    if (securityReport == null || securityReport.isSafe) {
        return
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.error
        ),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = stringResource(R.string.password_security_warning_title),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.error
            )
            securityReport.issues.forEach { issue ->
                Text(
                    text = passwordSecurityIssueText(issue),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
private fun passwordSecurityIssueText(
    issue: PasswordSecurityIssue
): String {
    return when (issue) {
        PasswordSecurityIssue.TOO_SHORT -> {
            stringResource(R.string.password_security_issue_too_short)
        }

        PasswordSecurityIssue.NO_LOWERCASE -> {
            stringResource(R.string.password_security_issue_no_lowercase)
        }

        PasswordSecurityIssue.NO_UPPERCASE -> {
            stringResource(R.string.password_security_issue_no_uppercase)
        }

        PasswordSecurityIssue.NO_DIGIT -> {
            stringResource(R.string.password_security_issue_no_digit)
        }

        PasswordSecurityIssue.NO_SYMBOL -> {
            stringResource(R.string.password_security_issue_no_symbol)
        }

        PasswordSecurityIssue.COMMON_PASSWORD -> {
            stringResource(R.string.password_security_issue_common)
        }

        PasswordSecurityIssue.REUSED -> {
            stringResource(R.string.password_security_issue_reused)
        }
    }
}