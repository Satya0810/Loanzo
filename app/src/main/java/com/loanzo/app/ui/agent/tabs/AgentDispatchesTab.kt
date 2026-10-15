package com.loanzo.app.ui.agent.tabs

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
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.material3.*
import com.loanzo.app.ui.components.LoanzoText as Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.loanzo.app.data.entity.AgentVisitEntity
import com.loanzo.app.data.entity.UserEntity
import com.loanzo.app.ui.components.SwipeToConfirmButton
import com.loanzo.app.ui.theme.*
import com.loanzo.app.util.toInrString

/**
 * Tab 1: Field Dispatches & Daily Itinerary.
 * Bank-grade institutional interface for active field routing, waypoint tracking,
 * and physical arrival workflows. Built strictly without decorative emojis.
 */
@Composable
fun AgentDispatchesTab(
    user: UserEntity?,
    visits: List<AgentVisitEntity>,
    isOnDuty: Boolean,
    onToggleDutyStatus: (Boolean) -> Unit,
    onStartInspection: (AgentVisitEntity) -> Unit,
    onUpdateVisitStage: (visitId: String, stage: String) -> Unit,
    onNavigateToChat: (channelId: String, loanId: String?, targetUserId: String?) -> Unit
) {
    val context = LocalContext.current
    var selectedFilter by remember { mutableStateOf("ALL") }
    var showDutyOtpDialog by remember { mutableStateOf(false) }

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

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // ── 1. Bank-Grade Institutional Bento Dual Hero Row ──
        item {
            AgentBentoDualHeroRow(
                isOnDuty = isOnDuty,
                onToggleDutyStatus = { targetDuty ->
                    if (targetDuty) {
                        showDutyOtpDialog = true
                    } else {
                        onToggleDutyStatus(false)
                    }
                },
                todayVisitsCount = todayVisits.size,
                completedVisitsCount = completedVisitsCount,
                totalVisitsCount = visits.size
            )
        }

        // ── 2. Bento Trio Action Strip (Scheduled, Attested, Pending) ──
        item {
            AgentBentoTrioActionStrip(
                todayVisitsCount = todayVisits.size,
                completedVisitsCount = completedVisitsCount,
                totalVisitsCount = visits.size
            )
        }

        // ── 2. Fuel & Performance Incentive Progress Bar ──
        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, BrandIceBorder),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.LocalGasStation,
                                contentDescription = null,
                                tint = BrandRoyalBlue,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "DAILY FUEL & TRAVEL INCENTIVE",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextNavyDark
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = BrandIceBlue,
                            border = BorderStroke(0.5.dp, BrandIceBorder)
                        ) {
                            Text(
                                text = "$completedVisitsCount of ${visits.size.coerceAtLeast(4)} Completed",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = BrandRoyalBlue,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    val progress = if (visits.isNotEmpty()) completedVisitsCount.toFloat() / visits.size.coerceAtLeast(1) else 0f
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(Gray200)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(progress.coerceIn(0f, 1f))
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(4.dp))
                                .background(BrandRoyalBlue)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    val stopsRemaining = (visits.size.coerceAtLeast(4) - completedVisitsCount).coerceAtLeast(0)
                    if (stopsRemaining > 0) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = BrandIceBlue,
                            border = BorderStroke(1.dp, BrandIceBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = null,
                                    tint = BrandRoyalBlue,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Complete $stopsRemaining more ${if (stopsRemaining == 1) "stop" else "stops"} to unlock the INR 300 daily travel allowance.",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = TextSlateMedium
                                )
                            }
                        }
                    } else {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = EmeraldLight,
                            border = BorderStroke(1.dp, EmeraldBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = Emerald500,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Target achieved. INR 300 travel allowance verified and added to wallet balance.",
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

        // ── 3. Multi-Stop Sequential Navigation Launcher ──
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
                                    val webUri = Uri.parse("https://www.google.com/maps/dir/?api=1&destination=${Uri.encode(destination)}&travelmode=driving")
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
                            .padding(horizontal = 14.dp, vertical = 12.dp),
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
                                    imageVector = Icons.AutoMirrored.Filled.AltRoute,
                                    contentDescription = "Multi-Stop Route Navigation",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
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
                                        color = Color.White.copy(alpha = 0.2f)
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
                                    text = "Google Maps turn-by-turn waypoint routing",
                                    fontSize = 11.sp,
                                    color = BrandIceBlue
                                )
                            }
                        }
                        Icon(
                            imageVector = Icons.Default.DirectionsCar,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }

        // ── 4. Filter Chips Row ──
        item {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Assigned Itinerary",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextNavyDark
                    )
                    Text(
                        text = "${filteredVisits.size} Total Stops",
                        fontSize = 12.sp,
                        color = TextSlateMuted
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        DispatchFilterChip(
                            label = "All Stops",
                            icon = Icons.AutoMirrored.Filled.List,
                            isSelected = selectedFilter == "ALL"
                        ) { selectedFilter = "ALL" }
                    }
                    item {
                        DispatchFilterChip(
                            label = "Collateral Valuation",
                            icon = Icons.Default.Scale,
                            isSelected = selectedFilter == "COLLATERAL"
                        ) { selectedFilter = "COLLATERAL" }
                    }
                    item {
                        DispatchFilterChip(
                            label = "Borrower KYC",
                            icon = Icons.Default.Person,
                            isSelected = selectedFilter == "BORROWER"
                        ) { selectedFilter = "BORROWER" }
                    }
                    item {
                        DispatchFilterChip(
                            label = "Lender Audit",
                            icon = Icons.Default.Business,
                            isSelected = selectedFilter == "LENDER"
                        ) { selectedFilter = "LENDER" }
                    }
                    item {
                        DispatchFilterChip(
                            label = "Attested",
                            icon = Icons.Default.CheckCircle,
                            isSelected = selectedFilter == "COMPLETED"
                        ) { selectedFilter = "COMPLETED" }
                    }
                }
            }
        }

        // ── 5. Field Stop Cards ──
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
                            text = "No Inspections In This Category",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextNavyDark
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Switch filter chips above to view other assigned stops.",
                            fontSize = 12.sp,
                            color = TextSlateMuted
                        )
                    }
                }
            }
        } else {
            itemsIndexed(filteredVisits) { index, visit ->
                val isFirst = index == 0
                val isDone = visit.status == "COMPLETED"

                DispatchStopCard(
                    stopNumber = index + 1,
                    visit = visit,
                    isCurrentTarget = isFirst && !isDone,
                    onUpdateStage = { stage -> onUpdateVisitStage(visit.visitId, stage) },
                    onNavigateMaps = {
                        try {
                            val target = if (visit.targetLatitude != 0.0 && visit.targetLongitude != 0.0) {
                                "${visit.targetLatitude},${visit.targetLongitude}"
                            } else {
                                Uri.encode(visit.targetAddress)
                            }
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("google.navigation:q=$target")).apply {
                                setPackage("com.google.android.apps.maps")
                            }
                            context.startActivity(intent)
                        } catch (_: Exception) {
                            val webUri = Uri.parse("https://www.google.com/maps/search/?api=1&query=${Uri.encode(visit.targetAddress)}")
                            context.startActivity(Intent(Intent.ACTION_VIEW, webUri))
                        }
                    },
                    onCallBorrower = {
                        val phone = visit.borrowerPhone.ifBlank { visit.lenderPhone }
                        if (phone.isNotBlank()) {
                            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone"))
                            context.startActivity(intent)
                        } else {
                            Toast.makeText(context, "Phone number unavailable", Toast.LENGTH_SHORT).show()
                        }
                    },
                    onWhatsAppBorrower = {
                        val phone = visit.borrowerPhone.ifBlank { visit.lenderPhone }.replace(" ", "").replace("+", "")
                        if (phone.isNotBlank()) {
                            try {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://api.whatsapp.com/send?phone=$phone"))
                                intent.setPackage("com.whatsapp")
                                context.startActivity(intent)
                            } catch (_: Exception) {
                                val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/$phone"))
                                context.startActivity(webIntent)
                            }
                        } else {
                            Toast.makeText(context, "Phone number unavailable", Toast.LENGTH_SHORT).show()
                        }
                    },
                    onChatBorrower = {
                        val channelId = "chat_${visit.visitId}"
                        onNavigateToChat(channelId, visit.loanId, null)
                    },
                    onStartInspection = { onStartInspection(visit) }
                )
            }
        }
    }

    if (showDutyOtpDialog) {
        AgentShiftOtpDialog(
            userPhone = user?.phone?.ifBlank { "+91 98123 45678" } ?: "+91 98123 45678",
            onDismiss = { showDutyOtpDialog = false },
            onVerifySuccess = {
                showDutyOtpDialog = false
                onToggleDutyStatus(true)
            }
        )
    }
}

