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
import com.loanzo.app.ui.components.LoanzoText as Text
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

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        permissions = AppPermissionManager.getAppPermissions(context)
    }

    val pendingPermissions = remember(permissions) {
        permissions.filter { !it.isGranted }
    }

    var currentStepIndex by remember { mutableIntStateOf(0) }

    LaunchedEffect(pendingPermissions.size) {
        if (pendingPermissions.isEmpty()) {
            onAllGranted()
        } else if (currentStepIndex >= pendingPermissions.size) {
            currentStepIndex = (pendingPermissions.size - 1).coerceAtLeast(0)
        }
    }

    if (pendingPermissions.isEmpty()) {
        return
    }

    val safeIndex = currentStepIndex.coerceIn(0, pendingPermissions.size - 1)
    val currentItem = pendingPermissions[safeIndex]
    val totalSteps = pendingPermissions.size
    val progress = (safeIndex + 1).toFloat() / totalSteps.toFloat()

    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(
            usePlatformDefaultWidth = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 16.dp),
                border = BorderStroke(1.dp, Gold500.copy(alpha = 0.35f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Step Counter & Progress
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Gold500.copy(alpha = 0.15f),
                            border = BorderStroke(1.dp, Gold500.copy(alpha = 0.3f))
                        ) {
                            Text(
                                text = "Permission ${safeIndex + 1} of $totalSteps",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Gold500,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }

                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Close",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp),
                        color = Gold500,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // Prominent Icon Circle
                    Surface(
                        shape = CircleShape,
                        color = Gold500.copy(alpha = 0.15f),
                        border = BorderStroke(2.dp, Gold500.copy(alpha = 0.4f)),
                        modifier = Modifier.size(72.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = currentItem.icon,
                                contentDescription = null,
                                tint = Gold500,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    }

                    // Title
                    Text(
                        text = currentItem.title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )

                    // Purpose explanation
                    Text(
                        text = currentItem.purpose,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        lineHeight = 20.sp,
                        fontSize = 13.sp
                    )

                    // Zero-Knowledge / Security Guarantee
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Lock,
                                contentDescription = null,
                                tint = Emerald400,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Bank-grade 256-bit encryption. Zero data sharing under RBI Fair Practices Code.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.sp,
                                lineHeight = 14.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Primary Button: Allow Permission
                    Button(
                        onClick = {
                            permissionLauncher.launch(currentItem.permissions.toTypedArray())
                            if (safeIndex < totalSteps - 1) {
                                currentStepIndex = safeIndex + 1
                            } else {
                                permissions = AppPermissionManager.getAppPermissions(context)
                                if (permissions.filter { it.isMandatory }.all { it.isGranted }) {
                                    onAllGranted()
                                } else {
                                    onDismiss()
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Gold500,
                            contentColor = Navy900
                        ),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Allow ${currentItem.title.substringBefore(" Access")}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }

                    // Secondary Button: Skip or Next
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = {
                                if (safeIndex < totalSteps - 1) {
                                    currentStepIndex = safeIndex + 1
                                } else {
                                    onDismiss()
                                }
                            }
                        ) {
                            Text(
                                if (safeIndex < totalSteps - 1) "Skip for Now" else "Decide Later",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 12.sp
                            )
                        }

                        TextButton(
                            onClick = { AppPermissionManager.openAppSettings(context) }
                        ) {
                            Icon(
                                Icons.Default.Settings,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                "Settings",
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 12.sp
                            )
                        }
                    }
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
