package dev.flondy.mynotes.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.NoteAdd
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.WorkspacePremium
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.flondy.mynotes.Category
import dev.flondy.mynotes.HomeUiState
import dev.flondy.mynotes.Note
import dev.flondy.mynotes.NotesViewModel
import dev.flondy.mynotes.R
import dev.flondy.mynotes.ads.AdBanner
import dev.flondy.mynotes.billing.BillingManager
import dev.flondy.mynotes.data.DateFilter
import dev.flondy.mynotes.data.NoteQuery
import dev.flondy.mynotes.data.QuickFilter
import dev.flondy.mynotes.settings.AppSettings
import dev.flondy.mynotes.settings.SortMode
import dev.flondy.mynotes.ui.components.AppFilterChip
import dev.flondy.mynotes.ui.components.EmptyState
import dev.flondy.mynotes.ui.components.NoteActionsSheet
import dev.flondy.mynotes.ui.components.NoteCard
import dev.flondy.mynotes.ui.components.NoteDragState
import dev.flondy.mynotes.ui.components.ReorderableNoteList
import dev.flondy.mynotes.ui.components.SheetLabel
import dev.flondy.mynotes.ui.theme.LocalAppExtras
import dev.flondy.mynotes.ui.theme.Motion

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HomeScreen(
    vm: NotesViewModel,
    settings: AppSettings,
    premium: BillingManager.State,
    adsReady: Boolean,
    isDark: Boolean,
    snackbarHostState: SnackbarHostState,
    onOpenSettings: () -> Unit,
    onOpenPremium: () -> Unit,
    onToggleTheme: () -> Unit,
    onHideAdFreeBadge: () -> Unit,
    onShare: (Note) -> Unit,
    onExportTxt: (Note) -> Unit,
) {
    val state by vm.homeState.collectAsStateWithLifecycle()
    val query by vm.query.collectAsStateWithLifecycle()
    val categories by vm.categories.collectAsStateWithLifecycle()
    val extras = LocalAppExtras.current
    val listState = rememberLazyListState()
    val dragState = remember { NoteDragState() }
    var menuNoteId by rememberSaveable { mutableStateOf<Int?>(null) }
    var showFilterSheet by rememberSaveable { mutableStateOf(false) }
    val fabExpanded by remember { derivedStateOf { listState.firstVisibleItemIndex == 0 } }
    val imeVisible = WindowInsets.isImeVisible
    val categoryNames = remember(categories) { categories.associate { it.id to it.name } }

    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        // Dezenter Farbverlauf hinter der Kopfzeile
        Box(
            Modifier
                .fillMaxWidth()
                .height(280.dp)
                .background(Brush.verticalGradient(listOf(extras.gradientTop, Color.Transparent)))
        )
        Scaffold(
            containerColor = Color.Transparent,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                HomeHeader(
                    isDark = isDark,
                    showPremium = premium.initialized && !premium.adFree,
                    onOpenPremium = onOpenPremium,
                    onToggleTheme = onToggleTheme,
                    onOpenSettings = onOpenSettings,
                )
            },
            floatingActionButton = {
                if (!imeVisible) {
                    HomeFabs(
                        expanded = fabExpanded,
                        onNewNote = { vm.openNewNote(Note.TYPE_TEXT) },
                        onNewChecklist = { vm.openNewNote(Note.TYPE_CHECKLIST) },
                    )
                }
            },
            bottomBar = {
                Column(Modifier.navigationBarsPadding()) {
                    val showAds = premium.initialized && !premium.adFree && adsReady && !imeVisible
                    if (showAds) AdBanner()
                    AnimatedVisibility(premium.adFree && settings.showAdFreeBadge && !imeVisible) {
                        AdFreeBadge(onClose = onHideAdFreeBadge)
                    }
                }
            },
        ) { padding ->
            Column(
                Modifier
                    .padding(top = padding.calculateTopPadding())
                    .fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Column(Modifier.widthIn(max = 760.dp).fillMaxSize()) {
                    SearchField(query.search, vm::setSearch)
                    FilterRow(
                        query = query,
                        categories = categories,
                        sortMode = settings.sortMode,
                        favoritesFirst = settings.favoritesFirst,
                        onQuickFilter = vm::setQuickFilter,
                        onCategory = vm::setCategoryFilter,
                        onOpenSheet = { showFilterSheet = true },
                    )
                    HomeContent(
                        state = state,
                        query = query,
                        settings = settings,
                        categoryNames = categoryNames,
                        listState = listState,
                        dragState = dragState,
                        bottomPadding = padding.calculateBottomPadding(),
                        vm = vm,
                        onMenu = { menuNoteId = it.id },
                    )
                }
            }
        }
    }

    val menuNote = menuNoteId?.let { id -> state.notes.firstOrNull { it.id == id } }
    if (menuNote != null) {
        NoteActionsSheet(
            note = menuNote,
            categories = categories,
            canMove = state.reorderAllowed,
            onDismiss = { menuNoteId = null },
            onTogglePin = { vm.togglePinned(menuNote) },
            onToggleFavorite = { vm.toggleFavorite(menuNote) },
            onColor = { vm.setColor(menuNote, it) },
            onCategory = { vm.setCategory(menuNote, it) },
            onShare = { onShare(menuNote) },
            onExportTxt = { onExportTxt(menuNote) },
            onMoveUp = { vm.moveByOne(menuNote.id, up = true) },
            onMoveDown = { vm.moveByOne(menuNote.id, up = false) },
            onTrash = { vm.moveToTrash(menuNote.id) },
        )
    }
    if (showFilterSheet) {
        FilterSortSheet(
            query = query,
            settings = settings,
            onDismiss = { showFilterSheet = false },
            onSort = vm::setSortMode,
            onFavoritesFirst = vm::setFavoritesFirst,
            onDateFilter = vm::setDateFilter,
            onOnlyWithContent = vm::setOnlyWithContent,
            onReset = { vm.resetFilters(); vm.setSortMode(SortMode.MANUAL); vm.setFavoritesFirst(false) },
        )
    }
}

