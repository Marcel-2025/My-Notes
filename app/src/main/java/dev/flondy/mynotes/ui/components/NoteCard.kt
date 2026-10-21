package dev.flondy.mynotes.ui.components

import android.text.format.DateFormat
import android.text.format.DateUtils
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.os.ConfigurationCompat
import dev.flondy.mynotes.Category
import dev.flondy.mynotes.Note
import dev.flondy.mynotes.R
import dev.flondy.mynotes.data.ChecklistCodec
import dev.flondy.mynotes.data.RichText
import dev.flondy.mynotes.settings.CardDensity
import dev.flondy.mynotes.ui.theme.Berry
import dev.flondy.mynotes.ui.theme.LocalAppExtras
import dev.flondy.mynotes.ui.theme.Motion
import dev.flondy.mynotes.ui.theme.noteColor
import java.util.Calendar
import java.util.Date

enum class CardLayout { GRID, LIST }

private val CardShape = RoundedCornerShape(20.dp)
val FavoriteGold = Color(0xFFF2B21B)

@Composable
fun noteContainerColor(colorIndex: Int): Color {
    val extras = LocalAppExtras.current
    val c = noteColor(colorIndex)
    val custom = if (extras.isDark) c.dark else c.light
    return if (custom == Color.Unspecified) extras.card else custom
}

/** Formatierung (fett/kursiv/…) für die Anzeige plus Hervorhebung des Suchbegriffs. */
@Composable
fun formattedPreview(text: String, query: String): AnnotatedString {
    val accent = LocalAppExtras.current.accent
    val linkColor = MaterialTheme.colorScheme.tertiary
    return remember(text, query, accent) {
        val rendered = RichText.render(text)
        buildAnnotatedString {
            append(rendered.text)
            rendered.spans.forEach { s ->
                val style = when (s.style) {
                    RichText.Style.BOLD -> SpanStyle(fontWeight = FontWeight.Bold)
                    RichText.Style.ITALIC -> SpanStyle(fontStyle = FontStyle.Italic)
                    RichText.Style.UNDERLINE -> SpanStyle(textDecoration = TextDecoration.Underline)
                    RichText.Style.LINK -> SpanStyle(color = linkColor, textDecoration = TextDecoration.Underline)
                }
                if (s.end > s.start) addStyle(style, s.start, s.end)
            }
            addHighlights(rendered.text, query, accent)
        }
    }
}

/** Hebt alle Vorkommen des Suchbegriffs hervor (Groß-/Kleinschreibung egal). */
@Composable
fun highlight(text: String, query: String): AnnotatedString {
    val accent = LocalAppExtras.current.accent
    if (query.isBlank()) return AnnotatedString(text)
    return remember(text, query, accent) {
        buildAnnotatedString { append(text); addHighlights(text, query, accent) }
    }
}

private fun AnnotatedString.Builder.addHighlights(text: String, query: String, accent: Color) {
    val n = query.trim().lowercase()
    if (n.isEmpty()) return
    val lower = text.lowercase()
    var i = lower.indexOf(n)
    while (i >= 0) {
        addStyle(SpanStyle(background = accent.copy(alpha = 0.6f), color = Color(0xFF3E2219), fontWeight = FontWeight.Bold), i, i + n.length)
        i = lower.indexOf(n, i + n.length)
    }
}

/** „Heute, 09:41“, „Gestern, 18:30“ oder „25. Mai 2024“. */
@Composable
fun formatCardDate(time: Long): String {
    val context = LocalContext.current
    val locale = ConfigurationCompat.getLocales(LocalConfiguration.current)[0] ?: java.util.Locale.getDefault()
    return remember(time, locale) {
        val timeText = DateFormat.getTimeFormat(context).format(Date(time))
        val now = Calendar.getInstance()
        val then = Calendar.getInstance().apply { timeInMillis = time }
        val sameDay = now.get(Calendar.YEAR) == then.get(Calendar.YEAR) && now.get(Calendar.DAY_OF_YEAR) == then.get(Calendar.DAY_OF_YEAR)
        val yesterday = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
        val wasYesterday = yesterday.get(Calendar.YEAR) == then.get(Calendar.YEAR) && yesterday.get(Calendar.DAY_OF_YEAR) == then.get(Calendar.DAY_OF_YEAR)
        when {
            sameDay -> context.getString(R.string.date_today_at, timeText)
            wasYesterday -> context.getString(R.string.date_yesterday_at, timeText)
            else -> {
                val pattern = DateFormat.getBestDateTimePattern(locale, "dMMMMyyyy")
                java.text.SimpleDateFormat(pattern, locale).format(Date(time))
            }
        }
    }
}

