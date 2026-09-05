package com.loanzo.app.ui.marketplace

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.loanzo.app.data.entity.MarketplacePostEntity
import com.loanzo.app.ui.components.*
import com.loanzo.app.ui.theme.*
import com.loanzo.app.util.toFormattedString
import com.loanzo.app.util.toRelativeTime
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import com.loanzo.app.ui.components.ContextualGuideCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MarketplaceFeedScreen(
    state: MarketplaceUiState,
    onTabSelected: (MarketplaceTabFilter) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onCategoryTagSelected: (String) -> Unit,
    onVouchPost: (String) -> Unit,
    onSubmitBid: (postId: String, amount: Double, rate: Double, tenure: Int, message: String) -> Unit,
    onNavigateToCreatePost: (String) -> Unit, // "OFFER_TO_LEND" or "SEEKING_LOAN"
    onNavigateBack: () -> Unit
) {
    var selectedPostForBid by remember { mutableStateOf<MarketplacePostEntity?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    val userRepository = com.loanzo.app.util.LocalUserRepository.current
    val marketplaceGuideSeen by userRepository.isGuideSeen(com.loanzo.app.data.repository.UserRepository.GUIDE_MARKETPLACE_SEEN)
        .collectAsStateWithLifecycle(initialValue = true)
    val scope = rememberCoroutineScope()

    val tabs = listOf("All Offers", "💰 Lenders", "🙋 Borrowers", "⭐ My Posts")
    val selectedTabIndex = when (state.selectedTab) {
        MarketplaceTabFilter.ALL -> 0
        MarketplaceTabFilter.LENDERS -> 1
        MarketplaceTabFilter.BORROWERS -> 2
        MarketplaceTabFilter.MY_POSTS -> 3
    }

    val categories = listOf("ALL", "EDUCATION", "MEDICAL", "BUSINESS", "EMERGENCY", "PERSONAL")

    LaunchedEffect(state.actionSuccessMessage, state.error) {
        state.actionSuccessMessage?.let {
            snackbarHostState.showSnackbar(it)
        }
        state.error?.let {
            snackbarHostState.showSnackbar(it)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "Community Loan Wall",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Emerald400.copy(alpha = 0.15f),
                                border = BorderStroke(1.dp, Emerald400.copy(alpha = 0.3f))
                            ) {
                                Text(
                                    "LIVE",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Emerald400,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            "Verified direct P2P lending opportunities",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { onNavigateToCreatePost("OFFER_TO_LEND") }) {
                        Icon(
                            imageVector = Icons.Default.AddCircleOutline,
                            contentDescription = "Create Post",
                            tint = MaterialTheme.colorScheme.primary
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
        ) {
            // Mode Switcher Tabs
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                SegmentedCapsuleTab(
                    tabs = tabs,
                    selectedIndex = selectedTabIndex,
                    onTabSelected = { idx ->
                        val selected = when (idx) {
                            0 -> MarketplaceTabFilter.ALL
                            1 -> MarketplaceTabFilter.LENDERS
                            2 -> MarketplaceTabFilter.BORROWERS
                            else -> MarketplaceTabFilter.MY_POSTS
                        }
                        onTabSelected(selected)
                    }
                )
            }

            // Embedded Search Bar
            OutlinedTextField(
                value = state.searchQuery,
                onValueChange = onSearchQueryChange,
                placeholder = { Text("Search by purpose, name, city (#Medical, #Education)...", fontSize = 13.sp, color = Gray400) },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = "Search", tint = Gold500, modifier = Modifier.size(20.dp))
                },
                trailingIcon = {
                    if (state.searchQuery.isNotBlank()) {
                        IconButton(onClick = { onSearchQueryChange("") }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear", tint = Gray400, modifier = Modifier.size(18.dp))
                        }
                    }
                },
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                ),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            )

            // Category Filter Pills
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(categories) { cat ->
                    val isSelected = state.selectedCategoryTag.equals(cat, ignoreCase = true)
                    FilterChip(
                        selected = isSelected,
                        onClick = { onCategoryTagSelected(cat) },
                        label = {
                            Text(
                                if (cat == "ALL") "All Categories" else "#$cat",
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = goldFilterChipColors(),
                        border = goldFilterChipBorder(isSelected),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Feed Content List
            if (state.isLoading && state.posts.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Gold500)
                }
            } else if (state.posts.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Outlined.Forum,
                            contentDescription = null,
                            tint = Gray500,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            "No Community Posts Found",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            "Be the first to publish a lending offer or post a loan request!",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { onNavigateToCreatePost("OFFER_TO_LEND") },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Create a Post", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 80.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(state.posts, key = { it.postId }) { post ->
                        SocialPostCard(
                            post = post,
                            onVouch = { onVouchPost(post.postId) },
                            onPrimaryAction = { selectedPostForBid = post }
                        )
                    }
                }
            }
        }
    }

    // Interactive Bid / Proposal Bottom Sheet
    selectedPostForBid?.let { post ->
        BidProposalBottomSheet(
            post = post,
            onDismiss = { selectedPostForBid = null },
            onSubmitBid = { amount, rate, tenure, msg ->
                onSubmitBid(post.postId, amount, rate, tenure, msg)
                selectedPostForBid = null
            }
        )
    }

    if (!marketplaceGuideSeen) {
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            ContextualGuideCard(
                visible = true,
                icon = Icons.Default.Storefront,
                title = "P2P Lending Marketplace",
                body = "Browse verified lender offers, submit bids, or post your own lending offer. Vouch for trusted community posts.",
                onDismiss = {
                    scope.launch {
                        userRepository.markGuideSeen(com.loanzo.app.data.repository.UserRepository.GUIDE_MARKETPLACE_SEEN)
                    }
                },
                autoDismissSeconds = 8
            )
        }
    }
    }
}

