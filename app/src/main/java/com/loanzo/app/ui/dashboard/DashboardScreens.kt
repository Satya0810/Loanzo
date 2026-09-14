package com.loanzo.app.ui.dashboard

import com.loanzo.app.util.isSuperAdmin
import com.loanzo.app.ui.marketplace.VouchReasonDialog
import androidx.hilt.navigation.compose.hiltViewModel
import com.loanzo.app.ui.notification.NotificationViewModel
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import com.loanzo.app.data.entity.LoanEntity

import androidx.compose.animation.*
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.blur
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.graphics.graphicsLayer
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
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import com.loanzo.app.ui.components.LoanzoText as Text
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
    onVouchPost: (postId: String, reason: String, note: String) -> Unit = { _, _, _ -> },
    onSubmitBid: (postId: String, amount: Double, rate: Double, tenure: Int, message: String) -> Unit = { _, _, _, _, _ -> },
    onNavigateToCreatePost: (String) -> Unit = {},
    onNavigateToCreateLoan: () -> Unit = {},
    onNavigateToCalculator: () -> Unit = {},
    onNavigateToLoanDetail: (String) -> Unit = {},
    onNavigateToProfile: () -> Unit = {},
    onNavigateToPortfolio: () -> Unit = {},
    onNavigateToApproval: (String) -> Unit = {},
    onNavigateToLoansTab: () -> Unit = {},
    onNavigateToCommunity: () -> Unit = {},
    onNavigateToChat: (String) -> Unit = {},
    onNavigateToChatHub: () -> Unit = {},
    onNavigateToKyc: () -> Unit = {},
    onNavigateToAdminHub: (Int) -> Unit = {},
    onNavigateToSupport: () -> Unit = {},
    onNavigateToUserProfile: (String) -> Unit = {},
    onNavigateToNotifications: () -> Unit = {},
    onNavigateToAgentCockpit: () -> Unit = {},
    onPushDemoData: () -> Unit = {},
    onRefresh: () -> Unit = {}
) {
    var isMenuExpanded by remember { mutableStateOf(false) }
    var showChatSheet by remember { mutableStateOf(false) }
    var showReportSheet by remember { mutableStateOf(false) }
    var selectedPostForBid by remember { mutableStateOf<MarketplacePostEntity?>(null) }
    var postToVouch by remember { mutableStateOf<MarketplacePostEntity?>(null) }
    var selectedVisitForInspection by remember { mutableStateOf<com.loanzo.app.data.entity.AgentVisitEntity?>(null) }

    val context = androidx.compose.ui.platform.LocalContext.current
    val userRepository = com.loanzo.app.util.LocalUserRepository.current
    val questCommunityDone by userRepository.isQuestStepDone(com.loanzo.app.data.repository.UserRepository.QUEST_COMMUNITY_EXPLORED)
        .collectAsStateWithLifecycle(initialValue = false)
    val questCalculatorDone by userRepository.isQuestStepDone(com.loanzo.app.data.repository.UserRepository.QUEST_CALCULATOR_TRIED)
        .collectAsStateWithLifecycle(initialValue = false)
    val questKycDone = state.user?.kycStatus == "VERIFIED"
    val questDismissed by userRepository.isQuestCardDismissed()
        .collectAsStateWithLifecycle(initialValue = true) // default true to avoid flash on navigation
    val scope = rememberCoroutineScope()
    val agentRepository = com.loanzo.app.util.LocalAgentRepository.current
    val agentVisits by (if (state.user != null) agentRepository.getVisitsForAgent(state.user.userId) else kotlinx.coroutines.flow.flowOf(emptyList()))
        .collectAsStateWithLifecycle(initialValue = emptyList())
    val adminRepository = com.loanzo.app.util.LocalAdminRepository.current
    val adminComplaints by adminRepository.allComplaints.collectAsStateWithLifecycle(initialValue = emptyList())
    val adminVaultItems by adminRepository.allVaultItems.collectAsStateWithLifecycle(initialValue = emptyList())
    val adminVisits by adminRepository.allVisits.collectAsStateWithLifecycle(initialValue = emptyList())
    val adminNocs by adminRepository.allNocs.collectAsStateWithLifecycle(initialValue = emptyList())
    val adminMeetings by adminRepository.allMeetings.collectAsStateWithLifecycle(initialValue = emptyList())
    val adminApplications by adminRepository.allAgentApplications.collectAsStateWithLifecycle(initialValue = emptyList())
    val adminRequests by adminRepository.allAdminRequests.collectAsStateWithLifecycle(initialValue = emptyList())
    val menuBlurRadius by animateDpAsState(
        targetValue = if (isMenuExpanded) 20.dp else 0.dp,
        animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing),
        label = "menu_blur_radius"
    )
    val menuScrimAlpha by animateFloatAsState(
        targetValue = if (isMenuExpanded) 0.45f else 0f,
        animationSpec = tween(durationMillis = 220),
        label = "menu_scrim_alpha"
    )

    Box(modifier = Modifier.fillMaxSize()) {
        PullToRefreshBox(
            isRefreshing = state.isRefreshing,
            onRefresh = onRefresh,
            modifier = Modifier.fillMaxSize()
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .blur(menuBlurRadius),
                contentPadding = PaddingValues(bottom = 100.dp)
            ) {
        // Persistent Field Officer Active Shift Banner (Rupeek/Uber Driver style)
        val isFieldOfficer = com.loanzo.app.util.VerificationManager.isFieldAgent(state.user)
        if (isFieldOfficer) {
            item {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = BrandIceBlue,
                    border = BorderStroke(1.dp, BrandCobalt.copy(alpha = 0.35f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 6.dp)
                        .clickable { onNavigateToAgentCockpit() }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(Emerald500)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "FIELD OFFICER SHIFT ACTIVE",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = BrandRoyalBlue
                                )
                                Text(
                                    text = "${agentVisits.count { it.status != "COMPLETED" }} stops assigned • Tap to return to Cockpit",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextNavyDark
                                )
                            }
                        }
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = "Return to Cockpit",
                            tint = BrandRoyalBlue,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

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
                            val profileName = state.user?.name?.trim()?.takeIf { it.isNotBlank() } ?: "User"
                            Text(
                                text = profileName,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground,
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { onNavigateToProfile() }
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            val moreMenuRotation by animateFloatAsState(
                                targetValue = if (isMenuExpanded) 90f else 0f,
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                    stiffness = Spring.StiffnessLow
                                ),
                                label = "more_menu_rotation"
                            )
                            val moreMenuScale by animateFloatAsState(
                                targetValue = if (isMenuExpanded) 1.08f else 1f,
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                    stiffness = Spring.StiffnessMedium
                                ),
                                label = "more_menu_scale"
                            )
                            val moreMenuBgColor by animateColorAsState(
                                targetValue = if (isMenuExpanded) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                                else MaterialTheme.colorScheme.surface,
                                label = "more_menu_bg"
                            )
                            val moreMenuBorderColor by animateColorAsState(
                                targetValue = if (isMenuExpanded) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                                else MaterialTheme.colorScheme.outlineVariant,
                                label = "more_menu_border"
                            )
                            val moreMenuIconTint by animateColorAsState(
                                targetValue = if (isMenuExpanded) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurface,
                                label = "more_menu_tint"
                            )

                            Box {
                                IconButton(
                                    onClick = { isMenuExpanded = !isMenuExpanded },
                                    modifier = Modifier
                                        .size(42.dp)
                                        .graphicsLayer {
                                            scaleX = moreMenuScale
                                            scaleY = moreMenuScale
                                        }
                                        .clip(CircleShape)
                                        .background(moreMenuBgColor)
                                        .border(1.dp, moreMenuBorderColor, CircleShape)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.MoreVert,
                                        contentDescription = "More Options",
                                        tint = moreMenuIconTint,
                                        modifier = Modifier.rotate(moreMenuRotation)
                                    )
                                }

                                if (isMenuExpanded) {
                                    androidx.compose.ui.window.Popup(
                                        alignment = Alignment.TopEnd,
                                        offset = androidx.compose.ui.unit.IntOffset(x = 0, y = 140),
                                        onDismissRequest = { isMenuExpanded = false },
                                        properties = androidx.compose.ui.window.PopupProperties(focusable = true)
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(22.dp),
                                            color = MaterialTheme.colorScheme.surface,
                                            tonalElevation = 12.dp,
                                            shadowElevation = 16.dp,
                                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                            modifier = Modifier
                                                .width(260.dp)
                                                .padding(end = 16.dp)
                                        ) {
                                            Column(modifier = Modifier.padding(8.dp)) {
                                                // Header with Title & Close (Cross) Button
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(horizontal = 8.dp, vertical = 4.dp),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(
                                                        text = "Quick Actions",
                                                        style = MaterialTheme.typography.labelLarge,
                                                        fontWeight = FontWeight.Bold,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                    IconButton(
                                                        onClick = { isMenuExpanded = false },
                                                        modifier = Modifier.size(28.dp)
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.Close,
                                                            contentDescription = "Close Menu",
                                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                            modifier = Modifier.size(18.dp)
                                                        )
                                                    }
                                                }

                                                HorizontalDivider(
                                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                                                    thickness = 0.5.dp,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                                )

                                                // Option 1: Report
                                                DashboardMenuRow(
                                                    icon = Icons.Default.ReportProblem,
                                                    iconTint = Red400,
                                                    iconBg = Red400.copy(alpha = 0.12f),
                                                    title = stringResource(R.string.report),
                                                    subtitle = stringResource(R.string.report_subtitle),
                                                    onClick = {
                                                        isMenuExpanded = false
                                                        showReportSheet = true
                                                    }
                                                )

                                                HorizontalDivider(
                                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                                                    thickness = 0.5.dp,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                                )

                                                // Option 2: Simulator
                                                DashboardMenuRow(
                                                    icon = Icons.Default.Calculate,
                                                    iconTint = Emerald400,
                                                    iconBg = Emerald400.copy(alpha = 0.12f),
                                                    title = stringResource(R.string.simulator),
                                                    subtitle = stringResource(R.string.simulator_subtitle),
                                                    onClick = {
                                                        isMenuExpanded = false
                                                        onNavigateToCalculator()
                                                    }
                                                )

                                                HorizontalDivider(
                                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                                                    thickness = 0.5.dp,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                                )

                                                // Option 3: Interactive Guide
                                                DashboardMenuRow(
                                                    icon = Icons.Default.Explore,
                                                    iconTint = BrandAmberGold,
                                                    iconBg = BrandAmberGold.copy(alpha = 0.15f),
                                                    title = stringResource(R.string.interactive_guide),
                                                    subtitle = stringResource(R.string.guide_subtitle),
                                                    onClick = {
                                                        isMenuExpanded = false
                                                        scope.launch {
                                                            userRepository.setActiveTour(com.loanzo.app.ui.components.AppTours.REQUEST_LOAN, 0)
                                                        }
                                                    }
                                                )

                                                HorizontalDivider(
                                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                                                    thickness = 0.5.dp,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                                )

                                                // Option 4: Help & Support
                                                DashboardMenuRow(
                                                    icon = Icons.Default.SupportAgent,
                                                    iconTint = BrandRoyalBlue,
                                                    iconBg = BrandIceBlue,
                                                    title = stringResource(R.string.help_support),
                                                    subtitle = stringResource(R.string.support_subtitle),
                                                    onClick = {
                                                        isMenuExpanded = false
                                                        onNavigateToSupport()
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                        }
                    }
                }
            }
        }
    }

        val userRole = state.user?.role?.uppercase() ?: "USER"
        val isAgent = com.loanzo.app.util.VerificationManager.isFieldAgent(state.user)
        val isAdmin = com.loanzo.app.util.VerificationManager.isAppOwner(state.user)

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
                                        text = if (isOnDuty) stringResource(R.string.on_duty) else stringResource(R.string.off_duty),
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
            // 👑 MASTER ADMIN EXECUTIVE COMMAND CENTER
            // ==========================================
            item {
                val totalVol = state.totalLentDisbursed + state.totalBorrowedDisbursed
                val adminVaultTotalValue = remember(adminVaultItems) { adminVaultItems.sumOf { it.estimatedValue } }
                val unassignedVisitsCount = remember(adminVisits) { adminVisits.count { it.agentId == "UNASSIGNED" || it.agentId.isBlank() } }
                val pendingDocsCount = remember(adminApplications) { adminApplications.count { it.status == "PENDING" } }
                val openComplaintsCount = remember(adminComplaints) { adminComplaints.count { it.status == "OPEN" || it.status == "INVESTIGATING" } }

                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
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
                                    border = BorderStroke(1.dp, Gold500.copy(alpha = 0.5f)),
                                    modifier = Modifier.size(42.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text("👑", fontSize = 22.sp)
                                    }
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text("APP OWNER & MASTER ADMIN", fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFFB45309))
                                    Text(
                                        text = state.user?.username?.let { if (it.startsWith("@")) it else "@$it" } ?: "@admin",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF0F172A)
                                    )
                                    Text("Institutional Reserve Custodian", fontSize = 10.sp, color = Color(0xFF64748B))
                                }
                            }
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Gold500.copy(alpha = 0.15f),
                                border = BorderStroke(1.dp, Gold500),
                                modifier = Modifier.clickable { onNavigateToAdminHub(0) }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text("HUB", fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFFB45309))
                                    Icon(Icons.Default.ArrowForward, contentDescription = null, tint = Color(0xFFB45309), modifier = Modifier.size(12.dp))
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // 3 Institutional Metrics Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFFF8FAFC),
                                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text(stringResource(R.string.escrow_vault), fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFF64748B))
                                    Text(
                                        text = "₹${(adminVaultTotalValue / 100000).formatDecimal(1)}L",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color(0xFFB45309)
                                    )
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFFF8FAFC),
                                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text(stringResource(R.string.platform_vol), fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFF64748B))
                                    Text(
                                        text = totalVol.toInrString(),
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color(0xFF0F172A)
                                    )
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFFF8FAFC),
                                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text("ACTIVE LOANS", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFF64748B))
                                    Text(
                                        text = "${state.loansAsLender.count { it.status == "ACTIVE" } + state.loansAsBorrower.count { it.status == "ACTIVE" }} Active",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Emerald500
                                    )
                                }
                            }
                        }

                        // Urgent Pending Review Queue Banner
                        val pendingAgentApps = remember(adminApplications) { adminApplications.filter { it.status == "PENDING" } }
                        val pendingAdminReqs = remember(adminRequests) { adminRequests.filter { it.status == "PENDING" } }
                        val totalPendingAlerts = pendingAgentApps.size + pendingAdminReqs.size

                        if (totalPendingAlerts > 0) {
                            Spacer(modifier = Modifier.height(14.dp))
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = Color(0xFFFFFBEB),
                                border = BorderStroke(1.5.dp, Gold500.copy(alpha = 0.7f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onNavigateToAdminHub(0) }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        modifier = Modifier.weight(1f),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Surface(
                                            shape = CircleShape,
                                            color = Color(0xFFEF4444)
                                        ) {
                                            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(26.dp)) {
                                                Text(
                                                    text = "$totalPendingAlerts",
                                                    color = Color.White,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                        Column {
                                            Text(
                                                text = "ACTION REQUIRED: PENDING DOSSIERS",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = Color(0xFFB45309),
                                                letterSpacing = 0.3.sp
                                            )
                                            val summaryText = buildString {
                                                if (pendingAgentApps.isNotEmpty()) append("${pendingAgentApps.size} Agent Application${if (pendingAgentApps.size > 1) "s" else ""}")
                                                if (pendingAgentApps.isNotEmpty() && pendingAdminReqs.isNotEmpty()) append(" • ")
                                                if (pendingAdminReqs.isNotEmpty()) append("${pendingAdminReqs.size} Elevation Request${if (pendingAdminReqs.size > 1) "s" else ""}")
                                            }
                                            Text(
                                                text = summaryText,
                                                fontSize = 11.sp,
                                                color = Color(0xFF0F172A),
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = Gold500,
                                        contentColor = Navy900
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Text("Review ➔", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        // 4-Module Executive Operational Matrix (2x2 Grid)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "OPERATIONAL COMMAND MODULES",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF0F172A),
                                letterSpacing = 0.5.sp
                            )
                            Text(
                                text = "Institutional Console",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Emerald500
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        val dashDesks = listOf(
                            DashboardAdminDesk(0, "Identity & Agents", Icons.Default.People, "${adminApplications.size} Agents • $pendingDocsCount Pending", pendingDocsCount, if (pendingDocsCount > 0) Gold500 else Emerald400),
                            DashboardAdminDesk(1, "Field Dispatch", Icons.Default.NearMe, "${adminVisits.size} Visits • $unassignedVisitsCount Unassigned", unassignedVisitsCount, if (unassignedVisitsCount > 0) Color(0xFFF97316) else Emerald400),
                            DashboardAdminDesk(2, "Vault & NOC", Icons.Default.Diamond, "${adminVaultItems.size} Lockers • ${adminNocs.size} NOCs", 0, Emerald400),
                            DashboardAdminDesk(3, "Disputes & Support", Icons.Default.Gavel, "$openComplaintsCount Open • ${adminMeetings.size} Hearings", openComplaintsCount, if (openComplaintsCount > 0) Color(0xFFEF4444) else Emerald400)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            DashboardAdminDeskCard(
                                desk = dashDesks[0],
                                onClick = { onNavigateToAdminHub(0) },
                                modifier = Modifier.weight(1f)
                            )
                            DashboardAdminDeskCard(
                                desk = dashDesks[1],
                                onClick = { onNavigateToAdminHub(1) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            DashboardAdminDeskCard(
                                desk = dashDesks[2],
                                onClick = { onNavigateToAdminHub(2) },
                                modifier = Modifier.weight(1f)
                            )
                            DashboardAdminDeskCard(
                                desk = dashDesks[3],
                                onClick = { onNavigateToAdminHub(3) },
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        OutlinedButton(
                            onClick = onNavigateToLoansTab,
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = Color(0xFFF8FAFC),
                                contentColor = Color(0xFF0F172A)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.AdminPanelSettings, contentDescription = null, tint = Color(0xFF0F172A), modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Manage Platform Loans & Custody Ledger", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        } else {
            // ==========================================
            // 👤 STANDARD MEMBER CONSUMER HERO
            // ==========================================
            if (state.user != null && !questDismissed) {
                item {
                    InteractiveGettingStartedQuestCard(
                        isCommunityDone = questCommunityDone,
                        isCalculatorDone = questCalculatorDone,
                        isKycDone = questKycDone,
                        onExploreCommunity = {
                            scope.launch {
                                userRepository.markQuestStepDone(com.loanzo.app.data.repository.UserRepository.QUEST_COMMUNITY_EXPLORED)
                            }
                        },
                        onOpenCalculator = {
                            scope.launch {
                                userRepository.markQuestStepDone(com.loanzo.app.data.repository.UserRepository.QUEST_CALCULATOR_TRIED)
                            }
                            onNavigateToCalculator()
                        },
                        onVerifyKyc = {
                            onNavigateToKyc()
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
                        .clickable { onNavigateToPortfolio() }
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
                                    text = stringResource(R.string.dashboard_portfolio_overview),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = stringResource(R.string.realtime_capital_balance),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Gray400
                                )
                            }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Emerald400.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = stringResource(R.string.active),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Emerald400,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Surface(
                                shape = CircleShape,
                                color = Color.White.copy(alpha = 0.12f),
                                modifier = Modifier.size(24.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                        contentDescription = "Smart Portfolio",
                                        tint = GoldCoinBright,
                                        modifier = Modifier.size(13.dp)
                                    )
                                }
                            }
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
                                text = stringResource(R.string.active_portfolio_value),
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
                                .clickable { onNavigateToPortfolio() }
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
                                        text = stringResource(R.string.lent_out),
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
                                    text = stringResource(R.string.active_loans_count, state.loansAsLender.count { it.status == "ACTIVE" }),
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
                                .clickable { onNavigateToPortfolio() }
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
                                        text = stringResource(R.string.borrowed),
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
                                    text = stringResource(R.string.active_debts_count, state.loansAsBorrower.count { it.status == "ACTIVE" }),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Gray400
                                )
                            }
                        }
                    }
                }
            }
        }
        // ─── MY LOANS & ENGAGEMENTS SECTION (FOR CONSUMER BORROWERS & LENDERS ONLY) ───
        if (!isAdmin) {
            item {
                val allActiveLoans = remember(state.loansAsLender, state.loansAsBorrower) {
                    (state.loansAsLender + state.loansAsBorrower)
                        .filter { it.status != "CLOSED" && it.status != "COMPLETED" }
                        .distinctBy { it.loanId }
                        .sortedByDescending { it.createdAt }
                }
                val activeLentLoans = remember(state.loansAsLender) {
                    state.loansAsLender
                        .filter { it.status != "CLOSED" && it.status != "COMPLETED" }
                        .sortedByDescending { it.createdAt }
                }
                val activeBorrowedLoans = remember(state.loansAsBorrower) {
                    state.loansAsBorrower
                        .filter { it.status != "CLOSED" && it.status != "COMPLETED" }
                        .sortedByDescending { it.createdAt }
                }

                val pagerState = rememberPagerState(initialPage = 0) { 3 }
                val tabs = listOf(
                    "All (${allActiveLoans.size})",
                    "Lent (${activeLentLoans.size})",
                    "Borrowed (${activeBorrowedLoans.size})"
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                ) {
                    // Section Header with "History ➔"
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "My Loans & Engagements",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (allActiveLoans.isNotEmpty()) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = Emerald400.copy(alpha = 0.15f),
                                    border = BorderStroke(1.dp, Emerald400.copy(alpha = 0.3f))
                                ) {
                                    Text(
                                        text = "${allActiveLoans.size} ACTIVE",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Emerald400,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 9.5.sp,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }

                        TextButton(onClick = onNavigateToLoansTab) {
                            Text(
                                text = "History ➔",
                                color = Gold500,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.5.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Synced Segmented Capsule Tabs
                    SegmentedCapsuleTab(
                        tabs = tabs,
                        selectedIndex = pagerState.currentPage,
                        onTabSelected = { idx ->
                            scope.launch {
                                pagerState.animateScrollToPage(idx)
                            }
                        }
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Swipeable HorizontalPager
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.fillMaxWidth()
                    ) { page ->
                        val pageLoans = when (page) {
                            1 -> activeLentLoans
                            2 -> activeBorrowedLoans
                            else -> allActiveLoans
                        }

                        if (pageLoans.isEmpty()) {
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(24.dp)
                                ) {
                                    Icon(
                                        imageVector = when (page) {
                                            1 -> Icons.Default.Savings
                                            2 -> Icons.Default.CreditCard
                                            else -> Icons.Default.ReceiptLong
                                        },
                                        contentDescription = null,
                                        tint = Gold500,
                                        modifier = Modifier.size(36.dp)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = when (page) {
                                            1 -> "No loans lent out"
                                            2 -> "No active debts"
                                            else -> "No active loans right now"
                                        },
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = when (page) {
                                            1 -> "Offer capital or grant peer loans to earn monthly interest."
                                            2 -> "Borrow directly from trusted peers with transparent terms."
                                            else -> "Grant or request peer loans with complete transparency."
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Gray400,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                pageLoans.take(5).forEach { loan ->
                                    val isLender = state.loansAsLender.any { it.loanId == loan.loanId }
                                    HomeLoanCard(
                                        loan = loan,
                                        isLender = isLender,
                                        onNavigateToDetail = onNavigateToLoanDetail,
                                        onNavigateToChat = onNavigateToChat
                                    )
                                }
                                if (pageLoans.size > 5) {
                                    TextButton(
                                        onClick = onNavigateToLoansTab,
                                        modifier = Modifier.align(Alignment.CenterHorizontally)
                                    ) {
                                        Text(
                                            text = "View all ${pageLoans.size} loans ➔",
                                            color = Gold500,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.5.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
            }


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
    }

    // Smooth backdrop scrim when 3-dots popup is open (dismisses popup on click)
    if (menuScrimAlpha > 0.01f) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = menuScrimAlpha))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { isMenuExpanded = false }
        )
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
                selectedVisitForInspection = null
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
                    }
                }
            },
            onCompleteDetailedInspection = { remarks, collOk, bOk, lOk, proof, appraisal, rec ->
                selectedVisitForInspection = null
                scope.launch(kotlinx.coroutines.Dispatchers.IO) {
                    agentRepository.completeVisit(
                        visitId = visit.visitId,
                        agentRemarks = remarks,
                        isCollateralAuthentic = collOk,
                        isBorrowerIdentityVerified = bOk,
                        isLenderIdentityVerified = lOk,
                        proofPhotoUris = proof,
                        appraisedValue = appraisal,
                        officerRecommendation = rec
                    )
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                        android.widget.Toast.makeText(context, "Inspection completed & ₹${visit.payoutAmount.toInt()} payout credited!", android.widget.Toast.LENGTH_SHORT).show()
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
}
}

data class DashboardAdminDesk(
    val id: Int,
    val title: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val subtitle: String,
    val alertCount: Int = 0,
    val alertColor: Color = Color(0xFFEF4444)
)

@Composable
private fun DashboardAdminDeskCard(
    desk: DashboardAdminDesk,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        color = Color(0xFFF8FAFC),
        border = BorderStroke(1.dp, if (desk.alertCount > 0) desk.alertColor.copy(alpha = 0.6f) else Color(0xFFE2E8F0)),
        modifier = modifier.height(54.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color.White,
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier.size(30.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(desk.icon, contentDescription = desk.title, tint = Color(0xFFB45309), modifier = Modifier.size(16.dp))
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(desk.title, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A), maxLines = 1, softWrap = false)
                    Text(desk.subtitle, fontSize = 9.sp, color = Color(0xFF64748B), maxLines = 1, softWrap = false)
                }
            }
            if (desk.alertCount > 0) {
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(desk.alertColor)
                        .padding(horizontal = 5.dp, vertical = 1.dp)
                ) {
                    Text(
                        text = "${desk.alertCount}",
                        color = Color.White,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
        }
    }
}

private fun Double.formatDecimal(decimals: Int): String {
    return String.format(java.util.Locale.getDefault(), "%.${decimals}f", this)
}

@Composable
fun HomeLoanCard(
    loan: LoanEntity,
    isLender: Boolean,
    onNavigateToDetail: (String) -> Unit,
    onNavigateToChat: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val statusColor = when (loan.status) {
        "ACTIVE", "ACTIVE_SERVICING" -> Emerald400
        "CLOSED", "COMPLETED" -> Gray400
        "DELINQUENT", "LEGAL_DISPUTE" -> Red400
        "CONTRACT_SIGNING", "BID_ACCEPTED" -> Blue400
        else -> Gold500
    }
    val roleLabel = if (isLender) "Lending Out" else "Borrowed"
    val roleColor = if (isLender) Emerald400 else GoldCoinBright
    val repaidAmount = (loan.sanctionedAmount - loan.outstandingAmount).coerceAtLeast(0.0)
    val progressRatio = if (loan.sanctionedAmount > 0) (repaidAmount / loan.sanctionedAmount).toFloat() else 0f
    val progressPercent = (progressRatio * 100).toInt().coerceIn(0, 100)

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        modifier = modifier
            .fillMaxWidth()
            .clickable { onNavigateToDetail(loan.loanId) }
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header Row: Role Pill + Loan ID + Status Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = roleColor.copy(alpha = 0.14f),
                        border = BorderStroke(0.5.dp, roleColor.copy(alpha = 0.35f))
                    ) {
                        Text(
                            text = roleLabel,
                            color = roleColor,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Text(
                        text = "#${loan.loanId.take(8)}",
                        fontSize = 11.sp,
                        color = Gray400,
                        fontWeight = FontWeight.Medium
                    )
                }

                StatusBadge(text = loan.status.replace("_", " "), color = statusColor)
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Purpose and Amount
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = loan.purpose.ifBlank { "Personal Loan" },
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = loan.sanctionedAmount.toInrString(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Progress Bar & Outstanding Amount
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Repaid $progressPercent%",
                    fontSize = 11.sp,
                    color = Gray400,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "${loan.outstandingAmount.toInrString()} remaining",
                    fontSize = 11.5.sp,
                    color = if (loan.outstandingAmount > 0) roleColor else Emerald400,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            LinearProgressIndicator(
                progress = { progressRatio.coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = roleColor,
                trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Actions Row: Terms info + Chat + Details/Pay
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${loan.interestRate}% APR • ${loan.tenureMonths} mo",
                    fontSize = 11.sp,
                    color = Gray400
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconButton(
                        onClick = { onNavigateToChat(loan.loanId) },
                        modifier = Modifier.size(30.dp)
                    ) {
                        Icon(
                            Icons.Default.ChatBubbleOutline,
                            contentDescription = "Chat",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                        modifier = Modifier.clickable { onNavigateToDetail(loan.loanId) }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (!isLender && loan.outstandingAmount > 0) "Pay EMI ➔" else "Details ➔",
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DashboardMenuRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    iconBg: Color,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = Color.Transparent,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = iconBg,
                modifier = Modifier.size(34.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(18.dp))
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.5.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = 10.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
