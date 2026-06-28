package com.crsmthw.unotracker.ui.theme

import android.app.Activity
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.crsmthw.unotracker.data.ThemeMode

// ── Static fallback palette (devices below Android 12) — UNO red-forward, game-night feel ──────
private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFFF8A80), onPrimary = Color(0xFF5F1412),
    primaryContainer = Color(0xFF8C1D18), onPrimaryContainer = Color(0xFFFFDAD6),
    secondary = Color(0xFF8FC7FF), onSecondary = Color(0xFF00344F),
    secondaryContainer = Color(0xFF124C70), onSecondaryContainer = Color(0xFFCDE5FF),
    tertiary = Color(0xFF8AD98A), onTertiary = Color(0xFF003A0B),
    tertiaryContainer = Color(0xFF1F5C20), onTertiaryContainer = Color(0xFFA6F5A2),
    background = Color(0xFF131013), onBackground = Color(0xFFEDE0DE),
    surface = Color(0xFF1B181B), onSurface = Color(0xFFEDE0DE),
    surfaceVariant = Color(0xFF2A2426), onSurfaceVariant = Color(0xFFD2C3C0),
    outline = Color(0xFF9B8D8A), outlineVariant = Color(0xFF4E4543),
    inverseSurface = Color(0xFFEDE0DE), inverseOnSurface = Color(0xFF1B181B),
    inversePrimary = Color(0xFFB3261E),
    error = Color(0xFFFFB4AB), onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A), onErrorContainer = Color(0xFFFFDAD6),
    scrim = Color(0xFF000000),
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFFC62828), onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFFFDAD6), onPrimaryContainer = Color(0xFF410002),
    secondary = Color(0xFF1565C0), onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFD3E4FF), onSecondaryContainer = Color(0xFF001D36),
    tertiary = Color(0xFF2E7D32), onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFB6F2B0), onTertiaryContainer = Color(0xFF002204),
    background = Color(0xFFFFF8F7), onBackground = Color(0xFF201A19),
    surface = Color(0xFFFFFFFF), onSurface = Color(0xFF201A19),
    surfaceVariant = Color(0xFFF5DDD9), onSurfaceVariant = Color(0xFF534340),
    outline = Color(0xFF85736F), outlineVariant = Color(0xFFD8C2BE),
    inverseSurface = Color(0xFF362F2E), inverseOnSurface = Color(0xFFFBEEEC),
    inversePrimary = Color(0xFFFF8A80),
    error = Color(0xFFBA1A1A), onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6), onErrorContainer = Color(0xFF410002),
    scrim = Color(0xFF000000),
)

// ── AMOLED pure-black overlay — keeps accents, blacks out surfaces ─────────────────────────────
private fun ColorScheme.withAmoledBlack(): ColorScheme = copy(
    background = Color(0xFF000000), onBackground = Color(0xFFEDE0DE),
    surface = Color(0xFF000000), onSurface = Color(0xFFEDE0DE),
    surfaceVariant = Color(0xFF0D0D0D), surfaceTint = Color(0xFFFF8A80),
    inverseSurface = Color(0xFFEDE0DE), inverseOnSurface = Color(0xFF000000),
)

// ── Smoothly animated scheme so light <-> dark <-> AMOLED transitions glide ─────────────────────
private val colorAnimSpec: AnimationSpec<Color> = tween(durationMillis = 450, easing = FastOutSlowInEasing)

@Composable
private fun animatedColorScheme(target: ColorScheme): ColorScheme {
    @Composable fun ac(c: Color): Color = animateColorAsState(c, colorAnimSpec, label = "").value
    return target.copy(
        primary = ac(target.primary), onPrimary = ac(target.onPrimary),
        primaryContainer = ac(target.primaryContainer), onPrimaryContainer = ac(target.onPrimaryContainer),
        secondary = ac(target.secondary), onSecondary = ac(target.onSecondary),
        secondaryContainer = ac(target.secondaryContainer), onSecondaryContainer = ac(target.onSecondaryContainer),
        tertiary = ac(target.tertiary), onTertiary = ac(target.onTertiary),
        tertiaryContainer = ac(target.tertiaryContainer), onTertiaryContainer = ac(target.onTertiaryContainer),
        background = ac(target.background), onBackground = ac(target.onBackground),
        surface = ac(target.surface), onSurface = ac(target.onSurface),
        surfaceVariant = ac(target.surfaceVariant), onSurfaceVariant = ac(target.onSurfaceVariant),
        surfaceTint = ac(target.surfaceTint),
        inverseSurface = ac(target.inverseSurface), inverseOnSurface = ac(target.inverseOnSurface),
        inversePrimary = ac(target.inversePrimary),
        error = ac(target.error), onError = ac(target.onError),
        errorContainer = ac(target.errorContainer), onErrorContainer = ac(target.onErrorContainer),
        outline = ac(target.outline), outlineVariant = ac(target.outlineVariant),
        scrim = ac(target.scrim),
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun UnoTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    amoledBlack: Boolean = false,
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val isDark = when (themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }

    // minSdk 35, so dynamic color (API 31+) is always available — no SDK_INT gate needed.
    val baseScheme = when {
        dynamicColor -> {
            val context = LocalContext.current
            if (isDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        isDark -> DarkColorScheme
        else -> LightColorScheme
    }

    val targetScheme = if (amoledBlack && isDark) baseScheme.withAmoledBlack() else baseScheme
    val colorScheme = animatedColorScheme(targetScheme)

    // Keep system-bar icon tint synced with the active theme after every composition (survives
    // navigating away, sheets, rotation). Setting the appearance flags is more reliable than
    // re-calling enableEdgeToEdge — playbook §6.
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !isDark
                isAppearanceLightNavigationBars = !isDark
            }
        }
    }

    MaterialExpressiveTheme(
        colorScheme = colorScheme,
        typography = UnoTypography,
        shapes = UnoShapes,
        motionScheme = MotionScheme.expressive(),
        content = content,
    )
}