/**
 * Rich Social Post Card with Author Header, Verification Badges, Financial Terms, and Social Actions.
 */
@Composable
fun SocialPostCard(
    post: MarketplacePostEntity,
    onVouch: () -> Unit,
    onPrimaryAction: () -> Unit
) {
    val isLenderOffer = post.postType == "OFFER_TO_LEND"
    val accentColor = if (isLenderOffer) Gold500 else Emerald400
    var isExpanded by remember { mutableStateOf(false) }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header: Author Info + Badges
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                val authorPhoto = post.authorAvatarUrl.ifBlank { null }
                if (authorPhoto != null) {
                    coil.compose.AsyncImage(
                        model = authorPhoto,
                        contentDescription = post.authorName,
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .border(1.5.dp, accentColor.copy(alpha = 0.5f), CircleShape),
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(accentColor.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isLenderOffer) Icons.Default.VolunteerActivism else Icons.Default.AccountBalanceWallet,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = post.authorName,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (post.authorKycVerified) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "KYC Verified",
                                tint = Emerald400,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (post.locationCity.isNotBlank()) {
                            Text(
                                "📍 ${post.locationCity}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text("•", color = Gray500, fontSize = 10.sp)
                        }
                        Text(
                            "⭐ ${post.authorTrustScore}/100 Trust",
                            style = MaterialTheme.typography.labelSmall,
                            color = Gold400,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text("•", color = Gray500, fontSize = 10.sp)
                        Text(
                            post.createdAt.toRelativeTime(),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Post Type Pill
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = accentColor.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = if (isLenderOffer) "LENDER OFFER" else "SEEKING LOAN",
                        color = accentColor,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        softWrap = false,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Post Title
            Text(
                text = post.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Pitch Description (expandable)
            Text(
                text = post.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = if (isExpanded) 10 else 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 18.sp,
                modifier = Modifier.clickable { isExpanded = !isExpanded }
            )

            if (post.description.length > 90) {
                Text(
                    text = if (isExpanded) "Show less" else "Read more...",
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .clickable { isExpanded = !isExpanded }
                        .padding(top = 2.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Financial Capsule Card
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1.3f)) {
                        Text(
                            if (isLenderOffer) "CAPITAL POOL" else "AMOUNT NEEDED",
                            fontSize = 9.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            if (isLenderOffer) "₹${post.minAmount.toFormattedString()} - ₹${post.maxAmount.toFormattedString()}"
                            else "₹${post.maxAmount.toFormattedString()}",
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Column(modifier = Modifier.weight(0.9f), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("INTEREST", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.SemiBold, maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis)
                        Text(
                            "${post.interestRate}% p.a.",
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = accentColor,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                        Text("TENURE", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.SemiBold, maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis)
                        Text(
                            "${post.tenureMonths} Mo",
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            if (post.collateralOffered.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Security, contentDescription = null, tint = Blue400, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        "Security / Proof: ${post.collateralOffered}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Footer: Social Counters + Primary CTA
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Vouch counter
                Surface(
                    onClick = onVouch,
                    shape = RoundedCornerShape(10.dp),
                    color = Red400.copy(alpha = 0.12f)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.Favorite, contentDescription = "Vouch", tint = Red400, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            "Vouch (${post.vouchCount})",
                            color = Red400,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Inquiries / Bids counter
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Blue400.copy(alpha = 0.12f)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.Bolt, contentDescription = "Bids", tint = Blue400, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            "${post.bidsCount} Offers",
                            color = Blue400,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Primary CTA Button
                Button(
                    onClick = onPrimaryAction,
                    colors = ButtonDefaults.buttonColors(containerColor = accentColor, contentColor = Navy900),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = if (isLenderOffer) "Apply Now ➔" else "Fund / Bid ➔",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

/**
 * Interactive Bidding / Proposal Bottom Sheet.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BidProposalBottomSheet(
    post: MarketplacePostEntity,
    isKycCompleted: Boolean = true,
    onNavigateToKyc: () -> Unit = {},
    onDismiss: () -> Unit,
    onSubmitBid: (amount: Double, rate: Double, tenure: Int, message: String) -> Unit
) {
    val isLenderOffer = post.postType == "OFFER_TO_LEND"
    var proposedAmount by remember { mutableStateOf(post.minAmount.toString()) }
    var proposedRate by remember { mutableStateOf(post.interestRate.toString()) }
    var proposedTenure by remember { mutableStateOf(post.tenureMonths.toString()) }
    var message by remember { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 36.dp)
        ) {
            Text(
                text = if (isLenderOffer) "Apply for Loan Capital" else "Submit Funding Offer / Bid",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Target Post: ${post.title}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Proposed Amount
            OutlinedTextField(
                value = proposedAmount,
                onValueChange = { proposedAmount = it },
                label = { Text("Proposed Amount (₹)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = proposedRate,
                    onValueChange = { proposedRate = it },
                    label = { Text("Interest Rate (%)") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = proposedTenure,
                    onValueChange = { proposedTenure = it },
                    label = { Text("Tenure (Months)") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = message,
                onValueChange = { message = it },
                label = { Text("Note / Repayment terms") },
                placeholder = { Text("e.g. Can clear via monthly UPI transfer on the 5th") },
                maxLines = 3,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = {
                    if (!isKycCompleted) {
                        onDismiss()
                        onNavigateToKyc()
                        return@Button
                    }
                    val amount = proposedAmount.toDoubleOrNull() ?: post.minAmount
                    val rate = proposedRate.toDoubleOrNull() ?: post.interestRate
                    val tenure = proposedTenure.toIntOrNull() ?: post.tenureMonths
                    onSubmitBid(amount, rate, tenure, message)
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (!isKycCompleted) Red400 else (if (isLenderOffer) Gold500 else Emerald400),
                    contentColor = if (!isKycCompleted) Color.White else Navy900
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Text(
                    text = if (!isKycCompleted) {
                        "Complete KYC to ${if (isLenderOffer) "Apply" else "Propose"} ➔"
                    } else if (isLenderOffer) {
                        "Submit Loan Application ➔"
                    } else {
                        "Submit Funding Proposal ➔"
                    },
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }
        }
    }
}
