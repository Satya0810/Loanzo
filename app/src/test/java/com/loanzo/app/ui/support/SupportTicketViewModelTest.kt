package com.loanzo.app.ui.support

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.loanzo.app.data.entity.LoanEntity
import com.loanzo.app.data.entity.SupportTicketEntity
import com.loanzo.app.data.entity.UserEntity
import com.loanzo.app.data.repository.LoanRepository
import com.loanzo.app.data.repository.SupportTicketRepository
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

class SupportTicketViewModelTest {

    @RegisterExtension
    @JvmField
    val mainDispatcher = MainDispatcherExtension()

    private val supportTicketRepository = mockk<SupportTicketRepository>(relaxed = true)
    private val userRepository = mockk<UserRepository>(relaxed = true)
    private val loanRepository = mockk<LoanRepository>(relaxed = true)

    private val sampleUser = UserEntity(
        userId = "usr_1",
        name = "Vikram Singh",
        email = "vikram@test.com",
        phone = "9876543210"
    )

    private val sampleTicket = SupportTicketEntity(
        ticketId = "TKT-1001",
        userId = "usr_1",
        subject = "Payment Issue",
        description = "UPI transfer pending",
        status = "OPEN"
    )

    private val resolvedTicketWithoutFeedback = SupportTicketEntity(
        ticketId = "TKT-1002",
        userId = "usr_1",
        subject = "KYC help",
        description = "Aadhaar sync",
        status = "RESOLVED",
        feedbackRating = null
    )

    private val sampleLoan = LoanEntity(
        loanId = "loan_1",
        borrowerId = "usr_1",
        lenderId = "usr_2",
        sanctionedAmount = 50000.0,
        purpose = "Business Expansion"
    )

    @BeforeEach
    fun setUp() {
        clearAllMocks()
        every { userRepository.getCurrentUserId() } returns flowOf("usr_1")
        every { userRepository.observeUser("usr_1") } returns flowOf(sampleUser)
        coEvery { userRepository.getCurrentUserIdSync() } returns "usr_1"
        coEvery { userRepository.getUserById("usr_1") } returns sampleUser

        every { supportTicketRepository.getTicketsForUser("usr_1") } returns flowOf(
            listOf(sampleTicket, resolvedTicketWithoutFeedback)
        )
        every { supportTicketRepository.getPendingFeedbackCount("usr_1") } returns flowOf(1)
        every { loanRepository.getLoansByBorrower("usr_1") } returns flowOf(listOf(sampleLoan))
        every { loanRepository.getLoansByLender("usr_1") } returns flowOf(emptyList())
    }

    private fun createViewModel(): SupportTicketViewModel {
        return SupportTicketViewModel(
            supportTicketRepository = supportTicketRepository,
            userRepository = userRepository,
            loanRepository = loanRepository
        )
    }

    // ═══════════════════════════════════════════════════════════════
    // Initialization & State Loading
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Initialization & State Loading")
    inner class InitTests {

        @Test
        fun `init loads user, tickets, loans and pending feedback`() = runTest {
            val viewModel = createViewModel()

            val state = viewModel.uiState.value
            assertThat(state.isLoading).isFalse()
            assertThat(state.user).isEqualTo(sampleUser)
            assertThat(state.tickets).containsExactly(sampleTicket, resolvedTicketWithoutFeedback)
            assertThat(state.pendingFeedbackCount).isEqualTo(1)
            assertThat(state.userLoans).containsExactly(sampleLoan)
        }

        @Test
        fun `init handles null or blank user id gracefully`() = runTest {
            every { userRepository.getCurrentUserId() } returns flowOf(null)

            val viewModel = createViewModel()

            val state = viewModel.uiState.value
            assertThat(state.isLoading).isFalse()
            assertThat(state.user).isNull()
            assertThat(state.tickets).isEmpty()
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // Filtering
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Ticket Filtering")
    inner class FilteringTests {

        @Test
        fun `ALL filter returns all tickets`() = runTest {
            val viewModel = createViewModel()
            viewModel.setFilter("ALL")

            val filtered = viewModel.getFilteredTickets()
            assertThat(filtered).hasSize(2)
        }

        @Test
        fun `PENDING_FEEDBACK filter returns only resolved tickets without rating`() = runTest {
            val viewModel = createViewModel()
            viewModel.setFilter("PENDING_FEEDBACK")

            val filtered = viewModel.getFilteredTickets()
            assertThat(filtered).containsExactly(resolvedTicketWithoutFeedback)
        }

        @Test
        fun `specific status filter returns matching tickets`() = runTest {
            val viewModel = createViewModel()
            viewModel.setFilter("OPEN")

            val filtered = viewModel.getFilteredTickets()
            assertThat(filtered).containsExactly(sampleTicket)
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // Ticket Creation & Validation
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Ticket Creation & Validation")
    inner class TicketCreationTests {

        @Test
        fun `createTicket fails if subject is blank`() = runTest {
            val viewModel = createViewModel()

            viewModel.createTicket(
                category = "PAYMENT_DISPUTE",
                priority = "HIGH",
                subject = "",
                description = "Details about dispute"
            )

            assertThat(viewModel.uiState.value.errorMessage).isEqualTo("Subject and description are required")
            assertThat(viewModel.uiState.value.ticketSubmitted).isFalse()
            coVerify(exactly = 0) { supportTicketRepository.createTicket(any(), any(), any(), any(), any()) }
        }

        @Test
        fun `createTicket succeeds and updates ticketSubmitted to true`() = runTest {
            val viewModel = createViewModel()
            coEvery {
                supportTicketRepository.createTicket(
                    user = any(),
                    category = any(),
                    priority = any(),
                    subject = any(),
                    description = any(),
                    relatedLoanId = any(),
                    attachmentUris = any(),
                    preferredCallbackAt = any()
                )
            } returns sampleTicket

            viewModel.createTicket(
                category = "PAYMENT_DISPUTE",
                priority = "HIGH",
                subject = "Failed transfer",
                description = "Amount deducted but not credited"
            )

            assertThat(viewModel.uiState.value.ticketSubmitted).isTrue()
            assertThat(viewModel.uiState.value.errorMessage).isNull()
        }

        @Test
        fun `clearTicketSubmitted resets the submission flag`() = runTest {
            val viewModel = createViewModel()
            viewModel.clearTicketSubmitted()
            assertThat(viewModel.uiState.value.ticketSubmitted).isFalse()
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // Feedback Submission
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Feedback Submission")
    inner class FeedbackTests {

        @Test
        fun `submitFeedback delegates to repository and sets flag`() = runTest {
            val viewModel = createViewModel()
            coEvery { supportTicketRepository.submitFeedback("TKT-1002", 5, "Great help!") } just Runs

            viewModel.submitFeedback("TKT-1002", 5, "Great help!")

            coVerify { supportTicketRepository.submitFeedback("TKT-1002", 5, "Great help!") }
            assertThat(viewModel.uiState.value.feedbackSubmitted).isTrue()
        }
    }
}
