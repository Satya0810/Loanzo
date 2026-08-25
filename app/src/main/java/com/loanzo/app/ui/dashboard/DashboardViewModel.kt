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
    val role: String = "BORROWER",
    val loans: List<LoanEntity> = emptyList(),
    val totalDisbursed: Double = 0.0,
    val totalOutstanding: Double = 0.0,
    val pendingApprovals: List<DisbursementEntity> = emptyList(),
    val overdueRepayments: List<RepaymentEntity> = emptyList(),
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
            val userId = userRepository.getCurrentUserIdSync() ?: return@launch
            val role = userRepository.getCurrentRole().first() ?: "BORROWER"
            val user = userRepository.getUserById(userId)

            _uiState.update { it.copy(user = user, role = role) }

            // Load loans
            val loansFlow = if (role == "LENDER") {
                loanRepository.getLoansByLender(userId)
            } else {
                loanRepository.getLoansByBorrower(userId)
            }

            loansFlow.collect { loans ->
                val totalDisbursed = loans.sumOf { it.disbursedAmount }
                val totalOutstanding = loans.sumOf { it.outstandingAmount }

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        loans = loans,
                        totalDisbursed = totalDisbursed,
                        totalOutstanding = totalOutstanding
                    )
                }
            }
        }

        viewModelScope.launch {
            val userId = userRepository.getCurrentUserIdSync() ?: return@launch
            val role = userRepository.getCurrentRole().first() ?: "BORROWER"

            // Load pending approvals for lenders
            if (role == "LENDER") {
                loanRepository.getPendingApprovalsForLender(userId).collect { approvals ->
                    _uiState.update { it.copy(pendingApprovals = approvals) }
                }
            }
        }

        viewModelScope.launch {
            val userId = userRepository.getCurrentUserIdSync() ?: return@launch
            val role = userRepository.getCurrentRole().first() ?: "BORROWER"

            // Load overdue repayments
            val overdueFlow = if (role == "LENDER") {
                loanRepository.getOverdueRepaymentsForLender(userId)
            } else {
                loanRepository.getOverdueRepaymentsForBorrower(userId)
            }

            overdueFlow.collect { overdue ->
                _uiState.update { it.copy(overdueRepayments = overdue) }
            }
        }

        viewModelScope.launch {
            loanRepository.getRecentEvents(20).collect { events ->
                _uiState.update { it.copy(recentEvents = events) }
            }
        }
    }
}
