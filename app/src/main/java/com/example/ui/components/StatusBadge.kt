package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.domain.model.VoiceState
import com.example.ui.theme.CyanBright
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.HoloBlack
import com.example.ui.theme.StatusError
import com.example.ui.theme.StatusListening
import com.example.ui.theme.StatusSpeaking
import com.example.ui.theme.StatusThinking
import com.example.ui.theme.TextMuted
import com.example.ui.theme.Typography

@Composable
fun StatusBadge(
    voiceState: VoiceState,
    modifier: Modifier = Modifier,
    customLabel: String? = null
) {
    val transition = rememberInfiniteTransition(label = "pulse")
    val alphaAnim by transition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    val (badgeColor, label) = when (voiceState) {
        VoiceState.IDLE -> CyanNeon to "AERIS • READY"
        VoiceState.LISTENING -> StatusListening to "AERIS • LISTENING"
        VoiceState.PROCESSING -> CyanBright to "AERIS • RECOGNIZING"
        VoiceState.THINKING -> StatusThinking to "AERIS • PROCESSING"
        VoiceState.RESPONDING -> CyanBright to "AERIS • RESPONDING"
        VoiceState.SPEAKING -> StatusSpeaking to "AERIS • VOCALIZING"
        VoiceState.OFFLINE -> TextMuted to "AERIS • OFFLINE"
        VoiceState.ERROR -> StatusError to "AERIS • FAULT"
    }

    val displayLabel = customLabel ?: label

    Row(
        modifier = modifier
            .border(
                width = 1.dp,
                color = badgeColor.copy(alpha = 0.5f),
                shape = RoundedCornerShape(16.dp)
            )
            .background(
                color = HoloBlack.copy(alpha = 0.8f),
                shape = RoundedCornerShape(16.dp)
            )
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .alpha(if (voiceState != VoiceState.IDLE) alphaAnim else 0.85f)
                .background(badgeColor, CircleShape)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = displayLabel,
            style = Typography.labelSmall,
            color = badgeColor
        )
    }
}
