package com.marinmiruna.vaultly.ui.theme

import android.os.Build
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val VaultlyDarkColorScheme = darkColorScheme(
    primary = VaultlyPrimary,
    onPrimary = VaultlyTextPrimary,
    primaryContainer = VaultlyPrimaryPressed,
    onPrimaryContainer = VaultlyTextPrimary,

    secondary = VaultlyTextSecondary,
    onSecondary = VaultlySecondary,

    background = VaultlyBackground,
    onBackground = VaultlyTextPrimary,

    surface = VaultlySurfaceMuted,
    onSurface = VaultlyTextPrimary,

    surfaceVariant = VaultlySurfaceElevated,
    onSurfaceVariant = VaultlyTextSecondary,

    error = VaultlyDanger,
    onError = VaultlyTextPrimary,

    outline = VaultlyBorder,
    outlineVariant = VaultlyBorderSubtle,

    tertiary = VaultlySuccess,
    onTertiary = VaultlyTextPrimary
)

@Composable
fun VaultlyTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val colorScheme = if (
        dynamicColor &&
        darkTheme &&
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    ) {
        dynamicDarkColorScheme(context)
    } else {
        VaultlyDarkColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}