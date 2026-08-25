package com.loanzo.app.ui.dashboard

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.loanzo.app.data.dao.CategoryBreakdown
import com.loanzo.app.data.dao.DisbursementDao
import com.loanzo.app.data.dao.RepaymentDao
import com.loanzo.app.data.repository.LoanRepository
import com.loanzo.app.data.repository.UserRepository
import com.loanzo.app.ui.components.GlassCard
import com.loanzo.app.ui.theme.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class FinancialHealthState(
    val categories: List<CategoryBreakdown> = emptyList(),
    val healthScore: Int = 0, // 0-100
    val onTimePayments: Int = 0,
    val totalPayments: Int = 0,
    val utilizationRatio: Float = 0f,
    val isLoading: Boolean = true
)

@HiltViewModel
class FinancialHealthViewModel @Inject constructor(
    private val loanRepository: LoanRepository,
    private val disbursementDao: DisbursementDao,
    private val repaymentDao: RepaymentDao,
    private val userRepository: UserRepository
) : ViewModel() {
    private val _state = MutableStateFlow(FinancialHealthState())
    val state: StateFlow<FinancialHealthState> = _state.asStateFlow()

    init { loadData() }

    private fun loadData() {
        viewModelScope.launch {
            val userId = userRepository.getCurrentUserIdSync() ?: return@launch
            
            // Category breakdown
            disbursementDao.getCategoryBreakdownForUser(userId).collect { categories ->
                // Calculate health score components
                val diversification = (categories.size.coerceAtMost(5) * 10) // max 50 pts for diversity
                val score = (diversification + 50).coerceIn(0, 100) // baseline 50 + diversity bonus
                
                _state.update {
                    it.copy(
                        categories = categories,
                        healthScore = score,
                        isLoading = false
                    )
                }
            }
        }
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
                    state.categories.forEach { cat ->
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
                            Text("₹${String.format("%,.0f", cat.total)}", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HealthScoreGauge(score: Int) {
    val animatedScore by animateFloatAsState(
        targetValue = score / 100f,
        animationSpec = tween(1500, easing = FastOutSlowInEasing),
        label = "score"
    )
    val color = when {
        score >= 80 -> Emerald400
        score >= 60 -> Gold500
        score >= 40 -> Orange400
        else -> Red400
    }

    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(160.dp)) {
        Canvas(modifier = Modifier.size(140.dp)) {
            // Background arc
            drawArc(
                color = color.copy(alpha = 0.15f),
                startAngle = 135f,
                sweepAngle = 270f,
                useCenter = false,
                style = Stroke(width = 20f, cap = StrokeCap.Round)
            )
            // Foreground arc
            drawArc(
                color = color,
                startAngle = 135f,
                sweepAngle = 270f * animatedScore,
                useCenter = false,
                style = Stroke(width = 20f, cap = StrokeCap.Round)
            )
        }
        Text(
            "$score",
            style = MaterialTheme.typography.displayMedium,
            fontWeight = FontWeight.Bold,
            color = color
        )
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
