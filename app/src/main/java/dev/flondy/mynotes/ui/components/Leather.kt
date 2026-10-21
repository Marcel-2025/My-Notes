package dev.flondy.mynotes.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.automirrored.outlined.StickyNote2
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.CardGiftcard
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.Flight
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Landscape
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.Pets
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.Savings
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.material.icons.outlined.WorkOutline
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageShader
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.flondy.mynotes.Category
import dev.flondy.mynotes.Note
import dev.flondy.mynotes.R
import dev.flondy.mynotes.ui.theme.LocalAppExtras

/** Lederfläche: Farbverlauf + feine Narbung + optionale Ziernaht unten. */
fun Modifier.leatherBackground(top: Color, bottom: Color, grain: ShaderBrush?, stitch: Boolean = true, grainAlpha: Float = 1f): Modifier =
    this.drawBehind {
        drawRect(Brush.verticalGradient(listOf(top, bottom)))
        if (grain != null) drawRect(grain, alpha = grainAlpha)
        if (stitch) {
            val y = size.height - 7.dp.toPx()
            drawLine(
                color = Color(0x55FFE7B0),
                start = Offset(12.dp.toPx(), y),
                end = Offset(size.width - 12.dp.toPx(), y),
                strokeWidth = 1.2.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(7.dp.toPx(), 5.dp.toPx())),
            )
        }
    }

@Composable
fun rememberLeatherGrain(): ShaderBrush {
    val bitmap = androidx.compose.ui.graphics.ImageBitmap.imageResource(R.drawable.leather_grain)
    return remember(bitmap) { ShaderBrush(ImageShader(bitmap, TileMode.Repeated, TileMode.Repeated)) }
}

@Composable
fun Modifier.leather(stitch: Boolean = true): Modifier {
    val extras = LocalAppExtras.current
    val grain = rememberLeatherGrain()
    return this.leatherBackground(extras.leather, extras.leatherDark, grain, stitch, grainAlpha = if (extras.isDark) 0.45f else 0.9f)
}

/** Das App-Icon (Leder-Notizbuch) als Logo in der Kopfzeile. */
@Composable
fun AppLogo(size: Dp = 56.dp, modifier: Modifier = Modifier) {
    Image(painterResource(R.drawable.app_logo), contentDescription = null, modifier = modifier.size(size))
}

// ---------------------------------------------------------------- Icons für Kategorien & Notizen

data class TileStyle(val icon: ImageVector, val light: Color, val lightFg: Color, val dark: Color, val darkFg: Color)

