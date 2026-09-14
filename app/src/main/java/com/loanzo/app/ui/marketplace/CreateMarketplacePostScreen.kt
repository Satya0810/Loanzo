package com.loanzo.app.ui.marketplace

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import kotlinx.coroutines.launch
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import com.loanzo.app.ui.components.LoanzoText as Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.loanzo.app.ui.components.SegmentedCapsuleTab
import com.loanzo.app.ui.components.UserPickerDropdown
import com.loanzo.app.ui.theme.*
import com.loanzo.app.util.calculateEMI
import java.text.NumberFormat
import java.util.Locale

private data class CategoryOption(val key: String, val label: String, val iconText: String)

private val PURPOSE_CATEGORIES = listOf(
    CategoryOption("EDUCATION", "Education", "🎓"),
    CategoryOption("MEDICAL", "Medical", "🏥"),
    CategoryOption("BUSINESS", "Business", "💼"),
    CategoryOption("EMERGENCY", "Emergency", "🚨"),
    CategoryOption("PERSONAL", "Personal", "🏠"),
    CategoryOption("AGRICULTURE", "Agriculture", "🌾")
)

private val COLLATERAL_OPTIONS = listOf(
    "Unsecured / Trust",
    "Gadget / Electronics",
    "Vehicle RC / Title",
    "Gold / Jewelry",
    "Salary Slip / ITR",
    "Invoice / Receivables"
)

private fun formatInr(amount: Double): String {
    return try {
        val format = NumberFormat.getNumberInstance(Locale("en", "IN"))
        format.maximumFractionDigits = 0
        "₹" + format.format(amount.coerceAtLeast(0.0))
    } catch (_: Exception) {
        "₹" + amount.toInt().toString()
    }
}

