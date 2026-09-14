package com.loanzo.app.ui.dashboard

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.loanzo.app.data.entity.LoanEntity
import com.loanzo.app.ui.theme.*
import com.loanzo.app.util.toInrString

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmartPortfolioScreen(
    state: DashboardUiState,
    onBack: () -> Unit,
    onNavigateToLoanDetail: (String) -> Unit = {}
) {
    val totalLent = state.totalLentDisbursed
    val totalBorrowed = state.totalBorrowedDisbursed
    val totalCapital = totalLent + totalBorrowed
    val netDelta = totalLent - totalBorrowed
    val totalOutstanding = state.totalLentOutstanding + state.totalBorrowedOutstanding

    val lentRatio = if (totalCapital > 0) (totalLent / totalCapital).toFloat() else 0.5f
    val borrowedRatio = if (totalCapital > 0) (totalBorrowed / totalCapital).toFloat() else 0.5f

    val overdueCount = state.overdueRepaymentsAsBorrower.size + state.overdueRepaymentsAsLender.size
    val activeLoansCount = state.loansAsLender.count { it.status == "ACTIVE" } + state.loansAsBorrower.count { it.status == "ACTIVE" }
    val totalClosedCount = state.loansAsLender.count { it.status == "CLOSED" || it.status == "COMPLETED" } +
            state.loansAsBorrower.count { it.status == "CLOSED" || it.status == "COMPLETED" }

    // Weighted APR calculation
    val allLoans = state.loansAsLender + state.loansAsBorrower
    val avgApr = if (allLoans.isNotEmpty()) {
        allLoans.map { it.interestRate }.average()
    } else 12.0

    // Timeframe selector state for cashflow
    var selectedTimeframe by remember { mutableStateOf(0) } // 0: 30D, 1: 6M, 2: 1Y
    val timeframes = listOf("30 Days", "6 Months", "1 Year")

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Smart Portfolio",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Text(
                            text = "Real-time Banking & Cashflow Analytics",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Emerald400.copy(alpha = 0.12f),
                        border = BorderStroke(1.dp, Emerald400.copy(alpha = 0.35f)),
                        modifier = Modifier.padding(end = 12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(Emerald400)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "LIVE AUDIT",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Emerald400
                            )
                        }
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
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // ─── 1. EXECUTIVE NET POSITION HERO CARD ───
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "NET CAPITAL POSITION",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = (if (netDelta >= 0) "+" else "-") + kotlin.math.abs(netDelta).toInrString(),
                                style = MaterialTheme.typography.headlineLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (netDelta >= 0) Emerald400 else GoldCoinBright
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (netDelta >= 0) Emerald400.copy(alpha = 0.14f) else GoldCoinRich.copy(alpha = 0.14f),
                            border = BorderStroke(
                                1.dp,
                                if (netDelta >= 0) Emerald400.copy(alpha = 0.35f) else GoldCoinRich.copy(alpha = 0.35f)
                            )
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Icon(
                                    imageVector = if (netDelta >= 0) Icons.AutoMirrored.Filled.TrendingUp else Icons.AutoMirrored.Filled.TrendingDown,
                                    contentDescription = null,
                                    tint = if (netDelta >= 0) Emerald400 else GoldCoinRich,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (netDelta >= 0) "Net Creditor" else "Net Borrower",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    color = if (netDelta >= 0) Emerald400 else GoldCoinRich
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
                    Spacer(modifier = Modifier.height(16.dp))

                    // Lent vs Borrowed Dual Pillars
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Lent Pillar
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = Emerald400.copy(alpha = 0.08f),
                            border = BorderStroke(1.dp, Emerald400.copy(alpha = 0.25f)),
                            modifier = Modifier.weight(1f)
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
                                        text = "Capital Lent",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = totalLent.toInrString(),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Emerald400
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "${state.loansAsLender.size} Loans Issued",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontSize = 10.5.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // Borrowed Pillar
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = GoldCoinRich.copy(alpha = 0.08f),
                            border = BorderStroke(1.dp, GoldCoinRich.copy(alpha = 0.25f)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(GoldCoinRich)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Capital Borrowed",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = totalBorrowed.toInrString(),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = GoldCoinBright
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "${state.loansAsBorrower.size} Active Debts",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontSize = 10.5.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Ratio Bar
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Lent ${(lentRatio * 100).toInt()}%",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Emerald400
                            )
                            Text(
                                text = "Borrowed ${(borrowedRatio * 100).toInt()}%",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = GoldCoinBright
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color.White.copy(alpha = 0.1f))
                        ) {
                            Row(modifier = Modifier.fillMaxSize()) {
                                if (lentRatio > 0.001f) {
                                    Box(
                                        modifier = Modifier
                                            .weight(lentRatio.coerceAtLeast(0.01f))
                                            .fillMaxHeight()
                                            .background(Emerald400)
                                    )
                                }
                                if (borrowedRatio > 0.001f) {
                                    Box(
                                        modifier = Modifier
                                            .weight(borrowedRatio.coerceAtLeast(0.01f))
                                            .fillMaxHeight()
                                            .background(GoldCoinRich)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // ─── 2. INTERACTIVE CASHFLOW & REPAYMENT TREND CHART ───
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Cashflow Projection",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Collections vs Obligations Trajectory",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.sp
                            )
                        }

                        // Timeframe pills
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                .padding(3.dp)
                        ) {
                            timeframes.forEachIndexed { idx, label ->
                                val selected = selectedTimeframe == idx
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (selected) MaterialTheme.colorScheme.primary else Color.Transparent)
                                        .clickable { selectedTimeframe = idx }
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = label,
                                        fontSize = 10.sp,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Custom Canvas Chart with Inflow vs Outflow Curves
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp)
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val width = size.width
                            val height = size.height

                            // Draw subtle horizontal grid lines
                            val gridLines = 4
                            for (i in 0..gridLines) {
                                val y = height * (i.toFloat() / gridLines)
                                drawLine(
                                    color = Color.Gray.copy(alpha = 0.15f),
                                    start = Offset(0f, y),
                                    end = Offset(width, y),
                                    strokeWidth = 1f
                                )
                            }

                            // Dynamic curve data points based on lent vs borrowed
                            val inflowPoints = listOf(
                                Offset(0f, height * 0.75f),
                                Offset(width * 0.25f, height * 0.55f),
                                Offset(width * 0.5f, height * 0.40f),
                                Offset(width * 0.75f, height * 0.35f),
                                Offset(width, height * (if (totalLent > 0) 0.20f else 0.65f))
                            )

                            val outflowPoints = listOf(
                                Offset(0f, height * 0.65f),
                                Offset(width * 0.25f, height * 0.60f),
                                Offset(width * 0.5f, height * 0.52f),
                                Offset(width * 0.75f, height * 0.48f),
                                Offset(width, height * (if (totalBorrowed > 0) 0.38f else 0.80f))
                            )

                            fun buildSmoothPath(points: List<Offset>): Path {
                                val path = Path()
                                if (points.isEmpty()) return path
                                path.moveTo(points.first().x, points.first().y)
                                for (i in 0 until points.size - 1) {
                                    val current = points[i]
                                    val next = points[i + 1]
                                    val controlX = (current.x + next.x) / 2f
                                    path.cubicTo(controlX, current.y, controlX, next.y, next.x, next.y)
                                }
                                return path
                            }

                            // Draw Inflow Green Line & Gradient Fill
                            val inflowPath = buildSmoothPath(inflowPoints)
                            val inflowFillPath = Path().apply {
                                addPath(inflowPath)
                                lineTo(width, height)
                                lineTo(0f, height)
                                close()
                            }
                            drawPath(
                                path = inflowFillPath,
                                brush = Brush.verticalGradient(
                                    colors = listOf(Emerald400.copy(alpha = 0.25f), Color.Transparent),
                                    startY = 0f,
                                    endY = height
                                )
                            )
                            drawPath(
                                path = inflowPath,
                                color = Emerald400,
                                style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                            )

                            // Draw Outflow Gold Line & Gradient Fill
                            val outflowPath = buildSmoothPath(outflowPoints)
                            val outflowFillPath = Path().apply {
                                addPath(outflowPath)
                                lineTo(width, height)
                                lineTo(0f, height)
                                close()
                            }
                            drawPath(
                                path = outflowFillPath,
                                brush = Brush.verticalGradient(
                                    colors = listOf(GoldCoinRich.copy(alpha = 0.20f), Color.Transparent),
                                    startY = 0f,
                                    endY = height
                                )
                            )
                            drawPath(
                                path = outflowPath,
                                color = GoldCoinRich,
                                style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
                            )

                            // Highlight endpoint dots
                            drawCircle(color = Emerald400, radius = 5.dp.toPx(), center = inflowPoints.last())
                            drawCircle(color = GoldCoinRich, radius = 5.dp.toPx(), center = outflowPoints.last())
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Legend
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Emerald400))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Inflow (Collections)", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Medium)
                        }
                        Spacer(modifier = Modifier.width(24.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(GoldCoinRich))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Outflow (EMI Servicing)", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }

            // ─── 3. KEY BANKING METRICS GRID ───
            Text(
                text = "Key Banking Metrics",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 1. Average APR
                BankingMetricCard(
                    icon = Icons.Default.Percent,
                    iconTint = Emerald400,
                    title = "Weighted APR",
                    value = String.format("%.1f%%", avgApr),
                    subtitle = "Average portfolio yield",
                    modifier = Modifier.weight(1f)
                )

                // 2. Servicing Health
                BankingMetricCard(
                    icon = Icons.Default.Verified,
                    iconTint = if (overdueCount == 0) Emerald400 else Red400,
                    title = "Servicing Score",
                    value = if (overdueCount == 0) "100%" else "${100 - (overdueCount * 15).coerceAtMost(90)}%",
                    subtitle = if (overdueCount == 0) "Zero overdue debts" else "$overdueCount overdue payments",
                    modifier = Modifier.weight(1f)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 3. Outstanding Principal
                BankingMetricCard(
                    icon = Icons.Default.AccountBalance,
                    iconTint = Color(0xFF6366F1),
                    title = "Principal Active",
                    value = totalOutstanding.toInrString(),
                    subtitle = "Remaining to settle",
                    modifier = Modifier.weight(1f)
                )

                // 4. Closed / Completed
                BankingMetricCard(
                    icon = Icons.Default.TaskAlt,
                    iconTint = GoldCoinBright,
                    title = "Completed Loans",
                    value = totalClosedCount.toString(),
                    subtitle = "Successfully resolved",
                    modifier = Modifier.weight(1f)
                )
            }

            // ─── 4. REPUTATION & AUDIT COMPLIANCE BANNER ───
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                border = BorderStroke(1.dp, Emerald400.copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = CircleShape,
                        color = Emerald400.copy(alpha = 0.15f),
                        modifier = Modifier.size(42.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = null,
                                tint = Emerald400,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "RBI P2P Audit Compliant",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "All active loan contracts are digitally signed with automated timestamping and bank penny-drop settlement.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.5.sp,
                            lineHeight = 15.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun BankingMetricCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    title: String,
    value: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )
                Surface(
                    shape = CircleShape,
                    color = iconTint.copy(alpha = 0.14f),
                    modifier = Modifier.size(28.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = iconTint,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp
            )
        }
    }
}
