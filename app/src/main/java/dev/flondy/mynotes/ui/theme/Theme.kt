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

/** Zusätzliche Markenwerte (Leder & Papier), die Material 3 nicht abdeckt. */
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
    val leather: Color = Leather,
    val leatherDark: Color = LeatherDark,
    val onLeather: Color = OnLeather,
    val card: Color = PaperCard,
    val cardHigh: Color = PaperCardHigh,
    val sunken: Color = PaperSunken,
    val leaf: Color = Leaf,
)

val LocalAppExtras = staticCompositionLocalOf {
    AppExtras(false, Gold, OnGold, PaperLine, Color.Transparent, Gold, true)
}

private fun darkScheme(accent: Color) = darkColorScheme(
    primary = accent,
    onPrimary = OnGold,
    primaryContainer = accent.copy(alpha = 0.22f).compositeOver(NightSurface),
    onPrimaryContainer = NightText,
    secondary = NightTextSoft,
    onSecondary = NightBackground,
    secondaryContainer = NightCardHigh,
    onSecondaryContainer = NightText,
    tertiary = Leaf,
    background = NightBackground,
    onBackground = NightText,
    surface = NightBackground,
    onSurface = NightText,
    surfaceVariant = NightCard,
    onSurfaceVariant = NightTextSoft,
    surfaceContainerLowest = Color(0xFF150E0A),
    surfaceContainerLow = NightSurface,
    surfaceContainer = NightCard,
    surfaceContainerHigh = NightCardHigh,
    surfaceContainerHighest = Color(0xFF45322A),
    inverseSurface = PaperBackground,
    inverseOnSurface = InkBrown,
    outline = NightLine,
    outlineVariant = Color(0xFF3A2A20),
    error = ErrorDark,
    onError = NightBackground,
    scrim = Color(0xCC000000),
)

private fun lightScheme(accent: Color) = lightColorScheme(
    // Braune Tinte als Primärfarbe (Kontrast), Gold als Akzent für Auswahl & wichtige Aktionen
    primary = Leather,
    onPrimary = OnLeather,
    primaryContainer = accent.copy(alpha = 0.45f).compositeOver(PaperCard),
    onPrimaryContainer = InkBrown,
    secondary = InkBrownSoft,
    onSecondary = Color.White,
    secondaryContainer = PaperSunken,
    onSecondaryContainer = InkBrown,
    tertiary = Leaf,
    background = PaperBackground,
    onBackground = InkBrown,
    surface = PaperBackground,
    onSurface = InkBrown,
    surfaceVariant = PaperSunken,
    onSurfaceVariant = InkBrownSoft,
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = PaperCardHigh,
    surfaceContainer = PaperCard,
    surfaceContainerHigh = Color(0xFFF6E8D1),
    surfaceContainerHighest = PaperSunken,
    inverseSurface = LeatherDark,
    inverseOnSurface = OnLeather,
    outline = Color(0xFFD9C3A3),
    outlineVariant = PaperLine,
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
        onAccent = OnGold,
        cardBorder = if (dark) NightLine else PaperLine,
        gradientTop = if (dark) LeatherDeep else Leather,
        premiumGold = Gold,
        animationsEnabled = animate,
        leather = if (dark) Color(0xFF3F2318) else Leather,
        leatherDark = if (dark) LeatherDeep else LeatherDark,
        onLeather = OnLeather,
        card = if (dark) NightCard else PaperCard,
        cardHigh = if (dark) NightCardHigh else PaperCardHigh,
        sunken = if (dark) NightSurface else PaperSunken,
        leaf = Leaf,
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
