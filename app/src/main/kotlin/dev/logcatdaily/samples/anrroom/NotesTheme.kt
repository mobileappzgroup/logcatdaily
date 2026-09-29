package dev.logcatdaily.samples.anrroom

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// Light theme for this sample only. The app-wide theme stays dark.
private val NotesColors = lightColorScheme(
    primary = Color(0xFF16C79A),
    // Dark text on the green: white on #16C79A is only about 2:1.
    onPrimary = Color(0xFF06231B),
    background = Color(0xFFF5F7F6),
    onBackground = Color(0xFF14201C),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF14201C),
    onSurfaceVariant = Color(0xFF55625D),
    outline = Color(0xFFB9C4BF),
    primaryContainer = Color(0xFFD5F5EC),
    onPrimaryContainer = Color(0xFF0A5C48),
)

@Composable
fun NotesTheme(content: @Composable () -> Unit) {
    val view = LocalView.current
    SideEffect {
        // Dark clock and battery icons on the light background.
        val window = (view.context as Activity).window
        WindowCompat.getInsetsController(window, view).apply {
            isAppearanceLightStatusBars = true
            isAppearanceLightNavigationBars = true
        }
    }
    MaterialTheme(colorScheme = NotesColors, content = content)
}
