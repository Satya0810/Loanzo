package com.loanzo.app.ui.auth

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import com.loanzo.app.R
import com.loanzo.app.ui.theme.*
import kotlin.random.Random

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    onLogin: (userId: String, pass: String) -> Unit,
    onBiometricLogin: (typedUserId: String) -> Unit,
    onGoogleLogin: (typedUserId: String) -> Unit = {},
    onNavigateToRegister: () -> Unit,
    onNavigateToForgotPassword: () -> Unit,
    isBiometricAvailable: Boolean = true,
    isLoading: Boolean = false,
    error: String? = null,
    onClearError: () -> Unit = {},
    isUserIdVerified: Boolean = false,
    onVerifyUserId: (String) -> Unit = {},
    onResetUserIdVerification: () -> Unit = {}
) {
    var userId by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var loginStep by rememberSaveable { androidx.compose.runtime.mutableIntStateOf(1) }
    var passwordVisible by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(isUserIdVerified) {
        if (isUserIdVerified) {
            loginStep = 2
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Navy900, Navy700, Navy900)))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(48.dp))

            // Brand Logo
            Image(
                painter = painterResource(id = R.drawable.app_logo),
                contentDescription = stringResource(R.string.loanzo_1),
                modifier = Modifier
                    .size(92.dp)
                    .clip(RoundedCornerShape(24.dp))
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = stringResource(R.string.loanzo),
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Bold,
                color = Gold500,
                letterSpacing = 4.sp
            )
            Text(
                text = stringResource(R.string.verified_loan_utilization),
                style = MaterialTheme.typography.bodyMedium,
                color = Gray300,
                modifier = Modifier.padding(top = 2.dp)
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Login Card
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceDarkCard.copy(alpha = 0.85f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(22.dp)) {
                    Text(
                        text = "Welcome Back",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "Sign in to your account",
                        style = MaterialTheme.typography.bodySmall,
                        color = Gray400,
                        modifier = Modifier.padding(top = 4.dp)
                    )

                    AnimatedVisibility(visible = error != null) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Red400.copy(alpha = 0.15f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.ErrorOutline, null, tint = Red400, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = error ?: "",
                                    color = Red400,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    if (loginStep == 1) {
                        OutlinedTextField(
                            value = userId,
                            onValueChange = { userId = it; onClearError() },
                            label = { Text("Username") },
                            leadingIcon = { Icon(Icons.Default.Person, null) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Gold500,
                                focusedLabelColor = Gold500,
                                cursorColor = Gold500,
                                unfocusedBorderColor = Gray600,
                                focusedLeadingIconColor = Gold500
                            )
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        Button(
                            onClick = { onVerifyUserId(userId) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Gold500,
                                contentColor = Navy900
                            ),
                            enabled = userId.isNotBlank()
                        ) {
                            Text("Next", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(Icons.Default.ArrowForward, contentDescription = "Next", modifier = Modifier.size(18.dp))
                        }

                        if (isBiometricAvailable) {
                            Spacer(modifier = Modifier.height(12.dp))
                            OutlinedButton(
                                onClick = { onBiometricLogin(userId.trim()) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Gold500),
                                border = ButtonDefaults.outlinedButtonBorder(enabled = true).copy(
                                    brush = Brush.horizontalGradient(listOf(Gold500, Emerald400))
                                ),
                                enabled = !isLoading
                            ) {
                                Icon(Icons.Default.Fingerprint, null, tint = Gold500, modifier = Modifier.size(22.dp))
                                Spacer(modifier = Modifier.width(10.dp))
                                Text("Sign in with Biometrics", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            }
                        }
                    } else {
                        // Step 2: Password & Biometrics
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = userId, color = Gold500, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                            TextButton(onClick = { 
                                loginStep = 1
                                password = ""
                                onResetUserIdVerification()
                            }) {
                                Text("Edit", color = Gray400)
                            }
                        }

                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it; onClearError() },
                            label = { Text("Password") },
                            leadingIcon = { Icon(Icons.Default.Lock, null) },
                            trailingIcon = {
                                val image = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff
                                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                    Icon(image, contentDescription = if (passwordVisible) "Hide password" else "Show password")
                                }
                            },
                            visualTransformation = if (passwordVisible) androidx.compose.ui.text.input.VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Gold500,
                                focusedLabelColor = Gold500,
                                cursorColor = Gold500,
                                unfocusedBorderColor = Gray600,
                                focusedLeadingIconColor = Gold500
                            )
                        )
                        
                        // Forgot Password link
                        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                            TextButton(onClick = onNavigateToForgotPassword) {
                                Text("Forgot Password?", color = Gold500)
                            }
                        }

                        Button(
                            onClick = {
                                onLogin(userId.trim(), password)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Gold500,
                                contentColor = Navy900
                            ),
                            enabled = password.isNotBlank() && !isLoading
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Navy900, strokeWidth = 2.dp)
                            } else {
                                Text("Sign In", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            }
                        }

                        // Biometric Sign In
                        if (isBiometricAvailable) {
                            Spacer(modifier = Modifier.height(12.dp))
                            OutlinedButton(
                                onClick = { onBiometricLogin(userId.trim()) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Gold500),
                                border = ButtonDefaults.outlinedButtonBorder(enabled = true).copy(
                                    brush = Brush.horizontalGradient(listOf(Gold500, Emerald400))
                                ),
                                enabled = !isLoading
                            ) {
                                Icon(Icons.Default.Fingerprint, null, tint = Gold500, modifier = Modifier.size(22.dp))
                                Spacer(modifier = Modifier.width(10.dp))
                                Text("Sign in with Biometrics", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            }
                        }

                        // Continue with Google
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedButton(
                            onClick = { onGoogleLogin(userId.trim()) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                            border = ButtonDefaults.outlinedButtonBorder(enabled = true).copy(
                                brush = Brush.horizontalGradient(
                                    listOf(
                                        Color(0xFF4285F4),
                                        Color(0xFF34A853),
                                        Color(0xFFFBBC05),
                                        Color(0xFFEA4335)
                                    )
                                )
                            ),
                            enabled = !isLoading
                        ) {
                            Icon(
                                Icons.Default.AccountCircle,
                                contentDescription = "Google",
                                tint = Color(0xFF4285F4),
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("Continue with Google", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    stringResource(R.string.don_t_have_an_account),
                    color = Gray400,
                    style = MaterialTheme.typography.bodyMedium
                )
                TextButton(onClick = onNavigateToRegister) {
                    Text(
                        stringResource(R.string.register),
                        color = Gold500,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(
    onRegister: (name: String, email: String, phone: String, pass: String, role: String, enableBiometrics: Boolean, username: String) -> Unit,
    onSendEmailVerification: (String) -> Unit,
    onSetPhoneVerified: (Boolean) -> Unit,
    onInitiatePhoneVerification: (String, String, String) -> Unit,
    onResetAuthState: () -> Unit,
    isEmailVerified: Boolean,
    isCheckingEmailVerification: Boolean,
    isPhoneVerified: Boolean,
    isCheckingPhoneVerification: Boolean,
    isUsernameUnique: Boolean? = null,
    onCheckUsernameUnique: (String) -> Unit = {},
    onNavigateToLogin: () -> Unit,
    isLoading: Boolean = false,
    error: String? = null,
    onClearError: () -> Unit = {}
) {
    LaunchedEffect(Unit) {
        onResetAuthState()
    }

    var step by rememberSaveable { mutableIntStateOf(1) } // 1: Details, 2: Password
    var name by rememberSaveable { mutableStateOf("") }
    var username by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var phone by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var confirmPassword by rememberSaveable { mutableStateOf("") }
    var passwordVisible by rememberSaveable { mutableStateOf(false) }
    var confirmPasswordVisible by rememberSaveable { mutableStateOf(false) }

    // Biometric Enrollment State
    var enableBiometrics by rememberSaveable { mutableStateOf(true) }
    var isBiometricEnrolled by rememberSaveable { mutableStateOf(false) }

    // Outbound Message Verification State
    var hasInitiatedMessage by rememberSaveable { mutableStateOf(false) }
    var showPhoneVerificationBox by rememberSaveable { mutableStateOf(false) }
    
    var countryCode by rememberSaveable { mutableStateOf("+91") }
    var expandedCountryCode by rememberSaveable { mutableStateOf(false) }
    
    val isAppOwnerSpecialCase = phone.trim() == "7061559039"
    val effectivelyEmailVerified = isEmailVerified // DO NOT bypass email verification
    val effectivelyPhoneVerified = isPhoneVerified || isAppOwnerSpecialCase

    val countryCodes = listOf(
        "+91" to "India",
        "+1" to "USA",
        "+44" to "UK",
        "+61" to "Australia",
        "+971" to "UAE",
        "+65" to "Singapore"
    )

    val context = LocalContext.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Navy900, Navy700, Navy900)))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            Image(
                painter = painterResource(id = R.drawable.app_logo),
                contentDescription = stringResource(R.string.loanzo_1),
                modifier = Modifier
                    .size(76.dp)
                    .clip(RoundedCornerShape(20.dp))
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = stringResource(R.string.loanzo),
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = Gold500,
                letterSpacing = 3.sp
            )
            Text(
                text = stringResource(R.string.create_account),
                style = MaterialTheme.typography.bodyMedium,
                color = Gray400
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Step Indicator
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                StepBadge(number = "1", label = "Details & 2FA", isActive = step == 1, isCompleted = step > 1)
                Spacer(modifier = Modifier.width(16.dp))
                HorizontalDivider(modifier = Modifier.width(32.dp), color = if (step > 1) Gold500 else Gray600)
                Spacer(modifier = Modifier.width(16.dp))
                StepBadge(number = "2", label = "Security", isActive = step == 2, isCompleted = false)
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Error banner
            AnimatedVisibility(visible = error != null) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (error?.contains("verified", ignoreCase = true) == true) Emerald400.copy(alpha = 0.15f) else Red400.copy(alpha = 0.15f),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                ) {
                    Text(
                        text = error ?: "",
                        color = if (error?.contains("verified", ignoreCase = true) == true) Emerald400 else Red400,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(14.dp)
                    )
                }
            }

            // FORM CARD
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = SurfaceDark,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                ) {
                    if (step == 1) {
                        // Full Name
                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it; onClearError() },
                            label = { Text("Full Name") },
                            leadingIcon = { Icon(Icons.Default.Person, null) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Gold500,
                                focusedLabelColor = Gold500,
                                cursorColor = Gold500,
                                unfocusedBorderColor = Gray600
                            )
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // Username Field
                        OutlinedTextField(
                            value = username,
                            onValueChange = { 
                                username = it
                                onClearError()
                                onCheckUsernameUnique(it)
                            },
                            label = { Text("Username") },
                            leadingIcon = { Icon(Icons.Default.AccountCircle, null) },
                            trailingIcon = {
                                if (isUsernameUnique == true) {
                                    Icon(Icons.Default.Check, contentDescription = "Unique", tint = Emerald400)
                                } else if (isUsernameUnique == false) {
                                    Icon(Icons.Default.Close, contentDescription = "Taken", tint = Red400)
                                }
                            },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Gold500,
                                focusedLabelColor = Gold500,
                                cursorColor = Gold500,
                                unfocusedBorderColor = Gray600
                            )
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // Email Field with Inline Verification
                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            OutlinedTextField(
                                value = email,
                                onValueChange = { email = it; onClearError(); },
                                label = { Text("Email Address") },
                                leadingIcon = { Icon(Icons.Default.Email, null) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(14.dp),
                                enabled = !effectivelyEmailVerified && !isCheckingEmailVerification,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = if (effectivelyEmailVerified) Emerald400 else Gold500,
                                    focusedLabelColor = Gold500,
                                    cursorColor = Gold500,
                                    unfocusedBorderColor = Gray600
                                )
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            if (isCheckingEmailVerification) {
                                CircularProgressIndicator(modifier = Modifier.size(28.dp), color = Gold500, strokeWidth = 2.dp)
                            } else if (!effectivelyEmailVerified) {
                                Button(
                                    onClick = {
                                        onSendEmailVerification(email.trim())
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Gold500, contentColor = Navy900),
                                    enabled = email.isNotBlank() && email.contains("@")
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.Send, null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Verify", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            } else {
                                Icon(Icons.Default.CheckCircle, contentDescription = "Verified", tint = Emerald400, modifier = Modifier.size(28.dp))
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Phone Field
                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            OutlinedTextField(
                                value = phone,
                                onValueChange = { phone = it; onClearError(); hasInitiatedMessage = false; showPhoneVerificationBox = false },
                                label = { Text("Phone Number") },
                                leadingIcon = { 
                                    Box {
                                        Row(
                                            modifier = Modifier.clickable { expandedCountryCode = true }.padding(start = 12.dp, end = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(countryCode, color = Gold500, fontWeight = FontWeight.Bold)
                                            Icon(Icons.Default.ArrowDropDown, contentDescription = "Country Code", tint = Gold500)
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Box(modifier = Modifier.width(1.dp).height(24.dp).background(Gray600))
                                        }
                                        DropdownMenu(
                                            expanded = expandedCountryCode,
                                            onDismissRequest = { expandedCountryCode = false }
                                        ) {
                                            countryCodes.forEach { (code, name) ->
                                                DropdownMenuItem(
                                                    text = { Text("$name ($code)") },
                                                    onClick = {
                                                        countryCode = code
                                                        expandedCountryCode = false
                                                    }
                                                )
                                            }
                                        }
                                    }
                                },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(14.dp),
                                enabled = !effectivelyPhoneVerified && !isCheckingPhoneVerification,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = if (effectivelyPhoneVerified) Emerald400 else Gold500,
                                    focusedLabelColor = Gold500,
                                    cursorColor = Gold500,
                                    unfocusedBorderColor = Gray600
                                )
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            if (isCheckingPhoneVerification) {
                                CircularProgressIndicator(modifier = Modifier.size(28.dp), color = Gold500, strokeWidth = 2.dp)
                            } else if (!effectivelyPhoneVerified) {
                                Button(
                                    onClick = { showPhoneVerificationBox = true },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Gold500, contentColor = Navy900),
                                    enabled = phone.isNotBlank() && phone.length >= 10 && !showPhoneVerificationBox
                                ) {
                                    Icon(Icons.Default.VerifiedUser, null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Verify", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            } else {
                                Icon(Icons.Default.CheckCircle, contentDescription = "Verified", tint = Emerald400, modifier = Modifier.size(28.dp))
                            }
                        }

                        // Phone Verification Box (Outbound SMS / WhatsApp)
                        if (showPhoneVerificationBox && !effectivelyPhoneVerified) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = SurfaceDarkCard,
                                border = androidx.compose.foundation.BorderStroke(1.dp, Gold500.copy(alpha = 0.4f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Text(
                                        text = "Verify Phone Number",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    Text(
                                        text = "Send a quick verification message from your SIM or WhatsApp:",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Gray400,
                                        modifier = Modifier.padding(top = 2.dp, bottom = 10.dp)
                                    )

                                    Row(modifier = Modifier.fillMaxWidth()) {
                                        // Button 1: Send SMS from SIM
                                        Button(
                                            onClick = {
                                                val token = com.loanzo.app.util.VerificationManager.generateSecureToken()
                                                val msg = "Loanzo-Verification-Token: $token for $phone"
                                                try {
                                                    val smsIntent = Intent(Intent.ACTION_SENDTO).apply {
                                                        data = Uri.parse("smsto:+910000000000")
                                                        putExtra("sms_body", msg)
                                                    }
                                                    context.startActivity(smsIntent)
                                                } catch (_: Exception) {}
                                                hasInitiatedMessage = true
                                                onInitiatePhoneVerification(countryCode + phone.trim(), token, username)
                                            },
                                            shape = RoundedCornerShape(10.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = Gold500, contentColor = Navy900),
                                            modifier = Modifier.weight(1f),
                                            enabled = phone.isNotBlank() && phone.length >= 10 && !isCheckingPhoneVerification
                                        ) {
                                            Icon(Icons.Default.Sms, null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Send SMS", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        }

                                        Spacer(modifier = Modifier.width(8.dp))

                                        // Button 2: Send WhatsApp Message
                                        Button(
                                            onClick = {
                                                val token = com.loanzo.app.util.VerificationManager.generateSecureToken()
                                                val msg = "Loanzo-Verification-Token: $token for $phone"
                                                try {
                                                    val url = "https://api.whatsapp.com/send?phone=+910000000000&text=" + Uri.encode(msg)
                                                    val waIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                                    context.startActivity(waIntent)
                                                } catch (_: Exception) {}
                                                hasInitiatedMessage = true
                                                onInitiatePhoneVerification(countryCode + phone.trim(), token, username)
                                            },
                                            shape = RoundedCornerShape(10.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = Emerald400, contentColor = Navy900),
                                            modifier = Modifier.weight(1f),
                                            enabled = phone.isNotBlank() && phone.length >= 10 && !isCheckingPhoneVerification
                                        ) {
                                            Icon(Icons.AutoMirrored.Filled.Message, null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("WhatsApp", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }

                                    // Automatic polling listener UI
                                    if (isCheckingPhoneVerification) {
                                        var timeLeft by rememberSaveable { mutableIntStateOf(60) }
                                        LaunchedEffect(isCheckingPhoneVerification) {
                                            if (isCheckingPhoneVerification) {
                                                timeLeft = 60
                                                while (timeLeft > 0) {
                                                    kotlinx.coroutines.delay(1000)
                                                    timeLeft--
                                                }
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(16.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.Center,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Gold500, strokeWidth = 2.dp)
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Text("Listening for Admin verification... ${timeLeft}s", color = Gray400, fontSize = 13.sp)
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(28.dp))

                        Button(
                            onClick = { step = 2 },
                            modifier = Modifier.fillMaxWidth().height(52.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Gold500, contentColor = Navy900),
                            enabled = name.isNotBlank() && effectivelyEmailVerified && effectivelyPhoneVerified && !isLoading
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Navy900, strokeWidth = 2.dp)
                            } else {
                                Text("Continue to Security Setup", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            }
                        }
                    } else {
                        // STEP 2: Password
                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            label = { Text("Create Password") },
                            leadingIcon = { Icon(Icons.Default.Lock, null) },
                            trailingIcon = {
                                val image = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff
                                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                    Icon(image, contentDescription = if (passwordVisible) "Hide password" else "Show password")
                                }
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            visualTransformation = if (passwordVisible) androidx.compose.ui.text.input.VisualTransformation.None else PasswordVisualTransformation(),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Gold500,
                                focusedLabelColor = Gold500,
                                cursorColor = Gold500,
                                unfocusedBorderColor = Gray600
                            )
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = confirmPassword,
                            onValueChange = { confirmPassword = it },
                            label = { Text("Confirm Password") },
                            leadingIcon = { Icon(Icons.Default.LockReset, null) },
                            trailingIcon = {
                                val image = if (confirmPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff
                                IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                                    Icon(image, contentDescription = if (confirmPasswordVisible) "Hide password" else "Show password")
                                }
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            visualTransformation = if (confirmPasswordVisible) androidx.compose.ui.text.input.VisualTransformation.None else PasswordVisualTransformation(),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Gold500,
                                focusedLabelColor = Gold500,
                                cursorColor = Gold500,
                                unfocusedBorderColor = Gray600
                            )
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Biometric Enrollment Option
                        val isBiometricSupported = remember(context) {
                            com.loanzo.app.util.BiometricAuthManager.isBiometricAvailable(context)
                        }

                        if (isBiometricSupported) {
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = SurfaceDarkCard,
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (isBiometricEnrolled) Emerald400.copy(alpha = 0.6f) else Gold500.copy(alpha = 0.3f)
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Icon(
                                                Icons.Default.Fingerprint,
                                                contentDescription = null,
                                                tint = if (isBiometricEnrolled) Emerald400 else Gold500,
                                                modifier = Modifier.size(28.dp)
                                            )
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Column {
                                                Text(
                                                    "Register Biometrics",
                                                    color = Color.White,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 14.sp
                                                )
                                                Text(
                                                    if (isBiometricEnrolled) "Biometrics Verified ✓ (Ready for 1-Touch Sign In)" else "Enable fingerprint or face unlock",
                                                    color = if (isBiometricEnrolled) Emerald400 else Gray400,
                                                    fontSize = 11.sp
                                                )
                                            }
                                        }

                                        Switch(
                                            checked = enableBiometrics,
                                            onCheckedChange = { checked ->
                                                enableBiometrics = checked
                                                if (!checked) isBiometricEnrolled = false
                                            },
                                            colors = SwitchDefaults.colors(
                                                checkedThumbColor = Navy900,
                                                checkedTrackColor = Gold500
                                            )
                                        )
                                    }

                                    if (enableBiometrics && !isBiometricEnrolled) {
                                        Spacer(modifier = Modifier.height(10.dp))
                                        val fragmentActivity = com.loanzo.app.util.BiometricAuthManager.getActivity(context)
                                        Button(
                                            onClick = {
                                                fragmentActivity?.let { fa ->
                                                    com.loanzo.app.util.BiometricAuthManager.authenticate(
                                                        activity = fa,
                                                        title = "Register Biometrics",
                                                        subtitle = "Verify fingerprint or face to enable 1-touch login",
                                                        onSuccess = {
                                                            isBiometricEnrolled = true
                                                            onClearError()
                                                        },
                                                        onError = { err ->
                                                            // optional note
                                                        }
                                                    )
                                                }
                                            },
                                            shape = RoundedCornerShape(10.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = Gold500, contentColor = Navy900),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Icon(Icons.Default.Fingerprint, null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Scan Fingerprint / Face ID Now", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))
                        }

                        Button(
                            onClick = {
                                if (password == confirmPassword && password.length >= 8) {
                                    onRegister(name.trim(), email.trim(), countryCode + phone.trim(), password.trim(), "USER", enableBiometrics, username.trim())
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(52.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Gold500, contentColor = Navy900),
                            enabled = password.length >= 8 && password == confirmPassword && !isLoading
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Navy900, strokeWidth = 2.dp)
                            } else {
                                Text("Complete Registration", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedButton(
                            onClick = { step = 1 },
                            modifier = Modifier.fillMaxWidth().height(50.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Gray300)
                        ) {
                            Text("Back to Details")
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Footer
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.already_have_an_account),
                    color = Gray400,
                    style = MaterialTheme.typography.bodyMedium
                )
                TextButton(onClick = onNavigateToLogin) {
                    Text(stringResource(R.string.sign_in), color = Gold500, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForgotPasswordScreen(
    step: Int, // 1: ID, 2: 2FA Selection, 3: Verify 1, 4: Verify 2, 5: Create New Pass
    verifiedFactors: List<String>,
    resetUserEmail: String = "",
    resetUserPhone: String = "",
    isEmailVerified: Boolean = false,
    onInitiate: (String) -> Unit,
    onAddFactor: (String) -> Unit,
    onResetPassword: (String) -> Unit,
    onSendEmailVerification: (String) -> Unit,
    onVerifyEmailOtp: (String) -> Unit,
    onSetPhoneVerified: (Boolean) -> Unit,
    onResetAuthState: () -> Unit,
    isLoading: Boolean,
    error: String?,
    onNavigateToLogin: () -> Unit,
    onClearError: () -> Unit
) {
    LaunchedEffect(Unit) {
        onResetAuthState()
    }

    LaunchedEffect(isEmailVerified) {
        if (isEmailVerified && !verifiedFactors.contains("email")) {
            onAddFactor("email")
        }
    }

    var loginId by remember { mutableStateOf("") }
    var selectedFactor by remember { mutableStateOf<String?>(null) }
    var pendingPhoneToken by remember { mutableStateOf<String?>(null) }
    var otpInput by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmNewPassword by remember { mutableStateOf("") }
    var newPasswordVisible by remember { mutableStateOf(false) }
    var confirmNewPasswordVisible by remember { mutableStateOf(false) }
    var step5Error by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    val activity = context as? Activity

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Navy900, Navy700, Navy900)))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(36.dp))
            Image(
                painter = painterResource(id = R.drawable.app_logo),
                contentDescription = "Loanzo Logo",
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(20.dp))
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text("Reset Password", style = MaterialTheme.typography.headlineMedium, color = Color.White, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(28.dp))

            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceDarkCard.copy(alpha = 0.85f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(22.dp)) {
                    val displayError = error ?: step5Error
                    AnimatedVisibility(visible = displayError != null) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (displayError?.contains("verified", ignoreCase = true) == true || displayError?.contains("successful", ignoreCase = true) == true) Emerald400.copy(alpha = 0.15f) else Red400.copy(alpha = 0.15f),
                            modifier = Modifier.fillMaxWidth().padding(bottom = 14.dp)
                        ) {
                            Text(
                                text = displayError ?: "",
                                color = if (displayError?.contains("verified", ignoreCase = true) == true || displayError?.contains("successful", ignoreCase = true) == true) Emerald400 else Red400,
                                modifier = Modifier.padding(10.dp),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }

                    if (step == 1) {
                        Text("Enter your registered User ID to begin.", color = Gray300, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(16.dp))
                        OutlinedTextField(
                            value = loginId,
                            onValueChange = { loginId = it; onClearError(); step5Error = null },
                            label = { Text("User ID") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Gold500, focusedLabelColor = Gold500, unfocusedBorderColor = Gray600)
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(
                            onClick = { onInitiate(loginId.trim()) },
                            modifier = Modifier.fillMaxWidth().height(50.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Gold500, contentColor = Navy900),
                            enabled = loginId.isNotBlank() && !isLoading
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Navy900)
                            } else {
                                Text("Continue", fontWeight = FontWeight.Bold)
                            }
                        }
                    } else if (step in 2..4) {
                        Text("Two-Factor Authentication (2FA)", color = Gold500, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text("Verify any 2 of the methods below to unlock password reset.", color = Gray300, fontSize = 13.sp, modifier = Modifier.padding(top = 4.dp, bottom = 16.dp))
                        
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Emerald400.copy(alpha = 0.15f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                "Methods Verified: ${verifiedFactors.size}/2 (${if (verifiedFactors.isEmpty()) "None" else verifiedFactors.joinToString()})",
                                color = Emerald400,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))

                        val targetEmail = if (resetUserEmail.isNotBlank()) resetUserEmail else if (loginId.contains("@")) loginId.trim() else ""
                        val targetPhone = if (resetUserPhone.isNotBlank()) resetUserPhone else loginId.trim()

                        // Method 1: Email Verification
                        if (!verifiedFactors.contains("email") && targetEmail.isNotBlank()) {
                            OutlinedButton(
                                onClick = { 
                                    onSendEmailVerification(targetEmail)
                                    selectedFactor = "email"
                                    otpInput = ""
                                },
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                shape = RoundedCornerShape(12.dp),
                                border = ButtonDefaults.outlinedButtonBorder(enabled = true).copy(brush = Brush.horizontalGradient(listOf(Gold500, Gold500)))
                            ) {
                                Icon(Icons.Default.Email, null, tint = Gold500, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Verify with Email Link ($targetEmail)", color = Gold500, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }

                        // Method 2: Biometric Verification
                        if (!verifiedFactors.contains("biometric")) {
                            OutlinedButton(
                                onClick = { 
                                    val fa = com.loanzo.app.util.BiometricAuthManager.getActivity(context) ?: (activity as? FragmentActivity)
                                    if (fa != null) {
                                        com.loanzo.app.util.BiometricAuthManager.authenticate(
                                            activity = fa,
                                            onSuccess = { onAddFactor("biometric") },
                                            onError = { err ->
                                                android.widget.Toast.makeText(context, "Biometric error: $err", android.widget.Toast.LENGTH_SHORT).show()
                                            }
                                        )
                                    } else {
                                        android.widget.Toast.makeText(context, "Biometrics unavailable on this device", android.widget.Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                shape = RoundedCornerShape(12.dp),
                                border = ButtonDefaults.outlinedButtonBorder(enabled = true).copy(brush = Brush.horizontalGradient(listOf(Gold500, Gold500)))
                            ) {
                                Icon(Icons.Default.Fingerprint, null, tint = Gold500, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Verify with Biometrics", color = Gold500, fontWeight = FontWeight.Bold)
                            }
                        }

                        // Method 3: Outbound SMS / WhatsApp verification
                        if (!verifiedFactors.contains("phone")) {
                            OutlinedButton(
                                onClick = {
                                    val token = com.loanzo.app.util.VerificationManager.generateSecureToken()
                                    pendingPhoneToken = token
                                    val msg = "Loanzo-Password-Reset-Token: $token for $targetPhone"
                                    try {
                                        val url = "https://api.whatsapp.com/send?phone=+910000000000&text=" + Uri.encode(msg)
                                        val waIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                        context.startActivity(waIntent)
                                    } catch (_: Exception) {}
                                    selectedFactor = "phone"
                                    otpInput = ""
                                },
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                shape = RoundedCornerShape(12.dp),
                                border = ButtonDefaults.outlinedButtonBorder(enabled = true).copy(brush = Brush.horizontalGradient(listOf(Gold500, Gold500)))
                            ) {
                                Icon(Icons.AutoMirrored.Filled.Message, null, tint = Gold500, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Send Verification to Admin", color = Gold500, fontWeight = FontWeight.Bold)
                            }
                        }

                        // Active Input Box
                        if (selectedFactor != null) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Enter the 6-digit OTP code for $selectedFactor verification:", color = Gray300, fontSize = 13.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                OutlinedTextField(
                                    value = otpInput,
                                    onValueChange = { otpInput = it },
                                    placeholder = { Text("6-digit OTP") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true,
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Gold500, cursorColor = Gold500)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Button(
                                    onClick = {
                                        if (selectedFactor == "email") {
                                            onVerifyEmailOtp(otpInput.trim())
                                        } else if (selectedFactor == "phone") {
                                            if (pendingPhoneToken != null && otpInput.trim() == pendingPhoneToken) {
                                                onAddFactor("phone")
                                            } else {
                                                onVerifyEmailOtp(otpInput.trim())
                                            }
                                        }
                                        selectedFactor = null
                                        otpInput = ""
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Emerald400, contentColor = Navy900)
                                ) {
                                    Text("Verify", fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                    } else if (step == 5) {
                        Text("Create New Password", color = Gold500, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text("Enter and confirm your new secure password below.", color = Gray300, fontSize = 13.sp, modifier = Modifier.padding(top = 4.dp))
                        Spacer(modifier = Modifier.height(16.dp))
                        OutlinedTextField(
                            value = newPassword,
                            onValueChange = { newPassword = it; step5Error = null },
                            label = { Text("New Password") },
                            leadingIcon = { Icon(Icons.Default.Lock, null) },
                            trailingIcon = {
                                val image = if (newPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff
                                IconButton(onClick = { newPasswordVisible = !newPasswordVisible }) {
                                    Icon(image, contentDescription = if (newPasswordVisible) "Hide password" else "Show password")
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            visualTransformation = if (newPasswordVisible) androidx.compose.ui.text.input.VisualTransformation.None else PasswordVisualTransformation(),
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Gold500, focusedLabelColor = Gold500, unfocusedBorderColor = Gray600)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = confirmNewPassword,
                            onValueChange = { confirmNewPassword = it; step5Error = null },
                            label = { Text("Confirm New Password") },
                            leadingIcon = { Icon(Icons.Default.LockReset, null) },
                            trailingIcon = {
                                val image = if (confirmNewPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff
                                IconButton(onClick = { confirmNewPasswordVisible = !confirmNewPasswordVisible }) {
                                    Icon(image, contentDescription = if (confirmNewPasswordVisible) "Hide password" else "Show password")
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            visualTransformation = if (confirmNewPasswordVisible) androidx.compose.ui.text.input.VisualTransformation.None else PasswordVisualTransformation(),
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Gold500, focusedLabelColor = Gold500, unfocusedBorderColor = Gray600)
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(
                            onClick = {
                                if (newPassword != confirmNewPassword) {
                                    step5Error = "Passwords do not match."
                                } else if (newPassword.length < 4) {
                                    step5Error = "Password must be at least 4 characters."
                                } else {
                                    onResetPassword(newPassword.trim())
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(50.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Gold500, contentColor = Navy900),
                            enabled = newPassword.isNotBlank() && confirmNewPassword.isNotBlank() && !isLoading
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Navy900)
                            } else {
                                Text("Update Password", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            TextButton(onClick = onNavigateToLogin) {
                Text("Back to Sign In", color = Gold500)
            }
        }
    }
}

@Composable
private fun StepBadge(
    number: String,
    label: String,
    isActive: Boolean,
    isCompleted: Boolean
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Surface(
            shape = CircleShape,
            color = if (isCompleted) Emerald400 else if (isActive) Gold500 else SurfaceDarkCard,
            border = if (!isActive && !isCompleted) androidx.compose.foundation.BorderStroke(1.dp, Gray600) else null,
            modifier = Modifier.size(24.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                if (isCompleted) {
                    Icon(Icons.Default.Check, contentDescription = null, tint = Navy900, modifier = Modifier.size(14.dp))
                } else {
                    Text(
                        text = number,
                        color = if (isActive) Navy900 else Gray400,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = if (isActive || isCompleted) FontWeight.Bold else FontWeight.Normal,
            color = if (isCompleted) Emerald400 else if (isActive) Gold500 else Gray400
        )
    }
}

