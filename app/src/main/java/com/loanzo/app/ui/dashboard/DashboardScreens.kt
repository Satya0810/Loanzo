package com.loanzo.app.ui.dashboard

import androidx.compose.ui.res.stringResource
import com.loanzo.app.R

import androidx.compose.animation.*
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
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.loanzo.app.ui.components.*
import com.loanzo.app.ui.theme.*
import com.loanzo.app.util.toDateString
import com.loanzo.app.util.toInrString
import com.loanzo.app.util.toRelativeTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BorrowerDashboardScreen(
    state: DashboardUiState,
    onNavigateToCreateLoan: () -> Unit,
    onNavigateToCalculator: () -> Unit,
    onNavigateToLoanDetail: (String) -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToFinancialHealth: () -> Unit
) {
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
                        Column {
                            Text(
                                text = "Hello, ${state.user?.name?.split(" ")?.firstOrNull() ?: "User"} 👋",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = stringResource(R.string.borrower_dashboard),
                                style = MaterialTheme.typography.bodyMedium,
                                color = Gray400
                            )
                        }
                        IconButton(onClick = onNavigateToProfile) {
                            Surface(
                                shape = CircleShape,
                                color = Gold500.copy(alpha = 0.2f),
                                modifier = Modifier.size(44.dp)
                            ) {
                                Icon(
                                    Icons.Default.Person,
                                    null,
                                    tint = Gold500,
                                    modifier = Modifier.padding(10.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Summary cards
        item {
            GradientCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                gradientColors = listOf(Navy600, Navy700)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(stringResource(R.string.total_outstanding), style = MaterialTheme.typography.labelMedium, color = Gray300)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            state.totalOutstanding.toInrString(),
                            style = MaterialTheme.typography.displaySmall,
                            fontWeight = FontWeight.Bold,
                            color = if (state.totalOutstanding > 0) Gold400 else Emerald400
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(stringResource(R.string.total_disbursed), style = MaterialTheme.typography.labelSmall, color = Gray400)
                        Text(
                            state.totalDisbursed.toInrString(),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color.White.copy(alpha = 0.1f)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    "${state.loans.count { it.status == "ACTIVE" }}",
                                    style = MaterialTheme.typography.headlineLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    stringResource(R.string.active_loans),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Gray300
                                )
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
        }

        // Quick stats row
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                GlassCard(modifier = Modifier.weight(1f)) {
                    StatItem(
                        label = "Overdue",
                        value = "${state.overdueRepayments.size}",
                        icon = Icons.Default.Warning,
                        iconTint = if (state.overdueRepayments.isNotEmpty()) Orange400 else Emerald400
                    )
                }
                GlassCard(modifier = Modifier.weight(1f)) {
                    StatItem(
                        label = "Total Loans",
                        value = "${state.loans.size}",
                        icon = Icons.Default.Receipt,
                        iconTint = Blue400
                    )
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
        }

        // Create loan button
        item {
            Button(
                onClick = onNavigateToCreateLoan,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Gold500,
                    contentColor = Navy900
                )
            ) {
                Icon(Icons.Default.Add, null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.create_new_loan), fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
            Spacer(modifier = Modifier.height(12.dp))
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
            Spacer(modifier = Modifier.height(20.dp))
        }

        // Active loans
        item {
            SectionHeader(
                title = "Your Loans",
                modifier = Modifier.padding(horizontal = 20.dp)
            )
        }

        if (state.loans.isEmpty() && !state.isLoading) {
            item {
                EmptyState(
                    icon = Icons.Default.AccountBalanceWallet,
                    title = "No loans yet",
                    subtitle = "Create your first loan to get started"
                )
            }
        }

        items(state.loans) { loan ->
            LoanSummaryCard(
                loanId = loan.loanId,
                purpose = loan.purpose,
                amount = loan.sanctionedAmount,
                outstanding = loan.outstandingAmount,
                status = loan.status,
                counterpartyName = "Lender",
                date = loan.createdAt.toDateString(),
                onClick = { onNavigateToLoanDetail(loan.loanId) },
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
            )
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LenderDashboardScreen(
    state: DashboardUiState,
    onNavigateToLoanDetail: (String) -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToApproval: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 100.dp)
    ) {
        // Header
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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Hello, ${state.user?.name?.split(" ")?.firstOrNull() ?: "User"} 👋",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            stringResource(R.string.lender_dashboard),
                            style = MaterialTheme.typography.bodyMedium,
                            color = Gray400
                        )
                    }
                    IconButton(onClick = onNavigateToProfile) {
                        Surface(
                            shape = CircleShape,
                            color = Gold500.copy(alpha = 0.2f),
                            modifier = Modifier.size(44.dp)
                        ) {
                            Icon(Icons.Default.Person, null, tint = Gold500, modifier = Modifier.padding(10.dp))
                        }
                    }
                }
            }
        }

        // Portfolio summary
        item {
            GradientCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                gradientColors = listOf(Navy600, Navy700)
            ) {
                Text(stringResource(R.string.portfolio_overview), style = MaterialTheme.typography.labelMedium, color = Gray300)
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(stringResource(R.string.total_lent), style = MaterialTheme.typography.labelSmall, color = Gray400)
                        Text(
                            state.totalDisbursed.toInrString(),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(stringResource(R.string.outstanding), style = MaterialTheme.typography.labelSmall, color = Gray400)
                        Text(
                            state.totalOutstanding.toInrString(),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = Gold400
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "${state.loans.size}",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(stringResource(R.string.total), style = MaterialTheme.typography.labelSmall, color = Gray400)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "${state.loans.count { it.status == "ACTIVE" }}",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Emerald400
                        )
                        Text(stringResource(R.string.active), style = MaterialTheme.typography.labelSmall, color = Gray400)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "${state.pendingApprovals.size}",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Orange400
                        )
                        Text(stringResource(R.string.pending), style = MaterialTheme.typography.labelSmall, color = Gray400)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "${state.overdueRepayments.size}",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = if (state.overdueRepayments.isNotEmpty()) Red400 else Gray400
                        )
                        Text(stringResource(R.string.overdue), style = MaterialTheme.typography.labelSmall, color = Gray400)
                    }
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
        }

        // Pending approvals section
        if (state.pendingApprovals.isNotEmpty()) {
            item {
                SectionHeader(
                    title = "⚡ Pending Approvals",
                    actionText = "View All",
                    modifier = Modifier.padding(horizontal = 20.dp)
                )
            }
            items(state.pendingApprovals.take(3)) { disbursement ->
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
            item { Spacer(modifier = Modifier.height(16.dp)) }
        }

        // Loans list
        item {
            SectionHeader(
                title = "Loan Portfolio",
                modifier = Modifier.padding(horizontal = 20.dp)
            )
        }

        if (state.loans.isEmpty() && !state.isLoading) {
            item {
                EmptyState(
                    icon = Icons.Default.Folder,
                    title = "No loans in your portfolio",
                    subtitle = "Loans will appear here when borrowers create them"
                )
            }
        }

        items(state.loans) { loan ->
            LoanSummaryCard(
                loanId = loan.loanId,
                purpose = loan.purpose,
                amount = loan.sanctionedAmount,
                outstanding = loan.outstandingAmount,
                status = loan.status,
                counterpartyName = "Borrower",
                date = loan.createdAt.toDateString(),
                onClick = { onNavigateToLoanDetail(loan.loanId) },
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
            )
        }

        item { Spacer(modifier = Modifier.height(24.dp)) }
    }
}
