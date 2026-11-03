package com.thomas.qwirkle.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColors = lightColorScheme(
    primary = QwirkleBlue,
    onPrimary = Color.White,
    secondary = QwirkleOrange,
    onSecondary = Color.White,
    tertiary = QwirkleGreen,
    onTertiary = Color.White
)

private val DarkColors = darkColorScheme(
    primary = QwirkleBlueLight,
    onPrimary = Color(0xFF0B1F3A),
    secondary = QwirkleOrangeLight,
    onSecondary = Color(0xFF3A1F00),
    tertiary = QwirkleGreenLight,
    onTertiary = Color(0xFF00301A)
)

/** Material-3-Theme mit Hell- und Dunkelmodus (folgt der Systemeinstellung). */
@Composable
fun QwirkleTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            val controller = WindowCompat.getInsetsController(window, view)
            // Dunkle Symbole auf hellem Hintergrund, helle Symbole auf dunklem
            controller.isAppearanceLightStatusBars = !darkTheme
            controller.isAppearanceLightNavigationBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content
    )
}
