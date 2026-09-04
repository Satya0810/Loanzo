package com.loanzo.app.ui.agent

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.loanzo.app.data.entity.AgentVisitEntity
import com.loanzo.app.data.entity.UserEntity
import com.loanzo.app.ui.components.LoanzoAvatar
import com.loanzo.app.util.isSuperAdmin

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

    val isOnDuty = user?.isOnDuty ?: true
    val totalEarnings = user?.totalAgentEarnings ?: 0.0

    val darkBg = Color(0xFF0D1117)
    val cardBg = Color(0xFF161B22)
    val borderColor = Color(0xFF30363D)
    val goldAccent = Color(0xFFFFB800)
    val emeraldAccent = Color(0xFF10B981)

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
        topBar = {
            Surface(
                color = Color(0xFF161B22),
                border = androidx.compose.foundation.BorderStroke(1.dp, borderColor)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 18.dp, vertical = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            LoanzoAvatar(
                                user = user,
                                size = 44.dp,
                                showVerifiedBadge = true,
                                borderColor = goldAccent,
                                borderWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = goldAccent.copy(alpha = 0.2f),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, goldAccent.copy(alpha = 0.4f))
                                ) {
                                    Text(
                                        text = "CERTIFIED FIELD OFFICER",
                                        color = goldAccent,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = user?.name?.ifBlank { "Loanzo Agent" } ?: "Loanzo Agent",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        // Top right actions: Super Admin Role Switcher + Logout
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (isSuperAdmin) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = goldAccent.copy(alpha = 0.18f),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, goldAccent.copy(alpha = 0.5f)),
                                    modifier = Modifier
                                        .clickable { showRoleSwitchDialog = true }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text("👑", fontSize = 12.sp)
                                        Text(
                                            text = "Role",
                                            color = goldAccent,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            softWrap = false
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(4.dp))
                            }

                            // Logout action
                            IconButton(onClick = onLogout) {
                                Icon(
                                    imageVector = Icons.Default.Logout,
                                    contentDescription = "Sign Out",
                                    tint = Color(0xFF8B949E)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Duty Status Switcher Bar
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        color = if (isOnDuty) Color(0xFF0F2E1E) else Color(0xFF2E1C0F),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isOnDuty) emeraldAccent.copy(alpha = 0.5f) else Color(0xFFF97316).copy(alpha = 0.5f)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier.weight(1f, fill = false),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(if (isOnDuty) emeraldAccent else Color(0xFFF97316))
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (isOnDuty) "Status: ON DUTY (Active)" else "Status: ON BREAK / OFF",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isOnDuty) emeraldAccent else Color(0xFFF97316),
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            Button(
                                onClick = { onToggleDutyStatus(!isOnDuty) },
                                shape = RoundedCornerShape(6.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isOnDuty) Color(0xFF374151) else emeraldAccent
                                ),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier.height(28.dp)
                            ) {
                                Text(
                                    text = if (isOnDuty) "Ask for Break" else "Go On Duty",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isOnDuty) Color.White else Color.Black,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                    }
                }
            }
        },
        containerColor = darkBg
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(vertical = 14.dp)
        ) {
            // Off Duty Alert Banner
            if (!isOnDuty) {
                item {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 14.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF26180B),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF97316).copy(alpha = 0.4f))
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Coffee,
                                contentDescription = null,
                                tint = Color(0xFFF97316),
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Break Active — Duty Paused",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    maxLines = 1,
                                    softWrap = false
                                )
                                Text(
                                    text = "You will not receive new emergency field verification requests while on break.",
                                    fontSize = 11.sp,
                                    color = Color(0xFFD1D5DB),
                                    lineHeight = 15.sp
                                )
                            }
                        }
                    }
                }
            }

            // Stats Summary Card
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StatCard(
                        modifier = Modifier.weight(1f),
                        title = "Today's Visits",
                        value = "${todayVisits.size}",
                        icon = Icons.Default.EventNote,
                        iconColor = Color(0xFF38BDF8)
                    )
                    StatCard(
                        modifier = Modifier.weight(1f),
                        title = "Money Earned",
                        value = "₹${totalEarnings.toInt()}",
                        icon = Icons.Default.Payments,
                        iconColor = emeraldAccent
                    )
                    StatCard(
                        modifier = Modifier.weight(1f),
                        title = "Completed",
                        value = "$completedVisitsCount",
                        icon = Icons.Default.TaskAlt,
                        iconColor = goldAccent
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(18.dp)) }

            // Filter Chips
            item {
                Text(
                    text = "Scheduled Field Inspections",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(10.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item { FilterChipItem("ALL", "All Visits", selectedFilter == "ALL") { selectedFilter = "ALL" } }
                    item { FilterChipItem("COLLATERAL", "🏷️ Collateral", selectedFilter == "COLLATERAL") { selectedFilter = "COLLATERAL" } }
                    item { FilterChipItem("BORROWER", "🟢 Borrower", selectedFilter == "BORROWER") { selectedFilter = "BORROWER" } }
                    item { FilterChipItem("LENDER", "🔵 Lender", selectedFilter == "LENDER") { selectedFilter = "LENDER" } }
                    item { FilterChipItem("COMPLETED", "✅ Completed", selectedFilter == "COMPLETED") { selectedFilter = "COMPLETED" } }
                }
                Spacer(modifier = Modifier.height(14.dp))
            }

            // Visits Feed
            if (filteredVisits.isEmpty()) {
                item {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 30.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = cardBg,
                        border = androidx.compose.foundation.BorderStroke(1.dp, borderColor)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.AssignmentLate,
                                contentDescription = null,
                                tint = Color(0xFF6B7280),
                                modifier = Modifier.size(40.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "No Inspections Found",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "New physical verification visits dispatched by Master Admin will appear here.",
                                fontSize = 12.sp,
                                color = Color(0xFF9CA3AF),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                items(filteredVisits, key = { it.visitId }) { visit ->
                    AgentVisitCard(
                        visit = visit,
                        onNavigateMaps = {
                            val uri = Uri.parse("geo:0,0?q=${Uri.encode(visit.targetAddress)}")
                            val mapIntent = Intent(Intent.ACTION_VIEW, uri)
                            mapIntent.setPackage("com.google.android.apps.maps")
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
                                android.widget.Toast.makeText(context, "Unable to launch dialer", android.widget.Toast.LENGTH_SHORT).show()
                            }
                        },
                        onWhatsAppBorrower = {
                            try {
                                val cleanNumber = visit.borrowerPhone.replace("+", "").replace(" ", "")
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://api.whatsapp.com/send?phone=$cleanNumber&text=Hello%20${visit.borrowerName},%20I%20am%20the%20Loanzo%20Verification%20Officer%20scheduled%20for%20your%20loan%20verification."))
                                context.startActivity(intent)
                            } catch (_: Exception) {
                                android.widget.Toast.makeText(context, "WhatsApp is not installed on this device", android.widget.Toast.LENGTH_SHORT).show()
                            }
                        },
                        onCallLender = {
                            try {
                                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${visit.lenderPhone}"))
                                context.startActivity(intent)
                            } catch (_: Exception) {
                                android.widget.Toast.makeText(context, "Unable to launch dialer", android.widget.Toast.LENGTH_SHORT).show()
                            }
                        },
                        onWhatsAppLender = {
                            try {
                                val cleanNumber = visit.lenderPhone.replace("+", "").replace(" ", "")
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://api.whatsapp.com/send?phone=$cleanNumber&text=Hello%20${visit.lenderName},%20I%20am%20the%20Loanzo%20Verification%20Officer%20scheduled%20for%20your%20loan%20verification."))
                                context.startActivity(intent)
                            } catch (_: Exception) {
                                android.widget.Toast.makeText(context, "WhatsApp is not installed on this device", android.widget.Toast.LENGTH_SHORT).show()
                            }
                        },
                        onStartInspection = {
                            activeInspectionVisit = visit
                        }
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                }
            }
        }
    }

    // Modal Inspection Sheet
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
            containerColor = Color(0xFF161B22),
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = emeraldAccent,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Inspection Verified!",
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            text = {
                Column {
                    Text(
                        text = "Field inspection report successfully logged and cryptographically attested.",
                        color = Color(0xFFD1D5DB),
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = emeraldAccent.copy(alpha = 0.15f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, emeraldAccent.copy(alpha = 0.4f))
                    ) {
                        Text(
                            text = "+ ₹${amount.toInt()} Credited to Agent Balance",
                            color = emeraldAccent,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showPayoutSuccessDialog = null },
                    colors = ButtonDefaults.buttonColors(containerColor = emeraldAccent)
                ) {
                    Text(
                        text = "Continue",
                        color = Color.Black,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        )
    }

    // 👑 Super Admin Role Switcher Dialog
    if (showRoleSwitchDialog) {
        AlertDialog(
            onDismissRequest = { showRoleSwitchDialog = false },
            containerColor = Color(0xFF161B22),
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("👑", fontSize = 18.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Master Role Switcher",
                        color = goldAccent,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            },
            text = {
                Column {
                    Text(
                        text = "Hello @${user?.username?.ifBlank { "satyam0810" } ?: "satyam0810"}, switch your operational view instantly:",
                        color = Color(0xFFD1D5DB),
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    // Option 1: Switch to Consumer Member Dashboard
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF21262D),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF30363D)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                showRoleSwitchDialog = false
                                onSwitchToConsumer()
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("👤", fontSize = 20.sp)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Normal Member Dashboard",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = "P2P loans, wallet, marketplace feed",
                                    color = Color(0xFF8B949E),
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Option 2: Switch to Master Admin Hub
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF21262D),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF30363D)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                showRoleSwitchDialog = false
                                onSwitchToAdmin()
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("🛡️", fontSize = 20.sp)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Master Admin Hub",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = "Empanelment approvals & verification tokens",
                                    color = Color(0xFF8B949E),
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showRoleSwitchDialog = false }) {
                    Text("Stay as Agent", color = goldAccent)
                }
            }
        )
    }
}