@Composable
private fun HomeHeader(
    isDark: Boolean,
    showPremium: Boolean,
    onOpenPremium: () -> Unit,
    onToggleTheme: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(start = 20.dp, end = 8.dp, top = 12.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            stringResource(R.string.home_title),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.weight(1f),
        )
        if (showPremium) {
            IconButton(onClick = onOpenPremium) {
                Icon(Icons.Outlined.WorkspacePremium, stringResource(R.string.premium_open), tint = Color(0xFFE0B800))
            }
        }
        IconButton(onClick = onToggleTheme) {
            Crossfade(isDark, animationSpec = Motion.tween(300), label = "themeIcon") { dark ->
                Icon(
                    if (dark) Icons.Outlined.LightMode else Icons.Outlined.DarkMode,
                    stringResource(if (dark) R.string.a11y_theme_light else R.string.a11y_theme_dark),
                )
            }
        }
        IconButton(onClick = onOpenSettings) {
            Icon(Icons.Outlined.Settings, stringResource(R.string.settings_title))
        }
    }
}

@Composable
private fun SearchField(value: String, onChange: (String) -> Unit) {
    val focus = LocalFocusManager.current
    TextField(
        value = value,
        onValueChange = onChange,
        singleLine = true,
        placeholder = { Text(stringResource(R.string.search_hint)) },
        leadingIcon = { Icon(Icons.Filled.Search, null) },
        trailingIcon = {
            AnimatedVisibility(value.isNotEmpty(), enter = fadeIn() + scaleIn(), exit = fadeOut() + scaleOut()) {
                IconButton(onClick = { onChange(""); focus.clearFocus() }) {
                    Icon(Icons.Filled.Close, stringResource(R.string.search_clear))
                }
            }
        },
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { focus.clearFocus() }),
        shape = CircleShape,
        colors = TextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            cursorColor = MaterialTheme.colorScheme.onSurface,
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .testTag("search_field"),
    )
}

