package com.marinmiruna.vaultly.ui.screens

import androidx.compose.runtime.Composable
import com.marinmiruna.vaultly.ui.navigation.VaultlyNavGraph
import com.marinmiruna.vaultly.ui.theme.ThemeMode

@Composable
fun UnlockedScreen(
    onTrustedSystemActivityStarted: () -> Unit,
    isPasswordsSessionValid: () -> Boolean,
    onPasswordsAuthRequested: (onSuccess: () -> Unit) -> Unit,
    onFilesAuthRequested: (onSuccess: () -> Unit) -> Unit,
    onPhotosAuthRequested: (onSuccess: () -> Unit) -> Unit,
    onExportAuthRequested: (onSuccess: () -> Unit) -> Unit,
    themeMode: ThemeMode,
    onThemeModeChange: (ThemeMode) -> Unit,
) {
    VaultlyNavGraph(
        onTrustedSystemActivityStarted = onTrustedSystemActivityStarted,
        isPasswordsSessionValid = isPasswordsSessionValid,
        onPasswordsAuthRequested = onPasswordsAuthRequested,
        onFilesAuthRequested = onFilesAuthRequested,
        onPhotosAuthRequested = onPhotosAuthRequested,
        onExportAuthRequested = onExportAuthRequested,
        themeMode = themeMode,
        onThemeModeChange = onThemeModeChange
    )
}