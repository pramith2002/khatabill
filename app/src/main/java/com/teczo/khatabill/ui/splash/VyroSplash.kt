package com.teczo.khatabill.ui.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import com.teczo.khatabill.R
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin

private const val TOTAL_MS = 2600
private const val RIPPLE_COUNT = 4

// Vyro brand colours on black: teal and amber glow.
private val GlowTeal = Color(0xFF5AD1EA)
private val GlowDeepTeal = Color(0xFF1B6377)
private val GlowAmber = Color(0xFFF5A54A)

/**
 * Opening animation: glowing ripple rings spread out from the middle of a black screen (teal into amber),
 * then the Vyro logo, name and tagline appear. Calls [onFinished] when it is done.
 */
@Composable
fun VyroSplash(onFinished: () -> Unit) {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        progress.animateTo(1f, tween(TOTAL_MS, easing = LinearEasing))
        delay(150)
        onFinished()
    }

    // Light status-bar icons on the black screen, restored afterwards.
    val view = LocalView.current
    DisposableEffect(view) {
        val window = (view.context as? android.app.Activity)?.window
        val controller = window?.let { WindowCompat.getInsetsController(it, view) }
        val previous = controller?.isAppearanceLightStatusBars
        controller?.isAppearanceLightStatusBars = false
        onDispose { if (previous != null) controller.isAppearanceLightStatusBars = previous }
    }

    val ms = progress.value * TOTAL_MS
    val logoAlpha = smooth((ms - 1000f) / 600f)
    val exitAlpha = 1f - smooth((ms - (TOTAL_MS - 300f)) / 300f)

    Box(
        Modifier.fillMaxSize().background(Color.Black).graphicsLayer { alpha = exitAlpha },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize()) { drawRipples(ms) }

        Column(
            Modifier.graphicsLayer {
                alpha = logoAlpha
                val scale = 0.92f + 0.08f * logoAlpha
                scaleX = scale
                scaleY = scale
            },
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // The square launcher mark on a white disc (the adaptive icon is 108dp with the mark in the middle 72dp).
                Box(Modifier.size(72.dp).clip(CircleShape).background(Color.White), contentAlignment = Alignment.Center) {
                    Image(
                        painter = painterResource(R.drawable.ic_launcher_foreground),
                        contentDescription = null,
                        modifier = Modifier.requiredSize(108.dp),
                    )
                }
                Spacer(Modifier.width(14.dp))
                Text(
                    stringResource(R.string.app_name),
                    color = Color.White,
                    fontSize = 44.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                )
            }
            Spacer(Modifier.height(14.dp))
            Text(
                stringResource(R.string.app_tagline),
                color = GlowAmber,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

/** 0 until [x] reaches 0, rising smoothly to 1 at x = 1. */
private fun smooth(x: Float): Float {
    val c = x.coerceIn(0f, 1f)
    return c * c * (3f - 2f * c)
}

private fun DrawScope.drawRipples(ms: Float) {
    val centre = Offset(size.width / 2f, size.height / 2f)
    val maxRadius = size.minDimension * 0.95f
    val minRadius = size.minDimension * 0.08f
    for (i in 0 until RIPPLE_COUNT) {
        val p = ((ms - i * 200f) / 1700f).coerceIn(0f, 1f)
        if (p <= 0f || p >= 1f) continue
        val eased = 1f - (1f - p).pow(3)
        val radius = minRadius + (maxRadius - minRadius) * eased
        val alpha = (1f - p).pow(0.9f)
        val path = blobPath(centre, radius, phase = ms / 700f + i * 0.9f)
        val brush = Brush.linearGradient(
            colors = listOf(GlowDeepTeal, GlowTeal, GlowAmber),
            start = Offset(centre.x - radius, centre.y - radius),
            end = Offset(centre.x + radius, centre.y + radius),
        )
        // Soft wide halo, then a crisp thin ring on top.
        drawPath(path, brush, alpha = alpha * 0.38f, style = Stroke(width = 16.dp.toPx(), join = StrokeJoin.Round))
        drawPath(path, brush, alpha = alpha, style = Stroke(width = 2.5.dp.toPx(), join = StrokeJoin.Round))
    }
}

/** A gently wobbling circle, so the rings look like soft liquid rather than perfect circles. */
private fun blobPath(centre: Offset, radius: Float, phase: Float): Path {
    val path = Path()
    val steps = 96
    for (s in 0..steps) {
        val a = (2.0 * PI * s / steps).toFloat()
        val wobble = 1f + 0.05f * sin(3f * a + phase) + 0.035f * sin(2f * a - phase * 1.3f)
        val r = radius * wobble
        val x = centre.x + r * cos(a)
        val y = centre.y + r * sin(a)
        if (s == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    path.close()
    return path
}
