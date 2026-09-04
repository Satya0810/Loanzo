package com.loanzo.app.ui.theme

import androidx.compose.ui.graphics.Color

// Official Loanzo Brand Palette (Extracted directly from App Logo app_logo.png)
// 1. Royal Cobalt & Sapphire Blue (Logo Background & Core Brand Color)
val BrandRoyalBlue = Color(0xFF1D4ED8) // Rich Royal Cobalt Blue (Primary CTA & Brand Anchor)
val BrandCobalt = Color(0xFF2563EB) // Bright Cobalt (Interactive & Hover)
val BrandSapphire = Color(0xFF0284C7) // Vibrant Sapphire Blue
val BrandIceBlue = Color(0xFFEFF6FF) // Layer-2 Pill & Tile Container
val BrandIceBorder = Color(0xFFBFDBFE) // Layer-2 Hairline Border

// 2. Golden Coin Stack & Solar Orange (Extracted directly from App Logo app_logo.png)
val GoldCoinBright = Color(0xFFFBBF24) // Bright Gold: High-impact numerical values, EMI, and primary stats
val GoldCoinRich = Color(0xFFF59E0B)   // Rich Gold: Middle coin accent, primary gold icons, active badges
val GoldCoinAmber = Color(0xFFD97706)  // Deep Amber: Coin ridges, high-contrast text, selected chip labels
val GoldCoinCream = Color(0xFFFEF9C3)  // Selected chip & container background tint (#FEF9C3 / #FFFBEB)
val GoldCoinBorder = Color(0xFFFDE68A) // Selected chip & active element hairline border

val BrandAmberGold = GoldCoinRich     // Radiant Amber Gold
val BrandSolarOrange = Color(0xFFF97316) // Energetic Growth Accent
val BrandGoldDark = GoldCoinAmber     // Deep Amber for text contrast
val BrandGoldLight = GoldCoinCream    // Layer-2 Gold Pill & Tile Container
val BrandGoldBorder = GoldCoinBorder  // Layer-2 Gold Border

// 3. Canvas & Text Tokens
val CanvasPorcelain = Color(0xFFF8FAFD) // Luminous Light Canvas with soft blue undertone
val TextNavyDark = Color(0xFF0F172A) // Deep Midnight Navy for headings
val TextSlateMedium = Color(0xFF334155) // Slate 700 for readable body text
val TextSlateMuted = Color(0xFF64748B) // Slate 500 for captions and meta

// Primary palette — Deep Matte Slate & Obsidian (Dark Mode)
val Navy900 = Color(0xFF0B0F19) // Deep Matte Slate (Replaces harsh cyber #0A1628)
val Navy800 = Color(0xFF111827) // Elevated Slate Surface
val Navy700 = Color(0xFF1E293B) // Card Border / Deep Element
val Navy600 = Color(0xFF334155) // Slate Muted
val Navy500 = Color(0xFF475569) // Neutral Slate

// Accent palette — Champagne Gold & Royal Sapphire
val Gold400 = Color(0xFFE5C07B)
val Gold500 = Color(0xFFD4AF37) // Warm Champagne Gold (Replaces neon traffic yellow #FFC107)
val Gold600 = Color(0xFFC59B27)
val GoldLight = Color(0xFFFFFBEB)

// Success — Refined Emerald Mint (Positive yields, verified status, incoming payments)
val Emerald400 = Color(0xFF10B981) // Crisp Mint (Replaces neon green #4CAF50)
val Emerald500 = Color(0xFF059669)
val Emerald600 = Color(0xFF047857)
val EmeraldLight = Color(0xFFECFDF5)

// Warning — Warm Amber
val Orange400 = Color(0xFFF59E0B)
val Orange500 = Color(0xFFD97706)
val OrangeLight = Color(0xFFFFFBEB)

// Danger — Soft Crimson
val Red400 = Color(0xFFEF4444)
val Red500 = Color(0xFFDC2626)
val RedLight = Color(0xFFFEF2F2)

// Info — Trust Royal Blue
val Blue400 = Color(0xFF3B82F6)
val Blue500 = Color(0xFF2563EB)
val BlueLight = Color(0xFFEFF6FF)

// Neutral
val Gray50 = Color(0xFFF8FAFC)
val Gray100 = Color(0xFFF1F5F9)
val Gray200 = Color(0xFFE2E8F0)
val Gray300 = Color(0xFFCBD5E1)
val Gray400 = Color(0xFF94A3B8)
val Gray500 = Color(0xFF64748B)
val Gray600 = Color(0xFF475569)
val Gray700 = Color(0xFF334155)
val Gray800 = Color(0xFF1E293B)
val Gray900 = Color(0xFF0F172A)

// Surface colors for dark mode (Matte Obsidian & Slate)
val SurfaceDark = Color(0xFF0B0F19)
val SurfaceDarkElevated = Color(0xFF111827)
val SurfaceDarkCard = Color(0xFF161F30)

// Surface colors for light mode (Pristine White & Soft Slate)
val SurfaceLight = Color(0xFFF8FAFC)
val SurfaceLightCard = Color(0xFFFFFFFF)

// Subtle Hairline Overlays & Borders (Zero Wireframe Glowing)
val GlassOverlay = Color(0x0AFFFFFF) // 4% subtle white sheen
val GlassOverlayDark = Color(0x14FFFFFF) // 8% white
val GlassBorder = Color(0x1AFFFFFF) // 10% subtle white hairline border (replaces harsh 20% white)

// Status colors
val StatusVerified = Emerald400
val StatusPending = Gold500
val StatusFlagged = Orange400
val StatusRejected = Red400
val StatusMismatch = Orange500
val StatusBlocked = Red500
val StatusAutoApproved = Emerald500

// Category colors for Pocket-Log style visual tagging
val CategoryMedical = Color(0xFF0284C7)
val CategoryMedicalBg = Color(0x140284C7)
val CategoryEducation = Color(0xFF2563EB)
val CategoryEducationBg = Color(0x142563EB)
val CategoryBusiness = Color(0xFFD97706)
val CategoryBusinessBg = Color(0x14D97706)
val CategoryPersonal = Color(0xFFDB2777)
val CategoryPersonalBg = Color(0x14DB2777)
val CategoryHousing = Color(0xFF7C3AED)
val CategoryHousingBg = Color(0x147C3AED)
val CategoryAgriculture = Color(0xFF059669)
val CategoryAgricultureBg = Color(0x14059669)
val CategoryOther = Color(0xFF475569)
val CategoryOtherBg = Color(0x14475569)

// Standard text colors
val TextPrimary = Color(0xFF0F172A)
val TextSecondary = Color(0xFF64748B)
