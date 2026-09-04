package com.loanzo.app.ui.loan

import androidx.compose.ui.res.stringResource
import com.loanzo.app.R

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.loanzo.app.ui.components.GlassCard
import com.loanzo.app.ui.components.GradientCard
import com.loanzo.app.ui.theme.*
import com.loanzo.app.util.calculateEMI
import com.loanzo.app.util.toInrString
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoanCalculatorScreen(
    onBack: () -> Unit
) {
    var principal by remember { androidx.compose.runtime.mutableFloatStateOf(100000f) }
    var interestRate by remember { androidx.compose.runtime.mutableFloatStateOf(12f) }
    var tenureMonths by remember { androidx.compose.runtime.mutableFloatStateOf(12f) }

    val emi = calculateEMI(principal.toDouble(), interestRate.toDouble(), tenureMonths.toInt())
    val totalPayment = emi * tenureMonths
    val totalInterest = totalPayment - principal

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.loan_simulator), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back") }
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
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Summary Card
            GradientCard(gradientColors = listOf(Navy600, Navy700)) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.monthly_emi), style = MaterialTheme.typography.labelLarge, color = Gray300)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        emi.toInrString(),
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.Bold,
                        color = Gold400
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text(stringResource(R.string.principal), style = MaterialTheme.typography.labelMedium, color = Gray300)
                        Text(principal.toDouble().toInrString(), style = MaterialTheme.typography.titleMedium, color = Emerald400, fontWeight = FontWeight.Bold)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(stringResource(R.string.total_interest), style = MaterialTheme.typography.labelMedium, color = Gray300)
                        Text(totalInterest.toInrString(), style = MaterialTheme.typography.titleMedium, color = Orange400, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Interactive chart
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                contentAlignment = Alignment.Center
            ) {
                val principalAngle = if (totalPayment > 0) (principal / totalPayment) * 360f else 0f
                val interestAngle = if (totalPayment > 0) (totalInterest / totalPayment) * 360f else 0f

                Canvas(modifier = Modifier.size(160.dp)) {
                    drawArc(
                        color = Emerald400,
                        startAngle = -90f,
                        sweepAngle = principalAngle.toFloat(),
                        useCenter = false,
                        style = Stroke(width = 40f, cap = StrokeCap.Round)
                    )
                    drawArc(
                        color = Orange400,
                        startAngle = -90f + principalAngle.toFloat(),
                        sweepAngle = interestAngle.toFloat(),
                        useCenter = false,
                        style = Stroke(width = 40f, cap = StrokeCap.Round)
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(stringResource(R.string.total), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(totalPayment.toInrString(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
            }

            // Sliders
            GlassCard {
                Text(stringResource(R.string.adjust_parameters), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(16.dp))

                // Principal Slider
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(stringResource(R.string.loan_amount), style = MaterialTheme.typography.labelLarge)
                    Text(principal.toDouble().toInrString(), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
                Slider(
                    value = principal,
                    onValueChange = { principal = it },
                    valueRange = 10000f..1000000f,
                    steps = 99, // 10k steps
                    colors = SliderDefaults.colors(thumbColor = Emerald400, activeTrackColor = Emerald400)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Interest Rate Slider
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(stringResource(R.string.interest_rate_p_a), style = MaterialTheme.typography.labelLarge)
                    Text("${String.format(java.util.Locale.getDefault(), "%.1f", interestRate)}%", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
                Slider(
                    value = interestRate,
                    onValueChange = { interestRate = it },
                    valueRange = 5f..36f,
                    colors = SliderDefaults.colors(thumbColor = Orange400, activeTrackColor = Orange400)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Tenure Slider
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(stringResource(R.string.tenure), style = MaterialTheme.typography.labelLarge)
                    Text("${tenureMonths.roundToInt()} Months", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
                Slider(
                    value = tenureMonths,
                    onValueChange = { tenureMonths = it },
                    valueRange = 3f..60f,
                    steps = 56, // 1 month steps
                    colors = SliderDefaults.colors(thumbColor = Blue400, activeTrackColor = Blue400)
                )
            }
            
            Button(
                onClick = { onBack() },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(Icons.Default.Calculate, null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.apply_for_this_loan), fontWeight = FontWeight.Bold)
            }
            
            Spacer(Modifier.height(24.dp))
        }
    }
}
