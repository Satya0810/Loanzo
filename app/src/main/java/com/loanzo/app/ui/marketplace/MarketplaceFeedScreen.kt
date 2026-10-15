package com.loanzo.app.ui.marketplace

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import com.loanzo.app.ui.components.LoanzoText as Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
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
import kotlinx.coroutines.Dispatchers
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
    targetPostId: String? = null,
    showBackButton: Boolean = true,
    onNavigateBack: () -> Unit
) {
    BackHandler {
        onNavigateBack()
    }
    var selectedPostForBid by remember { mutableStateOf<MarketplacePostEntity?>(null) }
    var activePostForBidsSheet by remember { mutableStateOf<MarketplacePostEntity?>(null) }
    var postToVouch by remember { mutableStateOf<MarketplacePostEntity?>(null) }
    var activeProfileToView by remember { mutableStateOf<UserProfileViewData?>(null) }
    var activePostForVouchers by remember { mutableStateOf<MarketplacePostEntity?>(null) }
    var selectedPostForAdminAction by remember { mutableStateOf<MarketplacePostEntity?>(null) }
    var selectedPostForAdminInspection by remember { mutableStateOf<MarketplacePostEntity?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    val userRepository = com.loanzo.app.util.LocalUserRepository.current
    val currentUserId = state.currentUserId
    val currentUser by (if (currentUserId.isNotBlank()) userRepository.observeUser(currentUserId) else kotlinx.coroutines.flow.flowOf(null))
        .collectAsStateWithLifecycle(initialValue = null)
    val isMasterAdmin = com.loanzo.app.util.VerificationManager.isAppOwner(currentUser)

    val marketplaceGuideSeen by userRepository.isGuideSeen(com.loanzo.app.data.repository.UserRepository.GUIDE_MARKETPLACE_SEEN)
        .collectAsStateWithLifecycle(initialValue = true)
    val scope = rememberCoroutineScope()

    val tabs = listOf("All Offers", "Lenders", "Borrowers", "My Posts")
    val selectedTabIndex = when (state.selectedTab) {
        MarketplaceTabFilter.ALL -> 0
        MarketplaceTabFilter.LENDERS -> 1
        MarketplaceTabFilter.BORROWERS -> 2
        MarketplaceTabFilter.MY_POSTS -> 3
    }
    var isFilterDropdownOpen by remember { mutableStateOf(false) }

    val categories = listOf("ALL", "EDUCATION", "MEDICAL", "BUSINESS", "EMERGENCY", "PERSONAL")

    LaunchedEffect(Unit) {
        viewModel?.refreshFeed()
    }

    LaunchedEffect(targetPostId, state.posts) {
        if (!targetPostId.isNullOrBlank()) {
            val target = state.posts.firstOrNull { it.postId == targetPostId }
                ?: state.rawPosts.firstOrNull { it.postId == targetPostId }
            if (target != null) {
                activePostForBidsSheet = target
            }
        }
    }

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
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            "Community Wall",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    },
                    navigationIcon = {
                        if (showBackButton) {
                            IconButton(onClick = onNavigateBack) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back",
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    },
                    actions = {},
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                // Inline Filter Dropdown ("All Offers") + Search Bar Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // 1. Filter Dropdown Button with 180° Bouncy Chevron Animation
                    Box {
                        val chevronRotation by animateFloatAsState(
                            targetValue = if (isFilterDropdownOpen) 180f else 0f,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessLow
                            ),
                            label = "filter_chevron_rotation"
                        )
                        val buttonBorderColor by animateColorAsState(
                            targetValue = if (isFilterDropdownOpen) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                            animationSpec = tween(200),
                            label = "filter_border_color"
                        )
                        val buttonBgColor by animateColorAsState(
                            targetValue = if (isFilterDropdownOpen) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            animationSpec = tween(200),
                            label = "filter_bg_color"
                        )

                        Surface(
                            onClick = { isFilterDropdownOpen = !isFilterDropdownOpen },
                            shape = RoundedCornerShape(14.dp),
                            color = buttonBgColor,
                            border = BorderStroke(1.dp, buttonBorderColor),
                            modifier = Modifier.height(50.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center,
                                modifier = Modifier.padding(horizontal = 12.dp)
                            ) {
                                Text(
                                    text = tabs.getOrElse(selectedTabIndex) { "All Offers" },
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isFilterDropdownOpen) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.Default.KeyboardArrowDown,
                                    contentDescription = "Select Filter",
                                    tint = if (isFilterDropdownOpen) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier
                                        .size(18.dp)
                                        .graphicsLayer { rotationZ = chevronRotation }
                                )
                            }
                        }

                        DropdownMenu(
                            expanded = isFilterDropdownOpen,
                            onDismissRequest = { isFilterDropdownOpen = false },
                            shape = RoundedCornerShape(16.dp),
                            containerColor = MaterialTheme.colorScheme.surface,
                            tonalElevation = 8.dp,
                            shadowElevation = 8.dp,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
                            modifier = Modifier.widthIn(min = 180.dp)
                        ) {
                            tabs.forEachIndexed { index, tabTitle ->
                                val isSelected = selectedTabIndex == index
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = tabTitle,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                            fontSize = 14.sp
                                        )
                                    },
                                    trailingIcon = {
                                        if (isSelected) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = "Selected",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    },
                                    onClick = {
                                        val filter = when (index) {
                                            0 -> MarketplaceTabFilter.ALL
                                            1 -> MarketplaceTabFilter.LENDERS
                                            2 -> MarketplaceTabFilter.BORROWERS
                                            3 -> MarketplaceTabFilter.MY_POSTS
                                            else -> MarketplaceTabFilter.ALL
                                        }
                                        onTabSelected(filter)
                                        isFilterDropdownOpen = false
                                    }
                                )
                                if (index < tabs.lastIndex) {
                                    HorizontalDivider(
                                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                                        thickness = 0.5.dp
                                    )
                                }
                            }
                        }
                    }

                    // 2. Compact Search Field Inline
                    OutlinedTextField(
                        value = state.searchQuery,
                        onValueChange = onSearchQueryChange,
                        placeholder = { Text("Search purpose...", fontSize = 13.sp, maxLines = 1) },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Search,
                                contentDescription = "Search",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
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
                    modifier = Modifier.padding(bottom = 4.dp)
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
                PullToRefreshBox(
                    isRefreshing = state.isLoading,
                    onRefresh = { viewModel?.refreshFeed() },
                    modifier = Modifier.fillMaxSize()
                ) {
                    if (state.isLoading && state.posts.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = Gold500)
                        }
                } else if (state.posts.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
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
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        items(state.posts, key = { it.postId }) { post ->
                            val isVouched = post.postId in state.vouchedPostIds
                            val isSelf = post.authorId == state.currentUserId
                            val hasSubmittedBid = post.postId in state.userSubmittedPostIds
                            val postVouches = state.vouchesByPost[post.postId] ?: emptyList()

                            SocialPostCard(
                                post = post,
                                isVouched = isVouched,
                                isSelf = isSelf,
                                hasSubmittedBid = hasSubmittedBid,
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
                                },
                                isAdmin = isMasterAdmin,
                                onAdminTakeAction = { selectedPostForAdminAction = post },
                                onAdminInspect = { selectedPostForAdminInspection = post },
                                onAdminToggleVerify = {
                                    viewModel?.setAdminVerification(post.postId, !post.isAdminVerified)
                                }
                            )
                        }
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
            val existingBid = state.userBidsByPost[post.postId]
            BidProposalBottomSheet(
                post = post,
                existingBid = existingBid,
                onDismiss = { selectedPostForBid = null },
                onSubmitBid = { amount, rate, tenure, msg ->
                    onSubmitBid(post.postId, amount, rate, tenure, msg)
                    selectedPostForBid = null
                }
            )
        }

        // Community Proposals & Bids Bottom Sheet (Accept Bids -> Create Loan Workflow)
        activePostForBidsSheet?.let { post ->
            LaunchedEffect(post.postId) {
                viewModel?.refreshBidsForPost(post.postId)
            }
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
            LaunchedEffect(post.postId) {
                viewModel?.refreshVouchesForPost(post.postId)
            }
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

        // Master Admin Moderation Action Sheet
        selectedPostForAdminAction?.let { post ->
            AdminPostModerationSheet(
                post = post,
                onDismiss = { selectedPostForAdminAction = null },
                onRemovePost = {
                    viewModel?.removePostByAdmin(post.postId)
                    selectedPostForAdminAction = null
                },
                onToggleVerification = {
                    viewModel?.setAdminVerification(post.postId, !post.isAdminVerified)
                    selectedPostForAdminAction = null
                },
                onContactAuthor = {
                    scope.launch(Dispatchers.IO) {
                        val author = userRepository.getUserById(post.authorId)
                        val phone = author?.phone?.ifBlank { null }
                        if (!phone.isNullOrBlank()) {
                            try {
                                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone"))
                                context.startActivity(intent)
                            } catch (_: Exception) {}
                        }
                    }
                    selectedPostForAdminAction = null
                }
            )
        }

        // Master Admin Inspection Sheet
        selectedPostForAdminInspection?.let { post ->
            AdminPostInspectionSheet(
                post = post,
                onDismiss = { selectedPostForAdminInspection = null },
                onViewProfile = {
                    selectedPostForAdminInspection = null
                    onNavigateToUserProfile(post.authorId)
                },
                onToggleVerification = {
                    viewModel?.setAdminVerification(post.postId, !post.isAdminVerified)
                }
            )
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
    hasSubmittedBid: Boolean = false,
    vouchers: List<MarketplaceVouchEntity> = emptyList(),
    onAuthorClick: () -> Unit = {},
    onCoBorrowerClick: () -> Unit = {},
    onVoucherClick: (MarketplaceVouchEntity) -> Unit = {},
    onViewAllVouchersClick: () -> Unit = {},
    onVouch: () -> Unit,
    onViewBidsClick: () -> Unit = {},
    onPrimaryAction: () -> Unit,
    isAdmin: Boolean = false,
    onAdminTakeAction: () -> Unit = {},
    onAdminInspect: () -> Unit = {},
    onAdminToggleVerify: () -> Unit = {}
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
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
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
                        if (post.isAdminVerified) {
                            Spacer(modifier = Modifier.width(5.dp))
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = Color(0xFF10B981).copy(alpha = 0.15f),
                                border = BorderStroke(0.5.dp, Color(0xFF10B981).copy(alpha = 0.5f))
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Verified,
                                        contentDescription = "Admin Verified",
                                        tint = Color(0xFF10B981),
                                        modifier = Modifier.size(10.dp)
                                    )
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Text(
                                        text = "Admin Verified",
                                        color = Color(0xFF10B981),
                                        fontSize = 9.5.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "@${post.authorName.lowercase().replace(" ", "_")}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text("•", color = Gray500, fontSize = 9.sp)
                        Text(
                            "Trust: ${post.authorTrustScore}",
                            style = MaterialTheme.typography.labelSmall,
                            color = Gold500,
                            fontWeight = FontWeight.SemiBold
                        )
                        if (post.locationCity.isNotBlank()) {
                            Text("•", color = Gray500, fontSize = 9.sp)
                            Text(
                                post.locationCity,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Text("•", color = Gray500, fontSize = 9.sp)
                        Text(
                            post.createdAt.toRelativeTime(),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.width(6.dp))

                // Post Type Pill
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = accentColor.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = if (isLenderOffer) "LENDER OFFER" else "SEEKING LOAN",
                        color = accentColor,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        softWrap = false,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
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
                                text = "${post.coBorrowerRelationship.ifBlank { "Co-Signer" }} • Tier 2 ID Verified • Trust: ${post.coBorrowerTrustScore}/100",
                                fontSize = 10.sp,
                                color = Color(0xFF7E22CE)
                            )
                        }

                        Text(
                            text = "View",
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

            // Financial Capsule Card - Responsive & Adaptive
            val amountText = if (isLenderOffer) "₹${post.minAmount.toFormattedString()} - ₹${post.maxAmount.toFormattedString()}"
            else "₹${post.maxAmount.toFormattedString()}"
            val amountFontSize = if (amountText.length > 16) 11.5.sp else if (amountText.length > 12) 12.sp else 13.sp

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1.3f)) {
                        Text(
                            text = if (isLenderOffer) "CAPITAL POOL" else "AMOUNT NEEDED",
                            fontSize = 8.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = amountText,
                            fontSize = amountFontSize,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            softWrap = false,
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
                        modifier = Modifier.weight(0.85f),
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
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = accentColor,
                            maxLines = 1,
                            softWrap = false,
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
                        modifier = Modifier.weight(0.85f),
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
                            fontSize = 12.5.sp,
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

            // Footer: Master Admin Action Bar OR Member 2-Tier Layout
            if (isAdmin) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Take Action Button
                    Button(
                        onClick = onAdminTakeAction,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Red400.copy(alpha = 0.12f),
                            contentColor = Red400
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(38.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Gavel,
                            contentDescription = "Take Action",
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Take Action",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Inspect Button
                    Button(
                        onClick = onAdminInspect,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Blue400.copy(alpha = 0.12f),
                            contentColor = Blue400
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(38.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Visibility,
                            contentDescription = "Inspect",
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Inspect",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Admin Verified Toggle Button
                    Button(
                        onClick = onAdminToggleVerify,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (post.isAdminVerified) Emerald600.copy(alpha = 0.15f) else Color(0xFF64748B).copy(alpha = 0.12f),
                            contentColor = if (post.isAdminVerified) Emerald600 else Color(0xFF64748B)
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1.2f)
                            .height(38.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Icon(
                            imageVector = if (post.isAdminVerified) Icons.Default.Verified else Icons.Default.CheckCircle,
                            contentDescription = "Verify",
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (post.isAdminVerified) "Verified" else "Verify",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Tier 1: Social Engagement & Offers Counter
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Left: Vouch Button & Voucher Avatars Stack
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

                            // Vouch Clickers Circular Avatars Stack
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

                        // Right: Bids / Offers counter badge
                        if (post.bidsCount > 0) {
                            Surface(
                                onClick = onViewBidsClick,
                                shape = RoundedCornerShape(8.dp),
                                color = Blue400.copy(alpha = 0.15f),
                                border = BorderStroke(1.dp, Blue400.copy(alpha = 0.35f))
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                                ) {
                                    Icon(Icons.Default.Bolt, contentDescription = "Bids", tint = Blue400, modifier = Modifier.size(13.dp))
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        "${post.bidsCount} Offers",
                                        color = Blue400,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    // Tier 2: Full-Width Prominent Primary CTA Button (Never crowded!)
                    val isPostFundedOrClosed = post.status.equals("FUNDED", ignoreCase = true) || post.status.equals("CLOSED", ignoreCase = true)

                    Button(
                        onClick = onPrimaryAction,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (hasSubmittedBid) Emerald500.copy(alpha = 0.15f) else if (isPostFundedOrClosed) Color(0xFFE2E8F0) else accentColor,
                            contentColor = if (hasSubmittedBid) Emerald500 else if (isPostFundedOrClosed) Color(0xFF64748B) else Navy900
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 40.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = if (isSelf) {
                                if (post.bidsCount > 0) "Review Received Bids (${post.bidsCount})" else "Manage Post"
                            } else if (hasSubmittedBid) {
                                "Proposal / Application Submitted"
                            } else if (isPostFundedOrClosed) {
                                "Deal Finalized / Post Closed"
                            } else if (isLenderOffer) {
                                "Apply for this Capital"
                            } else {
                                "Fund / Propose Bid"
                            },
                            fontSize = 12.sp,
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
    existingBid: MarketplaceBidEntity? = null,
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
        if (existingBid != null) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 36.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isLenderOffer) "Loan Application Submitted" else "Funding Proposal Submitted",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Target Post: ${post.title}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Emerald500.copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, Emerald500.copy(alpha = 0.4f))
                    ) {
                        Text(
                            text = existingBid.status,
                            color = Emerald500,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "SUBMITTED TERMS",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Proposed Principal", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("₹${existingBid.proposedAmount.toFormattedString()}", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Proposed Rate", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("${existingBid.proposedInterestRate}% p.a.", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Gold500)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Proposed Tenure", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("${existingBid.proposedTenureMonths} Months", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                        if (existingBid.message.isNotBlank()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Note / Repayment terms:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text("\"${existingBid.message}\"", fontSize = 12.sp, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Blue400.copy(alpha = 0.08f),
                    border = BorderStroke(1.dp, Blue400.copy(alpha = 0.25f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = Blue400, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Your application is under active review by the post author. You will receive a direct notification once terms are accepted.",
                            fontSize = 11.5.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            lineHeight = 16.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Text("Close", fontWeight = FontWeight.Bold)
                }
            }
        } else {
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

            var isSubmittingBid by remember { mutableStateOf(false) }

            val buttonText = if (!isKycCompleted) {
                "Complete KYC to ${if (isLenderOffer) "Apply" else "Propose"}"
            } else if (isLenderOffer) {
                "Submit Loan Application"
            } else {
                "Submit Funding Proposal"
            }

            com.loanzo.app.ui.components.LoanzoActionButton(
                text = buttonText,
                onClick = {
                    if (!isKycCompleted) {
                        onDismiss()
                        onNavigateToKyc()
                        return@LoanzoActionButton
                    }
                    if (!isSubmittingBid) {
                        isSubmittingBid = true
                        val amount = proposedAmount.toDoubleOrNull() ?: post.minAmount
                        val rate = proposedRate.toDoubleOrNull() ?: post.interestRate
                        val tenure = proposedTenure.toIntOrNull() ?: post.tenureMonths
                        onSubmitBid(amount, rate, tenure, message)
                    }
                },
                isLoading = isSubmittingBid,
                loadingText = "Submitting Proposal...",
                enabled = !isSubmittingBid,
                containerColor = if (!isKycCompleted) Red400 else (if (isLenderOffer) Gold500 else Emerald400),
                contentColor = if (!isKycCompleted) Color.White else Navy900,
                shape = RoundedCornerShape(14.dp),
                height = 50.dp,
                fontSize = 15.sp,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
}

/**
 * Master Admin Post Moderation Bottom Sheet.
 * Allows Master Admin to remove/hide posts, contact author, or toggle verification status.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminPostModerationSheet(
    post: MarketplacePostEntity,
    onDismiss: () -> Unit,
    onRemovePost: () -> Unit,
    onToggleVerification: () -> Unit,
    onContactAuthor: () -> Unit
) {
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Admin Moderation Desk",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Post ID: ${post.postId.take(12)}...",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Red400.copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, Red400.copy(alpha = 0.4f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Gavel,
                            contentDescription = null,
                            tint = Red400,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Admin Mode",
                            color = Red400,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Post Summary Card
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = post.title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Author: ${post.authorName} (@${post.authorName.lowercase().replace(" ", "_")})",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Amount: Rs. ${post.maxAmount.toFormattedString()} | Rate: ${post.interestRate}%",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Action 1: Toggle Admin Verification
            Surface(
                onClick = onToggleVerification,
                shape = RoundedCornerShape(12.dp),
                color = if (post.isAdminVerified) Emerald600.copy(alpha = 0.1f) else Blue400.copy(alpha = 0.1f),
                border = BorderStroke(1.dp, if (post.isAdminVerified) Emerald600.copy(alpha = 0.3f) else Blue400.copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (post.isAdminVerified) Icons.Default.Verified else Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = if (post.isAdminVerified) Emerald600 else Blue400,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (post.isAdminVerified) "Revoke Admin Verification" else "Grant Official Admin Verification",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.5.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (post.isAdminVerified) "Remove the official verified badge from this community post" else "Mark post as verified to increase credibility and trust across community",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Action 2: Contact Author
            Surface(
                onClick = onContactAuthor,
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Phone,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Call Author Directly",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.5.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Contact ${post.authorName} via dialer for compliance inquiry",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Action 3: Remove Post from Wall
            Surface(
                onClick = onRemovePost,
                shape = RoundedCornerShape(12.dp),
                color = Red400.copy(alpha = 0.1f),
                border = BorderStroke(1.dp, Red400.copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = null,
                        tint = Red400,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Remove Post from Community Wall",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.5.sp,
                            color = Red400
                        )
                        Text(
                            text = "Immediately delete post from community wall feed across all users",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

/**
 * Master Admin Post Inspection Bottom Sheet.
 * Provides deep audit view into post author KYC, trust score, co-borrower status, and terms.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminPostInspectionSheet(
    post: MarketplacePostEntity,
    onDismiss: () -> Unit,
    onViewProfile: () -> Unit,
    onToggleVerification: () -> Unit
) {
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Audit & Inspection",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Verification & Risk Audit",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Blue400.copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, Blue400.copy(alpha = 0.4f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Visibility,
                            contentDescription = null,
                            tint = Blue400,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Audit Mode",
                            color = Blue400,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Author Audit Card
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = post.authorName,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "User ID: ${post.authorId.take(12)}...",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (post.authorKycVerified) Emerald600.copy(alpha = 0.15f) else Red400.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = if (post.authorKycVerified) "KYC Verified" else "KYC Unverified",
                                color = if (post.authorKycVerified) Emerald600 else Red400,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), thickness = 0.5.dp)
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Trust Score", fontSize = 10.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("${post.authorTrustScore} / 100", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Gold500)
                        }
                        Column {
                            Text("City", fontSize = 10.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(post.locationCity.ifBlank { "Unspecified" }, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        }
                        Column {
                            Text("Admin Verified", fontSize = 10.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(if (post.isAdminVerified) "Yes" else "No", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = if (post.isAdminVerified) Emerald600 else MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Co-Borrower Audit Card (if present)
            if (post.coBorrowerName.isNotBlank()) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFFFAF5FF),
                    border = BorderStroke(1.dp, Color(0xFFE9D5FF)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Co-Borrower / Guarantor",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF7E22CE)
                                )
                                Text(
                                    text = post.coBorrowerName,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = Color(0xFF581C87)
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (post.coBorrowerKycVerified) Emerald600.copy(alpha = 0.15f) else Red400.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = if (post.coBorrowerKycVerified) "Verified" else "Pending",
                                    color = if (post.coBorrowerKycVerified) Emerald600 else Red400,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Relationship: ${post.coBorrowerRelationship.ifBlank { "Not specified" }} | Trust: ${post.coBorrowerTrustScore}",
                            fontSize = 11.sp,
                            color = Color(0xFF6B21A8)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Buttons: View Full Profile & Toggle Admin Verified
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onViewProfile,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(46.dp)
                ) {
                    Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Author Profile", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = onToggleVerification,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (post.isAdminVerified) Red400.copy(alpha = 0.15f) else Emerald600,
                        contentColor = if (post.isAdminVerified) Red400 else Color.White
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(46.dp)
                ) {
                    Icon(
                        imageVector = if (post.isAdminVerified) Icons.Default.Close else Icons.Default.Verified,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (post.isAdminVerified) "Revoke Verify" else "Verify Post",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
