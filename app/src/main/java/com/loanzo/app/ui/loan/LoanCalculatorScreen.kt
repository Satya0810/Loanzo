package com.loanzo.app.ui.loan

import androidx.compose.animation.*
import androidx.compose.animation.core.*
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.ui.text.input.KeyboardType
import com.loanzo.app.ui.components.LoanzoText as Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.loanzo.app.R
import com.loanzo.app.ui.components.GlassCard
import com.loanzo.app.ui.components.GradientCard
import com.loanzo.app.ui.theme.*
import com.loanzo.app.util.calculateEMI
import com.loanzo.app.util.toInrString
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

enum class SimulatorPerspective {
    BORROWER,
    LENDER
}

enum class SimulatorInterestEngine {
    REDUCING,
    FLAT
}

data class AmortizationScheduleRow(
    val month: Int,
    val emi: Double,
    val principalPaid: Double,
    val interestPaid: Double,
    val remainingBalance: Double
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoanCalculatorScreen(
    onBack: () -> Unit,
    onRequestLoan: (amount: Double, rate: Double, tenure: Int) -> Unit = { _, _, _ -> },
    onPostOffer: (amount: Double, rate: Double, tenure: Int) -> Unit = { _, _, _ -> }
) {
    var perspective by remember { mutableStateOf(SimulatorPerspective.BORROWER) }
    var interestEngine by remember { mutableStateOf(SimulatorInterestEngine.REDUCING) }

    var principal by remember { androidx.compose.runtime.mutableFloatStateOf(100000f) }
    var interestRate by remember { androidx.compose.runtime.mutableFloatStateOf(12f) }
    var tenureMonths by remember { androidx.compose.runtime.mutableIntStateOf(12) }
    var showAmortization by remember { mutableStateOf(false) }
    var showFullAmortization by remember { mutableStateOf(false) }

    // Calculations
    val principalDouble = principal.toDouble()
    val rateDouble = interestRate.toDouble()
    val tenureInt = tenureMonths

    val emi: Double
    val totalPayment: Double
    val totalInterest: Double

    if (interestEngine == SimulatorInterestEngine.REDUCING) {
        emi = calculateEMI(principalDouble, rateDouble, tenureInt)
        totalPayment = emi * tenureInt
        totalInterest = max(0.0, totalPayment - principalDouble)
    } else {
        totalInterest = principalDouble * (rateDouble / 100.0) * (tenureInt / 12.0)
        totalPayment = principalDouble + totalInterest
        emi = if (tenureInt > 0) totalPayment / tenureInt else totalPayment
    }

    // Effective APR for flat rate approximation
    val effectiveApr = if (interestEngine == SimulatorInterestEngine.FLAT && tenureInt > 1) {
        (2.0 * tenureInt / (tenureInt + 1)) * rateDouble
    } else {
        rateDouble
    }

    // Amortization Schedule generator
    val amortizationRows = remember(principalDouble, rateDouble, tenureInt, interestEngine) {
        val list = mutableListOf<AmortizationScheduleRow>()
        var balance = principalDouble
        val monthlyRate = (rateDouble / 100.0) / 12.0

        for (m in 1..tenureInt) {
            if (interestEngine == SimulatorInterestEngine.REDUCING) {
                val interestPart = balance * monthlyRate
                val principalPart = min(balance, max(0.0, emi - interestPart))
                balance = max(0.0, balance - principalPart)
                list.add(AmortizationScheduleRow(m, emi, principalPart, interestPart, balance))
            } else {
                val flatInterestPart = totalInterest / tenureInt
                val flatPrincipalPart = principalDouble / tenureInt
                balance = max(0.0, balance - flatPrincipalPart)
                list.add(AmortizationScheduleRow(m, emi, flatPrincipalPart, flatInterestPart, balance))
            }
        }
        list
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = stringResource(R.string.loan_simulator),
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            text = if (perspective == SimulatorPerspective.BORROWER) "Borrower EMI & Repayment Planner" else "Investor Yield & Cash Flow Planner",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // 1. Dual Perspective Pill Switch (Borrower vs Lender)
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val isBorrower = perspective == SimulatorPerspective.BORROWER
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { perspective = SimulatorPerspective.BORROWER },
                        color = if (isBorrower) BrandRoyalBlue else Color.Transparent,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 10.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccountBalance,
                                contentDescription = null,
                                tint = if (isBorrower) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "Borrower (EMI)",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = if (isBorrower) FontWeight.Bold else FontWeight.Medium,
                                color = if (isBorrower) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    val isLender = perspective == SimulatorPerspective.LENDER
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { perspective = SimulatorPerspective.LENDER },
                        color = if (isLender) Emerald500 else Color.Transparent,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 10.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.TrendingUp,
                                contentDescription = null,
                                tint = if (isLender) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "Investor (Yield)",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = if (isLender) FontWeight.Bold else FontWeight.Medium,
                                color = if (isLender) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // 2. Executive Hero Result Box (Luminous Crisp Card)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(
                        elevation = 4.dp,
                        shape = RoundedCornerShape(24.dp),
                        ambientColor = Color(0x0A0F172A),
                        spotColor = Color(0x181D4ED8)
                    ),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.2.dp, MaterialTheme.colorScheme.outlineVariant),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    MaterialTheme.colorScheme.surface,
                                    MaterialTheme.colorScheme.surface,
                                    BrandIceBlue.copy(alpha = 0.35f)
                                )
                            )
                        )
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Header Tag
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = if (perspective == SimulatorPerspective.BORROWER) BrandIceBlue else EmeraldLight,
                            border = BorderStroke(
                                1.dp,
                                if (perspective == SimulatorPerspective.BORROWER) BrandIceBorder else Emerald400.copy(alpha = 0.4f)
                            )
                        ) {
                            Text(
                                text = if (perspective == SimulatorPerspective.BORROWER) "BORROWER'S OBLIGATION" else "INVESTOR'S CASH INFLOW",
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (perspective == SimulatorPerspective.BORROWER) BrandRoyalBlue else Emerald600
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = GoldCoinCream,
                            border = BorderStroke(1.dp, GoldCoinBorder)
                        ) {
                            Text(
                                text = if (interestEngine == SimulatorInterestEngine.REDUCING) "REDUCING BAL" else "FLAT RATE",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = GoldCoinAmber
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Primary Metric Label & Value
                    Text(
                        text = if (perspective == SimulatorPerspective.BORROWER) "EQUATED MONTHLY INSTALLMENT (EMI)" else "MONTHLY INFLOW (PRINCIPAL + PROFIT)",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        letterSpacing = 0.8.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = emi.toInrString(),
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (perspective == SimulatorPerspective.BORROWER) BrandRoyalBlue else Emerald600
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Donut Chart & Percentage Split
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        val principalAngle = if (totalPayment > 0) ((principalDouble / totalPayment) * 360f).toFloat() else 0f
                        val interestAngle = if (totalPayment > 0) ((totalInterest / totalPayment) * 360f).toFloat() else 0f

                        val animatedPrincipalAngle by animateFloatAsState(
                            targetValue = principalAngle,
                            animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing),
                            label = "principalAngle"
                        )
                        val animatedInterestAngle by animateFloatAsState(
                            targetValue = interestAngle,
                            animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing),
                            label = "interestAngle"
                        )

                        Canvas(modifier = Modifier.size(130.dp)) {
                            // Principal Arc (Emerald Mint)
                            drawArc(
                                color = Emerald500,
                                startAngle = -90f,
                                sweepAngle = animatedPrincipalAngle,
                                useCenter = false,
                                style = Stroke(width = 30f, cap = StrokeCap.Round)
                            )
                            // Interest / Profit Arc (Warm Gold)
                            drawArc(
                                color = GoldCoinRich,
                                startAngle = -90f + animatedPrincipalAngle,
                                sweepAngle = animatedInterestAngle,
                                useCenter = false,
                                style = Stroke(width = 30f, cap = StrokeCap.Round)
                            )
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = if (perspective == SimulatorPerspective.BORROWER) "NET PAYABLE" else "TOTAL MATURITY",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 10.sp
                            )
                            Text(
                                text = totalPayment.toInrString(),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    // Donut Legend & Proportions
                    val principalPct = if (totalPayment > 0) ((principalDouble / totalPayment) * 100).roundToInt() else 0
                    val interestPct = 100 - principalPct

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(Emerald500)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "Principal ($principalPct%)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(Modifier.width(16.dp))
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(GoldCoinRich)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = if (perspective == SimulatorPerspective.BORROWER) "Interest ($interestPct%)" else "Profit Yield ($interestPct%)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // 3-Column Detailed Breakdown Strip
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(horizontalAlignment = Alignment.Start) {
                                Text(
                                    text = if (perspective == SimulatorPerspective.BORROWER) "Principal" else "Capital Lent",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    text = principalDouble.toInrString(),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Emerald600,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = if (perspective == SimulatorPerspective.BORROWER) "Total Interest" else "Total Profit",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    text = totalInterest.toInrString(),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = GoldCoinAmber,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = if (perspective == SimulatorPerspective.BORROWER) "Total Payable" else "Maturity Total",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    text = totalPayment.toInrString(),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Effective APR indicator
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = if (interestEngine == SimulatorInterestEngine.REDUCING) {
                                "Realized APR: ${String.format(java.util.Locale.getDefault(), "%.1f", rateDouble)}% p.a. on monthly reducing balance"
                            } else {
                                "Flat Rate ${String.format(java.util.Locale.getDefault(), "%.1f", rateDouble)}% = Effective ${String.format(java.util.Locale.getDefault(), "%.1f", effectiveApr)}% APR"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            // 3. Calculation Engine Selector (Reducing Balance vs Flat Rate)
            GlassCard {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Interest Calculation Method",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val isReducing = interestEngine == SimulatorInterestEngine.REDUCING
                        FilterChip(
                            selected = isReducing,
                            onClick = { interestEngine = SimulatorInterestEngine.REDUCING },
                            label = {
                                Text(
                                    "Reducing Balance (Standard EMI)",
                                    fontWeight = if (isReducing) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            modifier = Modifier.weight(1f),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = BrandIceBlue,
                                selectedLabelColor = BrandRoyalBlue
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = isReducing,
                                borderColor = if (isReducing) BrandCobalt else MaterialTheme.colorScheme.outlineVariant
                            )
                        )

                        val isFlat = interestEngine == SimulatorInterestEngine.FLAT
                        FilterChip(
                            selected = isFlat,
                            onClick = { interestEngine = SimulatorInterestEngine.FLAT },
                            label = {
                                Text(
                                    "Flat Rate (P2P)",
                                    fontWeight = if (isFlat) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            modifier = Modifier.weight(1f),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = GoldCoinCream,
                                selectedLabelColor = GoldCoinAmber
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = isFlat,
                                borderColor = if (isFlat) GoldCoinBorder else MaterialTheme.colorScheme.outlineVariant
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = if (interestEngine == SimulatorInterestEngine.REDUCING) {
                            "• Reducing: Monthly interest calculated only on the remaining principal. Standard banking practice."
                        } else {
                            "• Flat: Interest calculated on full principal throughout tenure. Standard in peer-to-peer / micro lending."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp
                    )
                }
            }

            // 4. Interactive Tactile Parameter Sliders & Quick Presets
            GlassCard {
                Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
                    Text(
                        text = "Customize Financial Parameters",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    // A. Loan / Investment Amount
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (perspective == SimulatorPerspective.BORROWER) "Loan Amount" else "Capital to Invest",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = BrandIceBlue,
                                border = BorderStroke(1.dp, BrandIceBorder)
                            ) {
                                Text(
                                    text = principalDouble.toInrString(),
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    fontWeight = FontWeight.Bold,
                                    color = BrandRoyalBlue,
                                    style = MaterialTheme.typography.titleSmall
                                )
                            }
                        }

                        Spacer(Modifier.height(8.dp))

                        // Direct Editable Numeric Input (Full-Width, Self-Filled)
                        OutlinedTextField(
                            value = if (principal == 0f) "" else principal.toInt().toString(),
                            onValueChange = { str ->
                                val num = str.filter { it.isDigit() }.toFloatOrNull() ?: 0f
                                principal = num.coerceIn(0f, 10000000f)
                            },
                            prefix = { Text("₹ ", fontWeight = FontWeight.Bold, color = BrandRoyalBlue) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            textStyle = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            shape = RoundedCornerShape(12.dp),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )

                        Spacer(Modifier.height(10.dp))

                        // Quick Preset Chips for Amount
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf(
                                25000f to "₹25k",
                                50000f to "₹50k",
                                100000f to "₹1L",
                                250000f to "₹2.5L",
                                500000f to "₹5L"
                            ).forEach { (amt, label) ->
                                val selected = (principal - amt).let { it >= -500f && it <= 500f }
                                SuggestionChip(
                                    onClick = { principal = amt },
                                    label = {
                                        Text(
                                            text = label,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                                        )
                                    },
                                    colors = SuggestionChipDefaults.suggestionChipColors(
                                        containerColor = if (selected) BrandRoyalBlue else MaterialTheme.colorScheme.surface,
                                        labelColor = if (selected) Color.White else MaterialTheme.colorScheme.onSurface
                                    ),
                                    border = BorderStroke(
                                        1.dp,
                                        if (selected) BrandRoyalBlue else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                    ),
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        Spacer(Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Min: ₹10,000", style = MaterialTheme.typography.bodySmall, color = Gray400, fontSize = 11.sp)
                            Text("Max: ₹1,00,00,000", style = MaterialTheme.typography.bodySmall, color = Gray400, fontSize = 11.sp)
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                    // B. Annual Interest Rate (% p.a.)
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Annual Interest Rate (% p.a.)",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = GoldCoinCream,
                                border = BorderStroke(1.dp, GoldCoinBorder)
                            ) {
                                Text(
                                    text = "${String.format(java.util.Locale.getDefault(), "%.1f", rateDouble)}% p.a.",
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    fontWeight = FontWeight.Bold,
                                    color = GoldCoinAmber,
                                    style = MaterialTheme.typography.titleSmall
                                )
                            }
                        }

                        Spacer(Modifier.height(8.dp))

                        // Direct Editable Numeric Input (Full-Width, Self-Filled)
                        OutlinedTextField(
                            value = if (interestRate == 0f) "" else String.format(java.util.Locale.US, "%.1f", interestRate),
                            onValueChange = { str ->
                                val num = str.toFloatOrNull() ?: 0f
                                interestRate = num.coerceIn(0.5f, 36.0f)
                            },
                            suffix = { Text("% p.a.", fontWeight = FontWeight.Bold, color = GoldCoinAmber) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            textStyle = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            shape = RoundedCornerShape(12.dp),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                        )

                        Spacer(Modifier.height(10.dp))

                        // Quick Rate Presets
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf(
                                8.5f to "8.5%",
                                12.0f to "12%",
                                15.0f to "15%",
                                18.0f to "18%",
                                24.0f to "24%"
                            ).forEach { (rt, label) ->
                                val selected = (interestRate - rt).let { it >= -0.2f && it <= 0.2f }
                                SuggestionChip(
                                    onClick = { interestRate = rt },
                                    label = {
                                        Text(
                                            text = label,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                                        )
                                    },
                                    colors = SuggestionChipDefaults.suggestionChipColors(
                                        containerColor = if (selected) GoldCoinRich else MaterialTheme.colorScheme.surface,
                                        labelColor = if (selected) Color.White else MaterialTheme.colorScheme.onSurface
                                    ),
                                    border = BorderStroke(
                                        1.dp,
                                        if (selected) GoldCoinRich else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                    ),
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        Spacer(Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("1.0% Prime Rate", style = MaterialTheme.typography.bodySmall, color = Gray400, fontSize = 11.sp)
                            Text("36.0% Maximum Cap", style = MaterialTheme.typography.bodySmall, color = Gray400, fontSize = 11.sp)
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                    // C. Tenure Duration
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Tenure Duration",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = EmeraldLight,
                                border = BorderStroke(1.dp, Emerald400.copy(alpha = 0.4f))
                            ) {
                                val years = tenureInt / 12f
                                val yearText = if (tenureInt % 12 == 0) "${tenureInt / 12} Yr" else "${String.format(java.util.Locale.getDefault(), "%.1f", years)} Yrs"
                                Text(
                                    text = "$tenureInt Months ($yearText)",
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    fontWeight = FontWeight.Bold,
                                    color = Emerald600,
                                    style = MaterialTheme.typography.titleSmall
                                )
                            }
                        }

                        Spacer(Modifier.height(8.dp))

                        // Direct Editable Numeric Input (Full-Width, Self-Filled)
                        OutlinedTextField(
                            value = if (tenureMonths == 0) "" else tenureMonths.toString(),
                            onValueChange = { str ->
                                val num = str.filter { it.isDigit() }.toIntOrNull() ?: 1
                                tenureMonths = num.coerceIn(1, 120)
                            },
                            suffix = { Text("Months", fontWeight = FontWeight.Bold, color = Emerald600) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            textStyle = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            shape = RoundedCornerShape(12.dp),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )

                        Spacer(Modifier.height(10.dp))

                        // Quick Tenure Presets
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf(
                                3 to "3M",
                                6 to "6M",
                                12 to "12M",
                                24 to "24M",
                                36 to "36M",
                                60 to "60M"
                            ).forEach { (t, label) ->
                                val selected = tenureMonths == t
                                SuggestionChip(
                                    onClick = { tenureMonths = t },
                                    label = {
                                        Text(
                                            text = label,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                                        )
                                    },
                                    colors = SuggestionChipDefaults.suggestionChipColors(
                                        containerColor = if (selected) Emerald500 else MaterialTheme.colorScheme.surface,
                                        labelColor = if (selected) Color.White else MaterialTheme.colorScheme.onSurface
                                    ),
                                    border = BorderStroke(
                                        1.dp,
                                        if (selected) Emerald500 else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                    ),
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        Spacer(Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("1 Month", style = MaterialTheme.typography.bodySmall, color = Gray400, fontSize = 11.sp)
                            Text("120 Months (10 Yrs)", style = MaterialTheme.typography.bodySmall, color = Gray400, fontSize = 11.sp)
                        }
                    }
                }
            }

            // 5. Month-by-Month Amortization Schedule (Collapsible)
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showAmortization = !showAmortization },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = BrandIceBlue,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.TableChart,
                                        contentDescription = null,
                                        tint = BrandRoyalBlue,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Amortization Breakdown",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Monthly principal reduction & interest split",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        IconButton(onClick = { showAmortization = !showAmortization }) {
                            Icon(
                                imageVector = if (showAmortization) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = if (showAmortization) "Collapse" else "Expand",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    AnimatedVisibility(
                        visible = showAmortization,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        Column(modifier = Modifier.padding(top = 16.dp)) {
                            // Payoff Ratio Progress Visualizer
                            val principalRatio = if (totalPayment > 0) (principalDouble / totalPayment).toFloat() else 1f
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Principal Payoff Ratio",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "${(principalRatio * 100).roundToInt()}% Principal • ${(100 - (principalRatio * 100).roundToInt())}% Interest",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                    Spacer(Modifier.height(6.dp))
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(8.dp)
                                            .clip(RoundedCornerShape(4.dp))
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .weight(principalRatio.coerceAtLeast(0.01f))
                                                .fillMaxHeight()
                                                .background(Emerald500)
                                        )
                                        Box(
                                            modifier = Modifier
                                                .weight((1f - principalRatio).coerceAtLeast(0.01f))
                                                .fillMaxHeight()
                                                .background(GoldCoinAmber)
                                        )
                                    }
                                }
                            }

                            Spacer(Modifier.height(12.dp))

                            // Table Header
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 8.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Month", modifier = Modifier.weight(0.8f), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
                                    Text("Principal", modifier = Modifier.weight(1.3f), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.End)
                                    Text("Interest", modifier = Modifier.weight(1.2f), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.End)
                                    Text("Balance", modifier = Modifier.weight(1.4f), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.End)
                                }
                            }

                            Spacer(Modifier.height(6.dp))

                            // Rows Preview (First 5 months)
                            val previewRows = amortizationRows.take(5)
                            previewRows.forEachIndexed { idx, row ->
                                val isEven = idx % 2 == 0
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (isEven) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f) else Color.Transparent,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 8.dp, vertical = 7.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = BrandIceBlue,
                                            border = BorderStroke(0.5.dp, BrandIceBorder)
                                        ) {
                                            Text(
                                                text = "M${row.month}",
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = BrandRoyalBlue,
                                                fontSize = 10.sp
                                            )
                                        }
                                        Text(
                                            text = row.principalPaid.toInrString(),
                                            modifier = Modifier.weight(1.3f),
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Emerald600,
                                            textAlign = TextAlign.End
                                        )
                                        Text(
                                            text = row.interestPaid.toInrString(),
                                            modifier = Modifier.weight(1.2f),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = GoldCoinAmber,
                                            fontWeight = FontWeight.SemiBold,
                                            textAlign = TextAlign.End
                                        )
                                        Text(
                                            text = row.remainingBalance.toInrString(),
                                            modifier = Modifier.weight(1.4f),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            fontWeight = FontWeight.Medium,
                                            textAlign = TextAlign.End
                                        )
                                    }
                                }
                            }

                            Spacer(Modifier.height(10.dp))

                            // Dedicated Action Button to launch full interactive amortization sheet
                            OutlinedButton(
                                onClick = { showFullAmortization = true },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 44.dp),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, BrandCobalt.copy(alpha = 0.5f)),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CalendarMonth,
                                    contentDescription = null,
                                    tint = BrandRoyalBlue,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = "View Full ${amortizationRows.size}-Month Schedule ➔",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = BrandRoyalBlue
                                )
                            }
                        }
                    }
                }
            }

            // 6. Direct Action Handoffs (Borrower vs Lender CTA Launchpads)
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (perspective == SimulatorPerspective.BORROWER) {
                    // Borrower Primary: Request Loan with simulated terms
                    Button(
                        onClick = { onRequestLoan(principalDouble, rateDouble, tenureInt) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = BrandRoyalBlue)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "Request Loan with These Terms",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleSmall
                        )
                    }

                    // Borrower Secondary: Post on Community Wall
                    OutlinedButton(
                        onClick = { onPostOffer(principalDouble, rateDouble, tenureInt) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, BrandCobalt),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        Icon(Icons.Default.TrendingUp, contentDescription = null, tint = BrandRoyalBlue, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "Publish Loan Request on Community Wall",
                            fontWeight = FontWeight.SemiBold,
                            color = BrandRoyalBlue,
                            style = MaterialTheme.typography.titleSmall
                        )
                    }
                } else {
                    // Lender Primary: Post Offer on Community Wall
                    Button(
                        onClick = { onPostOffer(principalDouble, rateDouble, tenureInt) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 50.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Emerald500),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        Icon(Icons.Default.TrendingUp, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "Post Capital Offer on Community Wall",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleSmall
                        )
                    }

                    // Lender Secondary: Create Direct Loan Agreement (Clean, unclipped, responsive)
                    OutlinedButton(
                        onClick = { onRequestLoan(principalDouble, rateDouble, tenureInt) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, Emerald500),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        Icon(Icons.Default.AccountBalance, contentDescription = null, tint = Emerald600, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "Create Direct Loan Agreement",
                            fontWeight = FontWeight.SemiBold,
                            color = Emerald600,
                            style = MaterialTheme.typography.titleSmall
                        )
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
        }

        // Full Amortization Schedule Bottom Sheet
        if (showFullAmortization) {
            AmortizationScheduleBottomSheet(
                amortizationRows = amortizationRows,
                principal = principalDouble,
                totalInterest = totalInterest,
                totalPayment = totalPayment,
                tenureMonths = tenureInt,
                onDismiss = { showFullAmortization = false }
            )
        }
    }
}