@Composable
private fun FilterRow(
    query: NoteQuery,
    categories: List<Category>,
    sortMode: SortMode,
    favoritesFirst: Boolean,
    onQuickFilter: (QuickFilter) -> Unit,
    onCategory: (Int?) -> Unit,
    onOpenSheet: () -> Unit,
) {
    val advancedActive = query.dateFilter != DateFilter.ANY || query.onlyWithContent || sortMode != SortMode.MANUAL || favoritesFirst
    Row(verticalAlignment = Alignment.CenterVertically) {
        Row(
            Modifier
                .weight(1f)
                .horizontalScroll(rememberScrollState())
                .padding(start = 16.dp, end = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            val noCategory = query.categoryId == null
            AppFilterChip(noCategory && query.quickFilter == QuickFilter.ALL, stringResource(R.string.filter_all)) { onQuickFilter(QuickFilter.ALL) }
            AppFilterChip(noCategory && query.quickFilter == QuickFilter.FAVORITES, stringResource(R.string.filter_favorites), Icons.Filled.Star) { onQuickFilter(QuickFilter.FAVORITES) }
            AppFilterChip(noCategory && query.quickFilter == QuickFilter.PINNED, stringResource(R.string.filter_pinned), Icons.Filled.PushPin) { onQuickFilter(QuickFilter.PINNED) }
            AppFilterChip(noCategory && query.quickFilter == QuickFilter.CHECKLISTS, stringResource(R.string.filter_checklists), Icons.Outlined.Checklist) { onQuickFilter(QuickFilter.CHECKLISTS) }
            categories.forEach { c ->
                AppFilterChip(query.categoryId == c.id, c.name) { onCategory(if (query.categoryId == c.id) null else c.id) }
            }
        }
        IconButton(onClick = onOpenSheet, modifier = Modifier.padding(end = 8.dp)) {
            BadgedBox(badge = { if (advancedActive) Badge(containerColor = LocalAppExtras.current.accent) }) {
                Icon(Icons.Outlined.Tune, stringResource(R.string.filter_sort_title))
            }
        }
    }
}

@Composable
private fun HomeContent(
    state: HomeUiState,
    query: NoteQuery,
    settings: AppSettings,
    categoryNames: Map<Int, String>,
    listState: androidx.compose.foundation.lazy.LazyListState,
    dragState: NoteDragState,
    bottomPadding: androidx.compose.ui.unit.Dp,
    vm: NotesViewModel,
    onMenu: (Note) -> Unit,
) {
    if (!state.loaded) return
    if (state.notes.isEmpty()) {
        Box(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), contentAlignment = Alignment.TopCenter) {
            when {
                state.totalActive == 0 -> EmptyState(Icons.AutoMirrored.Outlined.NoteAdd, stringResource(R.string.empty_title), stringResource(R.string.empty_subtitle), Modifier.padding(top = 48.dp))
                query.search.isNotBlank() -> EmptyState(Icons.Outlined.SearchOff, stringResource(R.string.empty_search_title, query.search.trim()), stringResource(R.string.empty_search_subtitle), Modifier.padding(top = 48.dp))
                else -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    EmptyState(Icons.Outlined.Tune, stringResource(R.string.empty_filter_title), stringResource(R.string.empty_filter_subtitle), Modifier.padding(top = 48.dp))
                    TextButton(onClick = { vm.resetFilters() }) { Text(stringResource(R.string.filter_reset)) }
                }
            }
        }
        return
    }

    val editLabel = stringResource(R.string.action_edit)
    val upLabel = stringResource(R.string.action_move_up)
    val downLabel = stringResource(R.string.action_move_down)
    ReorderableNoteList(
        notes = state.notes,
        reorderEnabled = state.reorderAllowed,
        listState = listState,
        dragState = dragState,
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = bottomPadding + 120.dp),
        onReorder = vm::reorder,
        onLongPressWithoutMove = onMenu,
        modifier = Modifier.fillMaxSize(),
    ) { note, isPlaceholder, isOverlay ->
        val actions = buildList {
            add(CustomAccessibilityAction(editLabel) { vm.openEditor(note); true })
            if (state.reorderAllowed) {
                add(CustomAccessibilityAction(upLabel) { vm.moveByOne(note.id, true); true })
                add(CustomAccessibilityAction(downLabel) { vm.moveByOne(note.id, false); true })
            }
        }
        NoteCard(
            note = note,
            categoryName = note.categoryId?.let(categoryNames::get),
            density = settings.cardDensity,
            searchQuery = query.search,
            hideCheckedItems = settings.hideCheckedItems,
            isPlaceholder = isPlaceholder,
            isOverlay = isOverlay,
            onClick = {
                // Loslassen nach einem Drag nicht als Klick werten
                if (!dragState.isDragging && System.currentTimeMillis() - dragState.lastDragEndMillis > 300) vm.openEditor(note)
            },
            onLongClick = if (state.reorderAllowed) null else ({ onMenu(note) }),
            onMenu = { onMenu(note) },
            accessibilityActions = actions,
        )
    }
}

