package com.loanzo.app.ui.auth

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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
    onFinish: () -> Unit
) {
    val context = LocalContext.current
    var selfieBitmap by remember { mutableStateOf<Bitmap?>(null) }

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
            .background(Brush.verticalGradient(colors = listOf(Navy900, Navy800)))
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
            text = "Complete your verified credentials to proceed",
            style = MaterialTheme.typography.bodyMedium,
            color = Gray400
        )

        Spacer(modifier = Modifier.height(24.dp))

        // 1. DIGILOCKER SECTION (GOVT OF INDIA)
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Navy700.copy(alpha = 0.9f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
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
                        Column {
                            Text("DigiLocker Official KYC", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
                            Text("Govt of India • MeitY", style = MaterialTheme.typography.labelSmall, color = Emerald400)
                        }
                    }
                    if (isDigiLockerDone) {
                        Icon(Icons.Default.CheckCircle, "Verified", tint = Emerald400)
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text("Direct live verification via official UIDAI Aadhaar OTP and Income Tax Department PAN Card records.", style = MaterialTheme.typography.bodySmall, color = Gray300)
                
                if (isDigiLockerDone) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Emerald400.copy(alpha = 0.15f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Emerald400.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.CheckCircle, null, tint = Emerald400, modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("DigiLocker Verified ✓", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = Emerald400)
                                Text(
                                    text = if (!user?.name.isNullOrBlank()) "Linked to: ${user?.name}" else "Aadhaar & PAN matched with Govt Records",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White
                                )
                            }
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

        // 2. SELFIE UPLOAD CARD
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Navy700.copy(alpha = 0.9f)),
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
                            Text("Selfie Photo", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
                            Text("Identity Verification", style = MaterialTheme.typography.labelSmall, color = Gold500)
                        }
                    }
                    if (isSelfieDone) {
                        Icon(Icons.Default.CheckCircle, "Verified", tint = Emerald400)
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
                            colors = ButtonDefaults.buttonColors(containerColor = Gold500, contentColor = Navy900),
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !isUploadingSelfie
                        ) {
                            if (isUploadingSelfie) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Navy900, strokeWidth = 2.dp)
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
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Gray500),
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
                        colors = ButtonDefaults.outlinedButtonColors(containerColor = Navy600.copy(alpha = 0.5f), contentColor = Color.White),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Gray600)
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
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 3. DOCUMENT UPLOAD CARD
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Navy700.copy(alpha = 0.9f)),
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
                            Text("Document Upload (Image)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
                            Text("PAN and Aadhaar Cards", style = MaterialTheme.typography.labelSmall, color = Blue500)
                        }
                    }
                    if (isPanDone && isAadhaarDone) {
                        Icon(Icons.Default.CheckCircle, "Verified", tint = Emerald400)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (isPanDone) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                        Icon(Icons.Default.CheckCircle, null, tint = Emerald400)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("PAN Card Uploaded", color = Emerald400, fontWeight = FontWeight.SemiBold)
                    }
                } else {
                    OutlinedButton(
                        onClick = { panLauncher.launch("image/*") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = if (panImageUri != null) Emerald400 else Color.White),
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (panImageUri != null) Emerald400 else Gray500),
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
                            colors = ButtonDefaults.buttonColors(containerColor = Blue500, contentColor = Color.White),
                            enabled = !isUploadingPan
                        ) {
                            if (isUploadingPan) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
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
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                        Icon(Icons.Default.CheckCircle, null, tint = Emerald400)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Aadhaar Card Uploaded", color = Emerald400, fontWeight = FontWeight.SemiBold)
                    }
                } else {
                    OutlinedButton(
                        onClick = { aadhaarLauncher.launch("image/*") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = if (aadhaarImageUri != null) Emerald400 else Color.White),
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (aadhaarImageUri != null) Emerald400 else Gray500),
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
                            colors = ButtonDefaults.buttonColors(containerColor = Blue500, contentColor = Color.White),
                            enabled = !isUploadingAadhaar
                        ) {
                            if (isUploadingAadhaar) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
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
            colors = ButtonDefaults.buttonColors(containerColor = Emerald400, contentColor = Navy900),
            enabled = isDigiLockerDone && isSelfieDone && isPanDone && isAadhaarDone
        ) {
            Text("Complete KYC & Go to Dashboard", fontWeight = FontWeight.Bold)
        }
    }
}


