package com.teczo.khatabill.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/**
 * Deep teal + warm orange + soft peach, following the approved design reference.
 *  - primary   : deep teal (hero cards, text fields, radio buttons, selected states)
 *  - secondary : warm orange (the main action button, the raised centre tab, accents)
 *  - peach     : tinted tiles (secondaryContainer)
 * Text on orange is dark teal (white on this orange is too faint for small text).
 */
object Brand {
    val Teal = Color(0xFF1B4B5A)
    val TealDark = Color(0xFF11323C)
    val Orange = Color(0xFFF5A54A)
    val OrangeDeep = Color(0xFF9A5B00) // orange used as text on white (5:1 contrast)
    val Peach = Color(0xFFFDE9D7)
}

val LightColors = lightColorScheme(
    primary = Brand.Teal,
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFD8E9EE),
    onPrimaryContainer = Brand.TealDark,
    secondary = Brand.Orange,
    onSecondary = Brand.TealDark,
    secondaryContainer = Brand.Peach,
    onSecondaryContainer = Brand.TealDark,
    tertiary = Brand.OrangeDeep,
    onTertiary = Color(0xFFFFFFFF),
    background = Color(0xFFF8FAFA),
    onBackground = Color(0xFF14333D),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF14333D),
    surfaceVariant = Color(0xFFEAF0F2),
    onSurfaceVariant = Color(0xFF4A6872),
    surfaceContainerLow = Color(0xFFFFFFFF),
    surfaceContainer = Color(0xFFFFFFFF),
    surfaceContainerHigh = Color(0xFFF3F7F8),
    outline = Color(0xFF8FA6AD),
    outlineVariant = Color(0xFFD3DEE1),
    error = Color(0xFFB3261E),
    onError = Color(0xFFFFFFFF),
)

val DarkColors = darkColorScheme(
    primary = Color(0xFF8CCFE0),
    onPrimary = Color(0xFF06313C),
    primaryContainer = Brand.Teal,
    onPrimaryContainer = Color(0xFFE0F4FA),
    secondary = Brand.Orange,
    onSecondary = Color(0xFF3A2200),
    secondaryContainer = Color(0xFF4A3622),
    onSecondaryContainer = Brand.Peach,
    tertiary = Color(0xFFFFC27A),
    onTertiary = Color(0xFF3A2200),
    background = Color(0xFF0E1B20),
    onBackground = Color(0xFFE3EDEF),
    surface = Color(0xFF132429),
    onSurface = Color(0xFFE3EDEF),
    surfaceVariant = Color(0xFF1E3138),
    onSurfaceVariant = Color(0xFFA9C1C8),
    surfaceContainerLow = Color(0xFF16292F),
    surfaceContainer = Color(0xFF16292F),
    surfaceContainerHigh = Color(0xFF1E3138),
    outline = Color(0xFF6F8A92),
    outlineVariant = Color(0xFF2E454C),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
)
