package com.loanzo.app.ui.components

import android.os.SystemClock
import androidx.compose.animation.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Universal Action Button with integrated loading state and anti-duplicate click debouncing.
 * Prevents accidental double-submissions, duplicate loan creation, and duplicate bid triggers.
 */
@Composable
fun LoanzoActionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isLoading: Boolean = false,
    loadingText: String = "Processing...",
    enabled: Boolean = true,
    icon: ImageVector? = null,
    containerColor: Color = MaterialTheme.colorScheme.primary,
    contentColor: Color = Color.White,
    disabledContainerColor: Color = containerColor.copy(alpha = 0.5f),
    disabledContentColor: Color = contentColor.copy(alpha = 0.7f),
    shape: Shape = RoundedCornerShape(12.dp),
    height: Dp = 50.dp,
    fontSize: TextUnit = 14.sp,
    debounceMillis: Long = 800L
) {
    val haptic = LocalHapticFeedback.current
    var lastClickTime by remember { mutableStateOf(0L) }

    val isEffectivelyEnabled = enabled && !isLoading

    Button(
        onClick = {
            val currentTime = SystemClock.elapsedRealtime()
            if (currentTime - lastClickTime >= debounceMillis) {
                lastClickTime = currentTime
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onClick()
            }
        },
        enabled = isEffectivelyEnabled,
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor,
            disabledContainerColor = disabledContainerColor,
            disabledContentColor = disabledContentColor
        ),
        shape = shape,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        modifier = modifier
            .heightIn(min = height)
            .semantics { role = Role.Button }
    ) {
        AnimatedContent(
            targetState = isLoading,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "action_btn_loading"
        ) { loading ->
            if (loading) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = contentColor,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = loadingText,
                        fontSize = fontSize,
                        fontWeight = FontWeight.Bold,
                        color = contentColor,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    if (icon != null) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = contentColor
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(
                        text = text,
                        fontSize = fontSize,
                        fontWeight = FontWeight.Bold,
                        color = contentColor,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

/**
 * Modifier extension to debounce standard click events across any Composable.
 */
@Composable
fun Modifier.debouncedClickable(
    debounceMillis: Long = 800L,
    enabled: Boolean = true,
    onClick: () -> Unit
): Modifier {
    var lastClickTime by remember { mutableStateOf(0L) }
    val haptic = LocalHapticFeedback.current

    return this.clickable(
        enabled = enabled,
        interactionSource = remember { MutableInteractionSource() },
        indication = null
    ) {
        val currentTime = SystemClock.elapsedRealtime()
        if (currentTime - lastClickTime >= debounceMillis) {
            lastClickTime = currentTime
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            onClick()
        }
    }
}
