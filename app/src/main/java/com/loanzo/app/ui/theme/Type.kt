package com.loanzo.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.Hyphens
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.unit.sp

// Using system default fonts (Inter-like)
val LoanzoFontFamily = FontFamily.Default

val LoanzoTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = LoanzoFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 34.sp,
        lineHeight = 40.sp,
        letterSpacing = (-0.5).sp,
        lineBreak = LineBreak.Heading,
        hyphens = Hyphens.None
    ),
    displayMedium = TextStyle(
        fontFamily = LoanzoFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 34.sp,
        letterSpacing = 0.sp,
        lineBreak = LineBreak.Heading,
        hyphens = Hyphens.None
    ),
    displaySmall = TextStyle(
        fontFamily = LoanzoFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        lineHeight = 30.sp,
        letterSpacing = 0.sp,
        lineBreak = LineBreak.Heading,
        hyphens = Hyphens.None
    ),
    headlineLarge = TextStyle(
        fontFamily = LoanzoFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp,
        lineBreak = LineBreak.Heading,
        hyphens = Hyphens.None
    ),
    headlineMedium = TextStyle(
        fontFamily = LoanzoFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 26.sp,
        letterSpacing = 0.sp,
        lineBreak = LineBreak.Heading,
        hyphens = Hyphens.None
    ),
    headlineSmall = TextStyle(
        fontFamily = LoanzoFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.sp,
        lineBreak = LineBreak.Heading,
        hyphens = Hyphens.None
    ),
    titleLarge = TextStyle(
        fontFamily = LoanzoFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.sp,
        lineBreak = LineBreak.Heading,
        hyphens = Hyphens.None
    ),
    titleMedium = TextStyle(
        fontFamily = LoanzoFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.1.sp,
        lineBreak = LineBreak.Heading,
        hyphens = Hyphens.None
    ),
    titleSmall = TextStyle(
        fontFamily = LoanzoFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp,
        lineBreak = LineBreak.Heading,
        hyphens = Hyphens.None
    ),
    bodyLarge = TextStyle(
        fontFamily = LoanzoFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp,
        lineBreak = LineBreak.Paragraph,
        hyphens = Hyphens.None
    ),
    bodyMedium = TextStyle(
        fontFamily = LoanzoFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.25.sp,
        lineBreak = LineBreak.Paragraph,
        hyphens = Hyphens.None
    ),
    bodySmall = TextStyle(
        fontFamily = LoanzoFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.4.sp,
        lineBreak = LineBreak.Paragraph,
        hyphens = Hyphens.None
    ),
    labelLarge = TextStyle(
        fontFamily = LoanzoFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp,
        lineBreak = LineBreak.Heading,
        hyphens = Hyphens.None
    ),
    labelMedium = TextStyle(
        fontFamily = LoanzoFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp,
        lineBreak = LineBreak.Heading,
        hyphens = Hyphens.None
    ),
    labelSmall = TextStyle(
        fontFamily = LoanzoFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 10.sp,
        lineHeight = 14.sp,
        letterSpacing = 0.5.sp,
        lineBreak = LineBreak.Heading,
        hyphens = Hyphens.None
    )
)
