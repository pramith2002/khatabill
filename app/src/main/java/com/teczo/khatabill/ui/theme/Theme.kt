package com.teczo.khatabill.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration

/** Follows the system light/dark setting for now; the in-app theme switch comes with Settings. */
@Composable
fun KhataBillTheme(content: @Composable () -> Unit) {
    val language = LocalConfiguration.current.locales[0].language
    val typography = remember(language) { typographyFor(language) }
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        typography = typography,
        content = content,
    )
}