object CategoryIcons {
    /** Reihenfolge = Reihenfolge in der Auswahl. Schlüssel werden in der DB gespeichert. */
    val all: List<Pair<String, TileStyle>> = listOf(
        "idea" to TileStyle(Icons.Outlined.Lightbulb, Color(0xFFFFE7A3), Color(0xFFB7791F), Color(0xFF4A3A16), Color(0xFFFFD36B)),
        "shopping" to TileStyle(Icons.Outlined.ShoppingCart, Color(0xFFDDEFC6), Color(0xFF4E8A1C), Color(0xFF2A3A1C), Color(0xFFA8D97A)),
        "travel" to TileStyle(Icons.Outlined.Flight, Color(0xFFD6E8F8), Color(0xFF2F6FB3), Color(0xFF1E2E40), Color(0xFF8DC0F0)),
        "heart" to TileStyle(Icons.Filled.Favorite, Color(0xFFFBD9D4), Color(0xFFD9443A), Color(0xFF452220), Color(0xFFFF8F84)),
        "work" to TileStyle(Icons.Outlined.WorkOutline, Color(0xFFEBDCCB), Color(0xFF7A4B2E), Color(0xFF3B2A20), Color(0xFFE0B892)),
        "school" to TileStyle(Icons.Outlined.School, Color(0xFFE6DDF6), Color(0xFF6A4BB0), Color(0xFF2E2540), Color(0xFFC2AEF0)),
        "tasks" to TileStyle(Icons.Outlined.TaskAlt, Color(0xFFFFE1C2), Color(0xFFCC6A14), Color(0xFF45301C), Color(0xFFFFB774)),
        "folder" to TileStyle(Icons.Outlined.Folder, Color(0xFFFFEBB5), Color(0xFFC8901C), Color(0xFF453818), Color(0xFFFFCF5C)),
        "home" to TileStyle(Icons.Outlined.Home, Color(0xFFE3EEDC), Color(0xFF52733A), Color(0xFF283223), Color(0xFFB2D39B)),
        "event" to TileStyle(Icons.Outlined.Event, Color(0xFFDCE7F2), Color(0xFF3E6A93), Color(0xFF1F2B38), Color(0xFF9CC2E6)),
        "image" to TileStyle(Icons.Outlined.Landscape, Color(0xFFD5EEF0), Color(0xFF2E8A8F), Color(0xFF1C3234), Color(0xFF86D2D6)),
        "book" to TileStyle(Icons.AutoMirrored.Outlined.MenuBook, Color(0xFFEFE0CF), Color(0xFF8A5A33), Color(0xFF3B2B1F), Color(0xFFE5BD94)),
        "food" to TileStyle(Icons.Outlined.Restaurant, Color(0xFFFDE0CC), Color(0xFFC0602A), Color(0xFF452A1E), Color(0xFFFFAE80)),
        "money" to TileStyle(Icons.Outlined.Savings, Color(0xFFDDF0E0), Color(0xFF2F8A4E), Color(0xFF1E3326), Color(0xFF8BD6A3)),
        "sport" to TileStyle(Icons.Outlined.FitnessCenter, Color(0xFFDCE4F4), Color(0xFF4659A8), Color(0xFF212840), Color(0xFFA5B4F0)),
        "music" to TileStyle(Icons.Outlined.MusicNote, Color(0xFFF6DCEB), Color(0xFFA83C78), Color(0xFF40213A), Color(0xFFF09CCB)),
        "gift" to TileStyle(Icons.Outlined.CardGiftcard, Color(0xFFFBDCDC), Color(0xFFB83B3B), Color(0xFF412222), Color(0xFFF59B9B)),
        "pets" to TileStyle(Icons.Outlined.Pets, Color(0xFFEDE3D3), Color(0xFF8A6A3A), Color(0xFF3A3020), Color(0xFFE0C38E)),
        "star" to TileStyle(Icons.Outlined.Star, Color(0xFFFFEDB8), Color(0xFFC08A0C), Color(0xFF453816), Color(0xFFFFD45C)),
    )
    private val byKey = all.toMap()
    val NOTE = TileStyle(Icons.AutoMirrored.Outlined.StickyNote2, Color(0xFFFFEDC4), Color(0xFFB7791F), Color(0xFF41321A), Color(0xFFFFD36B))
    val LIST = TileStyle(Icons.Outlined.Checklist, Color(0xFFFFEDC4), Color(0xFF8A5A1C), Color(0xFF41321A), Color(0xFFFFD36B))

    fun style(key: String): TileStyle? = byKey[key]

    /** Automatische Zuordnung für Kategorien ohne gewähltes Icon (z. B. Standardkategorien aus Version 2.0). */
    fun guess(name: String): String {
        val n = name.lowercase()
        return when {
            listOf("privat", "persön", "personal", "familie", "family", "liebe").any { it in n } -> "heart"
            listOf("arbeit", "work", "job", "büro", "office", "projekt").any { it in n } -> "work"
            listOf("schule", "school", "uni", "studium", "lernen").any { it in n } -> "school"
            listOf("idee", "idea").any { it in n } -> "idea"
            listOf("einkauf", "shopping", "kaufen", "grocer").any { it in n } -> "shopping"
            listOf("reise", "travel", "urlaub", "trip", "vacation").any { it in n } -> "travel"
            listOf("aufgabe", "task", "todo", "to-do", "erledigen").any { it in n } -> "tasks"
            listOf("rezept", "essen", "food", "recipe", "kochen").any { it in n } -> "food"
            listOf("finanz", "geld", "money", "budget").any { it in n } -> "money"
            listOf("sport", "fitness", "training").any { it in n } -> "sport"
            listOf("termin", "event", "kalender", "erinnerung", "reminder").any { it in n } -> "event"
            listOf("buch", "book", "lesen").any { it in n } -> "book"
            listOf("haus", "home", "wohnung").any { it in n } -> "home"
            else -> "folder"
        }
    }