/**
 * Dedicated Full Amortization Bottom Sheet with interactive filter tabs and detailed schedule.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AmortizationScheduleBottomSheet(
    amortizationRows: List<AmortizationScheduleRow>,
    principal: Double,
    totalInterest: Double,
    totalPayment: Double,
    tenureMonths: Int,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .padding(horizontal = 16.dp)
                .padding(bottom = 16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Amortization Schedule",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "$tenureMonths Months Complete Payoff Journey",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = BrandIceBlue,
                    border = BorderStroke(1.dp, BrandIceBorder)
                ) {
                    Text(
                        text = "$tenureMonths Months",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = BrandRoyalBlue
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            // Executive Summary Metric Strip
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Principal", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(principal.toInrString(), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = Emerald600)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Total Interest", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(totalInterest.toInrString(), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = GoldCoinAmber)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Total Payable", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(totalPayment.toInrString(), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            // Table Header (Fixed)
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Mo", modifier = Modifier.weight(0.7f), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
                    Text("Principal", modifier = Modifier.weight(1.3f), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.End)
                    Text("Interest", modifier = Modifier.weight(1.2f), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.End)
                    Text("Balance", modifier = Modifier.weight(1.4f), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.End)
                }
            }

            Spacer(Modifier.height(4.dp))

            // Full Scrollable Schedule
            androidx.compose.foundation.lazy.LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                items(amortizationRows.size, key = { amortizationRows[it].month }) { idx ->
                    val row = amortizationRows[idx]
                    val isEven = idx % 2 == 0
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (isEven) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f) else Color.Transparent,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 7.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = BrandIceBlue,
                                border = BorderStroke(0.5.dp, BrandIceBorder)
                            ) {
                                Text(
                                    text = "#${row.month}",
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = BrandRoyalBlue,
                                    fontSize = 10.sp
                                )
                            }
                            Text(
                                text = row.principalPaid.toInrString(),
                                modifier = Modifier.weight(1.3f),
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = Emerald600,
                                textAlign = TextAlign.End
                            )
                            Text(
                                text = row.interestPaid.toInrString(),
                                modifier = Modifier.weight(1.2f),
                                style = MaterialTheme.typography.bodySmall,
                                color = GoldCoinAmber,
                                fontWeight = FontWeight.SemiBold,
                                textAlign = TextAlign.End
                            )
                            Text(
                                text = row.remainingBalance.toInrString(),
                                modifier = Modifier.weight(1.4f),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Medium,
                                textAlign = TextAlign.End
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            Button(
                onClick = onDismiss,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("Close Schedule", fontWeight = FontWeight.Bold)
            }
        }
    }
}

