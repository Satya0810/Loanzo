package com.loanzo.app.data.dao

/**
 * Room query result class for monthly spending trend.
 */
data class MonthlySpending(
    val monthLabel: String,
    val total: Double
)