private fun resolveFileName(context: Context, uri: Uri): String {
    var name = uri.lastPathSegment ?: "document"
    try {
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (index != -1 && cursor.moveToFirst()) {
                name = cursor.getString(index)
            }
        }
    } catch (_: Exception) {}
    return name
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateMarketplacePostScreen(
    initialMode: String = "OFFER_TO_LEND",
    isKycCompleted: Boolean = true,
    onNavigateToKyc: () -> Unit = {},
    onEnhancePitch: (suspend (draft: String, category: String, amount: Double, tenure: Int) -> com.loanzo.app.data.ai.AiRaceResult)? = null,
    onPublish: (
        title: String,
        description: String,
        postType: String,
        minAmount: Double,
        maxAmount: Double,
        interestRate: Double,
        tenureMonths: Int,
        purposeCategory: String,
        locationCity: String,
        collateralOffered: String,
        coBorrowerName: String,
        coBorrowerRelationship: String
    ) -> Unit,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val userRepository = com.loanzo.app.util.LocalUserRepository.current

    var isEnhancingPitch by remember { mutableStateOf(false) }
    var pitchAiWinnerBadge by remember { mutableStateOf<String?>(null) }

    var postType by remember { mutableStateOf(if (initialMode == "SEEKING_LOAN") "SEEKING_LOAN" else "OFFER_TO_LEND") }
    val isLenderOffer = postType == "OFFER_TO_LEND"
    val accentColor = if (isLenderOffer) Gold500 else Emerald400
    val secondaryAccent = if (isLenderOffer) BrandAmberGold else Emerald500

    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var minAmountText by remember { mutableStateOf(if (isLenderOffer) "25000" else "40000") }
    var maxAmountText by remember { mutableStateOf(if (isLenderOffer) "150000" else "40000") }
    var interestRate by remember { mutableStateOf(if (isLenderOffer) 10.0f else 11.0f) }
    var selectedTenure by remember { mutableIntStateOf(6) }
    var selectedCategory by remember { mutableStateOf("EDUCATION") }
    var locationCity by remember { mutableStateOf("Bengaluru") }
    var selectedCollateralType by remember { mutableStateOf("Unsecured / Trust") }
    var collateralOffered by remember { mutableStateOf("") }
    var attachedProofUri by remember { mutableStateOf<Uri?>(null) }
    var attachedProofName by remember { mutableStateOf<String?>(null) }

    var coBorrowerName by remember { mutableStateOf("") }
    var coBorrowerRelationship by remember { mutableStateOf("") }

    var showValidationErrors by remember { mutableStateOf(false) }
    var isPublishing by remember { mutableStateOf(false) }

    val tenures = listOf(3, 6, 12, 18, 24, 36)

    // Document / Proof Picker Launcher
    val proofPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            attachedProofUri = uri
            attachedProofName = resolveFileName(context, uri)
        }
    }

    // Numerical Calculations for Live EMI / Yield Amortization
    val parsedMin = minAmountText.toDoubleOrNull() ?: (if (isLenderOffer) 25000.0 else 40000.0)
    val parsedMax = maxAmountText.toDoubleOrNull() ?: (if (isLenderOffer) 150000.0 else 40000.0)
    val principalAmount = if (isLenderOffer) parsedMax else parsedMax
    val liveEmi = remember(principalAmount, interestRate, selectedTenure) {
        calculateEMI(principalAmount, interestRate.toDouble(), selectedTenure)
    }
    val totalRepayment = remember(liveEmi, selectedTenure) {
        liveEmi * selectedTenure
    }
    val totalInterest = remember(totalRepayment, principalAmount) {
        (totalRepayment - principalAmount).coerceAtLeast(0.0)
    }
    val principalRatio = remember(principalAmount, totalRepayment) {
        if (totalRepayment > 0) (principalAmount / totalRepayment).toFloat().coerceIn(0f, 1f) else 1f
    }

    // Validations
    val isTitleValid = title.trim().length >= 5
    val isDescriptionValid = description.trim().length >= 15
    val isAmountValid = if (isLenderOffer) {
        val min = minAmountText.toDoubleOrNull() ?: 0.0
        val max = maxAmountText.toDoubleOrNull() ?: 0.0
        min >= 1000.0 && max >= min && max <= 5000000.0
    } else {
        val amt = maxAmountText.toDoubleOrNull() ?: 0.0
        amt in 1000.0..5000000.0
    }
    val isCityValid = locationCity.trim().isNotBlank()
    val isCoBorrowerValid = if (coBorrowerName.isNotBlank()) coBorrowerRelationship.isNotBlank() else true
    val isFormValid = isTitleValid && isDescriptionValid && isAmountValid && isCityValid && isCoBorrowerValid

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = if (isLenderOffer) "Publish Lending Offer" else "Publish Loan Request",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Community P2P Lending Wall",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                actions = {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = accentColor.copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.4f)),
                        modifier = Modifier.padding(end = 12.dp)
                    ) {
                        Text(
                            text = if (isLenderOffer) "💰 LENDER" else "🙋 SEEKER",
                            color = accentColor,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Segmented Switcher (Lender vs Borrower)
            SegmentedCapsuleTab(
                tabs = listOf("💰 Offer Capital (Lend)", "🙋 Request Loan (Borrow)"),
                selectedIndex = if (isLenderOffer) 0 else 1,
                onTabSelected = { idx ->
                    postType = if (idx == 0) "OFFER_TO_LEND" else "SEEKING_LOAN"
                    showValidationErrors = false
                }
            )

            Spacer(modifier = Modifier.height(20.dp))

            // ══════════════════════════════════════════════════════════════════
            // Section 1: Headline & Narrative
            // ══════════════════════════════════════════════════════════════════
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = CircleShape,
                    color = accentColor.copy(alpha = 0.15f),
                    modifier = Modifier.size(24.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("1", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = accentColor)
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Post Headline & Story",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = accentColor
                )
            }
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Catchy Headline *") },
                placeholder = {
                    Text(
                        if (isLenderOffer) "e.g. Capital pool for verified tech students"
                        else "e.g. Urgent tuition fee assistance for final semester"
                    )
                },
                singleLine = true,
                isError = showValidationErrors && !isTitleValid,
                supportingText = {
                    if (showValidationErrors && !isTitleValid) {
                        Text("Title must be at least 5 characters", color = MaterialTheme.colorScheme.error)
                    } else {
                        Text("${title.length}/60 chars")
                    }
                },
                trailingIcon = {
                    if (showValidationErrors && !isTitleValid) {
                        Icon(Icons.Default.ErrorOutline, contentDescription = "Error", tint = MaterialTheme.colorScheme.error)
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Detailed Narrative & Terms *",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (onEnhancePitch != null) {
                    FilledTonalButton(
                        onClick = {
                            if (!isEnhancingPitch) {
                                coroutineScope.launch {
                                    isEnhancingPitch = true
                                    val draft = description.ifBlank { title.ifBlank { "Loan request for $selectedCategory" } }
                                    val result = onEnhancePitch(draft, selectedCategory, parsedMax, selectedTenure)
                                    when (result) {
                                        is com.loanzo.app.data.ai.AiRaceResult.Success -> {
                                            description = result.content
                                            pitchAiWinnerBadge = "⚡ Enhanced via ${result.providerName} in ${result.latencyMs}ms"
                                        }
                                        is com.loanzo.app.data.ai.AiRaceResult.Failure -> {
                                            description = result.fallbackContent
                                            pitchAiWinnerBadge = "ℹ️ Offline Heuristic Rulebook"
                                        }
                                    }
                                    isEnhancingPitch = false
                                }
                            }
                        },
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = accentColor.copy(alpha = 0.15f),
                            contentColor = secondaryAccent
                        ),
                        modifier = Modifier.height(32.dp)
                    ) {
                        if (isEnhancingPitch) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(12.dp),
                                strokeWidth = 2.dp,
                                color = secondaryAccent
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Enhancing with AI...", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        } else {
                            Icon(
                                Icons.Default.AutoAwesome,
                                contentDescription = "AI Enhance",
                                modifier = Modifier.size(14.dp),
                                tint = secondaryAccent
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("✨ AI Enhance", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                placeholder = {
                    Text(
                        if (isLenderOffer) "Describe your lending terms, preferred causes, and documentation required..."
                        else "Explain why you need the loan, your repayment capacity, and work details..."
                    )
                },
                minLines = 3,
                maxLines = 6,
                isError = showValidationErrors && !isDescriptionValid,
                supportingText = {
                    if (showValidationErrors && !isDescriptionValid) {
                        Text("Please provide at least 15 characters to explain your request", color = MaterialTheme.colorScheme.error)
                    } else {
                        Text("${description.length} chars (minimum 15)")
                    }
                },
                trailingIcon = {
                    if (showValidationErrors && !isDescriptionValid) {
                        Icon(Icons.Default.ErrorOutline, contentDescription = "Error", tint = MaterialTheme.colorScheme.error)
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )

            AnimatedVisibility(visible = pitchAiWinnerBadge != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Emerald500.copy(alpha = 0.12f),
                        border = BorderStroke(1.dp, Emerald500.copy(alpha = 0.3f))
                    ) {
                        Text(
                            text = pitchAiWinnerBadge ?: "",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = Emerald500,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                    Text(
                        text = "Dismiss",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.clickable { pitchAiWinnerBadge = null }
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // ══════════════════════════════════════════════════════════════════
            // Section 2: Financial Terms
            // ══════════════════════════════════════════════════════════════════
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = CircleShape,
                    color = accentColor.copy(alpha = 0.15f),
                    modifier = Modifier.size(24.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("2", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = accentColor)
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Financial Terms & Duration",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = accentColor
                )
            }
            Spacer(modifier = Modifier.height(8.dp))

            if (isLenderOffer) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = minAmountText,
                        onValueChange = { minAmountText = it.filter { char -> char.isDigit() } },
                        label = { Text("Min Amount (₹)") },
                        prefix = { Text("₹ ", fontWeight = FontWeight.SemiBold) },
                        singleLine = true,
                        isError = showValidationErrors && !isAmountValid,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = maxAmountText,
                        onValueChange = { maxAmountText = it.filter { char -> char.isDigit() } },
                        label = { Text("Max Pool (₹)") },
                        prefix = { Text("₹ ", fontWeight = FontWeight.SemiBold) },
                        singleLine = true,
                        isError = showValidationErrors && !isAmountValid,
                        modifier = Modifier.weight(1f)
                    )
                }
            } else {
                OutlinedTextField(
                    value = maxAmountText,
                    onValueChange = {
                        val cleaned = it.filter { char -> char.isDigit() }
                        maxAmountText = cleaned
                        minAmountText = cleaned
                    },
                    label = { Text("Loan Amount Required (₹) *") },
                    prefix = { Text("₹ ", fontWeight = FontWeight.SemiBold) },
                    singleLine = true,
                    isError = showValidationErrors && !isAmountValid,
                    supportingText = {
                        if (showValidationErrors && !isAmountValid) {
                            Text("Amount must be between ₹1,000 and ₹50,00,000", color = MaterialTheme.colorScheme.error)
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Interest Rate Slider
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Target Interest Rate: ${String.format("%.1f", interestRate)}% p.a.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold
                )
                val rateBadgeText = when {
                    interestRate < 10.0f -> "Low APR 🌟"
                    interestRate <= 15.0f -> "Fair Rate 👍"
                    else -> "High Yield 🔥"
                }
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = accentColor.copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, accentColor.copy(alpha = 0.25f))
                ) {
                    Text(
                        rateBadgeText,
                        color = secondaryAccent,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }
            Slider(
                value = interestRate,
                onValueChange = { interestRate = it },
                valueRange = 6.0f..24.0f,
                steps = 35,
                colors = SliderDefaults.colors(
                    thumbColor = accentColor,
                    activeTrackColor = accentColor
                )
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Tenure Selector Chips
            Text(
                "Tenure Duration (Months)",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(6.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(tenures) { months ->
                    val isSelected = selectedTenure == months
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedTenure = months },
                        label = { Text("${months}M", fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = accentColor,
                            selectedLabelColor = Navy900
                        ),
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ══════════════════════════════════════════════════════════════════
            // Live EMI & Financial Breakdown Preview Card
            // ══════════════════════════════════════════════════════════════════
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = if (isLenderOffer) GoldLight.copy(alpha = 0.7f) else EmeraldLight.copy(alpha = 0.7f),
                border = BorderStroke(1.dp, accentColor.copy(alpha = 0.35f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (isLenderOffer) Icons.AutoMirrored.Filled.TrendingUp else Icons.Default.Calculate,
                                contentDescription = null,
                                tint = secondaryAccent,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                if (isLenderOffer) "Projected Inflow & Yield Amortization" else "Live EMI & Repayment Breakdown",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = TextNavyDark
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = accentColor.copy(alpha = 0.2f)
                        ) {
                            Text(
                                text = "${selectedTenure} Months",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = secondaryAccent,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = if (isLenderOffer) "Est. Monthly Inflow" else "Monthly Equated Installment",
                                fontSize = 11.sp,
                                color = TextSlateMedium
                            )
                            Text(
                                text = formatInr(liveEmi),
                                fontSize = 20.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = secondaryAccent
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = if (isLenderOffer) "Total Net Yield" else "Total Interest Payable",
                                fontSize = 11.sp,
                                color = TextSlateMedium
                            )
                            Text(
                                text = if (isLenderOffer) "+${formatInr(totalInterest)}" else formatInr(totalInterest),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isLenderOffer) Emerald600 else Orange500
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Progress / Distribution Bar (Principal vs Interest)
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Principal: ${formatInr(principalAmount)}",
                                fontSize = 10.sp,
                                color = TextSlateMuted
                            )
                            Text(
                                text = "Total Repay: ${formatInr(totalRepayment)}",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextNavyDark
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(Orange400.copy(alpha = 0.5f))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .fillMaxWidth(principalRatio)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(accentColor)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // ══════════════════════════════════════════════════════════════════
            // Section 3: Category & Community Proximity
            // ══════════════════════════════════════════════════════════════════
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = CircleShape,
                    color = accentColor.copy(alpha = 0.15f),
                    modifier = Modifier.size(24.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("3", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = accentColor)
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Purpose & Proximity",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = accentColor
                )
            }
            Spacer(modifier = Modifier.height(8.dp))

            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(PURPOSE_CATEGORIES) { cat ->
                    val isSelected = selectedCategory == cat.key
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedCategory = cat.key },
                        label = { Text("${cat.iconText} ${cat.label}", fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = accentColor,
                            selectedLabelColor = Navy900
                        ),
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = locationCity,
                onValueChange = { locationCity = it },
                label = { Text("Your City / Proximity *") },
                placeholder = { Text("e.g. Bengaluru, Mumbai, Pune") },
                leadingIcon = {
                    Icon(Icons.Default.LocationOn, contentDescription = null, tint = accentColor)
                },
                singleLine = true,
                isError = showValidationErrors && !isCityValid,
                supportingText = {
                    if (showValidationErrors && !isCityValid) {
                        Text("City is required for community matching", color = MaterialTheme.colorScheme.error)
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(20.dp))

            // ══════════════════════════════════════════════════════════════════
            // Section 4: Collateral & Proof Attachment
            // ══════════════════════════════════════════════════════════════════
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = CircleShape,
                    color = accentColor.copy(alpha = 0.15f),
                    modifier = Modifier.size(24.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("4", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = accentColor)
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Security, Collateral & Document Proof",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = accentColor
                )
            }
            Spacer(modifier = Modifier.height(8.dp))

            // Collateral Asset Category Chips
            Text(
                "Select Security Asset Type (Optional for peer trust)",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(6.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(COLLATERAL_OPTIONS) { opt ->
                    val isSelected = selectedCollateralType == opt
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedCollateralType = opt },
                        label = { Text(opt, fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = accentColor,
                            selectedLabelColor = Navy900
                        ),
                        shape = RoundedCornerShape(8.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = collateralOffered,
                onValueChange = { collateralOffered = it },
                label = { Text("Collateral / Asset Description & Estimated Value") },
                placeholder = { Text("e.g. MacBook Pro M2 (Valued ₹95,000) / 3-Month Payslip") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Document / Photo Proof Picker Tile
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AttachFile, contentDescription = null, tint = accentColor, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    "Attach Collateral Photo / Income Proof",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    "Supports JPG, PNG, or PDF proof",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        OutlinedButton(
                            onClick = { proofPickerLauncher.launch("*/*") },
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (attachedProofUri == null) "Browse" else "Change", fontSize = 12.sp)
                        }
                    }

                    // Attached File Preview
                    if (attachedProofUri != null) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Emerald400.copy(alpha = 0.12f),
                            border = BorderStroke(1.dp, Emerald400.copy(alpha = 0.35f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    if (attachedProofName?.endsWith(".jpg", true) == true ||
                                        attachedProofName?.endsWith(".png", true) == true ||
                                        attachedProofName?.endsWith(".jpeg", true) == true
                                    ) {
                                        AsyncImage(
                                            model = attachedProofUri,
                                            contentDescription = "Attachment preview",
                                            modifier = Modifier
                                                .size(36.dp)
                                                .clip(RoundedCornerShape(6.dp)),
                                            contentScale = ContentScale.Crop
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.Default.Description,
                                            contentDescription = null,
                                            tint = Emerald500,
                                            modifier = Modifier.size(28.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = attachedProofName ?: "Attached Document",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1
                                        )
                                        Text(
                                            text = "Ready for secure document vault upload",
                                            fontSize = 10.sp,
                                            color = Emerald600
                                        )
                                    }
                                }

                                IconButton(
                                    onClick = {
                                        attachedProofUri = null
                                        attachedProofName = null
                                    },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Remove",
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // ══════════════════════════════════════════════════════════════════
            // Section 5: Co-Borrower (Borrower mode only)
            // ══════════════════════════════════════════════════════════════════
            if (!isLenderOffer) {
                Spacer(modifier = Modifier.height(18.dp))
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFFFAF5FF),
                    border = BorderStroke(1.dp, Color(0xFFE9D5FF)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Group, null, tint = Color(0xFF9333EA), modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                "Add Co-Borrower (Optional • Boosts Approval)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = Color(0xFF581C87)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        UserPickerDropdown(
                            selectedUserId = "",
                            onUserSelected = { user ->
                                coBorrowerName = "${user.name} (@${user.username})"
                            },
                            label = "Pick Registered Co-Borrower (Optional)",
                            placeholder = "Search @username or name e.g. Dr. Rohan Patil...",
                            preferredRole = "BORROWER",
                            candidateUsers = emptyList(),
                            onSearchOnline = { query -> userRepository.searchUsersOnline(query) }
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = coBorrowerName,
                            onValueChange = { coBorrowerName = it },
                            label = { Text("Co-Borrower Full Name") },
                            placeholder = { Text("e.g. Priya Mehra") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = coBorrowerRelationship,
                            onValueChange = { coBorrowerRelationship = it },
                            label = { Text("Relationship to Seeker") },
                            placeholder = { Text("e.g. Spouse / Brother / Business Partner") },
                            isError = showValidationErrors && !isCoBorrowerValid,
                            supportingText = {
                                if (showValidationErrors && !isCoBorrowerValid) {
                                    Text("Please specify your relationship with the co-borrower", color = MaterialTheme.colorScheme.error)
                                }
                            },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Validation Error Banner
            AnimatedVisibility(
                visible = showValidationErrors && !isFormValid,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.errorContainer,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.ErrorOutline,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Please review the highlighted fields before broadcasting your post.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // Publish Button
            Button(
                onClick = {
                    showValidationErrors = true
                    if (!isFormValid || isPublishing) return@Button
                    isPublishing = true

                    val min = minAmountText.toDoubleOrNull() ?: (if (isLenderOffer) 25000.0 else 40000.0)
                    val max = maxAmountText.toDoubleOrNull() ?: (if (isLenderOffer) 150000.0 else 40000.0)

                    // Format collateral and proof information
                    val finalCollateral = buildString {
                        if (selectedCollateralType != "Unsecured / Trust") {
                            append("[$selectedCollateralType] ")
                        }
                        if (collateralOffered.isNotBlank()) {
                            append(collateralOffered.trim())
                        } else if (selectedCollateralType != "Unsecured / Trust") {
                            append(selectedCollateralType)
                        } else {
                            append("Unsecured Community Post")
                        }
                        if (attachedProofName != null) {
                            append(" (Proof Attached: $attachedProofName)")
                        }
                    }

                    onPublish(
                        if (title.isBlank()) (if (isLenderOffer) "Capital Lending Offer" else "Loan Request") else title.trim(),
                        if (description.isBlank()) "Community peer loan post with transparent terms." else description.trim(),
                        postType,
                        min,
                        max,
                        interestRate.toDouble(),
                        selectedTenure,
                        selectedCategory,
                        locationCity.trim(),
                        finalCollateral,
                        coBorrowerName.trim(),
                        coBorrowerRelationship.trim()
                    )
                },
                enabled = !isPublishing,
                colors = ButtonDefaults.buttonColors(
                    containerColor = accentColor,
                    contentColor = Navy900
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
            ) {
                if (isPublishing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Navy900,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Publishing to Community Wall...", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                } else {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isLenderOffer) "Publish Lending Offer to Wall ➔" else "Publish Loan Request to Wall ➔",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
            }

            if (!isKycCompleted) {
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Emerald400.copy(alpha = 0.10f),
                    border = BorderStroke(1.dp, Emerald400.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = Emerald400, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "⚡ One-Tap Fast-Track Verification active. Publishing will automatically verify your credentials & broadcast directly to the wall.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}
