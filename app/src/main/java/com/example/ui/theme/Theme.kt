package com.example.ui.theme

import android.app.Activity
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

val LocalWaterRiderColors = staticCompositionLocalOf<AppThemeColors> { Theme1DaylightRoyal }

object WaterRiderTheme {
    val colors: AppThemeColors
        @Composable
        @ReadOnlyComposable
        get() = LocalWaterRiderColors.current
}

val WaterRiderShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(24.dp)
)

private fun AppThemeColors.toMaterialColorScheme(): ColorScheme {
    return if (isDark) {
        darkColorScheme(
            primary = primaryAccent,
            onPrimary = onPrimaryAccent,
            primaryContainer = primaryContainer,
            onPrimaryContainer = onPrimaryContainer,
            secondary = secondaryAccent,
            onSecondary = onSecondaryAccent,
            secondaryContainer = secondaryContainer,
            onSecondaryContainer = onSecondaryContainer,
            background = background,
            onBackground = textPrimary,
            surface = surface,
            onSurface = textPrimary,
            surfaceVariant = surfaceVariant,
            onSurfaceVariant = textSecondary,
            outline = border,
            outlineVariant = border.copy(alpha = 0.85f),
            error = utang,
            onError = onUtang,
            errorContainer = utangContainer,
            onErrorContainer = utangSubtext
        )
    } else {
        lightColorScheme(
            primary = primaryAccent,
            onPrimary = onPrimaryAccent,
            primaryContainer = primaryContainer,
            onPrimaryContainer = onPrimaryContainer,
            secondary = secondaryAccent,
            onSecondary = onSecondaryAccent,
            secondaryContainer = secondaryContainer,
            onSecondaryContainer = onSecondaryContainer,
            background = background,
            onBackground = textPrimary,
            surface = surface,
            onSurface = textPrimary,
            surfaceVariant = surfaceVariant,
            onSurfaceVariant = textSecondary,
            outline = border,
            outlineVariant = border.copy(alpha = 0.85f),
            error = utang,
            onError = onUtang,
            errorContainer = utangContainer,
            onErrorContainer = utangSubtext
        )
    }
}

private val Theme1MaterialScheme = Theme1DaylightRoyal.toMaterialColorScheme()
private val Theme2MaterialScheme = Theme2MidnightCarbon.toMaterialColorScheme()
private val Theme3MaterialScheme = Theme3AlkalineEmerald.toMaterialColorScheme()
private val Theme4MaterialScheme = Theme4NavyGold.toMaterialColorScheme()
private val Theme5MaterialScheme = Theme5TwilightOcean.toMaterialColorScheme()

fun materialColorSchemeForTheme(appTheme: AppThemeId): ColorScheme = when (appTheme) {
    AppThemeId.ROYAL_DAYLIGHT -> Theme1MaterialScheme
    AppThemeId.MIDNIGHT_CARBON -> Theme2MaterialScheme
    AppThemeId.ALKALINE_EMERALD -> Theme3MaterialScheme
    AppThemeId.NAVY_GOLD -> Theme4MaterialScheme
    AppThemeId.TWILIGHT_OCEAN -> Theme5MaterialScheme
}

@Composable
fun MyApplicationTheme(
    appTheme: AppThemeId = AppThemeId.ROYAL_DAYLIGHT,
    content: @Composable () -> Unit,
) {
    val palette = paletteForTheme(appTheme)
    val colorScheme = materialColorSchemeForTheme(appTheme)

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val activity = view.context as? Activity
            val window = activity?.window
            if (window != null) {
                WindowCompat.getInsetsController(window, view).apply {
                    isAppearanceLightStatusBars = !palette.isDark
                    isAppearanceLightNavigationBars = !palette.isDark
                }
            }
        }
    }

    CompositionLocalProvider(LocalWaterRiderColors provides palette) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            shapes = WaterRiderShapes,
            content = content
        )
    }
}
