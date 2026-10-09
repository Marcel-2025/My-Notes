package dev.flondy.mynotes.ui.components

import android.text.format.DateUtils
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.flondy.mynotes.Note
import dev.flondy.mynotes.R
import dev.flondy.mynotes.data.ChecklistCodec
import dev.flondy.mynotes.settings.CardDensity
import dev.flondy.mynotes.ui.theme.LocalAppExtras
import dev.flondy.mynotes.ui.theme.Motion
import dev.flondy.mynotes.ui.theme.noteColor

private val CardShape = RoundedCornerShape(20.dp)

@Composable
fun noteContainerColor(colorIndex: Int): Color {
    val extras = LocalAppExtras.current
    val c = noteColor(colorIndex)
    val custom = if (extras.isDark) c.dark else c.light
    return if (custom == Color.Unspecified) MaterialTheme.colorScheme.surfaceContainer else custom
}

/** Hebt alle Vorkommen des Suchbegriffs hervor (Groß-/Kleinschreibung egal). */
@Composable
fun highlight(text: String, query: String): AnnotatedString {
    val accent = LocalAppExtras.current.accent
    val needle = query.trim()
    if (needle.isEmpty()) return AnnotatedString(text)
    return remember(text, needle, accent) {
        buildAnnotatedString {
            val lower = text.lowercase()
            val n = needle.lowercase()
            var start = 0
            while (true) {
                val i = lower.indexOf(n, start)
                if (i < 0 || n.isEmpty()) {
                    append(text.substring(start)); break
                }
                append(text.substring(start, i))
                pushStyle(SpanStyle(background = accent.copy(alpha = 0.55f), color = Color(0xFF1A1806), fontWeight = FontWeight.SemiBold))
                append(text.substring(i, i + n.length))
                pop()
                start = i + n.length
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
    categoryName: String?,
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
) {
    val extras = LocalAppExtras.current
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val pressScale by animateFloatAsState(if (pressed && !isOverlay) 0.985f else 1f, Motion.spring(), label = "press")
    val elevation by animateDpAsState(if (isOverlay) 14.dp else 0.dp, Motion.spring(), label = "elevation")
    val container by animateColorAsState(noteContainerColor(note.color), Motion.tween(300), label = "container")
    val compact = density == CardDensity.COMPACT
    val pinnedLabel = stringResource(R.string.state_pinned)
    val favLabel = stringResource(R.string.state_favorite)

    Box(modifier.fillMaxWidth()) {
        Surface(
            shape = CardShape,
            color = container,
            shadowElevation = elevation,
            tonalElevation = 0.dp,
            modifier = Modifier
                .fillMaxWidth()
                .scale(pressScale)
                .alpha(if (isPlaceholder) 0f else 1f)
                .border(1.dp, if (isOverlay) extras.accent.copy(alpha = 0.7f) else extras.cardBorder, CardShape)
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
                    stateDescription = listOfNotNull(
                        pinnedLabel.takeIf { note.isPinned },
                        favLabel.takeIf { note.isFavorite },
                    ).joinToString(", ")
                }
                .animateContentSize(Motion.spring()),
        ) {
            Column(Modifier.padding(start = 16.dp, top = if (compact) 10.dp else 14.dp, bottom = if (compact) 10.dp else 14.dp, end = 4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (note.isChecklist) {
                        Icon(Icons.Outlined.Checklist, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.width(6.dp))
                    }
                    Text(
                        text = highlight(note.title.ifBlank { stringResource(R.string.untitled_note) }, searchQuery),
                        style = MaterialTheme.typography.titleMedium,
                        color = if (note.title.isBlank()) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                        maxLines = if (compact) 1 else 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    if (note.isPinned) {
                        Icon(Icons.Filled.PushPin, contentDescription = null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.width(4.dp))
                    }
                    if (note.isFavorite) {
                        Icon(Icons.Filled.Star, contentDescription = null, Modifier.size(18.dp), tint = Color(0xFFF5C518))
                    }
                    IconButton(onClick = onMenu, modifier = Modifier.size(40.dp)) {
                        Icon(Icons.Filled.MoreVert, stringResource(R.string.a11y_note_options), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                if (note.isChecklist) {
                    ChecklistPreview(note.content, searchQuery, hideCheckedItems, maxItems = if (compact) 2 else 4)
                } else if (note.content.isNotBlank()) {
                    Text(
                        text = highlight(note.content, searchQuery),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = if (compact) 2 else 5,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(end = 12.dp, top = 2.dp),
                    )
                }

                Spacer(Modifier.height(if (compact) 6.dp else 10.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (categoryName != null) {
                        Text(
                            text = categoryName,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.07f))
                                .padding(horizontal = 8.dp, vertical = 3.dp),
                        )
                    }
                    Text(
                        text = formatRelative(note.updatedAt),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                    )
                }
            }
        }

        // Einfügemarkierung an der Zielposition während des Ziehens
        if (isPlaceholder) {
            val accent = extras.accent
            Box(
                Modifier
                    .matchParentSize()
                    .clip(CardShape)
                    .background(accent.copy(alpha = 0.08f))
                    .drawBehind {
                        drawRoundRect(
                            color = accent.copy(alpha = 0.8f),
                            cornerRadius = CornerRadius(20.dp.toPx()),
                            style = Stroke(width = 2.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 10f))),
                        )
                    }
            )
        }
    }
}

@Composable
private fun ChecklistPreview(content: String, query: String, hideChecked: Boolean, maxItems: Int) {
    val items = remember(content) { ChecklistCodec.decode(content) }
    if (items.isEmpty()) return
    val done = items.count { it.checked }
    val visible = (if (hideChecked) items.filterNot { it.checked } else items.sortedBy { it.checked }).take(maxItems)
    Column(Modifier.padding(end = 12.dp, top = 4.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        visible.forEach { item ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    if (item.checked) Icons.Filled.CheckBox else Icons.Filled.CheckBoxOutlineBlank,
                    contentDescription = null,
                    Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = if (item.checked) 0.5f else 0.9f),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = highlight(item.text, query),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = if (item.checked) 0.55f else 1f),
                    textDecoration = if (item.checked) TextDecoration.LineThrough else null,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        val hidden = items.size - visible.size
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 6.dp)) {
            LinearProgressIndicator(
                progress = { if (items.isEmpty()) 0f else done / items.size.toFloat() },
                modifier = Modifier.width(56.dp).height(4.dp).clip(CircleShape),
                color = LocalAppExtras.current.accent,
                trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                drawStopIndicator = {},
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.checklist_progress, done, items.size) +
                    if (hidden > 0) " · " + stringResource(R.string.checklist_more, hidden) else "",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
