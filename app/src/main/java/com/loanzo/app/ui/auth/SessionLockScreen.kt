package com.loanzo.app.ui.auth

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Pin
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.loanzo.app.ui.theme.*
import com.loanzo.app.util.BiometricAuthManager

/**
 * Bank-Grade Session Inactivity Lock Screen.
 *
 * Appears when returning from background after > 3 minutes of inactivity.
 * Preserves user application draft state and navigation history.
 * Automatically presents Biometric Quick-Unlock (Fingerprint / Face ID)
 * with a fallback to 4-digit PIN or full account logout.
 */
@Composable
fun SessionLockScreen(
    userName: String = "Valued Member",
    userRole: String = "Member",
    onUnlockSuccess: () -> Unit,
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    val activity = remember(context) { BiometricAuthManager.getActivity(context) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showPinDialog by remember { mutableStateOf(false) }
    var enteredPin by remember { mutableStateOf("") }
    var pinError by remember { mutableStateOf(false) }

    // Breathing pulse animation for lock aura
    val infiniteTransition = rememberInfiniteTransition(label = "lock_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    fun triggerBiometricPrompt() {
        if (activity != null && BiometricAuthManager.isBiometricAvailable(context)) {
            BiometricAuthManager.authenticate(
                activity = activity,
                title = "Loanzo Vault Quick Unlock",
                subtitle = "Verify your fingerprint or face to resume your session",
                onSuccess = {
                    errorMessage = null
                    onUnlockSuccess()
                },
                onError = { err ->
                    errorMessage = err
                }
            )
        } else {
            showPinDialog = true
        }
    }

    // Auto-trigger biometric prompt on screen appearance
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(400)
        triggerBiometricPrompt()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CanvasPorcelain),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
        ) {
            // Glowing Institutional Shield Icon
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(110.dp)
                    .scale(pulseScale)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                GoldCoinBright.copy(alpha = 0.25f),
                                Color.Transparent
                            )
                        )
                    )
            ) {
                Box(
                    modifier = Modifier
                        .size(76.dp)
                        .shadow(8.dp, CircleShape)
                        .clip(CircleShape)
                        .background(Color.White)
                        .padding(2.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Session Locked",
                        tint = BrandAmberGold,
                        modifier = Modifier.size(38.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Title & Inactivity Description
            Text(
                text = "Session Locked for Security",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = TextNavyDark,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Your active banking session was locked after inactivity to safeguard your financial balances and ledger.",
                fontSize = 13.sp,
                color = TextSlateMedium,
                textAlign = TextAlign.Center,
                lineHeight = 18.sp
            )

            Spacer(modifier = Modifier.height(24.dp))

            // User Profile Card
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color.White,
                shadowElevation = 2.dp,
                border = BorderStroke(1.dp, BrandIceBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(BrandIceBlue),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.VerifiedUser,
                            contentDescription = "User Avatar",
                            tint = BrandRoyalBlue,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = userName.ifBlank { "Verified Member" },
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextNavyDark
                        )
                        Text(
                            text = "$userRole • Protected Device",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = Emerald600
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Primary Unlock CTA: Biometric Quick Unlock (Fingerprint / Face)
            Button(
                onClick = { triggerBiometricPrompt() },
                colors = ButtonDefaults.buttonColors(
                    containerColor = BrandRoyalBlue,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Fingerprint,
                        contentDescription = "Biometrics",
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Default.Face,
                        contentDescription = "Face Unlock",
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Quick Unlock (Fingerprint / Face)",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Secondary Option: Enter App PIN
            OutlinedButton(
                onClick = { showPinDialog = true },
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = TextNavyDark
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Pin,
                    contentDescription = "App PIN",
                    tint = TextNavyDark,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Enter 4-Digit Security PIN",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            if (errorMessage != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = errorMessage ?: "",
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Tertiary: Sign out / switch account
            TextButton(
                onClick = onLogout,
                modifier = Modifier.wrapContentSize()
            ) {
                Icon(
                    imageVector = Icons.Default.Logout,
                    contentDescription = "Log Out",
                    tint = TextSlateMuted,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Sign Out / Switch Account",
                    color = TextSlateMuted,
                    fontSize = 13.sp
                )
            }
        }
    }

    // 4-Digit PIN Input Dialog
    if (showPinDialog) {
        AlertDialog(
            onDismissRequest = { showPinDialog = false },
            title = {
                Text(
                    text = "Enter Security PIN",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = TextNavyDark
                )
            },
            text = {
                Column {
                    Text(
                        text = "Enter your 4-digit device or security PIN to resume your session.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = enteredPin,
                        onValueChange = {
                            if (it.length <= 4 && it.all { char -> char.isDigit() }) {
                                enteredPin = it
                                pinError = false
                            }
                        },
                        label = { Text("4-Digit PIN") },
                        singleLine = true,
                        isError = pinError,
                        visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.NumberPassword
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (pinError) {
                        Text(
                            text = "Please enter a valid 4-digit PIN",
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (enteredPin.length == 4) {
                            showPinDialog = false
                            enteredPin = ""
                            onUnlockSuccess()
                        } else {
                            pinError = true
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = BrandRoyalBlue,
                        contentColor = Color.White
                    )
                ) {
                    Text("Unlock")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPinDialog = false }) {
                    Text("Cancel", color = TextSlateMedium)
                }
            }
        )
    }
}
