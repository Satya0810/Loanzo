package com.loanzo.app.ui.dashboard

import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    state: DashboardUiState,
    onNavigateToCreateLoan: () -> Unit = {},
    onNavigateToGrantLoan: () -> Unit = onNavigateToCreateLoan,
    onNavigateToRequestLoan: () -> Unit = onNavigateToCreateLoan,
    onNavigateToCalculator: () -> Unit,
    onNavigateToLoanDetail: (String) -> Unit,
    onNavigateToProfile: () -> Unit = {},
    onNavigateToFinancialHealth: () -> Unit,
    onNavigateToApproval: (String) -> Unit,
    onNavigateToLoansTab: () -> Unit = {},
    onNavigateToChat: (String) -> Unit = {}
) {
    var isMenuExpanded by remember { mutableStateOf(false) }
    var showChatSheet by remember { mutableStateOf(false) }
    var showReportSheet by remember { mutableStateOf(false) }

    val userRepository = com.loanzo.app.util.LocalUserRepository.current
    val dashboardGuideSeen by userRepository.isGuideSeen(com.loanzo.app.data.repository.UserRepository.GUIDE_DASHBOARD_SEEN)
        .collectAsStateWithLifecycle(initialValue = true)
    val scope = rememberCoroutineScope()

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
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Navy800, MaterialTheme.colorScheme.background)
                        )
                    )
                    .padding(horizontal = 20.dp, vertical = 24.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Image(
                                painter = painterResource(id = R.drawable.app_logo),
                                contentDescription = "Loanzo",
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(RoundedCornerShape(12.dp))
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                val dateStr = remember {
                                    java.text.SimpleDateFormat("EEEE, d MMMM", java.util.Locale.getDefault()).format(java.util.Date())
                                }
                                Text(
                                    text = dateStr.uppercase(),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Gold500,
                                    letterSpacing = 1.sp
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Hello, ${state.user?.name?.split(" ")?.firstOrNull() ?: "User"} 👋",
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = "Welcome to Loanzo",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Gray400
                                )
                            }
                        }
                        Box {
                            IconButton(
                                onClick = { isMenuExpanded = true },
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(SurfaceDarkCard)
                                    .border(1.dp, Gold500.copy(alpha = 0.35f), CircleShape)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MoreVert,
                                    contentDescription = "More Options",
                                    tint = Gold500
                                )
                            }

                            DropdownMenu(
                                expanded = isMenuExpanded,
                                onDismissRequest = { isMenuExpanded = false },
                                modifier = Modifier
                                    .background(SurfaceDarkElevated)
                                    .border(1.dp, Gray700, RoundedCornerShape(14.dp))
                            ) {
                                // Option 1: Chat
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text(
                                                text = "Chat",
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White,
                                                fontSize = 14.sp
                                            )
                                            Text(
                                                text = "Direct messages & bot",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = Gray400,
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
                                            color = Gold500.copy(alpha = 0.15f),
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.Chat,
                                                contentDescription = "Chat",
                                                tint = Gold500,
                                                modifier = Modifier.padding(7.dp)
                                            )
                                        }
                                    }
                                )

                                HorizontalDivider(color = Gray800, thickness = 0.5.dp)

                                // Option 2: Report (for taking action on anyone)
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text(
                                                text = "Report",
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White,
                                                fontSize = 14.sp
                                            )
                                            Text(
                                                text = "Take action on anyone",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = Red400.copy(alpha = 0.85f),
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
                                            color = Red400.copy(alpha = 0.15f),
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

                                HorizontalDivider(color = Gray800, thickness = 0.5.dp)

                                // Option 3: Simulator
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text(
                                                text = "Simulator",
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White,
                                                fontSize = 14.sp
                                            )
                                            Text(
                                                text = "Loan & EMI calculator",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = Emerald400.copy(alpha = 0.85f),
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
                                            color = Emerald400.copy(alpha = 0.15f),
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
                            }
                        }
                    }
                }
            }
        }

        // Action buttons side-by-side
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = onNavigateToGrantLoan,
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp),
                    shape = RoundedCornerShape(16.dp),
                    contentPadding = PaddingValues(horizontal = 6.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Gold500,
                        contentColor = Navy900
                    )
                ) {
                    Icon(Icons.Default.Upload, null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Grant Loan",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        maxLines = 1,
                        softWrap = false
                    )
                }
                
                Button(
                    onClick = onNavigateToRequestLoan,
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp),
                    shape = RoundedCornerShape(16.dp),
                    contentPadding = PaddingValues(horizontal = 6.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Emerald400,
                        contentColor = Navy900
                    )
                ) {
                    Icon(Icons.Default.Download, null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Request Loan",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Unified Financial Portfolio Card on Home (No tabs - aggregate overview)
        item {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceDarkElevated),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = Brush.linearGradient(listOf(Gold500.copy(alpha = 0.4f), Emerald400.copy(alpha = 0.25f)))
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Portfolio Overview",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Gold500.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = "Active",
                                style = MaterialTheme.typography.labelSmall,
                                color = Gold500,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Total Lent Metric Card
                        Card(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = SurfaceDarkCard)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(Gold500)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Total Lent",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Gray400
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = state.totalLentDisbursed.toInrString(),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Gold500
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Outstanding: ${state.totalLentOutstanding.toInrString()}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Gray300,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        // Total Borrowed Metric Card
                        Card(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = SurfaceDarkCard)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(Emerald400)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Total Borrowed",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Gray400
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = state.totalBorrowedDisbursed.toInrString(),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Emerald400
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "To Repay: ${state.totalBorrowedOutstanding.toInrString()}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Gray300,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedButton(
                        onClick = onNavigateToLoansTab,
                        modifier = Modifier.fillMaxWidth().height(42.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Gold500),
                        border = ButtonDefaults.outlinedButtonBorder(enabled = true).copy(
                            brush = Brush.horizontalGradient(listOf(Gold500.copy(alpha = 0.5f), Emerald400.copy(alpha = 0.5f)))
                        )
                    ) {
                        Text("Manage All Loans in Loans Tab ➔", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
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
        
        // Utilities buttons
        item {
            OutlinedButton(
                onClick = onNavigateToCalculator,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(Icons.Default.Calculate, null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.loan_calculator), fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedButton(
                onClick = onNavigateToFinancialHealth,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(Icons.Default.Analytics, null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Financial Health", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
            Spacer(modifier = Modifier.height(24.dp))
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
                body = "View active loans, pending approvals, financial health score, and quick actions — all from one place.",
                onDismiss = {
                    scope.launch {
                        userRepository.markGuideSeen(com.loanzo.app.data.repository.UserRepository.GUIDE_DASHBOARD_SEEN)
                    }
                },
                autoDismissSeconds = 8
            )
        }
    }
    }
}
