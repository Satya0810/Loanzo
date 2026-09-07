package com.loanzo.app.ui.auth

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.loanzo.app.R
import com.loanzo.app.ui.theme.*

/**
 * Ultra-Clean, Catchy Opening Splash Animation.
 * Displays ONLY the authentic Loanzo logo in its natural shape (no circular clipping, no extra text, no clutter).
 */
@Composable
fun SplashScreen(
    modifier: Modifier = Modifier
) {
    // 1. Spring-loaded entrance scale & alpha
    val entranceScale = remember { Animatable(0.72f) }
    val entranceAlpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        entranceAlpha.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 400, easing = LinearOutSlowInEasing)
        )
    }

    LaunchedEffect(Unit) {
        entranceScale.animateTo(
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            )
        )
    }

    // 2. Ambient breathing gold aura behind logo
    val infiniteTransition = rememberInfiniteTransition(label = "splash_halo")

    val haloScale by infiniteTransition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "halo_scale"
    )

    val haloAlpha by infiniteTransition.animateFloat(
        initialValue = 0.22f,
        targetValue = 0.42f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "halo_alpha"
    )

    // Subtle breathing pulse for the logo
    val logoBreathing by infiniteTransition.animateFloat(
        initialValue = 0.99f,
        targetValue = 1.02f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "logo_breathing"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF0C1B33),
                        Color(0xFF040A12)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        // Soft golden ambient glow behind the logo
        Box(
            modifier = Modifier
                .size(240.dp)
                .scale(haloScale)
                .alpha(haloAlpha * entranceAlpha.value)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Gold500.copy(alpha = 0.45f),
                            GoldCoinAmber.copy(alpha = 0.15f),
                            Color.Transparent
                        )
                    )
                )
        )

        // Official Loanzo Logo in its authentic native shape (No circle shape clipping, no text below)
        Image(
            painter = painterResource(id = R.drawable.app_logo),
            contentDescription = "Loanzo Logo",
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .size(168.dp)
                .scale(entranceScale.value * logoBreathing)
                .alpha(entranceAlpha.value)
        )
    }
}