@Composable
private fun AgentBentoDualHeroRow(
    isOnDuty: Boolean,
    onToggleDutyStatus: (Boolean) -> Unit,
    todayVisitsCount: Int,
    completedVisitsCount: Int,
    totalVisitsCount: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Left Hero Card: Field Shift Duty Status
        val shiftBgColor = if (isOnDuty) Color(0xFFECFDF5) else Color(0xFFEEF2FF)
        val shiftBorderColor = if (isOnDuty) Color(0xFFA7F3D0) else Color(0xFFC7D2FE)
        val shiftBubbleBg = if (isOnDuty) Color(0xFFD1FAE5) else Color(0xFFE0E7FF)
        val shiftAccentColor = if (isOnDuty) Color(0xFF059669) else Color(0xFF4F46E5)

        Surface(
            onClick = { onToggleDutyStatus(!isOnDuty) },
            shape = RoundedCornerShape(24.dp),
            color = shiftBgColor,
            border = BorderStroke(1.dp, shiftBorderColor),
            modifier = Modifier
                .weight(1f)
                .height(115.dp)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                // Background accent graphic bubble on right
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .align(Alignment.BottomEnd)
                        .offset(x = 12.dp, y = 14.dp)
                        .clip(CircleShape)
                        .background(shiftBubbleBg),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isOnDuty) Icons.Default.DirectionsRun else Icons.Default.Pause,
                        contentDescription = "Duty Status",
                        tint = shiftAccentColor,
                        modifier = Modifier.size(34.dp).offset(x = (-4).dp, y = (-4).dp)
                    )
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(if (isOnDuty) Emerald500 else Orange500)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Field Shift",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color.White.copy(alpha = 0.85f),
                        border = BorderStroke(0.5.dp, shiftBorderColor)
                    ) {
                        Text(
                            text = if (isOnDuty) "LIVE • Tap to pause" else "STANDBY • Tap to start",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = shiftAccentColor,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }
        }

        // Right Hero Card: Dispatches Itinerary
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFFF0F9FF), // Soft Sky Blue
            border = BorderStroke(1.dp, Color(0xFFBAE6FD)),
            modifier = Modifier
                .weight(1f)
                .height(115.dp)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                // Background accent graphic bubble on right
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .align(Alignment.BottomEnd)
                        .offset(x = 12.dp, y = 14.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFE0F2FE)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.AltRoute,
                        contentDescription = "Dispatches",
                        tint = Color(0xFF0284C7),
                        modifier = Modifier.size(34.dp).offset(x = (-4).dp, y = (-4).dp)
                    )
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Dispatches",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color.White.copy(alpha = 0.85f),
                        border = BorderStroke(0.5.dp, Color(0xFFBAE6FD))
                    ) {
                        Text(
                            text = "$completedVisitsCount of $totalVisitsCount Done",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF0369A1),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AgentBentoTrioActionStrip(
    todayVisitsCount: Int,
    completedVisitsCount: Int,
    totalVisitsCount: Int,
    modifier: Modifier = Modifier
) {
    val pendingCount = (totalVisitsCount - completedVisitsCount).coerceAtLeast(0)

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // 1. Scheduled (Soft Ice Blue)
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color(0xFFEFF6FF), // Soft Ice Blue
            border = BorderStroke(1.dp, Color(0xFFBFDBFE)),
            modifier = Modifier
                .weight(1f)
                .height(88.dp)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .align(Alignment.BottomEnd)
                        .offset(x = 8.dp, y = 10.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFDBEAFE)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.EventNote,
                        contentDescription = "Scheduled",
                        tint = Color(0xFF1E40AF),
                        modifier = Modifier.size(24.dp).offset(x = (-3).dp, y = (-3).dp)
                    )
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Scheduled",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )
                    Text(
                        text = "$todayVisitsCount stops",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF1E40AF)
                    )
                }
            }
        }

        // 2. Attested (Soft Mint Green)
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color(0xFFECFDF5), // Soft Mint
            border = BorderStroke(1.dp, Color(0xFFA7F3D0)),
            modifier = Modifier
                .weight(1f)
                .height(88.dp)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .align(Alignment.BottomEnd)
                        .offset(x = 8.dp, y = 10.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFD1FAE5)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Attested",
                        tint = Color(0xFF059669),
                        modifier = Modifier.size(24.dp).offset(x = (-3).dp, y = (-3).dp)
                    )
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Attested",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )
                    Text(
                        text = "$completedVisitsCount done",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF047857)
                    )
                }
            }
        }

        // 3. Pending (Soft Warm Amber)
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color(0xFFFFFBEB), // Soft Warm Amber
            border = BorderStroke(1.dp, Color(0xFFFDE68A)),
            modifier = Modifier
                .weight(1f)
                .height(88.dp)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .align(Alignment.BottomEnd)
                        .offset(x = 8.dp, y = 10.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFFEF3C7)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = "Pending",
                        tint = Color(0xFFD97706),
                        modifier = Modifier.size(24.dp).offset(x = (-3).dp, y = (-3).dp)
                    )
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Pending",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )
                    Text(
                        text = if (pendingCount > 0) "$pendingCount left" else "All complete",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFFB45309)
                    )
                }
            }
        }
    }
}

