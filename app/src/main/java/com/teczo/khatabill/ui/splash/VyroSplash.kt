package com.teczo.khatabill.ui.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.drawscope.withTransform
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
import kotlin.math.sin
import kotlin.random.Random

private const val TOTAL_MS = 3600
private const val RING_COUNT = 5

/** Half the width of the name plus the gap: how far right of its final spot the mark starts. */
private const val LOCKUP_SHIFT = 62

// Vyro brand colours on black: deep teal, bright teal and warm amber.
private val GlowDeepTeal = Color(0xFF1B6377)
private val GlowTeal = Color(0xFF5AD1EA)
private val GlowAmber = Color(0xFFF5A54A)

/**
 * Opening animation, in four beats (the same story as the reference clip):
 *  1. Nested glowing rings, teal at the top left and amber at the bottom right, drift on black with tiny stars.
 *  2. The view pushes into the centre: the outer rings sweep past and fade, the inner ring stays.
 *  3. The Vyro mark appears inside that inner ring.
 *  4. The mark slides left while the name "Vyro" arrives beside it, then the tagline.
 * Calls [onFinished] when it is done.
 */
@Composable
fun VyroSplash(onFinished: () -> Unit) {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        progress.animateTo(1f, tween(TOTAL_MS, easing = LinearEasing))
        delay(200)
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

    val stars = remember {
        val random = Random(7)
        List(34) { Star(random.nextFloat() * 2f - 1f, random.nextFloat() * 2f - 1f, random.nextFloat() * 6.28f) }
    }

    val ms = progress.value * TOTAL_MS
    val rings = smooth(ms / 500f) // beat 1: rings fade in
    val zoom = smooth((ms - 1100f) / 1300f) // beat 2: push into the centre
    val mark = smooth((ms - 1900f) / 600f) // beat 3: mark appears
    val lockup = smooth((ms - 2500f) / 600f) // beat 4: mark slides left, name arrives
    val tagline = smooth((ms - 3000f) / 400f)
    val exit = 1f - smooth((ms - (TOTAL_MS - 250f)) / 250f)

    Box(
        Modifier.fillMaxSize().background(Color.Black).graphicsLayer { alpha = exit },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize()) { drawRingScene(ms, rings, zoom, mark, stars) }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // The mark starts in the middle of the screen, then slides left as the name arrives.
                Box(
                    Modifier.size(72.dp)
                        .graphicsLayer {
                            alpha = mark
                            val s = 0.55f + 0.45f * mark
                            scaleX = s
                            scaleY = s
                            translationX = LOCKUP_SHIFT.dp.toPx() * (1f - lockup)
                        }
                        .clip(CircleShape)
                        .background(Color.White),
                    contentAlignment = Alignment.Center,
                ) {
                    // The adaptive icon is 108dp with the mark in the middle 72dp.
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
                    modifier = Modifier.graphicsLayer {
                        alpha = lockup
                        translationX = -24.dp.toPx() * (1f - lockup)
                    },
                )
            }
            Spacer(Modifier.height(14.dp))
            Text(
                stringResource(R.string.app_tagline),
                color = GlowAmber,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.graphicsLayer { alpha = tagline },
            )
        }
    }
}

private data class Star(val x: Float, val y: Float, val phase: Float)

/** 0 until [x] reaches 0, rising smoothly to 1 at x = 1. */
private fun smooth(x: Float): Float {
    val c = x.coerceIn(0f, 1f)
    return c * c * (3f - 2f * c)
}

private fun DrawScope.drawRingScene(ms: Float, rings: Float, zoom: Float, mark: Float, stars: List<Star>) {
    val centre = Offset(size.width / 2f, size.height / 2f)
    val dp = 1.dp.toPx()
    val innerRadius = 50 * dp
    val spacing = 60 * dp

    // A warm glow low and to the right, like the colour pooling in the reference.
    val glowAlpha = rings * (1f - zoom) * 0.22f
    if (glowAlpha > 0f) {
        drawRect(
            Brush.radialGradient(
                listOf(GlowAmber.copy(alpha = glowAlpha), Color.Transparent),
                center = Offset(centre.x + 90 * dp, centre.y + 150 * dp),
                radius = 340 * dp,
            ),
        )
    }

    // Tiny stars drifting in the dark; they fade as the view pushes in.
    val starAlpha = rings * (1f - zoom)
    if (starAlpha > 0f) {
        stars.forEach { s ->
            val twinkle = 0.5f + 0.5f * sin(ms / 420f + s.phase)
            drawCircle(
                Color.White.copy(alpha = 0.55f * twinkle * starAlpha),
                radius = 1.1f * dp,
                center = Offset(centre.x + s.x * size.width * 0.45f, centre.y + s.y * size.height * 0.4f),
            )
        }
    }

    // The nested rings. Outer rings grow and fade away during the push-in; the inner ring stays.
    for (i in RING_COUNT - 1 downTo 0) {
        val outer = i > 0
        val scale = if (outer) 1f + 5.5f * zoom * (0.4f + 0.15f * i) else 1f + 0.12f * zoom
        val alpha = if (outer) rings * (1f - zoom) * (1f - 0.12f * i) else rings * (1f - smooth((ms - 2500f) / 500f))
        if (alpha <= 0.01f) continue
        val radius = innerRadius + spacing * i
        val path = blobPath(centre, radius, phase = ms / 900f + i * 0.8f, wobble = 0.05f + 0.01f * i)
        val brush = Brush.linearGradient(
            colors = listOf(GlowDeepTeal.copy(alpha = 0.35f), GlowTeal, GlowAmber),
            start = Offset(centre.x - radius, centre.y - radius),
            end = Offset(centre.x + radius, centre.y + radius),
        )
        withTransform({ scale(scale, scale, centre) }) {
            drawPath(path, brush, alpha = alpha * 0.10f, style = Stroke(width = 26 * dp, join = StrokeJoin.Round))
            drawPath(path, brush, alpha = alpha * 0.22f, style = Stroke(width = 12 * dp, join = StrokeJoin.Round))
            drawPath(path, brush, alpha = alpha * 0.9f, style = Stroke(width = 2 * dp, join = StrokeJoin.Round))
        }
    }

    // A soft halo around the mark when it appears, which fades into the final lockup.
    val halo = mark * (1f - smooth((ms - 2500f) / 600f))
    if (halo > 0f) {
        drawCircle(
            Brush.radialGradient(listOf(GlowTeal.copy(alpha = 0.35f * halo), Color.Transparent), center = centre, radius = 90 * dp),
            radius = 90 * dp,
            center = centre,
        )
    }
}

/** A gently wobbling circle, so the rings look like soft liquid rather than perfect circles. */
private fun blobPath(centre: Offset, radius: Float, phase: Float, wobble: Float): Path {
    val path = Path()
    val steps = 96
    for (s in 0..steps) {
        val a = (2.0 * PI * s / steps).toFloat()
        val w = 1f + wobble * sin(3f * a + phase) + wobble * 0.7f * sin(2f * a - phase * 1.3f)
        val r = radius * w
        val x = centre.x + r * cos(a)
        val y = centre.y + r * sin(a)
        if (s == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    path.close()
    return path
}
