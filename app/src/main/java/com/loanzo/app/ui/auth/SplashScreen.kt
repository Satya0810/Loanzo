package com.loanzo.app.ui.auth

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.loanzo.app.R
import com.loanzo.app.ui.theme.*

/**
 * Premium Brand Splash Screen with Breathing Pulse Logo Animation.
 * Serves as the Session Gate while DataStore asynchronously resolves authentication status,
 * completely eliminating any login screen flicker.
 */
@Composable
fun SplashScreen(
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "splash_logo_pulse")

    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.20f,
        targetValue = 0.55f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow_alpha"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Navy900, Navy800, Navy900)
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(32.dp)
        ) {
            // Pulse Halo + Clean Logo
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(180.dp)
            ) {
                // Outer ambient pulse glow
                Box(
                    modifier = Modifier
                        .size(170.dp)
                        .scale(pulseScale * 1.08f)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    Gold500.copy(alpha = glowAlpha * 0.45f),
                                    Color(0xFF00B0FF).copy(alpha = glowAlpha * 0.25f),
                                    Color.Transparent
                                )
                            )
                        )
                )

                // The newly cutout, perfectly finished high-res logo
                Image(
                    painter = painterResource(id = R.drawable.app_logo),
                    contentDescription = "Loanzo Logo",
                    modifier = Modifier
                        .size(130.dp)
                        .scale(pulseScale)
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Brand Typography
            Text(
                text = "LOANZO",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 4.sp,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "PEER-TO-PEER CREDIT PROTOCOL",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp,
                color = Gold500
            )

            Spacer(modifier = Modifier.height(48.dp))

            // Subtle gold loading indicator
            CircularProgressIndicator(
                modifier = Modifier.size(28.dp),
                color = Gold500,
                strokeWidth = 2.5.dp,
                trackColor = SurfaceDarkCard
            )
        }

        // Bottom Security Footnote
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = SurfaceDarkElevated.copy(alpha = 0.7f),
            border = androidx.compose.foundation.BorderStroke(0.5.dp, GlassBorder),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 36.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "🔒 256-BIT ENCRYPTED & SECURE",
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Gray400,
                    letterSpacing = 1.sp
                )
            }
        }
    }
}
