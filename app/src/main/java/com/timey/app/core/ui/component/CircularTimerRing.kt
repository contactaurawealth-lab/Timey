package com.timey.app.core.ui.component

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.timey.app.core.ui.theme.EmeraldDragon
import com.timey.app.core.ui.theme.EmeraldLight
import com.timey.app.core.ui.theme.FieryAmber
import com.timey.app.core.ui.theme.FieryOrange
import com.timey.app.core.ui.theme.KomodoSurfaceVariant

@Composable
fun CircularTimerRing(
    progress: Float,
    isFocusMode: Boolean,
    isRunning: Boolean,
    size: Dp = 270.dp,
    strokeWidth: Dp = 14.dp,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing),
        label = "RingProgress"
    )

    // Breathing pulse for focus state
    val infiniteTransition = rememberInfiniteTransition(label = "PulseTransition")
    val pulseGlow by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = if (isRunning) 1.15f else 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "PulseGlow"
    )

    val primaryColor = if (isFocusMode) EmeraldDragon else FieryAmber
    val secondaryColor = if (isFocusMode) EmeraldLight else FieryOrange

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokePx = strokeWidth.toPx()
            val radius = (this.size.minDimension - strokePx) / 2
            val center = Offset(this.size.width / 2, this.size.height / 2)
            val arcSize = Size(radius * 2, radius * 2)
            val topLeft = Offset(center.x - radius, center.y - radius)

            // Background Track
            drawArc(
                color = KomodoSurfaceVariant,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokePx * 0.7f, cap = StrokeCap.Round)
            )

            // Dynamic Sweeping Progress Gradient
            if (animatedProgress > 0f) {
                drawArc(
                    brush = Brush.sweepGradient(
                        0.0f to primaryColor,
                        0.8f to secondaryColor,
                        1.0f to primaryColor,
                        center = center
                    ),
                    startAngle = -90f,
                    sweepAngle = 360f * animatedProgress,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokePx * pulseGlow, cap = StrokeCap.Round)
                )
            }
        }

        // Center Content (Time text + controls)
        content()
    }
}
