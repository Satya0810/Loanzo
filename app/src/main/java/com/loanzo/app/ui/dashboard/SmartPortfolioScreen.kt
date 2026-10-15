package com.loanzo.app.ui.dashboard

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.loanzo.app.data.entity.LoanEntity
import com.loanzo.app.ui.theme.*
import com.loanzo.app.util.toInrString
import java.text.SimpleDateFormat
import java.util.*

/**
 * Institutional Portfolio & Cash Flow Analytics Dashboard.
 * Designed with deterministic financial metrics, dual-ledger balance tracking,
 * month-by-month liquidity forecasting, and direct loan position inventory.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmartPortfolioScreen(
    state: DashboardUiState,
    onBack: () -> Unit,
    onNavigateToLoanDetail: (String) -> Unit = {},
    onRefresh: () -> Unit = {}
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Overview, 1: Holdings, 2: Cash Flow
    var showStatementSheet by remember { mutableStateOf(false) }

    // Aggregate Financial Numbers
    val totalLentDisbursed = state.totalLentDisbursed
    val totalLentOutstanding = state.totalLentOutstanding
    val totalBorrowedDisbursed = state.totalBorrowedDisbursed
    val totalBorrowedOutstanding = state.totalBorrowedOutstanding

    val netCapitalDelta = totalLentDisbursed - totalBorrowedDisbursed
    val netOutstandingDelta = totalLentOutstanding - totalBorrowedOutstanding
    val totalCapitalVolume = totalLentDisbursed + totalBorrowedDisbursed

    val lentProportion = if (totalCapitalVolume > 0) (totalLentDisbursed / totalCapitalVolume).toFloat() else 0.5f
    val borrowedProportion = if (totalCapitalVolume > 0) (totalBorrowedDisbursed / totalCapitalVolume).toFloat() else 0.5f

    val allLoans = remember(state.loansAsLender, state.loansAsBorrower) {
        state.loansAsLender + state.loansAsBorrower
    }

    val activeLentLoans = remember(state.loansAsLender) {
        state.loansAsLender.filter { it.status == "ACTIVE" || it.status == "ACTIVE_SERVICING" }
    }
    val activeBorrowedLoans = remember(state.loansAsBorrower) {
        state.loansAsBorrower.filter { it.status == "ACTIVE" || it.status == "ACTIVE_SERVICING" }
    }

    // Weighted APR calculation based on outstanding balance
    val weightedApr = remember(allLoans) {
        val totalOut = allLoans.sumOf { it.outstandingAmount }
        if (totalOut > 0) {
            allLoans.sumOf { it.outstandingAmount * it.interestRate } / totalOut
        } else if (allLoans.isNotEmpty()) {
            allLoans.map { it.interestRate }.average()
        } else {
            12.0
        }
    }

    // Asset Quality & NPA Tracking
    val overdueCount = state.overdueRepaymentsAsBorrower.size + state.overdueRepaymentsAsLender.size
    val overdueAmount = remember(state.overdueRepaymentsAsBorrower, state.overdueRepaymentsAsLender) {
        state.overdueRepaymentsAsBorrower.sumOf { it.amount } + state.overdueRepaymentsAsLender.sumOf { it.amount }
    }
    val totalActiveOutstanding = totalLentOutstanding + totalBorrowedOutstanding
    val npaRatio = if (totalActiveOutstanding > 0) {
        ((overdueAmount / totalActiveOutstanding) * 100.0).coerceIn(0.0, 100.0)
    } else 0.0
    val performingAssetRatio = (100.0 - npaRatio).coerceIn(0.0, 100.0)

    // Dynamic 4-Month Forward Cash Flow Projection
    val monthlyCashflowList = remember(activeLentLoans, activeBorrowedLoans) {
        computeForwardCashFlow(activeLentLoans, activeBorrowedLoans, monthsAhead = 4)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Portfolio Analytics",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = TextNavyDark
                        )
                        Text(
                            text = "Net position, cash flow, and risk distribution",
                            fontSize = 11.5.sp,
                            color = TextSlateMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = TextNavyDark
                        )
                    }
                },
                actions = {
                    IconButton(onClick = {
                        onRefresh()
                        Toast.makeText(context, "Refreshing portfolio metrics...", Toast.LENGTH_SHORT).show()
                    }) {
                        Icon(
                            imageVector = Icons.Default.Sync,
                            contentDescription = "Refresh Data",
                            tint = TextSlateMedium
                        )
                    }
                    IconButton(onClick = { showStatementSheet = true }) {
                        Icon(
                            imageVector = Icons.Outlined.ReceiptLong,
                            contentDescription = "Portfolio Statement",
                            tint = BrandRoyalBlue
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = CanvasPorcelain
                )
            )
        },
        containerColor = CanvasPorcelain
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Segmented Navigation Header (Overview | Holdings | Cash Flow)
            PortfolioSegmentedBar(
                selectedTab = selectedTab,
                holdingsCount = allLoans.size,
                onTabSelect = { selectedTab = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )

            when (selectedTab) {
                0 -> {
                    // TAB 0: OVERVIEW & EXECUTIVE BALANCE SHEET
                    PortfolioOverviewTab(
                        netCapitalDelta = netCapitalDelta,
                        netOutstandingDelta = netOutstandingDelta,
                        totalLentDisbursed = totalLentDisbursed,
                        totalLentOutstanding = totalLentOutstanding,
                        activeLentCount = activeLentLoans.size,
                        totalBorrowedDisbursed = totalBorrowedDisbursed,
                        totalBorrowedOutstanding = totalBorrowedOutstanding,
                        activeBorrowedCount = activeBorrowedLoans.size,
                        lentRatio = lentProportion,
                        borrowedRatio = borrowedProportion,
                        weightedApr = weightedApr,
                        performingRatio = performingAssetRatio,
                        npaRatio = npaRatio,
                        overdueCount = overdueCount,
                        overdueAmount = overdueAmount,
                        allLoans = allLoans,
                        monthlyCashflow = monthlyCashflowList,
                        onViewHoldings = { selectedTab = 1 },
                        onViewCashFlow = { selectedTab = 2 }
                    )
                }
                1 -> {
                    // TAB 1: HOLDINGS & LOAN POSITIONS (Clickable inventory)
                    PortfolioHoldingsTab(
                        allLoans = allLoans,
                        onNavigateToLoanDetail = onNavigateToLoanDetail
                    )
                }
                2 -> {
                    // TAB 2: DETAILED CASH FLOW SCHEDULE
                    PortfolioCashFlowScheduleTab(
                        monthlyCashflow = monthlyCashflowList,
                        activeLentLoans = activeLentLoans,
                        activeBorrowedLoans = activeBorrowedLoans,
                        onNavigateToLoanDetail = onNavigateToLoanDetail
                    )
                }
            }
        }
    }

    // Executive Statement Bottom Sheet
    if (showStatementSheet) {
        PortfolioStatementSheet(
            totalLent = totalLentDisbursed,
            outstandingLent = totalLentOutstanding,
            totalBorrowed = totalBorrowedDisbursed,
            outstandingBorrowed = totalBorrowedOutstanding,
            netPosition = netCapitalDelta,
            activeLoansCount = activeLentLoans.size + activeBorrowedLoans.size,
            weightedApr = weightedApr,
            npaRatio = npaRatio,
            onDismiss = { showStatementSheet = false }
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// COMPONENT 1: SEGMENTED SWITCHER (OVERVIEW | HOLDINGS | CASH FLOW)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun PortfolioSegmentedBar(
    selectedTab: Int,
    holdingsCount: Int,
    onTabSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color.White,
        border = BorderStroke(1.dp, BrandIceBorder),
        modifier = modifier.height(44.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val tabs = listOf(
                "Overview",
                if (holdingsCount > 0) "Holdings ($holdingsCount)" else "Holdings",
                "Cash Flow"
            )

            tabs.forEachIndexed { index, label ->
                val isSelected = selectedTab == index
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(9.dp))
                        .background(if (isSelected) BrandRoyalBlue else Color.Transparent)
                        .clickable { onTabSelect(index) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) Color.White else TextSlateMedium,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// TAB 0: OVERVIEW TAB (Balance Sheet, Cash Flow Forecast, Risk Matrix)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun PortfolioOverviewTab(
    netCapitalDelta: Double,
    netOutstandingDelta: Double,
    totalLentDisbursed: Double,
    totalLentOutstanding: Double,
    activeLentCount: Int,
    totalBorrowedDisbursed: Double,
    totalBorrowedOutstanding: Double,
    activeBorrowedCount: Int,
    lentRatio: Float,
    borrowedRatio: Float,
    weightedApr: Double,
    performingRatio: Double,
    npaRatio: Double,
    overdueCount: Int,
    overdueAmount: Double,
    allLoans: List<LoanEntity>,
    monthlyCashflow: List<MonthlyCashflowItem>,
    onViewHoldings: () -> Unit,
    onViewCashFlow: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // ── 1. EXECUTIVE BALANCE CARD ──
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color.White,
            border = BorderStroke(1.dp, BrandIceBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column {
                        Text(
                            text = "NET CAPITAL POSITION",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.8.sp,
                            color = TextSlateMuted
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = (if (netCapitalDelta >= 0) "+" else "-") + kotlin.math.abs(netCapitalDelta).toInrString(),
                            fontSize = 24.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (netCapitalDelta >= 0) Emerald500 else GoldCoinAmber
                        )
                        Text(
                            text = "Net Outstanding: ${(if (netOutstandingDelta >= 0) "+" else "-")}${kotlin.math.abs(netOutstandingDelta).toInrString()}",
                            fontSize = 11.5.sp,
                            color = TextSlateMuted
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (netCapitalDelta >= 0) EmeraldLight else GoldCoinCream,
                        border = BorderStroke(1.dp, if (netCapitalDelta >= 0) EmeraldBorder else GoldCoinBorder)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = if (netCapitalDelta >= 0) Icons.AutoMirrored.Filled.TrendingUp else Icons.AutoMirrored.Filled.TrendingDown,
                                contentDescription = null,
                                tint = if (netCapitalDelta >= 0) Emerald500 else GoldCoinAmber,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (netCapitalDelta >= 0) "Net Creditor" else "Net Debtor",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (netCapitalDelta >= 0) Emerald500 else GoldCoinAmber
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = BrandIceBorder.copy(alpha = 0.6f))
                Spacer(modifier = Modifier.height(16.dp))

                // Parallel Dual-Ledger Columns (Lent vs Borrowed)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Column A: Capital Lent (Receivables)
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = CanvasPorcelain,
                        border = BorderStroke(1.dp, BrandIceBorder),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(Emerald500)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Capital Lent",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextSlateMedium
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = totalLentDisbursed.toInrString(),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextNavyDark
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Active: ${totalLentOutstanding.toInrString()}",
                                fontSize = 10.5.sp,
                                color = TextSlateMuted
                            )
                            Text(
                                text = "$activeLentCount Active Loans",
                                fontSize = 10.5.sp,
                                color = Emerald500,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    // Column B: Capital Borrowed (Liabilities)
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = CanvasPorcelain,
                        border = BorderStroke(1.dp, BrandIceBorder),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(GoldCoinAmber)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Capital Borrowed",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextSlateMedium
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = totalBorrowedDisbursed.toInrString(),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextNavyDark
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Active: ${totalBorrowedOutstanding.toInrString()}",
                                fontSize = 10.5.sp,
                                color = TextSlateMuted
                            )
                            Text(
                                text = "$activeBorrowedCount Active Debts",
                                fontSize = 10.5.sp,
                                color = GoldCoinAmber,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Proportional Ratio Track
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Lent ${(lentRatio * 100).toInt()}%",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Emerald500
                        )
                        Text(
                            text = "Borrowed ${(borrowedRatio * 100).toInt()}%",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = GoldCoinAmber
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(BrandIceBorder)
                    ) {
                        Row(modifier = Modifier.fillMaxSize()) {
                            if (lentRatio > 0.001f) {
                                Box(
                                    modifier = Modifier
                                        .weight(lentRatio.coerceAtLeast(0.01f))
                                        .fillMaxHeight()
                                        .background(Emerald500)
                                )
                            }
                            if (borrowedRatio > 0.001f) {
                                Box(
                                    modifier = Modifier
                                        .weight(borrowedRatio.coerceAtLeast(0.01f))
                                        .fillMaxHeight()
                                        .background(GoldCoinAmber)
                                )
                            }
                        }
                    }
                }
            }
        }

        // ── 2. DYNAMIC 4-MONTH CASH FLOW FORECAST CHART ──
        ModernCashFlowForecastChart(
            monthlyCashflow = monthlyCashflow,
            onViewDetailedSchedule = onViewCashFlow
        )

        // ── 3. PORTFOLIO ALLOCATION & RISK HEALTH ──
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color.White,
            border = BorderStroke(1.dp, BrandIceBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text(
                    text = "Asset Allocation & Quality",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextNavyDark
                )
                Text(
                    text = "Sector distribution and portfolio risk health",
                    fontSize = 11.sp,
                    color = TextSlateMuted
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Categorical Distribution Bar
                PortfolioCategoryDistributionSection(allLoans = allLoans)

                Spacer(modifier = Modifier.height(16.dp))

                // Key Financial Risk Grid (2x2)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MetricMiniCard(
                        icon = Icons.Outlined.Percent,
                        iconTint = BrandRoyalBlue,
                        title = "Weighted APR",
                        value = String.format(Locale.getDefault(), "%.1f%% p.a.", weightedApr),
                        subtitle = "Average portfolio yield",
                        modifier = Modifier.weight(1f)
                    )
                    MetricMiniCard(
                        icon = Icons.Outlined.CheckCircle,
                        iconTint = if (performingRatio >= 95) Emerald500 else GoldCoinAmber,
                        title = "Performing Assets",
                        value = String.format(Locale.getDefault(), "%.1f%%", performingRatio),
                        subtitle = if (overdueCount == 0) "Zero overdue debts" else "$overdueCount overdue payments",
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MetricMiniCard(
                        icon = Icons.Outlined.Warning,
                        iconTint = if (npaRatio == 0.0) Emerald500 else Red500,
                        title = "NPA Exposure",
                        value = if (npaRatio == 0.0) "0.0%" else String.format(Locale.getDefault(), "%.1f%%", npaRatio),
                        subtitle = if (overdueAmount > 0) "${overdueAmount.toInrString()} in default" else "No default risk",
                        modifier = Modifier.weight(1f)
                    )
                    MetricMiniCard(
                        icon = Icons.Outlined.AccountBalance,
                        iconTint = Color(0xFF6366F1),
                        title = "Total Active Volume",
                        value = (totalLentOutstanding + totalBorrowedOutstanding).toInrString(),
                        subtitle = "${allLoans.count { it.status == "ACTIVE" || it.status == "ACTIVE_SERVICING" }} active contracts",
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // ── 4. QUICK LINK TO ACTIVE HOLDINGS ──
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = BrandIceBlue,
            border = BorderStroke(1.dp, BrandIceBorder),
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onViewHoldings() }
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.PieChart,
                        contentDescription = null,
                        tint = BrandRoyalBlue,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "View All ${allLoans.size} Loan Positions",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextNavyDark
                        )
                        Text(
                            text = "Filter by investments, debts, and contract status",
                            fontSize = 11.sp,
                            color = TextSlateMuted
                        )
                    }
                }
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = BrandRoyalBlue,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// TAB 1: HOLDINGS & LOAN POSITIONS (Clickable inventory)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun PortfolioHoldingsTab(
    allLoans: List<LoanEntity>,
    onNavigateToLoanDetail: (String) -> Unit
) {
    var filterIndex by remember { mutableIntStateOf(0) } // 0: All, 1: Investments (Lent), 2: Debts (Borrowed), 3: Delinquent
    var searchQuery by remember { mutableStateOf("") }

    val filteredLoans = remember(allLoans, filterIndex, searchQuery) {
        allLoans.filter { loan ->
            val matchesFilter = when (filterIndex) {
                1 -> loan.lenderId.isNotBlank() && loan.disbursedAmount > 0
                2 -> loan.borrowerId.isNotBlank() && loan.outstandingAmount > 0
                3 -> loan.status == "DELINQUENT" || loan.status == "LEGAL_DISPUTE"
                else -> true
            }
            val matchesSearch = searchQuery.isBlank() ||
                    loan.purpose.contains(searchQuery, ignoreCase = true) ||
                    loan.loanId.contains(searchQuery, ignoreCase = true) ||
                    loan.loanType.contains(searchQuery, ignoreCase = true)
            matchesFilter && matchesSearch
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        // Search TextField
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search by purpose, loan ID, or type", fontSize = 12.sp, color = TextSlateMuted) },
            leadingIcon = {
                Icon(Icons.Default.Search, contentDescription = "Search", tint = TextSlateMuted, modifier = Modifier.size(18.dp))
            },
            shape = RoundedCornerShape(10.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White,
                focusedBorderColor = BrandRoyalBlue,
                unfocusedBorderColor = BrandIceBorder
            ),
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Filter Pills Row
        val filters = listOf(
            "All (${allLoans.size})",
            "Investments",
            "Debts",
            "Delinquent"
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            filters.forEachIndexed { idx, label ->
                val isSelected = filterIndex == idx
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = if (isSelected) BrandRoyalBlue else Color.White,
                    border = BorderStroke(1.dp, if (isSelected) BrandRoyalBlue else BrandIceBorder),
                    modifier = Modifier.clickable { filterIndex = idx }
                ) {
                    Text(
                        text = label,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) Color.White else TextSlateMedium,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (filteredLoans.isEmpty()) {
            Spacer(modifier = Modifier.height(12.dp))
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color.White,
                border = BorderStroke(1.dp, BrandIceBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Surface(
                        shape = CircleShape,
                        color = if (allLoans.isEmpty()) BrandIceBlue else BrandAmberGold.copy(alpha = 0.12f),
                        modifier = Modifier.size(52.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (allLoans.isEmpty()) Icons.Outlined.AccountBalanceWallet else Icons.Outlined.SearchOff,
                                contentDescription = null,
                                tint = if (allLoans.isEmpty()) BrandRoyalBlue else GoldCoinAmber,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = if (allLoans.isEmpty()) "No Active Portfolio Positions" else "No Matching Contracts",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextNavyDark,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = if (allLoans.isEmpty()) {
                            "Loan disbursements, investments, and borrower obligations will automatically appear in this real-time ledger."
                        } else if (searchQuery.isNotBlank()) {
                            "No contracts match \"$searchQuery\". Try adjusting your search keyword."
                        } else {
                            "No loans recorded under this filter category."
                        },
                        fontSize = 12.sp,
                        color = TextSlateMuted,
                        textAlign = TextAlign.Center,
                        lineHeight = 17.sp
                    )

                    if (searchQuery.isNotBlank() || filterIndex != 0) {
                        Spacer(modifier = Modifier.height(16.dp))
                        OutlinedButton(
                            onClick = {
                                searchQuery = ""
                                filterIndex = 0
                            },
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, BrandRoyalBlue),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = BrandRoyalBlue
                            )
                        ) {
                            Text(
                                text = "Reset Filters",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                items(filteredLoans, key = { it.loanId }) { loan ->
                    LoanInventoryCard(
                        loan = loan,
                        onClick = { onNavigateToLoanDetail(loan.loanId) }
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// TAB 2: DETAILED CASH FLOW SCHEDULE
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun PortfolioCashFlowScheduleTab(
    monthlyCashflow: List<MonthlyCashflowItem>,
    activeLentLoans: List<LoanEntity>,
    activeBorrowedLoans: List<LoanEntity>,
    onNavigateToLoanDetail: (String) -> Unit
) {
    val hasActivePositions = activeLentLoans.isNotEmpty() || activeBorrowedLoans.isNotEmpty()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        item {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = BrandIceBlue,
                border = BorderStroke(1.dp, BrandIceBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.CalendarMonth,
                        contentDescription = null,
                        tint = BrandRoyalBlue,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Amortized EMI Projection",
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextNavyDark,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Calculated from active simple & compound amortisation schedules",
                            fontSize = 10.5.sp,
                            color = TextSlateMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        if (!hasActivePositions) {
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color.White,
                    border = BorderStroke(1.dp, BrandIceBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = BrandIceBlue,
                            modifier = Modifier.size(52.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Outlined.EventRepeat,
                                    contentDescription = null,
                                    tint = BrandRoyalBlue,
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "No Scheduled Cash Flow Obligations",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextNavyDark,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Your forward EMI collections and debt repayments will be automatically projected month-by-month as loans are disbursed and activated.",
                            fontSize = 12.sp,
                            color = TextSlateMuted,
                            textAlign = TextAlign.Center,
                            lineHeight = 17.sp
                        )

                        Spacer(modifier = Modifier.height(18.dp))

                        // 2-Card Feature Breakdown
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = CanvasPorcelain,
                                border = BorderStroke(1.dp, BrandIceBorder),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.TrendingUp,
                                        contentDescription = null,
                                        tint = Emerald500,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "Inward Liquidity",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextNavyDark,
                                        maxLines = 1
                                    )
                                    Text(
                                        text = "Tracks collections from lent capital",
                                        fontSize = 10.sp,
                                        color = TextSlateMuted,
                                        lineHeight = 13.sp
                                    )
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = CanvasPorcelain,
                                border = BorderStroke(1.dp, BrandIceBorder),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.TrendingDown,
                                        contentDescription = null,
                                        tint = GoldCoinAmber,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "Debt Servicing",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextNavyDark,
                                        maxLines = 1
                                    )
                                    Text(
                                        text = "Tracks upcoming borrower EMIs",
                                        fontSize = 10.sp,
                                        color = TextSlateMuted,
                                        lineHeight = 13.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        } else {
            items(monthlyCashflow) { month ->
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color.White,
                    border = BorderStroke(1.dp, BrandIceBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = month.monthLabel,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = TextNavyDark,
                                maxLines = 1
                            )
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (month.netDelta >= 0) EmeraldLight else GoldCoinCream,
                                border = BorderStroke(1.dp, if (month.netDelta >= 0) EmeraldBorder else GoldCoinBorder)
                            ) {
                                Text(
                                    text = "Net ${(if (month.netDelta >= 0) "+" else "-")}${kotlin.math.abs(month.netDelta).toInrString()}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (month.netDelta >= 0) Emerald500 else GoldCoinAmber,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                    maxLines = 1
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        HorizontalDivider(color = BrandIceBorder.copy(alpha = 0.5f))
                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Scheduled Inflows", fontSize = 10.5.sp, color = TextSlateMuted, maxLines = 1)
                                Text(
                                    text = month.inflow.toInrString(),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Emerald500,
                                    maxLines = 1
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("Scheduled Outflows", fontSize = 10.5.sp, color = TextSlateMuted, maxLines = 1)
                                Text(
                                    text = month.outflow.toInrString(),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = GoldCoinAmber,
                                    maxLines = 1
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Contributing active loans
                        if (activeLentLoans.isNotEmpty() || activeBorrowedLoans.isNotEmpty()) {
                            Text(
                                text = "Contributing Positions:",
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextSlateMedium,
                                maxLines = 1
                            )
                            Spacer(modifier = Modifier.height(6.dp))

                            activeLentLoans.take(2).forEach { loan ->
                                val emi = calculateMonthlyEmi(loan.outstandingAmount, loan.interestRate, loan.tenureMonths)
                                CashflowLineItem(
                                    title = "Collection • ${loan.purpose}",
                                    subtitle = "Loan ID: ...${loan.loanId.takeLast(6)} (${loan.interestRate}% APR)",
                                    amount = "+${emi.toInrString()}",
                                    isPositive = true,
                                    onClick = { onNavigateToLoanDetail(loan.loanId) }
                                )
                            }

                            activeBorrowedLoans.take(2).forEach { loan ->
                                val emi = calculateMonthlyEmi(loan.outstandingAmount, loan.interestRate, loan.tenureMonths)
                                CashflowLineItem(
                                    title = "Obligation • ${loan.purpose}",
                                    subtitle = "Loan ID: ...${loan.loanId.takeLast(6)} (${loan.interestRate}% APR)",
                                    amount = "-${emi.toInrString()}",
                                    isPositive = false,
                                    onClick = { onNavigateToLoanDetail(loan.loanId) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// SUB-COMPONENTS & HELPERS
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ModernCashFlowForecastChart(
    monthlyCashflow: List<MonthlyCashflowItem>,
    onViewDetailedSchedule: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedMonthIndex by remember { mutableIntStateOf(0) }
    val totalInflow = remember(monthlyCashflow) { monthlyCashflow.sumOf { it.inflow } }
    val totalOutflow = remember(monthlyCashflow) { monthlyCashflow.sumOf { it.outflow } }
    val netTotalDelta = totalInflow - totalOutflow
    val hasActiveFlow = totalInflow > 0 || totalOutflow > 0

    val safeSelectedIdx = selectedMonthIndex.coerceIn(0, (monthlyCashflow.size - 1).coerceAtLeast(0))
    val selectedItem = monthlyCashflow.getOrNull(safeSelectedIdx)

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        border = BorderStroke(1.dp, BrandIceBorder),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            // Header Row: Title & Action
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Cash Flow Forecast",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextNavyDark
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = BrandIceBlue
                        ) {
                            Text(
                                text = "4M OUTLOOK",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = BrandRoyalBlue,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Amortized collections vs obligations",
                        fontSize = 11.sp,
                        color = TextSlateMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                TextButton(onClick = onViewDetailedSchedule) {
                    Text(
                        text = "Schedule",
                        fontSize = 12.sp,
                        color = BrandRoyalBlue,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Dynamic Focused Month Overview Bar
            if (selectedItem != null) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = CanvasPorcelain,
                    border = BorderStroke(1.dp, BrandIceBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = selectedItem.monthLabel,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextNavyDark,
                                maxLines = 1
                            )
                            Text(
                                text = "Projected Position",
                                fontSize = 9.5.sp,
                                color = TextSlateMuted,
                                maxLines = 1
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "Inflow",
                                    fontSize = 9.5.sp,
                                    color = TextSlateMuted,
                                    maxLines = 1
                                )
                                Text(
                                    text = "+${selectedItem.inflow.toInrString()}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Emerald500,
                                    maxLines = 1
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "Outflow",
                                    fontSize = 9.5.sp,
                                    color = TextSlateMuted,
                                    maxLines = 1
                                )
                                Text(
                                    text = "-${selectedItem.outflow.toInrString()}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = GoldCoinAmber,
                                    maxLines = 1
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (selectedItem.netDelta >= 0) EmeraldLight else GoldCoinCream,
                                border = BorderStroke(1.dp, if (selectedItem.netDelta >= 0) EmeraldBorder else GoldCoinBorder)
                            ) {
                                Text(
                                    text = (if (selectedItem.netDelta >= 0) "+" else "-") + kotlin.math.abs(selectedItem.netDelta).toInrString(),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (selectedItem.netDelta >= 0) Emerald500 else GoldCoinAmber,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Canvas Chart Area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
            ) {
                val maxVal = remember(monthlyCashflow) {
                    val maxFlow = monthlyCashflow.maxOfOrNull { maxOf(it.inflow, it.outflow) } ?: 0.0
                    if (maxFlow > 0) maxFlow else 1000.0
                }

                Canvas(modifier = Modifier.fillMaxSize()) {
                    val width = size.width
                    val height = size.height
                    val bottomPadding = 20f
                    val topPadding = 15f
                    val chartHeight = height - bottomPadding - topPadding

                    // Draw 3 horizontal guideline dashes
                    val gridColor = Color(0xFFE2E8F0)
                    val dashEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)

                    for (step in 1..3) {
                        val y = topPadding + chartHeight * (step / 3f)
                        drawLine(
                            color = gridColor,
                            start = Offset(0f, y),
                            end = Offset(width, y),
                            strokeWidth = 1.5f,
                            pathEffect = dashEffect
                        )
                    }

                    if (monthlyCashflow.isNotEmpty()) {
                        val stepX = if (monthlyCashflow.size > 1) width / (monthlyCashflow.size - 1) else width

                        // Build Inflow Points and Spline
                        val inflowPoints = monthlyCashflow.mapIndexed { idx, item ->
                            val x = idx * stepX
                            val normalized = (item.inflow / maxVal).coerceIn(0.0, 1.0).toFloat()
                            val y = topPadding + chartHeight * (1f - normalized)
                            Offset(x, y)
                        }

                        // Build Outflow Points and Spline
                        val outflowPoints = monthlyCashflow.mapIndexed { idx, item ->
                            val x = idx * stepX
                            val normalized = (item.outflow / maxVal).coerceIn(0.0, 1.0).toFloat()
                            val y = topPadding + chartHeight * (1f - normalized)
                            Offset(x, y)
                        }

                        // Function to build smooth cubic spline
                        fun createSplinePath(points: List<Offset>): Path {
                            val path = Path()
                            if (points.isEmpty()) return path
                            path.moveTo(points.first().x, points.first().y)
                            for (i in 0 until points.size - 1) {
                                val p0 = points[i]
                                val p1 = points[i + 1]
                                val controlX1 = p0.x + (p1.x - p0.x) / 2f
                                val controlY1 = p0.y
                                val controlX2 = p0.x + (p1.x - p0.x) / 2f
                                val controlY2 = p1.y
                                path.cubicTo(controlX1, controlY1, controlX2, controlY2, p1.x, p1.y)
                            }
                            return path
                        }

                        val inflowPath = createSplinePath(inflowPoints)
                        val outflowPath = createSplinePath(outflowPoints)

                        // Inflow Gradient Fill Under Path
                        val inflowFillPath = Path().apply {
                            addPath(inflowPath)
                            lineTo(width, height - bottomPadding)
                            lineTo(0f, height - bottomPadding)
                            close()
                        }
                        drawPath(
                            path = inflowFillPath,
                            brush = Brush.verticalGradient(
                                colors = listOf(Emerald500.copy(alpha = 0.22f), Color.Transparent),
                                startY = topPadding,
                                endY = height - bottomPadding
                            )
                        )

                        // Outflow Gradient Fill Under Path
                        val outflowFillPath = Path().apply {
                            addPath(outflowPath)
                            lineTo(width, height - bottomPadding)
                            lineTo(0f, height - bottomPadding)
                            close()
                        }
                        drawPath(
                            path = outflowFillPath,
                            brush = Brush.verticalGradient(
                                colors = listOf(GoldCoinAmber.copy(alpha = 0.16f), Color.Transparent),
                                startY = topPadding,
                                endY = height - bottomPadding
                            )
                        )

                        // Draw Outflow Stroke
                        drawPath(
                            path = outflowPath,
                            color = GoldCoinAmber,
                            style = Stroke(width = 3.5f, cap = StrokeCap.Round)
                        )

                        // Draw Inflow Stroke
                        drawPath(
                            path = inflowPath,
                            color = Emerald500,
                            style = Stroke(width = 3.5f, cap = StrokeCap.Round)
                        )

                        // Draw selected month vertical cursor line
                        val selX = safeSelectedIdx * stepX
                        drawLine(
                            color = BrandRoyalBlue.copy(alpha = 0.4f),
                            start = Offset(selX, topPadding),
                            end = Offset(selX, height - bottomPadding),
                            strokeWidth = 2f,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f), 0f)
                        )

                        // Draw node dots
                        inflowPoints.forEachIndexed { idx, pt ->
                            val isSelected = idx == safeSelectedIdx
                            drawCircle(color = Color.White, radius = if (isSelected) 6f else 4.5f, center = pt)
                            drawCircle(color = Emerald500, radius = if (isSelected) 4f else 3f, center = pt)
                        }

                        outflowPoints.forEachIndexed { idx, pt ->
                            val isSelected = idx == safeSelectedIdx
                            drawCircle(color = Color.White, radius = if (isSelected) 6f else 4.5f, center = pt)
                            drawCircle(color = GoldCoinAmber, radius = if (isSelected) 4f else 3f, center = pt)
                        }
                    }
                }

                // If no active flow, overlay sleek indicator pill
                if (!hasActiveFlow) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.White.copy(alpha = 0.92f))
                            .border(1.dp, BrandIceBorder, RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "Baseline projection • Curves populate upon first loan activation",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextSlateMuted,
                            textAlign = TextAlign.Center,
                            maxLines = 1
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Month Pills Selector Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                monthlyCashflow.forEachIndexed { idx, item ->
                    val isSelected = idx == safeSelectedIdx
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isSelected) BrandRoyalBlue else CanvasPorcelain,
                        border = BorderStroke(1.dp, if (isSelected) BrandRoyalBlue else BrandIceBorder),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { selectedMonthIndex = idx }
                    ) {
                        Column(
                            modifier = Modifier.padding(vertical = 6.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = item.monthLabel.split(" ").firstOrNull() ?: item.monthLabel,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                color = if (isSelected) Color.White else TextNavyDark,
                                maxLines = 1
                            )
                            Text(
                                text = if (item.netDelta >= 0) "+${kotlin.math.abs(item.netDelta / 1000.0).formatDec(0)}k"
                                else "-${kotlin.math.abs(item.netDelta / 1000.0).formatDec(0)}k",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (isSelected) Color.White.copy(alpha = 0.85f)
                                else if (item.netDelta >= 0) Emerald500 else GoldCoinAmber,
                                maxLines = 1
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider(color = BrandIceBorder.copy(alpha = 0.6f))
            Spacer(modifier = Modifier.height(10.dp))

            // Legend Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Emerald500))
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = "Inward Liquidity (Collections)",
                            fontSize = 10.5.sp,
                            color = TextSlateMedium,
                            maxLines = 1
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(GoldCoinAmber))
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = "Debt Servicing (EMIs)",
                            fontSize = 10.5.sp,
                            color = TextSlateMedium,
                            maxLines = 1
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = if (netTotalDelta >= 0) EmeraldLight else GoldCoinCream
                ) {
                    Text(
                        text = if (netTotalDelta > 0) "Net Surplus" else if (netTotalDelta < 0) "Net Deficit" else "Balanced",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (netTotalDelta >= 0) Emerald500 else GoldCoinAmber,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
private fun MetricMiniCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    title: String,
    value: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = CanvasPorcelain,
        border = BorderStroke(1.dp, BrandIceBorder),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextSlateMuted
                )
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = value,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = TextNavyDark
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                fontSize = 10.sp,
                color = TextSlateMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun PortfolioCategoryDistributionSection(allLoans: List<LoanEntity>) {
    val totalAmount = allLoans.sumOf { it.disbursedAmount }.coerceAtLeast(1.0)
    val byType = remember(allLoans) {
        allLoans.groupBy { it.loanType.uppercase() }
            .mapValues { (_, loans) -> loans.sumOf { it.disbursedAmount } }
            .toList()
            .sortedByDescending { it.second }
    }

    val palette = listOf(
        BrandRoyalBlue,
        Emerald500,
        GoldCoinAmber,
        Color(0xFF8B5CF6),
        Color(0xFF06B6D4),
        Color(0xFFEC4899)
    )

    if (byType.isEmpty()) {
        Text("No loan disbursements recorded yet.", fontSize = 11.sp, color = TextSlateMuted)
        return
    }

    // Segmented horizontal bar
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(8.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(BrandIceBorder)
    ) {
        Row(modifier = Modifier.fillMaxSize()) {
            byType.forEachIndexed { index, pair ->
                val fraction = (pair.second / totalAmount).toFloat().coerceAtLeast(0.01f)
                Box(
                    modifier = Modifier
                        .weight(fraction)
                        .fillMaxHeight()
                        .background(palette[index % palette.size])
                )
            }
        }
    }

    Spacer(modifier = Modifier.height(8.dp))

    // Legend Chips
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        byType.forEachIndexed { index, pair ->
            val pct = ((pair.second / totalAmount) * 100).toInt()
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(palette[index % palette.size])
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "${pair.first.lowercase().replaceFirstChar { it.uppercase() }} $pct%",
                    fontSize = 10.5.sp,
                    color = TextSlateMedium
                )
            }
        }
    }
}

@Composable
private fun LoanInventoryCard(
    loan: LoanEntity,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = Color.White,
        border = BorderStroke(1.dp, BrandIceBorder),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Top Row: Role & Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    val isLenderRole = loan.lenderId.isNotBlank() && loan.disbursedAmount > 0
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = if (isLenderRole) EmeraldLight else GoldCoinCream
                    ) {
                        Text(
                            text = if (isLenderRole) "INVESTMENT" else "DEBT",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isLenderRole) Emerald500 else GoldCoinAmber,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = BrandIceBlue
                    ) {
                        Text(
                            text = loan.loanType.uppercase(),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = BrandRoyalBlue,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Text(
                    text = loan.status,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = when (loan.status) {
                        "ACTIVE", "ACTIVE_SERVICING" -> Emerald500
                        "DELINQUENT" -> Red500
                        "COMPLETED", "CLOSED" -> TextSlateMuted
                        else -> BrandCobalt
                    }
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Middle: Disbursed & Outstanding
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f, fill = false)) {
                    Text(
                        text = loan.purpose,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextNavyDark,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Disbursed: ${loan.disbursedAmount.toInrString()}",
                        fontSize = 11.5.sp,
                        color = TextSlateMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "${loan.interestRate}% p.a.",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextNavyDark,
                        maxLines = 1
                    )
                    Text(
                        text = "${loan.tenureMonths} Months (${loan.repaymentFrequency})",
                        fontSize = 10.5.sp,
                        color = TextSlateMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Progress Bar of repayment
            val repaidAmount = (loan.disbursedAmount - loan.outstandingAmount).coerceAtLeast(0.0)
            val progress = if (loan.disbursedAmount > 0) (repaidAmount / loan.disbursedAmount).toFloat().coerceIn(0f, 1f) else 1f

            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Repaid ${repaidAmount.toInrString()}",
                        fontSize = 10.sp,
                        color = TextSlateMuted
                    )
                    Text(
                        text = "Outstanding ${loan.outstandingAmount.toInrString()}",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (loan.outstandingAmount > 0) GoldCoinAmber else Emerald500
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(5.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = Emerald500,
                    trackColor = BrandIceBorder
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Bottom Action Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "ID: ...${loan.loanId.takeLast(8)}",
                    fontSize = 10.sp,
                    color = TextSlateMuted
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "View Details",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = BrandRoyalBlue
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = BrandRoyalBlue,
                        modifier = Modifier.size(13.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun CashflowLineItem(
    title: String,
    subtitle: String,
    amount: String,
    isPositive: Boolean,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = CanvasPorcelain,
        border = BorderStroke(1.dp, BrandIceBorder),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f, fill = false)
            ) {
                Icon(
                    imageVector = if (isPositive) Icons.AutoMirrored.Filled.TrendingUp else Icons.AutoMirrored.Filled.TrendingDown,
                    contentDescription = null,
                    tint = if (isPositive) Emerald500 else GoldCoinAmber,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = title,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextNavyDark,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = subtitle,
                        fontSize = 10.sp,
                        color = TextSlateMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = amount,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = if (isPositive) Emerald500 else GoldCoinAmber,
                maxLines = 1
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// COMPONENT: EXECUTIVE STATEMENT BOTTOM SHEET
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PortfolioStatementSheet(
    totalLent: Double,
    outstandingLent: Double,
    totalBorrowed: Double,
    outstandingBorrowed: Double,
    netPosition: Double,
    activeLoansCount: Int,
    weightedApr: Double,
    npaRatio: Double,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val currentDate = remember {
        SimpleDateFormat("dd MMM yyyy, HH:mm 'IST'", Locale.getDefault()).format(Date())
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Portfolio Statement",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextNavyDark
                    )
                    Text(
                        text = "Generated $currentDate",
                        fontSize = 11.sp,
                        color = TextSlateMuted
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSlateMedium)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = BrandIceBorder)
            Spacer(modifier = Modifier.height(16.dp))

            // Ledger Statement Rows
            StatementItemRow("Gross Capital Deployed (Lent)", totalLent.toInrString())
            StatementItemRow("Outstanding Capital Lent", outstandingLent.toInrString())
            StatementItemRow("Gross Capital Borrowed (Debts)", totalBorrowed.toInrString())
            StatementItemRow("Outstanding Debt Obligations", outstandingBorrowed.toInrString())
            StatementItemRow("Net Capital Position", (if (netPosition >= 0) "+" else "-") + kotlin.math.abs(netPosition).toInrString(), isBold = true)
            StatementItemRow("Active Loan Contracts", "$activeLoansCount Active")
            StatementItemRow("Weighted Portfolio Yield", String.format(Locale.getDefault(), "%.2f%% p.a.", weightedApr))
            StatementItemRow("Non-Performing Assets (NPA)", String.format(Locale.getDefault(), "%.1f%%", npaRatio))

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = {
                    Toast.makeText(context, "Portfolio Statement downloaded to device storage", Toast.LENGTH_SHORT).show()
                    onDismiss()
                },
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BrandRoyalBlue),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Outlined.FileDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Export PDF Statement", fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Composable
private fun StatementItemRow(
    label: String,
    value: String,
    isBold: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            color = if (isBold) TextNavyDark else TextSlateMedium,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal
        )
        Text(
            text = value,
            fontSize = 12.5.sp,
            fontWeight = if (isBold) FontWeight.ExtraBold else FontWeight.SemiBold,
            color = TextNavyDark
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// CASHFLOW CALCULATION ENGINE
// ─────────────────────────────────────────────────────────────────────────────

data class MonthlyCashflowItem(
    val monthLabel: String,
    val inflow: Double,
    val outflow: Double,
    val netDelta: Double = inflow - outflow
)

private fun computeForwardCashFlow(
    activeLent: List<LoanEntity>,
    activeBorrowed: List<LoanEntity>,
    monthsAhead: Int = 4
): List<MonthlyCashflowItem> {
    val calendar = Calendar.getInstance()
    val monthFormat = SimpleDateFormat("MMM yyyy", Locale.getDefault())

    val totalLenderEmi = activeLent.sumOf { loan ->
        calculateMonthlyEmi(loan.outstandingAmount, loan.interestRate, loan.tenureMonths)
    }

    val totalBorrowerEmi = activeBorrowed.sumOf { loan ->
        calculateMonthlyEmi(loan.outstandingAmount, loan.interestRate, loan.tenureMonths)
    }

    val list = mutableListOf<MonthlyCashflowItem>()
    for (i in 0 until monthsAhead) {
        val label = monthFormat.format(calendar.time).uppercase()
        list.add(
            MonthlyCashflowItem(
                monthLabel = label,
                inflow = totalLenderEmi,
                outflow = totalBorrowerEmi
            )
        )
        calendar.add(Calendar.MONTH, 1)
    }
    return list
}

private fun calculateMonthlyEmi(principal: Double, annualRate: Double, tenureMonths: Int): Double {
    if (principal <= 0.0 || tenureMonths <= 0) return 0.0
    if (annualRate <= 0.0) return principal / tenureMonths
    val monthlyRate = annualRate / 12.0 / 100.0
    val factor = Math.pow(1.0 + monthlyRate, tenureMonths.toDouble().coerceAtLeast(1.0))
    return if (factor > 1.0) (principal * monthlyRate * factor) / (factor - 1.0) else principal / tenureMonths
}

private fun Double.formatDec(digits: Int): String {
    return String.format(Locale.getDefault(), "%.${digits}f", this)
}
