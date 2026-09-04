package com.loanzo.app.ui.marketplace

import androidx.compose.animation.AnimatedVisibility
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
import com.loanzo.app.ui.components.SegmentedCapsuleTab
import com.loanzo.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateMarketplacePostScreen(
    initialMode: String = "OFFER_TO_LEND",
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
        collateralOffered: String
    ) -> Unit,
    onNavigateBack: () -> Unit
) {
    var postType by remember { mutableStateOf(if (initialMode == "SEEKING_LOAN") "SEEKING_LOAN" else "OFFER_TO_LEND") }
    val isLenderOffer = postType == "OFFER_TO_LEND"
    val accentColor = if (isLenderOffer) Gold500 else Emerald400

    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var minAmountText by remember { mutableStateOf(if (isLenderOffer) "25000" else "40000") }
    var maxAmountText by remember { mutableStateOf(if (isLenderOffer) "150000" else "40000") }
    var interestRate by remember { mutableStateOf(if (isLenderOffer) 10.0f else 11.0f) }
    var selectedTenure by remember { mutableIntStateOf(6) }
    var selectedCategory by remember { mutableStateOf("EDUCATION") }
    var locationCity by remember { mutableStateOf("Bengaluru") }
    var collateralOffered by remember { mutableStateOf("") }

    val tenures = listOf(3, 6, 12, 18, 24, 36)
    val categories = listOf("EDUCATION", "MEDICAL", "BUSINESS", "EMERGENCY", "PERSONAL")

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (isLenderOffer) "Publish Lending Offer" else "Post Loan Request",
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SurfaceDark)
            )
        },
        containerColor = Navy900
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Post Type Switcher (Lender vs Borrower)
            SegmentedCapsuleTab(
                tabs = listOf("💰 Offer Capital (Lend)", "🙋 Request Loan (Borrow)"),
                selectedIndex = if (isLenderOffer) 0 else 1,
                onTabSelected = { idx ->
                    postType = if (idx == 0) "OFFER_TO_LEND" else "SEEKING_LOAN"
                }
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Section 1: Headline & Narrative
            Text(
                "1. Post Headline & Story",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = accentColor
            )
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Catchy Headline") },
                placeholder = {
                    Text(
                        if (isLenderOffer) "e.g. Capital pool for verified tech students"
                        else "e.g. Urgent tuition fee assistance for final semester"
                    )
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Detailed Narrative & Terms") },
                placeholder = {
                    Text(
                        if (isLenderOffer) "Describe your lending terms, preferred causes, and documentation required..."
                        else "Explain why you need the loan, your repayment capacity, and work details..."
                    )
                },
                minLines = 3,
                maxLines = 6,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Section 2: Financial Terms
            Text(
                "2. Financial Scope",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = accentColor
            )
            Spacer(modifier = Modifier.height(8.dp))

            if (isLenderOffer) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = minAmountText,
                        onValueChange = { minAmountText = it },
                        label = { Text("Min Amount (₹)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = maxAmountText,
                        onValueChange = { maxAmountText = it },
                        label = { Text("Max Pool (₹)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
            } else {
                OutlinedTextField(
                    value = maxAmountText,
                    onValueChange = { maxAmountText = it; minAmountText = it },
                    label = { Text("Amount Needed (₹)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Interest Rate Slider
            Text(
                "Target Interest Rate: ${String.format("%.1f", interestRate)}% p.a.",
                fontSize = 13.sp,
                color = Color.White,
                fontWeight = FontWeight.SemiBold
            )
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

            Spacer(modifier = Modifier.height(14.dp))

            // Tenure Selector Chips
            Text(
                "Tenure Duration (Months)",
                fontSize = 13.sp,
                color = Color.White,
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

            Spacer(modifier = Modifier.height(20.dp))

            // Section 3: Category & Security
            Text(
                "3. Category & Trust Proof",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = accentColor
            )
            Spacer(modifier = Modifier.height(8.dp))

            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(categories) { cat ->
                    val isSelected = selectedCategory == cat
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedCategory = cat },
                        label = { Text(cat, fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = accentColor,
                            selectedLabelColor = Navy900
                        ),
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = locationCity,
                onValueChange = { locationCity = it },
                label = { Text("Your City / Location") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = collateralOffered,
                onValueChange = { collateralOffered = it },
                label = { Text("Security / Collateral / Eligibility Proof") },
                placeholder = { Text("e.g. 3-month salary slip / Gadget pledge / DigiLocker Aadhaar") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Publish Button
            Button(
                onClick = {
                    val min = minAmountText.toDoubleOrNull() ?: 25000.0
                    val max = maxAmountText.toDoubleOrNull() ?: 50000.0
                    onPublish(
                        if (title.isBlank()) (if (isLenderOffer) "Capital Lending Offer" else "Loan Request") else title,
                        if (description.isBlank()) "Community peer loan post with transparent terms." else description,
                        postType,
                        min,
                        max,
                        interestRate.toDouble(),
                        selectedTenure,
                        selectedCategory,
                        locationCity,
                        collateralOffered
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = accentColor, contentColor = Navy900),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isLenderOffer) "Publish Lending Offer to Wall ➔" else "Broadcast Loan Request ➔",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}
