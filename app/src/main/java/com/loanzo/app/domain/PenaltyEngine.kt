package com.loanzo.app.domain

import com.loanzo.app.data.entity.LoanEntity
import com.loanzo.app.data.entity.RepaymentEntity
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Calculates late payment penalties based on loan terms.
 * Supports PERCENTAGE and FLAT penalty models.
 * Caps penalty at RBI-recommended limits.
 */
@Singleton
class PenaltyEngine @Inject constructor() {

    companion object {
        const val MAX_PENALTY_RATE_MONTHLY = 2.0 // 2% per month max, per RBI guidance
    }

    /**
     * Calculate the penalty for a single overdue repayment.
     * @param repayment The overdue repayment
     * @param penaltyRate The penalty rate configured on the loan
     * @param penaltyModel "PERCENTAGE" or "FLAT" or "NONE"
     * @return The penalty amount
     */
    fun calculatePenalty(
        repayment: RepaymentEntity,
        penaltyRate: Double,
        penaltyModel: String
    ): Double {
        if (penaltyModel == "NONE" || repayment.status != "OVERDUE") return 0.0

        val daysOverdue = calculateDaysOverdue(repayment.dueDate)
        if (daysOverdue <= 0) return 0.0

        return when (penaltyModel) {
            "PERCENTAGE" -> {
                // Rate is per month, prorate by days
                val monthsOverdue = daysOverdue / 30.0
                val rawPenalty = repayment.amount * (penaltyRate / 100.0) * monthsOverdue
                // Cap at MAX_PENALTY_RATE_MONTHLY * months * principal
                val maxPenalty = repayment.amount * (MAX_PENALTY_RATE_MONTHLY / 100.0) * monthsOverdue
                rawPenalty.coerceAtMost(maxPenalty)
            }
            "FLAT" -> {
                // Flat fee per overdue period (per month)
                val monthsOverdue = (daysOverdue / 30).coerceAtLeast(1)
                penaltyRate * monthsOverdue
            }
            else -> 0.0
        }
    }

    /**
     * Apply penalties to all overdue repayments for a loan.
     * Returns the list of repayments with penalty field populated.
     */
    fun applyPenalties(
        repayments: List<RepaymentEntity>,
        loan: LoanEntity
    ): List<RepaymentEntity> {
        val penaltyRate = loan.penaltyRate
        val penaltyModel = loan.penaltyModel
        
        return repayments.map { repayment ->
            if (repayment.status == "OVERDUE") {
                val penalty = calculatePenalty(repayment, penaltyRate, penaltyModel)
                repayment.copy(penalty = penalty)
            } else {
                repayment
            }
        }
    }

    private fun calculateDaysOverdue(dueDate: Long): Int {
        val now = System.currentTimeMillis()
        val diff = now - dueDate
        return (diff / (1000 * 60 * 60 * 24)).toInt()
    }
}
