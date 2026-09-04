package com.loanzo.app.ui.loan

import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.loanzo.app.data.entity.LoanEntity
import com.loanzo.app.ui.components.GlassCard
import com.loanzo.app.ui.theme.*
import com.loanzo.app.util.BiometricAuthManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AgreementSigningScreen(
    loan: LoanEntity,
    onCancel: () -> Unit,
    onComplete: (signature: Bitmap, selfie: Bitmap, biometricSuccess: Boolean) -> Unit
) {
    var currentStep by remember { androidx.compose.runtime.mutableIntStateOf(1) } // 1: Preview, 2: Signature, 3: Selfie, 4: Biometric
    
    var signatureBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var selfieBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var biometricVerified by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val takePictureLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
        if (bitmap != null) {
            selfieBitmap = bitmap
            currentStep = 4 // Move to biometric
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) {
            try {
                takePictureLauncher.launch(null)
            } catch (_: Exception) {
                Toast.makeText(context, "No camera app available", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(context, "Camera permission is required for selfie verification", Toast.LENGTH_LONG).show()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Sign Loan Agreement", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(Icons.Default.Close, "Cancel")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color(0xFFF8FAFC))
        ) {
            // Step Progress
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                StepIndicator(step = 1, currentStep = currentStep, label = "Terms")
                Divider(modifier = Modifier.weight(1f).padding(horizontal = 8.dp), color = if (currentStep > 1) Emerald400 else Gray400)
                StepIndicator(step = 2, currentStep = currentStep, label = "Sign")
                Divider(modifier = Modifier.weight(1f).padding(horizontal = 8.dp), color = if (currentStep > 2) Emerald400 else Gray400)
                StepIndicator(step = 3, currentStep = currentStep, label = "Selfie")
                Divider(modifier = Modifier.weight(1f).padding(horizontal = 8.dp), color = if (currentStep > 3) Emerald400 else Gray400)
                StepIndicator(step = 4, currentStep = currentStep, label = "Verify")
            }

            AnimatedContent(targetState = currentStep, label = "step_animation") { step ->
                when (step) {
                    1 -> {
                        // STEP 1: AGREEMENT PREVIEW
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            GlassCard(modifier = Modifier.fillMaxWidth()) {
                                Text("Agreement Summary", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(16.dp))
                                
                                Text("Principal Amount: ₹${String.format("%,.2f", loan.sanctionedAmount)}", style = MaterialTheme.typography.bodyLarge)
                                Text("Interest Rate: ${loan.interestRate}% (${loan.interestModel})", style = MaterialTheme.typography.bodyLarge)
                                Text("Tenure: ${loan.tenureMonths} Months", style = MaterialTheme.typography.bodyLarge)
                                Text("Penalty Rate: ${loan.penaltyRate}% (${loan.penaltyModel})", style = MaterialTheme.typography.bodyLarge)
                                
                                Spacer(modifier = Modifier.height(24.dp))
                                Text("By proceeding, you agree to the full terms and conditions of this loan. A formal PDF will be generated and signed by both parties.", 
                                    style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            
                            Spacer(modifier = Modifier.height(32.dp))
                            
                            Button(
                                onClick = { currentStep = 2 },
                                modifier = Modifier.fillMaxWidth().height(52.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("I Agree, Proceed to Sign", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    2 -> {
                        // STEP 2: SIGNATURE PAD
                        Column(modifier = Modifier.fillMaxSize()) {
                            Box(modifier = Modifier.weight(1f)) {
                                LocalESignScreen(
                                    onSignatureCaptured = { bmp ->
                                        if (bmp != null) {
                                            signatureBitmap = bmp
                                            currentStep = 3 // Move to selfie
                                        }
                                    },
                                    onCancel = onCancel
                                )
                            }
                        }
                    }
                    3 -> {
                        // STEP 3: LIVENESS SELFIE
                        Column(
                            modifier = Modifier.fillMaxSize().padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(Icons.Default.Face, null, modifier = Modifier.size(80.dp), tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.height(24.dp))
                            Text("Identity Verification", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Please take a clear selfie to verify your identity. This will be attached to the agreement.", 
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                            
                            Spacer(modifier = Modifier.height(32.dp))
                            
                            Button(
                                onClick = {
                                    val hasCameraPerm = ContextCompat.checkSelfPermission(
                                        context,
                                        android.Manifest.permission.CAMERA
                                    ) == android.content.pm.PackageManager.PERMISSION_GRANTED

                                    if (hasCameraPerm) {
                                        try {
                                            takePictureLauncher.launch(null)
                                        } catch (_: Exception) {
                                            Toast.makeText(context, "No camera app available", Toast.LENGTH_SHORT).show()
                                        }
                                    } else {
                                        cameraPermissionLauncher.launch(android.Manifest.permission.CAMERA)
                                    }
                                },
                                modifier = Modifier.fillMaxWidth().height(52.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.CameraAlt, null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Capture Selfie", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    4 -> {
                        // STEP 4: BIOMETRIC VERIFY
                        Column(
                            modifier = Modifier.fillMaxSize().padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            if (selfieBitmap != null) {
                                Image(
                                    bitmap = selfieBitmap!!.asImageBitmap(),
                                    contentDescription = "Selfie",
                                    modifier = Modifier.size(120.dp).clip(CircleShape).border(2.dp, Emerald400, CircleShape),
                                    contentScale = ContentScale.Crop
                                )
                                Spacer(modifier = Modifier.height(24.dp))
                            }
                            
                            Icon(Icons.Default.Fingerprint, null, modifier = Modifier.size(60.dp), tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Final Authorization", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Use your fingerprint or face unlock to finalize the digital signature.", 
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                            
                            Spacer(modifier = Modifier.height(32.dp))
                            
                            Button(
                                onClick = {
                                    val fragmentActivity = BiometricAuthManager.getActivity(context)
                                    if (fragmentActivity != null) {
                                        BiometricAuthManager.authenticate(
                                            activity = fragmentActivity,
                                            title = "Sign Loan Agreement",
                                            subtitle = "Verify identity to digitally sign",
                                            onSuccess = {
                                                biometricVerified = true
                                                if (signatureBitmap != null && selfieBitmap != null) {
                                                    onComplete(signatureBitmap!!, selfieBitmap!!, biometricVerified)
                                                }
                                            },
                                            onError = { err ->
                                                Toast.makeText(context, "Biometric authorization failed: $err", Toast.LENGTH_LONG).show()
                                            }
                                        )
                                    } else {
                                        Toast.makeText(context, "Activity window unavailable for biometrics", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier.fillMaxWidth().height(52.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Emerald400, contentColor = Color.White)
                            ) {
                                Icon(Icons.Default.Security, null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Authenticate & Sign", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StepIndicator(step: Int, currentStep: Int, label: String) {
    val isCompleted = currentStep > step
    val isCurrent = currentStep == step
    
    val color = when {
        isCompleted -> Emerald400
        isCurrent -> MaterialTheme.colorScheme.primary
        else -> Gray400
    }
    
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(
            shape = CircleShape,
            color = if (isCompleted || isCurrent) color else Color.Transparent,
            border = if (!isCompleted && !isCurrent) androidx.compose.foundation.BorderStroke(2.dp, color) else null,
            modifier = Modifier.size(32.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                if (isCompleted) {
                    Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(20.dp))
                } else {
                    Text("$step", color = if (isCurrent) Color.White else color, fontWeight = FontWeight.Bold)
                }
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = color, fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal)
    }
}
