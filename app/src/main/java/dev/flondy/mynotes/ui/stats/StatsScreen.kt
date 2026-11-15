package dev.flondy.mynotes.ui.stats

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Notes
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.material.icons.outlined.TextFields
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.flondy.mynotes.Category
import dev.flondy.mynotes.Note
import dev.flondy.mynotes.R
import dev.flondy.mynotes.data.ChecklistCodec
import dev.flondy.mynotes.ui.components.CategoryIcons
import dev.flondy.mynotes.ui.components.IconTile
import dev.flondy.mynotes.ui.components.LeatherPage
import dev.flondy.mynotes.ui.theme.LocalAppExtras
import kotlinx.coroutines.delay
import java.util.Calendar

/** Kennzahlen aus den echten Notizen (nur lokal berechnet, nichts wird gesendet). */
data class NoteStats(
    val notes: Int, val lists: Int, val done: Int, val open: Int, val words: Int,
    val byCategory: List<Triple<Category?, Int, Float>>, // Kategorie (null = ohne), Anzahl, Anteil 0..1 am Maximum
    val activity: List<Int>, // bearbeitete Notizen je Tag, ältester zuerst (14 Tage)
) {
    val progress: Float get() = if (done + open == 0) 0f else done / (done + open).toFloat()

    companion object {
        fun of(notes: List<Note>, categories: List<Category>, now: Long = System.currentTimeMillis(), days: Int = 14): NoteStats {
            val live = notes.filter { !it.isInTrash }
            var done = 0; var open = 0; var words = 0
            for (n in live) {
                val items = ChecklistCodec.decode(n.checklist)
                done += items.count { it.checked }; open += items.count { !it.checked }
                words += (n.title + " " + n.content.replace("**", "")).split(Regex("\\s+")).count { it.any(Char::isLetterOrDigit) }
                words += items.sumOf { i -> i.text.split(Regex("\\s+")).count { it.any(Char::isLetterOrDigit) } }
            }
            val counts = live.groupingBy { it.categoryId }.eachCount()
            val max = (counts.values.maxOrNull() ?: 1).coerceAtLeast(1)
            val byCat = (categories.map { c -> c to (counts[c.id] ?: 0) } + listOf<Pair<Category?, Int>>(null to (counts[null] ?: 0)))
                .filter { it.second > 0 }.sortedByDescending { it.second }
                .map { Triple(it.first, it.second, it.second / max.toFloat()) }
            val cal = Calendar.getInstance().apply { timeInMillis = now; set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0) }
            val todayStart = cal.timeInMillis
            val dayMs = 24L * 60 * 60 * 1000
            val act = IntArray(days)
            for (n in live) {
                val idx = if (n.updatedAt >= todayStart) days - 1 else days - 1 - (((todayStart - n.updatedAt - 1) / dayMs) + 1).toInt()
                if (idx in 0 until days) act[idx]++
            }
            return NoteStats(live.size, live.count { it.isChecklist }, done, open, words, byCat, act.toList())
        }
    }
}

