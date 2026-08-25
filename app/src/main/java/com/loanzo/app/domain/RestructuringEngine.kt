package com.loanzo.app.domain

import com.loanzo.app.data.entity.LoanEntity
import com.loanzo.app.util.calculateEMI
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Handles loan restructuring (tenure extension) and moratorium (EMI holiday).
 */
@Singleton
class RestructuringEngine @Inject constructor() {

    /**
     * Restructure a loan by changing the tenure.
     * Recalculates EMI based on remaining outstanding and new tenure.
     */
    fun restructureLoan(loan: LoanEntity, newTenureMonths: Int): LoanEntity {
        return loan.copy(
            originalTenureMonths = if (loan.originalTenureMonths == 0) loan.tenureMonths else loan.originalTenureMonths,
            tenureMonths = newTenureMonths,
            isRestructured = true,
            restructuredAt = System.currentTimeMillis()
        )
    }

    /**
     * Apply a moratorium (payment holiday) to a loan.
     * Extends the tenure by the moratorium period.
     * If capitalizeInterest is true, accrued interest during moratorium is added to outstanding.
     */
    fun applyMoratorium(
        loan: LoanEntity,
        moratoriumMonths: Int,
        capitalizeInterest: Boolean = false
    ): LoanEntity {
        val extendedTenure = loan.tenureMonths + moratoriumMonths
        var newOutstanding = loan.outstandingAmount

        if (capitalizeInterest) {
            // Capitalize interest for the moratorium period
            val monthlyRate = loan.interestRate / 12.0 / 100.0
            for (i in 1..moratoriumMonths) {
                newOutstanding += newOutstanding * monthlyRate
            }
        }

        return loan.copy(
            originalTenureMonths = if (loan.originalTenureMonths == 0) loan.tenureMonths else loan.originalTenureMonths,
            tenureMonths = extendedTenure,
            moratoriumMonths = loan.moratoriumMonths + moratoriumMonths,
            outstandingAmount = newOutstanding,
            isRestructured = true,
            restructuredAt = System.currentTimeMillis()
        )
    }

    /**
     * Calculate the new EMI after restructuring.
     */
    fun calculateNewEMI(loan: LoanEntity): Double {
        return calculateEMI(
            principal = loan.outstandingAmount,
            annualRate = loan.interestRate,
            tenureMonths = loan.tenureMonths
        )
    }
}
