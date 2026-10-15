package com.loanzo.app.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.loanzo.app.ui.theme.Emerald400
import com.loanzo.app.ui.theme.Gold500
import com.loanzo.app.ui.theme.GoldCoinBright
import kotlin.math.sin

/**
 * Accessible, dynamic 7-bar audio soundwave visualizer.
 * Renders smooth animated audio bars modulated by real-time speech RMS dB or rhythmic speaking cadence.
 * Semantics are cleared so screen readers (TalkBack) are not inundated with visual animation events.
 */
@Composable
fun AudioWaveVisualizer(
    rmsDb: Float,
    isListening: Boolean,
    isSpeaking: Boolean,
    isThinking: Boolean = false,
    modifier: Modifier = Modifier,
    barCount: Int = 7,
    maxBarHeight: Dp = 48.dp,
    minBarHeight: Dp = 8.dp
) {
    // Infinite transition for ambient rhythmic animations
    val infiniteTransition = rememberInfiniteTransition(label = "audio_wave_infinite")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave_phase"
    )

    // Smoothly animate the incoming RMS volume level to avoid jitter
    val animatedRms by animateFloatAsState(
        targetValue = if (isListening) rmsDb.coerceIn(0f, 10f) else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "animated_rms"
    )

    val barGradient = Brush.verticalGradient(
        colors = listOf(
            com.loanzo.app.ui.theme.BrandCobalt,
            com.loanzo.app.ui.theme.BrandRoyalBlue,
            com.loanzo.app.ui.theme.Emerald500
        )
    )

    Row(
        modifier = modifier
            .clearAndSetSemantics { /* Visual-only element, accessibility status handled by parent container */ },
        horizontalArrangement = Arrangement.spacedBy(5.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val middleIndex = barCount / 2

        for (i in 0 until barCount) {
            // Distance from center bar (0 to middleIndex)
            val distFromCenter = kotlin.math.abs(i - middleIndex)
            val centerWeight = 1f - (distFromCenter.toFloat() / (middleIndex + 1)) * 0.4f

            val heightFraction = when {
                isListening -> {
                    // Combine RMS input with subtle sinusoidal movement
                    val microWave = (sin(phase + (i * 0.8f)) * 0.15f)
                    val rmsFactor = (animatedRms / 10f) * 0.85f
                    (0.2f + (rmsFactor + microWave) * centerWeight).coerceIn(0.12f, 1.0f)
                }
                isSpeaking -> {
                    // Rhythmic, energetic speaking wave cadence
                    val cadence = (sin(phase * 1.5f + (i * 0.9f)) * 0.45f) + 0.55f
                    (cadence * centerWeight).coerceIn(0.2f, 0.95f)
                }
                isThinking -> {
                    // Rolling wave pulse while LLM is generating
                    val pulse = (sin(phase * 2f + (i * 0.6f)) * 0.35f) + 0.45f
                    pulse.coerceIn(0.15f, 0.8f)
                }
                else -> {
                    // Idle resting state
                    0.15f
                }
            }

            val barHeight = minBarHeight + (maxBarHeight - minBarHeight) * heightFraction

            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(barHeight)
                    .clip(RoundedCornerShape(3.dp))
                    .background(barGradient)
            )
        }
    }
}
