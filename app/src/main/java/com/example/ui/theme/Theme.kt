package com.example.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val AerisDarkColorScheme = darkColorScheme(
    primary = CyanNeon,
    onPrimary = HoloBlack,
    primaryContainer = CyanDeep,
    onPrimaryContainer = CyanBright,
    secondary = ElectricBlue,
    onSecondary = TextWhite,
    secondaryContainer = HoloSurfaceVariant,
    onSecondaryContainer = CyanBright,
    tertiary = GoldAccent,
    onTertiary = HoloBlack,
    background = HoloBlack,
    onBackground = TextPrimary,
    surface = HoloSurface,
    onSurface = TextPrimary,
    surfaceVariant = HoloSurfaceVariant,
    onSurfaceVariant = TextSecondary,
    outline = CyanGlow,
    outlineVariant = HoloCardBorder,
    error = AlertRed,
    onError = TextWhite
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false, // Futuristic Cyan + Black aesthetic preserved
    content: @Composable () -> Unit
) {
    val colorScheme = AerisDarkColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            window?.let {
                it.statusBarColor = HoloBlack.toArgb()
                it.navigationBarColor = HoloBlack.toArgb()
                val controller = WindowCompat.getInsetsController(it, view)
                controller.isAppearanceLightStatusBars = false
                controller.isAppearanceLightNavigationBars = false
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
