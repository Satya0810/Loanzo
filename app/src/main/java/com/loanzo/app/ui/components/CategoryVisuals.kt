package com.loanzo.app.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.loanzo.app.ui.theme.*

data class CategoryStyle(
    val icon: ImageVector,
    val iconColor: Color,
    val bgColor: Color,
    val label: String
)

fun getCategoryStyle(typeOrPurpose: String): CategoryStyle {
    val lower = typeOrPurpose.uppercase()
    return when {
        lower.contains("MEDICAL") || lower.contains("HEALTH") -> CategoryStyle(
            icon = Icons.Default.MedicalServices,
            iconColor = CategoryMedical,
            bgColor = CategoryMedicalBg,
            label = "Medical"
        )
        lower.contains("EDU") || lower.contains("COLLEGE") || lower.contains("SCHOOL") -> CategoryStyle(
            icon = Icons.Default.School,
            iconColor = CategoryEducation,
            bgColor = CategoryEducationBg,
            label = "Education"
        )
        lower.contains("BUSIN") || lower.contains("STARTUP") || lower.contains("COMMERC") -> CategoryStyle(
            icon = Icons.Default.Storefront,
            iconColor = CategoryBusiness,
            bgColor = CategoryBusinessBg,
            label = "Business"
        )
        lower.contains("HOME") || lower.contains("HOUSE") || lower.contains("RENT") -> CategoryStyle(
            icon = Icons.Default.Home,
            iconColor = CategoryHousing,
            bgColor = CategoryHousingBg,
            label = "Housing"
        )
        lower.contains("AGRI") || lower.contains("FARM") || lower.contains("CROP") -> CategoryStyle(
            icon = Icons.Default.Eco,
            iconColor = CategoryAgriculture,
            bgColor = CategoryAgricultureBg,
            label = "Agriculture"
        )
        lower.contains("PERS") || lower.contains("EMERG") -> CategoryStyle(
            icon = Icons.Default.Person,
            iconColor = CategoryPersonal,
            bgColor = CategoryPersonalBg,
            label = "Personal"
        )
        else -> CategoryStyle(
            icon = Icons.Default.Payments,
            iconColor = CategoryOther,
            bgColor = CategoryOtherBg,
            label = if (typeOrPurpose.isNotBlank()) typeOrPurpose else "General"
        )
    }
}
