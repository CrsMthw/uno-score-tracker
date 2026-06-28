package com.crsmthw.unotracker.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.window.core.layout.WindowSizeClass
import com.crsmthw.unotracker.ui.theme.SuitColor
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/** True on a wide window (Fold6 unfolded / tablet) — drives the two-pane layouts. */
@Composable
fun isWidePane(): Boolean =
    currentWindowAdaptiveInfo().windowSizeClass.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND)

/**
 * A transparent→surface gradient pinned to the bottom of a scrolling list so content fades out under
 * the transparent nav bar instead of clipping hard. Place inside the Scaffold content Box, aligned
 * bottom. Playbook §6.
 */
@Composable
fun BoxScope.BottomFadeScrim(
    color: Color = MaterialTheme.colorScheme.surface,
    height: Int = 56,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(height.dp)
            .align(Alignment.BottomCenter)
            .background(Brush.verticalGradient(listOf(Color.Transparent, color))),
    )
}

/**
 * A slow, continuously-travelling wavy ring for The Table. The wave pattern rotates around the circle
 * in the direction of play (reverses when [clockwise] flips). Speed is controlled by [periodMillis] —
 * one full revolution — so it reads as a calm "play is flowing this way" cue, not a busy spinner.
 */
@Composable
fun WavyRing(
    clockwise: Boolean,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    waves: Int = 22,
    amplitudeDp: Float = 5f,
    strokeDp: Float = 4f,
    periodMillis: Int = 16000,
) {
    val transition = rememberInfiniteTransition(label = "wavyRing")
    val rotation by transition.animateFloat(
        initialValue = 0f,
        targetValue = if (clockwise) 360f else -360f,
        animationSpec = infiniteRepeatable(tween(periodMillis, easing = LinearEasing), RepeatMode.Restart),
        label = "rotation",
    )
    Canvas(modifier.fillMaxSize()) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val amp = amplitudeDp.dp.toPx()
        val baseR = min(cx, cy) - amp - strokeDp.dp.toPx()
        val steps = 240
        val path = Path()
        for (i in 0..steps) {
            val t = i.toFloat() / steps
            val ang = t * 2.0 * PI
            val r = baseR + amp * sin(waves * ang).toFloat()
            val x = cx + r * cos(ang).toFloat()
            val y = cy + r * sin(ang).toFloat()
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.close()
        rotate(rotation, pivot = Offset(cx, cy)) {
            drawPath(path, color = color, style = Stroke(width = strokeDp.dp.toPx(), cap = StrokeCap.Round))
        }
    }
}

/** A small UNO-card-shaped tile rendering a label in a suit color (used for avatars / number cards). */
@Composable
fun SuitCardTile(
    label: String,
    suit: SuitColor,
    modifier: Modifier = Modifier,
    widthDp: Int = 34,
    heightDp: Int = 44,
) {
    Box(
        modifier = modifier
            .size(widthDp.dp, heightDp.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(suit.fill),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            color = suit.onFill,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )
    }
}