@Composable
fun formatRelative(time: Long): String {
    val context = LocalContext.current
    return remember(time) {
        val now = System.currentTimeMillis()
        if (now - time < DateUtils.MINUTE_IN_MILLIS) context.getString(R.string.time_just_now)
        else DateUtils.getRelativeDateTimeString(context, time, DateUtils.MINUTE_IN_MILLIS, DateUtils.WEEK_IN_MILLIS, DateUtils.FORMAT_ABBREV_MONTH).toString()
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NoteCard(
    note: Note,
    category: Category?,
    layout: CardLayout,
    density: CardDensity,
    searchQuery: String,
    hideCheckedItems: Boolean,
    isPlaceholder: Boolean,
    isOverlay: Boolean,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)?,
    onMenu: () -> Unit,
    accessibilityActions: List<CustomAccessibilityAction>,
    modifier: Modifier = Modifier,
    onToggleFavorite: (() -> Unit)? = null,
) {
    val extras = LocalAppExtras.current
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val pressScale by animateFloatAsState(if (pressed && !isOverlay) 0.975f else 1f, Motion.spring(), label = "press")
    val elevation by animateDpAsState(if (isOverlay) 16.dp else 2.dp, Motion.spring(), label = "elevation")
    val container by animateColorAsState(noteContainerColor(note.color), Motion.tween(300), label = "container")
    val compact = density == CardDensity.COMPACT
    val pinnedLabel = stringResource(R.string.state_pinned)
    val favLabel = stringResource(R.string.state_favorite)
    val tile = CategoryIcons.forNote(note, category)

    Box(modifier.fillMaxWidth()) {
        Surface(
            shape = CardShape,
            color = container,
            shadowElevation = elevation,
            modifier = Modifier
                .fillMaxWidth()
                .scale(pressScale)
                .alpha(if (isPlaceholder) 0f else 1f)
                .border(1.dp, if (isOverlay) extras.accent else extras.cardBorder, CardShape)
                .clip(CardShape)
                .combinedClickable(
                    interactionSource = interaction,
                    indication = androidx.compose.material3.ripple(),
                    onClick = onClick,
                    onLongClick = onLongClick,
                    onLongClickLabel = stringResource(R.string.a11y_note_options),
                )
                .semantics {
                    customActions = accessibilityActions
                    stateDescription = listOfNotNull(pinnedLabel.takeIf { note.isPinned }, favLabel.takeIf { note.isFavorite }).joinToString(", ")
                },
        ) {
            if (layout == CardLayout.GRID) {
                GridContent(note, tile, compact, searchQuery, hideCheckedItems, onMenu)
            } else {
                ListContent(note, tile, compact, searchQuery, hideCheckedItems, onMenu, onToggleFavorite)
            }
        }
        if (isPlaceholder) PlaceholderOutline(Modifier.matchParentSize())
    }
}

@Composable
private fun PlaceholderOutline(modifier: Modifier) {
    val accent = LocalAppExtras.current.accent
    Box(
        modifier
            .clip(CardShape)
            .background(accent.copy(alpha = 0.12f))
            .drawBehind {
                drawRoundRect(
                    color = accent,
                    cornerRadius = CornerRadius(20.dp.toPx()),
                    style = Stroke(width = 2.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 10f))),
                )
            }
    )
}

