package com.loanzo.app.ui.auth

import androidx.compose.ui.res.stringResource
import com.loanzo.app.R

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.loanzo.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KycScreen(
    currentStep: Int,
    isDiditLoading: Boolean = false,
    diditStatus: String? = null,
    onStartDiditKyc: () -> Unit = {},
    onCompleteStep: (step: Int, data: Map<String, String>) -> Unit,
    onFinish: () -> Unit
) {
    var panNumber by remember { mutableStateOf("") }
    var aadhaarNumber by remember { mutableStateOf("") }
    var upiId by remember { mutableStateOf("") }
    var bankAccount by remember { mutableStateOf("") }
    var currentKycStep by remember { mutableIntStateOf(currentStep.coerceAtLeast(1)) }

    val steps = listOf(
        KycStepInfo("PAN Verification", Icons.Default.CreditCard, "Enter your PAN card number"),
        KycStepInfo("Aadhaar KYC", Icons.Default.Badge, "Verify your Aadhaar identity"),
        KycStepInfo("Selfie + Liveness", Icons.Default.CameraAlt, "Take a selfie for identity match"),
        KycStepInfo("Bank / UPI ID", Icons.Default.AccountBalance, "Link your payment identity")
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Navy900, Navy800)
                )
            )
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = stringResource(R.string.kyc_verification),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Text(
            text = stringResource(R.string.complete_verification_to_unlock_borrowing_and_lending),
            style = MaterialTheme.typography.bodyMedium,
            color = Gray400,
            modifier = Modifier.padding(top = 4.dp)
        )

        Spacer(modifier = Modifier.height(20.dp))

        // DIDIT AI VERIFICATION HERO CARD
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = Navy700.copy(alpha = 0.85f)
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = Gold500.copy(alpha = 0.15f),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                Icons.Default.Bolt,
                                contentDescription = stringResource(R.string.didit),
                                tint = Gold500,
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            stringResource(R.string.fast_track_with_didit_ai),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Emerald400.copy(alpha = 0.15f)
                    ) {
                        Text(
                            stringResource(R.string.str_30_sec_kyc),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Emerald400,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    stringResource(R.string.instant_1_click_verification_with_government_id_auto_scan_3d_biometric_liveness_detection_and_real_time_aml_fraud_checks),
                    style = MaterialTheme.typography.bodySmall,
                    color = Gray300,
                    lineHeight = MaterialTheme.typography.bodySmall.lineHeight
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    KycFeatureChip("🪪 ID Auto-Scan")
                    KycFeatureChip("👤 3D Liveness")
                    KycFeatureChip("🛡️ AML Safe")
                }

                Spacer(modifier = Modifier.height(18.dp))

                Button(
                    onClick = onStartDiditKyc,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Gold500,
                        contentColor = Navy900
                    ),
                    enabled = !isDiditLoading
                ) {
                    if (isDiditLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Navy900,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.launching_didit), fontWeight = FontWeight.Bold)
                    } else {
                        Icon(
                            Icons.Default.VerifiedUser,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            if (diditStatus != null) "Re-verify with Didit ($diditStatus)" else "Verify with Didit AI",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            HorizontalDivider(modifier = Modifier.weight(1f), color = Gray600)
            Text(
                stringResource(R.string.or_manual_verification),
                style = MaterialTheme.typography.labelSmall,
                color = Gray400,
                fontWeight = FontWeight.SemiBold
            )
            HorizontalDivider(modifier = Modifier.weight(1f), color = Gray600)
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Step progress indicator
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            steps.forEachIndexed { index, step ->
                val stepNum = index + 1
                val isCompleted = currentKycStep > stepNum
                val isCurrent = currentKycStep == stepNum

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = when {
                            isCompleted -> Emerald400
                            isCurrent -> Gold500
                            else -> Gray600
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                            if (isCompleted) {
                                Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(18.dp))
                            } else {
                                Text(
                                    "$stepNum",
                                    color = if (isCurrent) Navy900 else Gray300,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.labelLarge
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = step.title.split(" ").first(),
                        style = MaterialTheme.typography.labelSmall,
                        color = when {
                            isCompleted -> Emerald400
                            isCurrent -> Gold500
                            else -> Gray500
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Step content
        AnimatedContent(
            targetState = currentKycStep,
            transitionSpec = {
                slideInHorizontally { width -> width } + fadeIn() togetherWith
                        slideOutHorizontally { width -> -width } + fadeOut()
            },
            label = "kyc_step"
        ) { step ->
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = SurfaceDarkCard.copy(alpha = 0.7f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    when (step) {
                        1 -> {
                            KycStepHeader(steps[0])
                            Spacer(modifier = Modifier.height(20.dp))
                            OutlinedTextField(
                                value = panNumber,
                                onValueChange = { if (it.length <= 10) panNumber = it.uppercase() },
                                label = { Text(stringResource(R.string.pan_number)) },
                                placeholder = { Text(stringResource(R.string.abcde1234f)) },
                                leadingIcon = { Icon(Icons.Default.CreditCard, null) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                                colors = kycTextFieldColors()
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                stringResource(R.string.enter_your_10_character_pan_number),
                                style = MaterialTheme.typography.bodySmall,
                                color = Gray400
                            )
                        }
                        2 -> {
                            KycStepHeader(steps[1])
                            Spacer(modifier = Modifier.height(20.dp))
                            OutlinedTextField(
                                value = aadhaarNumber,
                                onValueChange = { if (it.length <= 12 && it.all { c -> c.isDigit() }) aadhaarNumber = it },
                                label = { Text(stringResource(R.string.aadhaar_number)) },
                                placeholder = { Text(stringResource(R.string.xxxx_xxxx_xxxx)) },
                                leadingIcon = { Icon(Icons.Default.Badge, null) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                                colors = kycTextFieldColors()
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                stringResource(R.string.prototype_enter_any_12_digit_number),
                                style = MaterialTheme.typography.bodySmall,
                                color = Gray400
                            )
                        }
                        3 -> {
                            KycStepHeader(steps[2])
                            Spacer(modifier = Modifier.height(20.dp))
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = Navy600.copy(alpha = 0.5f),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp)
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxSize(),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        Icons.Default.CameraAlt,
                                        null,
                                        tint = Gold500,
                                        modifier = Modifier.size(48.dp)
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        stringResource(R.string.tap_to_take_selfie),
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = Gray300
                                    )
                                    Text(
                                        stringResource(R.string.prototype_simulated_verification),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Gray500
                                    )
                                }
                            }
                        }
                        4 -> {
                            KycStepHeader(steps[3])
                            Spacer(modifier = Modifier.height(20.dp))
                            OutlinedTextField(
                                value = upiId,
                                onValueChange = { upiId = it },
                                label = { Text(stringResource(R.string.upi_id)) },
                                placeholder = { Text(stringResource(R.string.yourname_upi)) },
                                leadingIcon = { Icon(Icons.Default.Payment, null) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                                colors = kycTextFieldColors()
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            OutlinedTextField(
                                value = bankAccount,
                                onValueChange = { bankAccount = it },
                                label = { Text(stringResource(R.string.bank_account_optional)) },
                                leadingIcon = { Icon(Icons.Default.AccountBalance, null) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                                colors = kycTextFieldColors()
                            )
                        }
                        else -> {
                            // Completion
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = Emerald400.copy(alpha = 0.15f),
                                    modifier = Modifier.size(80.dp)
                                ) {
                                    Icon(
                                        Icons.Default.VerifiedUser,
                                        null,
                                        tint = Emerald400,
                                        modifier = Modifier.padding(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    stringResource(R.string.kyc_complete),
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Emerald400
                                )
                                Text(
                                    stringResource(R.string.your_profile_is_now_verified),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Gray400
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    val isLastStep = step >= 4
                    val canProceed = when (step) {
                        1 -> panNumber.length == 10
                        2 -> aadhaarNumber.length == 12
                        3 -> true // Simulated
                        4 -> upiId.isNotBlank()
                        else -> true
                    }

                    Button(
                        onClick = {
                            if (step <= 4) {
                                val data = when (step) {
                                    1 -> mapOf("pan" to panNumber)
                                    4 -> mapOf("upiId" to upiId, "bankAccount" to bankAccount)
                                    else -> emptyMap()
                                }
                                onCompleteStep(step, data)
                                currentKycStep = step + 1
                            }
                            if (step >= 5 || (isLastStep && canProceed)) {
                                if (step >= 5) onFinish()
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (step >= 5) Emerald400 else Gold500,
                            contentColor = Navy900
                        ),
                        enabled = canProceed
                    ) {
                        Text(
                            when {
                                step >= 5 -> "Go to Dashboard"
                                isLastStep -> "Complete KYC"
                                else -> "Continue"
                            },
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun KycStepHeader(step: KycStepInfo) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(step.icon, null, tint = Gold500, modifier = Modifier.size(28.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                step.title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                step.description,
                style = MaterialTheme.typography.bodySmall,
                color = Gray400
            )
        }
    }
}

@Composable
private fun kycTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = Gold500,
    focusedLabelColor = Gold500,
    cursorColor = Gold500,
    unfocusedBorderColor = Gray600,
    focusedLeadingIconColor = Gold500
)

private data class KycStepInfo(
    val title: String,
    val icon: ImageVector,
    val description: String
)

@Composable
private fun KycFeatureChip(text: String) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = Navy800.copy(alpha = 0.8f)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = Gray300,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
        )
    }
}