@Composable
private fun DispatchFilterChip(
    label: String,
    icon: ImageVector,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = if (isSelected) BrandRoyalBlue else Color.White,
        border = BorderStroke(1.dp, if (isSelected) BrandRoyalBlue else BrandIceBorder),
        modifier = Modifier.clickable { onSelect() }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) Color.White else TextSlateMedium,
                modifier = Modifier.size(13.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) Color.White else TextSlateMedium,
                maxLines = 1,
                softWrap = false
            )
        }
    }
}

@Composable
private fun DispatchStopCard(
    stopNumber: Int,
    visit: AgentVisitEntity,
    isCurrentTarget: Boolean = false,
    onUpdateStage: (String) -> Unit,
    onNavigateMaps: () -> Unit,
    onCallBorrower: () -> Unit,
    onWhatsAppBorrower: () -> Unit,
    onChatBorrower: () -> Unit,
    onStartInspection: () -> Unit
) {
    val isCompleted = visit.status == "COMPLETED"
    val stageStatus = visit.visitStageStatus

    val typeColor = when (visit.visitType) {
        "COLLATERAL_VERIFICATION" -> BrandRoyalBlue
        "BORROWER_VERIFICATION" -> Emerald500
        else -> BrandCobalt
    }

    val typeBgColor = when (visit.visitType) {
        "COLLATERAL_VERIFICATION" -> BrandIceBlue
        "BORROWER_VERIFICATION" -> EmeraldLight
        else -> Gray100
    }

    val typeBorderColor = when (visit.visitType) {
        "COLLATERAL_VERIFICATION" -> BrandIceBorder
        "BORROWER_VERIFICATION" -> EmeraldBorder
        else -> Gray300
    }

    val typeIcon = when (visit.visitType) {
        "COLLATERAL_VERIFICATION" -> Icons.Default.Scale
        "BORROWER_VERIFICATION" -> Icons.Default.Person
        else -> Icons.Default.Business
    }

    val typeLabel = when (visit.visitType) {
        "COLLATERAL_VERIFICATION" -> "COLLATERAL VALUATION"
        "BORROWER_VERIFICATION" -> "BORROWER KYC"
        else -> "LENDER AUDIT"
    }

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isCompleted) Color(0xFFFAFCFF) else Color.White
        ),
        border = BorderStroke(
            1.dp,
            if (isCompleted) EmeraldBorder else if (isCurrentTarget) BrandCobalt else BrandIceBorder
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isCompleted) 0.dp else 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Priority Waypoint Notice (for active stop)
            if (isCurrentTarget && !isCompleted) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = BrandIceBlue,
                    border = BorderStroke(0.8.dp, BrandIceBorder),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(BrandRoyalBlue))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "ACTIVE WAYPOINT",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = BrandRoyalBlue
                            )
                        }
                        Text(
                            text = "Next Stop on Route",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextSlateMedium
                        )
                    }
                }
            }

            // Header: Sequence Number & Visit Type Pill
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (isCompleted) EmeraldLight else BrandRoyalBlue
                    ) {
                        Text(
                            text = if (isCompleted) "STOP #$stopNumber • COMPLETED" else "STOP #$stopNumber",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isCompleted) Emerald500 else Color.White,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = typeBgColor,
                        border = BorderStroke(0.8.dp, typeBorderColor)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.5.dp)
                        ) {
                            Icon(
                                imageVector = typeIcon,
                                contentDescription = null,
                                tint = typeColor,
                                modifier = Modifier.size(11.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = typeLabel,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = typeColor
                            )
                        }
                    }
                }

                // Payout Badge
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = BrandIceBlue,
                    border = BorderStroke(0.8.dp, BrandIceBorder)
                ) {
                    Text(
                        text = "+${visit.payoutAmount.toInrString()}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = BrandRoyalBlue,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.5.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Borrower Details
            val borrowerDisplay = visit.borrowerName.ifBlank { "Registered Borrower" }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = borrowerDisplay,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextNavyDark
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = visit.targetAddress.ifBlank { "Address pending sync" },
                        fontSize = 12.sp,
                        color = TextSlateMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Gray100,
                    border = BorderStroke(0.5.dp, Gray200)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = visit.scheduledTimeSlot.ifBlank { "11:00 AM" },
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextNavyDark
                        )
                        Text(
                            text = visit.scheduledDate.ifBlank { "Today" },
                            fontSize = 9.sp,
                            color = TextSlateMuted
                        )
                    }
                }
            }

            // Pledged Collateral Box
            if (visit.collateralItemName?.isNotBlank() == true) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = BrandIceBlue,
                    border = BorderStroke(0.8.dp, BrandIceBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Inventory2,
                                contentDescription = null,
                                tint = BrandRoyalBlue,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Collateral: ${visit.collateralItemName}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextNavyDark,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        if (visit.collateralEstimatedValue != null) {
                            Text(
                                text = "Est: ${visit.collateralEstimatedValue.toInrString()}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = BrandRoyalBlue
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 4-Stage Visual Progress Stepper
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CanvasPorcelain, RoundedCornerShape(8.dp))
                    .border(0.6.dp, BrandIceBorder, RoundedCornerShape(8.dp))
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val stages = listOf(
                    "SCHEDULED" to "1. Assigned",
                    "EN_ROUTE" to "2. En Route",
                    "ARRIVED" to "3. Arrived",
                    "COMPLETED" to "4. Attested"
                )
                stages.forEachIndexed { sIdx, (stageKey, stageLabel) ->
                    val isStagePassed = when (stageKey) {
                        "SCHEDULED" -> true
                        "EN_ROUTE" -> stageStatus == "EN_ROUTE" || stageStatus == "ARRIVED" || isCompleted
                        "ARRIVED" -> stageStatus == "ARRIVED" || isCompleted
                        "COMPLETED" -> isCompleted
                        else -> false
                    }
                    val isCurrent = stageStatus == stageKey && !isCompleted
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(if (isStagePassed) Emerald500 else Gray400)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = stageLabel,
                            fontSize = 9.sp,
                            fontWeight = if (isCurrent) FontWeight.ExtraBold else FontWeight.Medium,
                            color = if (isStagePassed) TextNavyDark else TextSlateMuted
                        )
                    }
                    if (sIdx < stages.size - 1) {
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = Gray400,
                            modifier = Modifier.size(10.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = BrandIceBorder, thickness = 0.6.dp)
            Spacer(modifier = Modifier.height(10.dp))

            // 1-Tap Action Row (Navigate, Call, WhatsApp, Chat)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Navigate Maps Button
                Button(
                    onClick = onNavigateMaps,
                    modifier = Modifier.weight(1.2f),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = BrandIceBlue,
                        contentColor = BrandRoyalBlue
                    ),
                    border = BorderStroke(1.dp, BrandIceBorder),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Default.Directions, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Maps", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                // Call Button
                OutlinedButton(
                    onClick = onCallBorrower,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextNavyDark),
                    border = BorderStroke(1.dp, BrandIceBorder),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Default.Phone, contentDescription = "Call", modifier = Modifier.size(14.dp), tint = Emerald500)
                }

                // WhatsApp Button
                OutlinedButton(
                    onClick = onWhatsAppBorrower,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextNavyDark),
                    border = BorderStroke(1.dp, BrandIceBorder),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = "WhatsApp", modifier = Modifier.size(14.dp), tint = Emerald500)
                }

                // Field Chat Button
                OutlinedButton(
                    onClick = onChatBorrower,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextNavyDark),
                    border = BorderStroke(1.dp, BrandIceBorder),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Default.ChatBubbleOutline, contentDescription = "Chat", modifier = Modifier.size(14.dp), tint = BrandCobalt)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Primary Verification Action
            if (isCompleted) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = EmeraldLight,
                    border = BorderStroke(1.dp, EmeraldBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Emerald500, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Attestation Complete • Certified Sec 65B",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Emerald500
                        )
                    }
                }
            } else {
                when (stageStatus) {
                    "SCHEDULED" -> {
                        SwipeToConfirmButton(
                            text = "Swipe to Start En Route",
                            thumbColor = BrandRoyalBlue,
                            trackColors = listOf(BrandIceBlue, BrandIceBorder),
                            onConfirm = { onUpdateStage("EN_ROUTE") }
                        )
                    }
                    "EN_ROUTE" -> {
                        Button(
                            onClick = { onUpdateStage("ARRIVED") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = BrandRoyalBlue)
                        ) {
                            Icon(Icons.Default.PinDrop, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Confirm Arrival at Location", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    else -> {
                        Button(
                            onClick = onStartInspection,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Emerald500)
                        ) {
                            Icon(Icons.Default.QrCodeScanner, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Start Physical Verification", fontSize = 13.sp, fontWeight = FontWeight.ExtraBold)
                        }
                    }
                }
            }
        }
    }
}