    fun keyFor(category: Category): String = category.icon.ifBlank { guess(category.name) }
    fun forCategory(category: Category): TileStyle = style(keyFor(category)) ?: NOTE

    /** Icon einer Notiz: Kategorie-Icon, sonst Liste bzw. Notiz. */
    fun forNote(note: Note, category: Category?): TileStyle =
        category?.let(::forCategory) ?: if (note.isChecklist) LIST else NOTE
}

/** Abgerundete farbige Kachel mit Icon (wie in den Store-Grafiken). */
@Composable
fun IconTile(style: TileStyle, size: Dp = 44.dp, modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit = {}) {
    val dark = LocalAppExtras.current.isDark
    val shape = RoundedCornerShape(size * 0.3f)
    Box(
        modifier
            .size(size)
            .clip(shape)
            .background(Brush.verticalGradient(listOf(if (dark) style.dark.copy(alpha = 0.9f) else Color.White.copy(alpha = 0.5f).compositeLight(style.light), if (dark) style.dark else style.light)))
            .border(1.dp, (if (dark) style.darkFg else style.lightFg).copy(alpha = 0.18f), shape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(style.icon, contentDescription = null, tint = if (dark) style.darkFg else style.lightFg, modifier = Modifier.size(size * 0.55f))
        content()
    }
}

private fun Color.compositeLight(base: Color): Color {
    val a = alpha
    return Color(red * a + base.red * (1 - a), green * a + base.green * (1 - a), blue * a + base.blue * (1 - a), 1f)
}

/** Einheitliche Unterseite: Lederkopfzeile mit Zurück-Pfeil, darunter die Papierfläche mit runden Ecken. */
@Composable
fun LeatherPage(
    title: String,
    onBack: () -> Unit,
    actions: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit = {},
    floatingActionButton: @Composable () -> Unit = {},
    content: @Composable (bottomPadding: Dp) -> Unit,
) {
    val extras = LocalAppExtras.current
    Box(Modifier.fillMaxSize().leather(stitch = false)) {
        androidx.compose.material3.Scaffold(
            containerColor = Color.Transparent,
            floatingActionButton = floatingActionButton,
            topBar = {
                androidx.compose.foundation.layout.Row(
                    Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 4.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    androidx.compose.material3.IconButton(onClick = onBack) {
                        Icon(androidx.compose.material.icons.Icons.AutoMirrored.Filled.ArrowBack,
                            androidx.compose.ui.res.stringResource(R.string.action_back), tint = extras.onLeather)
                    }
                    androidx.compose.material3.Text(
                        title,
                        style = androidx.compose.material3.MaterialTheme.typography.titleLarge,
                        color = extras.onLeather,
                        modifier = Modifier.weight(1f).padding(start = 4.dp),
                    )
                    actions()
                }
            },
        ) { padding ->
            Box(
                Modifier
                    .padding(top = padding.calculateTopPadding())
                    .fillMaxSize()
                    .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                    .background(androidx.compose.material3.MaterialTheme.colorScheme.background),
                contentAlignment = Alignment.TopCenter,
            ) {
                androidx.compose.runtime.CompositionLocalProvider(
                    androidx.compose.material3.LocalContentColor provides androidx.compose.material3.MaterialTheme.colorScheme.onBackground,
                ) {
                    content(padding.calculateBottomPadding())
                }
            }
        }
    }
}
