package com.loanzo.app.ui.loan

import androidx.compose.ui.res.stringResource
import com.loanzo.app.R

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.loanzo.app.ui.components.*
import com.loanzo.app.ui.theme.*
import com.loanzo.app.util.toDateString
import com.loanzo.app.util.toInrString
import com.loanzo.app.util.toRelativeTime
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CreateLoanScreen(
    onCreateLoan: (lenderId: String, amount: Double, purpose: String, loanType: String,
                   interestRate: Double, interestModel: String, tenureMonths: Int,
                   repaymentFrequency: String, notes: String) -> Unit,
    onBack: () -> Unit,
    loanCreated: Boolean = false
) {
    var lenderEmail by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var purpose by remember { mutableStateOf("") }
    var selectedLoanType by remember { mutableStateOf("PERSONAL") }
    var interestRate by remember { mutableStateOf("") }
    var selectedInterestModel by remember { mutableStateOf("SIMPLE") }
    var tenure by remember { mutableStateOf("") }
    var selectedFrequency by remember { mutableStateOf("MONTHLY") }
    var notes by remember { mutableStateOf("") }

    val loanTypes = listOf("PERSONAL", "BUSINESS", "EDUCATION", "MEDICAL", "AGRICULTURE", "OTHER")
    val interestModels = listOf("SIMPLE", "COMPOUND", "FLAT", "NONE")
    val frequencies = listOf("MONTHLY", "WEEKLY", "BI_WEEKLY")

    LaunchedEffect(loanCreated) {
        if (loanCreated) onBack()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.create_loan), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Lender
            OutlinedTextField(
                value = lenderEmail,
                onValueChange = { lenderEmail = it },
                label = { Text(stringResource(R.string.lender_email_id)) },
                leadingIcon = { Icon(Icons.Default.PersonSearch, null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp)
            )

            // Amount
            OutlinedTextField(
                value = amount,
                onValueChange = { amount = it.filter { c -> c.isDigit() || c == '.' } },
                label = { Text(stringResource(R.string.loan_amount_1)) },
                leadingIcon = { Icon(Icons.Default.CurrencyRupee, null) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp)
            )

            // Purpose
            OutlinedTextField(
                value = purpose,
                onValueChange = { purpose = it },
                label = { Text(stringResource(R.string.loan_purpose)) },
                leadingIcon = { Icon(Icons.Default.Description, null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp)
            )

            // Loan Type chips
            Text(stringResource(R.string.loan_type), style = MaterialTheme.typography.labelLarge)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                loanTypes.forEach { type ->
                    FilterChip(
                        selected = selectedLoanType == type,
                        onClick = { selectedLoanType = type },
                        label = { Text(type.replace("_", " ")) },
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            }

            // Interest rate
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = interestRate,
                    onValueChange = { interestRate = it.filter { c -> c.isDigit() || c == '.' } },
                    label = { Text(stringResource(R.string.interest_rate)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp)
                )
                OutlinedTextField(
                    value = tenure,
                    onValueChange = { tenure = it.filter { c -> c.isDigit() } },
                    label = { Text(stringResource(R.string.tenure_months)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp)
                )
            }

            // Interest model
            Text(stringResource(R.string.interest_model), style = MaterialTheme.typography.labelLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                interestModels.forEach { model ->
                    FilterChip(
                        selected = selectedInterestModel == model,
                        onClick = { selectedInterestModel = model },
                        label = { Text(model) },
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            }

            // Repayment frequency
            Text(stringResource(R.string.repayment_frequency), style = MaterialTheme.typography.labelLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                frequencies.forEach { freq ->
                    FilterChip(
                        selected = selectedFrequency == freq,
                        onClick = { selectedFrequency = freq },
                        label = { Text(freq.replace("_", " ")) },
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            }

            // Notes
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text(stringResource(R.string.notes_optional)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp),
                shape = RoundedCornerShape(14.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Create button
            Button(
                onClick = {
                    onCreateLoan(
                        lenderEmail,
                        amount.toDoubleOrNull() ?: 0.0,
                        purpose,
                        selectedLoanType,
                        interestRate.toDoubleOrNull() ?: 0.0,
                        selectedInterestModel,
                        tenure.toIntOrNull() ?: 12,
                        selectedFrequency,
                        notes
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Gold500,
                    contentColor = Navy900
                ),
                enabled = amount.isNotBlank() && purpose.isNotBlank() && lenderEmail.isNotBlank()
            ) {
                Icon(Icons.Default.Add, null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.create_loan), fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoanDetailScreen(
    state: LoanUiState,
    onBack: () -> Unit,
    onRequestTranche: () -> Unit,
    onMakeRepayment: () -> Unit,
    onAddPledge: () -> Unit,
    onViewAuditTrail: () -> Unit,
    onSignAgreement: (String) -> Unit,
    onNavigateToChat: () -> Unit,
    onNavigateToDocument: () -> Unit
) {
    val loan = state.selectedLoan
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var showAllSchedule by remember { mutableStateOf(false) }
    var showAllDisbursements by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.loan_dashboard), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        if (loan == null) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // â”€â”€ 1. NEXT PAYMENT DUE CARD â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
            val nextDue = state.nextDueRepayment
            if (nextDue != null) {
                item {
                    val isOverdue = state.daysUntilNextDue < 0
                    val gradientColors = if (isOverdue) listOf(Red500, Red400) else listOf(Navy600, Navy700)
                    GradientCard(gradientColors = gradientColors) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    if (isOverdue) "âš ï¸ Payment Overdue" else "ðŸ“… Next Payment Due",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (isOverdue) Color.White.copy(alpha = 0.8f) else Gray300
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    nextDue.amount.toInrString(),
                                    style = MaterialTheme.typography.displaySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    if (isOverdue) "Overdue by ${-state.daysUntilNextDue} days â€¢ Due ${nextDue.dueDate.toDateString()}"
                                    else "Due in ${state.daysUntilNextDue} days â€¢ ${nextDue.dueDate.toDateString()}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.7f)
                                )
                            }
                            Button(
                                onClick = onMakeRepayment,
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isOverdue) Color.White else Gold500,
                                    contentColor = if (isOverdue) Red500 else Navy900
                                )
                            ) {
                                Icon(Icons.Default.Payment, null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(stringResource(R.string.pay_now), fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // â”€â”€ 2. LOAN OVERVIEW CARD â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
            item {
                GradientCard(gradientColors = listOf(Navy600, Navy700)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column {
                            Text(loan.purpose, style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold, color = Color.White)
                            Spacer(modifier = Modifier.height(4.dp))
                            StatusBadge(text = loan.status, color = when (loan.status) {
                                "ACTIVE" -> Emerald400
                                "CLOSED" -> Gray400
                                "DEFAULTED" -> Red400
                                else -> Gold500
                            })
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(stringResource(R.string.sanctioned), style = MaterialTheme.typography.labelSmall, color = Gray400)
                            Text(loan.sanctionedAmount.toInrString(), style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text(stringResource(R.string.disbursed), style = MaterialTheme.typography.labelSmall, color = Gray400)
                            Text(state.totalDisbursed.toInrString(), style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold, color = Gold400)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(stringResource(R.string.outstanding), style = MaterialTheme.typography.labelSmall, color = Gray400)
                            Text(loan.outstandingAmount.toInrString(), style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold, color = if (loan.outstandingAmount > 0) Orange400 else Emerald400)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(stringResource(R.string.repaid), style = MaterialTheme.typography.labelSmall, color = Gray400)
                            Text(state.totalPaid.toInrString(), style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold, color = Emerald400)
                        }
                    }
                }
            }

            // Utilization ring
            if (state.totalDisbursed > 0) {
                item {
                    GlassCard {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(stringResource(R.string.verified_utilization), style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("Verified: ${state.totalVerified.toInrString()}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("Total Disbursed: ${state.totalDisbursed.toInrString()}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            UtilizationRing(percentage = state.utilizationPercentage, size = 100)
                        }
                    }
                }
            }

            // â”€â”€ 3. INTERACTIVE CHARTS â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
            if (state.balanceHistory.size >= 2 || state.totalPaid > 0) {
                item {
                    SectionHeader(title = "ðŸ“Š Insights")
                }

                // Balance Over Time chart
                if (state.balanceHistory.size >= 2) {
                    item {
                        GlassCard {
                            Text(stringResource(R.string.balance_over_time), style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold)
                            Text(stringResource(R.string.outstanding_balance_trend), style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(16.dp))
                            BalanceLineChart(
                                balanceHistory = state.balanceHistory,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                // Payment Breakdown donut
                if (state.totalPrincipalPaid > 0 || state.totalInterestPaid > 0) {
                    item {
                        GlassCard {
                            Text(stringResource(R.string.payment_breakdown), style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold)
                            Text(stringResource(R.string.principal_vs_interest_split), style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(12.dp))
                            PaymentDonutChart(
                                principalPaid = state.totalPrincipalPaid,
                                interestPaid = state.totalInterestPaid,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }

            // â”€â”€ 4. REPAYMENT SCHEDULE / AMORTIZATION TIMELINE â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
            if (state.amortizationSchedule.isNotEmpty()) {
                item {
                    SectionHeader(title = "ðŸ“‹ Repayment Schedule")
                }
                val visibleSchedule = if (showAllSchedule) state.amortizationSchedule
                    else state.amortizationSchedule.take(4)

                items(visibleSchedule) { schedule ->
                    ScheduleTimelineItem(schedule)
                }

                if (state.amortizationSchedule.size > 4) {
                    item {
                        TextButton(
                            onClick = { showAllSchedule = !showAllSchedule },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                if (showAllSchedule) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                null, modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                if (showAllSchedule) "Show Less" else "Show All ${state.amortizationSchedule.size} Installments",
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            // â”€â”€ 5. TRANCHES & DISBURSEMENT HISTORY â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
            if (state.disbursements.isNotEmpty()) {
                item {
                    SectionHeader(title = "ðŸ’¸ Disbursements")
                }
                val visibleDisb = if (showAllDisbursements) state.disbursements
                    else state.disbursements.take(3)

                items(visibleDisb) { disb ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (disb.approvalStatus == "PENDING")
                                Orange400.copy(alpha = 0.06f)
                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                        )
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(disb.payeeName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                                    Text(disb.purpose, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(
                                        disb.timestamp.toRelativeTime(),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                    )
                                }
                                Text(disb.amount.toInrString(), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                StatusBadge(disb.approvalStatus, color = when (disb.approvalStatus) {
                                    "APPROVED" -> Emerald400; "REJECTED" -> Red400; else -> Orange400
                                })
                                StatusBadge(disb.ruleEngineResult, color = when (disb.ruleEngineResult) {
                                    "CONSISTENT", "AUTO_APPROVED" -> Emerald400
                                    "MISMATCH" -> Orange400; "BLOCKED" -> Red400; else -> Gray400
                                })
                            }
                        }
                    }
                }

                if (state.disbursements.size > 3) {
                    item {
                        TextButton(
                            onClick = { showAllDisbursements = !showAllDisbursements },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                if (showAllDisbursements) "Show Less" else "View All ${state.disbursements.size} Disbursements",
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            // â”€â”€ 6. REPAYMENT HISTORY â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
            if (state.repayments.isNotEmpty()) {
                item { SectionHeader(title = "ðŸ’° Repayments") }
                items(state.repayments) { repayment ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp).fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = CircleShape,
                                    color = when (repayment.status) {
                                        "PAID" -> Emerald400; "OVERDUE" -> Red400; else -> Gold500
                                    }.copy(alpha = 0.15f),
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        when (repayment.status) {
                                            "PAID" -> Icons.Default.CheckCircle
                                            "OVERDUE" -> Icons.Default.Warning
                                            else -> Icons.Default.Schedule
                                        },
                                        null,
                                        modifier = Modifier.padding(8.dp),
                                        tint = when (repayment.status) {
                                            "PAID" -> Emerald400; "OVERDUE" -> Red400; else -> Gold500
                                        }
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(repayment.amount.toInrString(), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                                        if (repayment.penalty > 0.0) {
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("+ ${repayment.penalty.toInrString()} penalty", style = MaterialTheme.typography.labelSmall, color = Red400)
                                        }
                                    }
                                    Text(
                                        if (repayment.paidDate != null) "Paid ${repayment.paidDate.toDateString()}"
                                        else "Due ${repayment.dueDate.toDateString()}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            StatusBadge(repayment.status, color = when (repayment.status) {
                                "PAID" -> Emerald400; "OVERDUE" -> Red400; else -> Gold500
                            })
                        }
                    }
                }
            }

            // â”€â”€ 7. COLLATERAL & PLEDGES OVERVIEW â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
            item {
                SectionHeader(title = "ðŸ›¡ï¸ Collateral & Pledges")
            }

            if (state.pledges.isEmpty()) {
                item {
                    GlassCard {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(stringResource(R.string.no_collateral_pledged), style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(stringResource(R.string.adding_pledges_can_improve_loan_terms), style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                            }
                            OutlinedButton(
                                onClick = onAddPledge,
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(stringResource(R.string.add))
                            }
                        }
                    }
                }
            } else {
                // LTV Health Summary
                item {
                    GlassCard {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(stringResource(R.string.loan_to_value_ltv), style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Pledge: ${state.totalPledgeValue.toInrString()} vs Outstanding: ${loan.outstandingAmount.toInrString()}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            // LTV Badge
                            val ltvColor = when {
                                state.ltvRatio < 60f -> Emerald400
                                state.ltvRatio < 80f -> Orange400
                                else -> Red400
                            }
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = ltvColor.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    "${state.ltvRatio.toInt()}%",
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = ltvColor
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        // LTV progress bar
                        val ltvColor = when {
                            state.ltvRatio < 60f -> Emerald400
                            state.ltvRatio < 80f -> Orange400
                            else -> Red400
                        }
                        val animatedLtv by animateFloatAsState(
                            targetValue = (state.ltvRatio / 100f).coerceIn(0f, 1f),
                            animationSpec = tween(1000, easing = FastOutSlowInEasing),
                            label = "ltv_bar"
                        )
                        LinearProgressIndicator(
                            progress = { animatedLtv },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = ltvColor,
                            trackColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(stringResource(R.string.str_0), style = MaterialTheme.typography.labelSmall, color = Gray400)
                            Text(stringResource(R.string.safe), style = MaterialTheme.typography.labelSmall, color = Emerald400)
                            Text(stringResource(R.string.risk), style = MaterialTheme.typography.labelSmall, color = Orange400)
                            Text(stringResource(R.string.str_100), style = MaterialTheme.typography.labelSmall, color = Red400)
                        }
                    }
                }

                // Individual pledge items
                items(state.pledges) { pledge ->
                    GlassCard {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = CircleShape,
                                    color = when (pledge.assetType) {
                                        "GOLD" -> Gold500; "VEHICLE" -> Blue400
                                        "PROPERTY" -> Emerald400; else -> Gray400
                                    }.copy(alpha = 0.15f),
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Icon(
                                        when (pledge.assetType) {
                                            "GOLD" -> Icons.Default.Diamond
                                            "VEHICLE" -> Icons.Default.DirectionsCar
                                            "PROPERTY" -> Icons.Default.Home
                                            "EQUIPMENT" -> Icons.Default.Build
                                            else -> Icons.Default.Security
                                        },
                                        null,
                                        modifier = Modifier.padding(8.dp),
                                        tint = when (pledge.assetType) {
                                            "GOLD" -> Gold500; "VEHICLE" -> Blue400
                                            "PROPERTY" -> Emerald400; else -> Gray400
                                        }
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(pledge.assetDescription, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                                    Text("${pledge.assetType} â€¢ ${pledge.receiptStatus}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            Text(pledge.estimatedValue.toInrString(), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // ——— 8. DOCUMENT VAULT ————————————————————————————————————————————
            item {
                SectionHeader(title = "📄 Documents")
            }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    DocumentVaultItem(
                        icon = Icons.Default.Description,
                        title = if (loan.isAgreementSigned) "Digital Loan Agreement (Signed)" else "Sign Loan Agreement",
                        subtitle = if (loan.isAgreementSigned) "Signed digitally" else "Action Required",
                        accentColor = if (loan.isAgreementSigned) Emerald400 else Blue400,
                        onClick = {
                            if (loan.isAgreementSigned) {
                                scope.launch { snackbarHostState.showSnackbar("Opening signed document...") }
                            } else {
                                onSignAgreement(loan.loanId)
                            }
                        }
                    )
                    DocumentVaultItem(
                        icon = Icons.Default.Assignment,
                        title = "Sanction Letter",
                        subtitle = "â‚¹${loan.sanctionedAmount.toInrString()} sanctioned",
                        accentColor = Emerald400,
                        onClick = {
                            scope.launch { snackbarHostState.showSnackbar("Document generation coming soon") }
                        }
                    )
                    DocumentVaultItem(
                        icon = Icons.Default.Gavel,
                        title = "Terms & Conditions",
                        subtitle = "${loan.interestRate}% ${loan.interestModel} â€¢ ${loan.tenureMonths} months",
                        accentColor = Gold500,
                        onClick = onNavigateToDocument
                    )
                }
            }

            // â”€â”€ 9. LOAN TERMS â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
            item {
                GlassCard {
                    Text(stringResource(R.string.loan_terms), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(12.dp))
                    LoanTermRow("Type", loan.loanType)
                    LoanTermRow("Interest", "${loan.interestRate}% (${loan.interestModel})")
                    LoanTermRow("Tenure", "${loan.tenureMonths} months")
                    LoanTermRow("Repayment", loan.repaymentFrequency)
                    LoanTermRow("Created", loan.createdAt.toDateString())
                    if (loan.notes.isNotBlank()) {
                        LoanTermRow("Notes", loan.notes)
                    }
                }
            }

            // â”€â”€ 10. ACTION BUTTONS â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = onRequestTranche,
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Gold500, contentColor = Navy900),
                        enabled = loan.status == "ACTIVE"
                    ) {
                        Icon(Icons.Default.Send, null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(stringResource(R.string.tranche), fontWeight = FontWeight.Bold)
                    }
                    OutlinedButton(
                        onClick = onMakeRepayment,
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        enabled = loan.status == "ACTIVE"
                    ) {
                        Icon(Icons.Default.Payment, null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(stringResource(R.string.repay))
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onAddPledge,
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Security, null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(stringResource(R.string.pledge))
                    }
                    OutlinedButton(
                        onClick = onViewAuditTrail,
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.History, null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(stringResource(R.string.audit))
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onNavigateToChat,
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Chat, null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Chat with Lender")
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }
}

// â”€â”€ HELPER COMPOSABLES â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

/** Single timeline item in the amortization schedule */
@Composable
private fun ScheduleTimelineItem(schedule: ScheduleItem) {
    val statusColor = when (schedule.status) {
        "PAID" -> Emerald400
        "UPCOMING" -> Gold500
        "OVERDUE" -> Red400
        else -> Gray400
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (schedule.status == "UPCOMING")
                Gold500.copy(alpha = 0.06f)
            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        )
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Status icon
            Surface(
                shape = CircleShape,
                color = statusColor.copy(alpha = 0.15f),
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    when (schedule.status) {
                        "PAID" -> Icons.Default.CheckCircle
                        "UPCOMING" -> Icons.Default.Upcoming
                        "OVERDUE" -> Icons.Default.Warning
                        else -> Icons.Default.Schedule
                    },
                    null,
                    modifier = Modifier.padding(8.dp),
                    tint = statusColor
                )
            }
            Spacer(modifier = Modifier.width(12.dp))

            // Details
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "EMI #${schedule.installmentNumber}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    StatusBadge(schedule.status, color = statusColor)
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    schedule.dueDate.toDateString(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        schedule.amount.toInrString(),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "P: ${"%.0f".format(schedule.principal)} â€¢ I: ${"%.0f".format(schedule.interest)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

/** Clickable document item for the Document Vault */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DocumentVaultItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    accentColor: Color,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left accent bar
            Surface(
                shape = RoundedCornerShape(4.dp),
                color = accentColor,
                modifier = Modifier
                    .width(4.dp)
                    .height(40.dp)
            ) {}
            Spacer(modifier = Modifier.width(12.dp))

            Surface(
                shape = CircleShape,
                color = accentColor.copy(alpha = 0.12f),
                modifier = Modifier.size(40.dp)
            ) {
                Icon(icon, null, modifier = Modifier.padding(8.dp), tint = accentColor)
            }
            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text(subtitle, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            Icon(
                Icons.Default.ChevronRight, null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
        }
    }
}

@Composable
private fun LoanTermRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
    }
}
