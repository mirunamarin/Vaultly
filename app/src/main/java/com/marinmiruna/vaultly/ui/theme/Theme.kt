package com.marinmiruna.vaultly.ui.theme

import android.os.Build
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color

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

private val VaultlyLightColorScheme = lightColorScheme(
    primary = VaultlyLightPrimary,
    onPrimary = Color.White,
    primaryContainer = VaultlyLightPrimarySoft,
    onPrimaryContainer = VaultlyLightTextPrimary,

    secondary = VaultlyLightTextSecondary,
    onSecondary = Color.White,

    background = VaultlyLightBackground,
    onBackground = VaultlyLightTextPrimary,

    surface = VaultlyLightSurface,
    onSurface = VaultlyLightTextPrimary,

    surfaceVariant = VaultlyLightSurfaceVariant,
    onSurfaceVariant = VaultlyLightTextSecondary,

    error = VaultlyLightDanger,
    onError = Color.White,

    outline = VaultlyLightBorder,
    outlineVariant = VaultlyLightBorderSubtle,

    tertiary = VaultlyLightSuccess,
    onTertiary = Color.White
)

@Composable
fun VaultlyTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val colorScheme = when {
        dynamicColor && darkTheme && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            dynamicDarkColorScheme(context)
        }

        dynamicColor && !darkTheme && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            dynamicLightColorScheme(context)
        }

        darkTheme -> VaultlyDarkColorScheme

        else -> VaultlyLightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}