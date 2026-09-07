package com.loanzo.app.ui.auth

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.loanzo.app.R
import com.loanzo.app.ui.theme.*
import kotlinx.coroutines.delay

/**
 * Cinematic High-Impact Logo Animation Splash Screen.
 * Features:
 * - Spring-loaded entrance animation with elastic bounce.
 * - Dual counter-rotating golden orbital rings.
 * - Multi-layer pulsating sonar aura / radar waves.
 * - Expanding golden divider rule with glowing crest.
 * - Progressive multi-stage system security verification badge (3.6s duration).
 */
@Composable
fun SplashScreen(
    modifier: Modifier = Modifier
) {
    // ── 1. Spring-loaded entrance animation ─────────────────────────────
    val entranceScale = remember { Animatable(0.4f) }
    val entranceAlpha = remember { Animatable(0f) }
    val textAlpha = remember { Animatable(0f) }
    val dividerWidth = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        // Step 1: Rapid spring pop for logo
        entranceScale.animateTo(
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            )
        )
    }

    LaunchedEffect(Unit) {
        entranceAlpha.animateTo(1f, animationSpec = tween(600, easing = LinearEasing))
        delay(400)
        textAlpha.animateTo(1f, animationSpec = tween(800, easing = FastOutSlowInEasing))
        dividerWidth.animateTo(140f, animationSpec = tween(1000, easing = FastOutSlowInEasing))
    }

    // ── 2. Continuous ambient orbital & pulse loops ──────────────────────
    val infiniteTransition = rememberInfiniteTransition(label = "splash_loops")

    // Slow clockwise orbit
    val orbitRotationClockwise by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 10000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "orbit_cw"
    )

    // Faster counter-clockwise orbit
    val orbitRotationCounter by infiniteTransition.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 7000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "orbit_ccw"
    )

    // Breathing pulse for logo
    val breathingPulse by infiniteTransition.animateFloat(
        initialValue = 0.97f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breathing_pulse"
    )

    // Sonar glow ripple expanding
    val rippleScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.55f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ripple_scale"
    )

    val rippleAlpha by infiniteTransition.animateFloat(
        initialValue = 0.55f,
        targetValue = 0.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ripple_alpha"
    )

    // ── 3. Multi-stage system verification state ─────────────────────────
    var statusStage by remember { mutableStateOf(0) }
    var progressPercent by remember { mutableFloatStateOf(0.15f) }

    LaunchedEffect(Unit) {
        // Stage 0 (0ms - 1100ms): Connecting
        progressPercent = 0.35f
        delay(1100)
        // Stage 1 (1100ms - 2200ms): Hardware Vault
        statusStage = 1
        progressPercent = 0.70f
        delay(1100)
        // Stage 2 (2200ms - 3600ms): Verification complete
        statusStage = 2
        progressPercent = 1.0f
    }

    val statusText = when (statusStage) {
        0 -> "Auditing Hardware Cryptographic Signature..."
        1 -> "Validating Bank-Grade Session Vault & Biometrics..."
        else -> "Security Clearance Granted — Pre-warmed & Secure"
    }

    val statusColor = when (statusStage) {
        0 -> Gold500
        1 -> GoldCoinAmber
        else -> Emerald400
    }

    // ── UI Layout ────────────────────────────────────────────────────────
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF0F223D),
                        Color(0xFF0A1628),
                        Color(0xFF050B14)
                    ),
                    center = Offset.Unspecified,
                    radius = 1400f
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        // Subtle ambient gold background glow
        Box(
            modifier = Modifier
                .size(340.dp)
                .scale(breathingPulse)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Gold500.copy(alpha = 0.12f),
                            Color.Transparent
                        )
                    )
                )
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 24.dp)
        ) {
            // ── Centerpiece: Multi-Ring Holographic Logo Cluster ───────────
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(240.dp)
                    .scale(entranceScale.value)
                    .alpha(entranceAlpha.value)
            ) {
                // Expanding Sonar Ripple Wave
                Canvas(
                    modifier = Modifier
                        .size(190.dp)
                        .scale(rippleScale)
                        .alpha(rippleAlpha)
                ) {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(Gold500.copy(alpha = 0.35f), Color.Transparent)
                        ),
                        radius = size.minDimension / 2
                    )
                    drawCircle(
                        color = Gold500,
                        radius = size.minDimension / 2,
                        style = Stroke(width = 2.dp.toPx())
                    )
                }

                // Outer Orbital Track 1 (Clockwise Sweep)
                Canvas(
                    modifier = Modifier
                        .size(220.dp)
                        .rotate(orbitRotationClockwise)
                ) {
                    drawCircle(
                        brush = Brush.sweepGradient(
                            listOf(
                                Gold500.copy(alpha = 0.7f),
                                Color.Transparent,
                                Gold500.copy(alpha = 0.15f),
                                Color.Transparent,
                                Gold500.copy(alpha = 0.7f)
                            )
                        ),
                        radius = size.minDimension / 2,
                        style = Stroke(
                            width = 1.8.dp.toPx(),
                            cap = StrokeCap.Round
                        )
                    )
                    // Satellite indicator dot
                    drawCircle(
                        color = Gold500,
                        radius = 4.dp.toPx(),
                        center = Offset(size.width / 2, 0f)
                    )
                }

                // Inner Orbital Track 2 (Counter-Clockwise Sweep)
                Canvas(
                    modifier = Modifier
                        .size(185.dp)
                        .rotate(orbitRotationCounter)
                ) {
                    drawCircle(
                        brush = Brush.sweepGradient(
                            listOf(
                                Emerald400.copy(alpha = 0.6f),
                                Color.Transparent,
                                Gold500.copy(alpha = 0.4f),
                                Color.Transparent,
                                Emerald400.copy(alpha = 0.6f)
                            )
                        ),
                        radius = size.minDimension / 2,
                        style = Stroke(
                            width = 1.2.dp.toPx(),
                            cap = StrokeCap.Round
                        )
                    )
                    // Secondary indicator dot
                    drawCircle(
                        color = Emerald400,
                        radius = 3.dp.toPx(),
                        center = Offset(size.width, size.height / 2)
                    )
                }

                // Solid Circular Backplate for Contrast
                Surface(
                    shape = CircleShape,
                    color = Color(0xFF091424),
                    border = BorderStroke(2.dp, Brush.linearGradient(listOf(Gold500, GoldCoinAmber))),
                    shadowElevation = 18.dp,
                    modifier = Modifier
                        .size(142.dp)
                        .scale(breathingPulse)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        // High-Resolution Official Loanzo Logo
                        Image(
                            painter = painterResource(id = R.drawable.app_logo),
                            contentDescription = "Loanzo Logo",
                            modifier = Modifier
                                .size(118.dp)
                                .padding(6.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(30.dp))

            // ── Brand Title with Tracking & Gradient ──────────────────────
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.alpha(textAlpha.value)
            ) {
                Text(
                    text = "L O A N Z O",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 32.sp,
                    letterSpacing = 6.sp,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Expanding Golden Rule Line with Center Gem
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.width(dividerWidth.value.dp)
                ) {
                    HorizontalDivider(
                        color = Gold500.copy(alpha = 0.65f),
                        thickness = 1.5.dp
                    )
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .rotate(45f)
                            .background(Gold500)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "PEER-TO-PEER CREDIT PROTOCOL",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    letterSpacing = 2.5.sp,
                    color = Gold500
                )
            }

            Spacer(modifier = Modifier.height(44.dp))

            // ── Live Security Verification Pill (Animated Content) ────────
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = Navy800.copy(alpha = 0.85f),
                border = BorderStroke(1.dp, statusColor.copy(alpha = 0.45f)),
                modifier = Modifier
                    .alpha(textAlpha.value)
                    .padding(horizontal = 16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    if (statusStage < 2) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(14.dp),
                            color = statusColor,
                            strokeWidth = 2.dp,
                            trackColor = Color.White.copy(alpha = 0.1f)
                        )
                    } else {
                        Icon(
                            Icons.Default.VerifiedUser,
                            contentDescription = null,
                            tint = Emerald400,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    AnimatedContent(
                        targetState = statusText,
                        transitionSpec = { fadeIn(tween(300)) togetherWith fadeOut(tween(300)) },
                        label = "status_anim"
                    ) { text ->
                        Text(
                            text = text,
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White.copy(alpha = 0.9f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Smooth linear progress bar (0% -> 100% across 3.6 seconds)
            val animatedProgress by animateFloatAsState(
                targetValue = progressPercent,
                animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
                label = "progress_anim"
            )

            Box(
                modifier = Modifier
                    .width(180.dp)
                    .height(3.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color.White.copy(alpha = 0.12f))
                    .alpha(textAlpha.value)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(animatedProgress)
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(Gold500, GoldCoinAmber, Emerald400)
                            )
                        )
                )
            }
        }

        // ── Bottom Security & Regulatory Footnote ─────────────────────────
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color(0xFF060F1A).copy(alpha = 0.85f),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.06f)),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp)
                .alpha(textAlpha.value)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Icon(
                    Icons.Default.Shield,
                    contentDescription = null,
                    tint = Gold500,
                    modifier = Modifier.size(12.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "RBI P2P COMPLIANT • 256-BIT HARDWARE VAULT",
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 9.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = Gray400,
                    letterSpacing = 1.sp
                )
            }
        }
    }
}
