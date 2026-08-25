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
    primaryContainer = Navy600,
    onPrimaryContainer = Gold400,
    secondary = Blue400,
    onSecondary = Navy900,
    secondaryContainer = Navy700,
    onSecondaryContainer = Blue400,
    tertiary = Emerald400,
    onTertiary = Navy900,
    tertiaryContainer = Emerald600,
    onTertiaryContainer = EmeraldLight,
    error = Red400,
    onError = Navy900,
    errorContainer = Red500,
    onErrorContainer = RedLight,
    background = Navy900,
    onBackground = Gray100,
    surface = SurfaceDark,
    onSurface = Gray100,
    surfaceVariant = SurfaceDarkElevated,
    onSurfaceVariant = Gray300,
    outline = Gray600,
    outlineVariant = Gray700,
    inverseSurface = Gray100,
    inverseOnSurface = Navy900,
    inversePrimary = Navy600,
    surfaceTint = Gold500
)

private val LightColorScheme = lightColorScheme(
    primary = Navy600,
    onPrimary = Color.White,
    primaryContainer = BlueLight,
    onPrimaryContainer = Navy800,
    secondary = Gold600,
    onSecondary = Color.White,
    secondaryContainer = GoldLight,
    onSecondaryContainer = Navy800,
    tertiary = Emerald500,
    onTertiary = Color.White,
    tertiaryContainer = EmeraldLight,
    onTertiaryContainer = Emerald600,
    error = Red500,
    onError = Color.White,
    errorContainer = RedLight,
    onErrorContainer = Red500,
    background = SurfaceLight,
    onBackground = Gray900,
    surface = SurfaceLightCard,
    onSurface = Gray900,
    surfaceVariant = Gray100,
    onSurfaceVariant = Gray600,
    outline = Gray300,
    outlineVariant = Gray200,
    inverseSurface = Gray800,
    inverseOnSurface = Gray100,
    inversePrimary = Gold400,
    surfaceTint = Navy600
)

@Composable
fun LoanzoTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            window.navigationBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = LoanzoTypography,
        content = content
    )
}
