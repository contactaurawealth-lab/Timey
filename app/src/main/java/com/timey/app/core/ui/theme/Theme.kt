package com.timey.app.core.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = EmeraldDragon,
    onPrimary = KomodoObsidian,
    secondary = FieryAmber,
    onSecondary = KomodoObsidian,
    tertiary = CalmingSky,
    background = KomodoObsidian,
    surface = KomodoSurface,
    surfaceVariant = KomodoSurfaceVariant,
    onBackground = TextWhite,
    onSurface = TextWhite,
    error = FireRed
)

@Composable
fun TimeyTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}
