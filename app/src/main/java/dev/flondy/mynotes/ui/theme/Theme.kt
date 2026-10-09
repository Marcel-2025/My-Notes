package dev.flondy.mynotes.ui.theme

import android.provider.Settings
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import dev.flondy.mynotes.settings.AccentColor
import dev.flondy.mynotes.settings.ThemeMode

/** Zusätzliche Markenwerte, die Material 3 nicht abdeckt. */
@Immutable
data class AppExtras(
    val isDark: Boolean,
    val accent: Color,
    val onAccent: Color,
    val cardBorder: Color,
    val gradientTop: Color,
    val premiumGold: Color,
    /** false, wenn der Nutzer Animationen deaktiviert hat oder das System sie reduziert. */
    val animationsEnabled: Boolean,
)

val LocalAppExtras = staticCompositionLocalOf {
    AppExtras(false, BrandYellow, OnAccent, Slate200, Color.Transparent, BrandYellow, true)
}

private fun darkScheme(accent: Color) = darkColorScheme(
    primary = accent,
    onPrimary = OnAccent,
    primaryContainer = accent.copy(alpha = 0.16f).compositeOver(InkSurface),
    onPrimaryContainer = PremiumWhite,
    secondary = Slate400,
    onSecondary = Ink,
    secondaryContainer = InkSurfaceHighest,
    onSecondaryContainer = PremiumWhite,
    tertiary = accent,
    background = Ink,
    onBackground = PremiumWhite,
    surface = Ink,
    onSurface = PremiumWhite,
    surfaceVariant = InkSurfaceHigh,
    onSurfaceVariant = Slate400,
    surfaceContainerLowest = Color(0xFF080A14),
    surfaceContainerLow = Color(0xFF0F1322),
    surfaceContainer = InkSurface,
    surfaceContainerHigh = InkSurfaceHigh,
    surfaceContainerHighest = InkSurfaceHighest,
    inverseSurface = PremiumWhite,
    inverseOnSurface = Ink,
    outline = InkOutline,
    outlineVariant = Color(0xFF1F2737),
    error = ErrorDark,
    onError = Ink,
    scrim = Color(0xCC000000),
)

private fun lightScheme(accent: Color) = lightColorScheme(
    // Gelb auf Weiß hat zu wenig Kontrast für Text/Icons → „Ink“ als Primärfarbe, Gelb als Akzent.
    primary = Ink,
    onPrimary = PremiumWhite,
    primaryContainer = accent.copy(alpha = 0.35f).compositeOver(Color.White),
    onPrimaryContainer = Ink,
    secondary = Slate600,
    onSecondary = Color.White,
    secondaryContainer = Slate100,
    onSecondaryContainer = Ink,
    tertiary = accent,
    background = LightBackground,
    onBackground = Color(0xFF0F172A),
    surface = LightBackground,
    onSurface = Color(0xFF0F172A),
    surfaceVariant = Slate100,
    onSurfaceVariant = Slate600,
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFFBFCFE),
    surfaceContainer = Color.White,
    surfaceContainerHigh = Color(0xFFF1F3F8),
    surfaceContainerHighest = Color(0xFFE9ECF3),
    inverseSurface = Ink,
    inverseOnSurface = PremiumWhite,
    outline = Color(0xFFCBD5E1),
    outlineVariant = Slate200,
    error = ErrorLight,
    onError = Color.White,
    scrim = Color(0x99000000),
)

private fun Color.compositeOver(background: Color): Color {
    val a = alpha
    return Color(
        red = red * a + background.red * (1 - a),
        green = green * a + background.green * (1 - a),
        blue = blue * a + background.blue * (1 - a),
        alpha = 1f,
    )
}

@Composable
private fun systemAnimationsEnabled(): Boolean {
    val context = LocalContext.current
    return remember {
        runCatching {
            Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) > 0f
        }.getOrDefault(true)
    }
}

@Composable
fun isAppInDarkTheme(mode: ThemeMode): Boolean = when (mode) {
    ThemeMode.SYSTEM -> isSystemInDarkTheme()
    ThemeMode.LIGHT -> false
    ThemeMode.DARK -> true
}

@Composable
fun MyNotesTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    accentColor: AccentColor = AccentColor.YELLOW,
    animationsEnabled: Boolean = true,
    content: @Composable () -> Unit,
) {
    val dark = isAppInDarkTheme(themeMode)
    val accent = Color(accentColor.argb)
    val animate = animationsEnabled && systemAnimationsEnabled()
    val target = if (dark) darkScheme(accent) else lightScheme(accent)
    val scheme = if (animate) target.animated() else target

    val extras = AppExtras(
        isDark = dark,
        accent = accent,
        onAccent = OnAccent,
        cardBorder = if (dark) Color.White.copy(alpha = 0.06f) else Color(0x140F172A),
        gradientTop = accent.copy(alpha = if (dark) 0.10f else 0.22f),
        premiumGold = BrandYellow,
        animationsEnabled = animate,
    )

    CompositionLocalProvider(LocalAppExtras provides extras) {
        MaterialTheme(colorScheme = scheme, typography = Typography, shapes = AppShapes, content = content)
    }
}

/** Sanfter Theme-Wechsel: die wichtigsten Flächen- und Textfarben werden überblendet. */
@Composable
private fun ColorScheme.animated(): ColorScheme {
    val spec = tween<Color>(durationMillis = 350)
    val background by animateColorAsState(background, spec, label = "bg")
    val surface by animateColorAsState(surface, spec, label = "surface")
    val onSurface by animateColorAsState(onSurface, spec, label = "onSurface")
    val onBackground by animateColorAsState(onBackground, spec, label = "onBackground")
    val surfaceContainer by animateColorAsState(surfaceContainer, spec, label = "sc")
    val surfaceContainerHigh by animateColorAsState(surfaceContainerHigh, spec, label = "sch")
    val surfaceVariant by animateColorAsState(surfaceVariant, spec, label = "sv")
    val onSurfaceVariant by animateColorAsState(onSurfaceVariant, spec, label = "osv")
    val primary by animateColorAsState(primary, spec, label = "primary")
    return copy(
        background = background, surface = surface, onSurface = onSurface, onBackground = onBackground,
        surfaceContainer = surfaceContainer, surfaceContainerHigh = surfaceContainerHigh,
        surfaceVariant = surfaceVariant, onSurfaceVariant = onSurfaceVariant, primary = primary,
    )
}

/** Animations-Spezifikationen, die „reduzierte Bewegung“ respektieren. */
object Motion {
    @Composable
    fun <T> spring(): FiniteAnimationSpec<T> =
        if (LocalAppExtras.current.animationsEnabled) spring(Spring.DampingRatioLowBouncy, Spring.StiffnessMediumLow) else snap()

    @Composable
    fun <T> tween(ms: Int = 250): FiniteAnimationSpec<T> =
        if (LocalAppExtras.current.animationsEnabled) androidx.compose.animation.core.tween(ms) else snap()

    @Composable
    fun <T> placement(): FiniteAnimationSpec<T>? =
        if (LocalAppExtras.current.animationsEnabled) spring(Spring.DampingRatioNoBouncy, Spring.StiffnessMediumLow) else null
}
