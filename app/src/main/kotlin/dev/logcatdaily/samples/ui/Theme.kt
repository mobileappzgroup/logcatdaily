package dev.logcatdaily.samples.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// Seed: logcat green #16C79A (strategy/BRAND.md). Amber #FFB454 is the highlight.
private val Green = Color(0xFF16C79A)
private val Amber = Color(0xFFFFB454)
private val ErrorRed = Color(0xFFFF5C5C)

private val DarkColors = darkColorScheme(
    primary = Green,
    onPrimary = Color(0xFF00382B),
    primaryContainer = Color(0xFF005140),
    onPrimaryContainer = Color(0xFF6FF7D0),
    secondary = Amber,
    onSecondary = Color(0xFF2E1C00),
    secondaryContainer = Color(0xFF4A3300),
    onSecondaryContainer = Color(0xFFFFDDB0),
    tertiary = Color(0xFF8DB4FF),
    onTertiary = Color(0xFF00255E),
    tertiaryContainer = Color(0xFF1B3C78),
    onTertiaryContainer = Color(0xFFD8E3FF),
    error = ErrorRed,
    onError = Color(0xFF3B0007),
    errorContainer = Color(0xFF5E1520),
    onErrorContainer = Color(0xFFFFDAD9),
    background = Color(0xFF0A0F0E),
    onBackground = Color(0xFFF2F5F8),
    surface = Color(0xFF111A17),
    onSurface = Color(0xFFF2F5F8),
    surfaceVariant = Color(0xFF1E2B27),
    onSurfaceVariant = Color(0xFFB4C3BD),
    surfaceContainerLowest = Color(0xFF070B0A),
    surfaceContainerLow = Color(0xFF0F1614),
    surfaceContainer = Color(0xFF182823),
    surfaceContainerHigh = Color(0xFF203530),
    surfaceContainerHighest = Color(0xFF28423B),
    outline = Color(0xFF9AA8A2),
    outlineVariant = Color(0xFF34443E),
)

private val LightColors = lightColorScheme(
    primary = Green,
    // Dark text on the green: white on #16C79A is only about 2:1.
    onPrimary = Color(0xFF06231B),
    primaryContainer = Color(0xFFBFF5E5),
    onPrimaryContainer = Color(0xFF00382B),
    secondary = Color(0xFF8A5A00),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFFFE2B8),
    onSecondaryContainer = Color(0xFF2E1C00),
    tertiary = Color(0xFF2F5BC0),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFD8E3FF),
    onTertiaryContainer = Color(0xFF00255E),
    error = Color(0xFFC62F3B),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD9),
    onErrorContainer = Color(0xFF3B0007),
    background = Color(0xFFF7F8FA),
    onBackground = Color(0xFF0E1A16),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF0E1A16),
    surfaceVariant = Color(0xFFE3ECE8),
    onSurfaceVariant = Color(0xFF45554F),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFFBFCFD),
    surfaceContainer = Color(0xFFFFFFFF),
    surfaceContainerHigh = Color(0xFFF0F3F5),
    surfaceContainerHighest = Color(0xFFE8EDEF),
    outline = Color(0xFF6B7A75),
    outlineVariant = Color(0xFFDCE3E1),
)

// Recordings use the light scheme unless the launch intent asks otherwise:
// adb shell am start ... --es theme dark   (or: system, to follow the device)
@Composable
fun LogcatDailyTheme(
    darkTheme: Boolean = wantsDark(),
    content: @Composable () -> Unit,
) {
    val view = LocalView.current
    SideEffect {
        // Clock and battery icons stay readable on both backgrounds.
        val window = view.context.findActivity()?.window ?: return@SideEffect
        WindowCompat.getInsetsController(window, view).apply {
            isAppearanceLightStatusBars = !darkTheme
            isAppearanceLightNavigationBars = !darkTheme
        }
    }
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
    ) {
        // Fills the area behind the system bars too, so a light scheme is not cut by the dark window.
        Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            content()
        }
    }
}

@Composable
private fun wantsDark(): Boolean {
    val system = isSystemInDarkTheme()
    return when (LocalView.current.context.findActivity()?.intent?.getStringExtra("theme")) {
        "dark" -> true
        "system" -> system
        else -> false
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

// Used by Avatar: solid brand-friendly hues, dark initials on each.
private val AvatarInk = Color(0xFF08130F)
private val AvatarFills = listOf(
    Color(0xFF16C79A),
    Color(0xFFFFB454),
    Color(0xFF6EA0FF),
    Color(0xFFFF8A7A),
    Color(0xFFB59CFF),
    Color(0xFF5CC8E8),
)

internal fun avatarColors(seed: String): Pair<Color, Color> =
    AvatarFills[seed.sumOf { it.code } % AvatarFills.size] to AvatarInk
