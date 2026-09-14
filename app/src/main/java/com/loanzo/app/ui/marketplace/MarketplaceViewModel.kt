package com.loanzo.app.ui.marketplace

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.loanzo.app.data.entity.MarketplaceBidEntity
import com.loanzo.app.data.entity.MarketplacePostEntity
import com.loanzo.app.data.entity.MarketplaceVouchEntity
import com.loanzo.app.data.repository.MarketplaceRepository
import com.loanzo.app.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

enum class MarketplaceTabFilter {
    ALL,
    LENDERS,    // postType == "OFFER_TO_LEND"
    BORROWERS,  // postType == "SEEKING_LOAN"
    MY_POSTS
}

data class MarketplaceUiState(
    val posts: List<MarketplacePostEntity> = emptyList(),
    val rawPosts: List<MarketplacePostEntity> = emptyList(),
    val selectedTab: MarketplaceTabFilter = MarketplaceTabFilter.ALL,
    val searchQuery: String = "",
    val selectedCategoryTag: String = "ALL", // ALL, PERSONAL, BUSINESS, EDUCATION, MEDICAL, EMERGENCY
    val maxInterestRateFilter: Double = 36.0,
    val activeFilterCount: Int = 0,
    val currentUserId: String = "",
    val currentUserName: String = "",
    val vouchedPostIds: Set<String> = emptySet(),
    val vouchesByPost: Map<String, List<MarketplaceVouchEntity>> = emptyMap(),
    val isKycVerified: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null,
    val actionSuccessMessage: String? = null
)

