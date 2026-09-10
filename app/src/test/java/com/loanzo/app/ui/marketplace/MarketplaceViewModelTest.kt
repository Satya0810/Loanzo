package com.loanzo.app.ui.marketplace

import com.google.common.truth.Truth.assertThat
import com.loanzo.app.data.dao.AgentDao
import com.loanzo.app.data.entity.MarketplacePostEntity
import com.loanzo.app.data.entity.UserEntity
import com.loanzo.app.data.repository.LoanRepository
import com.loanzo.app.data.repository.MarketplaceRepository
import com.loanzo.app.data.repository.UserRepository
import com.loanzo.app.testutil.MainDispatcherExtension
import io.mockk.*
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

class MarketplaceViewModelTest {

    @RegisterExtension
    @JvmField
    val mainDispatcher = MainDispatcherExtension()

    private val marketplaceRepository = mockk<MarketplaceRepository>(relaxed = true)
    private val userRepository = mockk<UserRepository>(relaxed = true)
    private val loanRepository = mockk<LoanRepository>(relaxed = true)
    private val agentDao = mockk<AgentDao>(relaxed = true)

    private val sampleUser = UserEntity(
        userId = "usr_rahul",
        name = "Rahul Verma",
        role = "BORROWER",
        kycStatus = "VERIFIED"
    )

    private val lenderPost = MarketplacePostEntity(
        postId = "post_lender_1",
        authorId = "usr_lender",
        authorName = "Sunil Sharma",
        postType = "OFFER_TO_LEND",
        title = "Capital for Small Businesses",
        description = "Instant business funds up to 2 lakhs",
        minAmount = 25000.0,
        maxAmount = 200000.0,
        interestRate = 12.0,
        purposeCategory = "BUSINESS",
        locationCity = "Mumbai",
        status = "OPEN"
    )

    private val borrowerPost = MarketplacePostEntity(
        postId = "post_borrower_1",
        authorId = "usr_rahul",
        authorName = "Rahul Verma",
        postType = "SEEKING_LOAN",
        title = "Medical Clinic Expansion",
        description = "Need medical equipment funding",
        minAmount = 50000.0,
        maxAmount = 50000.0,
        interestRate = 10.5,
        purposeCategory = "MEDICAL",
        locationCity = "Pune",
        status = "OPEN"
    )

    @BeforeEach
    fun setUp() {
        clearAllMocks()
        every { userRepository.getCurrentUserId() } returns flowOf("usr_rahul")
        coEvery { userRepository.getUserById("usr_rahul") } returns sampleUser
        coEvery { userRepository.getCurrentUserIdSync() } returns "usr_rahul"

        every { marketplaceRepository.getAllPosts() } returns flowOf(listOf(lenderPost, borrowerPost))
        every { marketplaceRepository.getUserVouchedPostIdsFlow("usr_rahul") } returns flowOf(emptyList())
        every { marketplaceRepository.getVouchesForPostFlow(any()) } returns flowOf(emptyList())
    }

    private fun createViewModel(): MarketplaceViewModel {
        return MarketplaceViewModel(
            marketplaceRepository = marketplaceRepository,
            userRepository = userRepository,
            loanRepository = loanRepository,
            agentDao = agentDao
        )
    }

    // ═══════════════════════════════════════════════════════════════
    // Initialization & Realtime Feed Listener
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Initialization & Realtime Sync")
    inner class InitTests {

        @Test
        fun `init starts realtime feed listener and loads posts`() = runTest {
            val viewModel = createViewModel()

            // Verify real-time feed listener is started
            verify { marketplaceRepository.startRealtimeFeedListener(any()) }

            val state = viewModel.uiState.value
            assertThat(state.currentUserId).isEqualTo("usr_rahul")
            assertThat(state.currentUserName).isEqualTo("Rahul Verma")
            assertThat(state.isKycVerified).isTrue()
            assertThat(state.rawPosts).containsExactly(lenderPost, borrowerPost)
            assertThat(state.posts).containsExactly(lenderPost, borrowerPost)
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // Tab & Category Filtering
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Tab & Category Filtering")
    inner class FilterTests {

        @Test
        fun `LENDERS tab shows only OFFER_TO_LEND posts`() = runTest {
            val viewModel = createViewModel()
            viewModel.setTab(MarketplaceTabFilter.LENDERS)

            val posts = viewModel.uiState.value.posts
            assertThat(posts).containsExactly(lenderPost)
        }

        @Test
        fun `BORROWERS tab shows only SEEKING_LOAN posts`() = runTest {
            val viewModel = createViewModel()
            viewModel.setTab(MarketplaceTabFilter.BORROWERS)

            val posts = viewModel.uiState.value.posts
            assertThat(posts).containsExactly(borrowerPost)
        }

        @Test
        fun `MY_POSTS tab shows only posts created by current user`() = runTest {
            val viewModel = createViewModel()
            viewModel.setTab(MarketplaceTabFilter.MY_POSTS)

            val posts = viewModel.uiState.value.posts
            assertThat(posts).containsExactly(borrowerPost)
        }

        @Test
        fun `category filter matches selected purpose category`() = runTest {
            val viewModel = createViewModel()
            viewModel.setCategoryTag("BUSINESS")

            val posts = viewModel.uiState.value.posts
            assertThat(posts).containsExactly(lenderPost)
        }

        @Test
        fun `search query filters by title or location city`() = runTest {
            val viewModel = createViewModel()
            viewModel.setSearchQuery("Pune")

            val posts = viewModel.uiState.value.posts
            assertThat(posts).containsExactly(borrowerPost)
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // Post Publishing
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Publishing to Community Wall")
    inner class PublishTests {

        @Test
        fun `publishPost delegates to repository and inserts eagerly into UI`() = runTest {
            val viewModel = createViewModel()
            coEvery { marketplaceRepository.publishPost(any()) } returns Result.success(Unit)

            var successCallbackInvoked = false
            viewModel.publishPost(
                title = "Emergency Funds Needed",
                description = "Hospital bills",
                postType = "SEEKING_LOAN",
                minAmount = 30000.0,
                maxAmount = 30000.0,
                interestRate = 11.0,
                tenureMonths = 6,
                purposeCategory = "EMERGENCY",
                locationCity = "Delhi",
                collateralOffered = "Gold ring",
                onSuccess = { successCallbackInvoked = true }
            )

            coVerify { marketplaceRepository.publishPost(match { it.title == "Emergency Funds Needed" }) }
            assertThat(successCallbackInvoked).isTrue()

            val state = viewModel.uiState.value
            assertThat(state.actionSuccessMessage).contains("Post published successfully")
            assertThat(state.posts.any { it.title == "Emergency Funds Needed" }).isTrue()
        }
    }
}
