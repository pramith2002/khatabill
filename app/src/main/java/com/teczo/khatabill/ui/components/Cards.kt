package com.teczo.khatabill.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Blue-teal to warm amber, the soft gradient used on hero cards (as in the design references). */
private val HeroBrush = Brush.linearGradient(
    colors = listOf(Color(0xFF11323C), Color(0xFF1B6377), Color(0xFF2B8197), Color(0xFFB8741F)),
    start = Offset(0f, 0f),
    end = Offset(1100f, 700f),
)

/** Gradient hero card with white text (the big card at the top of Home). */
@Composable
fun TealCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    val shape = RoundedCornerShape(32.dp)
    Card(
        modifier = modifier,
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = Color.Transparent, contentColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
    ) { Column(Modifier.clip(shape).background(HeroBrush).padding(22.dp), content = content) }
}

/** Soft peach tile. */
@Composable
fun PeachCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        ),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.5f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) { Column(Modifier.padding(16.dp), content = content) }
}

/** Soft white tile with a hairline border and a soft shadow. */
@Composable
fun WhiteCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.6f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
    ) { Column(Modifier.padding(16.dp), content = content) }
}

/** A bento tile: small label, a big number, and an optional tiny trend line underneath. */
@Composable
fun StatTile(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    trend: List<Long>? = null,
    peach: Boolean = false,
    valueColor: Color = Color.Unspecified,
) {
    val body: @Composable ColumnScope.() -> Unit = {
        Text(label, style = MaterialTheme.typography.titleSmall)
        Text(
            value,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
            color = valueColor,
            modifier = Modifier.padding(top = 4.dp),
        )
        if (trend != null) Sparkline(trend, Modifier.padding(top = 10.dp))
    }
    if (peach) PeachCard(modifier, body) else WhiteCard(modifier, body)
}

/** A tiny smooth trend line with a dot on the latest value. Flat when there is no data yet. */
@Composable
fun Sparkline(
    values: List<Long>,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
) {
    Canvas(modifier.fillMaxWidth().height(30.dp)) {
        if (values.size < 2) return@Canvas
        val stroke = 2.dp.toPx()
        val dot = 3.5.dp.toPx()
        val max = values.max().coerceAtLeast(1L).toFloat()
        val stepX = (size.width - dot) / (values.size - 1)
        val top = dot
        val bottom = size.height - dot
        fun y(v: Long) = bottom - (v / max) * (bottom - top)
        val path = Path()
        var prevX = 0f
        var prevY = y(values.first())
        path.moveTo(prevX, prevY)
        for (i in 1 until values.size) {
            val x = i * stepX
            val yy = y(values[i])
            val mid = (prevX + x) / 2
            path.cubicTo(mid, prevY, mid, yy, x, yy)
            prevX = x
            prevY = yy
        }
        drawPath(path, color, style = Stroke(stroke, cap = StrokeCap.Round, join = StrokeJoin.Round))
        drawCircle(color, dot, Offset(prevX, prevY))
    }
}

/** The main action: a full-width-capable orange pill with dark teal text (accessible contrast). */
@Composable
fun AccentButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit,
) {
    Button(
        onClick = onClick,
        modifier = modifier.heightIn(min = 56.dp),
        enabled = enabled,
        shape = CircleShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.secondary,
            contentColor = MaterialTheme.colorScheme.onSecondary,
            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        ),
        content = content,
    )
}

/** Circular progress with the percentage in the middle. */
@Composable
fun ProgressRing(
    fraction: Float,
    modifier: Modifier = Modifier,
    size: Dp = 92.dp,
    trackColor: Color = Color.White.copy(alpha = 0.25f),
    progressColor: Color = MaterialTheme.colorScheme.secondary,
    textColor: Color = Color.White,
) {
    val clamped = fraction.coerceIn(0f, 1f)
    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(size)) {
            val stroke = 10.dp.toPx()
            val inset = stroke / 2
            val arc = Size(this.size.width - stroke, this.size.height - stroke)
            drawArc(trackColor, 0f, 360f, false, Offset(inset, inset), arc, style = Stroke(stroke))
            drawArc(progressColor, -90f, 360f * clamped, false, Offset(inset, inset), arc, style = Stroke(stroke, cap = StrokeCap.Round))
        }
        Text("${(clamped * 100).toInt()}%", color = textColor, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
    }
}
