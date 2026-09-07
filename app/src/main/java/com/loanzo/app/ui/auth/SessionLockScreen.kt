package com.loanzo.app.ui.auth

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF070F1E),
                        Color(0xFF0F1B2E),
                        Color(0xFF050B14)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 28.dp)
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
                                Gold500.copy(alpha = 0.22f),
                                Color.Transparent
                            )
                        )
                    )
            ) {
                Box(
                    modifier = Modifier
                        .size(76.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF13233D))
                        .padding(2.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Session Locked",
                        tint = GoldCoinAmber,
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
                color = Color.White,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Your active banking session was locked after inactivity to safeguard your financial balances and ledger.",
                fontSize = 13.sp,
                color = Color(0xFF94A3B8),
                textAlign = TextAlign.Center,
                lineHeight = 18.sp
            )

            Spacer(modifier = Modifier.height(24.dp))

            // User Profile Pill
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFF14243B),
                border = BorderStroke(1.dp, Color(0xFF2A3F60)),
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
                            .background(Gold500.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.VerifiedUser,
                            contentDescription = "User Avatar",
                            tint = GoldCoinAmber,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = userName.ifBlank { "Verified Member" },
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                        Text(
                            text = "$userRole • Protected Device",
                            fontSize = 12.sp,
                            color = Emerald400
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Primary Unlock CTA: Biometric Quick Unlock
            Button(
                onClick = { triggerBiometricPrompt() },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Gold500,
                    contentColor = Navy900
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Fingerprint,
                    contentDescription = "Biometrics",
                    tint = Navy900,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Quick Unlock with Biometrics",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Secondary Option: Enter App PIN
            OutlinedButton(
                onClick = { showPinDialog = true },
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, Color(0xFF334B6E)),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Color.White
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Pin,
                    contentDescription = "App PIN",
                    tint = Color.White,
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
                    fontSize = 18.sp
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
                    }
                ) {
                    Text("Unlock")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPinDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
