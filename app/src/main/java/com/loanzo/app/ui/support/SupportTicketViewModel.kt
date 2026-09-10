package com.loanzo.app.ui.support

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.loanzo.app.data.entity.LoanEntity
import com.loanzo.app.data.entity.SupportTicketEntity
import com.loanzo.app.data.entity.UserEntity
import com.loanzo.app.data.repository.LoanRepository
import com.loanzo.app.data.repository.SupportTicketRepository
import com.loanzo.app.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SupportTicketUiState(
    val isLoading: Boolean = true,
    val user: UserEntity? = null,
    val tickets: List<SupportTicketEntity> = emptyList(),
    val selectedTicket: SupportTicketEntity? = null,
    val userLoans: List<LoanEntity> = emptyList(),
    val pendingFeedbackCount: Int = 0,
    val selectedFilter: String = "ALL",
    val ticketSubmitted: Boolean = false,
    val feedbackSubmitted: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class SupportTicketViewModel @Inject constructor(
    private val supportTicketRepository: SupportTicketRepository,
    private val userRepository: UserRepository,
    private val loanRepository: LoanRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SupportTicketUiState())
    val uiState: StateFlow<SupportTicketUiState> = _uiState.asStateFlow()

    init {
        loadUserAndTickets()
    }

    private fun loadUserAndTickets() {
        viewModelScope.launch {
            userRepository.getCurrentUserId().collectLatest { userId ->
                if (userId.isNullOrBlank()) {
                    _uiState.update { it.copy(isLoading = false) }
                    return@collectLatest
                }

                // Load user
                launch {
                    userRepository.observeUser(userId).collect { user ->
                        _uiState.update { it.copy(user = user) }
                    }
                }

                // Load user's tickets
                launch {
                    supportTicketRepository.getTicketsForUser(userId).collect { tickets ->
                        _uiState.update { it.copy(isLoading = false, tickets = tickets) }
                    }
                }

                // Load pending feedback count
                launch {
                    supportTicketRepository.getPendingFeedbackCount(userId).collect { count ->
                        _uiState.update { it.copy(pendingFeedbackCount = count) }
                    }
                }

                // Load user's loans (for linking a ticket to a loan)
                launch {
                    loanRepository.getLoansByBorrower(userId).combine(
                        loanRepository.getLoansByLender(userId)
                    ) { borrowerLoans, lenderLoans ->
                        (borrowerLoans + lenderLoans).distinctBy { it.loanId }
                    }.collect { loans ->
                        _uiState.update { it.copy(userLoans = loans) }
                    }
                }
            }
        }
    }

    fun setFilter(filter: String) {
        _uiState.update { it.copy(selectedFilter = filter) }
    }

    fun getFilteredTickets(): List<SupportTicketEntity> {
        val state = _uiState.value
        return when (state.selectedFilter) {
            "ALL" -> state.tickets
            "PENDING_FEEDBACK" -> state.tickets.filter { it.status == "RESOLVED" && it.feedbackRating == null }
            else -> state.tickets.filter { it.status == state.selectedFilter }
        }
    }

    fun loadTicketDetail(ticketId: String) {
        viewModelScope.launch {
            supportTicketRepository.observeTicket(ticketId).collect { ticket ->
                _uiState.update { it.copy(selectedTicket = ticket) }
            }
        }
    }

    fun createTicket(
        category: String,
        priority: String,
        subject: String,
        description: String,
        relatedLoanId: String? = null,
        preferredCallbackAt: Long? = null
    ) {
        if (subject.isBlank() || description.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Subject and description are required") }
            return
        }
        viewModelScope.launch {
            val user = _uiState.value.user 
                ?: userRepository.getCurrentUserIdSync()?.let { userRepository.getUserById(it) }
                ?: UserEntity(
                    userId = "usr_guest_" + System.currentTimeMillis().toString().takeLast(6),
                    name = "Verified User",
                    email = "",
                    phone = "+91 70615 59039",
                    role = "BORROWER"
                )
            try {
                supportTicketRepository.createTicket(
                    user = user,
                    category = category,
                    priority = priority,
                    subject = subject,
                    description = description,
                    relatedLoanId = relatedLoanId,
                    preferredCallbackAt = preferredCallbackAt
                )
                _uiState.update { it.copy(ticketSubmitted = true, errorMessage = null) }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Failed to submit ticket: ${e.message}") }
            }
        }
    }

    fun submitFeedback(ticketId: String, rating: Int, comment: String?) {
        viewModelScope.launch {
            try {
                supportTicketRepository.submitFeedback(ticketId, rating, comment)
                _uiState.update { it.copy(feedbackSubmitted = true) }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Failed to submit feedback: ${e.message}") }
            }
        }
    }

    fun clearTicketSubmitted() {
        _uiState.update { it.copy(ticketSubmitted = false) }
    }

    fun clearFeedbackSubmitted() {
        _uiState.update { it.copy(feedbackSubmitted = false) }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
