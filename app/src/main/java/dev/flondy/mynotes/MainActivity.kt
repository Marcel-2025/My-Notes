package dev.flondy.mynotes

import android.app.Activity
import android.app.Application
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.NoteAdd
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.consumeAllChanges
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.android.billingclient.api.*
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.MobileAds
import dev.flondy.mynotes.ui.theme.MyApplicationTheme
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


// --- 1. GLOBALER PREMIUM STATE ---
object PremiumManager {
    private val _isPremium = MutableStateFlow(false)
    val isPremium = _isPremium.asStateFlow()
    fun setPremium(value: Boolean) { _isPremium.value = value }
}

// --- 2. DRAG & DROP STATE ---
@Stable
class DragDropState {
    var isDragging by mutableStateOf(false)
    var dragOffset by mutableStateOf(0f)
    var draggedItemIndex by mutableStateOf<Int?>(null)
    var initialDraggedItemIndex by mutableStateOf<Int?>(null)
}

// --- 3. SORT MODE ---
enum class SortMode(val label: String) {
    MANUAL("Manuell"),
    NEWEST("Neueste"),
    OLDEST("Älteste"),
    TITLE_AZ("Titel A–Z"),
    TITLE_ZA("Titel Z–A")
}

// --- 4. HILFSFUNKTIONEN ---
fun formatTimestamp(timestamp: Long): String {
    val sdf = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

// --- 5. MAIN ACTIVITY ---
class MainActivity : ComponentActivity() {
    private val notesViewModel: NotesViewModel by viewModels()
    private val themeViewModel: ThemeViewModel by viewModels()
    private lateinit var billingClientWrapper: BillingClientWrapper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        MobileAds.initialize(this) {}
        billingClientWrapper = BillingClientWrapper(this)

        setContent {
            val useDarkTheme by themeViewModel.isDarkThemeEnabled.collectAsStateWithLifecycle()

            MyApplicationTheme(useDarkTheme = useDarkTheme) {
                var isImmersive by remember { mutableStateOf(false) }

                LaunchedEffect(isImmersive) {
                    val window = this@MainActivity.window
                    val controller = WindowInsetsControllerCompat(window, window.decorView)

                    if (isImmersive) {
                        WindowCompat.setDecorFitsSystemWindows(window, false)
                        controller.hide(WindowInsetsCompat.Type.systemBars())
                        controller.systemBarsBehavior =
                            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                    } else {
                        WindowCompat.setDecorFitsSystemWindows(window, true)
                        controller.show(WindowInsetsCompat.Type.systemBars())
                    }
                }

                AppRoot(
                    notesViewModel = notesViewModel,
                    themeViewModel = themeViewModel,
                    isDarkTheme = useDarkTheme,
                    billingClientWrapper = billingClientWrapper,
                    isImmersive = isImmersive,
                    onToggleImmersive = { isImmersive = !isImmersive }
                )
            }
        }
    }
}

// --- 6. APP ROOT ---
@Composable
fun AppRoot(
    notesViewModel: NotesViewModel,
    themeViewModel: ThemeViewModel,
    isDarkTheme: Boolean,
    billingClientWrapper: BillingClientWrapper,
    isImmersive: Boolean,
    onToggleImmersive: () -> Unit
) {
    val isPremium by PremiumManager.isPremium.collectAsState()

    var showPremiumBanner by rememberSaveable { mutableStateOf(true) }
    var showPremiumThanks by rememberSaveable { mutableStateOf(true) }
    var showSettings by rememberSaveable { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(Modifier.fillMaxSize()) {

            Box(Modifier.weight(1f)) {

                NoteAppScreen(
                    notesViewModel = notesViewModel,
                    themeViewModel = themeViewModel,
                    isDarkTheme = isDarkTheme,
                    onOpenSettings = { showSettings = true },
                    onRemoveAds = { billingClientWrapper.launchPurchaseFlow() },
                    isPremium = isPremium
                )

                if (isPremium && showPremiumBanner) {
                    PremiumBanner(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 70.dp),
                        onClose = { showPremiumBanner = false }
                    )
                }
            }

            if (!isPremium) {
                AdBanner()
            } else if (showPremiumThanks) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF0C0F1C))
                        .padding(vertical = 8.dp, horizontal = 16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Werbefrei dank Premium ✨ Danke!",
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 12.sp,
                            modifier = Modifier.weight(1f)
                        )
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Schließen",
                            tint = Color.White.copy(alpha = 0.7f),
                            modifier = Modifier
                                .size(18.dp)
                                .clickable { showPremiumThanks = false }
                        )
                    }
                }
            }
        }

        if (showSettings) {
            SettingsScreen(
                isImmersive = isImmersive,
                onToggleImmersive = onToggleImmersive,
                showPremiumBanner = showPremiumBanner,
                onChangeShowPremiumBanner = { showPremiumBanner = it },
                showPremiumThanks = showPremiumThanks,
                onChangeShowPremiumThanks = { showPremiumThanks = it },
                onClose = { showSettings = false }
            )
        }
    }
}

