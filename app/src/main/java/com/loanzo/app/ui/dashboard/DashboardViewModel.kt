package com.loanzo.app.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.loanzo.app.data.entity.*
import com.loanzo.app.data.repository.LoanRepository
import com.loanzo.app.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DashboardUiState(
    val isLoading: Boolean = true,
    val user: UserEntity? = null,
    val loansAsBorrower: List<LoanEntity> = emptyList(),
    val loansAsLender: List<LoanEntity> = emptyList(),
    val totalBorrowedDisbursed: Double = 0.0,
    val totalBorrowedOutstanding: Double = 0.0,
    val totalLentDisbursed: Double = 0.0,
    val totalLentOutstanding: Double = 0.0,
    val pendingApprovals: List<DisbursementEntity> = emptyList(),
    val overdueRepaymentsAsBorrower: List<RepaymentEntity> = emptyList(),
    val overdueRepaymentsAsLender: List<RepaymentEntity> = emptyList(),
    val recentEvents: List<AuditEventEntity> = emptyList()
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val loanRepository: LoanRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        loadDashboard()
    }

    fun loadDashboard() {
        viewModelScope.launch {
            userRepository.getCurrentUserId().collectLatest { userId ->
                if (userId.isNullOrBlank()) {
                    _uiState.update { it.copy(isLoading = false) }
                    return@collectLatest
                }

                // Observe UserEntity continuously
                launch {
                    userRepository.observeUser(userId).collect { user ->
                        _uiState.update { it.copy(user = user) }
                    }
                }

                // Load loans as borrower
                launch {
                    loanRepository.getLoansByBorrower(userId).collect { loans ->
                        val totalDisbursed = loans.sumOf { it.disbursedAmount }
                        val totalOutstanding = loans.sumOf { it.outstandingAmount }
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                loansAsBorrower = loans,
                                totalBorrowedDisbursed = totalDisbursed,
                                totalBorrowedOutstanding = totalOutstanding
                            )
                        }
                    }
                }

                // Load loans as lender
                launch {
                    loanRepository.getLoansByLender(userId).collect { loans ->
                        val totalDisbursed = loans.sumOf { it.disbursedAmount }
                        val totalOutstanding = loans.sumOf { it.outstandingAmount }
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                loansAsLender = loans,
                                totalLentDisbursed = totalDisbursed,
                                totalLentOutstanding = totalOutstanding
                            )
                        }
                    }
                }

                // Load pending approvals for lenders
                launch {
                    loanRepository.getPendingApprovalsForLender(userId).collect { approvals ->
                        _uiState.update { it.copy(pendingApprovals = approvals) }
                    }
                }

                // Load overdue repayments as borrower
                launch {
                    loanRepository.getOverdueRepaymentsForBorrower(userId).collect { overdue ->
                        _uiState.update { it.copy(overdueRepaymentsAsBorrower = overdue) }
                    }
                }

                // Load overdue repayments as lender
                launch {
                    loanRepository.getOverdueRepaymentsForLender(userId).collect { overdue ->
                        _uiState.update { it.copy(overdueRepaymentsAsLender = overdue) }
                    }
                }

                // Load recent events
                launch {
                    loanRepository.getRecentEvents(20).collect { events ->
                        _uiState.update { it.copy(recentEvents = events) }
                    }
                }
            }
        }
    }
}
