package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import android.provider.Settings
import androidx.compose.ui.platform.LocalContext
import com.example.R
import com.example.domain.model.VoiceState
import com.example.ui.theme.AlertRed
import com.example.ui.theme.BlueGlow
import com.example.ui.theme.CyanBright
import com.example.ui.theme.CyanGlow
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.CyanTeal
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.HoloBlack
import com.example.ui.theme.HoloPurple
import com.example.ui.theme.HoloPurpleBright
import com.example.ui.theme.HoloPurpleDeep
import com.example.ui.theme.HoloPurpleGlow
import com.example.ui.theme.StatusListening
import com.example.ui.theme.StatusSpeaking
import com.example.ui.theme.TextMuted
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Original female digital holographic AI assistant avatar component (Stage 3).
 * Explicitly differentiates all operational states:
 * IDLE, LISTENING, PROCESSING, THINKING, RESPONDING, SPEAKING, OFFLINE, ERROR.
 * Pauses animation work cleanly when the lifecycle pauses to preserve battery on Redmi 13 5G.
 */
@Composable
fun HolographicAvatar(
    voiceState: VoiceState,
    rmsLevel: Float = 0f,
    modifier: Modifier = Modifier,
    size: Dp = 200.dp,
    showHaloRings: Boolean = true,
    animationsEnabled: Boolean = true
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var isLifecycleActive by remember { mutableStateOf(true) }
    val systemAnimationsActive = remember(context) {
        try {
            Settings.Global.getFloat(
                context.contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE,
                1f
            ) > 0f
        } catch (_: Exception) {
            true
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> isLifecycleActive = true
                Lifecycle.Event.ON_PAUSE, Lifecycle.Event.ON_STOP -> isLifecycleActive = false
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val animateNow = animationsEnabled && isLifecycleActive && systemAnimationsActive

    val transition = rememberInfiniteTransition(label = "avatar_holo_anim")

    val slowRotation by transition.animateFloat(
        initialValue = 0f,
        targetValue = if (animateNow) 360f else 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = when (voiceState) {
                    VoiceState.THINKING -> 7500
                    VoiceState.RESPONDING -> 9500
                    else -> 16000
                },
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "slow_halo_rot"
    )

    val fastCounterRotation by transition.animateFloat(
        initialValue = if (animateNow) 360f else 0f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "fast_counter_rot"
    )

    val breathingScale by transition.animateFloat(
        initialValue = 0.96f,
        targetValue = if (animateNow) 1.04f else 0.98f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "avatar_breathing"
    )

    val microFloatY by transition.animateFloat(
        initialValue = -2.5f,
        targetValue = if (animateNow) 2.5f else 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "head_micro_float"
    )

    val blinkPulse by transition.animateFloat(
        initialValue = 0f,
        targetValue = if (animateNow) 1f else 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "blink_cycle"
    )

    val targetAuraColor = when (voiceState) {
        VoiceState.IDLE -> CyanNeon
        VoiceState.LISTENING -> StatusListening
        VoiceState.PROCESSING -> CyanBright
        VoiceState.THINKING -> ElectricBlue
        VoiceState.RESPONDING -> GoldAccent
        VoiceState.SPEAKING -> StatusSpeaking
        VoiceState.OFFLINE -> TextMuted
        VoiceState.ERROR -> AlertRed
    }
    val auraColor by animateColorAsState(
        targetValue = targetAuraColor,
        animationSpec = tween(durationMillis = 350),
        label = "aura_color"
    )

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        // 1. Holographic Reticles & Vocalization Rings
        if (showHaloRings) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val center = Offset(this.size.width / 2f, this.size.height / 2f)
                val maxRadius = (this.size.minDimension / 2f) * 0.98f

                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            auraColor.copy(alpha = if (voiceState == VoiceState.OFFLINE) 0.14f else 0.36f * breathingScale),
                            HoloPurpleGlow.copy(alpha = if (voiceState == VoiceState.OFFLINE) 0.05f else 0.22f),
                            ElectricBlue.copy(alpha = if (voiceState == VoiceState.OFFLINE) 0.04f else 0.12f),
                            Color.Transparent
                        ),
                        center = center,
                        radius = maxRadius
                    ),
                    radius = maxRadius,
                    center = center
                )

                rotate(degrees = slowRotation, pivot = center) {
                    drawCircle(
                        brush = Brush.sweepGradient(
                            colors = if (voiceState == VoiceState.ERROR) {
                                listOf(AlertRed.copy(alpha = 0.7f), HoloPurpleDeep.copy(alpha = 0.6f), AlertRed.copy(alpha = 0.7f))
                            } else {
                                listOf(CyanNeon.copy(alpha = 0.7f), HoloPurpleBright.copy(alpha = 0.75f), CyanTeal.copy(alpha = 0.7f))
                            },
                            center = center
                        ),
                        radius = maxRadius * 0.94f,
                        center = center,
                        style = Stroke(
                            width = 2.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(24f, 16f, 8f, 16f), 0f)
                        )
                    )

                    for (i in 0..3) {
                        val angle = Math.toRadians(i * 90.0)
                        val nodeX = center.x + (maxRadius * 0.94f * cos(angle)).toFloat()
                        val nodeY = center.y + (maxRadius * 0.94f * sin(angle)).toFloat()
                        drawCircle(
                            color = if (i % 2 == 0) auraColor else HoloPurpleBright,
                            radius = 3.5.dp.toPx(),
                            center = Offset(nodeX, nodeY)
                        )
                    }
                }

                rotate(degrees = fastCounterRotation, pivot = center) {
                    drawCircle(
                        color = HoloPurple.copy(alpha = if (voiceState == VoiceState.OFFLINE) 0.25f else 0.72f),
                        radius = maxRadius * 0.85f,
                        center = center,
                        style = Stroke(
                            width = 2.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(38f, 22f), 0f),
                            cap = StrokeCap.Round
                        )
                    )
                }

                val dynamicScale = when (voiceState) {
                    VoiceState.LISTENING -> 1f + (rmsLevel.coerceIn(0f, 10f) / 22f)
                    VoiceState.SPEAKING -> 1f + ((0.35f + (rmsLevel.coerceIn(0f, 8f) / 14f)) * 0.12f)
                    VoiceState.RESPONDING -> breathingScale * 1.03f
                    else -> breathingScale
                }

                drawCircle(
                    color = auraColor.copy(alpha = 0.82f),
                    radius = maxRadius * 0.74f * dynamicScale,
                    center = center,
                    style = Stroke(width = 1.6.dp.toPx())
                )
            }
        }

        // 2. Central Female Holographic Core Image with Natural Motion & Expression Resonance
        Box(
            modifier = Modifier
                .offset { IntOffset(0, microFloatY.roundToInt()) }
                .size(size * 0.70f)
                .clip(CircleShape)
                .background(HoloBlack)
                .border(
                    width = 2.dp,
                    brush = Brush.linearGradient(
                        listOf(
                            auraColor,
                            if (voiceState == VoiceState.OFFLINE) TextMuted else HoloPurpleBright,
                            if (voiceState == VoiceState.OFFLINE) TextMuted else ElectricBlue
                        )
                    ),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.aeris_avatar),
                contentDescription = "AERIS AI Holographic Avatar (${voiceState.label})",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            // Scanline + periodic holographic blink / expression glow
            val isBlinking = animateNow && blinkPulse in 0.94f..0.985f
            val scanAlpha = when {
                isBlinking -> 0.42f
                voiceState == VoiceState.THINKING -> 0.28f
                voiceState == VoiceState.OFFLINE -> 0.55f
                else -> 0.16f
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                auraColor.copy(alpha = scanAlpha),
                                Color.Transparent,
                                BlueGlow.copy(alpha = if (voiceState == VoiceState.OFFLINE) 0.45f else 0.22f)
                            )
                        )
                    )
            )

            // Speech vocalization equalizer bar at lower visor rim when SPEAKING or RESPONDING
            if (voiceState == VoiceState.SPEAKING || voiceState == VoiceState.RESPONDING) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val bars = 7
                    val centerX = this.size.width / 2f
                    val baseY = this.size.height * 0.84f
                    val spacing = 5.dp.toPx()
                    val startX = centerX - ((bars - 1) * spacing / 2f)
                    for (idx in 0 until bars) {
                        val factor = if (voiceState == VoiceState.SPEAKING) {
                            (0.45f + 0.55f * kotlin.math.abs(sin(blinkPulse * 24f + idx * 0.9f)))
                        } else {
                            0.35f + 0.25f * kotlin.math.abs(cos(blinkPulse * 14f + idx))
                        }
                        val barH = (12.dp.toPx() * factor).coerceAtLeast(3.dp.toPx())
                        val x = startX + idx * spacing
                        drawLine(
                            color = auraColor.copy(alpha = 0.9f),
                            start = Offset(x, baseY - barH / 2f),
                            end = Offset(x, baseY + barH / 2f),
                            strokeWidth = 2.2.dp.toPx(),
                            cap = StrokeCap.Round
                        )
                    }
                }
            }
        }
    }
}
