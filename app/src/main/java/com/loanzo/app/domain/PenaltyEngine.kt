package com.loanzo.app.domain

import com.loanzo.app.data.entity.LoanEntity
import com.loanzo.app.data.entity.RepaymentEntity
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.pow

/**
 * Calculates late payment penalties based on loan terms.
 * Supports PERCENTAGE (simple), COMPOUND, and FLAT penalty models.
 * Caps penalty at RBI-recommended limits.
 *
 * Compound model: penalty = principal * ((1 + rate/100)^months - 1)
 * This means penalties accrue on previously accrued penalties.
 */
@Singleton
class PenaltyEngine @Inject constructor() {

    companion object {
        const val MAX_PENALTY_RATE_MONTHLY = 2.0 // 2% per month max, per RBI guidance
        const val MAX_TOTAL_PENALTY_PERCENT = 100.0 // Penalty cannot exceed 100% of EMI amount
    }

    /**
     * Calculate the penalty for a single overdue repayment.
     * @param repayment The overdue repayment
     * @param penaltyRate The penalty rate configured on the loan (monthly %)
     * @param penaltyModel "PERCENTAGE", "COMPOUND", "FLAT", or "NONE"
     * @param graceDays Number of grace days before penalties begin
     * @param capPercent Maximum penalty allowed as % of repayment amount
     * @return The penalty amount
     */
    fun calculatePenalty(
        repayment: RepaymentEntity,
        penaltyRate: Double,
        penaltyModel: String,
        graceDays: Int = 3,
        capPercent: Double = MAX_TOTAL_PENALTY_PERCENT
    ): Double {
        if (penaltyModel == "NONE" || repayment.status != "OVERDUE" || repayment.penaltyWaived) return 0.0

        val daysOverdue = calculateDaysOverdue(repayment.dueDate)
        if (daysOverdue <= graceDays) return 0.0

        val effectiveDays = daysOverdue - graceDays
        val effectiveRate = penaltyRate.coerceAtMost(MAX_PENALTY_RATE_MONTHLY)

        val calculatedPenalty = when (penaltyModel) {
            "PERCENTAGE" -> {
                // Simple interest: rate is per month, prorate by effective days
                val monthsOverdue = effectiveDays / 30.0
                repayment.amount * (effectiveRate / 100.0) * monthsOverdue
            }
            "COMPOUND" -> {
                // Compound interest: P * ((1 + r)^n - 1) where r is monthly rate, n is months
                val monthsOverdue = effectiveDays / 30.0
                val monthlyRate = effectiveRate / 100.0
                repayment.amount * ((1.0 + monthlyRate).pow(monthsOverdue) - 1.0)
            }
            "FLAT" -> {
                // Flat fee per overdue period (per month)
                val monthsOverdue = (effectiveDays / 30).coerceAtLeast(1)
                penaltyRate * monthsOverdue
            }
            else -> 0.0
        }

        val maxAllowedPenalty = repayment.amount * (capPercent / 100.0)
        return calculatedPenalty.coerceAtMost(maxAllowedPenalty)
    }

    /**
     * Apply penalties to all overdue repayments for a loan.
     * Also auto-marks SCHEDULED repayments as OVERDUE if past due date + grace period.
     * Returns the list of repayments with penalty field populated and statuses updated.
     */
    fun applyPenalties(
        repayments: List<RepaymentEntity>,
        loan: LoanEntity
    ): List<RepaymentEntity> {
        val penaltyRate = loan.penaltyRate
        val penaltyModel = loan.penaltyModel
        val graceDays = loan.penaltyGraceDays
        val capPercent = loan.penaltyCapPercent
        val now = System.currentTimeMillis()

        return repayments.map { repayment ->
            // Auto-mark SCHEDULED -> OVERDUE if past due date
            val effectiveRepayment = if (
                repayment.status == "SCHEDULED" &&
                repayment.dueDate < now
            ) {
                repayment.copy(status = "OVERDUE")
            } else {
                repayment
            }

            if (effectiveRepayment.status == "OVERDUE") {
                val penalty = calculatePenalty(effectiveRepayment, penaltyRate, penaltyModel, graceDays, capPercent)
                effectiveRepayment.copy(penalty = penalty)
            } else {
                effectiveRepayment
            }
        }
    }

    /**
     * Compute total penalty accrued across all overdue repayments for a loan.
     */
    fun totalPenaltyForLoan(
        repayments: List<RepaymentEntity>,
        loan: LoanEntity
    ): Double {
        return applyPenalties(repayments, loan)
            .filter { it.status == "OVERDUE" }
            .sumOf { it.penalty }
    }

    /**
     * Returns a breakdown of penalty details for display in the UI.
     */
    fun getPenaltyBreakdown(
        repayment: RepaymentEntity,
        loan: LoanEntity
    ): PenaltyBreakdown {
        val daysOverdue = calculateDaysOverdue(repayment.dueDate)
        val effectiveDays = (daysOverdue - loan.penaltyGraceDays).coerceAtLeast(0)
        val penalty = calculatePenalty(
            repayment, loan.penaltyRate, loan.penaltyModel,
            loan.penaltyGraceDays, loan.penaltyCapPercent
        )
        val totalDue = repayment.amount + penalty

        return PenaltyBreakdown(
            daysOverdue = daysOverdue,
            effectiveDays = effectiveDays,
            graceDays = loan.penaltyGraceDays,
            penaltyModel = loan.penaltyModel,
            penaltyRate = loan.penaltyRate,
            penaltyAmount = penalty,
            emiAmount = repayment.amount,
            totalDue = totalDue,
            isWaived = repayment.penaltyWaived
        )
    }

    private fun calculateDaysOverdue(dueDate: Long): Int {
        val now = System.currentTimeMillis()
        val diff = now - dueDate
        return (diff / (1000 * 60 * 60 * 24)).toInt().coerceAtLeast(0)
    }
}

/**
 * Data class representing a detailed penalty breakdown for UI display.
 */
data class PenaltyBreakdown(
    val daysOverdue: Int,
    val effectiveDays: Int,
    val graceDays: Int,
    val penaltyModel: String,
    val penaltyRate: Double,
    val penaltyAmount: Double,
    val emiAmount: Double,
    val totalDue: Double,
    val isWaived: Boolean
)
