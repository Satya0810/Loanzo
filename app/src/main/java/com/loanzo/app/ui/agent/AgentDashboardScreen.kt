package com.loanzo.app.ui.agent

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.loanzo.app.data.entity.AgentVisitEntity
import com.loanzo.app.data.entity.UserEntity
import com.loanzo.app.ui.components.LoanzoAvatar
import com.loanzo.app.ui.theme.*
import com.loanzo.app.util.isSuperAdmin
import com.loanzo.app.util.toInrString

/**
 * Daylight Enterprise Field Operations Dashboard for Loanzo Certified Officers.
 * Designed for high legibility under outdoor sunlight conditions on Android devices.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AgentDashboardScreen(
    user: UserEntity?,
    visits: List<AgentVisitEntity>,
    onToggleDutyStatus: (Boolean) -> Unit,
    onCompleteVisit: (
        visitId: String,
        remarks: String,
        isCollateralAuthentic: Boolean,
        isBorrowerVerified: Boolean,
        isLenderVerified: Boolean,
        photoProof: String
    ) -> Unit,
    onUpdateVisitStage: (visitId: String, stage: String) -> Unit = { _, _ -> },
    onNavigateToChat: (channelId: String, loanId: String?, targetUserId: String?) -> Unit = { _, _, _ -> },
    onSwitchToConsumer: () -> Unit = {},
    onSwitchToAdmin: () -> Unit = {},
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    val isSuperAdmin = user.isSuperAdmin()
    var showRoleSwitchDialog by remember { mutableStateOf(false) }
    var selectedFilter by remember { mutableStateOf("ALL") }
    var activeInspectionVisit by remember { mutableStateOf<AgentVisitEntity?>(null) }
    var showPayoutSuccessDialog by remember { mutableStateOf<Double?>(null) }
    var showSosConfirmationDialog by remember { mutableStateOf(false) }

    val isOnDuty = user?.isOnDuty ?: true
    val totalEarnings = user?.totalAgentEarnings ?: 0.0

    // Daylight Enterprise Palette (Non-AI, High-Contrast Outdoors)
    val pageBackground = Color(0xFFF8FAFC) // Light Slate
    val surfaceCard = Color.White
    val borderNormal = Color(0xFFE2E8F0)
    val textPrimary = Color(0xFF0F172A) // Deep Slate
    val textSecondary = Color(0xFF64748B) // Slate 500
    val textMuted = Color(0xFF94A3B8)
    val emeraldOfficial = Color(0xFF059669) // Emerald 600
    val amberSecurity = Color(0xFFB45309) // Amber 700

    val todayVisits = remember(visits) {
        visits.filter { it.scheduledDate.equals("Today", ignoreCase = true) }
    }
    val completedVisitsCount = remember(visits) {
        visits.count { it.status == "COMPLETED" }
    }

    val filteredVisits = remember(visits, selectedFilter) {
        when (selectedFilter) {
            "COLLATERAL" -> visits.filter { it.visitType == "COLLATERAL_VERIFICATION" }
            "BORROWER" -> visits.filter { it.visitType == "BORROWER_VERIFICATION" }
            "LENDER" -> visits.filter { it.visitType == "LENDER_VERIFICATION" }
            "COMPLETED" -> visits.filter { it.status == "COMPLETED" }
            else -> visits
        }
    }

    Scaffold(
        containerColor = pageBackground,
        topBar = {
            Surface(
                color = surfaceCard,
                border = BorderStroke(1.dp, borderNormal)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        LoanzoAvatar(
                            user = user,
                            size = 40.dp,
                            showVerifiedBadge = true,
                            borderColor = emeraldOfficial,
                            borderWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = emeraldOfficial.copy(alpha = 0.12f)
                                ) {
                                    Text(
                                        text = "OFFICIAL FIELD OFFICER",
                                        color = emeraldOfficial,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                if (isOnDuty) {
                                    Box(
                                        modifier = Modifier
                                            .size(7.dp)
                                            .clip(CircleShape)
                                            .background(emeraldOfficial)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = user?.name?.ifBlank { "Loanzo Officer" } ?: "Loanzo Officer",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = textPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (isSuperAdmin) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFFF1F5F9),
                                border = BorderStroke(1.dp, borderNormal),
                                modifier = Modifier.clickable { showRoleSwitchDialog = true }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text("👑", fontSize = 11.sp)
                                    Text(
                                        text = "Role",
                                        color = textPrimary,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                        }

                        IconButton(
                            onClick = onLogout,
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFF1F5F9))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Logout,
                                contentDescription = "Sign Out",
                                tint = textSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. Shift & GPS Operations Bar
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = surfaceCard),
                    border = BorderStroke(1.dp, if (isOnDuty) emeraldOfficial.copy(alpha = 0.3f) else borderNormal),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .clip(CircleShape)
                                            .background(if (isOnDuty) emeraldOfficial else Color(0xFFEA580C))
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (isOnDuty) "ACTIVE ON DUTY" else "SHIFT PAUSED (ON BREAK)",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = if (isOnDuty) emeraldOfficial else Color(0xFFEA580C)
                                    )
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = if (isOnDuty) "📍 Live GPS Broadcast Active (±4m Accuracy)" else "Emergency field dispatches temporarily paused",
                                    fontSize = 11.sp,
                                    color = textSecondary
                                )
                            }

                            Button(
                                onClick = { onToggleDutyStatus(!isOnDuty) },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isOnDuty) Color(0xFFF1F5F9) else emeraldOfficial,
                                    contentColor = if (isOnDuty) textPrimary else Color.White
                                ),
                                border = BorderStroke(1.dp, if (isOnDuty) borderNormal else emeraldOfficial),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Icon(
                                    imageVector = if (isOnDuty) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (isOnDuty) "Take Break" else "Go On Duty",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Daily Shift Metrics
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            MetricBox(
                                modifier = Modifier.weight(1f),
                                label = "Today's Stops",
                                value = "${todayVisits.size} Stops",
                                icon = Icons.Default.AltRoute,
                                iconColor = Color(0xFF0284C7)
                            )
                            MetricBox(
                                modifier = Modifier.weight(1.2f),
                                label = "Total Credited",
                                value = totalEarnings.toInrString(),
                                icon = Icons.Default.Payments,
                                iconColor = emeraldOfficial
                            )
                            MetricBox(
                                modifier = Modifier.weight(1f),
                                label = "Attested",
                                value = "$completedVisitsCount Done",
                                icon = Icons.Default.Verified,
                                iconColor = amberSecurity
                            )
                        }
                    }
                }
            }

            // 2. Emergency Safety & SOS Quick Trigger
            item {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFFFEF2F2),
                    border = BorderStroke(1.dp, Color(0xFFFCA5A5)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showSosConfirmationDialog = true }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Warning, null, tint = Color(0xFFDC2626), modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "Field Officer Safety SOS Hotline",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF991B1B)
                                )
                                Text(
                                    text = "Tap if facing on-ground dispute, safety threat or emergency assistance",
                                    fontSize = 10.sp,
                                    color = Color(0xFFB91C1C)
                                )
                            }
                        }
                        Icon(Icons.Default.ChevronRight, null, tint = Color(0xFFDC2626), modifier = Modifier.size(18.dp))
                    }
                }
            }

            // 3. Filter Row
            item {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Today's Route & Inspections",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = textPrimary
                        )
                        Text(
                            text = "${filteredVisits.size} Assigned",
                            fontSize = 12.sp,
                            color = textSecondary
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        item { DaylightFilterChip("ALL", "All Visits", selectedFilter == "ALL") { selectedFilter = "ALL" } }
                        item { DaylightFilterChip("COLLATERAL", "🏷️ Gold / Collateral", selectedFilter == "COLLATERAL") { selectedFilter = "COLLATERAL" } }
                        item { DaylightFilterChip("BORROWER", "🟢 Borrower KYC", selectedFilter == "BORROWER") { selectedFilter = "BORROWER" } }
                        item { DaylightFilterChip("LENDER", "🔵 Lender KYC", selectedFilter == "LENDER") { selectedFilter = "LENDER" } }
                        item { DaylightFilterChip("COMPLETED", "✅ Attested", selectedFilter == "COMPLETED") { selectedFilter = "COMPLETED" } }
                    }
                }
            }

            // 4. Visits Feed
            if (filteredVisits.isEmpty()) {
                item {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = surfaceCard,
                        border = BorderStroke(1.dp, borderNormal)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.AssignmentLate,
                                contentDescription = null,
                                tint = textMuted,
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "No Inspections Found",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = textPrimary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "New physical verification stops dispatched by the Master Admin will appear here in chronological order.",
                                fontSize = 11.sp,
                                color = textSecondary,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                itemsIndexed(filteredVisits, key = { _, visit -> visit.visitId }) { index, visit ->
                    DaylightVisitStopCard(
                        stopNumber = index + 1,
                        visit = visit,
                        onUpdateStage = { stage -> onUpdateVisitStage(visit.visitId, stage) },
                        onNavigateMaps = {
                            val uri = Uri.parse("geo:0,0?q=${Uri.encode(visit.targetAddress)}")
                            val mapIntent = Intent(Intent.ACTION_VIEW, uri).apply {
                                setPackage("com.google.android.apps.maps")
                            }
                            try {
                                context.startActivity(mapIntent)
                            } catch (_: Exception) {
                                val webMapIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://maps.google.com/?q=${Uri.encode(visit.targetAddress)}"))
                                context.startActivity(webMapIntent)
                            }
                        },
                        onCallBorrower = {
                            try {
                                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${visit.borrowerPhone}"))
                                context.startActivity(intent)
                            } catch (_: Exception) {
                                Toast.makeText(context, "Unable to launch dialer", Toast.LENGTH_SHORT).show()
                            }
                        },
                        onWhatsAppBorrower = {
                            try {
                                val cleanNumber = visit.borrowerPhone.replace("+", "").replace(" ", "")
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://api.whatsapp.com/send?phone=$cleanNumber&text=Hello%20${visit.borrowerName},%20I%20am%20the%20Loanzo%20Verification%20Officer%20scheduled%20for%20your%20verification."))
                                context.startActivity(intent)
                            } catch (_: Exception) {
                                Toast.makeText(context, "WhatsApp not installed", Toast.LENGTH_SHORT).show()
                            }
                        },
                        onChatBorrower = {
                            val channelId = if (visit.loanId.isNotBlank()) "chat_agent_${visit.loanId}" else "chat_agent_${visit.visitId}"
                            onNavigateToChat(channelId, visit.loanId.ifBlank { null }, null)
                        },
                        onStartInspection = {
                            activeInspectionVisit = visit
                        }
                    )
                }
            }
        }
    }

    // Modal Inspection Attestation Sheet
    activeInspectionVisit?.let { visit ->
        AgentInspectionSheet(
            visit = visit,
            onDismiss = { activeInspectionVisit = null },
            onCompleteInspection = { remarks, isCollateralAuthentic, isBorrowerVerified, isLenderVerified, photoProof ->
                onCompleteVisit(
                    visit.visitId,
                    remarks,
                    isCollateralAuthentic,
                    isBorrowerVerified,
                    isLenderVerified,
                    photoProof
                )
                val earned = visit.payoutAmount
                activeInspectionVisit = null
                showPayoutSuccessDialog = earned
            }
        )
    }

    // Payout Confirmation Dialog
    showPayoutSuccessDialog?.let { amount ->
        AlertDialog(
            onDismissRequest = { showPayoutSuccessDialog = null },
            containerColor = surfaceCard,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = emeraldOfficial,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Attestation Successfully Logged!",
                        color = textPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            },
            text = {
                Column {
                    Text(
                        text = "Physical verification report and proof have been attested and saved to immutable ledger.",
                        color = textSecondary,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = emeraldOfficial.copy(alpha = 0.12f),
                        border = BorderStroke(1.dp, emeraldOfficial.copy(alpha = 0.3f))
                    ) {
                        Text(
                            text = "+ ₹${amount.toInt()} Credited to Officer Balance",
                            color = emeraldOfficial,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showPayoutSuccessDialog = null },
                    colors = ButtonDefaults.buttonColors(containerColor = emeraldOfficial)
                ) {
                    Text("Continue Shift", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    // Emergency SOS Confirmation Dialog
    if (showSosConfirmationDialog) {
        AlertDialog(
            onDismissRequest = { showSosConfirmationDialog = false },
            containerColor = surfaceCard,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Emergency, null, tint = Color(0xFFDC2626), modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Emergency Field Assistance", color = Color(0xFF991B1B), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            },
            text = {
                Text(
                    text = "Do you want to immediately call the Loanzo Master Admin / Emergency Assistance team for support at your current location?",
                    fontSize = 13.sp,
                    color = textSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showSosConfirmationDialog = false
                        try {
                            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:+919876543210"))
                            context.startActivity(intent)
                        } catch (_: Exception) {
                            Toast.makeText(context, "Unable to dial helpline", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
                ) {
                    Text("Call Safety Hotline", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSosConfirmationDialog = false }) {
                    Text("Cancel", color = textSecondary)
                }
            }
        )
    }

    // 👑 Role Switcher Dialog for SuperAdmin
    if (showRoleSwitchDialog) {
        AlertDialog(
            onDismissRequest = { showRoleSwitchDialog = false },
            containerColor = surfaceCard,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("👑", fontSize = 18.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Operational View Switcher",
                        color = textPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFFF8FAFC),
                        border = BorderStroke(1.dp, borderNormal),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                showRoleSwitchDialog = false
                                onSwitchToConsumer()
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("📱", fontSize = 18.sp)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("Borrower / Lender View", color = textPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text("Switch to consumer loan application screens", color = textSecondary, fontSize = 11.sp)
                            }
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFFF8FAFC),
                        border = BorderStroke(1.dp, borderNormal),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                showRoleSwitchDialog = false
                                onSwitchToAdmin()
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("🛡️", fontSize = 18.sp)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("Master Admin Hub", color = textPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text("Dispatch engine, approvals & ledger oversight", color = textSecondary, fontSize = 11.sp)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showRoleSwitchDialog = false }) {
                    Text("Stay as Officer", color = textPrimary, fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}

@Composable
private fun MetricBox(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    icon: ImageVector,
    iconColor: Color
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = Color(0xFFF8FAFC),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(13.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = label,
                    fontSize = 10.sp,
                    color = Color(0xFF64748B),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                fontSize = 13.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFF0F172A),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun DaylightFilterChip(
    key: String,
    label: String,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = if (isSelected) Color(0xFF0F172A) else Color.White,
        border = BorderStroke(1.dp, if (isSelected) Color(0xFF0F172A) else Color(0xFFE2E8F0)),
        modifier = Modifier.clickable { onSelect() }
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) Color.White else Color(0xFF475569),
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            maxLines = 1,
            softWrap = false
        )
    }
}

@Composable
private fun DaylightVisitStopCard(
    stopNumber: Int,
    visit: AgentVisitEntity,
    onUpdateStage: (String) -> Unit,
    onNavigateMaps: () -> Unit,
    onCallBorrower: () -> Unit,
    onWhatsAppBorrower: () -> Unit,
    onChatBorrower: () -> Unit = {},
    onStartInspection: () -> Unit
) {
    val isCompleted = visit.status == "COMPLETED"
    val stageStatus = visit.visitStageStatus

    val typeColor = when (visit.visitType) {
        "COLLATERAL_VERIFICATION" -> Color(0xFFB45309)
        "BORROWER_VERIFICATION" -> Color(0xFF059669)
        else -> Color(0xFF0284C7)
    }

    val typeLabel = when (visit.visitType) {
        "COLLATERAL_VERIFICATION" -> "GOLD / ASSET APPRAISAL"
        "BORROWER_VERIFICATION" -> "BORROWER RESIDENCE KYC"
        else -> "LENDER IDENTITY AUDIT"
    }

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(
            1.dp,
            if (isCompleted) Color(0xFF059669).copy(alpha = 0.4f) else Color(0xFFE2E8F0)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Stop Header: Sequence badge + Type + Payout
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFF0F172A)
                    ) {
                        Text(
                            text = "STOP $stopNumber",
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = typeColor.copy(alpha = 0.12f)
                    ) {
                        Text(
                            text = typeLabel,
                            color = typeColor,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xFF059669).copy(alpha = 0.12f)
                ) {
                    Text(
                        text = "₹${visit.payoutAmount.toInt()} Bounty",
                        color = Color(0xFF059669),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = visit.title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0F172A),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Time & Distance Tag
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Schedule, null, tint = Color(0xFF64748B), modifier = Modifier.size(13.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "${visit.scheduledDate} • ${visit.scheduledTimeSlot}",
                    fontSize = 11.sp,
                    color = Color(0xFF64748B)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Icon(Icons.Default.DirectionsCar, null, tint = Color(0xFF0284C7), modifier = Modifier.size(13.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "${visit.distanceKm ?: 3.5} km away",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF0284C7)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Daylight Navigation Address Box
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFFF8FAFC),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Place, null, tint = Color(0xFFEF4444), modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = visit.targetAddress,
                        fontSize = 12.sp,
                        color = Color(0xFF1E293B),
                        modifier = Modifier.weight(1f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Button(
                        onClick = onNavigateMaps,
                        shape = RoundedCornerShape(6.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Icon(Icons.Default.Navigation, null, modifier = Modifier.size(13.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Maps", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Counterparty Contact Strip
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFFF8FAFC),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Counterparty: ${visit.borrowerName}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = visit.borrowerPhone,
                            fontSize = 11.sp,
                            color = Color(0xFF64748B)
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        IconButton(
                            onClick = onCallBorrower,
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(Color.White)
                                .border(1.dp, Color(0xFFCBD5E1), CircleShape)
                        ) {
                            Icon(Icons.Default.Phone, "Call", tint = Color(0xFF0284C7), modifier = Modifier.size(15.dp))
                        }

                        IconButton(
                            onClick = onWhatsAppBorrower,
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(Color.White)
                                .border(1.dp, Color(0xFFCBD5E1), CircleShape)
                        ) {
                            Icon(Icons.Default.Chat, "WhatsApp", tint = Color(0xFF059669), modifier = Modifier.size(15.dp))
                        }

                        IconButton(
                            onClick = onChatBorrower,
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(Color.White)
                                .border(1.dp, Color(0xFFCBD5E1), CircleShape)
                        ) {
                            Icon(Icons.Default.QuestionAnswer, "In-App Chat", tint = Color(0xFF7C3AED), modifier = Modifier.size(15.dp))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Stage Action Button
            if (isCompleted) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF059669).copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, Color(0xFF059669).copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 9.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF059669), modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Inspection Completed & Verified",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF059669)
                        )
                    }
                }
            } else {
                when (stageStatus) {
                    "SCHEDULED", "DISPATCHED" -> {
                        Button(
                            onClick = { onUpdateStage("EN_ROUTE") },
                            modifier = Modifier.fillMaxWidth().height(42.dp),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7))
                        ) {
                            Icon(Icons.Default.DirectionsBike, null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Start Route / Go En Route ➔", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    "EN_ROUTE" -> {
                        Button(
                            onClick = { onUpdateStage("ARRIVED") },
                            modifier = Modifier.fillMaxWidth().height(42.dp),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEA580C))
                        ) {
                            Icon(Icons.Default.LocationOn, null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Mark Arrived at Doorstep 📍", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    else -> {
                        Button(
                            onClick = onStartInspection,
                            modifier = Modifier.fillMaxWidth().height(42.dp),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669))
                        ) {
                            Icon(Icons.Default.VpnKey, null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Enter PIN & Complete Inspection 📋", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
