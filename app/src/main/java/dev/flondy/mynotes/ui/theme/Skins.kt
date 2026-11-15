package dev.flondy.mynotes.ui.theme

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import dev.flondy.mynotes.R
import dev.flondy.mynotes.ui.components.StrRes

/** Farbpalette eines Designs (hell + Abend). */
data class SkinPalette(
    val leather: Color, val leatherLight: Color, val leatherDark: Color, val leatherDeep: Color, val onLeather: Color,
    val paper: Color, val card: Color, val cardHigh: Color, val line: Color, val sunken: Color,
    val ink: Color, val inkSoft: Color,
    val accent: Color, val onAccent: Color, val leaf: Color,
    val nightBg: Color, val nightSurface: Color, val nightCard: Color, val nightCardHigh: Color, val nightLine: Color,
    val nightText: Color, val nightTextSoft: Color, val nightLeather: Color,
)

/**
 * Designs (Version 2.4). „Leder & Papier“ und „Graphit“ sind kostenlos, die übrigen Premium.
 * Premium-Designs lassen sich 30 Sekunden ansehen ([ShopPreview]); Kauf folgt später über den Store.
 */
enum class Skin(val title: StrRes, val premium: Boolean, val p: SkinPalette) {
    LEATHER(R.string.skin_leather, false, SkinPalette(
        Leather, LeatherLight, LeatherDark, LeatherDeep, OnLeather,
        PaperBackground, PaperCard, PaperCardHigh, PaperLine, PaperSunken, InkBrown, InkBrownSoft,
        Gold, OnGold, Leaf,
        NightBackground, NightSurface, NightCard, NightCardHigh, NightLine, NightText, NightTextSoft, Color(0xFF3F2318))),
    GRAPHITE(R.string.skin_graphite, false, SkinPalette(
        Color(0xFF2E3138), Color(0xFF3B3F47), Color(0xFF1E2025), Color(0xFF15171A), Color(0xFFF2F3F5),
        Color(0xFFF1F2F4), Color(0xFFFFFFFF), Color(0xFFFFFFFF), Color(0xFFDADDE2), Color(0xFFE6E8EC), Color(0xFF22252B), Color(0xFF5E636D),
        Color(0xFFF2C14E), Color(0xFF1E2025), Color(0xFF5FA35A),
        Color(0xFF121316), Color(0xFF1A1C20), Color(0xFF22252A), Color(0xFF2A2D33), Color(0xFF34383F), Color(0xFFECEEF1), Color(0xFFA6ABB4), Color(0xFF24272D))),
    MIDNIGHT(R.string.skin_midnight, true, SkinPalette(
        Color(0xFF1F2A44), Color(0xFF2A3858), Color(0xFF151D30), Color(0xFF0E1422), Color(0xFFE8EEFB),
        Color(0xFFEEF1F7), Color(0xFFFAFBFE), Color(0xFFFFFFFF), Color(0xFFD5DCEA), Color(0xFFE2E7F1), Color(0xFF1B2335), Color(0xFF56607A),
        Color(0xFF8FB3FF), Color(0xFF0E1422), Color(0xFF5BB98C),
        Color(0xFF0D111C), Color(0xFF131A28), Color(0xFF1A2233), Color(0xFF212B3F), Color(0xFF2B364D), Color(0xFFE6ECF8), Color(0xFF9AA6C2), Color(0xFF18213A))),
    FOREST(R.string.skin_forest, true, SkinPalette(
        Color(0xFF2F4A35), Color(0xFF3C5C43), Color(0xFF223627), Color(0xFF17251B), Color(0xFFEEF5E6),
        Color(0xFFF2EFE2), Color(0xFFFBF9EF), Color(0xFFFFFDF5), Color(0xFFDCD6BF), Color(0xFFE8E3CF), Color(0xFF263228), Color(0xFF5D6B5E),
        Color(0xFFE0A526), Color(0xFF2B1D08), Color(0xFF7BB342),
        Color(0xFF111A13), Color(0xFF17221A), Color(0xFF1E2B21), Color(0xFF253428), Color(0xFF304235), Color(0xFFE9F0E3), Color(0xFFA9B8A6), Color(0xFF203325))),
    PARCHMENT(R.string.skin_parchment, true, SkinPalette(
        Color(0xFF7A5230), Color(0xFF8E6440), Color(0xFF5C3B21), Color(0xFF3F2815), Color(0xFFFFF4E0),
        Color(0xFFF4E6C8), Color(0xFFFBF1DA), Color(0xFFFFF7E6), Color(0xFFE2CFA6), Color(0xFFEBDDBA), Color(0xFF3E2A16), Color(0xFF7B6247),
        Color(0xFFC0392B), Color(0xFFFFF4E0), Color(0xFF6E8B3D),
        Color(0xFF1E150C), Color(0xFF271B10), Color(0xFF302216), Color(0xFF3A2A1C), Color(0xFF4A3624), Color(0xFFF3E4C6), Color(0xFFC7B08E), Color(0xFF4A3018))),
    BLOSSOM(R.string.skin_blossom, true, SkinPalette(
        Color(0xFF8E3B5B), Color(0xFFA34D6E), Color(0xFF6E2B45), Color(0xFF4C1D30), Color(0xFFFFF0F5),
        Color(0xFFFBEFF2), Color(0xFFFFF8FA), Color(0xFFFFFFFF), Color(0xFFF0D3DC), Color(0xFFF6E1E8), Color(0xFF4A2232), Color(0xFF8A5F70),
        Color(0xFFF7A8C4), Color(0xFF4C1D30), Color(0xFF6DAA6C),
        Color(0xFF1C0F15), Color(0xFF24141C), Color(0xFF2E1923), Color(0xFF38202B), Color(0xFF4A2B39), Color(0xFFF8E6ED), Color(0xFFCFA9B8), Color(0xFF55233A))),
    OCEAN(R.string.skin_ocean, true, SkinPalette(
        Color(0xFF1D5D73), Color(0xFF266F88), Color(0xFF154656), Color(0xFF0E303B), Color(0xFFE9F7FB),
        Color(0xFFEAF4F6), Color(0xFFF8FCFD), Color(0xFFFFFFFF), Color(0xFFCFE3E8), Color(0xFFDEECEF), Color(0xFF173640), Color(0xFF557883),
        Color(0xFF3FC1C9), Color(0xFF082A30), Color(0xFF4FAE7C),
        Color(0xFF0B171B), Color(0xFF102127), Color(0xFF152A31), Color(0xFF1B343C), Color(0xFF23434D), Color(0xFFE3F3F7), Color(0xFF9EC0C9), Color(0xFF123D4B)));