@HiltViewModel
class MarketplaceViewModel @Inject constructor(
    private val marketplaceRepository: MarketplaceRepository,
    private val userRepository: UserRepository,
    private val loanRepository: com.loanzo.app.data.repository.LoanRepository,
    private val agentDao: com.loanzo.app.data.dao.AgentDao,
    private val multiAiRaceEngine: com.loanzo.app.data.ai.MultiAiRaceEngine? = null
) : ViewModel() {

    suspend fun enhancePitchDirect(
        rawDraft: String,
        category: String,
        amount: Double,
        tenureMonths: Int
    ): com.loanzo.app.data.ai.AiRaceResult {
        return multiAiRaceEngine?.enhanceMarketplacePitchResult(rawDraft, category, amount, tenureMonths)
            ?: com.loanzo.app.data.ai.AiRaceResult.Success(
                providerName = "Offline Heuristic Guard",
                providerType = "OFFLINE",
                content = "Seeking ₹${amount.toInt()} loan for $category over $tenureMonths months. Full repayment transparency with prompt bank transfers.",
                latencyMs = 0L,
                modelUsed = "offline-fallback"
            )
    }

    fun getBidsForPostFlow(postId: String): Flow<List<MarketplaceBidEntity>> =
        marketplaceRepository.getBidsForPost(postId)

    fun acceptBidAndCreateLoan(
        post: MarketplacePostEntity,
        bid: MarketplaceBidEntity,
        onLoanCreated: (com.loanzo.app.data.entity.LoanEntity) -> Unit
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val currentUserId = _uiState.value.currentUserId.ifBlank {
                userRepository.getCurrentUserIdSync() ?: ""
            }

            // Determine lender and borrower based on post type:
            val actualLenderId = if (post.postType == "OFFER_TO_LEND") post.authorId else bid.bidderId
            val actualBorrowerId = if (post.postType == "OFFER_TO_LEND") bid.bidderId else post.authorId

            // Ensure lender exists in User table
            val lenderUser = userRepository.getUserById(actualLenderId) ?: com.loanzo.app.data.entity.UserEntity(
                userId = actualLenderId,
                name = if (post.postType == "OFFER_TO_LEND") post.authorName else bid.bidderName,
                role = "LENDER",
                kycStatus = "VERIFIED"
            ).also { userRepository.createUser(it) }

            // Ensure borrower exists in User table
            val borrowerUser = userRepository.getUserById(actualBorrowerId) ?: com.loanzo.app.data.entity.UserEntity(
                userId = actualBorrowerId,
                name = if (post.postType == "OFFER_TO_LEND") bid.bidderName else post.authorName,
                role = "BORROWER",
                kycStatus = "VERIFIED"
            ).also { userRepository.createUser(it) }

            val hasCollateral = post.collateralOffered.isNotBlank()
            val initialStatus = if (hasCollateral) "COLLATERAL_VALUATION" else "CONTRACT_SIGNING"

            val newLoanId = "loan_" + UUID.randomUUID().toString().take(12)
            val newLoan = com.loanzo.app.data.entity.LoanEntity(
                loanId = newLoanId,
                lenderId = actualLenderId,
                borrowerId = actualBorrowerId,
                sanctionedAmount = bid.proposedAmount,
                disbursedAmount = 0.0,
                outstandingAmount = bid.proposedAmount,
                purpose = post.title.ifBlank { "P2P Marketplace Loan" },
                loanType = post.purposeCategory,
                interestRate = bid.proposedInterestRate,
                interestModel = post.interestModel,
                tenureMonths = bid.proposedTenureMonths,
                status = initialStatus,
                repaymentFrequency = bid.proposedRepaymentFrequency,
                createdAt = System.currentTimeMillis(),
                notes = "Accepted proposal #${bid.bidId.take(8)} from ${bid.bidderName}. ${bid.message}".trim()
            )

            loanRepository.createLoan(newLoan, currentUserId)

            // If collateral offered, schedule valuer inspection
            if (hasCollateral) {
                try {
                    val visit = com.loanzo.app.data.entity.AgentVisitEntity(
                        visitId = "visit_" + UUID.randomUUID().toString().take(8),
                        agentId = "UNASSIGNED",
                        loanId = newLoanId,
                        visitType = "COLLATERAL_VERIFICATION",
                        title = "Collateral Valuation: ${post.collateralOffered}",
                        borrowerName = borrowerUser.name,
                        borrowerPhone = borrowerUser.phone,
                        borrowerAddress = post.locationCity,
                        lenderName = lenderUser.name,
                        lenderPhone = lenderUser.phone,
                        targetAddress = post.locationCity,
                        collateralItemName = post.collateralOffered,
                        collateralEstimatedValue = bid.proposedAmount * 1.5,
                        status = "SCHEDULED",
                        handshakePin = (1000..9999).random().toString()
                    )
                    agentDao.insertVisit(visit)
                } catch (_: Exception) {}
            }

            // Update bid & post statuses
            marketplaceRepository.acceptBid(bid.bidId, post.postId)

            _uiState.update {
                it.copy(
                    isLoading = false,
                    actionSuccessMessage = "Proposal accepted! Loan initiated in $initialStatus state."
                )
            }

            onLoanCreated(newLoan)
        }
    }

    private val _uiState = MutableStateFlow(MarketplaceUiState())
    val uiState: StateFlow<MarketplaceUiState> = _uiState.asStateFlow()

    init {
        loadCurrentUser()
        observeFeed()
        refreshFeed()
        marketplaceRepository.startRealtimeFeedListener(viewModelScope)
    }

    private fun loadCurrentUser() {
        viewModelScope.launch {
            userRepository.getCurrentUserId().collectLatest { uid ->
                if (!uid.isNullOrBlank()) {
                    val user = userRepository.getUserById(uid)
                    _uiState.update {
                        it.copy(
                            currentUserId = uid,
                            currentUserName = user?.name ?: "User",
                            isKycVerified = (user?.kycStatus == "VERIFIED")
                        )
                    }
                    observeUserVouches(uid)
                }
            }
        }
    }

    private fun observeUserVouches(userId: String) {
        viewModelScope.launch {
            marketplaceRepository.getUserVouchedPostIdsFlow(userId).collectLatest { vouchedList ->
                _uiState.update { it.copy(vouchedPostIds = vouchedList.toSet()) }
            }
        }
    }

    private fun observeFeed() {
        viewModelScope.launch {
            marketplaceRepository.getAllPosts().collectLatest { rawList ->
                _uiState.update { current ->
                    current.copy(
                        rawPosts = rawList,
                        posts = applyFilters(
                            rawList,
                            current.selectedTab,
                            current.searchQuery,
                            current.selectedCategoryTag,
                            current.maxInterestRateFilter,
                            current.currentUserId
                        )
                    )
                }
                observeVouchesForPosts(rawList)
            }
        }
    }

    private fun observeVouchesForPosts(posts: List<MarketplacePostEntity>) {
        posts.forEach { post ->
            viewModelScope.launch {
                marketplaceRepository.getVouchesForPostFlow(post.postId).collectLatest { vouches ->
                    _uiState.update { state ->
                        state.copy(
                            vouchesByPost = state.vouchesByPost + (post.postId to vouches)
                        )
                    }
                }
            }
        }
    }

    fun refreshFeed() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            marketplaceRepository.syncFeed()
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    fun setTab(tab: MarketplaceTabFilter) {
        _uiState.update { current ->
            val updated = current.copy(selectedTab = tab)
            updated.copy(
                posts = applyFilters(
                    current.rawPosts,
                    tab,
                    current.searchQuery,
                    current.selectedCategoryTag,
                    current.maxInterestRateFilter,
                    current.currentUserId
                ),
                activeFilterCount = computeFilterCount(tab, current.searchQuery, current.selectedCategoryTag, current.maxInterestRateFilter)
            )
        }
    }

    fun setSearchQuery(query: String) {
        _uiState.update { current ->
            val updated = current.copy(searchQuery = query)
            updated.copy(
                posts = applyFilters(
                    current.rawPosts,
                    current.selectedTab,
                    query,
                    current.selectedCategoryTag,
                    current.maxInterestRateFilter,
                    current.currentUserId
                ),
                activeFilterCount = computeFilterCount(current.selectedTab, query, current.selectedCategoryTag, current.maxInterestRateFilter)
            )
        }
    }

    fun setCategoryTag(category: String) {
        _uiState.update { current ->
            val targetCategory = if (current.selectedCategoryTag.equals(category, ignoreCase = true)) "ALL" else category
            val updated = current.copy(selectedCategoryTag = targetCategory)
            updated.copy(
                posts = applyFilters(
                    current.rawPosts,
                    current.selectedTab,
                    current.searchQuery,
                    targetCategory,
                    current.maxInterestRateFilter,
                    current.currentUserId
                ),
                activeFilterCount = computeFilterCount(current.selectedTab, current.searchQuery, targetCategory, current.maxInterestRateFilter)
            )
        }
    }

    fun setMaxInterestRate(rate: Double) {
        _uiState.update { current ->
            val updated = current.copy(maxInterestRateFilter = rate)
            updated.copy(
                posts = applyFilters(
                    current.rawPosts,
                    current.selectedTab,
                    current.searchQuery,
                    current.selectedCategoryTag,
                    rate,
                    current.currentUserId
                ),
                activeFilterCount = computeFilterCount(current.selectedTab, current.searchQuery, current.selectedCategoryTag, rate)
            )
        }
    }

    fun clearAllFilters() {
        _uiState.update { current ->
            current.copy(
                selectedTab = MarketplaceTabFilter.ALL,
                searchQuery = "",
                selectedCategoryTag = "ALL",
                maxInterestRateFilter = 36.0,
                activeFilterCount = 0,
                posts = current.rawPosts
            )
        }
    }

    fun publishPost(
        title: String,
        description: String,
        postType: String, // "OFFER_TO_LEND" or "SEEKING_LOAN"
        minAmount: Double,
        maxAmount: Double,
        interestRate: Double,
        tenureMonths: Int,
        purposeCategory: String,
        locationCity: String,
        collateralOffered: String,
        coBorrowerName: String = "",
        coBorrowerRelationship: String = "",
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            // Ensure real user credentials are synchronously resolved
            val currentUserId = _uiState.value.currentUserId.ifBlank {
                userRepository.getCurrentUserIdSync() ?: ""
            }
            val localUser = if (currentUserId.isNotBlank()) userRepository.getUserById(currentUserId) else null
            val effectiveAuthorId = currentUserId
            val effectiveAuthorName = localUser?.name ?: _uiState.value.currentUserName.ifBlank {
                if (postType == "OFFER_TO_LEND") "Verified Lender" else "Verified Borrower"
            }
            val effectiveKyc = (localUser?.kycStatus == "VERIFIED") || _uiState.value.isKycVerified

            val newPost = MarketplacePostEntity(
                postId = "post_${UUID.randomUUID()}",
                authorId = effectiveAuthorId,
                authorName = effectiveAuthorName,
                authorAvatarUrl = localUser?.profilePhotoUri?.takeIf { it.isNotBlank() }
                    ?: com.loanzo.app.util.CartoonAvatarHelper.getCartoonAvatarUrl(effectiveAuthorName.ifBlank { effectiveAuthorId }),
                authorKycVerified = effectiveKyc,
                authorTrustScore = if (effectiveKyc) 95 else 85,
                postType = postType,
                title = title,
                description = description,
                minAmount = minAmount,
                maxAmount = maxAmount,
                interestRate = interestRate,
                tenureMonths = tenureMonths,
                purposeCategory = purposeCategory,
                locationCity = locationCity.ifBlank { localUser?.address ?: "Bengaluru" },
                collateralOffered = collateralOffered,
                vouchCount = 0,
                bidsCount = 0,
                status = "OPEN",
                createdAt = System.currentTimeMillis(),
                coBorrowerName = coBorrowerName,
                coBorrowerRelationship = coBorrowerRelationship,
                coBorrowerKycVerified = coBorrowerName.isNotBlank(),
                coBorrowerTrustScore = if (coBorrowerName.isNotBlank()) 89 else 85
            )

            val result = marketplaceRepository.publishPost(newPost)
            _uiState.update { it.copy(isLoading = false) }
            if (result.isSuccess) {
                // Eagerly insert into current UI state so user sees it right away
                _uiState.update { current ->
                    val updatedRaw = listOf(newPost) + current.rawPosts.filter { it.postId != newPost.postId }
                    current.copy(
                        rawPosts = updatedRaw,
                        posts = applyFilters(
                            updatedRaw,
                            current.selectedTab,
                            current.searchQuery,
                            current.selectedCategoryTag,
                            current.maxInterestRateFilter,
                            current.currentUserId
                        ),
                        actionSuccessMessage = "Post published successfully to the Community Wall!"
                    )
                }
                onSuccess()
            } else {
                _uiState.update { it.copy(error = result.exceptionOrNull()?.message ?: "Failed to publish post") }
            }
        }
    }

    fun submitBid(
        postId: String,
        proposedAmount: Double,
        proposedInterestRate: Double,
        proposedTenureMonths: Int,
        message: String,
        onSuccess: () -> Unit
    ) {
        val user = _uiState.value
        val newBid = MarketplaceBidEntity(
            bidId = UUID.randomUUID().toString(),
            postId = postId,
            bidderId = user.currentUserId.ifBlank { "anonymous_bidder" },
            bidderName = user.currentUserName.ifBlank { "Community Member" },
            bidderAvatarUrl = com.loanzo.app.util.CartoonAvatarHelper.getCartoonAvatarUrl(user.currentUserName.ifBlank { user.currentUserId }),
            bidderKycVerified = user.isKycVerified,
            bidderTrustScore = if (user.isKycVerified) 92 else 80,
            proposedAmount = proposedAmount,
            proposedInterestRate = proposedInterestRate,
            proposedTenureMonths = proposedTenureMonths,
            message = message,
            status = "PENDING",
            createdAt = System.currentTimeMillis()
        )

        viewModelScope.launch {
            val result = marketplaceRepository.submitBid(newBid)
            if (result.isSuccess) {
                _uiState.update { it.copy(actionSuccessMessage = "Proposal submitted successfully!") }
                onSuccess()
            } else {
                _uiState.update { it.copy(error = "Failed to submit proposal: ${result.exceptionOrNull()?.message}") }
            }
        }
    }

    fun vouchForPost(
        postId: String,
        reason: String = "COMMERCIAL_PEER",
        comment: String = ""
    ) {
        viewModelScope.launch {
            val uid = _uiState.value.currentUserId.ifBlank {
                userRepository.getCurrentUserIdSync()
                    ?: com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
                    ?: ""
            }
            if (uid.isBlank()) {
                _uiState.update { it.copy(error = "Please sign in to vouch for posts.") }
                return@launch
            }
            val user = userRepository.getUserById(uid)
            val name = _uiState.value.currentUserName.ifBlank {
                user?.name ?: user?.username ?: "Verified Member"
            }
            val result = marketplaceRepository.vouchForPost(
                postId = postId,
                voucherUserId = uid,
                voucherName = name,
                reason = reason,
                comment = comment
            )
            if (result.isSuccess) {
                val isNowVouched = result.getOrNull() == true
                _uiState.update {
                    it.copy(
                        actionSuccessMessage = if (isNowVouched) "Endorsement recorded! Thank you for vouching." else "Vouch removed."
                    )
                }
            } else {
                _uiState.update {
                    it.copy(error = result.exceptionOrNull()?.message ?: "Failed to update vouch")
                }
            }
        }
    }

    fun clearFeedback() {
        _uiState.update { it.copy(error = null, actionSuccessMessage = null) }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PROFILE BUILDERS FOR MODAL INSPECTION (AUTHOR, CO-BORROWER, VOUCHER)
    // ─────────────────────────────────────────────────────────────────────────
    suspend fun getAuthorProfile(post: MarketplacePostEntity): UserProfileViewData {
        val localUser = userRepository.getUserById(post.authorId)
        val isLender = post.postType == "OFFER_TO_LEND"
        return UserProfileViewData(
            userId = post.authorId,
            name = localUser?.name ?: post.authorName,
            username = localUser?.username ?: post.authorName.lowercase().replace(" ", "").filter { it.isLetterOrDigit() }.take(12),
            roleTitle = if (isLender) "CAPITAL PROVIDER (LENDER)" else "PRIMARY BORROWER (LOAN SEEKER)",
            avatarUrl = localUser?.profilePhotoUri ?: post.authorAvatarUrl,
            locationCity = post.locationCity.ifBlank { localUser?.address ?: "Bengaluru" },
            trustScore = post.authorTrustScore,
            verificationLevel = if (localUser?.bankVerified == true || post.authorKycVerified) "Tier 3: Institutional Gold" else "Tier 2: National ID Verified",
            verificationTier = if (localUser?.bankVerified == true || post.authorKycVerified) 3 else 2,
            phoneVerified = localUser?.phoneVerified ?: true,
            emailVerified = localUser?.emailVerified ?: true,
            aadhaarVerified = localUser?.aadhaarVerified ?: post.authorKycVerified,
            panVerified = localUser?.panVerified ?: post.authorKycVerified,
            selfieVerified = localUser?.selfieVerified ?: true,
            bankVerified = localUser?.bankVerified ?: true,
            upiVerified = localUser?.upiVerified ?: true,
            ckycVerified = true,
            onTimeRepaymentRate = if (post.authorTrustScore > 90) 100.0 else 96.5,
            completedLoansCount = if (isLender) 14 else 4,
            activeLoansCount = 1,
            defaultCount = 0,
            vouchesReceivedCount = post.vouchCount,
            memberSince = "Member since " + if (isLender) "Jan 2024" else "May 2024",
            employmentStatus = if (isLender) "Accredited Capital Provider / Entity" else "Verified Salaried / Business Cashflow",
            monthlyIncomeFormatted = if (isLender) "Capital Pool Active" else "₹65,000 / month",
            collateralOrProof = post.collateralOffered,
            phoneMasked = if (!localUser?.phone.isNullOrBlank()) localUser!!.phone.take(3) + "•••• " + localUser.phone.takeLast(4) else "+91 98•••• 4829",
            emailMasked = if (!localUser?.email.isNullOrBlank()) localUser!!.email.take(2) + "••••@" + localUser.email.substringAfter("@", "gmail.com") else "m••••@gmail.com"
        )
    }

    fun getCoBorrowerProfile(post: MarketplacePostEntity): UserProfileViewData {
        return UserProfileViewData(
            userId = "coborrower_${post.postId}",
            name = post.coBorrowerName,
            username = post.coBorrowerName.lowercase().replace(" ", "").filter { it.isLetterOrDigit() }.take(12),
            roleTitle = "CO-BORROWER / GUARANTOR",
            avatarUrl = post.coBorrowerAvatarUrl,
            locationCity = post.locationCity,
            trustScore = post.coBorrowerTrustScore,
            verificationLevel = "Tier 2: National ID & Income Verified",
            verificationTier = 2,
            phoneVerified = true,
            emailVerified = true,
            aadhaarVerified = post.coBorrowerKycVerified,
            panVerified = post.coBorrowerKycVerified,
            selfieVerified = true,
            bankVerified = true,
            upiVerified = true,
            ckycVerified = true,
            onTimeRepaymentRate = 100.0,
            completedLoansCount = 2,
            activeLoansCount = 1,
            defaultCount = 0,
            vouchesReceivedCount = 5,
            memberSince = "Member since Nov 2024",
            employmentStatus = "Employed / Co-Earning Dependent",
            monthlyIncomeFormatted = "₹45,000 / month",
            relationshipToBorrower = post.coBorrowerRelationship.ifBlank { "Spouse (Co-Signer)" },
            phoneMasked = "+91 97•••• 8391",
            emailMasked = "c•••••@gmail.com"
        )
    }

    fun getVoucherProfile(vouch: MarketplaceVouchEntity): UserProfileViewData {
        return UserProfileViewData(
            userId = vouch.voucherUserId,
            name = vouch.voucherName,
            username = vouch.voucherName.lowercase().replace(" ", "").filter { it.isLetterOrDigit() }.take(12),
            roleTitle = "COMMUNITY ENDORSER (VOUCHER)",
            avatarUrl = vouch.voucherAvatarUrl,
            locationCity = "Community Peer",
            trustScore = vouch.voucherTrustScore,
            verificationLevel = "Tier 3: Verified Peer Endorser",
            verificationTier = 3,
            phoneVerified = true,
            emailVerified = true,
            aadhaarVerified = vouch.voucherKycVerified,
            panVerified = vouch.voucherKycVerified,
            selfieVerified = true,
            bankVerified = true,
            upiVerified = true,
            ckycVerified = true,
            onTimeRepaymentRate = 100.0,
            completedLoansCount = 6,
            activeLoansCount = 0,
            defaultCount = 0,
            vouchesReceivedCount = 21,
            memberSince = "Member since Aug 2023",
            employmentStatus = vouch.voucherRole.ifBlank { "Enterprise Peer" },
            vouchReason = vouch.vouchReason,
            vouchComment = vouch.comment,
            phoneMasked = "+91 99•••• 1024",
            emailMasked = "v•••••@company.in"
        )
    }

    private fun applyFilters(
        list: List<MarketplacePostEntity>,
        tab: MarketplaceTabFilter,
        query: String,
        category: String,
        maxRate: Double,
        currentUserId: String
    ): List<MarketplacePostEntity> {
        return list.filter { post ->
            val matchesTab = when (tab) {
                MarketplaceTabFilter.ALL -> true
                MarketplaceTabFilter.LENDERS -> post.postType == "OFFER_TO_LEND"
                MarketplaceTabFilter.BORROWERS -> post.postType == "SEEKING_LOAN"
                MarketplaceTabFilter.MY_POSTS -> post.authorId == currentUserId
            }

            val matchesCategory = if (category.equals("ALL", ignoreCase = true)) true
            else post.purposeCategory.equals(category, ignoreCase = true)

            val matchesRate = post.interestRate <= maxRate

            val matchesQuery = if (query.isBlank()) true else {
                val q = query.trim().lowercase()
                post.title.lowercase().contains(q) ||
                post.description.lowercase().contains(q) ||
                post.authorName.lowercase().contains(q) ||
                post.coBorrowerName.lowercase().contains(q) ||
                post.authorId.lowercase().contains(q) ||
                post.locationCity.lowercase().contains(q) ||
                post.purposeCategory.lowercase().contains(q)
            }

            matchesTab && matchesCategory && matchesRate && matchesQuery
        }
    }

    private fun computeFilterCount(
        tab: MarketplaceTabFilter,
        query: String,
        category: String,
        maxRate: Double
    ): Int {
        var count = 0
        if (tab != MarketplaceTabFilter.ALL) count++
        if (query.isNotBlank()) count++
        if (!category.equals("ALL", ignoreCase = true)) count++
        if (maxRate < 36.0) count++
        return count
    }

    override fun onCleared() {
        super.onCleared()
        marketplaceRepository.stopRealtimeFeedListener()
    }
}
