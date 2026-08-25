package com.loanzo.app.data.dao

/**
 * Room query result class for category-wise spending breakdown.
 */
data class CategoryBreakdown(
    val purposeCategory: String,
    val total: Double
)
