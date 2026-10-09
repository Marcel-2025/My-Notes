package dev.flondy.mynotes.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import dev.flondy.mynotes.Note
import dev.flondy.mynotes.ui.theme.Motion
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.abs

/**
 * Zustand eines laufenden Drag-Vorgangs. Alle Koordinaten beziehen sich auf den Listen-Container.
 */
@Stable
class NoteDragState {
    var draggingId by mutableStateOf<Int?>(null)
        internal set
    /** Oberkante der schwebenden Karte (Overlay). */
    var overlayTop by mutableFloatStateOf(0f)
        internal set
    internal var pointerY by mutableFloatStateOf(0f)
    internal var grabOffset = 0f
    internal var itemHeight = 0f
    internal var startY = 0f
    internal var moved = false
    /** Zeitpunkt des letzten Drag-Endes – verhindert, dass das Loslassen als Klick gewertet wird. */
    var lastDragEndMillis = 0L
        internal set

    val isDragging: Boolean get() = draggingId != null
}

/**
 * Notizliste mit zuverlässigem Drag & Drop:
 *
 * - Lang drücken hebt die Karte an; sie wird als **Overlay oberhalb der Liste** gezeichnet und folgt
 *   exakt dem Finger. Sie kann dadurch nie hinter anderen Karten verschwinden und wird auch beim
 *   Auto-Scrollen nicht entsorgt.
 * - In der Liste bleibt an der Zielposition ein Platzhalter (Einfügemarkierung); die übrigen Karten
 *   machen animiert Platz.
 * - Am oberen/unteren Rand scrollt die Liste automatisch.
 * - Die Geste liegt auf dem Container und läuft im Initial-Pass: Sobald gezogen wird, bekommen
 *   LazyColumn (Scrollen) und Karte (Klick) keine Ereignisse mehr → keine Konflikte.
 * - Ein normaler Tipp öffnet weiterhin den Editor.
 * - Erst beim Loslassen wird die neue Reihenfolge einmalig gespeichert ([onReorder]).
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ReorderableNoteList(
    notes: List<Note>,
    reorderEnabled: Boolean,
    listState: LazyListState,
    contentPadding: PaddingValues,
    onReorder: (List<Int>) -> Unit,
    onLongPressWithoutMove: (Note) -> Unit,
    modifier: Modifier = Modifier,
    dragState: NoteDragState = remember { NoteDragState() },
    itemContent: @Composable (note: Note, isPlaceholder: Boolean, isOverlay: Boolean) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current
    val density = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current

    // Lokale Arbeitskopie während des Ziehens; danach die gespeicherte Reihenfolge bis die DB nachzieht.
    var working by remember { mutableStateOf(notes) }
    var pendingOrder by remember { mutableStateOf<List<Int>?>(null) }

    LaunchedEffect(notes, dragState.draggingId) {
        if (dragState.draggingId != null) return@LaunchedEffect
        val pending = pendingOrder
        working = if (pending != null) {
            if (notes.map { it.id } == pending || notes.map { it.id }.toSet() != pending.toSet()) {
                pendingOrder = null
                notes
            } else {
                // DB hat die neue Reihenfolge noch nicht geliefert → neue Daten in gemerkter Reihenfolge zeigen
                val byId = notes.associateBy { it.id }
                pending.mapNotNull(byId::get)
            }
        } else notes
    }

    val currentNotes by rememberUpdatedState(working)
    val currentOnReorder by rememberUpdatedState(onReorder)
    val currentOnLongPress by rememberUpdatedState(onLongPressWithoutMove)

    fun noteIndex(id: Int?) = currentNotes.indexOfFirst { it.id == id }

    /** Prüft, ob die schwebende Karte über einer anderen liegt, und verschiebt den Platzhalter. */
    fun checkForSwap() {
        val id = dragState.draggingId ?: return
        val layout = listState.layoutInfo
        val center = dragState.overlayTop + dragState.itemHeight / 2f
        val target = layout.visibleItemsInfo.firstOrNull { info ->
            info.key != id && info.key is Int && center >= info.offset && center <= info.offset + info.size
        } ?: return
        val from = noteIndex(id)
        val to = noteIndex(target.key as Int)
        if (from < 0 || to < 0 || from == to) return
        // Angeheftete und normale Notizen bleiben jeweils in ihrer Gruppe
        if (currentNotes[from].isPinned != currentNotes[to].isPinned) return
        val targetCenter = target.offset + target.size / 2f
        val passed = if (to > from) center > targetCenter else center < targetCenter
        if (!passed) return

        // Verhindert, dass die Liste beim Verschieben des ersten sichtbaren Elements „wegspringt“
        val firstIndex = listState.firstVisibleItemIndex
        val firstOffset = listState.firstVisibleItemScrollOffset
        if (from == firstIndex || to == firstIndex) {
            scope.launch { listState.scrollToItem(firstIndex, firstOffset) }
        }
        working = currentNotes.toMutableList().apply { add(to, removeAt(from)) }
        dragState.moved = true
    }

    fun endDrag(commit: Boolean) {
        val id = dragState.draggingId ?: return
        if (commit && dragState.moved) {
            val order = currentNotes.map { it.id }
            pendingOrder = order
            currentOnReorder(order)
        } else if (!commit) {
            working = notes
        } else if (!dragState.moved && abs(dragState.pointerY - dragState.startY) < with(density) { 8.dp.toPx() }) {
            currentNotes.firstOrNull { it.id == id }?.let(currentOnLongPress)
        }
        dragState.draggingId = null
        dragState.moved = false
        dragState.lastDragEndMillis = System.currentTimeMillis()
    }

    // Auto-Scroll, solange sich der Finger nahe am Rand befindet
    LaunchedEffect(dragState.draggingId != null) {
        if (dragState.draggingId == null) return@LaunchedEffect
        val edge = with(density) { 88.dp.toPx() }
        val maxSpeed = with(density) { 18.dp.toPx() }
        while (isActive && dragState.draggingId != null) {
            val viewport = listState.layoutInfo.viewportSize.height.toFloat()
            val y = dragState.pointerY
            val speed = when {
                y < edge && listState.canScrollBackward -> -maxSpeed * ((edge - y) / edge).coerceIn(0.15f, 1f)
                y > viewport - edge && listState.canScrollForward -> maxSpeed * ((y - (viewport - edge)) / edge).coerceIn(0.15f, 1f)
                else -> 0f
            }
            if (speed != 0f) {
                listState.scrollBy(speed)
                checkForSwap()
            }
            withFrameNanos { }
        }
    }

    BoxWithConstraints(modifier) {
        val startPad = contentPadding.calculateStartPadding(layoutDirection)
        val endPad = contentPadding.calculateEndPadding(layoutDirection)
        val viewportPx = with(density) { maxHeight.toPx() }

        LazyColumn(
            state = listState,
            contentPadding = contentPadding,
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier
                .fillMaxSize()
                .testTag("note_list")
                .pointerInput(reorderEnabled) {
                    if (!reorderEnabled) return@pointerInput
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                        // Auf langes Drücken warten, ohne Scrollen oder Klicks zu stören
                        val longPress = withTimeoutOrNull(viewConfiguration.longPressTimeoutMillis) {
                            while (true) {
                                val event = awaitPointerEvent(PointerEventPass.Initial)
                                val change = event.changes.firstOrNull { it.id == down.id } ?: return@withTimeoutOrNull false
                                if (change.changedToUpIgnoreConsumed() || change.isConsumed) return@withTimeoutOrNull false
                                if ((change.position - down.position).getDistance() > viewConfiguration.touchSlop) {
                                    return@withTimeoutOrNull false
                                }
                            }
                            @Suppress("UNREACHABLE_CODE") false
                        }
                        if (longPress != null) return@awaitEachGesture // abgebrochen → normales Scrollen/Klicken

                        val y = down.position.y
                        val item = listState.layoutInfo.visibleItemsInfo.firstOrNull {
                            it.key is Int && y >= it.offset && y <= it.offset + it.size
                        } ?: return@awaitEachGesture

                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        dragState.itemHeight = item.size.toFloat()
                        dragState.grabOffset = y - item.offset
                        dragState.pointerY = y
                        dragState.startY = y
                        dragState.overlayTop = item.offset.toFloat()
                        dragState.moved = false
                        dragState.draggingId = item.key as Int

                        var committed = false
                        while (true) {
                            val event = awaitPointerEvent(PointerEventPass.Initial)
                            val change = event.changes.firstOrNull { it.id == down.id }
                            if (change == null) break
                            if (change.changedToUpIgnoreConsumed()) {
                                change.consume()
                                committed = true
                                break
                            }
                            val dy = change.positionChange().y
                            change.consume()
                            dragState.pointerY += dy
                            dragState.overlayTop = (dragState.pointerY - dragState.grabOffset)
                                .coerceIn(-dragState.itemHeight / 2f, viewportPx - dragState.itemHeight / 2f)
                            checkForSwap()
                        }
                        endDrag(commit = committed)
                    }
                },
        ) {
            items(currentNotes, key = { it.id }, contentType = { it.noteType }) { note ->
                val isPlaceholder = note.id == dragState.draggingId
                Box(
                    Modifier.animateItem(
                        fadeInSpec = Motion.tween(220),
                        placementSpec = Motion.placement(),
                        fadeOutSpec = Motion.tween(180),
                    )
                ) {
                    itemContent(note, isPlaceholder, false)
                }
            }
        }

        // Die schwebende Karte – immer über allen anderen Karten
        val draggedNote = dragState.draggingId?.let { id -> currentNotes.firstOrNull { it.id == id } }
        if (draggedNote != null) {
            Box(
                Modifier
                    .zIndex(10f)
                    .fillMaxWidth()
                    .padding(start = startPad, end = endPad)
                    .graphicsLayer {
                        translationY = dragState.overlayTop
                        scaleX = 1.03f
                        scaleY = 1.03f
                    }
            ) {
                itemContent(draggedNote, false, true)
            }
        }
    }
}
