package com.loanzo.app.ui.admin

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.loanzo.app.R
import com.loanzo.app.data.entity.VerificationEntity
import com.loanzo.app.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppOwnerVerificationScreen(
    verifications: List<VerificationEntity>,
    onApproveVerification: (token: String, phone: String) -> Unit,
    onManualVerify: (String) -> Unit,
    onNavigateBack: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var manualTokenInput by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("ALL") } // ALL, PENDING, VERIFIED

    val context = androidx.compose.ui.platform.LocalContext.current
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions()
    ) { _ -> }

    LaunchedEffect(Unit) {
        val hasReceiveSms = androidx.core.content.ContextCompat.checkSelfPermission(
            context, 
            android.Manifest.permission.RECEIVE_SMS
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        if (!hasReceiveSms) {
            permissionLauncher.launch(
                arrayOf(
                    android.Manifest.permission.RECEIVE_SMS, 
                    android.Manifest.permission.READ_SMS
                )
            )
        }

        // Check if Notification Listener is enabled
        val enabledListeners = android.provider.Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
        val isNotificationAccessGranted = enabledListeners?.contains(context.packageName) == true
        if (!isNotificationAccessGranted) {
            android.widget.Toast.makeText(context, "Please enable Notification Access for WhatsApp Interception", android.widget.Toast.LENGTH_LONG).show()
            val intent = android.content.Intent(android.provider.Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
            context.startActivity(intent)
        }
    }

    val filteredList = remember(verifications, searchQuery, selectedFilter) {
        verifications.filter { item ->
            val matchesSearch = item.phone.contains(searchQuery, ignoreCase = true) ||
                    item.token.contains(searchQuery, ignoreCase = true) ||
                    item.username.contains(searchQuery, ignoreCase = true)
            val matchesFilter = when (selectedFilter) {
                "PENDING" -> item.status == "PENDING"
                "VERIFIED" -> item.status == "VERIFIED"
                else -> true
            }
            matchesSearch && matchesFilter
        }
    }

    val pendingCount = verifications.count { it.status == "PENDING" }
    val verifiedCount = verifications.count { it.status == "VERIFIED" }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = painterResource(id = R.drawable.app_logo),
                            contentDescription = "Loanzo Logo",
                            modifier = Modifier.size(32.dp).clip(RoundedCornerShape(8.dp))
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("App Owner Hub", fontWeight = FontWeight.Bold, fontSize = 17.sp, color = Color.White)
                            Text("+91 7061559039 (Master Admin)", fontSize = 11.sp, color = Gold500)
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Navy900)
            )
        },
        containerColor = Navy900
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(4.dp))
                // Live Verification Listener Status Card
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceDarkCard),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Gold500.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = CircleShape,
                                color = Emerald400.copy(alpha = 0.2f),
                                modifier = Modifier.size(42.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.Sensors, contentDescription = null, tint = Emerald400, modifier = Modifier.size(24.dp))
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Live Verification Listener", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                                Text("Active • Auto-verifies tokens", color = Emerald400, fontSize = 12.sp)
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Emerald400.copy(alpha = 0.12f),
                                modifier = Modifier.weight(1f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(Icons.Default.Sms, null, tint = Emerald400, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("SMS", color = Emerald400, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Emerald400.copy(alpha = 0.12f),
                                modifier = Modifier.weight(1f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.Message, null, tint = Emerald400, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("WhatsApp", color = Emerald400, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Emerald400.copy(alpha = 0.12f),
                                modifier = Modifier.weight(1f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(Icons.Default.Notifications, null, tint = Emerald400, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Firestore", color = Emerald400, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

            // Stats row
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Card(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = SurfaceDarkCard)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("Pending Requests", color = Gray400, fontSize = 11.sp)
                            Text("$pendingCount", fontWeight = FontWeight.Bold, color = Gold500, fontSize = 20.sp)
                        }
                    }
                    Card(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = SurfaceDarkCard)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("Total Verified", color = Gray400, fontSize = 11.sp)
                            Text("$verifiedCount", fontWeight = FontWeight.Bold, color = Emerald400, fontSize = 20.sp)
                        }
                    }
                }
            }

            // Quick Manual Approve Box
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceDarkCard),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text("Manual Token / Phone Verification", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            OutlinedTextField(
                                value = manualTokenInput,
                                onValueChange = { manualTokenInput = it },
                                placeholder = { Text("Enter 6-digit Code or Phone") },
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Gold500,
                                    cursorColor = Gold500,
                                    unfocusedBorderColor = Gray600
                                )
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    if (manualTokenInput.isNotBlank()) {
                                        onManualVerify(manualTokenInput.trim())
                                        manualTokenInput = ""
                                    }
                                },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Gold500, contentColor = Navy900),
                                enabled = manualTokenInput.isNotBlank()
                            ) {
                                Text("Verify", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Search and Filters
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    leadingIcon = { Icon(Icons.Default.Search, null, tint = Gray400) },
                    placeholder = { Text("Search phone number or token...") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Gold500,
                        cursorColor = Gold500,
                        unfocusedBorderColor = Gray600
                    )
                )
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = selectedFilter == "ALL",
                        onClick = { selectedFilter = "ALL" },
                        label = { Text("All (${verifications.size})") }
                    )
                    FilterChip(
                        selected = selectedFilter == "PENDING",
                        onClick = { selectedFilter = "PENDING" },
                        label = { Text("Pending ($pendingCount)") }
                    )
                    FilterChip(
                        selected = selectedFilter == "VERIFIED",
                        onClick = { selectedFilter = "VERIFIED" },
                        label = { Text("Verified ($verifiedCount)") }
                    )
                }
            }

            if (filteredList.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No verification requests found.", color = Gray400, fontSize = 14.sp)
                    }
                }
            } else {
                items(filteredList, key = { it.token }) { item ->
                    VerificationItemCard(
                        item = item,
                        onApprove = { onApproveVerification(item.token, item.phone) }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}

@Composable
private fun VerificationItemCard(
    item: VerificationEntity,
    onApprove: () -> Unit
) {
    val isVerified = item.status == "VERIFIED"
    val dateStr = remember(item.createdAt) {
        val sdf = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())
        sdf.format(Date(item.createdAt))
    }

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceDarkCard),
        border = androidx.compose.foundation.BorderStroke(1.dp, if (isVerified) Emerald400.copy(alpha = 0.4f) else Gold500.copy(alpha = 0.3f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = if (isVerified) Emerald400.copy(alpha = 0.15f) else Gold500.copy(alpha = 0.15f),
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    if (item.channel.contains("WHATSAPP", ignoreCase = true)) {
                        Icon(Icons.AutoMirrored.Filled.Message, null, tint = if (isVerified) Emerald400 else Gold500, modifier = Modifier.size(20.dp))
                    } else {
                        Icon(Icons.Default.Sms, null, tint = if (isVerified) Emerald400 else Gold500, modifier = Modifier.size(20.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(item.phone, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                    if (item.username.isNotBlank()) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("(@${item.username})", color = Gray400, fontSize = 12.sp)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (isVerified) Emerald400.copy(alpha = 0.2f) else Gold500.copy(alpha = 0.2f)
                    ) {
                        Text(
                            text = item.status,
                            color = if (isVerified) Emerald400 else Gold500,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Token: ", color = Gray400, fontSize = 12.sp)
                    Text(item.token, fontWeight = FontWeight.Bold, color = Gold500, fontSize = 13.sp)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("• $dateStr", color = Gray400, fontSize = 11.sp)
                }
            }

            if (!isVerified) {
                Button(
                    onClick = onApprove,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Emerald400, contentColor = Navy900),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text("Approve", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            } else {
                Icon(Icons.Default.CheckCircle, contentDescription = "Verified", tint = Emerald400, modifier = Modifier.size(22.dp))
            }
        }
    }
}
