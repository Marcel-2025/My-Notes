package dev.flondy.mynotes.ui.components

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridItemInfo
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import dev.flondy.mynotes.Note
import dev.flondy.mynotes.ui.theme.Motion
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.abs

/** Zustand eines laufenden Drag-Vorgangs. Koordinaten beziehen sich auf den Listen-Container. */
@Stable
class NoteDragState {
    var draggingId by mutableStateOf<Int?>(null)
        internal set
    var overlayTop by mutableFloatStateOf(0f)
        internal set
    var overlayLeft by mutableFloatStateOf(0f)
        internal set
    internal var pointerX by mutableFloatStateOf(0f)
    internal var pointerY by mutableFloatStateOf(0f)
    internal var grabX = 0f
    internal var grabY = 0f
    internal var itemHeight = 0f
    internal var itemWidth by mutableIntStateOf(0)
    internal var startX = 0f
    internal var startY = 0f
    internal var moved = false
    /** Zeitpunkt des letzten Drag-Endes – verhindert, dass das Loslassen als Klick gewertet wird. */
    var lastDragEndMillis = 0L
        internal set

    val isDragging: Boolean get() = draggingId != null
}

/**
 * Notizliste bzw. -raster mit zuverlässigem Drag & Drop.
 *
 * - Lang drücken hebt die Karte an; sie wird als **Overlay oberhalb der Liste** gezeichnet und folgt
 *   exakt dem Finger (auch seitlich im Raster). Sie kann nie hinter anderen Karten verschwinden.
 * - An der Zielposition bleibt ein Platzhalter (Einfügemarkierung); die übrigen Karten machen animiert Platz.
 * - Am oberen/unteren Rand scrollt die Liste automatisch.
 * - Die Geste liegt auf dem Container (Initial-Pass): Während des Ziehens bekommen Scrollen und Klick
 *   keine Ereignisse → keine Konflikte. Ein normaler Tipp öffnet weiterhin den Editor.
 * - Erst beim Loslassen wird die neue Reihenfolge einmalig gespeichert ([onReorder]).
 *
 * @param columns z. B. GridCells.Fixed(1) für die Liste, GridCells.Adaptive(160.dp) für das Raster
 * @param header optionale Elemente oberhalb der Notizen (Schlüssel dürfen keine Int sein)
 */
