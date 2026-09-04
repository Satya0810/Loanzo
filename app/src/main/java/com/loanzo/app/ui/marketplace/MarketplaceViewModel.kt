package com.loanzo.app.ui.marketplace

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.loanzo.app.data.entity.MarketplaceBidEntity
import com.loanzo.app.data.entity.MarketplacePostEntity
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
    val isKycVerified: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null,
    val actionSuccessMessage: String? = null
)

@HiltViewModel
class MarketplaceViewModel @Inject constructor(
    private val marketplaceRepository: MarketplaceRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(MarketplaceUiState())
    val uiState: StateFlow<MarketplaceUiState> = _uiState.asStateFlow()

    init {
        loadCurrentUser()
        observeFeed()
        refreshFeed()
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
                }
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
            val targetCategory = if (current.selectedCategoryTag == category) "ALL" else category
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
        onSuccess: () -> Unit
    ) {
        val user = _uiState.value
        val newPost = MarketplacePostEntity(
            postId = UUID.randomUUID().toString(),
            authorId = user.currentUserId.ifBlank { "anonymous_user" },
            authorName = user.currentUserName.ifBlank { if (postType == "OFFER_TO_LEND") "Verified Lender" else "Verified Borrower" },
            authorAvatarUrl = "",
            authorKycVerified = user.isKycVerified,
            authorTrustScore = if (user.isKycVerified) 92 else 80,
            postType = postType,
            title = title,
            description = description,
            minAmount = minAmount,
            maxAmount = maxAmount,
            interestRate = interestRate,
            tenureMonths = tenureMonths,
            purposeCategory = purposeCategory,
            locationCity = locationCity,
            collateralOffered = collateralOffered,
            vouchCount = 0,
            bidsCount = 0,
            status = "OPEN",
            createdAt = System.currentTimeMillis()
        )

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val result = marketplaceRepository.publishPost(newPost)
            _uiState.update { it.copy(isLoading = false) }
            if (result.isSuccess) {
                _uiState.update { it.copy(actionSuccessMessage = "Post published successfully to the Community Wall!") }
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
            bidderAvatarUrl = "",
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

    fun vouchForPost(postId: String) {
        viewModelScope.launch {
            marketplaceRepository.vouchForPost(postId)
        }
    }

    fun clearFeedback() {
        _uiState.update { it.copy(error = null, actionSuccessMessage = null) }
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
}
