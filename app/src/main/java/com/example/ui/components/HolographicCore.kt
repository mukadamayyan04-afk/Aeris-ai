package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.domain.model.VoiceState
import com.example.ui.theme.CyanBright
import com.example.ui.theme.CyanDeep
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.CyanTeal
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.StatusError
import com.example.ui.theme.StatusListening
import com.example.ui.theme.StatusSpeaking
import com.example.ui.theme.StatusThinking
import com.example.ui.theme.TextMuted
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun HolographicCore(
    voiceState: VoiceState,
    rmsLevel: Float = 0f,
    modifier: Modifier = Modifier,
    size: Dp = 220.dp
) {
    val transition = rememberInfiniteTransition(label = "holo_rotation")

    val slowRotation by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 14000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "slow_rot"
    )

    val fastCounterRotation by transition.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 7000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "counter_rot"
    )

    val pulseScale by transition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    val coreColor = when (voiceState) {
        VoiceState.IDLE -> CyanNeon
        VoiceState.LISTENING -> StatusListening
        VoiceState.PROCESSING -> CyanBright
        VoiceState.THINKING -> StatusThinking
        VoiceState.RESPONDING -> GoldAccent
        VoiceState.SPEAKING -> StatusSpeaking
        VoiceState.OFFLINE -> TextMuted
        VoiceState.ERROR -> StatusError
    }

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(this.size.width / 2f, this.size.height / 2f)
            val maxRadius = (this.size.minDimension / 2f) * 0.92f

            // 1. Ambient Radial Glow Background
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        coreColor.copy(alpha = 0.28f * pulseScale),
                        coreColor.copy(alpha = 0.08f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = maxRadius
                ),
                radius = maxRadius,
                center = center
            )

            // 2. Outermost Reticle Ring with Dashes
            rotate(degrees = slowRotation, pivot = center) {
                drawCircle(
                    color = CyanDeep.copy(alpha = 0.6f),
                    radius = maxRadius * 0.95f,
                    center = center,
                    style = Stroke(
                        width = 2.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(18f, 14f), 0f)
                    )
                )

                // 4 Orbital Satellite Nodes
                for (i in 0..3) {
                    val angle = Math.toRadians((i * 90.0))
                    val nodeX = center.x + (maxRadius * 0.95f * cos(angle)).toFloat()
                    val nodeY = center.y + (maxRadius * 0.95f * sin(angle)).toFloat()
                    drawCircle(
                        color = coreColor,
                        radius = 3.5.dp.toPx(),
                        center = Offset(nodeX, nodeY)
                    )
                }
            }

            // 3. Middle Holographic Segmented Ring
            rotate(degrees = fastCounterRotation, pivot = center) {
                drawCircle(
                    color = CyanBright.copy(alpha = 0.75f),
                    radius = maxRadius * 0.76f,
                    center = center,
                    style = Stroke(
                        width = 3.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(35f, 25f, 15f, 25f), 0f),
                        cap = StrokeCap.Round
                    )
                )
            }

            // 4. Inner Wave Resonance Ring (dynamically scales with speech RMS level)
            val dynamicRmsScale = if (voiceState == VoiceState.LISTENING) {
                1f + (rmsLevel.coerceIn(0f, 10f) / 25f)
            } else pulseScale

            val innerRadius = (maxRadius * 0.52f) * dynamicRmsScale
            drawCircle(
                brush = Brush.sweepGradient(
                    colors = listOf(
                        coreColor.copy(alpha = 0.9f),
                        CyanTeal.copy(alpha = 0.5f),
                        coreColor.copy(alpha = 0.9f)
                    ),
                    center = center
                ),
                radius = innerRadius,
                center = center,
                style = Stroke(width = 2.5.dp.toPx())
            )

            // 5. Central AI Iris (Holographic Core)
            val irisRadius = maxRadius * 0.28f * (if (voiceState == VoiceState.THINKING) pulseScale else 1f)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.9f),
                        coreColor.copy(alpha = 0.85f),
                        coreColor.copy(alpha = 0.3f)
                    ),
                    center = center,
                    radius = irisRadius
                ),
                radius = irisRadius,
                center = center
            )

            // Crosshair markers in the center
            val crosshairLen = 14.dp.toPx()
            drawLine(
                color = coreColor.copy(alpha = 0.9f),
                start = Offset(center.x - crosshairLen, center.y),
                end = Offset(center.x + crosshairLen, center.y),
                strokeWidth = 1.5.dp.toPx()
            )
            drawLine(
                color = coreColor.copy(alpha = 0.9f),
                start = Offset(center.x, center.y - crosshairLen),
                end = Offset(center.x, center.y + crosshairLen),
                strokeWidth = 1.5.dp.toPx()
            )
        }
    }
}
