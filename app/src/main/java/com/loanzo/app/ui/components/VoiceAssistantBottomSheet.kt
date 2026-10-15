package com.loanzo.app.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.loanzo.app.data.voice.VoiceState
import com.loanzo.app.ui.theme.*

/**
 * Modern, clean FinTech AI Voice Assistant bottom sheet for Loanzo.
 * Designed with a light porcelain canvas, high-contrast typography, and Royal Blue brand accents.
 * Complies with WCAG 2.2 AAA guidelines:
 * - 56dp oversized touch targets for motor accessibility.
 * - LiveRegionMode.Polite TalkBack announcements for screen readers.
 * - High-contrast text on clean surfaces (eliminates dark obsidian / black sheet).
 * - Real-time animated soundwave visualizer.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun VoiceAssistantBottomSheet(
    voiceState: VoiceState,
    onDismiss: () -> Unit,
    onMicClick: () -> Unit,
    onStopSpeaking: () -> Unit,
    onReplayAudio: (() -> Unit)? = null,
    onNavigateToCalculator: (() -> Unit)? = null,
    onNavigateToKyc: (() -> Unit)? = null,
    onNavigateToMarketplace: (() -> Unit)? = null,
    onNavigateToPortfolio: (() -> Unit)? = null,
    onNavigateToCreatePost: (() -> Unit)? = null,
    onNavigateToSupport: (() -> Unit)? = null,
    onNavigateToNotifications: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current

    // Pulsing aura animation for active recording
    val infiniteTransition = rememberInfiniteTransition(label = "mic_pulse")
    val auraScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "aura_scale"
    )

    // Current TalkBack announcement string
    val currentAnnouncement = when (voiceState) {
        is VoiceState.Listening -> "Listening. Speak your question or command."
        is VoiceState.Thinking -> "Processing your request with Loanzo AI assistant."
        is VoiceState.Speaking -> "Assistant is reading response aloud."
        is VoiceState.Error -> "Notice: ${voiceState.message}"
        is VoiceState.Idle -> "Voice Assistant ready. Tap microphone to speak."
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = TextNavyDark,
        dragHandle = {
            BottomSheetDefaults.DragHandle(
                color = MaterialTheme.colorScheme.outlineVariant,
                width = 48.dp,
                height = 4.dp
            )
        },
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 36.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Live region accessibility announcer for screen readers (TalkBack)
            Box(
                modifier = Modifier
                    .size(0.dp)
                    .semantics {
                        liveRegion = LiveRegionMode.Polite
                        contentDescription = currentAnnouncement
                    }
            )

            // Header: Title & Close Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(BrandIceBlue),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.RecordVoiceOver,
                            contentDescription = null,
                            tint = BrandRoyalBlue,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        LoanzoText(
                            text = "Loanzo AI Voice Assistant",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextNavyDark
                        )
                        LoanzoText(
                            text = "Your In-App Financial Concierge",
                            fontSize = 12.sp,
                            color = BrandCobalt,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // Oversized 56dp close button for accessibility
                IconButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onDismiss()
                    },
                    modifier = Modifier
                        .size(56.dp)
                        .semantics {
                            role = Role.Button
                            contentDescription = "Close Voice Assistant"
                        }
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = null,
                        tint = TextSlateMedium,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // State Badge Pill
            val (badgeText, badgeBgColor, badgeBorderColor, badgeTextColor, badgeIcon) = when (voiceState) {
                is VoiceState.Listening -> Tuple5("LISTENING...", EmeraldLight, EmeraldBorder, Emerald600, Icons.Default.Mic)
                is VoiceState.Thinking -> Tuple5("THINKING...", BrandIceBlue, BrandIceBorder, BrandRoyalBlue, Icons.Default.AutoAwesome)
                is VoiceState.Speaking -> {
                    if (voiceState.isAudioPlaying) {
                        Tuple5("SPEAKING ALOUD...", Color(0xFFF3E8FF), Color(0xFFDDD6FE), Color(0xFF7C3AED), Icons.Default.VolumeUp)
                    } else {
                        Tuple5("ANSWER READY", EmeraldLight, EmeraldBorder, Emerald600, Icons.Default.CheckCircle)
                    }
                }
                is VoiceState.Error -> Tuple5("ATTENTION", RedLight, RedBorder, Red500, Icons.Default.Warning)
                is VoiceState.Idle -> Tuple5("READY TO LISTEN", BrandIceBlue, BrandIceBorder, BrandRoyalBlue, Icons.Default.RecordVoiceOver)
            }

            Surface(
                shape = RoundedCornerShape(20.dp),
                color = badgeBgColor,
                border = BorderStroke(1.dp, badgeBorderColor),
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(badgeIcon, contentDescription = null, tint = badgeTextColor, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    LoanzoText(
                        text = badgeText,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = badgeTextColor
                    )
                }
            }

            // Real-Time Soundwave Visualizer
            val currentRms = (voiceState as? VoiceState.Listening)?.rmsDb ?: 0f
            AudioWaveVisualizer(
                rmsDb = currentRms,
                isListening = voiceState is VoiceState.Listening,
                isSpeaking = (voiceState as? VoiceState.Speaking)?.isAudioPlaying == true,
                isThinking = voiceState is VoiceState.Thinking,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                maxBarHeight = 44.dp,
                minBarHeight = 8.dp
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Dynamic Content Display Area
            AnimatedContent(
                targetState = voiceState,
                transitionSpec = { fadeIn(tween(250)) togetherWith fadeOut(tween(200)) },
                label = "voice_content_transition"
            ) { state ->
                when (state) {
                    is VoiceState.Listening -> {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            LoanzoText(
                                text = state.partialText.ifBlank { "Listening... Speak naturally" },
                                fontSize = 18.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (state.partialText.isNotBlank()) TextNavyDark else TextSlateMuted,
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            LoanzoText(
                                text = "Try: \"What is my next EMI?\" or \"Show community loan requests\"",
                                fontSize = 12.sp,
                                color = BrandCobalt,
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    is VoiceState.Thinking -> {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            val displayPrompt = if (state.prompt.isNotBlank()) "\"${state.prompt}\"" else "Processing your question..."
                            LoanzoText(
                                text = displayPrompt,
                                fontSize = 16.sp,
                                color = TextNavyDark,
                                fontWeight = FontWeight.SemiBold,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            CircularProgressIndicator(
                                color = BrandRoyalBlue,
                                strokeWidth = 2.5.dp,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            LoanzoText(
                                text = "Analyzing account data & reasoning...",
                                fontSize = 12.sp,
                                color = TextSlateMuted
                            )
                        }
                    }

                    is VoiceState.Speaking -> {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = BrandIceBlue,
                                border = BorderStroke(1.dp, BrandIceBorder),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(18.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.VolumeUp, null, tint = BrandRoyalBlue, modifier = Modifier.size(18.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            LoanzoText(
                                                text = if (state.isAudioPlaying) "Speaking Aloud" else "Assistant Response",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = BrandRoyalBlue
                                            )
                                        }

                                        // Stop / Replay audio button
                                        if (state.isAudioPlaying) {
                                            TextButton(
                                                onClick = onStopSpeaking,
                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                            ) {
                                                Icon(Icons.Default.StopCircle, null, tint = Red500, modifier = Modifier.size(15.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                LoanzoText("Stop Audio", fontSize = 11.sp, color = Red500, fontWeight = FontWeight.Bold)
                                            }
                                        } else {
                                            TextButton(
                                                onClick = { onReplayAudio?.invoke() },
                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                            ) {
                                                Icon(Icons.Default.PlayCircle, null, tint = BrandRoyalBlue, modifier = Modifier.size(15.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                LoanzoText("Replay Audio", fontSize = 11.sp, color = BrandRoyalBlue, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    LoanzoText(
                                        text = state.displayText.ifBlank { state.spokenText },
                                        fontSize = 15.sp,
                                        lineHeight = 22.sp,
                                        color = TextNavyDark
                                    )
                                }
                            }

                            // Render Action Pills if extracted from tags (wrapped in FlowRow to prevent clipping)
                            val actions = state.actionTags
                            if (actions.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(16.dp))
                                FlowRow(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    actions.forEach { actionType ->
                                        when (actionType) {
                                            "CALCULATOR" -> {
                                                ActionNavigationPill(
                                                    label = "Open Loan Calculator",
                                                    icon = Icons.Default.Calculate,
                                                    onClick = {
                                                        onDismiss()
                                                        onNavigateToCalculator?.invoke()
                                                    }
                                                )
                                            }
                                            "KYC" -> {
                                                ActionNavigationPill(
                                                    label = "Verify KYC",
                                                    icon = Icons.Default.VerifiedUser,
                                                    onClick = {
                                                        onDismiss()
                                                        onNavigateToKyc?.invoke()
                                                    }
                                                )
                                            }
                                            "PORTFOLIO" -> {
                                                ActionNavigationPill(
                                                    label = "Smart Portfolio",
                                                    icon = Icons.Default.PieChart,
                                                    onClick = {
                                                        onDismiss()
                                                        onNavigateToPortfolio?.invoke()
                                                    }
                                                )
                                            }
                                            "MARKETPLACE" -> {
                                                ActionNavigationPill(
                                                    label = "Community Marketplace",
                                                    icon = Icons.Default.Groups,
                                                    onClick = {
                                                        onDismiss()
                                                        onNavigateToMarketplace?.invoke()
                                                    }
                                                )
                                            }
                                            "CREATE_POST" -> {
                                                ActionNavigationPill(
                                                    label = "Post Loan Request",
                                                    icon = Icons.Default.AddCircle,
                                                    onClick = {
                                                        onDismiss()
                                                        onNavigateToCreatePost?.invoke()
                                                    }
                                                )
                                            }
                                            "SUPPORT" -> {
                                                ActionNavigationPill(
                                                    label = "Support Help Desk",
                                                    icon = Icons.Default.SupportAgent,
                                                    onClick = {
                                                        onDismiss()
                                                        onNavigateToSupport?.invoke()
                                                    }
                                                )
                                            }
                                            "NOTIFICATIONS" -> {
                                                ActionNavigationPill(
                                                    label = "Alerts & Notifications",
                                                    icon = Icons.Default.Notifications,
                                                    onClick = {
                                                        onDismiss()
                                                        onNavigateToNotifications?.invoke()
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    is VoiceState.Error -> {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Default.ErrorOutline, null, tint = Red500, modifier = Modifier.size(36.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            LoanzoText(
                                text = state.message,
                                fontSize = 14.sp,
                                color = Red500,
                                fontWeight = FontWeight.Medium,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            LoanzoText(
                                text = "Tap the microphone below to try again.",
                                fontSize = 12.sp,
                                color = TextSlateMuted
                            )
                        }
                    }

                    is VoiceState.Idle -> {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            LoanzoText(
                                text = "Tap the microphone and ask anything.",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextNavyDark,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            LoanzoText(
                                text = "Ask about EMIs, loan status, or say \"Open calculator\"",
                                fontSize = 12.sp,
                                color = TextSlateMuted,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Oversized Accessible Microphone Button (64dp touch target)
            val isListening = voiceState is VoiceState.Listening
            Box(contentAlignment = Alignment.Center) {
                // Pulsing ambient ring when listening
                if (isListening) {
                    Box(
                        modifier = Modifier
                            .size(80.dp * auraScale)
                            .clip(CircleShape)
                            .background(Red500.copy(alpha = 0.15f))
                    )
                }

                Surface(
                    shape = CircleShape,
                    color = if (isListening) Red500 else BrandRoyalBlue,
                    border = BorderStroke(2.dp, if (isListening) Red400 else BrandCobalt),
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onMicClick()
                        }
                        .semantics {
                            role = Role.Button
                            contentDescription = if (isListening) "Stop Listening" else "Start Listening for Speech"
                        }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (isListening) Icons.Default.Stop else Icons.Default.Mic,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(30.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            LoanzoText(
                text = if (isListening) "Tap to finish listening" else "Tap to speak",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = TextSlateMuted
            )
        }
    }
}

@Composable
private fun ActionNavigationPill(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = Color.White,
        border = BorderStroke(1.dp, BrandIceBorder),
        modifier = Modifier
            .height(44.dp)
            .shadow(1.dp, RoundedCornerShape(24.dp))
            .clickable(onClick = onClick)
            .semantics {
                role = Role.Button
                contentDescription = "Navigate to $label"
            }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = BrandRoyalBlue, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            LoanzoText(
                text = label,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextNavyDark
            )
        }
    }
}

private data class Tuple5<A, B, C, D, E>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D,
    val fifth: E
)
