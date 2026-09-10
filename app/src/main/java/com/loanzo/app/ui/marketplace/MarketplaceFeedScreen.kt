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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import coil.compose.AsyncImage
import coil.compose.SubcomposeAsyncImage
import com.loanzo.app.data.entity.MarketplaceBidEntity
import com.loanzo.app.data.entity.MarketplacePostEntity
import com.loanzo.app.data.entity.MarketplaceVouchEntity
import com.loanzo.app.ui.components.*
import com.loanzo.app.ui.theme.*
import com.loanzo.app.util.getCartoonAvatarRes
import com.loanzo.app.util.getEffectiveAvatarUrl
import com.loanzo.app.util.toFormattedString
import com.loanzo.app.util.toRelativeTime
import kotlinx.coroutines.launch

/**
 * P2P Social Community Wall / Feed Screen.
 *
 * Displays direct peer lending offers & borrowing requests with trust scores,
 * verified identities, co-borrower credentials, and social vouches.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MarketplaceFeedScreen(
    state: MarketplaceUiState,
    viewModel: MarketplaceViewModel? = null,
    onTabSelected: (MarketplaceTabFilter) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onCategoryTagSelected: (String) -> Unit,
    onVouchPost: (postId: String, reason: String, note: String) -> Unit,
    onSubmitBid: (postId: String, amount: Double, rate: Double, tenure: Int, message: String) -> Unit,
    onNavigateToCreatePost: (String) -> Unit, // "OFFER_TO_LEND" or "SEEKING_LOAN"
    onNavigateToUserProfile: (String) -> Unit = {},
    onNavigateToChat: (channelId: String, loanId: String?, targetUserId: String?) -> Unit = { _, _, _ -> },
    onNavigateToLoanDetail: (String) -> Unit = {},
    onAcceptBid: (MarketplacePostEntity, MarketplaceBidEntity) -> Unit = { _, _ -> },
    onNavigateBack: () -> Unit
) {
    var selectedPostForBid by remember { mutableStateOf<MarketplacePostEntity?>(null) }
    var activePostForBidsSheet by remember { mutableStateOf<MarketplacePostEntity?>(null) }
    var postToVouch by remember { mutableStateOf<MarketplacePostEntity?>(null) }
    var activeProfileToView by remember { mutableStateOf<UserProfileViewData?>(null) }
    var activePostForVouchers by remember { mutableStateOf<MarketplacePostEntity?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    val userRepository = com.loanzo.app.util.LocalUserRepository.current
    val marketplaceGuideSeen by userRepository.isGuideSeen(com.loanzo.app.data.repository.UserRepository.GUIDE_MARKETPLACE_SEEN)
        .collectAsStateWithLifecycle(initialValue = true)
    val scope = rememberCoroutineScope()

    val tabs = listOf("All Offers", "💼 Lenders", "🤝 Borrowers", "👤 My Posts")
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
                        IconButton(onClick = { onNavigateToCreatePost("SEEKING_LOAN") }) {
                            Icon(
                                imageVector = Icons.Default.AddCircleOutline,
                                contentDescription = "Create Post",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                // Segmented Tabs: All, Lenders, Borrowers, My Posts
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    SegmentedCapsuleTab(
                        tabs = tabs,
                        selectedIndex = selectedTabIndex,
                        onTabSelected = { index ->
                            val filter = when (index) {
                                0 -> MarketplaceTabFilter.ALL
                                1 -> MarketplaceTabFilter.LENDERS
                                2 -> MarketplaceTabFilter.BORROWERS
                                3 -> MarketplaceTabFilter.MY_POSTS
                                else -> MarketplaceTabFilter.ALL
                            }
                            onTabSelected(filter)
                        }
                    )
                }

                // Search Bar + Quick Filter Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = state.searchQuery,
                        onValueChange = onSearchQueryChange,
                        placeholder = { Text("Search by purpose, city, or name...", fontSize = 13.sp) },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Search,
                                contentDescription = "Search",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        trailingIcon = {
                            if (state.searchQuery.isNotEmpty()) {
                                IconButton(onClick = { onSearchQueryChange("") }) {
                                    Icon(Icons.Default.Close, contentDescription = "Clear", modifier = Modifier.size(16.dp))
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                    )
                }

                // Purpose Category Chips (Filter Bar)
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(bottom = 8.dp)
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
                            val isVouched = post.postId in state.vouchedPostIds
                            val isSelf = post.authorId == state.currentUserId
                            val postVouches = state.vouchesByPost[post.postId] ?: emptyList()

                            SocialPostCard(
                                post = post,
                                isVouched = isVouched,
                                isSelf = isSelf,
                                vouchers = postVouches,
                                onAuthorClick = {
                                    onNavigateToUserProfile(post.authorId)
                                },
                                onCoBorrowerClick = {
                                    activeProfileToView = viewModel?.getCoBorrowerProfile(post) ?: UserProfileViewData(
                                        userId = "coborrower_${post.postId}",
                                        name = post.coBorrowerName,
                                        roleTitle = "CO-BORROWER / GUARANTOR",
                                        avatarUrl = post.coBorrowerAvatarUrl,
                                        locationCity = post.locationCity,
                                        trustScore = post.coBorrowerTrustScore,
                                        verificationLevel = "Tier 2: National ID & Income Verified",
                                        verificationTier = 2,
                                        aadhaarVerified = post.coBorrowerKycVerified,
                                        panVerified = post.coBorrowerKycVerified,
                                        relationshipToBorrower = post.coBorrowerRelationship.ifBlank { "Spouse (Co-Signer)" }
                                    )
                                },
                                onVoucherClick = { v ->
                                    activeProfileToView = viewModel?.getVoucherProfile(v) ?: UserProfileViewData(
                                        userId = v.voucherUserId,
                                        name = v.voucherName,
                                        roleTitle = "COMMUNITY ENDORSER (VOUCHER)",
                                        avatarUrl = v.voucherAvatarUrl,
                                        locationCity = "Community Peer",
                                        trustScore = v.voucherTrustScore,
                                        verificationLevel = "Tier 3: Verified Peer Endorser",
                                        verificationTier = 3,
                                        vouchReason = v.vouchReason,
                                        vouchComment = v.comment
                                    )
                                },
                                onViewAllVouchersClick = {
                                    activePostForVouchers = post
                                },
                                onVouch = {
                                    if (isSelf) {
                                        scope.launch {
                                            snackbarHostState.showSnackbar("You cannot vouch for your own post.")
                                        }
                                    } else if (isVouched) {
                                        onVouchPost(post.postId, "", "")
                                    } else {
                                        postToVouch = post
                                    }
                                },
                                onViewBidsClick = { activePostForBidsSheet = post },
                                onPrimaryAction = {
                                    if (isSelf) {
                                        activePostForBidsSheet = post
                                    } else {
                                        selectedPostForBid = post
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }

        // Vouch Reason Selection Dialog
        postToVouch?.let { post ->
            VouchReasonDialog(
                authorName = post.authorName,
                onDismiss = { postToVouch = null },
                onConfirmVouch = { reason, note ->
                    onVouchPost(post.postId, reason, note)
                    postToVouch = null
                }
            )
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

        // Community Proposals & Bids Bottom Sheet (Accept Bids -> Create Loan Workflow)
        activePostForBidsSheet?.let { post ->
            val bids by (viewModel?.getBidsForPostFlow(post.postId) ?: kotlinx.coroutines.flow.flowOf(emptyList()))
                .collectAsStateWithLifecycle(initialValue = emptyList())
            PostDetailAndBidsSheet(
                post = post,
                bids = bids,
                onAcceptBid = { bid ->
                    activePostForBidsSheet = null
                    onAcceptBid(post, bid)
                },
                onDismiss = { activePostForBidsSheet = null }
            )
        }

        // User Profile Inspection Bottom Sheet (Opened by clicking author, co-borrower, or voucher circular avatar!)
        activeProfileToView?.let { profile ->
            UserProfileDetailBottomSheet(
                profile = profile,
                onDismiss = { activeProfileToView = null },
                onConnectClick = {
                    activeProfileToView = null
                    val myId = state.currentUserId
                    val channel = com.loanzo.app.ui.loan.ChatViewModel.getDirectChannelId(myId, profile.userId)
                    onNavigateToChat(channel, null, profile.userId)
                }
            )
        }

        // Community Endorsements / Vouchers Bottom Sheet
        activePostForVouchers?.let { post ->
            PostVouchersBottomSheet(
                post = post,
                vouches = state.vouchesByPost[post.postId] ?: emptyList(),
                onDismiss = { activePostForVouchers = null },
                onVoucherProfileClick = { voucher ->
                    activeProfileToView = viewModel?.getVoucherProfile(voucher) ?: UserProfileViewData(
                        userId = voucher.voucherUserId,
                        name = voucher.voucherName,
                        roleTitle = "COMMUNITY ENDORSER (VOUCHER)",
                        avatarUrl = voucher.voucherAvatarUrl,
                        locationCity = "Community Peer",
                        trustScore = voucher.voucherTrustScore,
                        verificationLevel = "Tier 3: Verified Peer Endorser",
                        verificationTier = 3,
                        vouchReason = voucher.vouchReason,
                        vouchComment = voucher.comment
                    )
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
 * Rich Social Post Card with Author Header, Verification Badges, Co-Borrower Support,
 * Financial Terms, and Clickable Vouch Endorsers.
 */
