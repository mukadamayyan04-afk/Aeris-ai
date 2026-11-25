package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.CyanBright
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.ElectricBlue
import kotlin.math.sin

@Composable
fun WaveformVisualizer(
    isActive: Boolean = true,
    amplitudeMultiplier: Float = 1.0f,
    modifier: Modifier = Modifier,
    height: Dp = 26.dp,
    barCount: Int = 36
) {
    val transition = rememberInfiniteTransition(label = "waveform_anim")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 6.28318f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
    ) {
        val width = size.width
        val midY = size.height / 2f
        val step = width / barCount.toFloat()

        for (i in 0 until barCount) {
            val x = i * step + step / 2f
            val normalizedX = (i / barCount.toFloat()) * 3.14159f * 2.5f

            val wave = if (isActive) {
                (sin(normalizedX + phase) * 0.5f + sin(normalizedX * 1.5f - phase * 0.8f) * 0.35f + 0.15f)
            } else {
                0.15f
            }

            val barHeight = (midY * 0.85f * wave * amplitudeMultiplier).coerceIn(2.dp.toPx(), midY * 0.95f)

            drawLine(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        CyanNeon,
                        ElectricBlue,
                        CyanBright
                    ),
                    startY = midY - barHeight,
                    endY = midY + barHeight
                ),
                start = Offset(x, midY - barHeight),
                end = Offset(x, midY + barHeight),
                strokeWidth = (step * 0.45f).coerceIn(1.5f, 4f),
                cap = StrokeCap.Round
            )
        }
    }
}
