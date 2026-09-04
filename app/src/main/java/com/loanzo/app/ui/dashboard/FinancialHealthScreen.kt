package com.loanzo.app.ui.dashboard

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.loanzo.app.data.dao.CategoryBreakdown
import com.loanzo.app.data.dao.DisbursementDao
import com.loanzo.app.data.dao.MonthlySpending
import com.loanzo.app.data.dao.RepaymentDao
import com.loanzo.app.data.repository.LoanRepository
import com.loanzo.app.data.repository.UserRepository
import com.loanzo.app.domain.PenaltyEngine
import com.loanzo.app.ui.components.GlassCard
import com.loanzo.app.ui.theme.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class FinancialHealthState(
    val categories: List<CategoryBreakdown> = emptyList(),
    val monthlyTrend: List<MonthlySpending> = emptyList(),
    val healthScore: Int = 0, // 0-100
    val onTimePayments: Int = 0,
    val totalPayments: Int = 0,
    val utilizationRatio: Float = 0f,
    val diversityScore: Int = 0,
    val punctualityScore: Int = 0,
    val utilizationScore: Int = 0,
    val ageScore: Int = 0,
    val oldestLoanDays: Int = 0,
    val totalOutstanding: Double = 0.0,
    val totalSanctioned: Double = 0.0,
    val totalPenaltyAccrued: Double = 0.0,
    val overdueCount: Int = 0,
    val insights: List<FinancialInsight> = emptyList(),
    val isLoading: Boolean = true
)

data class FinancialInsight(
    val icon: ImageVector,
    val title: String,
    val description: String,
    val color: Color,
    val severity: String // "GOOD", "WARNING", "CRITICAL"
)

