package com.loanzo.app.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.loanzo.app.ui.theme.*
import com.loanzo.app.util.toInrString
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.ln
import kotlin.math.roundToInt

/**
 * Prepayment & Foreclosure Savings Calculator Modal Bottom Sheet.
 * Inspired by Jupiter Debt Payoff Planner and Cred Foreclosure Simulator.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrepaymentSimulatorSheet(
    outstandingPrincipal: Double,
    annualInterestRate: Double,
    tenureRemainingMonths: Int,
    currentMonthlyEmi: Double,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var lumpSumPrepayment by remember { mutableDoubleStateOf(0.0) }
    var extraMonthlyContribution by remember { mutableDoubleStateOf(0.0) }

    // Calculation logic
    val r = (annualInterestRate / 100.0) / 12.0
    val baselineInterest = ((currentMonthlyEmi * tenureRemainingMonths) - outstandingPrincipal).coerceAtLeast(0.0)

    val adjustedPrincipal = (outstandingPrincipal - lumpSumPrepayment).coerceAtLeast(0.0)
    val adjustedMonthly = currentMonthlyEmi + extraMonthlyContribution

    val newTenureMonths: Int = remember(adjustedPrincipal, adjustedMonthly, r, tenureRemainingMonths) {
        if (adjustedPrincipal <= 0.0) {
            0
        } else if (r > 0 && adjustedMonthly > adjustedPrincipal * r) {
            val n = ln(adjustedMonthly / (adjustedMonthly - adjustedPrincipal * r)) / ln(1.0 + r)
            n.roundToInt().coerceIn(1, tenureRemainingMonths)
        } else if (r == 0.0 && adjustedMonthly > 0) {
            (adjustedPrincipal / adjustedMonthly).roundToInt().coerceIn(1, tenureRemainingMonths)
        } else {
            tenureRemainingMonths
        }
    }

    val newTotalInterest = if (newTenureMonths == 0) 0.0 else ((adjustedMonthly * newTenureMonths) - adjustedPrincipal).coerceAtLeast(0.0)
    val interestSaved = (baselineInterest - newTotalInterest).coerceAtLeast(0.0)
    val monthsSaved = (tenureRemainingMonths - newTenureMonths).coerceAtLeast(0)

    // Projected payoff dates
    val originalPayoffCalendar = remember(tenureRemainingMonths) {
        Calendar.getInstance().apply { add(Calendar.MONTH, tenureRemainingMonths) }
    }
    val newPayoffCalendar = remember(newTenureMonths) {
        Calendar.getInstance().apply { add(Calendar.MONTH, newTenureMonths) }
    }
    val dateFormat = remember { SimpleDateFormat("MMMM yyyy", Locale.getDefault()) }
    val originalPayoffDate = remember(originalPayoffCalendar) { dateFormat.format(originalPayoffCalendar.time) }
    val newPayoffDate = remember(newPayoffCalendar) { dateFormat.format(newPayoffCalendar.time) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = {
            BottomSheetDefaults.DragHandle(color = MaterialTheme.colorScheme.outlineVariant)
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 36.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Debt Payoff Simulator",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "See how much interest you save with prepayments",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, "Close", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            // Hero Savings Card
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = EmeraldLight),
                border = androidx.compose.foundation.BorderStroke(1.dp, Emerald400.copy(alpha = 0.4f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .animateContentSize()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "TOTAL INTEREST SAVINGS",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Gray400,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = interestSaved.toInrString(),
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = Emerald400
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Surface(
                        shape = CircleShape,
                        color = Emerald400.copy(alpha = 0.15f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Timer, null, tint = Emerald400, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (monthsSaved > 0) "Debt-Free $monthsSaved Months Faster!" else "No tenure change yet",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = Emerald400
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = Gray700.copy(alpha = 0.4f))
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("New Payoff Date", style = MaterialTheme.typography.labelSmall, color = Gray400)
                            Text(newPayoffDate, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = Gold500)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Original Payoff Date", style = MaterialTheme.typography.labelSmall, color = Gray400)
                            Text(originalPayoffDate, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = Gray500)
                        }
                    }
                }
            }

            // 1. Lump Sum Prepayment Slider
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Lump Sum Prepayment",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = lumpSumPrepayment.toInrString(),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Slider(
                        value = lumpSumPrepayment.toFloat(),
                        onValueChange = { lumpSumPrepayment = it.toDouble() },
                        valueRange = 0f..outstandingPrincipal.toFloat(),
                        colors = SliderDefaults.colors(
                            thumbColor = MaterialTheme.colorScheme.primary,
                            activeTrackColor = MaterialTheme.colorScheme.primary,
                            inactiveTrackColor = MaterialTheme.colorScheme.outlineVariant
                        )
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(5000.0, 10000.0, 25000.0, 50000.0).forEach { preset ->
                            if (preset <= outstandingPrincipal) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (lumpSumPrepayment == preset) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                                    contentColor = if (lumpSumPrepayment == preset) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { lumpSumPrepayment = preset }
                                ) {
                                    Text(
                                        text = "+₹${(preset / 1000).toInt()}K",
                                        modifier = Modifier.padding(vertical = 6.dp),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 2. Extra Monthly Contribution Slider
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Extra Monthly Top-up",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "+${extraMonthlyContribution.toInrString()}/mo",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = Emerald500
                        )
                    }

                    Slider(
                        value = extraMonthlyContribution.toFloat(),
                        onValueChange = { extraMonthlyContribution = it.toDouble() },
                        valueRange = 0f..(currentMonthlyEmi * 2).toFloat().coerceAtLeast(10000f),
                        colors = SliderDefaults.colors(
                            thumbColor = Emerald500,
                            activeTrackColor = Emerald500,
                            inactiveTrackColor = MaterialTheme.colorScheme.outlineVariant
                        )
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(1000.0, 2500.0, 5000.0).forEach { preset ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (extraMonthlyContribution == preset) Emerald500 else MaterialTheme.colorScheme.surface,
                                contentColor = if (extraMonthlyContribution == preset) Color.White else MaterialTheme.colorScheme.onSurface,
                                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { extraMonthlyContribution = preset }
                            ) {
                                Text(
                                    text = "+₹${(preset / 1000).toInt()}K",
                                    modifier = Modifier.padding(vertical = 6.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }

            // Close button
            Button(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary)
            ) {
                Text("Done", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }
    }
}