// --- 7. NOTE APP SCREEN ---
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun NoteAppScreen(
    notesViewModel: NotesViewModel,
    themeViewModel: ThemeViewModel,
    isDarkTheme: Boolean,
    onOpenSettings: () -> Unit,
    onRemoveAds: () -> Unit,
    isPremium: Boolean
) {
    val notes by notesViewModel.notes.collectAsStateWithLifecycle()

    // lokale Liste für live Drag-Reorder
    var displayedNotes by remember { mutableStateOf(notes) }

    val lazyListState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    var overscrollJob by remember { mutableStateOf<Job?>(null) }
    val dragDropState = remember { DragDropState() }

    // Search + Sort
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var sortMode by rememberSaveable { mutableStateOf(SortMode.MANUAL) }

    var isInEditMode by remember { mutableStateOf(false) }

    // Sync from DB only when not dragging
    LaunchedEffect(notes) {
        if (!dragDropState.isDragging) {
            displayedNotes = notes
        }
    }
    LaunchedEffect(dragDropState.isDragging) {
        if (!dragDropState.isDragging) {
            displayedNotes = notes
        }
    }

    // Wenn nicht MANUAL -> Edit Mode aus (Drag wäre sonst widersprüchlich)
    LaunchedEffect(sortMode) {
        if (sortMode != SortMode.MANUAL) {
            isInEditMode = false
        }
    }

    // Add Dialog
    var showAddDialog by remember { mutableStateOf(false) }
    var newNoteTitle by remember { mutableStateOf("") }
    var newNoteContent by remember { mutableStateOf("") }

    // Edit Dialog
    var editNote by remember { mutableStateOf<Note?>(null) }
    var editTitle by remember { mutableStateOf("") }
    var editContent by remember { mutableStateOf("") }

    // gefiltert + sortiert (nur Anzeige!)
    val filteredSortedNotes by remember(displayedNotes, searchQuery, sortMode) {
        mutableStateOf(
            displayedNotes
                .let { list ->
                    val q = searchQuery.trim()
                    if (q.isBlank()) list
                    else {
                        val lower = q.lowercase(Locale.getDefault())
                        list.filter {
                            it.title.lowercase(Locale.getDefault()).contains(lower) ||
                                    it.content.lowercase(Locale.getDefault()).contains(lower)
                        }
                    }
                }
                .let { list ->
                    when (sortMode) {
                        SortMode.MANUAL -> list
                        SortMode.NEWEST -> list.sortedByDescending { it.timestamp }
                        SortMode.OLDEST -> list.sortedBy { it.timestamp }
                        SortMode.TITLE_AZ -> list.sortedBy { it.title.lowercase(Locale.getDefault()) }
                        SortMode.TITLE_ZA -> list.sortedByDescending { it.title.lowercase(Locale.getDefault()) }
                    }
                }
        )
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
                    if (!isPremium) {
                        IconButton(onClick = onRemoveAds) {
                            Icon(
                                imageVector = Icons.Filled.Star,
                                contentDescription = "Remove Ads",
                                tint = Color(0xFFFDD835)
                            )
                        }
                    }

                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Filled.Settings, contentDescription = "Einstellungen")
                    }

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
                    showAddDialog = true
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
            // ✅ Search + Sort Bar
            SearchAndSortBar(
                searchQuery = searchQuery,
                onSearchChange = { searchQuery = it },
                sortMode = sortMode,
                onSortChange = { sortMode = it },
                isManual = sortMode == SortMode.MANUAL,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            if (filteredSortedNotes.isEmpty() && !isInEditMode) {
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
                        Text(
                            text = if (searchQuery.isBlank()) "Keine Notizen vorhanden."
                            else "Keine Treffer für „${searchQuery.trim()}“.",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = if (searchQuery.isBlank()) "Füge eine neue Notiz hinzu!"
                            else "Versuch’s mit einem anderen Suchbegriff.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    state = lazyListState,
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        top = if (isPremium) 8.dp else 8.dp,
                        bottom = 8.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(
                        items = filteredSortedNotes,
                        key = { _, note -> note.id }
                    ) { indexInFilteredList, note ->

                        // Für Drag brauchen wir Index in displayedNotes (manual order list)
                        val indexInDisplayed = displayedNotes.indexOfFirst { it.id == note.id }
                        val isDragged = dragDropState.isDragging && dragDropState.draggedItemIndex == indexInDisplayed

                        NoteItem(
                            note = note,
                            isBeingDragged = isDragged,
                            isInEditMode = isInEditMode,
                            onClick = {
                                if (!isInEditMode && !dragDropState.isDragging) {
                                    editNote = note
                                    editTitle = note.title
                                    editContent = note.content
                                }
                            },
                            onLongPress = {
                                // Edit/Wiggle nur in MANUAL, sonst verwirrend
                                if (sortMode == SortMode.MANUAL && !dragDropState.isDragging) {
                                    isInEditMode = !isInEditMode
                                }
                            },
                            onDeleteClick = {
                                notesViewModel.removeNote(note)
                                if (filteredSortedNotes.size == 1 && isInEditMode) {
                                    isInEditMode = false
                                }
                            },
                            onDragStartFromHandle = {
                                if (sortMode == SortMode.MANUAL && isInEditMode && indexInDisplayed != -1) {
                                    dragDropState.isDragging = true
                                    dragDropState.draggedItemIndex = indexInDisplayed
                                    dragDropState.initialDraggedItemIndex = indexInDisplayed
                                    dragDropState.dragOffset = 0f
                                }
                            },
                            onDragFromHandle = { change, dragAmount ->
                                if (sortMode == SortMode.MANUAL && dragDropState.isDragging && isInEditMode) {
                                    change.consumeAllChanges()
                                    dragDropState.dragOffset += dragAmount.y

                                    val currentIndex = dragDropState.draggedItemIndex ?: return@NoteItem
                                    if (currentIndex !in displayedNotes.indices) return@NoteItem

                                    val layoutInfo = lazyListState.layoutInfo

                                    // ⚠️ Wir ziehen in der aktuell angezeigten LazyColumn (filteredSortedNotes)
                                    // deswegen müssen wir den sichtbaren Index über note.id finden
                                    val draggedVisibleIndex = layoutInfo.visibleItemsInfo.firstOrNull {
                                        val n = filteredSortedNotes.getOrNull(it.index)
                                        n?.id == displayedNotes[currentIndex].id
                                    } ?: return@NoteItem

                                    val draggedCenterY =
                                        draggedVisibleIndex.offset.toFloat() +
                                                draggedVisibleIndex.size.toFloat() / 2f +
                                                dragDropState.dragOffset

                                    // Zielindex ermitteln innerhalb der sichtbaren Liste
                                    var newFilteredIndex = indexInFilteredList
                                    layoutInfo.visibleItemsInfo
                                        .filter { it.index != indexInFilteredList }
                                        .forEach { other ->
                                            val otherCenterY = other.offset.toFloat() + other.size.toFloat() / 2f
                                            val threshold = other.size.toFloat() * 0.30f

                                            if (dragAmount.y < 0) {
                                                if (draggedCenterY < otherCenterY + threshold && other.index < newFilteredIndex) {
                                                    newFilteredIndex = other.index
                                                }
                                            } else {
                                                if (draggedCenterY > otherCenterY - threshold && other.index > newFilteredIndex) {
                                                    newFilteredIndex = other.index
                                                }
                                            }
                                        }

                                    val targetNote = filteredSortedNotes.getOrNull(newFilteredIndex)
                                    if (targetNote != null) {
                                        val newIndexInDisplayed = displayedNotes.indexOfFirst { it.id == targetNote.id }
                                        if (newIndexInDisplayed != -1 && newIndexInDisplayed != currentIndex) {
                                            val temp = displayedNotes.toMutableList()
                                            val moved = temp.removeAt(currentIndex)
                                            temp.add(newIndexInDisplayed, moved)
                                            displayedNotes = temp
                                            dragDropState.draggedItemIndex = newIndexInDisplayed
                                        }
                                    }

                                    // Overscroll
                                    val viewportHeight = layoutInfo.viewportSize.height
                                    val draggedVisualY =
                                        draggedVisibleIndex.offset.toFloat() + dragDropState.dragOffset

                                    if (draggedVisualY < layoutInfo.viewportStartOffset + viewportHeight * 0.20f &&
                                        lazyListState.canScrollBackward
                                    ) {
                                        overscrollJob?.cancel()
                                        overscrollJob = coroutineScope.launch { lazyListState.scrollBy(-35f) }
                                    } else if (draggedVisualY + draggedVisibleIndex.size >
                                        layoutInfo.viewportEndOffset - viewportHeight * 0.20f &&
                                        lazyListState.canScrollForward
                                    ) {
                                        overscrollJob?.cancel()
                                        overscrollJob = coroutineScope.launch { lazyListState.scrollBy(35f) }
                                    } else {
                                        overscrollJob?.cancel()
                                    }
                                }
                            },
                            onDragEndFromHandle = {
                                if (sortMode == SortMode.MANUAL && dragDropState.isDragging && isInEditMode) {
                                    overscrollJob?.cancel()

                                    val from = dragDropState.initialDraggedItemIndex
                                    val to = dragDropState.draggedItemIndex

                                    if (from != null && to != null && from != to &&
                                        from in notes.indices && to in notes.indices
                                    ) {
                                        notesViewModel.moveNote(from, to) // persist displayOrder
                                    }
                                }

                                dragDropState.isDragging = false
                                dragDropState.dragOffset = 0f
                                dragDropState.draggedItemIndex = null
                                dragDropState.initialDraggedItemIndex = null
                            },
                            onDragCancelFromHandle = {
                                overscrollJob?.cancel()
                                dragDropState.isDragging = false
                                dragDropState.dragOffset = 0f
                                dragDropState.draggedItemIndex = null
                                dragDropState.initialDraggedItemIndex = null
                                displayedNotes = notes
                            },
                            modifier = Modifier
                                .animateItemPlacement(
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioMediumBouncy,
                                        stiffness = Spring.StiffnessLow
                                    )
                                )
                                .graphicsLayer {
                                    if (isDragged) {
                                        translationY = dragDropState.dragOffset
                                        scaleX = 1.02f
                                        scaleY = 1.02f
                                        alpha = 0.98f
                                    }
                                }
                                .zIndex(if (isDragged) 10f else 0f)
                                .shadow(if (isDragged) 10.dp else 2.dp, RoundedCornerShape(12.dp))
                        )
                    }
                }
            }
        }
    }

    // --- Add Dialog ---
    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
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
                            .heightIn(min = 120.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val title = newNoteTitle.trim()
                        val content = newNoteContent.trim()
                        if (title.isNotBlank() || content.isNotBlank()) {
                            notesViewModel.addNote(title, content)
                        }
                        newNoteTitle = ""
                        newNoteContent = ""
                        showAddDialog = false
                    }
                ) { Text("Hinzufügen") }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) { Text("Abbrechen") }
            }
        )
    }

    // --- Edit Dialog ---
    if (editNote != null) {
        AlertDialog(
            onDismissRequest = { editNote = null },
            title = { Text("Notiz bearbeiten") },
            text = {
                Column {
                    OutlinedTextField(
                        value = editTitle,
                        onValueChange = { editTitle = it },
                        label = { Text("Titel") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = editContent,
                        onValueChange = { editContent = it },
                        label = { Text("Inhalt") },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 140.dp)
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    val n = editNote!!
                    val updated = n.copy(
                        title = editTitle.trim(),
                        content = editContent.trim(),
                        timestamp = System.currentTimeMillis()
                    )
                    notesViewModel.updateNote(updated)
                    editNote = null
                }) { Text("Speichern") }
            },
            dismissButton = {
                TextButton(onClick = { editNote = null }) { Text("Abbrechen") }
            }
        )
    }
}

