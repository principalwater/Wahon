package com.wahon.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColorScheme = lightColorScheme(
    primary = WahonPrimary,
    onPrimary = WahonOnPrimary,
    primaryContainer = WahonPrimaryContainer,
    onPrimaryContainer = WahonOnPrimaryContainer,
    secondary = WahonSecondary,
    onSecondary = WahonOnSecondary,
    secondaryContainer = WahonSecondaryContainer,
    onSecondaryContainer = WahonOnSecondaryContainer,
    tertiary = WahonTertiary,
    onTertiary = WahonOnTertiary,
    tertiaryContainer = WahonTertiaryContainer,
    onTertiaryContainer = WahonOnTertiaryContainer,
)

private val DarkColorScheme = darkColorScheme(
    primary = WahonPrimaryContainer,
    onPrimary = WahonOnPrimaryContainer,
    primaryContainer = WahonPrimary,
    onPrimaryContainer = WahonOnPrimary,
    secondary = WahonSecondaryContainer,
    onSecondary = WahonOnSecondaryContainer,
    secondaryContainer = WahonSecondary,
    onSecondaryContainer = WahonOnSecondary,
    tertiary = WahonTertiaryContainer,
    onTertiary = WahonOnTertiaryContainer,
    tertiaryContainer = WahonTertiary,
    onTertiaryContainer = WahonOnTertiary,
)

@Composable
fun WahonTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = WahonTypography,
        content = content,
    )
}
