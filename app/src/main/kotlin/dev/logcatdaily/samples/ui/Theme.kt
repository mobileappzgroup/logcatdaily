package dev.logcatdaily.samples.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Bg0 = Color(0xFF0B0E13)
private val Bg1 = Color(0xFF12161D)
private val Accent = Color(0xFF16C79A)
private val Amber = Color(0xFFFFB454)
private val TextPrimary = Color(0xFFF2F5F8)
private val TextSecondary = Color(0xFF9AA4B2)
private val ErrorRed = Color(0xFFFF5C5C)

private val LogcatDailyColors = darkColorScheme(
    background = Bg0,
    onBackground = TextPrimary,
    surface = Bg1,
    onSurface = TextPrimary,
    primary = Accent,
    onPrimary = Bg0,
    secondary = Amber,
    onSecondary = Bg0,
    error = ErrorRed,
    onError = TextPrimary,
    outline = TextSecondary,
)

@Composable
fun LogcatDailyTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LogcatDailyColors,
        content = content,
    )
}