@Composable
fun SocialPostCard(
    post: MarketplacePostEntity,
    isVouched: Boolean = false,
    isSelf: Boolean = false,
    vouchers: List<MarketplaceVouchEntity> = emptyList(),
    onAuthorClick: () -> Unit = {},
    onCoBorrowerClick: () -> Unit = {},
    onVoucherClick: (MarketplaceVouchEntity) -> Unit = {},
    onViewAllVouchersClick: () -> Unit = {},
    onVouch: () -> Unit,
    onViewBidsClick: () -> Unit = {},
    onPrimaryAction: () -> Unit
) {
    val isLenderOffer = post.postType == "OFFER_TO_LEND"
    val accentColor = if (isLenderOffer) Gold500 else Emerald400
    var isExpanded by remember { mutableStateOf(false) }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header: Author Circular Avatar + Info (Clickable to User Profile!)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onAuthorClick() }
            ) {
                val effectiveAvatar = post.getEffectiveAvatarUrl()
                val cartoonRes = post.getCartoonAvatarRes()
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .border(1.5.dp, accentColor.copy(alpha = 0.5f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    SubcomposeAsyncImage(
                        model = effectiveAvatar,
                        contentDescription = post.authorName,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                        loading = {
                            Image(
                                painter = painterResource(id = cartoonRes),
                                contentDescription = "2D Cartoon Avatar",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = androidx.compose.ui.layout.ContentScale.Crop
                            )
                        },
                        error = {
                            Image(
                                painter = painterResource(id = cartoonRes),
                                contentDescription = "2D Cartoon Avatar",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = androidx.compose.ui.layout.ContentScale.Crop
                            )
                        }
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = post.authorName,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (post.authorKycVerified) {
                            Spacer(modifier = Modifier.width(5.dp))
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = Emerald400.copy(alpha = 0.15f),
                                border = BorderStroke(0.5.dp, Emerald400.copy(alpha = 0.4f))
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Verified",
                                        tint = Emerald400,
                                        modifier = Modifier.size(10.dp)
                                    )
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Text(
                                        text = "Verified",
                                        color = Emerald400,
                                        fontSize = 9.5.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "@${post.authorName.lowercase().replace(" ", "_")}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text("•", color = Gray500, fontSize = 9.sp)
                        Text(
                            "⭐ ${post.authorTrustScore}/100",
                            style = MaterialTheme.typography.labelSmall,
                            color = Gold500,
                            fontWeight = FontWeight.SemiBold
                        )
                        if (post.locationCity.isNotBlank()) {
                            Text("•", color = Gray500, fontSize = 9.sp)
                            Text(
                                "📍 ${post.locationCity}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text("•", color = Gray500, fontSize = 9.sp)
                        Text(
                            post.createdAt.toRelativeTime(),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Post Type Pill
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = accentColor.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = if (isLenderOffer) "LENDER OFFER" else "SEEKING LOAN",
                        color = accentColor,
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        softWrap = false,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                    )
                }
            }

            // Co-Borrower Row (if present on seeking loan post)
            if (post.coBorrowerName.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFFFAF5FF),
                    border = BorderStroke(1.dp, Color(0xFFE9D5FF)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onCoBorrowerClick() }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFF3E8FF))
                                .border(1.dp, Color(0xFF9333EA), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = post.coBorrowerName.take(2).uppercase(),
                                color = Color(0xFF7E22CE),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Co-Borrower: ${post.coBorrowerName}",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF581C87)
                                )
                                if (post.coBorrowerKycVerified) {
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(
                                        Icons.Default.CheckCircle,
                                        contentDescription = "Verified",
                                        tint = Emerald600,
                                        modifier = Modifier.size(12.dp)
                                    )
                                }
                            }
                            Text(
                                text = "${post.coBorrowerRelationship.ifBlank { "Co-Signer" }} • Tier 2 ID Verified • ⭐ ${post.coBorrowerTrustScore}/100",
                                fontSize = 10.sp,
                                color = Color(0xFF7E22CE)
                            )
                        }

                        Text(
                            text = "View ➔",
                            color = Color(0xFF9333EA),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Post Title
            Text(
                text = post.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Pitch Description (expandable)
            Text(
                text = post.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = if (isExpanded) 10 else 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 17.sp,
                modifier = Modifier.clickable { isExpanded = !isExpanded }
            )

            if (post.description.length > 90) {
                Text(
                    text = if (isExpanded) "Show less" else "Read more...",
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .clickable { isExpanded = !isExpanded }
                        .padding(top = 2.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Financial Capsule Card - Streamlined & Balanced
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1.2f)) {
                        Text(
                            text = if (isLenderOffer) "CAPITAL POOL" else "AMOUNT NEEDED",
                            fontSize = 8.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = if (isLenderOffer) "₹${post.minAmount.toFormattedString()} - ₹${post.maxAmount.toFormattedString()}"
                            else "₹${post.maxAmount.toFormattedString()}",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(24.dp)
                            .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    )

                    Column(
                        modifier = Modifier.weight(0.9f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "INTEREST",
                            fontSize = 8.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${post.interestRate}% p.a.",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = accentColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(24.dp)
                            .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    )

                    Column(
                        modifier = Modifier.weight(0.9f),
                        horizontalAlignment = Alignment.End
                    ) {
                        Text(
                            text = "TENURE",
                            fontSize = 8.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${post.tenureMonths} Mo",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            if (post.collateralOffered.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Blue400.copy(alpha = 0.08f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Icon(Icons.Default.Security, contentDescription = null, tint = Blue400, modifier = Modifier.size(13.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Security / Proof: ${post.collateralOffered}",
                        fontSize = 10.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Footer: Social Counters & Endorsers + Primary CTA
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Vouch Counter & Vouch Clickers Circular Avatars
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Vouch Button
                    Surface(
                        onClick = onVouch,
                        shape = RoundedCornerShape(8.dp),
                        color = if (isVouched) Red400.copy(alpha = 0.20f) else Red400.copy(alpha = 0.08f),
                        border = if (isVouched) BorderStroke(1.dp, Red400.copy(alpha = 0.5f)) else null
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                        ) {
                            Icon(
                                imageVector = if (isVouched) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = "Vouch",
                                tint = Red400,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isVouched) "Vouched (${post.vouchCount})" else "Vouch (${post.vouchCount})",
                                color = Red400,
                                fontSize = 11.sp,
                                fontWeight = if (isVouched) FontWeight.ExtraBold else FontWeight.Bold
                            )
                        }
                    }

                    // Vouch Clickers Circular Avatars Stack (Clickable!)
                    if (vouchers.isNotEmpty()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .clickable { onViewAllVouchersClick() }
                                .padding(horizontal = 2.dp, vertical = 2.dp)
                        ) {
                            vouchers.take(3).forEach { v ->
                                Box(
                                    modifier = Modifier
                                        .size(22.dp)
                                        .clip(CircleShape)
                                        .background(EmeraldLight)
                                        .border(1.dp, Emerald600, CircleShape)
                                        .clickable { onVoucherClick(v) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = v.voucherName.take(1).uppercase(),
                                        color = Emerald600,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                }
                                Spacer(modifier = Modifier.width(2.dp))
                            }
                            if (vouchers.size > 3 || post.vouchCount > vouchers.size) {
                                Text(
                                    text = "+${maxOf(vouchers.size - 3, post.vouchCount - 3)}",
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextSlateMedium
                                )
                            }
                        }
                    }
                }

                // Inquiries / Bids counter & Primary Action CTA
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (post.bidsCount > 0) {
                        Surface(
                            onClick = onViewBidsClick,
                            shape = RoundedCornerShape(8.dp),
                            color = Blue400.copy(alpha = 0.15f),
                            border = BorderStroke(1.dp, Blue400.copy(alpha = 0.35f))
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 5.dp)
                            ) {
                                Icon(Icons.Default.Bolt, contentDescription = "Bids", tint = Blue400, modifier = Modifier.size(13.dp))
                                Spacer(modifier = Modifier.width(2.dp))
                                Text(
                                    "${post.bidsCount} Offers",
                                    color = Blue400,
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    // Primary CTA Button
                    Button(
                        onClick = onPrimaryAction,
                        colors = ButtonDefaults.buttonColors(containerColor = accentColor, contentColor = Navy900),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 11.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = if (isSelf) {
                                if (post.bidsCount > 0) "Review Bids (${post.bidsCount}) ➔" else "My Post"
                            } else if (isLenderOffer) {
                                "Apply Now ➔"
                            } else {
                                "Fund / Bid ➔"
                            },
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
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