@Composable
fun ReorderableNoteList(
    notes: List<Note>,
    reorderEnabled: Boolean,
    listState: LazyGridState,
    contentPadding: PaddingValues,
    onReorder: (List<Int>) -> Unit,
    onLongPressWithoutMove: (Note) -> Unit,
    modifier: Modifier = Modifier,
    columns: GridCells = GridCells.Fixed(1),
    dragState: NoteDragState = remember { NoteDragState() },
    header: (LazyGridScope.() -> Unit)? = null,
    itemContent: @Composable (note: Note, isPlaceholder: Boolean, isOverlay: Boolean) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current
    val density = LocalDensity.current

    // Lokale Arbeitskopie während des Ziehens; danach die gespeicherte Reihenfolge, bis die DB nachzieht.
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
                val byId = notes.associateBy { it.id }
                pending.mapNotNull(byId::get)
            }
        } else notes
    }

    val currentNotes by rememberUpdatedState(working)
    val currentOnReorder by rememberUpdatedState(onReorder)
    val currentOnLongPress by rememberUpdatedState(onLongPressWithoutMove)

    fun noteIndex(id: Any?) = currentNotes.indexOfFirst { it.id == id }

    fun itemAt(x: Float, y: Float): LazyGridItemInfo? = listState.layoutInfo.visibleItemsInfo.firstOrNull {
        it.key is Int && x >= it.offset.x && x <= it.offset.x + it.size.width && y >= it.offset.y && y <= it.offset.y + it.size.height
    }

    /** Liegt die Mitte der schwebenden Karte über einer anderen Karte, wandert der Platzhalter dorthin. */
    fun checkForSwap() {
        val id = dragState.draggingId ?: return
        val cx = dragState.overlayLeft + dragState.itemWidth / 2f
        val cy = dragState.overlayTop + dragState.itemHeight / 2f
        val target = itemAt(cx, cy)?.takeIf { it.key != id } ?: return
        val from = noteIndex(id)
        val to = noteIndex(target.key)
        if (from < 0 || to < 0 || from == to) return
        // Angeheftete und normale Notizen bleiben jeweils in ihrer Gruppe
        if (currentNotes[from].isPinned != currentNotes[to].isPinned) return
        val dragged = listState.layoutInfo.visibleItemsInfo.firstOrNull { it.key == id }
        val tx = target.offset.x.toFloat()
        val ty = target.offset.y.toFloat()
        // Hysterese: erst wechseln, wenn die schwebende Karte ein gutes Stück über dem Ziel liegt
        // (nicht exakt die Mitte – am Bildschirmrand kann die Karte die Mitte sonst nie erreichen)
        val passed = if (dragged != null && dragged.row == target.row) {
            if (to > from) cx > tx + target.size.width * 0.3f else cx < tx + target.size.width * 0.7f
        } else {
            if (to > from) cy > ty + target.size.height * 0.35f else cy < ty + target.size.height * 0.65f
        }
        if (!passed) return

        // Verhindert, dass die Liste beim Verschieben des ersten sichtbaren Elements „wegspringt“
        val firstIndex = listState.firstVisibleItemIndex
        val firstOffset = listState.firstVisibleItemScrollOffset
        val firstKey = listState.layoutInfo.visibleItemsInfo.firstOrNull()?.key
        if (firstKey == id || firstKey == target.key) {
            scope.launch { listState.scrollToItem(firstIndex, firstOffset) }
        }
        working = currentNotes.toMutableList().apply { add(to, removeAt(from)) }
        dragState.moved = true
    }

    fun endDrag(commit: Boolean) {
        val id = dragState.draggingId ?: return
        val moveDistance = abs(dragState.pointerY - dragState.startY) + abs(dragState.pointerX - dragState.startX)
        if (commit && dragState.moved) {
            val order = currentNotes.map { it.id }
            pendingOrder = order
            currentOnReorder(order)
        } else if (!commit) {
            working = notes
        } else if (!dragState.moved && moveDistance < with(density) { 8.dp.toPx() }) {
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
        val viewportPx = with(density) { maxHeight.toPx() }
        val widthPx = with(density) { maxWidth.toPx() }

        LazyVerticalGrid(
            columns = columns,
            state = listState,
            contentPadding = contentPadding,
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxSize()
                .testTag("note_list")
                .pointerInput(reorderEnabled) {
                    if (!reorderEnabled) return@pointerInput
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                        // Auf langes Drücken warten, ohne Scrollen oder Klicks zu stören
                        val cancelled = withTimeoutOrNull(viewConfiguration.longPressTimeoutMillis) {
                            while (true) {
                                val event = awaitPointerEvent(PointerEventPass.Initial)
                                val change = event.changes.firstOrNull { it.id == down.id } ?: return@withTimeoutOrNull true
                                if (change.changedToUpIgnoreConsumed() || change.isConsumed) return@withTimeoutOrNull true
                                if ((change.position - down.position).getDistance() > viewConfiguration.touchSlop) {
                                    return@withTimeoutOrNull true
                                }
                            }
                            @Suppress("UNREACHABLE_CODE") true
                        }
                        if (cancelled != null) return@awaitEachGesture

                        val item = itemAt(down.position.x, down.position.y) ?: return@awaitEachGesture
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        dragState.itemHeight = item.size.height.toFloat()
                        dragState.itemWidth = item.size.width
                        dragState.grabX = down.position.x - item.offset.x
                        dragState.grabY = down.position.y - item.offset.y
                        dragState.pointerX = down.position.x
                        dragState.pointerY = down.position.y
                        dragState.startX = down.position.x
                        dragState.startY = down.position.y
                        dragState.overlayLeft = item.offset.x.toFloat()
                        dragState.overlayTop = item.offset.y.toFloat()
                        dragState.moved = false
                        dragState.draggingId = item.key as Int

                        var committed = false
                        while (true) {
                            val event = awaitPointerEvent(PointerEventPass.Initial)
                            val change = event.changes.firstOrNull { it.id == down.id } ?: break
                            if (change.changedToUpIgnoreConsumed()) {
                                change.consume()
                                committed = true
                                break
                            }
                            val delta = change.positionChange()
                            change.consume()
                            dragState.pointerX += delta.x
                            dragState.pointerY += delta.y
                            dragState.overlayTop = (dragState.pointerY - dragState.grabY)
                                .coerceIn(-dragState.itemHeight / 2f, viewportPx - dragState.itemHeight / 2f)
                            dragState.overlayLeft = (dragState.pointerX - dragState.grabX)
                                .coerceIn(0f, (widthPx - dragState.itemWidth).coerceAtLeast(0f))
                            checkForSwap()
                        }
                        endDrag(commit = committed)
                    }
                },
        ) {
            header?.invoke(this)
            items(currentNotes, key = { it.id }, contentType = { if (it.isChecklist) 1 else 0 }) { note ->
                Box(
                    Modifier.animateItem(
                        fadeInSpec = Motion.tween(220),
                        placementSpec = Motion.placement(),
                        fadeOutSpec = Motion.tween(180),
                    )
                ) {
                    itemContent(note, note.id == dragState.draggingId, false)
                }
            }
        }

        // Die schwebende Karte – immer über allen anderen Karten
        val draggedNote = dragState.draggingId?.let { id -> currentNotes.firstOrNull { it.id == id } }
        if (draggedNote != null) {
            Box(
                Modifier
                    .zIndex(10f)
                    .width(with(density) { dragState.itemWidth.toDp() })
                    .graphicsLayer {
                        translationX = dragState.overlayLeft
                        translationY = dragState.overlayTop
                        scaleX = 1.04f
                        scaleY = 1.04f
                        rotationZ = if (dragState.moved) -1.2f else 0f
                    }
            ) {
                itemContent(draggedNote, false, true)
            }
        }
    }
}

/** Kopfzeilen-Element über die volle Breite des Rasters. */
fun LazyGridScope.fullWidthItem(key: String, content: @Composable () -> Unit) {
    item(key = key, span = { GridItemSpan(maxLineSpan) }, contentType = "header") { content() }
}