@Composable
fun StatsScreen(notes: List<Note>, categories: List<Category>, onBack: () -> Unit) {
    val stats = remember(notes, categories) { NoteStats.of(notes, categories) }
    val extras = LocalAppExtras.current
    val grow = remember { Animatable(if (extras.animationsEnabled) 0f else 1f) }
    LaunchedEffect(Unit) { if (extras.animationsEnabled) { delay(150); grow.animateTo(1f, tween(900, easing = FastOutSlowInEasing)) } }
    val g = grow.value

    LeatherPage(title = stringResource(R.string.stats_title), onBack = onBack) { bottom ->
        LazyVerticalGrid(
            columns = GridCells.Adaptive(170.dp),
            modifier = Modifier.fillMaxSize().widthIn(max = 1200.dp).testTag("stats_grid"),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = bottom + 24.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item { Tile(Icons.AutoMirrored.Outlined.Notes, stringResource(R.string.stats_notes), (stats.notes * g).toInt()) }
            item { Tile(Icons.Outlined.Checklist, stringResource(R.string.stats_lists), (stats.lists * g).toInt()) }
            item { Tile(Icons.Outlined.TaskAlt, stringResource(R.string.stats_done), (stats.done * g).toInt()) }
            item { Tile(Icons.Outlined.TextFields, stringResource(R.string.stats_words), (stats.words * g).toInt()) }
            item(span = { GridItemSpan(maxLineSpan) }) {
                Card {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Ring(stats.progress * g, Modifier.size(96.dp))
                        Spacer(Modifier.width(18.dp))
                        Column(Modifier.weight(1f)) {
                            Text(stringResource(R.string.stats_progress), style = MaterialTheme.typography.titleMedium)
                            Text("${stats.done} / ${stats.done + stats.open}", style = MaterialTheme.typography.headlineSmall, color = extras.accent)
                        }
                    }
                }
            }
            item(span = { GridItemSpan(maxLineSpan) }) {
                Card {
                    Text(stringResource(R.string.stats_by_category), style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(10.dp))
                    stats.byCategory.forEach { (cat, n, share) ->
                        Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                            IconTile(CategoryIcons.style(cat?.icon ?: "") ?: CategoryIcons.all.first().second, size = 30.dp)
                            Spacer(Modifier.width(10.dp))
                            Text(cat?.name ?: stringResource(R.string.stats_no_category), style = MaterialTheme.typography.bodyMedium,
                                maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.width(130.dp))
                            Box(Modifier.weight(1f).height(14.dp).clip(RoundedCornerShape(7.dp)).background(extras.sunken)) {
                                Box(Modifier.fillMaxHeight().fillMaxWidth((share * g).coerceIn(0.02f, 1f)).clip(RoundedCornerShape(7.dp)).background(extras.accent))
                            }
                            Text("$n", style = MaterialTheme.typography.labelLarge, modifier = Modifier.width(40.dp).padding(start = 8.dp))
                        }
                    }
                }
            }
            item(span = { GridItemSpan(maxLineSpan) }) {
                Card {
                    Text(stringResource(R.string.stats_activity), style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(10.dp))
                    val max = (stats.activity.maxOrNull() ?: 0).coerceAtLeast(1)
                    val bar = extras.accent; val base = extras.sunken
                    Canvas(Modifier.fillMaxWidth().height(120.dp).testTag("stats_activity")) {
                        val n = stats.activity.size
                        val gap = 6.dp.toPx(); val bw = (size.width - gap * (n - 1)) / n
                        stats.activity.forEachIndexed { i, v ->
                            val x = i * (bw + gap)
                            drawRoundRect(base, Offset(x, 0f), Size(bw, size.height), androidx.compose.ui.geometry.CornerRadius(6f))
                            val h = size.height * (v / max.toFloat()) * g
                            if (h > 0) drawRoundRect(bar, Offset(x, size.height - h), Size(bw, h), androidx.compose.ui.geometry.CornerRadius(6f))
                        }
                    }
                    Text(stringResource(R.string.stats_edited), style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 6.dp))
                }
            }
        }
    }
}

@Composable
private fun Card(content: @Composable () -> Unit) {
    val extras = LocalAppExtras.current
    Surface(shape = RoundedCornerShape(22.dp), color = extras.card, border = BorderStroke(1.dp, extras.cardBorder), shadowElevation = 2.dp) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) { content() }
    }
}

@Composable
private fun Tile(icon: ImageVector, label: String, value: Int) {
    val extras = LocalAppExtras.current
    Card {
        Icon(icon, null, tint = if (extras.isDark) extras.accent else extras.leather)
        Spacer(Modifier.height(8.dp))
        Text("$value", style = MaterialTheme.typography.headlineMedium)
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun Ring(progress: Float, modifier: Modifier) {
    val extras = LocalAppExtras.current
    val track = extras.sunken; val arc = extras.leaf
    Box(modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val s = Stroke(12.dp.toPx(), cap = StrokeCap.Round)
            drawArc(track, 0f, 360f, false, style = s)
            drawArc(arc, -90f, 360f * progress, false, style = s)
        }
        Text("${(progress * 100).toInt()} %", style = MaterialTheme.typography.titleMedium)
    }
}
