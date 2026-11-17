package com.teczo.khatabill.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.teczo.khatabill.R

/** One variable font file covers every weight; each weight pins the "wght" axis. minSdk 26 supports this. */
private fun variableFamily(resId: Int): FontFamily = FontFamily(
    listOf(400, 500, 600, 700).map { w ->
        Font(
            resId = resId,
            weight = FontWeight(w),
            variationSettings = FontVariation.Settings(FontVariation.weight(w)),
        )
    },
)

private val KannadaFamily = variableFamily(R.font.noto_sans_kannada)
private val DevanagariFamily = variableFamily(R.font.noto_sans_devanagari)

/** Bundled font per language so text renders the same on every phone; English uses the system font. */
fun fontFamilyFor(language: String): FontFamily = when (language) {
    "kn" -> KannadaFamily
    "hi" -> DevanagariFamily
    else -> FontFamily.Default
}

/**
 * Body text is at least 16 sp (UI rule). Line heights are generous so Kannada and Devanagari
 * ascenders and conjuncts are not clipped.
 */
fun typographyFor(language: String): Typography {
    val family = fontFamilyFor(language)
    fun style(size: Int, weight: FontWeight = FontWeight.Normal) = TextStyle(
        fontFamily = family,
        fontSize = size.sp,
        lineHeight = (size * 1.5f).sp,
        fontWeight = weight,
    )
    val base = Typography()
    return base.copy(
        displayLarge = style(48, FontWeight.Bold),
        displayMedium = style(40, FontWeight.Bold),
        displaySmall = style(32, FontWeight.Bold),
        headlineLarge = style(30, FontWeight.Bold),
        headlineMedium = style(26, FontWeight.Bold),
        headlineSmall = style(22, FontWeight.SemiBold),
        titleLarge = style(22, FontWeight.SemiBold),
        titleMedium = style(18, FontWeight.SemiBold),
        titleSmall = style(16, FontWeight.SemiBold),
        bodyLarge = style(18),
        bodyMedium = style(16),
        bodySmall = style(14),
        labelLarge = style(16, FontWeight.Medium),
        labelMedium = style(14, FontWeight.Medium),
        labelSmall = style(12, FontWeight.Medium),
    )
}