@HiltViewModel
class FinancialHealthViewModel @Inject constructor(
    private val loanRepository: LoanRepository,
    private val disbursementDao: DisbursementDao,
    private val repaymentDao: RepaymentDao,
    private val userRepository: UserRepository,
    private val penaltyEngine: PenaltyEngine
) : ViewModel() {
    private val _state = MutableStateFlow(FinancialHealthState())
    val state: StateFlow<FinancialHealthState> = _state.asStateFlow()

    init { loadData() }

    private fun loadData() {
        viewModelScope.launch {
            val userId = userRepository.getCurrentUserIdSync() ?: return@launch

            // Collect all data streams
            combine(
                disbursementDao.getCategoryBreakdownForUser(userId),
                disbursementDao.getMonthlySpendingTrend(userId),
                loanRepository.getAllLoansForUser(userId),
                repaymentDao.getAllRepaymentsForBorrower(userId)
            ) { categories, monthlyTrend, loans, repayments ->
                // 1. Punctuality Score (40 pts) — % of on-time payments
                val paidRepayments = repayments.filter { it.status == "PAID" }
                val overdueRepayments = repayments.filter { it.status == "OVERDUE" }
                val onTimePayments = paidRepayments.count { r ->
                    val actualPaid = r.paidDate ?: r.timestamp
                    actualPaid <= r.dueDate + (86_400_000L * 3) // Within 3-day grace
                }
                val totalMaturedRepayments = paidRepayments.size + overdueRepayments.size
                val punctualityScore = if (totalMaturedRepayments == 0) {
                    if (loans.isEmpty()) 25 else 35 // Baseline for new users
                } else {
                    ((onTimePayments.toFloat() / totalMaturedRepayments) * 40).toInt()
                }

                // 2. Utilization Score (30 pts) — lower outstanding/sanctioned = better
                val totalSanctioned = loans.sumOf { it.sanctionedAmount }.coerceAtLeast(1.0)
                val totalOutstanding = loans.sumOf { it.outstandingAmount }
                val utilRatio = (totalOutstanding / totalSanctioned).toFloat().coerceIn(0f, 1f)
                val utilizationScore = ((1f - utilRatio) * 30).toInt()

                // 3. Diversity Score (15 pts)
                val diversityScore = (categories.size.coerceAtMost(5) * 3) // 3 pts per category, max 15

                // 4. Loan Age Score (15 pts) — longer credit history is better
                val now = System.currentTimeMillis()
                val oldestLoanMs = loans.minOfOrNull { it.createdAt } ?: now
                val daysSinceOldest = ((now - oldestLoanMs) / (1000 * 60 * 60 * 24)).toInt()
                val ageScore = when {
                    daysSinceOldest > 365 -> 15
                    daysSinceOldest > 180 -> 12
                    daysSinceOldest > 90 -> 9
                    daysSinceOldest > 30 -> 6
                    else -> 3
                }

                val healthScore = (punctualityScore + utilizationScore + diversityScore + ageScore).coerceIn(0, 100)

                // Calculate total penalty accrued
                var totalPenalty = 0.0
                loans.forEach { loan ->
                    val loanRepayments = repayments.filter { it.loanId == loan.loanId }
                    totalPenalty += penaltyEngine.totalPenaltyForLoan(loanRepayments, loan)
                }

                // Generate actionable insights
                val insights = generateInsights(
                    healthScore, punctualityScore, utilizationScore, diversityScore,
                    utilRatio, onTimePayments, totalMaturedRepayments,
                    overdueRepayments.size, totalPenalty, categories, monthlyTrend
                )

                _state.update {
                    it.copy(
                        categories = categories,
                        monthlyTrend = monthlyTrend,
                        healthScore = healthScore,
                        onTimePayments = onTimePayments,
                        totalPayments = totalMaturedRepayments,
                        utilizationRatio = utilRatio,
                        totalOutstanding = totalOutstanding,
                        totalSanctioned = totalSanctioned,
                        diversityScore = diversityScore,
                        punctualityScore = punctualityScore,
                        utilizationScore = utilizationScore,
                        ageScore = ageScore,
                        oldestLoanDays = daysSinceOldest,
                        totalPenaltyAccrued = totalPenalty,
                        overdueCount = overdueRepayments.size,
                        insights = insights,
                        isLoading = false
                    )
                }
            }.collect { /* combined flow */ }
        }
    }

    private fun generateInsights(
        healthScore: Int, punctualityScore: Int, utilizationScore: Int, diversityScore: Int,
        utilRatio: Float, onTimePayments: Int, totalPayments: Int,
        overdueCount: Int, totalPenalty: Double,
        categories: List<CategoryBreakdown>, monthlyTrend: List<MonthlySpending>
    ): List<FinancialInsight> {
        val insights = mutableListOf<FinancialInsight>()

        // Overdue alert
        if (overdueCount > 0) {
            insights.add(FinancialInsight(
                icon = Icons.Default.Warning,
                title = "$overdueCount Overdue Payment${if (overdueCount > 1) "s" else ""}",
                description = "You have $overdueCount overdue EMI${if (overdueCount > 1) "s" else ""} accruing ₹${String.format(java.util.Locale.getDefault(), "%,.0f", totalPenalty)} in penalties. Pay immediately to avoid further charges.",
                color = Red400,
                severity = "CRITICAL"
            ))
        }

        // Penalty warning
        if (totalPenalty > 0 && overdueCount == 0) {
            insights.add(FinancialInsight(
                icon = Icons.Default.MoneyOff,
                title = "₹${String.format(java.util.Locale.getDefault(), "%,.0f", totalPenalty)} in Penalties",
                description = "You have accrued penalties on past overdue payments. Maintaining on-time payments will prevent future penalties.",
                color = Orange400,
                severity = "WARNING"
            ))
        }

        // Punctuality insight
        if (totalPayments > 0) {
            val punctualityPct = (onTimePayments.toFloat() / totalPayments * 100).toInt()
            if (punctualityPct >= 90) {
                insights.add(FinancialInsight(
                    icon = Icons.Default.Verified,
                    title = "Excellent Payment Record!",
                    description = "$punctualityPct% of your payments are on time. This boosts your credit reputation significantly.",
                    color = Emerald400,
                    severity = "GOOD"
                ))
            } else if (punctualityPct < 60) {
                insights.add(FinancialInsight(
                    icon = Icons.Default.TrendingDown,
                    title = "Payment Punctuality Needs Work",
                    description = "Only $punctualityPct% of payments were on time. Set up reminders or auto-pay to improve your score.",
                    color = Orange400,
                    severity = "WARNING"
                ))
            }
        }

        // Utilization insight
        if (utilRatio > 0.8f) {
            insights.add(FinancialInsight(
                icon = Icons.Default.PieChart,
                title = "High Credit Utilization (${(utilRatio * 100).toInt()}%)",
                description = "Using more than 80% of your sanctioned amount hurts your score. Try to keep utilization below 60%.",
                color = Orange400,
                severity = "WARNING"
            ))
        } else if (utilRatio <= 0.3f && utilRatio > 0f) {
            insights.add(FinancialInsight(
                icon = Icons.Default.CheckCircle,
                title = "Healthy Credit Utilization",
                description = "You're using only ${(utilRatio * 100).toInt()}% of your credit. This is ideal for maintaining a strong financial profile.",
                color = Emerald400,
                severity = "GOOD"
            ))
        }

        // Spending concentration
        if (categories.isNotEmpty()) {
            val totalSpending = categories.sumOf { it.total }
            val topCategory = categories.maxByOrNull { it.total }
            if (topCategory != null && totalSpending > 0) {
                val topPct = (topCategory.total / totalSpending * 100).toInt()
                if (topPct > 70) {
                    insights.add(FinancialInsight(
                        icon = Icons.Default.Category,
                        title = "Spending Concentrated in ${topCategory.purposeCategory}",
                        description = "$topPct% of your disbursements go to ${topCategory.purposeCategory}. Diversifying spending categories improves your diversity score.",
                        color = Color(0xFF4FC3F7),
                        severity = "WARNING"
                    ))
                }
            }
        }

        // Monthly trend insight
        if (monthlyTrend.size >= 2) {
            val last = monthlyTrend.last().total
            val secondLast = monthlyTrend[monthlyTrend.size - 2].total
            if (secondLast > 0) {
                val changePercent = ((last - secondLast) / secondLast * 100).toInt()
                if (changePercent > 30) {
                    insights.add(FinancialInsight(
                        icon = Icons.Default.TrendingUp,
                        title = "Spending Up ${changePercent}% This Month",
                        description = "Your spending increased significantly compared to last month. Review if this aligns with your loan purpose.",
                        color = Orange400,
                        severity = "WARNING"
                    ))
                } else if (changePercent < -20) {
                    insights.add(FinancialInsight(
                        icon = Icons.Default.TrendingDown,
                        title = "Spending Down ${-changePercent}% This Month",
                        description = "Great job! Your spending decreased compared to last month, which helps with faster loan repayment.",
                        color = Emerald400,
                        severity = "GOOD"
                    ))
                }
            }
        }

        // Score-based encouragement
        if (healthScore >= 80 && insights.none { it.severity == "CRITICAL" }) {
            insights.add(FinancialInsight(
                icon = Icons.Default.EmojiEvents,
                title = "Outstanding Financial Health!",
                description = "Your score of $healthScore/100 puts you in the top tier. Keep it up to unlock better lending terms.",
                color = Gold500,
                severity = "GOOD"
            ))
        }

        return insights.sortedBy { when(it.severity) { "CRITICAL" -> 0; "WARNING" -> 1; else -> 2 } }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinancialHealthScreen(
    onBack: () -> Unit,
    viewModel: FinancialHealthViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Financial Health", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            if (state.isLoading) {
                CircularProgressIndicator()
                return@Column
            }

            // Health Score Gauge
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Financial Health Score", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(16.dp))
                    HealthScoreGauge(score = state.healthScore)
                    Spacer(modifier = Modifier.height(12.dp))
                    val (label, color) = when {
                        state.healthScore >= 80 -> "Excellent" to Emerald400
                        state.healthScore >= 60 -> "Good" to Gold500
                        state.healthScore >= 40 -> "Fair" to Orange400
                        else -> "Needs Improvement" to Red400
                    }
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = color.copy(alpha = 0.15f)
                    ) {
                        Text(
                            label,
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                            fontWeight = FontWeight.Bold,
                            color = color
                        )
                    }
                }
            }

            // Actionable Insights
            if (state.insights.isNotEmpty()) {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Text("💡 Insights & Tips", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(12.dp))
                    state.insights.forEach { insight ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = insight.color.copy(alpha = 0.08f)
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Icon(
                                    insight.icon, null,
                                    tint = insight.color,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        insight.title,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = insight.color
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        insight.description,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Penalty Summary
            if (state.totalPenaltyAccrued > 0 || state.overdueCount > 0) {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Text("⚠️ Penalty Summary", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Overdue EMIs", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("${state.overdueCount}", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = Red400)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Total Penalty Accrued", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("₹${String.format(java.util.Locale.getDefault(), "%,.0f", state.totalPenaltyAccrued)}", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = Red400)
                        }
                    }
                }
            }

            // Score Breakdown
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Text("Score Breakdown", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(16.dp))

                ScoreFactorRow("Repayment Punctuality", state.punctualityScore, 40, Emerald400)
                Spacer(modifier = Modifier.height(12.dp))
                ScoreFactorRow("Credit Utilization", state.utilizationScore, 30, Gold500)
                Spacer(modifier = Modifier.height(12.dp))
                ScoreFactorRow("Spending Diversity", state.diversityScore, 15, Color(0xFF4FC3F7))
                Spacer(modifier = Modifier.height(12.dp))
                ScoreFactorRow("Credit History Age", state.ageScore, 15, Color(0xFFBA68C8))

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Utilization", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("₹${String.format(java.util.Locale.getDefault(), "%,.0f", state.totalOutstanding)} / ₹${String.format(java.util.Locale.getDefault(), "%,.0f", state.totalSanctioned)}",
                        style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Credit Age", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("${state.oldestLoanDays} days", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                }
            }

            // Monthly Spending Trend
            if (state.monthlyTrend.isNotEmpty()) {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Text("📈 Monthly Spending Trend", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("Disbursement totals by month", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(16.dp))

                    MonthlyTrendChart(
                        data = state.monthlyTrend,
                        modifier = Modifier.fillMaxWidth().height(180.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                    // Month labels
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        state.monthlyTrend.takeLast(6).forEach { month ->
                            Text(
                                month.monthLabel,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            // Spending Breakdown Donut Chart
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Text("Spending Breakdown", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("By disbursement category", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(16.dp))

                if (state.categories.isEmpty()) {
                    Text("No disbursement data yet.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    CategoryDonutChart(categories = state.categories, modifier = Modifier.fillMaxWidth().height(200.dp))
                    Spacer(modifier = Modifier.height(16.dp))
                    // Legend
                    val totalSpending = state.categories.sumOf { it.total }
                    state.categories.forEach { cat ->
                        val pct = if (totalSpending > 0) (cat.total / totalSpending * 100).toInt() else 0
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Canvas(modifier = Modifier.size(12.dp)) {
                                    drawCircle(color = categoryColor(cat.purposeCategory))
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(cat.purposeCategory, style = MaterialTheme.typography.bodyMedium)
                            }
                            Text("₹${String.format(java.util.Locale.getDefault(), "%,.0f", cat.total)} ($pct%)", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
private fun ScoreFactorRow(label: String, score: Int, maxScore: Int, color: Color) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, style = MaterialTheme.typography.bodyMedium)
            Text("$score / $maxScore", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = color)
        }
        Spacer(modifier = Modifier.height(4.dp))
        val animatedProgress by animateFloatAsState(
            targetValue = if (maxScore > 0) score.toFloat() / maxScore else 0f,
            animationSpec = tween(1000, easing = FastOutSlowInEasing),
            label = "factor_$label"
        )
        LinearProgressIndicator(
            progress = { animatedProgress },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp),
            color = color,
            trackColor = color.copy(alpha = 0.15f)
        )
    }
}

@Composable
fun HealthScoreGauge(score: Int) {
    val animatedProgress by animateFloatAsState(
        targetValue = (score / 100f).coerceIn(0f, 1f),
        animationSpec = tween(1600, easing = FastOutSlowInEasing),
        label = "speedometer_score"
    )

    val (tierLabel, tierColor) = when {
        score >= 85 -> "PRIME TIER" to Emerald400
        score >= 70 -> "VERY GOOD" to Emerald500
        score >= 50 -> "FAIR TIER" to Gold500
        else -> "SUBPRIME" to Red400
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            contentAlignment = Alignment.BottomCenter,
            modifier = Modifier
                .size(width = 240.dp, height = 140.dp)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val strokeWidth = 22f
                val arcRadius = (size.width - strokeWidth) / 2
                val arcTop = size.height - arcRadius
                val arcSize = Size(arcRadius * 2, arcRadius * 2)
                val arcTopLeft = Offset((size.width - arcRadius * 2) / 2, arcTop)

                // Background 180-degree arc
                drawArc(
                    color = Gray700.copy(alpha = 0.4f),
                    startAngle = 180f,
                    sweepAngle = 180f,
                    useCenter = false,
                    topLeft = arcTopLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )

                // Multi-color active gauge gradient: Red -> Orange -> Gold -> Emerald
                val gradientBrush = Brush.horizontalGradient(
                    colors = listOf(Red400, Orange400, Gold500, Emerald400),
                    startX = arcTopLeft.x,
                    endX = arcTopLeft.x + arcSize.width
                )

                drawArc(
                    brush = gradientBrush,
                    startAngle = 180f,
                    sweepAngle = 180f * animatedProgress,
                    useCenter = false,
                    topLeft = arcTopLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )

                // Draw needle pointer
                val needleAngleRad = Math.toRadians((180.0 + 180.0 * animatedProgress).toDouble())
                val center = Offset(size.width / 2, size.height - 4f)
                val needleLength = arcRadius - strokeWidth - 6f
                val needleEnd = Offset(
                    (center.x + needleLength * Math.cos(needleAngleRad)).toFloat(),
                    (center.y + needleLength * Math.sin(needleAngleRad)).toFloat()
                )

                // Needle Line
                drawLine(
                    color = Color.White,
                    start = center,
                    end = needleEnd,
                    strokeWidth = 5f,
                    cap = StrokeCap.Round
                )

                // Center Needle Hub (Cap)
                drawCircle(color = tierColor, radius = 12f, center = center)
                drawCircle(color = Navy900, radius = 6f, center = center)
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Big Score Display
        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = "$score",
                style = MaterialTheme.typography.displayLarge,
                fontWeight = FontWeight.ExtraBold,
                color = tierColor
            )
            Text(
                text = " /100",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Gray400,
                modifier = Modifier.padding(bottom = 8.dp, start = 2.dp)
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Credit Tier Pill
        Surface(
            shape = CircleShape,
            color = tierColor.copy(alpha = 0.15f),
            border = androidx.compose.foundation.BorderStroke(1.dp, tierColor.copy(alpha = 0.35f))
        ) {
            Text(
                text = tierLabel,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                fontWeight = FontWeight.ExtraBold,
                color = tierColor,
                fontSize = 12.sp,
                letterSpacing = 1.sp
            )
        }
    }
}

@Composable
fun MonthlyTrendChart(data: List<MonthlySpending>, modifier: Modifier = Modifier) {
    val displayData = data.takeLast(6)
    if (displayData.isEmpty()) return

    val maxVal = displayData.maxOf { it.total }.toFloat().coerceAtLeast(1f)

    Canvas(modifier = modifier) {
        val barWidth = size.width / (displayData.size * 2f)
        val spacing = barWidth

        displayData.forEachIndexed { index, month ->
            val barHeight = (month.total.toFloat() / maxVal) * (size.height * 0.85f)
            val x = index * (barWidth + spacing) + spacing / 2

            // Bar
            drawRoundRect(
                color = Gold500.copy(alpha = 0.3f),
                topLeft = Offset(x, size.height - barHeight),
                size = Size(barWidth, barHeight),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(8f, 8f)
            )
            drawRoundRect(
                color = Gold500,
                topLeft = Offset(x, size.height - barHeight),
                size = Size(barWidth, barHeight),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(8f, 8f),
                style = Stroke(width = 2f)
            )
        }

        // Draw connecting line
        if (displayData.size > 1) {
            val linePath = Path()
            displayData.forEachIndexed { index, month ->
                val barHeight = (month.total.toFloat() / maxVal) * (size.height * 0.85f)
                val x = index * (barWidth + spacing) + spacing / 2 + barWidth / 2
                val y = size.height - barHeight
                if (index == 0) linePath.moveTo(x, y) else linePath.lineTo(x, y)
            }
            drawPath(
                path = linePath,
                color = Emerald400,
                style = Stroke(width = 3f, cap = StrokeCap.Round)
            )
        }
    }
}

@Composable
fun CategoryDonutChart(categories: List<CategoryBreakdown>, modifier: Modifier = Modifier) {
    val total = categories.sumOf { it.total }.toFloat().coerceAtLeast(1f)
    Canvas(modifier = modifier) {
        val strokeWidth = 40f
        val diameter = minOf(size.width, size.height) - strokeWidth
        val topLeft = Offset((size.width - diameter) / 2, (size.height - diameter) / 2)
        val arcSize = Size(diameter, diameter)
        var startAngle = -90f

        categories.forEach { cat ->
            val sweep = (cat.total.toFloat() / total) * 360f
            drawArc(
                color = categoryColor(cat.purposeCategory),
                startAngle = startAngle,
                sweepAngle = sweep - 2f, // gap
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
            startAngle += sweep
        }
    }
}

fun categoryColor(category: String): Color = when (category.uppercase()) {
    "MEDICAL" -> Color(0xFF4FC3F7)
    "EDUCATION" -> Color(0xFF81C784)
    "BUSINESS" -> Color(0xFFFFB74D)
    "HOUSING" -> Color(0xFFE57373)
    "AGRICULTURE" -> Color(0xFFAED581)
    "OTHER" -> Color(0xFFBA68C8)
    else -> Color(0xFF90A4AE)
}