    companion object {
        fun of(name: String?): Skin = entries.firstOrNull { it.name == name } ?: LEATHER
    }
}

/** Papiermuster für Editor und Karten (Version 2.4). */
enum class PaperPattern(val title: StrRes, val premium: Boolean) {
    PLAIN(R.string.paper_plain, false),
    LINED(R.string.paper_lined, false),
    GRID(R.string.paper_grid, true),
    DOTS(R.string.paper_dots, true);

    companion object {
        fun of(name: String?): PaperPattern = entries.firstOrNull { it.name == name } ?: PLAIN
    }
}

/**
 * Vorschau eines gesperrten Designs/Musters für 30 Sekunden. Liegt nur im Arbeitsspeicher –
 * es wird nichts gespeichert und nichts gekauft.
 */
object ShopPreview {
    const val SECONDS = 30
    var skin by mutableStateOf<Skin?>(null)
        private set
    var pattern by mutableStateOf<PaperPattern?>(null)
        private set
    var endsAt by mutableLongStateOf(0L)
        private set

    val active: Boolean get() = skin != null || pattern != null

    fun start(skin: Skin? = null, pattern: PaperPattern? = null, now: Long = System.currentTimeMillis()) {
        this.skin = skin; this.pattern = pattern; endsAt = now + SECONDS * 1000L
    }

    fun stop() { skin = null; pattern = null; endsAt = 0L }

    fun secondsLeft(now: Long = System.currentTimeMillis()): Int = ((endsAt - now + 999) / 1000).toInt().coerceAtLeast(0)
}

/** Freischaltung: kostenlose Inhalte immer, Premium nur, wenn freigeschaltet (Kauf folgt später; Test-Schalter nur in Debug). */
object Entitlements {
    fun skinUnlocked(skin: Skin, unlocked: Set<String>): Boolean = !skin.premium || "skin:${skin.name}" in unlocked || ALL in unlocked
    fun patternUnlocked(p: PaperPattern, unlocked: Set<String>): Boolean = !p.premium || "paper:${p.name}" in unlocked || ALL in unlocked
    const val ALL = "all"

    /** Was gerade zu sehen ist: Vorschau > gespeicherte Wahl (nur wenn freigeschaltet) > Standard. */
    fun effectiveSkin(saved: String, unlocked: Set<String>): Skin =
        ShopPreview.skin ?: Skin.of(saved).takeIf { skinUnlocked(it, unlocked) } ?: Skin.LEATHER

    fun effectivePattern(saved: String, unlocked: Set<String>): PaperPattern =
        ShopPreview.pattern ?: PaperPattern.of(saved).takeIf { patternUnlocked(it, unlocked) } ?: PaperPattern.PLAIN
}

/** Zeichnet das Papiermuster hinter den Inhalt (Editor, Shop-Vorschau). */
fun androidx.compose.ui.Modifier.paperPattern(p: PaperPattern, line: Color, accent: Color): androidx.compose.ui.Modifier =
    if (p == PaperPattern.PLAIN) this else this.then(androidx.compose.ui.Modifier.drawBehind {
        val step = 28.dp.toPx()
        val c = line.copy(alpha = 0.75f)
        when (p) {
            PaperPattern.LINED -> {
                var y = step * 2
                while (y < size.height) { drawLine(c, Offset(0f, y), Offset(size.width, y), 1.dp.toPx()); y += step }
                drawLine(accent.copy(alpha = 0.45f), Offset(step * 1.6f, 0f), Offset(step * 1.6f, size.height), 1.5.dp.toPx())
            }
            PaperPattern.GRID -> {
                val s = step * 0.8f
                var y = s; while (y < size.height) { drawLine(c.copy(alpha = 0.5f), Offset(0f, y), Offset(size.width, y), 1f); y += s }
                var x = s; while (x < size.width) { drawLine(c.copy(alpha = 0.5f), Offset(x, 0f), Offset(x, size.height), 1f); x += s }
            }
            PaperPattern.DOTS -> {
                val s = step * 0.8f
                var y = s
                while (y < size.height) { var x = s; while (x < size.width) { drawCircle(c, 1.4.dp.toPx(), Offset(x, y)); x += s }; y += s }
            }
            PaperPattern.PLAIN -> Unit
        }
    })