@Composable
private fun HomeFabs(expanded: Boolean, onNewNote: () -> Unit, onNewChecklist: () -> Unit) {
    val extras = LocalAppExtras.current
    Column(horizontalAlignment = Alignment.End) {
        AnimatedVisibility(
            visible = expanded,
            enter = fadeIn(Motion.tween()) + slideInVertically(Motion.spring()) { it / 2 },
            exit = fadeOut(Motion.tween()) + slideOutVertically(Motion.tween()) { it / 2 },
        ) {
            SmallFloatingActionButton(
                onClick = onNewChecklist,
                containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                contentColor = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 12.dp, end = 4.dp),
            ) {
                Icon(Icons.Outlined.Checklist, stringResource(R.string.new_checklist))
            }
        }
        ExtendedFloatingActionButton(
            onClick = onNewNote,
            expanded = expanded,
            icon = { Icon(Icons.Outlined.Edit, null) },
            text = { Text(stringResource(R.string.new_note)) },
            containerColor = extras.accent,
            contentColor = extras.onAccent,
            modifier = Modifier.testTag("fab_new_note"),
        )
    }
}

@Composable
private fun AdFreeBadge(onClose: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(start = 20.dp, end = 8.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(18.dp).clip(CircleShape).background(LocalAppExtras.current.premiumGold), contentAlignment = Alignment.Center) {
            Text("★", style = MaterialTheme.typography.labelSmall, color = Color(0xFF1A1806))
        }
        Spacer(Modifier.width(10.dp))
        Text(
            stringResource(R.string.ad_free_thanks),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        IconButton(onClick = onClose) { Icon(Icons.Filled.Close, stringResource(R.string.action_close), Modifier.size(18.dp)) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FilterSortSheet(
    query: NoteQuery,
    settings: AppSettings,
    onDismiss: () -> Unit,
    onSort: (SortMode) -> Unit,
    onFavoritesFirst: (Boolean) -> Unit,
    onDateFilter: (DateFilter) -> Unit,
    onOnlyWithContent: (Boolean) -> Unit,
    onReset: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Column(Modifier.verticalScroll(rememberScrollState()).navigationBarsPadding()) {
            Row(Modifier.padding(horizontal = 24.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.filter_sort_title), style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                TextButton(onClick = onReset) { Text(stringResource(R.string.filter_reset)) }
            }
            SheetLabel(stringResource(R.string.sort_title))
            SortMode.entries.forEach { mode ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable(role = Role.RadioButton) { onSort(mode) }
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(selected = settings.sortMode == mode, onClick = { onSort(mode) })
                    Text(stringResource(sortLabel(mode)), style = MaterialTheme.typography.bodyLarge)
                }
            }
            SwitchRow(stringResource(R.string.sort_favorites_first), settings.favoritesFirst, onFavoritesFirst)
            if (settings.sortMode != SortMode.MANUAL || settings.favoritesFirst) {
                Text(
                    stringResource(R.string.sort_manual_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp),
                )
            }
            SheetLabel(stringResource(R.string.filter_period))
            Row(Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DateFilter.entries.forEach { f ->
                    AppFilterChip(query.dateFilter == f, stringResource(dateLabel(f))) { onDateFilter(f) }
                }
            }
            SwitchRow(stringResource(R.string.filter_only_content), query.onlyWithContent, onOnlyWithContent)
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SwitchRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(role = Role.Switch) { onChange(!checked) }
            .padding(horizontal = 24.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = null)
    }
}

fun sortLabel(mode: SortMode): Int = when (mode) {
    SortMode.MANUAL -> R.string.sort_manual
    SortMode.UPDATED -> R.string.sort_updated
    SortMode.NEWEST -> R.string.sort_newest
    SortMode.OLDEST -> R.string.sort_oldest
    SortMode.TITLE_AZ -> R.string.sort_title_az
    SortMode.TITLE_ZA -> R.string.sort_title_za
}

private fun dateLabel(f: DateFilter): Int = when (f) {
    DateFilter.ANY -> R.string.filter_any_time
    DateFilter.TODAY -> R.string.filter_today
    DateFilter.LAST_7_DAYS -> R.string.filter_7_days
    DateFilter.LAST_30_DAYS -> R.string.filter_30_days
}