@Composable
private fun StatCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    icon: ImageVector,
    iconColor: Color
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFF161B22),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF30363D))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = value,
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                maxLines = 1,
                softWrap = false
            )
            Text(
                text = title,
                fontSize = 10.sp,
                color = Color(0xFF8B949E),
                maxLines = 1,
                softWrap = false
            )
        }
    }
}

@Composable
private fun FilterChipItem(
    key: String,
    label: String,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = if (isSelected) Color(0xFFFFB800) else Color(0xFF161B22),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isSelected) Color(0xFFFFB800) else Color(0xFF30363D)
        ),
        modifier = Modifier.clickable { onSelect() }
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) Color.Black else Color(0xFFD1D5DB),
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            maxLines = 1,
            softWrap = false
        )
    }
}

@Composable
private fun AgentVisitCard(
    visit: AgentVisitEntity,
    onNavigateMaps: () -> Unit,
    onCallBorrower: () -> Unit,
    onWhatsAppBorrower: () -> Unit,
    onCallLender: () -> Unit,
    onWhatsAppLender: () -> Unit,
    onStartInspection: () -> Unit
) {
    val cardBg = Color(0xFF161B22)
    val borderColor = Color(0xFF30363D)
    val goldAccent = Color(0xFFFFB800)
    val emeraldAccent = Color(0xFF10B981)

    val isCompleted = visit.status == "COMPLETED"

    val typeBadgeColor = when (visit.visitType) {
        "COLLATERAL_VERIFICATION" -> goldAccent
        "BORROWER_VERIFICATION" -> emeraldAccent
        else -> Color(0xFF38BDF8)
    }

    val typeBadgeText = when (visit.visitType) {
        "COLLATERAL_VERIFICATION" -> "COLLATERAL VALUATION"
        "BORROWER_VERIFICATION" -> "BORROWER PHYSICAL KYC"
        else -> "LENDER PHYSICAL KYC"
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = cardBg,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isCompleted) emeraldAccent.copy(alpha = 0.5f) else borderColor
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Row: Type Badge + Payout Tag
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = typeBadgeColor.copy(alpha = 0.15f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, typeBadgeColor.copy(alpha = 0.4f))
                ) {
                    Text(
                        text = typeBadgeText,
                        color = typeBadgeColor,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        maxLines = 1,
                        softWrap = false
                    )
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = emeraldAccent.copy(alpha = 0.15f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, emeraldAccent.copy(alpha = 0.4f))
                ) {
                    Text(
                        text = "Earn ₹${visit.payoutAmount.toInt()}",
                        color = emeraldAccent,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = visit.title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Time Slot & Date
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Schedule,
                    contentDescription = null,
                    tint = Color(0xFF9CA3AF),
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "${visit.scheduledDate} • ${visit.scheduledTimeSlot}",
                    fontSize = 12.sp,
                    color = Color(0xFFD1D5DB),
                    maxLines = 1,
                    softWrap = false
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Address Box with Maps button
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFF0F141C),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF21262D))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Place,
                        contentDescription = null,
                        tint = Color(0xFFEF4444),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = visit.targetAddress,
                        fontSize = 12.sp,
                        color = Color(0xFFCBD5E1),
                        modifier = Modifier.weight(1f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    IconButton(
                        onClick = onNavigateMaps,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Navigation,
                            contentDescription = "Navigate Maps",
                            tint = Color(0xFF38BDF8),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Counterparty Contact Rows
            CounterpartyRow(
                roleLabel = "Borrower",
                name = visit.borrowerName,
                phone = visit.borrowerPhone,
                onCall = onCallBorrower,
                onWhatsApp = onWhatsAppBorrower
            )

            Spacer(modifier = Modifier.height(6.dp))

            CounterpartyRow(
                roleLabel = "Lender",
                name = visit.lenderName,
                phone = visit.lenderPhone,
                onCall = onCallLender,
                onWhatsApp = onWhatsAppLender
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Inspection Action Button
            if (isCompleted) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    color = emeraldAccent.copy(alpha = 0.15f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, emeraldAccent.copy(alpha = 0.4f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 10.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = emeraldAccent,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Inspection Completed & Verified",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = emeraldAccent
                        )
                    }
                }
            } else {
                Button(
                    onClick = onStartInspection,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = goldAccent)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.AssignmentTurnedIn,
                            contentDescription = null,
                            tint = Color.Black,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Start Physical Inspection",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CounterpartyRow(
    roleLabel: String,
    name: String,
    phone: String,
    onCall: () -> Unit,
    onWhatsApp: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(4.dp),
                color = Color(0xFF21262D)
            ) {
                Text(
                    text = roleLabel,
                    fontSize = 10.sp,
                    color = Color(0xFF8B949E),
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    maxLines = 1,
                    softWrap = false
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "$name ($phone)",
                fontSize = 12.sp,
                color = Color(0xFFE5E7EB),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            // Call Button
            Surface(
                shape = CircleShape,
                color = Color(0xFF1E293B),
                modifier = Modifier
                    .size(28.dp)
                    .clickable { onCall() }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Phone,
                        contentDescription = "Call",
                        tint = Color(0xFF38BDF8),
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            // WhatsApp Button
            Surface(
                shape = CircleShape,
                color = Color(0xFF143324),
                modifier = Modifier
                    .size(28.dp)
                    .clickable { onWhatsApp() }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Chat,
                        contentDescription = "WhatsApp",
                        tint = Color(0xFF10B981),
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}
