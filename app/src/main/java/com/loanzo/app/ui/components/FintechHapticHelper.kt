package com.loanzo.app.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback

/**
 * Standardized tactile feedback controller tailored for fintech micro-interactions.
 */
class FintechHaptics(private val haptic: HapticFeedback) {

    /**
     * Subtle, snappy tick for tabs, filter chips, radio choices, and keypad digits.
     */
    fun lightTick() {
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
    }

    /**
     * Tick for interest rate or tenure slider thumb dragging.
     */
    fun sliderTick() {
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
    }

    /**
     * Firm, prominent vibration for financial commitments
     * (e.g. Slide-to-Confirm disbursement, Repayment submission, Biometric success).
     */
    fun confirmAction() {
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
    }

    /**
     * Alert vibration for validation errors or statutory limits violations
     * (e.g. State Usury Cap breach, Section 269SS cash limit exceeded).
     */
    fun warningBuzz() {
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
    }
}

/**
 * Remember and obtain a [FintechHaptics] instance in any Composable.
 */
@Composable
fun rememberFintechHaptics(): FintechHaptics {
    val haptic = LocalHapticFeedback.current
    return remember(haptic) { FintechHaptics(haptic) }
}

/**
 * Convenience modifier that performs a tactile tick upon click.
 */
fun Modifier.fintechClickable(
    haptics: FintechHaptics,
    isDestructiveOrMajor: Boolean = false,
    onClick: () -> Unit
): Modifier = this.clickable {
    if (isDestructiveOrMajor) {
        haptics.confirmAction()
    } else {
        haptics.lightTick()
    }
    onClick()
}