// --- 8. SEARCH + SORT BAR ---
@Composable
fun SearchAndSortBar(
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    sortMode: SortMode,
    onSortChange: (SortMode) -> Unit,
    isManual: Boolean,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchChange,
            singleLine = true,
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = "Suchen") },
            trailingIcon = {
                if (searchQuery.isNotBlank()) {
                    IconButton(onClick = { onSearchChange("") }) {
                        Icon(Icons.Filled.Close, contentDescription = "Clear")
                    }
                }
            },
            placeholder = { Text("Suchen (Titel oder Inhalt)…") },
            modifier = Modifier.fillMaxWidth()
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "Sortieren:",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            var expanded by remember { mutableStateOf(false) }

            Box {
                AssistChip(
                    onClick = { expanded = true },
                    label = { Text(sortMode.label) }
                )

                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    SortMode.entries.forEach { mode ->
                        DropdownMenuItem(
                            text = { Text(mode.label) },
                            onClick = {
                                onSortChange(mode)
                                expanded = false
                            }
                        )
                    }
                }
            }

            if (!isManual) {
                Text(
                    text = "Drag nur bei „Manuell“",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
                )
            }
        }
    }
}

// --- 9. ADS & PREMIUM UI ---
@Composable
fun AdBanner() {
    val adUnitId =
        if (BuildConfig.DEBUG) "ca-app-pub-3940256099942544/6300978111"
        else "ca-app-pub-6130152704553606/6365675325"

    AndroidView(
        modifier = Modifier.fillMaxWidth(),
        factory = { context ->
            AdView(context).apply {
                setAdSize(AdSize.BANNER)
                this.adUnitId = adUnitId
                loadAd(AdRequest.Builder().build())
            }
        }
    )
}

