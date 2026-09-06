package com.loanzo.app.ui.components

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.loanzo.app.ui.theme.Emerald400
import com.loanzo.app.ui.theme.Gold500
import com.loanzo.app.ui.theme.Navy900
import com.loanzo.app.util.permissions.AppPermissionItem
import com.loanzo.app.util.permissions.AppPermissionManager

/**
 * Premium, bank-grade permissions rationale modal bottom sheet.
 * Explains transparently why Camera, Push Notifications, and Financial SMS
 * permissions are required pursuant to RBI P2P guidelines, and prompts system dialogs.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RequiredPermissionsDialog(
    onDismiss: () -> Unit,
    onAllGranted: () -> Unit = onDismiss
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var permissions by remember { mutableStateOf(AppPermissionManager.getAppPermissions(context)) }
    val allEssentialGranted = remember(permissions) {
        permissions.filter { it.isMandatory }.all { it.isGranted }
    }

    // Refresh permission states on resume (e.g. returning from System App Settings)
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                permissions = AppPermissionManager.getAppPermissions(context)
                if (permissions.filter { it.isMandatory }.all { it.isGranted }) {
                    // All essentials granted
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val multiplePermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        permissions = AppPermissionManager.getAppPermissions(context)
        if (permissions.filter { it.isMandatory }.all { it.isGranted }) {
            onAllGranted()
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header: Shield Badge + Title
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Surface(
                    shape = CircleShape,
                    color = Gold500.copy(alpha = 0.15f),
                    modifier = Modifier.size(48.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.Security,
                            contentDescription = null,
                            tint = Gold500,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Device Security & Permissions",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "RBI P2P Compliance & Identity Protection",
                        style = MaterialTheme.typography.bodySmall,
                        color = Gold500,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 11.sp
                    )
                }
            }

            Text(
                text = "Loanzo requests the following permissions to verify your identity, deliver instant repayment updates, and ensure safe peer-to-peer lending.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp,
                lineHeight = 16.sp
            )

            // Permission Items List
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                permissions.forEach { item ->
                    PermissionRowCard(item = item)
                }
            }

            // Zero-Trust Privacy Guarantee Card
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Lock,
                        contentDescription = null,
                        tint = Emerald400,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Your privacy is guaranteed with 256-bit encryption. Personal conversations, call logs, and sensitive data are never accessed.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp,
                        lineHeight = 15.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Action Buttons
            if (!allEssentialGranted) {
                Button(
                    onClick = {
                        val pending = AppPermissionManager.getPendingPermissionsList(context)
                        if (pending.isNotEmpty()) {
                            multiplePermissionLauncher.launch(pending)
                        } else {
                            onAllGranted()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Gold500,
                        contentColor = Navy900
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.VerifiedUser, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Grant Required Permissions", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }

                OutlinedButton(
                    onClick = { AppPermissionManager.openAppSettings(context) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(42.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Open System App Settings", fontSize = 12.sp)
                }

                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "Continue with Limited Features",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp
                    )
                }
            } else {
                Button(
                    onClick = onAllGranted,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Emerald400,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("All Clear • Continue to Loanzo", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
        }
    }
}

@Composable
private fun PermissionRowCard(item: AppPermissionItem) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
            1.dp,
            if (item.isGranted) Emerald400.copy(alpha = 0.4f) else MaterialTheme.colorScheme.outlineVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = if (item.isGranted) Emerald400.copy(alpha = 0.15f) else Gold500.copy(alpha = 0.15f),
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        item.icon,
                        contentDescription = null,
                        tint = if (item.isGranted) Emerald400 else Gold500,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 13.sp
                    )
                    if (item.isMandatory) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "• REQUIRED",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = Gold500
                        )
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = item.purpose,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp,
                    lineHeight = 15.sp
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Status Pill
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = if (item.isGranted) Emerald400.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant,
                border = BorderStroke(1.dp, if (item.isGranted) Emerald400.copy(alpha = 0.35f) else MaterialTheme.colorScheme.outlineVariant)
            ) {
                Text(
                    text = if (item.isGranted) "GRANTED ✓" else "ACTION",
                    color = if (item.isGranted) Emerald400 else Gold500,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}