@Composable
private fun GridContent(note: Note, tile: TileStyle, compact: Boolean, query: String, hideChecked: Boolean, onMenu: () -> Unit) {
    Column(Modifier.padding(start = 14.dp, top = 12.dp, bottom = 12.dp, end = 2.dp)) {
        Row(verticalAlignment = Alignment.Top) {
            IconTile(tile, size = if (compact) 36.dp else 44.dp)
            Spacer(Modifier.weight(1f))
            if (note.isPinned) Icon(Icons.Filled.PushPin, null, Modifier.padding(top = 6.dp).size(16.dp), tint = Berry)
            if (note.isFavorite) Icon(Icons.Filled.Star, null, Modifier.padding(top = 5.dp, start = 2.dp).size(18.dp), tint = FavoriteGold)
            MenuButton(onMenu)
        }
        Spacer(Modifier.height(10.dp))
        Text(
            text = highlight(note.title.ifBlank { stringResource(R.string.untitled_note) }, query),
            style = MaterialTheme.typography.titleMedium,
            color = if (note.title.isBlank()) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(end = 12.dp),
        )
        Column(Modifier.padding(end = 12.dp, top = 6.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            if (note.hasChecklist) ChecklistPreview(note.checklist, query, hideChecked, maxItems = if (compact) 3 else 5)
            if (note.content.isNotBlank()) {
                Text(
                    text = formattedPreview(note.content, query),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = if (compact) 2 else if (note.hasChecklist) 2 else 6,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        DateRow(note.updatedAt)
    }
}

@Composable
private fun ListContent(
    note: Note, tile: TileStyle, compact: Boolean, query: String, hideChecked: Boolean,
    onMenu: () -> Unit, onToggleFavorite: (() -> Unit)?,
) {
    Row(Modifier.padding(start = 12.dp, top = 12.dp, bottom = 12.dp, end = 2.dp), verticalAlignment = Alignment.CenterVertically) {
        IconTile(tile, size = if (compact) 44.dp else 54.dp)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (note.isPinned) {
                    Icon(Icons.Filled.PushPin, null, Modifier.size(15.dp), tint = Berry)
                    Spacer(Modifier.width(4.dp))
                }
                Text(
                    text = highlight(note.title.ifBlank { stringResource(R.string.untitled_note) }, query),
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            val preview = remember(note.checklist, note.content, hideChecked) { listPreviewText(note, hideChecked) }
            if (preview.isNotBlank()) {
                Text(
                    text = formattedPreview(preview, query),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = if (compact) 1 else 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
            Spacer(Modifier.height(6.dp))
            DateRow(note.updatedAt)
        }
        if (onToggleFavorite != null) {
            IconButton(onClick = onToggleFavorite) {
                Icon(
                    if (note.isFavorite) Icons.Filled.Star else Icons.Outlined.StarOutline,
                    stringResource(if (note.isFavorite) R.string.action_unfavorite else R.string.action_favorite),
                    tint = if (note.isFavorite) FavoriteGold else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else if (note.isFavorite) {
            Icon(Icons.Filled.Star, null, Modifier.size(18.dp), tint = FavoriteGold)
        }
        MenuButton(onMenu)
    }
}

private fun listPreviewText(note: Note, hideChecked: Boolean): String {
    val items = ChecklistCodec.decode(note.checklist).let { l -> if (hideChecked) l.filterNot { it.checked } else l }
    val list = items.take(3).joinToString("\n") { RichText.BULLET + it.text }
    return listOf(list, note.content).filter { it.isNotBlank() }.joinToString("\n")
}

@Composable
private fun MenuButton(onMenu: () -> Unit) {
    IconButton(onClick = onMenu, modifier = Modifier.size(36.dp)) {
        Icon(Icons.Filled.MoreVert, stringResource(R.string.a11y_note_options), tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun DateRow(time: Long) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Outlined.CalendarToday, null, Modifier.size(13.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f))
        Spacer(Modifier.width(6.dp))
        Text(formatCardDate(time), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun ChecklistPreview(checklist: String, query: String, hideChecked: Boolean, maxItems: Int) {
    val items = remember(checklist) { ChecklistCodec.decode(checklist) }
    if (items.isEmpty()) return
    val visible = (if (hideChecked) items.filterNot { it.checked } else items).take(maxItems)
    val extras = LocalAppExtras.current
    visible.forEach { item ->
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (item.checked) {
                Icon(Icons.Filled.Check, null, Modifier.size(16.dp), tint = extras.leaf)
            } else {
                Icon(Icons.Outlined.RadioButtonUnchecked, null, Modifier.size(15.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.width(8.dp))
            Text(
                text = highlight(item.text, query),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = if (item.checked) 0.55f else 0.9f),
                textDecoration = if (item.checked) TextDecoration.LineThrough else null,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
    val hidden = items.size - visible.size
    if (hidden > 0) {
        Text(
            stringResource(R.string.checklist_more, hidden),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 24.dp),
        )
    }
}
