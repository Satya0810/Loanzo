package com.loanzo.app.data.network

import com.loanzo.app.data.entity.LoanEntity
import com.loanzo.app.data.entity.UserEntity
import kotlinx.coroutines.delay
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LeegalityService @Inject constructor() {
    suspend fun createSigningWorkflow(loan: LoanEntity, borrower: UserEntity): String? {
        delay(1000)
        return "https://sandbox.leegality.com/sign/mock-workflow-id-${loan.loanId}"
    }
}
