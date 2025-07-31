package com.example.myapplication

import android.app.Application // Für ThemeViewModel Preview
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.NoteAdd
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Brightness4
import androidx.compose.material.icons.filled.Brightness7
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.isEmpty
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.myapplication.ui.theme.MyApplicationTheme
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

// Drag & Drop Helfer
data class DragDropState(
    var isDragging: Boolean = false,
    var dragOffset: Float = 0f, // Vertikaler Offset während des Ziehens
    var draggedItemIndex: Int? = null,
    var initialDraggedItemIndex: Int? = null
)

// Hilfsfunktion für Zeitstempel-Formatierung
fun formatTimestamp(timestamp: Long): String {
    val sdf = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}


class MainActivity : ComponentActivity() {
    private val notesViewModel: NotesViewModel by viewModels()
    private val themeViewModel: ThemeViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val useDarkTheme by themeViewModel.isDarkThemeEnabled.collectAsStateWithLifecycle()
            // notes wird jetzt direkt in NoteAppScreen per collectAsStateWithLifecycle geholt
            // um Recomposition-Probleme mit displayedNotes zu vermeiden, wenn notes direkt übergeben wird.

            MyApplicationTheme(useDarkTheme = useDarkTheme) {
                NoteAppScreen( // notes hier nicht mehr direkt übergeben
                    notesViewModel = notesViewModel, // ViewModel übergeben
                    onAddNote = { title, content -> notesViewModel.addNote(title, content) },
                    onDeleteNote = { note -> notesViewModel.removeNote(note) },
                    onMoveNote = { from, to -> notesViewModel.moveNote(from, to) },
                    themeViewModel = themeViewModel,
                    isDarkTheme = useDarkTheme
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun NoteAppScreen(
    notesViewModel: NotesViewModel, // ViewModel hier
    onAddNote: (String, String) -> Unit,
    onDeleteNote: (Note) -> Unit,
    onMoveNote: (from: Int, to: Int) -> Unit,
    themeViewModel: ThemeViewModel,
    isDarkTheme: Boolean
) {
    // Original-Notizen direkt vom ViewModel holen
    val notes by notesViewModel.notes.collectAsStateWithLifecycle()

    var showDialog by remember { mutableStateOf(false) }
    var newNoteTitle by remember { mutableStateOf("") }
    var newNoteContent by remember { mutableStateOf("") }

    val lazyListState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    var overscrollJob by remember { mutableStateOf<Job?>(null) }
    val dragDropState = remember { DragDropState() }

    // KORRIGIERTE LOGIK FÜR displayedNotes:
    var displayedNotes by remember { mutableStateOf(notes) }

    LaunchedEffect(notes) { // Key ist 'notes' vom ViewModel
        if (!dragDropState.isDragging) {
            Log.d("NoteAppScreen", "LaunchedEffect(notes): Updating displayedNotes. New notes size: ${notes.size}")
            displayedNotes = notes
        } else {
            Log.d("NoteAppScreen", "LaunchedEffect(notes): Dragging is active, not updating displayedNotes from source notes.")
        }
    }

    LaunchedEffect(dragDropState.isDragging) { // Key ist nur 'isDragging'
        if (!dragDropState.isDragging) {
            // Wenn das Ziehen beendet wurde, sorge dafür, dass displayedNotes mit den
            // originalen 'notes' synchronisiert ist.
            // Die originalen 'notes' sollten durch onMoveNote bereits korrekt sein.
            Log.d("NoteAppScreen", "LaunchedEffect(isDragging): Dragging ended. Syncing displayedNotes with notes.")
            if (displayedNotes.map { it.id } != notes.map { it.id } || displayedNotes.size != notes.size) {
                Log.d("NoteAppScreen", "LaunchedEffect(isDragging): displayedNotes differs from notes. Updating. Displayed: ${displayedNotes.map {it.id}}, Notes: ${notes.map{it.id}}")
                displayedNotes = notes
            }
        }
    }


    var isInEditMode by remember { mutableStateOf(false) }

    LaunchedEffect(isInEditMode) {
        if (!isInEditMode && dragDropState.isDragging) {
            overscrollJob?.cancel()
            dragDropState.isDragging = false
            dragDropState.dragOffset = 0f
            dragDropState.draggedItemIndex = null
            dragDropState.initialDraggedItemIndex = null
            // displayedNotes wird durch LaunchedEffect(dragDropState.isDragging) aktualisiert
            Log.d("NoteAppScreen", "LaunchedEffect(isInEditMode): Edit mode left while dragging. Reset drag state.")
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Meine Notizen", style = MaterialTheme.typography.headlineSmall) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                actions = {
                    if (isInEditMode) {
                        TextButton(onClick = { isInEditMode = false }) {
                            Text("Fertig", color = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                    }
                    IconButton(onClick = { themeViewModel.toggleTheme() }) {
                        Icon(
                            imageVector = if (isDarkTheme) Icons.Filled.Brightness7 else Icons.Filled.Brightness4,
                            contentDescription = "Toggle Dark Mode"
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    if (isInEditMode) isInEditMode = false
                    showDialog = true
                },
                shape = CircleShape,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Notiz hinzufügen")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            if (displayedNotes.isEmpty() && !isInEditMode) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.NoteAdd,
                            contentDescription = "Keine Notizen",
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.secondary
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Keine Notizen vorhanden.", style = MaterialTheme.typography.titleMedium)
                        Text("Füge eine neue Notiz hinzu!", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else {
                LazyColumn(
                    state = lazyListState,
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(
                        items = displayedNotes,
                        key = { _, note -> note.id }
                    ) { index, note ->
                        val isCurrentlyBeingDragged = dragDropState.isDragging && dragDropState.draggedItemIndex == index

                        NoteItem(
                            note = note,
                            isBeingDragged = isCurrentlyBeingDragged,
                            isInEditMode = isInEditMode,
                            onDeleteClick = {
                                onDeleteNote(note)
                                if (displayedNotes.size == 1 && isInEditMode) { // Nur noch ein Item und es wird gelöscht
                                    isInEditMode = false
                                }
                            },
                            onItemLongClick = {
                                if (!dragDropState.isDragging) {
                                    isInEditMode = !isInEditMode
                                }
                            },
                            onDragStartFromHandle = {
                                if (isInEditMode && index < displayedNotes.size) {
                                    dragDropState.isDragging = true
                                    dragDropState.draggedItemIndex = index
                                    dragDropState.initialDraggedItemIndex = index
                                    dragDropState.dragOffset = 0f
                                    Log.d("DragDrop", "onDragStartFromHandle: Item $index, ID ${note.id}")
                                }
                            },
                            onDragFromHandle = { change, dragAmount ->
                                if (dragDropState.isDragging && isInEditMode) {
                                    change.consume()
                                    dragDropState.dragOffset += dragAmount.y

                                    val currentDraggedIndex = dragDropState.draggedItemIndex ?: return@NoteItem
                                    if (currentDraggedIndex >= displayedNotes.size) return@NoteItem // Sicherheitscheck

                                    val draggedItemInfo = lazyListState.layoutInfo.visibleItemsInfo
                                        .firstOrNull { it.index == currentDraggedIndex }

                                    val draggedItemCenterY = (draggedItemInfo?.offset?.toFloat() ?: 0f) +
                                            (draggedItemInfo?.size?.toFloat()?.div(2f) ?: 0f) +
                                            dragDropState.dragOffset

                                    var newTargetIndex = currentDraggedIndex
                                    lazyListState.layoutInfo.visibleItemsInfo
                                        .filter { it.index != currentDraggedIndex }
                                        .forEach { otherItemInfo ->
                                            val otherItemCenterY = otherItemInfo.offset.toFloat() + otherItemInfo.size.toFloat() / 2f
                                            val threshold = otherItemInfo.size.toFloat() * 0.3f // Schwelle, um "einzurasten"

                                            if (dragAmount.y < 0) { // Nach oben ziehen
                                                if (draggedItemCenterY < otherItemCenterY + threshold && otherItemInfo.index < newTargetIndex) {
                                                    newTargetIndex = otherItemInfo.index
                                                }
                                            } else { // Nach unten ziehen
                                                if (draggedItemCenterY > otherItemCenterY - threshold && otherItemInfo.index > newTargetIndex) {
                                                    newTargetIndex = otherItemInfo.index
                                                }
                                            }
                                        }

                                    if (newTargetIndex != currentDraggedIndex && newTargetIndex < displayedNotes.size) {
                                        Log.d("DragDrop", "Moving item from $currentDraggedIndex to $newTargetIndex")
                                        val tempList = displayedNotes.toMutableList()
                                        val movedItem = tempList.removeAt(currentDraggedIndex)
                                        tempList.add(newTargetIndex, movedItem)
                                        displayedNotes = tempList
                                        dragDropState.draggedItemIndex = newTargetIndex
                                    }

                                    val layoutInfo = lazyListState.layoutInfo
                                    val viewportHeight = layoutInfo.viewportSize.height
                                    val draggedItemVisualY = (draggedItemInfo?.offset?.toFloat() ?: 0f) + dragDropState.dragOffset

                                    if (draggedItemVisualY < layoutInfo.viewportStartOffset + viewportHeight * 0.2f && lazyListState.canScrollBackward) {
                                        overscrollJob?.cancel()
                                        overscrollJob = coroutineScope.launch { lazyListState.scrollBy(-35f) }
                                    } else if (draggedItemVisualY + (draggedItemInfo?.size?.toFloat() ?: 0f) > layoutInfo.viewportEndOffset - viewportHeight * 0.2f && lazyListState.canScrollForward) {
                                        overscrollJob?.cancel()
                                        overscrollJob = coroutineScope.launch { lazyListState.scrollBy(35f) }
                                    } else {
                                        overscrollJob?.cancel()
                                    }
                                }
                            },
                            onDragEndFromHandle = {
                                if (dragDropState.isDragging && isInEditMode) {
                                    overscrollJob?.cancel()
                                    val initialIndex = dragDropState.initialDraggedItemIndex
                                    val finalIndex = dragDropState.draggedItemIndex
                                    Log.d("DragDrop", "onDragEndFromHandle: Initial $initialIndex, Final $finalIndex")

                                    if (initialIndex != null && finalIndex != null && initialIndex != finalIndex &&
                                        initialIndex < notes.size && finalIndex < notes.size) { // Verwende notes.size für den Check
                                        Log.d("DragDrop", "Calling onMoveNote($initialIndex, $finalIndex)")
                                        onMoveNote(initialIndex, finalIndex)
                                    } else {
                                        Log.d("DragDrop", "onDragEndFromHandle: No move needed or invalid indices. Initial: $initialIndex, Final: $finalIndex, notes.size: ${notes.size}")
                                        // Wenn keine Bewegung stattfand, aber displayedNotes manipuliert wurde,
                                        // sorge dafür, dass es mit den originalen notes übereinstimmt.
                                        // Dies wird auch durch den LaunchedEffect(dragDropState.isDragging) gehandhabt.
                                    }
                                }
                                dragDropState.isDragging = false
                                // dragDropState.dragOffset = 0f // Offset nicht hier zurücksetzen
                                dragDropState.draggedItemIndex = null
                                dragDropState.initialDraggedItemIndex = null
                                // displayedNotes wird durch LaunchedEffect(dragDropState.isDragging) aktualisiert
                            },
                            onDragCancelFromHandle = {
                                overscrollJob?.cancel()
                                Log.d("DragDrop", "onDragCancelFromHandle")
                                dragDropState.isDragging = false
                                dragDropState.dragOffset = 0f
                                dragDropState.draggedItemIndex = null
                                dragDropState.initialDraggedItemIndex = null
                                // displayedNotes wird durch LaunchedEffect(dragDropState.isDragging) zurückgesetzt
                            },
                            modifier = Modifier
                                .animateItemPlacement(
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioMediumBouncy,
                                        stiffness = Spring.StiffnessLow
                                    )
                                )
                                .graphicsLayer {
                                    if (isCurrentlyBeingDragged) {
                                        translationY = dragDropState.dragOffset
                                        scaleX = 1.02f
                                        scaleY = 1.02f
                                        alpha = 0.97f
                                    }
                                }
                                .zIndex(if (isCurrentlyBeingDragged) 1f else 0f)
                                .shadow(
                                    if (isCurrentlyBeingDragged) 8.dp else 2.dp,
                                    RoundedCornerShape(12.dp)
                                )
                        )
                    }
                }
            }
        }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Neue Notiz") },
            text = {
                Column {
                    OutlinedTextField(
                        value = newNoteTitle,
                        onValueChange = { newNoteTitle = it },
                        label = { Text("Titel") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newNoteContent,
                        onValueChange = { newNoteContent = it },
                        label = { Text("Inhalt") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 80.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onAddNote(newNoteTitle.trim(), newNoteContent.trim())
                        newNoteTitle = ""
                        newNoteContent = ""
                        showDialog = false
                    }
                ) { Text("Hinzufügen") }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) { Text("Abbrechen") }
            }
        )
    }
}

@Stable
fun Modifier.jiggleEffect(enabled: Boolean): Modifier = composed {
    if (enabled) {
        val infiniteTransition = rememberInfiniteTransition(label = "jiggle_transition")
        val angle by infiniteTransition.animateFloat(
            initialValue = -1.0f,
            targetValue = 1.0f,
            animationSpec = infiniteRepeatable(
                animation = keyframes {
                    durationMillis = 300
                    -0.7f at 0
                    0.7f at (durationMillis / 4)
                    -1.0f at (durationMillis / 2)
                    1.0f at (durationMillis * 3 / 4)
                    -0.7f at durationMillis
                },
                repeatMode = RepeatMode.Restart
            ),
            label = "jiggle_angle"
        )
        this.graphicsLayer {
            rotationZ = angle
        }
    } else {
        this.graphicsLayer { rotationZ = 0f } // Zurücksetzen, wenn nicht aktiviert
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NoteItem(
    note: Note,
    isBeingDragged: Boolean,
    isInEditMode: Boolean,
    onDeleteClick: () -> Unit,
    onItemLongClick: () -> Unit,
    onDragStartFromHandle: () -> Unit,
    onDragFromHandle: (change: PointerInputChange, dragAmount: Offset) -> Unit,
    onDragEndFromHandle: () -> Unit,
    onDragCancelFromHandle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val elevation by animateDpAsState(if (isBeingDragged) 8.dp else 2.dp, label = "elevation_animation_noteitem")
    val cardContainerColor by animateColorAsState(
        targetValue = if (isBeingDragged) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f)
        else MaterialTheme.colorScheme.surfaceVariant,
        label = "card_color_animation_noteitem"
    )

    val itemInteractionModifier = Modifier.combinedClickable(
        onClick = { /* Implement if needed */ },
        onLongClick = {
            if (!isBeingDragged) {
                onItemLongClick()
            }
        }
    )

    Card(
        modifier = modifier
            .jiggleEffect(enabled = isInEditMode && !isBeingDragged)
            .then(itemInteractionModifier)
            .fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = elevation),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = cardContainerColor)
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 8.dp, vertical = 10.dp), // Weniger horizontal padding
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isInEditMode) {
                Icon(
                    imageVector = Icons.Filled.DragHandle,
                    contentDescription = "Notiz verschieben",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier
                        .size(40.dp) // Etwas größerer Klickbereich
                        .padding(start = 4.dp, end = 10.dp) // Abstand zum Rand und zum Text
                        .pointerInput(note.id) {
                            detectDragGesturesAfterLongPress(
                                onDragStart = { _ -> onDragStartFromHandle() },
                                onDrag = { change, dragAmount ->
                                    onDragFromHandle(
                                        change,
                                        dragAmount
                                    )
                                },
                                onDragEnd = { onDragEndFromHandle() },
                                onDragCancel = { onDragCancelFromHandle() }
                            )
                        }
                )
            } else {
                // Platzhalter, damit die Ausrichtung gleich bleibt, wenn das Handle nicht da ist
                // Spacer(modifier = Modifier.width(40.dp + 4.dp + 10.dp)) // Breite des Handles + Paddings
                // Oder, wenn der Text direkt links beginnen soll, wenn kein Handle da ist:
                Spacer(modifier = Modifier.width(4.dp)) // Kleiner Abstand links, wenn kein Handle
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = note.title.ifBlank { "Unbenannte Notiz" },
                    style = MaterialTheme.typography.titleMedium,
                    color = if (isBeingDragged) MaterialTheme.colorScheme.onPrimaryContainer
                    else MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (note.content.isNotBlank()) {
                    Text(
                        text = note.content,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isBeingDragged) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(
                    text = formatTimestamp(note.timestamp),
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 10.sp,
                    color = (if (isBeingDragged) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f)
                    else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
                )
            }

            if (isInEditMode) {
                IconButton(
                    onClick = onDeleteClick,
                    modifier = Modifier
                        .size(40.dp)
                        .padding(start = 4.dp) // Etwas Abstand zum Text
                ) {
                    Icon(
                        Icons.Filled.Delete,
                        contentDescription = "Notiz löschen",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                    )
                }
            } else {
                // Platzhalter, damit die Ausrichtung gleich bleibt
                // Spacer(modifier = Modifier.width(40.dp + 4.dp))
            }
        }
    }
}


// --- Previews ---
@Preview(showBackground = true, name = "NoteAppScreen - Light Empty")
@Composable
fun NoteAppScreenPreviewLightEmpty() {
    val dummyNotesViewModel = NotesViewModel(Application())
    // Clear notes for empty preview
    LaunchedEffect(Unit) {
        val currentNotes = dummyNotesViewModel.notes.first()
        currentNotes.forEach { dummyNotesViewModel.removeNote(it) }
    }

    MyApplicationTheme(useDarkTheme = false) {
        NoteAppScreen(
            notesViewModel = dummyNotesViewModel,
            onAddNote = { t, c -> dummyNotesViewModel.addNote(t, c) },
            onDeleteNote = { n -> dummyNotesViewModel.removeNote(n) },
            onMoveNote = { f, t -> dummyNotesViewModel.moveNote(f, t) },
            themeViewModel = ThemeViewModel(Application()),
            isDarkTheme = false
        )
    }
}

@Preview(showBackground = true, name = "NoteAppScreen - Light With Notes")
@Composable
fun NoteAppScreenPreviewLightWithNotes() {
    // ViewModel wird mit initialen Notizen erstellt
    val dummyNotesViewModel = NotesViewModel(Application())

    MyApplicationTheme(useDarkTheme = false) {
        NoteAppScreen(
            notesViewModel = dummyNotesViewModel,
            onAddNote = { t, c -> dummyNotesViewModel.addNote(t, c) },
            onDeleteNote = { n -> dummyNotesViewModel.removeNote(n) },
            onMoveNote = { f, t -> dummyNotesViewModel.moveNote(f, t) },
            themeViewModel = ThemeViewModel(Application()),
            isDarkTheme = false
        )
    }
}


@Preview(showBackground = true, name = "NoteItem - Light")
@Composable
fun NoteItemPreviewLight() {
    MyApplicationTheme(useDarkTheme = false) {
        NoteItem(
            note = Note(1, "Test Titel Preview", "Dies ist der Inhalt einer Testnotiz, die etwas länger sein kann.", System.currentTimeMillis(), 0),
            isBeingDragged = false,
            isInEditMode = false,
            onDeleteClick = {},
            onItemLongClick = {},
            onDragStartFromHandle = {},
            onDragFromHandle = { _, _ -> },
            onDragEndFromHandle = {},
            onDragCancelFromHandle = {}
        )
    }
}

@Preview(showBackground = true, name = "NoteItem - Edit Mode")
@Composable
fun NoteItemPreviewEditMode() {
    MyApplicationTheme(useDarkTheme = false) {
        NoteItem(
            note = Note(1, "Edit Modus Titel", "Inhalt der Notiz im Bearbeitungsmodus.", System.currentTimeMillis(), 0),
            isBeingDragged = false,
            isInEditMode = true,
            onDeleteClick = {},
            onItemLongClick = {},
            onDragStartFromHandle = {},
            onDragFromHandle = { _, _ -> },
            onDragEndFromHandle = {},
            onDragCancelFromHandle = {}
        )
    }
}

@Preview(showBackground = true, name = "NoteItem - Being Dragged")
@Composable
fun NoteItemPreviewBeingDragged() {
    MyApplicationTheme(useDarkTheme = false) {
        NoteItem(
            note = Note(1, "Wird gezogen Titel", "Diese Notiz wird gerade verschoben.", System.currentTimeMillis(), 0),
            isBeingDragged = true,
            isInEditMode = true,
            onDeleteClick = {},
            onItemLongClick = {},
            onDragStartFromHandle = {},
            onDragFromHandle = { _, _ -> },
            onDragEndFromHandle = {},
            onDragCancelFromHandle = {}
        )
    }
}
