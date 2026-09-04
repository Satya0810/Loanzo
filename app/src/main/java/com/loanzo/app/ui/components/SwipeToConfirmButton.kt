package com.loanzo.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.loanzo.app.ui.theme.*
import kotlin.math.roundToInt

/**
 * Modern Slide to Confirm / Pay component inspired by Cred, Revolut & Jupiter.
 * Prevents accidental taps in financial flows with a smooth, tactile drag gesture.
 */
@Composable
fun SwipeToConfirmButton(
    text: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    thumbColor: Color = Gold500,
    trackColors: List<Color> = listOf(Navy700, Navy800),
    activeTrackColor: Color = Gold500.copy(alpha = 0.25f),
    onConfirm: () -> Unit
) {
    val density = LocalDensity.current
    val haptic = LocalHapticFeedback.current
    var totalWidthPx by remember { mutableFloatStateOf(0f) }
    var dragOffsetPx by remember { mutableFloatStateOf(0f) }
    var isConfirmed by remember { mutableStateOf(false) }

    val thumbSizeDp = 50.dp
    val thumbSizePx = with(density) { thumbSizeDp.toPx() }
    val maxDragPx = (totalWidthPx - thumbSizePx).coerceAtLeast(0f)

    // Animated return if released before threshold
    val animatedOffsetPx by animateFloatAsState(
        targetValue = dragOffsetPx,
        animationSpec = spring(dampingRatio = 0.75f, stiffness = 400f),
        label = "dragOffset"
    )

    val progress = if (maxDragPx > 0) (animatedOffsetPx / maxDragPx).coerceIn(0f, 1f) else 0f

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(58.dp)
            .clip(RoundedCornerShape(29.dp))
            .background(
                Brush.horizontalGradient(trackColors)
            )
            .border(
                width = 1.dp,
                color = if (enabled) GlassBorder else Gray700.copy(alpha = 0.3f),
                shape = RoundedCornerShape(29.dp)
            )
            .onSizeChanged { totalWidthPx = it.width.toFloat() },
        contentAlignment = Alignment.CenterStart
    ) {
        // Active filled progress background behind the thumb
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(with(density) { (animatedOffsetPx + thumbSizePx / 2).toDp() })
                .clip(RoundedCornerShape(29.dp))
                .background(activeTrackColor)
        )

        // Centered Hint Text
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (isConfirmed) "Confirmed ✓" else text,
                color = if (isConfirmed) Emerald400 else Color.White.copy(alpha = (1f - progress * 0.8f).coerceIn(0.2f, 1f)),
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                letterSpacing = 0.5.sp
            )
        }

        // Draggable Thumb
        Box(
            modifier = Modifier
                .offset { IntOffset(animatedOffsetPx.roundToInt(), 0) }
                .padding(4.dp)
                .size(thumbSizeDp)
                .clip(CircleShape)
                .background(if (isConfirmed) Emerald400 else if (enabled) thumbColor else Gray600)
                .draggable(
                    enabled = enabled && !isConfirmed,
                    orientation = Orientation.Horizontal,
                    state = rememberDraggableState { delta ->
                        dragOffsetPx = (dragOffsetPx + delta).coerceIn(0f, maxDragPx)
                    },
                    onDragStopped = {
                        if (dragOffsetPx >= maxDragPx * 0.82f) {
                            // Completed slide!
                            dragOffsetPx = maxDragPx
                            isConfirmed = true
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onConfirm()
                        } else {
                            // Snap back
                            dragOffsetPx = 0f
                        }
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isConfirmed) Icons.Default.Check else Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = "Slide to confirm",
                tint = Navy900,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}
