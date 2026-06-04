package com.marinmiruna.vaultly.ui.screens

import androidx.compose.runtime.Composable
import com.marinmiruna.vaultly.ui.navigation.VaultlyNavGraph

@Composable
fun UnlockedScreen(
    onTrustedSystemActivityStarted: () -> Unit,
    isPasswordsSessionValid: () -> Boolean,
    onPasswordsAuthRequested: (onSuccess: () -> Unit) -> Unit,
    onFilesAuthRequested: (onSuccess: () -> Unit) -> Unit,
    onPhotosAuthRequested: (onSuccess: () -> Unit) -> Unit,
    onExportAuthRequested: (onSuccess: () -> Unit) -> Unit
) {
    VaultlyNavGraph(
        onTrustedSystemActivityStarted = onTrustedSystemActivityStarted,
        isPasswordsSessionValid = isPasswordsSessionValid,
        onPasswordsAuthRequested = onPasswordsAuthRequested,
        onFilesAuthRequested = onFilesAuthRequested,
        onPhotosAuthRequested = onPhotosAuthRequested,
        onExportAuthRequested = onExportAuthRequested
    )
}