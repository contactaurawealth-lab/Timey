package com.timey.app.core.ui.theme

import androidx.compose.ui.graphics.Color

// Neutral Backgrounds & Cards (Dark Mode - Slate 900)
val DarkBg = Color(0xFF0B0F17)
val DarkSurface = Color(0xFF151D2A)
val DarkSurfaceVariant = Color(0xFF1E293B)
val DarkBorder = Color(0x3394A3B8)
val DarkTextPrimary = Color(0xFFF8FAFC)
val DarkTextSecondary = Color(0xFF94A3B8)

// Neutral Backgrounds & Cards (Light Mode - Clean Paper)
val LightBg = Color(0xFFF8FAFC)
val LightSurface = Color(0xFFFFFFFF)
val LightSurfaceVariant = Color(0xFFF1F5F9)
val LightBorder = Color(0xFFE2E8F0)
val LightTextPrimary = Color(0xFF0F172A)
val LightTextSecondary = Color(0xFF64748B)

// Status Accents
val SoftWarning = Color(0xFFF43F5E)
val SoftSuccess = Color(0xFF10B981)
val SoftSky = Color(0xFF38BDF8)

data class AccentOption(
    val name: String,
    val darkPrimary: Color,
    val lightPrimary: Color,
    val darkSecondary: Color,
    val lightSecondary: Color
)

val AccentPalettes = listOf(
    // 0: Emerald Focus
    AccentOption(
        name = "Emerald Focus",
        darkPrimary = Color(0xFF2DD4BF),
        lightPrimary = Color(0xFF0D9488),
        darkSecondary = Color(0xFFFBBF24),
        lightSecondary = Color(0xFFD97706)
    ),
    // 1: Warm Amber
    AccentOption(
        name = "Warm Amber",
        darkPrimary = Color(0xFFF59E0B),
        lightPrimary = Color(0xFFD97706),
        darkSecondary = Color(0xFF2DD4BF),
        lightSecondary = Color(0xFF0D9488)
    ),
    // 2: Sky Horizon
    AccentOption(
        name = "Sky Horizon",
        darkPrimary = Color(0xFF38BDF8),
        lightPrimary = Color(0xFF0284C7),
        darkSecondary = Color(0xFFF43F5E),
        lightSecondary = Color(0xFFE11D48)
    ),
    // 3: Sunset Coral
    AccentOption(
        name = "Sunset Coral",
        darkPrimary = Color(0xFFFB7185),
        lightPrimary = Color(0xFFE11D48),
        darkSecondary = Color(0xFF38BDF8),
        lightSecondary = Color(0xFF0284C7)
    ),
    // 4: Sage & Earth
    AccentOption(
        name = "Sage Forest",
        darkPrimary = Color(0xFF34D399),
        lightPrimary = Color(0xFF059669),
        darkSecondary = Color(0xFFFBBF24),
        lightSecondary = Color(0xFFD97706)
    )
)
