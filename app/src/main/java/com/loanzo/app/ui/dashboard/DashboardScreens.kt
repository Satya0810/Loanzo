package com.loanzo.app.ui.dashboard

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.loanzo.app.R
import com.loanzo.app.ui.components.*
import com.loanzo.app.ui.theme.*
import androidx.compose.foundation.border
import com.loanzo.app.util.toDateString
import com.loanzo.app.util.toInrString
import com.loanzo.app.util.toRelativeTime
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.foundation.lazy.LazyRow
import com.loanzo.app.data.entity.MarketplacePostEntity
import com.loanzo.app.ui.marketplace.MarketplaceTabFilter
import com.loanzo.app.ui.marketplace.MarketplaceUiState
import com.loanzo.app.ui.marketplace.SocialPostCard
import com.loanzo.app.ui.marketplace.BidProposalBottomSheet
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    state: DashboardUiState,
    marketState: MarketplaceUiState = MarketplaceUiState(),
    onTabSelected: (MarketplaceTabFilter) -> Unit = {},
    onSearchQueryChange: (String) -> Unit = {},
    onCategoryTagSelected: (String) -> Unit = {},
    onVouchPost: (String) -> Unit = {},
    onSubmitBid: (postId: String, amount: Double, rate: Double, tenure: Int, message: String) -> Unit = { _, _, _, _, _ -> },
    onNavigateToCreatePost: (String) -> Unit = {},
    onNavigateToCreateLoan: () -> Unit = {},
    onNavigateToCalculator: () -> Unit = {},
    onNavigateToLoanDetail: (String) -> Unit = {},
    onNavigateToProfile: () -> Unit = {},
    onNavigateToApproval: (String) -> Unit = {},
    onNavigateToLoansTab: () -> Unit = {},
    onNavigateToChat: (String) -> Unit = {},
    onNavigateToKyc: () -> Unit = {},
    onPushDemoData: () -> Unit = {}
) {
    var isMenuExpanded by remember { mutableStateOf(false) }
    var showChatSheet by remember { mutableStateOf(false) }
    var showReportSheet by remember { mutableStateOf(false) }
    var showAcademySimulator by remember { mutableStateOf(false) }
    var selectedPostForBid by remember { mutableStateOf<MarketplacePostEntity?>(null) }
    var selectedVisitForInspection by remember { mutableStateOf<com.loanzo.app.data.entity.AgentVisitEntity?>(null) }

    val context = androidx.compose.ui.platform.LocalContext.current
    val userRepository = com.loanzo.app.util.LocalUserRepository.current
    val dashboardGuideSeen by userRepository.isGuideSeen(com.loanzo.app.data.repository.UserRepository.GUIDE_DASHBOARD_SEEN)
        .collectAsStateWithLifecycle(initialValue = true)
    val questCommunityDone by userRepository.isQuestStepDone(com.loanzo.app.data.repository.UserRepository.QUEST_COMMUNITY_EXPLORED)
        .collectAsStateWithLifecycle(initialValue = false)
    val questCalculatorDone by userRepository.isQuestStepDone(com.loanzo.app.data.repository.UserRepository.QUEST_CALCULATOR_TRIED)
        .collectAsStateWithLifecycle(initialValue = false)
    val questKycDone = state.user?.kycStatus == "VERIFIED"
    val questDemoDone by userRepository.isQuestStepDone(com.loanzo.app.data.repository.UserRepository.QUEST_DEMO_SEEDED)
        .collectAsStateWithLifecycle(initialValue = false)
    val questDismissed by userRepository.isQuestCardDismissed()
        .collectAsStateWithLifecycle(initialValue = false)
    val scope = rememberCoroutineScope()
    val agentRepository = com.loanzo.app.util.LocalAgentRepository.current
    val agentVisits by (if (state.user != null) agentRepository.getVisitsForAgent(state.user.userId) else kotlinx.coroutines.flow.flowOf(emptyList()))
        .collectAsStateWithLifecycle(initialValue = emptyList())

    Box(modifier = Modifier.fillMaxSize()) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 100.dp)
    ) {
        // Greeting header
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(horizontal = 20.dp, vertical = 24.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            LoanzoAvatar(
                                user = state.user,
                                size = 48.dp,
                                showVerifiedBadge = true,
                                borderColor = BrandAmberGold,
                                borderWidth = 2.dp,
                                onClick = onNavigateToProfile
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                val dateStr = remember {
                                    java.text.SimpleDateFormat("EEEE, d MMMM", java.util.Locale.getDefault()).format(java.util.Date())
                                }
                                Text(
                                    text = dateStr.uppercase(),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    letterSpacing = 0.5.sp,
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Hello, ${state.user?.name?.split(" ")?.firstOrNull() ?: "User"} 👋",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onBackground,
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "Welcome to Loanzo",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = BrandAmberGold.copy(alpha = 0.12f),
                                border = BorderStroke(1.dp, BrandAmberGold.copy(alpha = 0.4f)),
                                modifier = Modifier.clickable { showAcademySimulator = true }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text("✨", fontSize = 12.sp)
                                    Text("Academy", color = BrandAmberGold, fontWeight = FontWeight.Bold, fontSize = 12.sp, maxLines = 1, softWrap = false)
                                }
                            }

                            Box {
                                IconButton(
                                    onClick = { isMenuExpanded = true },
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.surface)
                                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.MoreVert,
                                        contentDescription = "More Options",
                                        tint = MaterialTheme.colorScheme.onSurface
                                    )
                                }

                            DropdownMenu(
                                expanded = isMenuExpanded,
                                onDismissRequest = { isMenuExpanded = false },
                                modifier = Modifier
                                    .background(MaterialTheme.colorScheme.surface)
                                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(14.dp))
                            ) {
                                // Option 1: Chat
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text(
                                                text = "Chat",
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                fontSize = 14.sp
                                            )
                                            Text(
                                                text = "Direct messages & bot",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                fontSize = 11.sp
                                            )
                                        }
                                    },
                                    onClick = {
                                        isMenuExpanded = false
                                        showChatSheet = true
                                    },
                                    leadingIcon = {
                                        Surface(
                                            shape = CircleShape,
                                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.Chat,
                                                contentDescription = "Chat",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.padding(7.dp)
                                            )
                                        }
                                    }
                                )

                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 0.5.dp)

                                // Option 2: Report
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text(
                                                text = "Report",
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                fontSize = 14.sp
                                            )
                                            Text(
                                                text = "Take action on anyone",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = Red400,
                                                fontSize = 11.sp
                                            )
                                        }
                                    },
                                    onClick = {
                                        isMenuExpanded = false
                                        showReportSheet = true
                                    },
                                    leadingIcon = {
                                        Surface(
                                            shape = CircleShape,
                                            color = Red400.copy(alpha = 0.12f),
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.ReportProblem,
                                                contentDescription = "Report",
                                                tint = Red400,
                                                modifier = Modifier.padding(7.dp)
                                            )
                                        }
                                    }
                                )

                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 0.5.dp)

                                // Option 3: Simulator
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text(
                                                text = "Simulator",
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                fontSize = 14.sp
                                            )
                                            Text(
                                                text = "Loan & EMI calculator",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = Emerald400,
                                                fontSize = 11.sp
                                            )
                                        }
                                    },
                                    onClick = {
                                        isMenuExpanded = false
                                        onNavigateToCalculator()
                                    },
                                    leadingIcon = {
                                        Surface(
                                            shape = CircleShape,
                                            color = Emerald400.copy(alpha = 0.12f),
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.Calculate,
                                                contentDescription = "Simulator",
                                                tint = Emerald400,
                                                modifier = Modifier.padding(7.dp)
                                            )
                                        }
                                    }
                                )

                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 0.5.dp)

                                // Option 4: Academy & Live Simulator
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text(
                                                text = "Loanzo Academy",
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                fontSize = 14.sp
                                            )
                                            Text(
                                                text = "Interactive tutorials & simulators",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = BrandAmberGold,
                                                fontSize = 11.sp
                                            )
                                        }
                                    },
                                    onClick = {
                                        isMenuExpanded = false
                                        showAcademySimulator = true
                                    },
                                    leadingIcon = {
                                        Surface(
                                            shape = CircleShape,
                                            color = BrandAmberGold.copy(alpha = 0.15f),
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Text("🎓", fontSize = 16.sp)
                                            }
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

        val userRole = state.user?.role?.uppercase() ?: "USER"
        val isAgent = userRole == "AGENT" && state.user?.agentStatus == "APPROVED"
        val isAdmin = userRole == "ADMIN" || com.loanzo.app.util.VerificationManager.isAppOwner(state.user)

        if (isAgent) {
            // ==========================================
            // 🕵️ CERTIFIED AGENT COCKPIT & DOORSTEP VISITS
            // ==========================================
            val pendingAgentVisits = agentVisits.filter { it.status != "COMPLETED" }
            val completedAgentVisits = agentVisits.filter { it.status == "COMPLETED" }

            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.2.dp, Emerald500.copy(alpha = 0.6f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = CircleShape,
                                    color = Emerald500.copy(alpha = 0.15f),
                                    modifier = Modifier.size(38.dp)
                                ) {
                                    Icon(Icons.Default.Security, contentDescription = null, tint = Emerald500, modifier = Modifier.padding(8.dp))
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text("AGENT CONTROL COCKPIT", fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, color = Emerald500)
                                    Text("Field Officer Active Desk", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                }
                            }
                            
                            val isOnDuty = state.user?.isOnDuty ?: true
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isOnDuty) Emerald500.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant,
                                border = BorderStroke(1.dp, if (isOnDuty) Emerald500.copy(alpha = 0.4f) else MaterialTheme.colorScheme.outlineVariant),
                                modifier = Modifier.clickable {
                                    scope.launch {
                                        userRepository.updateUser(state.user!!.copy(isOnDuty = !isOnDuty))
                                        agentRepository.setDutyStatus(state.user.userId, !isOnDuty)
                                        android.widget.Toast.makeText(context, if (!isOnDuty) "🟢 You are now ON DUTY for Doorstep Visits" else "⚪ Duty set to OFF", android.widget.Toast.LENGTH_SHORT).show()
                                    }
                                }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(if (isOnDuty) Emerald500 else TextSlateMuted)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (isOnDuty) "ON DUTY" else "OFF DUTY",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isOnDuty) Emerald500 else TextSlateMuted
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text("EARNINGS", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("₹${state.user?.totalAgentEarnings?.toInt() ?: 0}", fontSize = 17.sp, fontWeight = FontWeight.ExtraBold, color = Emerald500)
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text("PENDING", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("${pendingAgentVisits.size}", fontSize = 17.sp, fontWeight = FontWeight.ExtraBold, color = Gold500)
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text("COMPLETED", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("${completedAgentVisits.size}", fontSize = 17.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onSurface)
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Doorstep Inspections Section Header
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Assigned Doorstep Inspections (${pendingAgentVisits.size})",
                        modifier = Modifier.weight(1f).padding(end = 8.dp),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    TextButton(onClick = onNavigateToLoansTab) {
                        Text("View All ↗", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            if (pendingAgentVisits.isEmpty()) {
                item {
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("🎉", fontSize = 28.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("All doorstep visits completed!", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                            Text("Keep On-Duty toggle ON to receive automated dispatch alerts when borrowers nearby request physical verification.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
            } else {
                items(pendingAgentVisits.take(3).size) { idx ->
                    val visit = pendingAgentVisits[idx]
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 5.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = Gold500.copy(alpha = 0.15f),
                                    border = BorderStroke(0.5.dp, Gold500.copy(alpha = 0.4f))
                                ) {
                                    Text(
                                        text = visit.visitType.replace("_", " "),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Gold500,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                                Text("+₹${visit.payoutAmount.toInt()} Payout", fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = Emerald500)
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(visit.borrowerName, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface)
                            Text("📍 ${visit.targetAddress}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        val intent = android.content.Intent(android.content.Intent.ACTION_DIAL, android.net.Uri.parse("tel:${visit.borrowerPhone}"))
                                        context.startActivity(intent)
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1f),
                                    contentPadding = PaddingValues(vertical = 6.dp)
                                ) {
                                    Text("Call", fontSize = 11.sp, maxLines = 1, softWrap = false)
                                }
                                OutlinedButton(
                                    onClick = {
                                        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse("geo:0,0?q=${android.net.Uri.encode(visit.targetAddress)}"))
                                        context.startActivity(intent)
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1f),
                                    contentPadding = PaddingValues(vertical = 6.dp)
                                ) {
                                    Text("Map", fontSize = 11.sp, maxLines = 1, softWrap = false)
                                }
                                Button(
                                    onClick = { selectedVisitForInspection = visit },
                                    colors = ButtonDefaults.buttonColors(containerColor = Gold500, contentColor = Navy900),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1.2f),
                                    contentPadding = PaddingValues(vertical = 6.dp)
                                ) {
                                    Text("Inspect", fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
                                }
                            }
                        }
                    }
                }
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        } else if (isAdmin) {
            // ==========================================
            // 👑 MASTER ADMIN EXECUTIVE HUB CARD
            // ==========================================
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.2.dp, Gold500.copy(alpha = 0.6f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = CircleShape,
                                    color = Gold500.copy(alpha = 0.15f),
                                    modifier = Modifier.size(38.dp)
                                ) {
                                    Text("👑", fontSize = 20.sp, modifier = Modifier.padding(8.dp))
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text("APP OWNER & MASTER ADMIN", fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, color = Gold500)
                                    Text("@${state.user?.username?.ifBlank { "satyam0810" }}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                }
                            }
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Gold500.copy(alpha = 0.15f),
                                border = BorderStroke(1.dp, Gold500.copy(alpha = 0.4f)),
                                modifier = Modifier.clickable { onNavigateToLoansTab() }
                            ) {
                                Text("PLATFORM ACTIVE", fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, color = Gold500, modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp))
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        val totalVol = state.totalLentDisbursed + state.totalBorrowedDisbursed
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text("PLATFORM VOL", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(totalVol.toInrString(), fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text("ACTIVE LOANS", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("${state.loansAsLender.count { it.status == "ACTIVE" } + state.loansAsBorrower.count { it.status == "ACTIVE" }}", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = Emerald500)
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text("ESCROW VAULT", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("SECURED 🛡️", fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF6366F1), modifier = Modifier.padding(top = 4.dp))
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        OutlinedButton(
                            onClick = onNavigateToLoansTab,
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, Gold500.copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.AdminPanelSettings, contentDescription = null, tint = Gold500, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Manage Platform Loans & Custody Ledger", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        } else {
            // ==========================================
            // 👤 STANDARD MEMBER CONSUMER HERO
            // ==========================================
            if (!questDismissed) {
                item {
                    InteractiveGettingStartedQuestCard(
                        isCommunityDone = questCommunityDone,
                        isCalculatorDone = questCalculatorDone,
                        isKycDone = questKycDone,
                        isDemoDone = questDemoDone,
                        onExploreCommunity = {
                            scope.launch {
                                userRepository.markQuestStepDone(com.loanzo.app.data.repository.UserRepository.QUEST_COMMUNITY_EXPLORED)
                            }
                        },
                        onOpenCalculator = {
                            scope.launch {
                                userRepository.markQuestStepDone(com.loanzo.app.data.repository.UserRepository.QUEST_CALCULATOR_TRIED)
                            }
                            showAcademySimulator = true
                        },
                        onVerifyKyc = {
                            onNavigateToKyc()
                        },
                        onSeedDemo = {
                            scope.launch {
                                userRepository.markQuestStepDone(com.loanzo.app.data.repository.UserRepository.QUEST_DEMO_SEEDED)
                            }
                            onPushDemoData()
                        },
                        onDismiss = {
                            scope.launch {
                                userRepository.dismissQuestCard()
                            }
                        }
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                }
            }

            item {
                ExecutiveHeroCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = CircleShape,
                                color = GoldCoinRich.copy(alpha = 0.2f),
                                modifier = Modifier.size(38.dp)
                            ) {
                                Icon(
                                    Icons.Default.AccountBalanceWallet,
                                    contentDescription = null,
                                    tint = GoldCoinRich,
                                    modifier = Modifier.padding(9.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Portfolio Overview",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = "Real-time capital balance",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Gray400
                                )
                            }
                        }
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Emerald400.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = "ACTIVE",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = Emerald400,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    val totalVolume = state.totalLentDisbursed + state.totalBorrowedDisbursed
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "Active Portfolio Value",
                                style = MaterialTheme.typography.labelMedium,
                                color = Gray300
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = totalVolume.toInrString(),
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = GoldCoinBright
                            )
                        }

                        Box(
                            modifier = Modifier.size(72.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            val lentAngle = if (totalVolume > 0) ((state.totalLentDisbursed / totalVolume) * 360f).toFloat() else 180f
                            val borrowedAngle = if (totalVolume > 0) ((state.totalBorrowedDisbursed / totalVolume) * 360f).toFloat() else 180f

                            Canvas(modifier = Modifier.size(64.dp)) {
                                drawArc(
                                    color = Emerald400,
                                    startAngle = -90f,
                                    sweepAngle = lentAngle,
                                    useCenter = false,
                                    style = Stroke(width = 16f, cap = StrokeCap.Round)
                                )
                                drawArc(
                                    color = GoldCoinRich,
                                    startAngle = -90f + lentAngle,
                                    sweepAngle = borrowedAngle,
                                    useCenter = false,
                                    style = Stroke(width = 16f, cap = StrokeCap.Round)
                                )
                            }

                            Icon(
                                imageVector = Icons.Default.PieChart,
                                contentDescription = null,
                                tint = GoldCoinRich,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = SurfaceDarkElevated,
                            border = BorderStroke(0.8.dp, Emerald400.copy(alpha = 0.3f)),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { onNavigateToLoansTab() }
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(Emerald400)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Lent Out",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Gray400
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = state.totalLentDisbursed.toInrString(),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Emerald400
                                )
                                Text(
                                    text = "${state.loansAsLender.count { it.status == "ACTIVE" }} Active Loans",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Gray400
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = SurfaceDarkElevated,
                            border = BorderStroke(0.8.dp, GoldCoinRich.copy(alpha = 0.3f)),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { onNavigateToLoansTab() }
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(GoldCoinRich)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Borrowed",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Gray400
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = state.totalBorrowedDisbursed.toInrString(),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = GoldCoinBright
                                )
                                Text(
                                    text = "${state.loansAsBorrower.count { it.status == "ACTIVE" }} Active Debts",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Gray400
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color.White.copy(alpha = 0.08f),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onNavigateToLoansTab() }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 10.dp, horizontal = 14.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Manage All Loans in Loans Tab",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                tint = GoldCoinBright,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
        // ─── COMMUNITY LOAN WALL (LIVE & OPEN DIRECTLY ON HOME) ────────────────
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
            ) {
                // Header with LIVE badge + Create Post CTA
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "Community Loan Wall",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Emerald400.copy(alpha = 0.15f),
                                border = BorderStroke(1.dp, Emerald400.copy(alpha = 0.3f))
                            ) {
                                Text(
                                    "LIVE",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Emerald400,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            "Verified direct P2P lending opportunities",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp
                        )
                    }

                    Surface(
                        onClick = { 
                            if (marketState.isKycVerified) onNavigateToCreatePost("OFFER_TO_LEND") else onNavigateToKyc() 
                        },
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f))
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.Add, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Post", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Segmented Tabs (All Offers, Lenders, Borrowers, My Posts)
                val tabs = listOf("All Offers", "💰 Lenders", "🙋 Borrowers", "⭐ My Posts")
                val selectedTabIndex = when (marketState.selectedTab) {
                    MarketplaceTabFilter.ALL -> 0
                    MarketplaceTabFilter.LENDERS -> 1
                    MarketplaceTabFilter.BORROWERS -> 2
                    MarketplaceTabFilter.MY_POSTS -> 3
                }
                SegmentedCapsuleTab(
                    tabs = tabs,
                    selectedIndex = selectedTabIndex,
                    onTabSelected = { idx ->
                        val selected = when (idx) {
                            0 -> MarketplaceTabFilter.ALL
                            1 -> MarketplaceTabFilter.LENDERS
                            2 -> MarketplaceTabFilter.BORROWERS
                            else -> MarketplaceTabFilter.MY_POSTS
                        }
                        onTabSelected(selected)
                    }
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Embedded Search Bar
                OutlinedTextField(
                    value = marketState.searchQuery,
                    onValueChange = onSearchQueryChange,
                    placeholder = { Text("Search by purpose, name, city (#Medical, #Education)...", fontSize = 13.sp, color = Gray400) },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = "Search", tint = GoldCoinRich, modifier = Modifier.size(20.dp))
                    },
                    trailingIcon = {
                        if (marketState.searchQuery.isNotBlank()) {
                            IconButton(onClick = { onSearchQueryChange("") }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear", tint = Gray400, modifier = Modifier.size(18.dp))
                            }
                        }
                    },
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                    ),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Golden Category Chips
                val categories = listOf("ALL", "EDUCATION", "MEDICAL", "BUSINESS", "EMERGENCY", "PERSONAL")
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(categories) { cat ->
                        val isSelected = marketState.selectedCategoryTag.equals(cat, ignoreCase = true)
                        FilterChip(
                            selected = isSelected,
                            onClick = { onCategoryTagSelected(cat) },
                            label = {
                                Text(
                                    if (cat == "ALL") "All Categories" else "#$cat",
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            colors = goldFilterChipColors(),
                            border = goldFilterChipBorder(isSelected),
                            shape = RoundedCornerShape(10.dp)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        // Live Community Post Cards directly in LazyColumn
        if (marketState.isLoading && marketState.posts.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = GoldCoinRich)
                }
            }
        } else if (marketState.posts.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(24.dp).fillMaxWidth()
                        ) {
                            Icon(Icons.Outlined.Forum, contentDescription = null, tint = Gray400, modifier = Modifier.size(44.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("No Community Posts Found", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "Be the first to publish a lending offer or loan request!",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = { 
                                    if (marketState.isKycVerified) onNavigateToCreatePost("OFFER_TO_LEND") else onNavigateToKyc() 
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("Create Post", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        } else {
            items(marketState.posts, key = { it.postId }) { post ->
                Box(modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)) {
                    SocialPostCard(
                        post = post,
                        onVouch = { onVouchPost(post.postId) },
                        onPrimaryAction = { selectedPostForBid = post }
                    )
                }
            }
            item { Spacer(modifier = Modifier.height(16.dp)) }
        }

        // Pending approvals section (if any)
        if (state.pendingApprovals.isNotEmpty()) {
            item {
                SectionHeader(
                    title = "⚡ Pending Approvals",
                    actionText = "View All",
                    modifier = Modifier.padding(horizontal = 20.dp)
                )
                state.pendingApprovals.take(3).forEach { disbursement ->
                    Card(
                        onClick = { onNavigateToApproval(disbursement.disbursementId) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Orange400.copy(alpha = 0.08f)
                        ),
                        border = CardDefaults.outlinedCardBorder().copy(
                            brush = Brush.linearGradient(listOf(Orange400.copy(alpha = 0.3f), Color.Transparent))
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = Orange400.copy(alpha = 0.15f),
                                modifier = Modifier.size(44.dp)
                            ) {
                                Icon(
                                    Icons.Default.HourglassTop,
                                    null,
                                    tint = Orange400,
                                    modifier = Modifier.padding(10.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    disbursement.payeeName,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    "${disbursement.purpose} • ${disbursement.ruleEngineResult}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                disbursement.amount.toInrString(),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = Orange400
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
            }
        }

        // Recent Loans on Home (Unified list)
        item {
            val allRecentLoans = (state.loansAsLender + state.loansAsBorrower)
                .distinctBy { it.loanId }
                .sortedByDescending { it.createdAt }

            SectionHeader(
                title = "Recent Loans",
                actionText = if (allRecentLoans.isNotEmpty()) "View All" else null,
                onAction = onNavigateToLoansTab,
                modifier = Modifier.padding(horizontal = 20.dp)
            )

            if (allRecentLoans.isEmpty() && !state.isLoading) {
                EmptyState(
                    icon = Icons.Default.Folder,
                    title = "No loans found",
                    subtitle = "Grant or request a loan to get started"
                )
            } else {
                allRecentLoans.take(3).forEach { loan ->
                    val isLender = state.loansAsLender.any { it.loanId == loan.loanId }
                    LoanSummaryCard(
                        loanId = loan.loanId,
                        purpose = loan.purpose,
                        amount = loan.sanctionedAmount,
                        outstanding = loan.outstandingAmount,
                        status = loan.status,
                        counterpartyName = if (isLender) "Lent" else "Borrowed",
                        date = loan.createdAt.toDateString(),
                        onClick = { onNavigateToLoanDetail(loan.loanId) },
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp),
                        loanType = loan.loanType
                    )
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
        }


        // Recent activity
        if (state.recentEvents.isNotEmpty()) {
            item {
                SectionHeader(
                    title = "Recent Activity",
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                )
            }
            items(state.recentEvents.take(5)) { event ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 2.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = when (event.event) {
                                "CREATED" -> Blue400
                                "APPROVED" -> Emerald400
                                "REJECTED" -> Red400
                                "PAID" -> Emerald400
                                else -> Gray400
                            }.copy(alpha = 0.15f),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                when (event.event) {
                                    "CREATED" -> Icons.Default.Add
                                    "APPROVED" -> Icons.Default.Check
                                    "REJECTED" -> Icons.Default.Close
                                    "PAID" -> Icons.Default.Payment
                                    else -> Icons.Default.Info
                                },
                                null,
                                modifier = Modifier.padding(8.dp),
                                tint = when (event.event) {
                                    "CREATED" -> Blue400
                                    "APPROVED" -> Emerald400
                                    "REJECTED" -> Red400
                                    "PAID" -> Emerald400
                                    else -> Gray400
                                }
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                event.description,
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 1
                            )
                            Text(
                                event.timestamp.toRelativeTime(),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(24.dp)) }
    }

    if (showChatSheet) {
        val allLoans = remember(state.loansAsLender, state.loansAsBorrower) {
            (state.loansAsLender + state.loansAsBorrower).distinctBy { it.loanId }
        }
        ChatSelectionBottomSheet(
            loans = allLoans,
            currentUser = state.user,
            onDismiss = { showChatSheet = false },
            onSelectLoanChat = { loanId ->
                showChatSheet = false
                onNavigateToChat(loanId)
            }
        )
    }

    if (showReportSheet) {
        val allLoans = remember(state.loansAsLender, state.loansAsBorrower) {
            (state.loansAsLender + state.loansAsBorrower).distinctBy { it.loanId }
        }
        ReportActionBottomSheet(
            currentUser = state.user,
            loans = allLoans,
            onDismiss = { showReportSheet = false }
        )
    }

    selectedVisitForInspection?.let { visit ->
        val agentRepository = com.loanzo.app.util.LocalAgentRepository.current
        val context = androidx.compose.ui.platform.LocalContext.current
        com.loanzo.app.ui.agent.AgentInspectionSheet(
            visit = visit,
            onDismiss = { selectedVisitForInspection = null },
            onCompleteInspection = { remarks, collOk, bOk, lOk, proof ->
                scope.launch(kotlinx.coroutines.Dispatchers.IO) {
                    agentRepository.completeVisit(
                        visitId = visit.visitId,
                        agentRemarks = remarks,
                        isCollateralAuthentic = collOk,
                        isBorrowerIdentityVerified = bOk,
                        isLenderIdentityVerified = lOk,
                        proofPhotoUris = proof
                    )
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                        android.widget.Toast.makeText(context, "Inspection completed & ₹${visit.payoutAmount.toInt()} payout credited!", android.widget.Toast.LENGTH_SHORT).show()
                        selectedVisitForInspection = null
                    }
                }
            }
        )
    }

    selectedPostForBid?.let { post ->
        BidProposalBottomSheet(
            post = post,
            isKycCompleted = marketState.isKycVerified,
            onNavigateToKyc = onNavigateToKyc,
            onDismiss = { selectedPostForBid = null },
            onSubmitBid = { amount, rate, tenure, msg ->
                onSubmitBid(post.postId, amount, rate, tenure, msg)
                selectedPostForBid = null
            }
        )
    }

    if (!dashboardGuideSeen) {
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            ContextualGuideCard(
                visible = true,
                icon = Icons.Default.Dashboard,
                title = "Your Financial Command Center",
                body = "Explore the Community Loan Wall, view portfolio metrics, and track all your active loans — all in one place.",
                onDismiss = {
                    scope.launch {
                        userRepository.markGuideSeen(com.loanzo.app.data.repository.UserRepository.GUIDE_DASHBOARD_SEEN)
                    }
                },
                autoDismissSeconds = 8
            )
        }
    }

    // Hands-on Interactive Academy Simulator Sheet
    if (showAcademySimulator) {
        LoanzoAcademySimulatorSheet(
            onDismiss = { showAcademySimulator = false },
            onNavigateToCreateLoan = {
                showAcademySimulator = false
                onNavigateToCreateLoan()
            }
        )
    }
    }
}
