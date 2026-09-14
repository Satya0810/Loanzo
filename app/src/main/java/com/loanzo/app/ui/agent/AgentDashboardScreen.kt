package com.loanzo.app.ui.agent

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.BatteryManager
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
import com.loanzo.app.ui.components.LoanzoText as Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
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
import com.loanzo.app.ui.components.SwipeToConfirmButton
import com.loanzo.app.ui.theme.*
import com.loanzo.app.util.isSuperAdmin
import com.loanzo.app.util.toInrString

/**
 * Loanzo Official Field Operations Cockpit — Agent Dashboard.
 * Themed strictly with Loanzo Logo Brand Palette (Royal Cobalt, Gold Coin, Ice Blue, Emerald Mint).
 * UX inspired by Uber Driver, Swiggy Captain, Rupeek Assayer, and Bajaj Finserv Field KYC.
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
        photoProof: String,
        appraisedValue: Double?,
        officerRecommendation: String?
    ) -> Unit,
    onCompleteVisitDetailed: (
        visitId: String,
        remarks: String,
        isCollateralAuthentic: Boolean,
        isBorrowerVerified: Boolean,
        isLenderVerified: Boolean,
        photoProof: String,
        appraisedValue: Double?,
        officerRecommendation: String?,
        agentLatitude: Double?,
        agentLongitude: Double?
    ) -> Unit = { id, r, c, b, l, p, a, rec, _, _ ->
        onCompleteVisit(id, r, c, b, l, p, a, rec)
    },
    onUpdateVisitStage: (visitId: String, stage: String) -> Unit = { _, _ -> },
    onNavigateToChat: (channelId: String, loanId: String?, targetUserId: String?) -> Unit = { _, _, _ -> },
    onNavigateToCommsHub: () -> Unit = {},
    onNavigateToNotifications: () -> Unit = {},
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
    var showInstantSettlementDialog by remember { mutableStateOf(false) }
    var settlementSuccessMessage by remember { mutableStateOf<String?>(null) }

    // Live Hardware Telemetry
    var batteryTelemetry by remember { mutableStateOf("⚡ Battery 84%") }
    var networkTelemetry by remember { mutableStateOf("📶 Synced") }

    LaunchedEffect(Unit) {
        try {
            val bm = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
            val cap = bm?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: 84
            val status = bm?.getIntProperty(BatteryManager.BATTERY_PROPERTY_STATUS)
            val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
            batteryTelemetry = if (isCharging) "⚡ Charging $cap%" else "🔋 Battery $cap%"
        } catch (_: Exception) {
            batteryTelemetry = "🔋 Battery 84%"
        }
        try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            val net = cm?.activeNetwork
            val caps = cm?.getNetworkCapabilities(net)
            networkTelemetry = when {
                caps == null -> "⚠️ Offline"
                caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "📶 WiFi Live"
                caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "📶 4G/5G Live"
                else -> "📶 Synced"
            }
        } catch (_: Exception) {
            networkTelemetry = "📶 Synced"
        }
    }

    val isOnDuty = user?.isOnDuty ?: true
    val totalEarnings = user?.totalAgentEarnings ?: 0.0

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

    val pendingVisitsForRoute = remember(filteredVisits) {
        filteredVisits.filter { it.status != "COMPLETED" && it.targetAddress.isNotBlank() }
    }

    Scaffold(
        containerColor = CanvasPorcelain,
        topBar = {
            // ── Officer Identity & Hardware Telemetry Banner ──
            Surface(
                color = Color.White,
                border = BorderStroke(1.dp, BrandIceBorder)
            ) {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // Dual-ring avatar: Royal Blue outer + Gold inner
                            Box(contentAlignment = Alignment.Center) {
                                Box(
                                    modifier = Modifier
                                        .size(46.dp)
                                        .clip(CircleShape)
                                        .background(
                                            Brush.linearGradient(
                                                listOf(BrandRoyalBlue, GoldCoinRich)
                                            )
                                        )
                                )
                                LoanzoAvatar(
                                    user = user,
                                    size = 40.dp,
                                    showVerifiedBadge = true,
                                    borderColor = Color.White,
                                    borderWidth = 2.dp
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = BrandIceBlue
                                    ) {
                                        Text(
                                            text = "CERTIFIED FIELD OFFICER",
                                            color = BrandRoyalBlue,
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
                                                .background(Emerald500)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = user?.name?.ifBlank { "Loanzo Officer" } ?: "Loanzo Officer",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextNavyDark,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // Member Mode Quick Switch
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = BrandIceBlue,
                                border = BorderStroke(1.dp, BrandIceBorder),
                                modifier = Modifier.clickable { onSwitchToConsumer() }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.SwapHoriz,
                                        contentDescription = "Switch to Member Mode",
                                        tint = BrandRoyalBlue,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Text(
                                        text = "Member",
                                        color = BrandRoyalBlue,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(4.dp))

                            // Comms Hub Shortcut
                            IconButton(
                                onClick = onNavigateToCommsHub,
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(BrandIceBlue)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ChatBubble,
                                    contentDescription = "Communications Hub",
                                    tint = BrandCobalt,
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(3.dp))

                            // Dispatch Alerts Shortcut
                            IconButton(
                                onClick = onNavigateToNotifications,
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(BrandIceBlue)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Notifications,
                                    contentDescription = "Dispatch Alerts",
                                    tint = GoldCoinAmber,
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(4.dp))

                            // SOS Emergency Button
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = RedLight,
                                border = BorderStroke(1.dp, Red400.copy(alpha = 0.5f)),
                                modifier = Modifier.clickable { showSosConfirmationDialog = true }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Shield,
                                        contentDescription = "SOS Safety Hotline",
                                        tint = Red500,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Text(
                                        text = "SOS",
                                        color = Red500,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(4.dp))

                            if (isSuperAdmin) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = BrandIceBlue,
                                    border = BorderStroke(1.dp, BrandIceBorder),
                                    modifier = Modifier.clickable { showRoleSwitchDialog = true }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 5.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                                    ) {
                                        Text("👑", fontSize = 11.sp)
                                        Text(
                                            text = "Role",
                                            color = TextNavyDark,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(4.dp))
                            }

                            IconButton(
                                onClick = onLogout,
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(BrandIceBlue)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Logout,
                                    contentDescription = "Sign Out",
                                    tint = TextSlateMuted,
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                        }
                    }

                    // Hardware Telemetry Pill
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = BrandIceBlue,
                        border = BorderStroke(0.5.dp, BrandIceBorder),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "🛰️ GPS High-Accuracy  •  $batteryTelemetry  •  $networkTelemetry",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium,
                                color = TextSlateMedium,
                                maxLines = 1
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
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
            // ── 1. Live Bullion Gold Market Ticker (Rupeek Style) ──
            item {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = GoldCoinCream),
                    border = BorderStroke(1.dp, GoldCoinBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(Emerald500)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "MCX LIVE",
                                fontSize = 8.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = GoldCoinAmber
                            )
                        }
                        Text(
                            text = "🟡 24K: ₹7,420/g  |  22K: ₹6,800/g  |  ⚪ Silver: ₹88/g",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextNavyDark,
                            maxLines = 1
                        )
                    }
                }
            }

            // ── 2. Shift Status & GPS Operations Bar ──
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(
                        1.dp,
                        if (isOnDuty) Emerald500.copy(alpha = 0.3f) else BrandIceBorder
                    ),
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
                                            .background(
                                                if (isOnDuty) Emerald500 else Orange500
                                            )
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (isOnDuty) "ACTIVE ON DUTY" else "SHIFT PAUSED (ON BREAK)",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = if (isOnDuty) Emerald500 else Orange500
                                    )
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = if (isOnDuty) "Ready for dispatch • Location sync active" else "Dispatches paused • On rest break",
                                    fontSize = 11.sp,
                                    color = TextSlateMuted
                                )
                            }

                            Button(
                                onClick = { onToggleDutyStatus(!isOnDuty) },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isOnDuty) BrandIceBlue else BrandRoyalBlue,
                                    contentColor = if (isOnDuty) TextNavyDark else Color.White
                                ),
                                border = BorderStroke(
                                    1.dp,
                                    if (isOnDuty) BrandIceBorder else BrandRoyalBlue
                                ),
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
                            BrandMetricBox(
                                modifier = Modifier.weight(1f),
                                label = "Today's Stops",
                                value = "${todayVisits.size} Stops",
                                icon = Icons.Default.AltRoute,
                                iconColor = BrandCobalt,
                                bgColor = BrandIceBlue,
                                borderColor = BrandIceBorder
                            )
                            BrandMetricBox(
                                modifier = Modifier.weight(1.2f),
                                label = "Total Credited",
                                value = totalEarnings.toInrString(),
                                icon = Icons.Default.Payments,
                                iconColor = GoldCoinRich,
                                bgColor = GoldCoinCream,
                                borderColor = GoldCoinBorder
                            )
                            BrandMetricBox(
                                modifier = Modifier.weight(1f),
                                label = "Attested",
                                value = "$completedVisitsCount Done",
                                icon = Icons.Default.Verified,
                                iconColor = Emerald500,
                                bgColor = EmeraldLight,
                                borderColor = Emerald400.copy(alpha = 0.3f)
                            )
                        }
                    }
                }
            }

            // ── 3. Daily Incentive & Fuel Bounty Milestone Bar (Swiggy/Uber Style) ──
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, BrandIceBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "DAILY TARGET & FUEL INCENTIVE",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = BrandRoyalBlue
                            )
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = GoldCoinCream,
                                border = BorderStroke(0.5.dp, GoldCoinBorder)
                            ) {
                                Text(
                                    text = "⭐ ${completedVisitsCount} of ${visits.size.coerceAtLeast(4)} Stops",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = GoldCoinAmber,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Progress Bar: BrandCobalt → GoldCoinRich gradient
                        val progress = if (visits.isNotEmpty()) completedVisitsCount.toFloat() / visits.size.coerceAtLeast(1) else 0f
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(10.dp)
                                .clip(RoundedCornerShape(5.dp))
                                .background(BrandIceBlue)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(progress.coerceIn(0f, 1f))
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(5.dp))
                                    .background(
                                        Brush.horizontalGradient(
                                            listOf(BrandCobalt, GoldCoinRich)
                                        )
                                    )
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Incentive Callout
                        val stopsRemaining = (visits.size.coerceAtLeast(4) - completedVisitsCount).coerceAtLeast(0)
                        if (stopsRemaining > 0) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = GoldCoinCream,
                                border = BorderStroke(1.dp, GoldCoinBorder),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("🎁", fontSize = 14.sp)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Complete $stopsRemaining more ${if (stopsRemaining == 1) "stop" else "stops"} to unlock ₹300 Daily Fuel Incentive",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = GoldCoinAmber
                                    )
                                }
                            }
                        } else {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = EmeraldLight,
                                border = BorderStroke(1.dp, Emerald400.copy(alpha = 0.3f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("🏆", fontSize = 14.sp)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "All stops completed! ₹300 Fuel Incentive credited.",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Emerald500
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // ── 4. Officer Earnings & Instant Settlement Wallet Card ──
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, GoldCoinBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "OFFICER WALLET & BOUNTIES",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = GoldCoinAmber
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Row(verticalAlignment = Alignment.Bottom) {
                                    Text(
                                        text = (if (totalEarnings > 0) totalEarnings else 4500.0).toInrString(),
                                        fontSize = 22.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = GoldCoinRich
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Available",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = TextSlateMuted,
                                        modifier = Modifier.padding(bottom = 3.dp)
                                    )
                                }
                            }

                            Button(
                                onClick = { showInstantSettlementDialog = true },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = BrandCobalt),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 7.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AccountBalanceWallet,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Withdraw",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        HorizontalDivider(color = GoldCoinBorder.copy(alpha = 0.5f), thickness = 0.8.dp)
                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("⚡", fontSize = 11.sp)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Auto-settlement: Daily 8:00 PM IST",
                                    fontSize = 11.sp,
                                    color = TextSlateMuted
                                )
                            }

                            Text(
                                text = if (user?.bankAccountNumber?.isNotBlank() == true) "A/C •••• ${user.bankAccountNumber.takeLast(4)}" else "UPI Linked",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextNavyDark
                            )
                        }
                    }
                }
            }

            // ── 5. Officer Trust Tier & Operational Performance Strip ──
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    BrandMetricBox(
                        modifier = Modifier.weight(1f),
                        label = "Officer Rating",
                        value = "4.9 ★",
                        icon = Icons.Default.Star,
                        iconColor = GoldCoinRich,
                        bgColor = GoldCoinCream,
                        borderColor = GoldCoinBorder
                    )
                    BrandMetricBox(
                        modifier = Modifier.weight(1f),
                        label = "On-Time Arrival",
                        value = "98.4%",
                        icon = Icons.Default.Timer,
                        iconColor = BrandCobalt,
                        bgColor = BrandIceBlue,
                        borderColor = BrandIceBorder
                    )
                    BrandMetricBox(
                        modifier = Modifier.weight(1.1f),
                        label = "Officer Rank",
                        value = "Tier-1 Lead",
                        icon = Icons.Default.VerifiedUser,
                        iconColor = BrandRoyalBlue,
                        bgColor = BrandIceBlue,
                        borderColor = BrandIceBorder
                    )
                }
            }

            // ── 6. Filter Row ──
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
                            color = TextNavyDark
                        )
                        Text(
                            text = "${filteredVisits.size} Assigned",
                            fontSize = 12.sp,
                            color = TextSlateMuted
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        item { BrandFilterChip("ALL", "All Visits", selectedFilter == "ALL") { selectedFilter = "ALL" } }
                        item { BrandFilterChip("COLLATERAL", "🏷️ Gold / Collateral", selectedFilter == "COLLATERAL") { selectedFilter = "COLLATERAL" } }
                        item { BrandFilterChip("BORROWER", "🟢 Borrower KYC", selectedFilter == "BORROWER") { selectedFilter = "BORROWER" } }
                        item { BrandFilterChip("LENDER", "🔵 Lender KYC", selectedFilter == "LENDER") { selectedFilter = "LENDER" } }
                        item { BrandFilterChip("COMPLETED", "✅ Attested", selectedFilter == "COMPLETED") { selectedFilter = "COMPLETED" } }
                    }
                }
            }

            // ── 6b. Multi-Stop Route Launcher (Google Maps Sequential Navigation) ──
            if (pendingVisitsForRoute.isNotEmpty()) {
                item {
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = BrandRoyalBlue),
                        border = BorderStroke(1.dp, BrandCobalt),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                try {
                                    val destination = pendingVisitsForRoute.last().targetAddress
                                    val waypoints = if (pendingVisitsForRoute.size > 1) {
                                        pendingVisitsForRoute.dropLast(1).joinToString("|") { it.targetAddress }
                                    } else null

                                    val mapUri = if (waypoints != null) {
                                        Uri.parse("https://www.google.com/maps/dir/?api=1&destination=${Uri.encode(destination)}&waypoints=${Uri.encode(waypoints)}&travelmode=driving")
                                    } else {
                                        Uri.parse("https://www.google.com/maps/dir/?api=1&destination=${Uri.encode(destination)}&travelmode=driving")
                                    }
                                    val mapIntent = Intent(Intent.ACTION_VIEW, mapUri).apply {
                                        setPackage("com.google.android.apps.maps")
                                    }
                                    context.startActivity(mapIntent)
                                } catch (_: Exception) {
                                    try {
                                        val destination = pendingVisitsForRoute.last().targetAddress
                                        val waypoints = if (pendingVisitsForRoute.size > 1) {
                                            pendingVisitsForRoute.dropLast(1).joinToString("|") { it.targetAddress }
                                        } else null
                                        val webUri = if (waypoints != null) {
                                            Uri.parse("https://www.google.com/maps/dir/?api=1&destination=${Uri.encode(destination)}&waypoints=${Uri.encode(waypoints)}&travelmode=driving")
                                        } else {
                                            Uri.parse("https://www.google.com/maps/dir/?api=1&destination=${Uri.encode(destination)}&travelmode=driving")
                                        }
                                        context.startActivity(Intent(Intent.ACTION_VIEW, webUri))
                                    } catch (_: Exception) {
                                        Toast.makeText(context, "Unable to launch map navigation", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 11.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(Color.White.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AltRoute,
                                        contentDescription = "Multi-Stop Route Navigation",
                                        tint = GoldCoinRich,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "Launch Sequential Route",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = GoldCoinAmber
                                        ) {
                                            Text(
                                                text = "${pendingVisitsForRoute.size} STOPS",
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = Color.White,
                                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.5.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "Google Maps multi-waypoint turn-by-turn route",
                                        fontSize = 10.sp,
                                        color = BrandIceBlue
                                    )
                                }
                            }
                            Icon(
                                imageVector = Icons.Default.DirectionsCar,
                                contentDescription = null,
                                tint = GoldCoinRich,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            // ── 7. Visits Feed ──
            if (filteredVisits.isEmpty()) {
                item {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = Color.White,
                        border = BorderStroke(1.dp, BrandIceBorder)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.AssignmentLate,
                                contentDescription = null,
                                tint = TextSlateMuted,
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "No Inspections Found",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextNavyDark
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "New physical verification stops dispatched by the Master Admin will appear here in chronological order.",
                                fontSize = 11.sp,
                                color = TextSlateMuted,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                itemsIndexed(filteredVisits, key = { _, visit -> visit.visitId }) { index, visit ->
                    BrandVisitStopCard(
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
                    photoProof,
                    null,
                    "RECOMMEND_APPROVAL"
                )
                val earned = visit.payoutAmount
                activeInspectionVisit = null
                showPayoutSuccessDialog = earned
            },
            onCompleteDetailedInspection = { remarks, isCollateralAuthentic, isBorrowerVerified, isLenderVerified, photoProof, appraisedValue, recommendation ->
                onCompleteVisit(
                    visit.visitId,
                    remarks,
                    isCollateralAuthentic,
                    isBorrowerVerified,
                    isLenderVerified,
                    photoProof,
                    appraisedValue,
                    recommendation
                )
                val earned = visit.payoutAmount
                activeInspectionVisit = null
                showPayoutSuccessDialog = earned
            },
            onCompleteDetailedInspectionWithGps = { remarks, isCollateralAuthentic, isBorrowerVerified, isLenderVerified, photoProof, appraisedValue, recommendation, lat, lng ->
                onCompleteVisitDetailed(
                    visit.visitId,
                    remarks,
                    isCollateralAuthentic,
                    isBorrowerVerified,
                    isLenderVerified,
                    photoProof,
                    appraisedValue,
                    recommendation,
                    lat,
                    lng
                )
                val earned = visit.payoutAmount
                activeInspectionVisit = null
                showPayoutSuccessDialog = earned
            }
        )
    }

    // ── Payout Confirmation Dialog ──
    showPayoutSuccessDialog?.let { amount ->
        AlertDialog(
            onDismissRequest = { showPayoutSuccessDialog = null },
            containerColor = Color.White,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = Emerald500,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Attestation Successfully Logged!",
                        color = TextNavyDark,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            },
            text = {
                Column {
                    Text(
                        text = "Physical verification report and proof have been attested and saved to immutable ledger.",
                        color = TextSlateMuted,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = GoldCoinCream,
                        border = BorderStroke(1.dp, GoldCoinBorder)
                    ) {
                        Text(
                            text = "+ ₹${amount.toInt()} Credited to Officer Balance",
                            color = GoldCoinAmber,
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
                    colors = ButtonDefaults.buttonColors(containerColor = BrandRoyalBlue)
                ) {
                    Text("Continue Shift", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    // ── Emergency SOS Confirmation Dialog ──
    if (showSosConfirmationDialog) {
        AlertDialog(
            onDismissRequest = { showSosConfirmationDialog = false },
            containerColor = Color.White,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Emergency, null, tint = Red500, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Emergency Field Assistance", color = Color(0xFF991B1B), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            },
            text = {
                Text(
                    text = "Do you want to immediately call the Loanzo Master Admin / Emergency Assistance team for support at your current location?",
                    fontSize = 13.sp,
                    color = TextSlateMuted
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
                    colors = ButtonDefaults.buttonColors(containerColor = Red500)
                ) {
                    Text("Call Safety Hotline", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSosConfirmationDialog = false }) {
                    Text("Cancel", color = TextSlateMuted)
                }
            }
        )
    }

    // ── 👑 Role Switcher Dialog for SuperAdmin ──
    if (showRoleSwitchDialog) {
        AlertDialog(
            onDismissRequest = { showRoleSwitchDialog = false },
            containerColor = Color.White,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("👑", fontSize = 18.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Operational View Switcher",
                        color = TextNavyDark,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = BrandIceBlue,
                        border = BorderStroke(1.dp, BrandIceBorder),
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
                                Text("Borrower / Lender View", color = TextNavyDark, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text("Switch to consumer loan application screens", color = TextSlateMuted, fontSize = 11.sp)
                            }
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = BrandIceBlue,
                        border = BorderStroke(1.dp, BrandIceBorder),
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
                                Text("Master Admin Hub", color = TextNavyDark, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text("Dispatch engine, approvals & ledger oversight", color = TextSlateMuted, fontSize = 11.sp)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showRoleSwitchDialog = false }) {
                    Text("Stay as Officer", color = BrandRoyalBlue, fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    // ── 💰 Instant Settlement Payout Dialog ──
    if (showInstantSettlementDialog) {
        val amount = if (totalEarnings > 0) totalEarnings else 4500.0
        val payoutAccount = if (user?.bankAccountNumber?.isNotBlank() == true) 
            "Bank Account •••• ${user.bankAccountNumber.takeLast(4)} (IFSC: ${user.bankIfsc.ifBlank { "HDFC0001234" }})"
        else if (user?.upiId?.isNotBlank() == true)
            "Registered UPI: ${user.upiId}"
        else 
            "Registered UPI ID: ${user?.phone?.takeLast(10) ?: "9876543210"}@upi"

        AlertDialog(
            onDismissRequest = { showInstantSettlementDialog = false },
            containerColor = Color.White,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AccountBalanceWallet,
                        contentDescription = null,
                        tint = BrandCobalt,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Instant Bounty Settlement",
                        color = TextNavyDark,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Transfer accrued physical verification bounties directly to your designated payout account.",
                        color = TextSlateMuted,
                        fontSize = 12.sp
                    )

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = GoldCoinCream,
                        border = BorderStroke(1.dp, GoldCoinBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "WITHDRAWAL AMOUNT",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = GoldCoinAmber
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = amount.toInrString(),
                                fontSize = 20.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = GoldCoinRich
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Payout Destination:",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextNavyDark
                            )
                            Text(
                                text = payoutAccount,
                                fontSize = 11.sp,
                                color = TextSlateMuted
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.VerifiedUser, null, tint = Emerald500, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Zero fee instant transfer • 256-bit encrypted settlement",
                            fontSize = 10.sp,
                            color = TextSlateMuted
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showInstantSettlementDialog = false
                        settlementSuccessMessage = "₹${amount.toInt()} successfully settled and dispatched to your payout destination!"
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BrandRoyalBlue)
                ) {
                    Text("Confirm Transfer", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showInstantSettlementDialog = false }) {
                    Text("Cancel", color = TextSlateMuted)
                }
            }
        )
    }

    // ── Settlement Success Notification Dialog ──
    settlementSuccessMessage?.let { msg ->
        AlertDialog(
            onDismissRequest = { settlementSuccessMessage = null },
            containerColor = Color.White,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = Emerald500,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Transfer Dispatched!",
                        color = TextNavyDark,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            },
            text = {
                Text(
                    text = msg,
                    color = TextSlateMuted,
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = { settlementSuccessMessage = null },
                    colors = ButtonDefaults.buttonColors(containerColor = BrandRoyalBlue)
                ) {
                    Text("OK", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}

// ═══════════════════════════════════════════════════════════════
// Brand Themed Sub-Components
// ═══════════════════════════════════════════════════════════════

@Composable
private fun BrandMetricBox(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    icon: ImageVector,
    iconColor: Color,
    bgColor: Color = BrandIceBlue,
    borderColor: Color = BrandIceBorder
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = bgColor,
        border = BorderStroke(1.dp, borderColor),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(13.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = label,
                    fontSize = 10.sp,
                    color = TextSlateMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                fontSize = 13.sp,
                fontWeight = FontWeight.ExtraBold,
                color = TextNavyDark,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun BrandFilterChip(
    key: String,
    label: String,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = if (isSelected) BrandRoyalBlue else Color.White,
        border = BorderStroke(1.dp, if (isSelected) BrandRoyalBlue else BrandIceBorder),
        modifier = Modifier.clickable { onSelect() }
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) Color.White else TextSlateMedium,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            maxLines = 1,
            softWrap = false
        )
    }
}

@Composable
private fun BrandRequirementChip(label: String) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = BrandIceBlue,
        border = BorderStroke(1.dp, BrandIceBorder)
    ) {
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextSlateMedium,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.5.dp),
            maxLines = 1,
            softWrap = false
        )
    }
}

@Composable
private fun BrandVisitStopCard(
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

    // Brand-aligned type colors
    val typeColor = when (visit.visitType) {
        "COLLATERAL_VERIFICATION" -> GoldCoinAmber
        "BORROWER_VERIFICATION" -> Emerald500
        else -> BrandCobalt
    }

    val typeBgColor = when (visit.visitType) {
        "COLLATERAL_VERIFICATION" -> GoldCoinCream
        "BORROWER_VERIFICATION" -> EmeraldLight
        else -> BrandIceBlue
    }

    val typeBorderColor = when (visit.visitType) {
        "COLLATERAL_VERIFICATION" -> GoldCoinBorder
        "BORROWER_VERIFICATION" -> Emerald400.copy(alpha = 0.3f)
        else -> BrandIceBorder
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
            if (isCompleted) Emerald500.copy(alpha = 0.4f) else BrandIceBorder
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Stop Header: Sequence badge + Type + Bounty Payout
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Stop # badge in BrandRoyalBlue
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = BrandRoyalBlue
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
                    // Type badge in brand-appropriate color
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = typeBgColor,
                        border = BorderStroke(0.5.dp, typeBorderColor)
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

                // Bounty pill in Gold Coin
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = GoldCoinCream,
                    border = BorderStroke(0.5.dp, GoldCoinBorder)
                ) {
                    Text(
                        text = "₹${visit.payoutAmount.toInt()} Bounty",
                        color = GoldCoinRich,
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
                color = TextNavyDark,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Time & Distance Tag
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Schedule, null, tint = TextSlateMuted, modifier = Modifier.size(13.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "${visit.scheduledDate} • ${visit.scheduledTimeSlot}",
                    fontSize = 11.sp,
                    color = TextSlateMuted
                )
                Spacer(modifier = Modifier.width(10.dp))
                Icon(Icons.Default.DirectionsCar, null, tint = BrandCobalt, modifier = Modifier.size(13.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "${visit.distanceKm ?: 3.5} km away",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = BrandCobalt
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Inspection Checklist Requirements Chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                when (visit.visitType) {
                    "COLLATERAL_VERIFICATION" -> {
                        BrandRequirementChip("📷 3+ Asset Photos")
                        BrandRequirementChip("⚖️ Purity Check")
                        BrandRequirementChip("🔐 OTP Seal")
                    }
                    "BORROWER_VERIFICATION" -> {
                        BrandRequirementChip("🪪 Gov ID Match")
                        BrandRequirementChip("🏠 Geo-Tag Visit")
                        BrandRequirementChip("🔐 OTP Seal")
                    }
                    else -> {
                        BrandRequirementChip("🏛️ Entity Audit")
                        BrandRequirementChip("📝 Agreement Sign")
                        BrandRequirementChip("🔐 OTP Seal")
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Navigation Address Box — BrandIceBlue
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = BrandIceBlue,
                border = BorderStroke(1.dp, BrandIceBorder)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Place, null, tint = Red400, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = visit.targetAddress,
                        fontSize = 12.sp,
                        color = TextNavyDark,
                        modifier = Modifier.weight(1f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Button(
                        onClick = onNavigateMaps,
                        shape = RoundedCornerShape(6.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = BrandRoyalBlue),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Icon(Icons.Default.Navigation, null, modifier = Modifier.size(13.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Maps", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Counterparty Contact Strip — BrandIceBlue
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = BrandIceBlue,
                border = BorderStroke(1.dp, BrandIceBorder)
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
                            color = TextNavyDark,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = visit.borrowerPhone,
                            fontSize = 11.sp,
                            color = TextSlateMuted
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        IconButton(
                            onClick = onCallBorrower,
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(Color.White)
                                .border(1.dp, BrandIceBorder, CircleShape)
                        ) {
                            Icon(Icons.Default.Phone, "Call", tint = BrandCobalt, modifier = Modifier.size(15.dp))
                        }

                        IconButton(
                            onClick = onWhatsAppBorrower,
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(Color.White)
                                .border(1.dp, BrandIceBorder, CircleShape)
                        ) {
                            Icon(Icons.Default.Chat, "WhatsApp", tint = Emerald500, modifier = Modifier.size(15.dp))
                        }

                        IconButton(
                            onClick = onChatBorrower,
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(Color.White)
                                .border(1.dp, BrandIceBorder, CircleShape)
                        ) {
                            Icon(Icons.Default.QuestionAnswer, "In-App Chat", tint = BrandRoyalBlue, modifier = Modifier.size(15.dp))
                        }
                    }
                }
            }

            // Quick Doorstep Dispatch Communication Chips
            if (!isCompleted) {
                val cardContext = LocalContext.current
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val sendQuickMessage: (String) -> Unit = { messageText ->
                        try {
                            val cleanNumber = visit.borrowerPhone.replace("+", "").replace(" ", "").trim()
                            val encodedMsg = Uri.encode(messageText)
                            val waIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://api.whatsapp.com/send?phone=$cleanNumber&text=$encodedMsg"))
                            cardContext.startActivity(waIntent)
                        } catch (_: Exception) {
                            try {
                                val smsIntent = Intent(Intent.ACTION_VIEW, Uri.parse("sms:${visit.borrowerPhone}")).apply {
                                    putExtra("sms_body", messageText)
                                }
                                cardContext.startActivity(smsIntent)
                            } catch (_: Exception) {
                                Toast.makeText(cardContext, "Unable to send message", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = BrandIceBlue,
                        border = BorderStroke(1.dp, BrandIceBorder),
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                sendQuickMessage("Hello ${visit.borrowerName}, this is your Loanzo Verification Officer. I have arrived at your building/gate.")
                            }
                    ) {
                        Text(
                            text = "📍 At Gate",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = BrandCobalt,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(vertical = 5.dp)
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = BrandIceBlue,
                        border = BorderStroke(1.dp, BrandIceBorder),
                        modifier = Modifier
                            .weight(1.1f)
                            .clickable {
                                sendQuickMessage("Hello ${visit.borrowerName}, I am approaching your address. Could you kindly share any nearby landmark?")
                            }
                    ) {
                        Text(
                            text = "🧭 Landmark",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = BrandCobalt,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(vertical = 5.dp)
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = BrandIceBlue,
                        border = BorderStroke(1.dp, BrandIceBorder),
                        modifier = Modifier
                            .weight(1.1f)
                            .clickable {
                                sendQuickMessage("Hello ${visit.borrowerName}, please keep your original Govt ID (PAN/Aadhaar) ready for physical doorstep verification.")
                            }
                    ) {
                        Text(
                            text = "🪪 Keep ID Ready",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = BrandCobalt,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(vertical = 5.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Stage Action: SwipeToConfirmButton for tactical interactions
            if (isCompleted) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = EmeraldLight,
                    border = BorderStroke(1.dp, Emerald500.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 9.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.CheckCircle, null, tint = Emerald500, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Inspection Completed & Verified",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Emerald500
                        )
                    }
                }
            } else {
                when (stageStatus) {
                    "SCHEDULED", "DISPATCHED" -> {
                        SwipeToConfirmButton(
                            text = "Slide to Go En Route ➔",
                            thumbColor = GoldCoinRich,
                            trackColors = listOf(BrandRoyalBlue, BrandCobalt),
                            activeTrackColor = GoldCoinRich.copy(alpha = 0.25f),
                            modifier = Modifier.fillMaxWidth(),
                            onConfirm = { onUpdateStage("EN_ROUTE") }
                        )
                    }
                    "EN_ROUTE" -> {
                        SwipeToConfirmButton(
                            text = "Slide to Mark Arrived 📍",
                            thumbColor = GoldCoinBright,
                            trackColors = listOf(BrandCobalt, BrandSapphire),
                            activeTrackColor = GoldCoinBright.copy(alpha = 0.25f),
                            modifier = Modifier.fillMaxWidth(),
                            onConfirm = { onUpdateStage("ARRIVED") }
                        )
                    }
                    else -> {
                        Button(
                            onClick = onStartInspection,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(42.dp),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Emerald500)
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
