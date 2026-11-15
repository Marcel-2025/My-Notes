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

private fun darkScheme(accent: Color, k: SkinPalette = Skin.LEATHER.p) = darkColorScheme(
    primary = accent,
    onPrimary = k.onAccent,
    primaryContainer = accent.copy(alpha = 0.22f).compositeOver(k.nightSurface),
    onPrimaryContainer = k.nightText,
    secondary = k.nightTextSoft,
    onSecondary = k.nightBg,
    secondaryContainer = k.nightCardHigh,
    onSecondaryContainer = k.nightText,
    tertiary = k.leaf,
    background = k.nightBg,
    onBackground = k.nightText,
    surface = k.nightBg,
    onSurface = k.nightText,
    surfaceVariant = k.nightCard,
    onSurfaceVariant = k.nightTextSoft,
    surfaceContainerLowest = Color(0xFF150E0A),
    surfaceContainerLow = k.nightSurface,
    surfaceContainer = k.nightCard,
    surfaceContainerHigh = k.nightCardHigh,
    surfaceContainerHighest = Color(0xFF45322A),
    inverseSurface = k.paper,
    inverseOnSurface = k.ink,
    outline = k.nightLine,
    outlineVariant = Color(0xFF3A2A20),
    error = ErrorDark,
    onError = k.nightBg,
    scrim = Color(0xCC000000),
)

private fun lightScheme(accent: Color, k: SkinPalette = Skin.LEATHER.p) = lightColorScheme(
    // Braune Tinte als Primärfarbe (Kontrast), Gold als Akzent für Auswahl & wichtige Aktionen
    primary = k.leather,
    onPrimary = k.onLeather,
    primaryContainer = accent.copy(alpha = 0.45f).compositeOver(k.card),
    onPrimaryContainer = k.ink,
    secondary = k.inkSoft,
    onSecondary = Color.White,
    secondaryContainer = k.sunken,
    onSecondaryContainer = k.ink,
    tertiary = k.leaf,
    background = k.paper,
    onBackground = k.ink,
    surface = k.paper,
    onSurface = k.ink,
    surfaceVariant = k.sunken,
    onSurfaceVariant = k.inkSoft,
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = k.cardHigh,
    surfaceContainer = k.card,
    surfaceContainerHigh = Color(0xFFF6E8D1),
    surfaceContainerHighest = k.sunken,
    inverseSurface = k.leatherDark,
    inverseOnSurface = k.onLeather,
    outline = Color(0xFFD9C3A3),
    outlineVariant = k.line,
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
    skin: Skin = Skin.LEATHER,
    pattern: PaperPattern = PaperPattern.PLAIN,
    content: @Composable () -> Unit,
) {
    val k = skin.p
    // Shop-Vorschau (30 s) endet von selbst – auch wenn der Shop schon verlassen wurde
    androidx.compose.runtime.LaunchedEffect(ShopPreview.endsAt) {
        if (ShopPreview.active) {
            kotlinx.coroutines.delay((ShopPreview.endsAt - System.currentTimeMillis()).coerceAtLeast(0L))
            ShopPreview.stop()
        }
    }
    val dark = isAppInDarkTheme(themeMode)
    // Eigene Akzentfarbe gilt nur, wenn nicht „Gelb“ (Standard) gewählt ist – sonst die des Designs
    val accent = if (accentColor == AccentColor.YELLOW) k.accent else Color(accentColor.argb)
    val animate = animationsEnabled && systemAnimationsEnabled()
    val target = if (dark) darkScheme(accent, k) else lightScheme(accent, k)
    val scheme = if (animate) target.animated() else target

    val extras = AppExtras(
        isDark = dark,
        accent = accent,
        onAccent = if (accentColor == AccentColor.YELLOW) k.onAccent else OnGold,
        cardBorder = if (dark) k.nightLine else k.line,
        gradientTop = if (dark) k.leatherDeep else k.leather,
        premiumGold = Gold,
        animationsEnabled = animate,
        leather = if (dark) k.nightLeather else k.leather,
        leatherDark = if (dark) k.leatherDeep else k.leatherDark,
        onLeather = k.onLeather,
        card = if (dark) k.nightCard else k.card,
        cardHigh = if (dark) k.nightCardHigh else k.cardHigh,
        sunken = if (dark) k.nightSurface else k.sunken,
        leaf = k.leaf,
    ).animatedExtras(animate)

    CompositionLocalProvider(LocalAppExtras provides extras, LocalPaperPattern provides pattern, LocalSkin provides skin) {
        MaterialTheme(colorScheme = scheme, typography = Typography, shapes = AppShapes, content = content)
    }
}

val LocalPaperPattern = staticCompositionLocalOf { PaperPattern.PLAIN }
val LocalSkin = staticCompositionLocalOf { Skin.LEATHER }

/** Designwechsel (2.4): Leder-, Karten- und Akzentfarben blenden weich über. */
@Composable
private fun AppExtras.animatedExtras(animate: Boolean): AppExtras {
    if (!animate) return this
    val spec = tween<Color>(durationMillis = 450)
    val leather by animateColorAsState(leather, spec, label = "leather")
    val leatherDark by animateColorAsState(leatherDark, spec, label = "leatherDark")
    val card by animateColorAsState(card, spec, label = "card")
    val accent by animateColorAsState(accent, spec, label = "accent")
    val cardBorder by animateColorAsState(cardBorder, spec, label = "border")
    return copy(leather = leather, leatherDark = leatherDark, card = card, accent = accent, cardBorder = cardBorder)
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
    val outlineVariant by animateColorAsState(outlineVariant, spec, label = "ov")
    val primaryContainer by animateColorAsState(primaryContainer, spec, label = "pc")
    return copy(outlineVariant = outlineVariant, primaryContainer = primaryContainer,
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