/**
 * Bank-Grade Agent Shift Authorization OTP Dialog.
 * Prompts the field agent for a 6-digit cryptographic shift OTP before activating GPS tracking,
 * real-time itinerary updates, and duty status.
 */
@Composable
fun AgentShiftOtpDialog(
    userPhone: String,
    onDismiss: () -> Unit,
    onVerifySuccess: () -> Unit
) {
    val context = LocalContext.current
    var otpValue by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(BrandIceBlue),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        tint = BrandRoyalBlue,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "Shift Authorization",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextNavyDark
                    )
                    Text(
                        text = "Agent Duty OTP Protocol",
                        fontSize = 11.sp,
                        color = TextSlateMuted
                    )
                }
            }
        },
        text = {
            Column {
                Text(
                    text = "Enter the 6-digit shift activation OTP sent to $userPhone to activate GPS tracking, audit logging, and field dispatch duty.",
                    fontSize = 13.sp,
                    color = TextSlateMuted,
                    lineHeight = 18.sp
                )
                Spacer(modifier = Modifier.height(14.dp))

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = BrandIceBlue,
                    border = BorderStroke(1.dp, BrandIceBorder),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            otpValue = "882190"
                            errorMessage = null
                        }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "DEMO OTP: 882190",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = BrandRoyalBlue
                        )
                        Text(
                            text = "TAP TO FILL",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = BrandRoyalBlue
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = otpValue,
                    onValueChange = { input ->
                        if (input.length <= 6 && input.all { it.isDigit() }) {
                            otpValue = input
                            errorMessage = null
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("6-Digit OTP") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    textStyle = TextStyle(
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        letterSpacing = 4.sp
                    ),
                    isError = errorMessage != null,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = BrandRoyalBlue,
                        unfocusedBorderColor = Gray300
                    )
                )

                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = errorMessage ?: "",
                        fontSize = 12.sp,
                        color = Color(0xFFDC2626)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (otpValue.length != 6) {
                        errorMessage = "Please enter all 6 digits"
                    } else {
                        Toast.makeText(context, "Shift Authorized • GPS Active", Toast.LENGTH_SHORT).show()
                        onVerifySuccess()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = BrandRoyalBlue),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = "Verify & Go On Duty",
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = "Cancel",
                    color = TextSlateMuted,
                    fontWeight = FontWeight.Medium
                )
            }
        },
        containerColor = Color.White,
        shape = RoundedCornerShape(16.dp)
    )
}

