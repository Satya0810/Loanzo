package com.loanzo.app.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = Gold500,
    onPrimary = Navy900,
    primaryContainer = Navy800,
    onPrimaryContainer = Gold400,
    secondary = Blue400,
    onSecondary = Navy900,
    secondaryContainer = Navy700,
    onSecondaryContainer = Blue400,
    tertiary = Emerald400,
    onTertiary = Navy900,
    tertiaryContainer = Navy700,
    onTertiaryContainer = Emerald400,
    error = Red400,
    onError = Navy900,
    errorContainer = Navy700,
    onErrorContainer = Red400,
    background = Navy900,
    onBackground = Gray100,
    surface = SurfaceDarkElevated,
    onSurface = Gray100,
    surfaceVariant = SurfaceDarkCard,
    onSurfaceVariant = Gray400,
    outline = Gray800,
    outlineVariant = Color(0xFF161F30),
    inverseSurface = Gray100,
    inverseOnSurface = Navy900,
    inversePrimary = Gold400,
    surfaceTint = Gold500
)

private val LightColorScheme = lightColorScheme(
    primary = BrandRoyalBlue, // Vibrant Royal Cobalt Blue (from app logo background)
    onPrimary = Color.White,
    primaryContainer = BrandIceBlue, // Soft Ice Blue (#EFF6FF)
    onPrimaryContainer = Color(0xFF1E3A8A), // Deep Navy Blue for text on Ice Blue
    secondary = BrandAmberGold, // Warm Amber Gold (#F59E0B from logo growth arrow)
    onSecondary = Color.White,
    secondaryContainer = BrandGoldLight, // Soft Gold Tint (#FFFBEB)
    onSecondaryContainer = BrandGoldDark,
    tertiary = Emerald600, // Fresh Mint Emerald
    onTertiary = Color.White,
    tertiaryContainer = EmeraldLight,
    onTertiaryContainer = Emerald600,
    error = Red500,
    onError = Color.White,
    errorContainer = RedLight,
    onErrorContainer = Red500,
    background = CanvasPorcelain, // #F8FAFD Luminous porcelain canvas with soft blue undertone
    onBackground = TextNavyDark, // #0F172A Midnight Navy
    surface = Color.White, // Pure #FFFFFF crisp white cards
    onSurface = TextNavyDark, // #0F172A
    surfaceVariant = Color(0xFFF1F5F9), // Layer 2 neutral container
    onSurfaceVariant = TextSlateMedium, // #334155 (clean readable body text)
    outline = Color(0xFFCBD5E1), // Slate 300
    outlineVariant = Color(0xFFE2E8F0), // Clean 1.dp hairline border
    inverseSurface = Gray800,
    inverseOnSurface = Gray100,
    inversePrimary = BrandCobalt,
    surfaceTint = BrandRoyalBlue
)

@Composable
fun LoanzoTheme(
    darkTheme: Boolean = false, // Pristine Light Theme as original default
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            var ctx = view.context
            while (ctx is android.content.ContextWrapper) {
                if (ctx is Activity) break
                ctx = ctx.baseContext
            }
            val window = (ctx as? Activity)?.window
            if (window != null) {
                window.statusBarColor = colorScheme.background.toArgb()
                window.navigationBarColor = colorScheme.background.toArgb()
                WindowCompat.getInsetsController(window, view).apply {
                    isAppearanceLightStatusBars = !darkTheme
                    isAppearanceLightNavigationBars = !darkTheme
                }
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = LoanzoTypography,
        content = content
    )
}