@Composable
fun PremiumBanner(modifier: Modifier = Modifier, onClose: () -> Unit) {
    Box(
        modifier = modifier
            .fillMaxWidth(0.9f)
            .clip(CircleShape)
            .background(Color(0xFF0C0F1C))
            .border(width = 1.dp, color = Color(0x66FDEE51), shape = CircleShape)
            .padding(horizontal = 14.dp, vertical = 8.dp)
            .zIndex(10f)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .size(18.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFFDEE51)),
                contentAlignment = Alignment.Center
            ) {
                Text("★", fontSize = 12.sp, color = Color.Black)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(text = "Premium aktiviert", color = Color.White, fontSize = 13.sp)
                Text(text = "Werbefrei · Voller Fokus", color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp)
            }
            Icon(
                imageVector = Icons.Filled.Close,
                contentDescription = "Schließen",
                tint = Color.White.copy(alpha = 0.7f),
                modifier = Modifier
                    .size(18.dp)
                    .clickable(onClick = onClose)
            )
        }
    }
}

@Composable
fun SettingsScreen(
    isImmersive: Boolean,
    onToggleImmersive: () -> Unit,
    showPremiumBanner: Boolean,
    onChangeShowPremiumBanner: (Boolean) -> Unit,
    showPremiumThanks: Boolean,
    onChangeShowPremiumThanks: (Boolean) -> Unit,
    onClose: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xAA000000))
            .zIndex(20f),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xFF101018))
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = "Einstellungen", color = Color.White, fontSize = 18.sp)
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = "Schließen",
                    tint = Color.White.copy(alpha = 0.7f),
                    modifier = Modifier
                        .size(20.dp)
                        .clickable(onClick = onClose)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            SettingsToggleRow("Vollbildmodus", "Statusleiste ausblenden", isImmersive) { onToggleImmersive() }
            SettingsToggleRow("Premium-Banner anzeigen", "Goldenes Banner oben", showPremiumBanner, onChangeShowPremiumBanner)
            SettingsToggleRow("Premium-Hinweis unten", "Werbefrei-Leiste unten", showPremiumThanks, onChangeShowPremiumThanks)
        }
    }
}

