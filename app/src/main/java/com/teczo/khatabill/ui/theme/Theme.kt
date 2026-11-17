package com.teczo.khatabill.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp

/** Very rounded corners, as in the design reference. */
private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(16.dp),
    medium = RoundedCornerShape(24.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(36.dp),
)

/** Follows the system light/dark setting for now; the in-app theme switch comes with Settings. */
@Composable
fun KhataBillTheme(content: @Composable () -> Unit) {
    val language = LocalConfiguration.current.locales[0].language
    val typography = remember(language) { typographyFor(language) }
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        typography = typography,
        shapes = AppShapes,
        content = content,
    )
}

/** The soft peach-to-pale-blue page backdrop behind the tabs (plain background in dark mode). */
@Composable
fun pageBackground(): Brush {
    val colors = MaterialTheme.colorScheme
    return if (isSystemInDarkTheme()) {
        Brush.verticalGradient(listOf(colors.background, colors.background))
    } else {
        Brush.verticalGradient(listOf(Color(0xFFFFEBD6), Color(0xFFE6F1F4), Color(0xFFF8FAFA)))
    }
}
