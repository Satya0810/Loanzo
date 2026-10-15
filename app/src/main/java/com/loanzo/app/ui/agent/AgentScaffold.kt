package com.loanzo.app.ui.agent

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.BatteryManager
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.automirrored.outlined.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import com.loanzo.app.ui.components.LoanzoText as Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.loanzo.app.data.entity.AgentVisitEntity
import com.loanzo.app.data.entity.UserEntity
import com.loanzo.app.ui.agent.components.AgentCredentialDialog
import com.loanzo.app.ui.agent.tabs.AgentCommsTab
import com.loanzo.app.ui.agent.tabs.AgentDispatchesTab
import com.loanzo.app.ui.agent.tabs.AgentEarningsTab
import com.loanzo.app.ui.agent.tabs.AgentToolsDeskTab
import com.loanzo.app.ui.components.LoanzoAvatar
import com.loanzo.app.ui.theme.*

/**
 * Dedicated Root Scaffold for Loanzo FieldOps (The Field Agent Experience).
 * Completely isolated from consumer/member features.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AgentScaffold(
    user: UserEntity?,
    visits: List<AgentVisitEntity>,
    isOnDuty: Boolean,
    onToggleDutyStatus: (Boolean) -> Unit,
    onStartInspection: (AgentVisitEntity) -> Unit,
    onUpdateVisitStage: (visitId: String, stage: String) -> Unit,
    onNavigateToChat: (channelId: String, loanId: String?, targetUserId: String?) -> Unit,
    onLogout: () -> Unit,
    onSwitchToConsumer: (() -> Unit)? = null,
    onSwitchToAdmin: (() -> Unit)? = null
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) }
    var showCredentialDialog by remember { mutableStateOf(false) }

    // Live Hardware Telemetry
    var batteryTelemetry by remember { mutableStateOf("Battery 84%") }
    var networkTelemetry by remember { mutableStateOf("Online Synced") }

    LaunchedEffect(Unit) {
        try {
            val bm = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
            val cap = bm?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: 84
            val status = bm?.getIntProperty(BatteryManager.BATTERY_PROPERTY_STATUS)
            val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
            batteryTelemetry = if (isCharging) "Charging $cap%" else "Battery $cap%"
        } catch (_: Exception) {
            batteryTelemetry = "Battery 84%"
        }
        try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            val net = cm?.activeNetwork
            val caps = cm?.getNetworkCapabilities(net)
            networkTelemetry = when {
                caps == null -> "Offline"
                caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "WiFi Live"
                caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "Cellular 4G/5G"
                else -> "Online Synced"
            }
        } catch (_: Exception) {
            networkTelemetry = "Online Synced"
        }
    }

    val pendingStopsCount = remember(visits) {
        visits.count { it.status != "COMPLETED" }
    }

    Scaffold(
        containerColor = CanvasPorcelain,
        topBar = {
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
                        // Left: Tappable Officer Identity Pill
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(24.dp))
                                .clickable { showCredentialDialog = true }
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .background(
                                            Brush.linearGradient(
                                                listOf(BrandRoyalBlue, GoldCoinRich)
                                            )
                                        )
                                )
                                LoanzoAvatar(
                                    user = user,
                                    size = 38.dp,
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
                                            text = "FIELD OFFICER",
                                            color = BrandRoyalBlue,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.5.dp)
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
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = user?.name?.ifBlank { "Loanzo Officer" } ?: "Loanzo Officer",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextNavyDark,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Icon(
                                        imageVector = Icons.Default.ExpandMore,
                                        contentDescription = "Open ID Card",
                                        tint = TextSlateMuted,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }

                        // Right: Quick SOS Emergency Safety Button
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = RedLight,
                            border = BorderStroke(1.dp, RedBorder),
                            modifier = Modifier.clickable { selectedTab = 2 } // Switches to Comms & SOS tab
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Shield,
                                    contentDescription = "SOS Safety Hotline",
                                    tint = Red500,
                                    modifier = Modifier.size(13.dp)
                                )
                                Text(
                                    text = "SOS",
                                    color = Red500,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.ExtraBold
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
                            Icon(
                                imageVector = Icons.Default.GpsFixed,
                                contentDescription = null,
                                tint = BrandRoyalBlue,
                                modifier = Modifier.size(11.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = "GPS High-Accuracy  •  $batteryTelemetry  •  $networkTelemetry",
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
        },
        bottomBar = {
            Surface(
                shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
                color = Color.White,
                border = BorderStroke(1.dp, BrandIceBorder),
                tonalElevation = 8.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                NavigationBar(
                    containerColor = Color.Transparent,
                    tonalElevation = 0.dp
                ) {
                    val navItemColors = NavigationBarItemDefaults.colors(
                        selectedIconColor = BrandRoyalBlue,
                        selectedTextColor = BrandRoyalBlue,
                        indicatorColor = BrandIceBlue,
                        unselectedIconColor = TextSlateMuted,
                        unselectedTextColor = TextSlateMuted
                    )

                    // 1. Dispatches Tab
                    NavigationBarItem(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        icon = {
                            if (pendingStopsCount > 0 && selectedTab != 0) {
                                BadgedBox(
                                    badge = {
                                        Badge(containerColor = GoldCoinAmber) {
                                            Text(
                                                text = pendingStopsCount.toString(),
                                                fontSize = 9.sp,
                                                color = Color.White
                                            )
                                        }
                                    }
                                ) {
                                    Icon(
                                        imageVector = if (selectedTab == 0) Icons.AutoMirrored.Filled.AltRoute else Icons.AutoMirrored.Outlined.AltRoute,
                                        contentDescription = "Dispatches"
                                    )
                                }
                            } else {
                                Icon(
                                    imageVector = if (selectedTab == 0) Icons.AutoMirrored.Filled.AltRoute else Icons.AutoMirrored.Outlined.AltRoute,
                                    contentDescription = "Dispatches"
                                )
                            }
                        },
                        label = {
                            Text(
                                text = "Dispatches",
                                fontSize = 10.sp,
                                fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Medium
                            )
                        },
                        colors = navItemColors
                    )

                    // 2. Tools Desk Tab
                    NavigationBarItem(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        icon = {
                            Icon(
                                imageVector = if (selectedTab == 1) Icons.Filled.Build else Icons.Outlined.Build,
                                contentDescription = "Tools Desk"
                            )
                        },
                        label = {
                            Text(
                                text = "Tools Desk",
                                fontSize = 10.sp,
                                fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Medium
                            )
                        },
                        colors = navItemColors
                    )

                    // 3. Comms & SOS Tab
                    NavigationBarItem(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        icon = {
                            Icon(
                                imageVector = if (selectedTab == 2) Icons.Filled.ChatBubble else Icons.Outlined.ChatBubbleOutline,
                                contentDescription = "Comms"
                            )
                        },
                        label = {
                            Text(
                                text = "Comms & SOS",
                                fontSize = 10.sp,
                                fontWeight = if (selectedTab == 2) FontWeight.Bold else FontWeight.Medium
                            )
                        },
                        colors = navItemColors
                    )

                    // 4. Earnings Tab
                    NavigationBarItem(
                        selected = selectedTab == 3,
                        onClick = { selectedTab = 3 },
                        icon = {
                            Icon(
                                imageVector = if (selectedTab == 3) Icons.Filled.AccountBalanceWallet else Icons.Outlined.AccountBalanceWallet,
                                contentDescription = "Earnings"
                            )
                        },
                        label = {
                            Text(
                                text = "Earnings",
                                fontSize = 10.sp,
                                fontWeight = if (selectedTab == 3) FontWeight.Bold else FontWeight.Medium
                            )
                        },
                        colors = navItemColors
                    )
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (selectedTab) {
                0 -> AgentDispatchesTab(
                    user = user,
                    visits = visits,
                    isOnDuty = isOnDuty,
                    onToggleDutyStatus = onToggleDutyStatus,
                    onStartInspection = onStartInspection,
                    onUpdateVisitStage = onUpdateVisitStage,
                    onNavigateToChat = onNavigateToChat
                )
                1 -> AgentToolsDeskTab()
                2 -> AgentCommsTab(
                    visits = visits,
                    onNavigateToChat = onNavigateToChat,
                    onTriggerSos = { selectedTab = 2 }
                )
                3 -> AgentEarningsTab(
                    user = user,
                    totalEarnings = user?.totalAgentEarnings ?: 0.0,
                    completedVisitsCount = visits.count { it.status == "COMPLETED" }
                )
            }
        }
    }

    // Modal Officer Credential Dialog
    if (showCredentialDialog) {
        AgentCredentialDialog(
            user = user,
            onDismiss = { showCredentialDialog = false },
            onLogout = onLogout,
            onSwitchToConsumer = onSwitchToConsumer,
            onSwitchToAdmin = onSwitchToAdmin
        )
    }
}