@Composable
fun SettingsToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = Color.White, fontSize = 14.sp)
            Text(subtitle, color = Color.White.copy(alpha = 0.6f), fontSize = 11.sp)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

// --- 10. BILLING CLIENT ---
class BillingClientWrapper(private val activity: Activity) {
    private val purchasesUpdatedListener =
        PurchasesUpdatedListener { billingResult, purchases ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
                for (purchase in purchases) {
                    if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED && !purchase.isAcknowledged) {
                        val params = AcknowledgePurchaseParams
                            .newBuilder()
                            .setPurchaseToken(purchase.purchaseToken)
                            .build()
                        billingClient.acknowledgePurchase(params) {
                            if (it.responseCode == BillingClient.BillingResponseCode.OK) {
                                PremiumManager.setPremium(true)
                            }
                        }
                    }
                }
            }
        }

    private var billingClient: BillingClient =
        BillingClient.newBuilder(activity)
            .setListener(purchasesUpdatedListener)
            .enablePendingPurchases()
            .build()

    init { connectToGooglePlay() }

    private fun connectToGooglePlay() {
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) queryPurchases()
            }
            override fun onBillingServiceDisconnected() { connectToGooglePlay() }
        })
    }

    private fun queryPurchases() {
        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.INAPP)
            .build()
        billingClient.queryPurchasesAsync(params) { _, purchases ->
            if (purchases.any { it.products.contains("remove_ads") }) {
                PremiumManager.setPremium(true)
            }
        }
    }

    fun launchPurchaseFlow() {
        val productList = listOf(
            QueryProductDetailsParams.Product
                .newBuilder()
                .setProductId("remove_ads")
                .setProductType(BillingClient.ProductType.INAPP)
                .build()
        )
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(productList)
            .build()
        billingClient.queryProductDetailsAsync(params) { billingResult, productDetailsList ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK &&
                productDetailsList.isNotEmpty()
            ) {
                val flowParams = BillingFlowParams.newBuilder()
                    .setProductDetailsParamsList(
                        listOf(
                            BillingFlowParams.ProductDetailsParams
                                .newBuilder()
                                .setProductDetails(productDetailsList[0])
                                .build()
                        )
                    )
                    .build()
                billingClient.launchBillingFlow(activity, flowParams)
            }
        }
    }
}

