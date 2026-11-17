package com.teczo.khatabill.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// Placeholder brand palette (deep green + amber); high contrast on purpose. Final branding comes later.
val LightColors = lightColorScheme(
    primary = Color(0xFF1B5E20),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFC8E6C9),
    onPrimaryContainer = Color(0xFF0B2E0F),
    secondary = Color(0xFF8A5A00),
    onSecondary = Color(0xFFFFFFFF),
    background = Color(0xFFFCFDF7),
    onBackground = Color(0xFF1A1C19),
    surface = Color(0xFFFCFDF7),
    onSurface = Color(0xFF1A1C19),
)

val DarkColors = darkColorScheme(
    primary = Color(0xFF81C784),
    onPrimary = Color(0xFF00390F),
    primaryContainer = Color(0xFF1B5E20),
    onPrimaryContainer = Color(0xFFC8E6C9),
    secondary = Color(0xFFFFB74D),
    onSecondary = Color(0xFF452B00),
    background = Color(0xFF121411),
    onBackground = Color(0xFFE3E3DC),
    surface = Color(0xFF121411),
    onSurface = Color(0xFFE3E3DC),
)
