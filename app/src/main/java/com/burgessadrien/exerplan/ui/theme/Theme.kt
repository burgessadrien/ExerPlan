
package com.burgessadrien.exerplan.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val AmoledDarkColorScheme = darkColorScheme(
    primary = GreenAccent,
    secondary = PurpleGrey80,
    tertiary = Pink80,
    background = Black, // Pure black for AMOLED
    surface = DarkGrey, // Slightly off-black for surfaces
    onPrimary = Black,
    onSecondary = Black,
    onTertiary = Black,
    onBackground = LightGrey,
    onSurface = LightGrey
)

@Composable
fun ExerPlanTheme(
    content: @Composable () -> Unit
) {
    val colorScheme = AmoledDarkColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            window.navigationBarColor = colorScheme.background.toArgb()
            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = false
            controller.isAppearanceLightNavigationBars = false
        }
    }

    CompositionLocalProvider(LocalSpacing provides Spacing()) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