// --- 11. UI HELPERS ---
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
        this.graphicsLayer { rotationZ = angle }
    } else {
        this.graphicsLayer { rotationZ = 0f }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NoteItem(
    note: Note,
    isBeingDragged: Boolean,
    isInEditMode: Boolean,
    onClick: () -> Unit,
    onLongPress: () -> Unit,
    onDeleteClick: () -> Unit,
    onDragStartFromHandle: () -> Unit,
    onDragFromHandle: (change: PointerInputChange, dragAmount: Offset) -> Unit,
    onDragEndFromHandle: () -> Unit,
    onDragCancelFromHandle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val elevation by animateDpAsState(
        targetValue = if (isBeingDragged) 10.dp else 2.dp,
        label = "elevation"
    )
    val cardColor by animateColorAsState(
        targetValue = if (isBeingDragged)
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.95f)
        else
            MaterialTheme.colorScheme.surfaceVariant,
        label = "color"
    )

    Card(
        modifier = modifier
            .jiggleEffect(enabled = isInEditMode && !isBeingDragged)
            .combinedClickable(
                onClick = { if (!isInEditMode && !isBeingDragged) onClick() },
                onLongClick = { if (!isBeingDragged) onLongPress() }
            )
            .fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = elevation),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isInEditMode) {
                Icon(
                    imageVector = Icons.Filled.DragHandle,
                    contentDescription = "Verschieben",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                    modifier = Modifier
                        .size(40.dp)
                        .padding(end = 10.dp)
                        .pointerInput(note.id) {
                            detectDragGesturesAfterLongPress(
                                onDragStart = { onDragStartFromHandle() },
                                onDrag = { change, dragAmount -> onDragFromHandle(change, dragAmount) },
                                onDragEnd = { onDragEndFromHandle() },
                                onDragCancel = { onDragCancelFromHandle() }
                            )
                        }
                )
            } else {
                Spacer(modifier = Modifier.width(4.dp))
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = note.title.ifBlank { "Unbenannte Notiz" },
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (note.content.isNotBlank()) {
                    Text(
                        text = note.content,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(
                    text = formatTimestamp(note.timestamp),
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f)
                )
            }

            if (isInEditMode) {
                IconButton(onClick = onDeleteClick) {
                    Icon(
                        imageVector = Icons.Filled.Delete,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.9f)
                    )
                }
            }
        }
    }
}

// --- 12. PREVIEW ---
@Preview(showBackground = true, name = "NoteAppScreen - Light")
@Composable
fun NoteAppScreenPreviewLight() {
    val dummyNotesViewModel = NotesViewModel(Application())
    val dummyThemeViewModel = ThemeViewModel(Application())

    MyApplicationTheme(useDarkTheme = false) {
        NoteAppScreen(
            notesViewModel = dummyNotesViewModel,
            themeViewModel = dummyThemeViewModel,
            isDarkTheme = false,
            onOpenSettings = {},
            onRemoveAds = {},
            isPremium = false
        )
    }
}
