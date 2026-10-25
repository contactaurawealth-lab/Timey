package com.timey.app.core.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import com.timey.app.domain.model.ThemeMode

@Composable
fun TimeyTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    accentIndex: Int = 0,
    content: @Composable () -> Unit
) {
    val isDark = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
    }

    val palette = AccentPalettes.getOrElse(accentIndex) { AccentPalettes[0] }

    val darkScheme = darkColorScheme(
        primary = palette.darkPrimary,
        onPrimary = DarkBg,
        primaryContainer = DarkSurfaceVariant,
        onPrimaryContainer = palette.darkPrimary,
        secondary = palette.darkSecondary,
        onSecondary = DarkBg,
        tertiary = SoftSky,
        background = DarkBg,
        onBackground = DarkTextPrimary,
        surface = DarkSurface,
        onSurface = DarkTextPrimary,
        surfaceVariant = DarkSurfaceVariant,
        onSurfaceVariant = DarkTextSecondary,
        outline = DarkBorder,
        error = SoftWarning
    )

    val lightScheme = lightColorScheme(
        primary = palette.lightPrimary,
        onPrimary = LightSurface,
        primaryContainer = LightSurfaceVariant,
        onPrimaryContainer = palette.lightPrimary,
        secondary = palette.lightSecondary,
        onSecondary = LightSurface,
        tertiary = SoftSky,
        background = LightBg,
        onBackground = LightTextPrimary,
        surface = LightSurface,
        onSurface = LightTextPrimary,
        surfaceVariant = LightSurfaceVariant,
        onSurfaceVariant = LightTextSecondary,
        outline = LightBorder,
        error = SoftWarning
    )

    val colorScheme = if (isDark) darkScheme else lightScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
