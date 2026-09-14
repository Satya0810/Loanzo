package com.loanzo.app.ui.auth

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import com.loanzo.app.ui.components.LoanzoText as Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.loanzo.app.R
import com.loanzo.app.ui.theme.*
import com.loanzo.app.util.getDisplayProfilePhoto

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KycScreen(
    currentStep: Int,
    user: com.loanzo.app.data.entity.UserEntity?,
    isDigiLockerLoading: Boolean = false,
    isUploadingPan: Boolean = false,
    isUploadingAadhaar: Boolean = false,
    isUploadingSelfie: Boolean = false,
    error: String? = null,
    onClearError: () -> Unit = {},
    onStartDigiLockerKyc: () -> Unit = {},
    onQuickSimulate: () -> Unit = {},
    onCompleteStep: (step: Int, data: Map<String, String>) -> Unit,
    onUploadSelfie: (Bitmap) -> Unit,
    onUploadDocument: (String, android.net.Uri) -> Unit,
    onResetKyc: () -> Unit = {},
    onSkip: () -> Unit = {},
    onFinish: () -> Unit
) {
    val context = LocalContext.current
    var selfieBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var showResetDialog by remember { mutableStateOf(false) }

    LaunchedEffect(error) {
        error?.let {
            android.widget.Toast.makeText(context, it, android.widget.Toast.LENGTH_LONG).show()
            onClearError()
        }
    }

    var panImageUri by remember { mutableStateOf<android.net.Uri?>(null) }
    var aadhaarImageUri by remember { mutableStateOf<android.net.Uri?>(null) }

    val panLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri -> panImageUri = uri }
    val aadhaarLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri -> aadhaarImageUri = uri }

    val takePictureLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
        if (bitmap != null) {
            selfieBitmap = bitmap
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) {
            try {
                takePictureLauncher.launch(null)
            } catch (e: Exception) {
                android.widget.Toast.makeText(context, "No camera app available or permission denied.", android.widget.Toast.LENGTH_LONG).show()
            }
        } else {
            android.widget.Toast.makeText(context, "Camera permission is required for selfie verification", android.widget.Toast.LENGTH_LONG).show()
        }
    }

    val isDigiLockerDone = user?.aadhaarVerified == true || user?.kycStatus == "VERIFIED" || currentStep >= 5
    val isSelfieDone = user?.selfieVerified == true
    val isPanDone = user?.panImageUrl?.isNotBlank() == true
    val isAadhaarDone = user?.aadhaarImageUrl?.isNotBlank() == true

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.kyc_verification),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Complete your verified credentials to proceed",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Surface(
                onClick = onSkip,
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Skip",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Skip for now",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        val isReKycMode = user?.kycStatus == "VERIFIED" || user?.kycStatus == "REJECTED"
        if (isReKycMode) {
            Spacer(modifier = Modifier.height(16.dp))
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = if (user?.kycStatus == "REJECTED") Red400.copy(alpha = 0.12f) else Gold500.copy(alpha = 0.12f),
                border = BorderStroke(1.dp, if (user?.kycStatus == "REJECTED") Red400.copy(alpha = 0.4f) else Gold500.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            if (user?.kycStatus == "REJECTED") Icons.Default.Warning else Icons.Default.PublishedWithChanges,
                            contentDescription = null,
                            tint = if (user?.kycStatus == "REJECTED") Red400 else Gold500,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = if (user?.kycStatus == "REJECTED") "KYC Re-Submission Required" else "Re-KYC / Credentials Update Active",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (user?.kycStatus == "REJECTED") Red400 else Gold500
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = if (user?.kycStatus == "REJECTED") 
                            "Your previous submission was flagged for updates. Please re-verify or re-upload your documents below."
                        else 
                            "You are in Re-KYC mode. You can update any of your verification records below. Changes will be synchronized across your account.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 1. DIGILOCKER SECTION (GOVT OF INDIA)
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Emerald400.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.VerifiedUser, null, tint = Emerald400)
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("DigiLocker Official KYC", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis)
                            Text("Govt of India • MeitY", style = MaterialTheme.typography.labelSmall, color = Emerald500, maxLines = 1, softWrap = false)
                        }
                    }
                    if (isDigiLockerDone) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(Icons.Default.CheckCircle, "Verified", tint = Emerald500)
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text("Direct live verification via official UIDAI Aadhaar OTP and Income Tax Department PAN Card records.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                
                if (isDigiLockerDone) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Emerald400.copy(alpha = 0.12f),
                        border = BorderStroke(1.dp, Emerald400.copy(alpha = 0.3f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.CheckCircle, null, tint = Emerald500, modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("DigiLocker Verified ✓", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = Emerald500, maxLines = 1, softWrap = false)
                                Text(
                                    text = if (!user?.name.isNullOrBlank()) "Linked to: ${user?.name}" else "Aadhaar & PAN matched with Govt Records",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedButton(
                        onClick = onStartDigiLockerKyc,
                        modifier = Modifier.fillMaxWidth().height(42.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Emerald500),
                        border = BorderStroke(1.dp, Emerald500.copy(alpha = 0.5f)),
                        enabled = !isDigiLockerLoading
                    ) {
                        if (isDigiLockerLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Emerald500, strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Connecting...", fontSize = 13.sp)
                        } else {
                            Icon(Icons.Default.Refresh, null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Re-verify via DigiLocker / Update", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                } else {
                    Spacer(modifier = Modifier.height(18.dp))
                    Button(
                        onClick = onStartDigiLockerKyc,
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Emerald400, contentColor = Navy900),
                        enabled = !isDigiLockerLoading
                    ) {
                        if (isDigiLockerLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Navy900, strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Connecting...", fontWeight = FontWeight.Bold)
                        } else {
                            Icon(Icons.Default.Verified, null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Verify with DigiLocker", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 2. SELFIE LIVENESS SECTION
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(shape = CircleShape, color = Gold500.copy(alpha = 0.15f), modifier = Modifier.size(36.dp)) {
                            Icon(Icons.Default.CameraAlt, null, tint = Gold500, modifier = Modifier.padding(8.dp))
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("Selfie Photo", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                            Text("Identity Verification", style = MaterialTheme.typography.labelSmall, color = Gold500)
                        }
                    }
                    if (isSelfieDone) {
                        Icon(Icons.Default.CheckCircle, "Verified", tint = Emerald500)
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))

                if (selfieBitmap != null && !isSelfieDone) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Image(
                            bitmap = selfieBitmap!!.asImageBitmap(),
                            contentDescription = "Captured Selfie",
                            modifier = Modifier.size(120.dp).clip(CircleShape).border(2.dp, Emerald400, CircleShape),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Selfie Captured ✓", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = Emerald400)
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Button(
                            onClick = { onUploadSelfie(selfieBitmap!!) },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary),
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !isUploadingSelfie
                        ) {
                            if (isUploadingSelfie) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Uploading...", fontWeight = FontWeight.Bold)
                            } else {
                                Icon(Icons.Default.Upload, null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Upload & Save Selfie", fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        
                        OutlinedButton(
                            onClick = {
                                if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                                    try {
                                        takePictureLauncher.launch(null)
                                    } catch (e: Exception) {
                                        android.widget.Toast.makeText(context, "No camera app available.", android.widget.Toast.LENGTH_LONG).show()
                                    }
                                } else {
                                    cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                                }
                            },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                            enabled = !isUploadingSelfie
                        ) {
                            Icon(Icons.Default.CameraAlt, null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Retake Photo")
                        }
                    }
                } else if (!isSelfieDone) {
                    OutlinedButton(
                        onClick = {
                            if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                                try {
                                    takePictureLauncher.launch(null)
                                } catch (e: Exception) {
                                    android.widget.Toast.makeText(context, "No camera app available.", android.widget.Toast.LENGTH_LONG).show()
                                }
                            } else {
                                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(120.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurfaceVariant),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.CameraAlt, null, tint = Gold500, modifier = Modifier.size(36.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Take a Selfie", fontWeight = FontWeight.Bold)
                        }
                    }
                } else {
                    // isSelfieDone is true
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        val avatarModel = user.getDisplayProfilePhoto(context)
                        if (avatarModel != null) {
                            coil.compose.AsyncImage(
                                model = avatarModel,
                                contentDescription = "Verified Selfie",
                                modifier = Modifier
                                    .size(100.dp)
                                    .clip(CircleShape)
                                    .border(2.dp, Emerald400, CircleShape),
                                contentScale = ContentScale.Crop
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                        Text("Selfie Verified & Active as Profile Photo ✓", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = Emerald400)
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedButton(
                            onClick = {
                                if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                                    try {
                                        takePictureLauncher.launch(null)
                                    } catch (e: Exception) {
                                        android.widget.Toast.makeText(context, "No camera app available.", android.widget.Toast.LENGTH_LONG).show()
                                    }
                                } else {
                                    cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(42.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Gold500),
                            border = BorderStroke(1.dp, Gold500.copy(alpha = 0.5f)),
                            enabled = !isUploadingSelfie
                        ) {
                            Icon(Icons.Default.CameraAlt, null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Retake & Update Selfie", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 3. PHYSICAL DOCUMENT UPLOADS
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(shape = CircleShape, color = Blue500.copy(alpha = 0.15f), modifier = Modifier.size(36.dp)) {
                            Icon(Icons.Default.UploadFile, null, tint = Blue500, modifier = Modifier.padding(8.dp))
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("Document Upload (Image)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                            Text("PAN and Aadhaar Cards", style = MaterialTheme.typography.labelSmall, color = Blue500)
                        }
                    }
                    if (isPanDone && isAadhaarDone) {
                        Icon(Icons.Default.CheckCircle, "Verified", tint = Emerald500)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (isPanDone) {
                    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CheckCircle, null, tint = Emerald400)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("PAN Card Uploaded ✓", color = Emerald400, fontWeight = FontWeight.SemiBold)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedButton(
                            onClick = { panLauncher.launch("image/*") },
                            modifier = Modifier.fillMaxWidth().height(38.dp),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Blue400),
                            border = BorderStroke(1.dp, Blue400.copy(alpha = 0.4f)),
                            enabled = !isUploadingPan
                        ) {
                            Icon(Icons.Default.UploadFile, null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (panImageUri != null) "New PAN Image Selected" else "Replace / Re-upload PAN", fontSize = 12.sp)
                        }
                        if (panImageUri != null) {
                            Button(
                                onClick = { onUploadDocument("PAN", panImageUri!!) },
                                modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                enabled = !isUploadingPan
                            ) {
                                if (isUploadingPan) {
                                    CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Uploading...", fontSize = 12.sp)
                                } else {
                                    Text("Confirm & Upload New PAN", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                } else {
                    OutlinedButton(
                        onClick = { panLauncher.launch("image/*") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = if (panImageUri != null) Emerald500 else MaterialTheme.colorScheme.onSurface),
                        border = BorderStroke(1.dp, if (panImageUri != null) Emerald500 else MaterialTheme.colorScheme.outlineVariant),
                        enabled = !isUploadingPan
                    ) {
                        Icon(if (panImageUri != null) Icons.Default.Check else Icons.Default.Image, null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (panImageUri != null) "PAN Card Image Selected" else "Select PAN Card Image")
                    }
                    if (panImageUri != null) {
                        Button(
                            onClick = { onUploadDocument("PAN", panImageUri!!) },
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary),
                            enabled = !isUploadingPan
                        ) {
                            if (isUploadingPan) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Uploading...")
                            } else {
                                Text("Upload & Save PAN", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (isAadhaarDone) {
                    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CheckCircle, null, tint = Emerald500)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Aadhaar Card Uploaded ✓", color = Emerald500, fontWeight = FontWeight.SemiBold)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedButton(
                            onClick = { aadhaarLauncher.launch("image/*") },
                            modifier = Modifier.fillMaxWidth().height(38.dp),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Blue400),
                            border = BorderStroke(1.dp, Blue400.copy(alpha = 0.4f)),
                            enabled = !isUploadingAadhaar
                        ) {
                            Icon(Icons.Default.UploadFile, null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (aadhaarImageUri != null) "New Aadhaar Image Selected" else "Replace / Re-upload Aadhaar", fontSize = 12.sp)
                        }
                        if (aadhaarImageUri != null) {
                            Button(
                                onClick = { onUploadDocument("AADHAAR", aadhaarImageUri!!) },
                                modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                enabled = !isUploadingAadhaar
                            ) {
                                if (isUploadingAadhaar) {
                                    CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Uploading...", fontSize = 12.sp)
                                } else {
                                    Text("Confirm & Upload New Aadhaar", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                } else {
                    OutlinedButton(
                        onClick = { aadhaarLauncher.launch("image/*") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = if (aadhaarImageUri != null) Emerald500 else MaterialTheme.colorScheme.onSurface),
                        border = BorderStroke(1.dp, if (aadhaarImageUri != null) Emerald500 else MaterialTheme.colorScheme.outlineVariant),
                        enabled = !isUploadingAadhaar
                    ) {
                        Icon(if (aadhaarImageUri != null) Icons.Default.Check else Icons.Default.Image, null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (aadhaarImageUri != null) "Aadhaar Card Image Selected" else "Select Aadhaar Card Image")
                    }
                    if (aadhaarImageUri != null) {
                        Button(
                            onClick = { onUploadDocument("AADHAAR", aadhaarImageUri!!) },
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary),
                            enabled = !isUploadingAadhaar
                        ) {
                            if (isUploadingAadhaar) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Uploading...")
                            } else {
                                Text("Upload & Save Aadhaar", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onFinish,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary),
            enabled = isDigiLockerDone && isSelfieDone && isPanDone && isAadhaarDone
        ) {
            Text(if (isReKycMode) "Update & Save Re-KYC" else "Complete KYC & Go to Dashboard", fontWeight = FontWeight.Bold)
        }

        if (isReKycMode) {
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedButton(
                onClick = { showResetDialog = true },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, Red400.copy(alpha = 0.5f)),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Red400)
            ) {
                Icon(Icons.Default.DeleteOutline, null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Reset All Documents & Start Fresh", fontWeight = FontWeight.SemiBold)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedButton(
            onClick = onSkip,
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurfaceVariant)
        ) {
            Text("Skip for Now (Explore App)", fontWeight = FontWeight.SemiBold)
        }
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            icon = { Icon(Icons.Default.Warning, null, tint = Red400) },
            title = { Text("Reset KYC Records?") },
            text = { Text("This will reset your Aadhaar, PAN, and Selfie verification so you can re-submit all documents cleanly.") },
            confirmButton = {
                Button(
                    onClick = {
                        showResetDialog = false
                        onResetKyc()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Red400, contentColor = Color.White)
                ) {
                    Text("Confirm Reset")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}


