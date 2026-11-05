package dev.flondy.mynotes.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.automirrored.outlined.NoteAdd
import androidx.compose.material.icons.automirrored.outlined.Notes
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.ViewAgenda
import androidx.compose.material.icons.outlined.WorkspacePremium
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.flondy.mynotes.Category
import dev.flondy.mynotes.HomeTab
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
import dev.flondy.mynotes.settings.HomeLayout
import dev.flondy.mynotes.settings.SortMode
import dev.flondy.mynotes.ui.components.AppFilterChip
import dev.flondy.mynotes.ui.components.AppLogo
import dev.flondy.mynotes.ui.components.CardLayout
import dev.flondy.mynotes.ui.components.CategoryIcons
import dev.flondy.mynotes.ui.components.EmptyState
import dev.flondy.mynotes.ui.components.IconTile
import dev.flondy.mynotes.ui.components.NoteActionsSheet
import dev.flondy.mynotes.ui.components.NoteCard
import dev.flondy.mynotes.ui.components.NoteDragState
import dev.flondy.mynotes.ui.components.ReorderableNoteList
import dev.flondy.mynotes.ui.components.SheetLabel
import dev.flondy.mynotes.ui.components.leather
import dev.flondy.mynotes.ui.components.WindowLayout
import dev.flondy.mynotes.ui.components.rememberWindowLayout
import dev.flondy.mynotes.ui.theme.LocalAppExtras
import dev.flondy.mynotes.ui.theme.Motion
import kotlinx.coroutines.launch

data class HomeActions(
    val onOpenSettings: () -> Unit,
    val onOpenPremium: () -> Unit,
    val onOpenTrash: () -> Unit,
    val onOpenCategories: () -> Unit,
    val onToggleTheme: () -> Unit,
    val onHideAdFreeBadge: () -> Unit,
    val onShare: (Note) -> Unit,
    val onExportTxt: (Note) -> Unit,
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HomeScreen(
    vm: NotesViewModel,
    settings: AppSettings,
    premium: BillingManager.State,
    adsReady: Boolean,
    isDark: Boolean,
    snackbarHostState: SnackbarHostState,
    actions: HomeActions,
) {
    val state by vm.homeState.collectAsStateWithLifecycle()
    val query by vm.query.collectAsStateWithLifecycle()
    val categories by vm.categories.collectAsStateWithLifecycle()
    val extras = LocalAppExtras.current
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var menuNoteId by rememberSaveable { mutableStateOf<Int?>(null) }
    var showFilterSheet by rememberSaveable { mutableStateOf(false) }
    var searchOpen by rememberSaveable { mutableStateOf(query.search.isNotEmpty()) }
    val imeVisible = WindowInsets.isImeVisible
    val categoryById = remember(categories) { categories.associateBy { it.id } }
    val window = rememberWindowLayout()
    val showFab = !imeVisible && state.tab != HomeTab.ARCHIVE
    val onNewNote = { vm.openNewNote(if (query.quickFilter == QuickFilter.CHECKLISTS) Note.TYPE_CHECKLIST else Note.TYPE_TEXT) }

    fun closeDrawerThen(action: () -> Unit) {
        scope.launch { drawerState.close() }
        action()
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            AppDrawer(
                tab = state.tab,
                showPremium = premium.initialized && !premium.adFree,
                onTab = { t -> closeDrawerThen { vm.setTab(t); if (t == HomeTab.NOTES) vm.resetFilters() } },
                onTrash = { closeDrawerThen(actions.onOpenTrash) },
                onCategories = { closeDrawerThen(actions.onOpenCategories) },
                onSettings = { closeDrawerThen(actions.onOpenSettings) },
                onPremium = { closeDrawerThen(actions.onOpenPremium) },
            )
        },
    ) {
        Box(Modifier.fillMaxSize().leather(stitch = false)) {
            Scaffold(
                containerColor = Color.Transparent,
                snackbarHost = { SnackbarHost(snackbarHostState) },
                topBar = {
                    HomeHeader(
                        compact = window.compactHeader,
                        isDark = isDark,
                        layout = settings.homeLayout,
                        onMenu = { scope.launch { drawerState.open() } },
                        onSearch = { searchOpen = !searchOpen; if (!searchOpen) vm.setSearch("") },
                        onLayout = vm::setHomeLayout,
                        onFilter = { showFilterSheet = true },
                        onToggleTheme = actions.onToggleTheme,
                        onSettings = actions.onOpenSettings,
                    )
                },
                floatingActionButton = {
                    // Mit seitlicher Navigation sitzt der Button oben in der Leiste (gut mit dem Daumen erreichbar im Querformat)
                    if (showFab && !window.useNavigationRail) {
                        FloatingActionButton(
                            onClick = onNewNote,
                            shape = CircleShape,
                            containerColor = extras.accent,
                            contentColor = extras.onAccent,
                            modifier = Modifier.size(64.dp).testTag("fab_new_note"),
                        ) {
                            Icon(Icons.Filled.Add, stringResource(R.string.new_note), Modifier.size(32.dp))
                        }
                    }
                },
                bottomBar = {
                    Column(Modifier.background(MaterialTheme.colorScheme.background)) {
                        val showAds = premium.initialized && !premium.adFree && adsReady && !imeVisible
                        if (showAds) AdBanner()
                        AnimatedVisibility(premium.adFree && settings.showAdFreeBadge && !imeVisible) {
                            AdFreeBadge(onClose = actions.onHideAdFreeBadge)
                        }
                        if (!imeVisible && !window.useNavigationRail) HomeBottomBar(state.tab, state.archivedCount, vm::setTab)
                        else Spacer(Modifier.navigationBarsPadding())
                    }
                },
            ) { padding ->
                Row(Modifier.padding(top = padding.calculateTopPadding()).fillMaxSize()) {
                if (window.useNavigationRail) {
                    HomeRail(
                        tab = state.tab,
                        showFab = showFab,
                        short = window.isShort,
                        onNewNote = onNewNote,
                        onTab = vm::setTab,
                        modifier = Modifier.padding(bottom = padding.calculateBottomPadding()),
                    )
                }
                Column(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                        .background(MaterialTheme.colorScheme.background),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    androidx.compose.runtime.CompositionLocalProvider(androidx.compose.material3.LocalContentColor provides MaterialTheme.colorScheme.onBackground) {
                    Column(Modifier.widthIn(max = window.contentMaxWidth).fillMaxSize()) {
                        AnimatedVisibility(searchOpen, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
                            SearchField(query.search, vm::setSearch, onClose = { searchOpen = false; vm.setSearch("") })
                        }
                        if (state.tab == HomeTab.NOTES) {
                            FilterChips(query, categories, onQuickFilter = vm::setQuickFilter, onCategory = vm::setCategoryFilter,
                                modifier = Modifier.padding(top = if (searchOpen) 0.dp else 16.dp))
                        }
                        ResultsBar(state, query, settings, onSort = vm::setSortMode, onFavoritesFirst = vm::setFavoritesFirst,
                            onMoreFilters = { showFilterSheet = true })
                        HomeContent(
                            state = state,
                            query = query,
                            settings = settings,
                            categories = categories,
                            categoryById = categoryById,
                            bottomPadding = padding.calculateBottomPadding(),
                            window = window,
                            vm = vm,
                            onMenu = { menuNoteId = it.id },
                            onOpenCategories = actions.onOpenCategories,
                        )
                    }
                    }
                }
                }
            }
        }
    }

    val menuNote = menuNoteId?.let { id -> state.notes.firstOrNull { it.id == id } ?: state.pinned.firstOrNull { it.id == id } ?: state.favorites.firstOrNull { it.id == id } }
    if (menuNote != null) {
        NoteActionsSheet(
            note = menuNote,
            categories = categories,
            canMove = state.reorderAllowed && settings.homeLayout != HomeLayout.OVERVIEW,
            onDismiss = { menuNoteId = null },
            onTogglePin = { vm.togglePinned(menuNote) },
            onToggleFavorite = { vm.toggleFavorite(menuNote) },
            onToggleArchive = { vm.setArchived(menuNote, !menuNote.isArchived) },
            onDuplicate = { vm.duplicate(menuNote.id) },
            onColor = { vm.setColor(menuNote, it) },
            onCategory = { vm.setCategory(menuNote, it) },
            onShare = { actions.onShare(menuNote) },
            onExportTxt = { actions.onExportTxt(menuNote) },
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
            onLayout = vm::setHomeLayout,
            onReset = { vm.resetFilters(); vm.setSortMode(SortMode.MANUAL); vm.setFavoritesFirst(false) },
        )
    }
}

// ------------------------------------------------------------------------------------------ Kopfzeile

@Composable
private fun HomeHeader(
    compact: Boolean,
    isDark: Boolean,
    layout: HomeLayout,
    onMenu: () -> Unit,
    onSearch: () -> Unit,
    onLayout: (HomeLayout) -> Unit,
    onFilter: () -> Unit,
    onToggleTheme: () -> Unit,
    onSettings: () -> Unit,
) {
    val extras = LocalAppExtras.current
    var menuOpen by remember { mutableStateOf(false) }
    val actions: @Composable () -> Unit = {
        IconButton(onClick = onSearch) { Icon(Icons.Filled.Search, stringResource(R.string.search_open), tint = extras.onLeather) }
        Box {
            IconButton(onClick = { menuOpen = true }) { Icon(Icons.Filled.MoreVert, stringResource(R.string.a11y_more), tint = extras.onLeather) }
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                Text(stringResource(R.string.view_title), style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp))
                HomeLayout.entries.forEach { l ->
                    DropdownMenuItem(
                        text = { Text(stringResource(layoutLabel(l))) },
                        leadingIcon = { Icon(layoutIcon(l), null) },
                        trailingIcon = { if (l == layout) Icon(Icons.Filled.Check, null) },
                        onClick = { menuOpen = false; onLayout(l) },
                    )
                }
                HorizontalDivider()
                DropdownMenuItem(text = { Text(stringResource(R.string.filter_sort_title)) }, leadingIcon = { Icon(Icons.Outlined.Tune, null) },
                    onClick = { menuOpen = false; onFilter() })
                DropdownMenuItem(
                    text = { Text(stringResource(if (isDark) R.string.a11y_theme_light else R.string.a11y_theme_dark)) },
                    leadingIcon = { Icon(if (isDark) Icons.Outlined.LightMode else Icons.Outlined.DarkMode, null) },
                    onClick = { menuOpen = false; onToggleTheme() },
                )
                DropdownMenuItem(text = { Text(stringResource(R.string.settings_title)) }, leadingIcon = { Icon(Icons.Outlined.Settings, null) },
                    onClick = { menuOpen = false; onSettings() })
            }
        }
    }
    if (compact) {
        // Querformat: eine Zeile mit Logo, Titel und Aktionen – spart Höhe für die Notizen
        Row(
            Modifier.fillMaxWidth().statusBarsPadding().padding(start = 4.dp, end = 4.dp, top = 4.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onMenu) { Icon(Icons.Filled.Menu, stringResource(R.string.a11y_open_menu), tint = extras.onLeather) }
            Spacer(Modifier.width(4.dp))
            AppLogo(size = 40.dp)
            Spacer(Modifier.width(12.dp))
            Text(
                stringResource(R.string.home_title),
                style = MaterialTheme.typography.headlineSmall,
                color = extras.onLeather,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false),
            )
            Icon(Icons.Filled.Eco, null, tint = extras.leaf, modifier = Modifier.padding(start = 6.dp, bottom = 12.dp).size(18.dp).rotate(-20f))
            Spacer(Modifier.weight(1f))
            actions()
        }
        return
    }
    Column(Modifier.fillMaxWidth().statusBarsPadding()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onMenu) { Icon(Icons.Filled.Menu, stringResource(R.string.a11y_open_menu), tint = extras.onLeather) }
            Spacer(Modifier.weight(1f))
            actions()
        }
        Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, bottom = 22.dp), verticalAlignment = Alignment.CenterVertically) {
            AppLogo(size = 58.dp)
            Spacer(Modifier.width(14.dp))
            Text(
                stringResource(R.string.home_title),
                style = MaterialTheme.typography.headlineMedium,
                color = extras.onLeather,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false),
            )
            Icon(Icons.Filled.Eco, null, tint = extras.leaf, modifier = Modifier.padding(start = 6.dp, bottom = 18.dp).size(22.dp).rotate(-20f))
        }
    }
}

fun layoutLabel(l: HomeLayout) = when (l) {
    HomeLayout.GRID -> R.string.view_grid
    HomeLayout.LIST -> R.string.view_list
    HomeLayout.OVERVIEW -> R.string.view_overview
}

private fun layoutIcon(l: HomeLayout): ImageVector = when (l) {
    HomeLayout.GRID -> Icons.Outlined.GridView
    HomeLayout.LIST -> Icons.Outlined.ViewAgenda
    HomeLayout.OVERVIEW -> Icons.Outlined.Dashboard
}

@Composable
private fun SearchField(value: String, onChange: (String) -> Unit, onClose: () -> Unit) {
    val focus = LocalFocusManager.current
    val requester = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { requester.requestFocus() } }
    TextField(
        value = value,
        onValueChange = onChange,
        singleLine = true,
        placeholder = { Text(stringResource(R.string.search_hint)) },
        leadingIcon = { Icon(Icons.Filled.Search, null) },
        trailingIcon = {
            IconButton(onClick = { if (value.isNotEmpty()) onChange("") else onClose(); focus.clearFocus() }) {
                Box(Modifier.size(26.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.Close, stringResource(R.string.search_clear), Modifier.size(16.dp))
                }
            }
        },
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { focus.clearFocus() }),
        shape = RoundedCornerShape(26.dp),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = LocalAppExtras.current.cardHigh,
            unfocusedContainerColor = LocalAppExtras.current.cardHigh,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            cursorColor = MaterialTheme.colorScheme.onSurface,
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 4.dp)
            .border(1.dp, LocalAppExtras.current.cardBorder, RoundedCornerShape(26.dp))
            .focusRequester(requester)
            .testTag("search_field"),
    )
}

@Composable
private fun FilterChips(
    query: NoteQuery,
    categories: List<Category>,
    onQuickFilter: (QuickFilter) -> Unit,
    onCategory: (Int?) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        val noCategory = query.categoryId == null
        AppFilterChip(noCategory && query.quickFilter == QuickFilter.ALL, stringResource(R.string.filter_all)) { onQuickFilter(QuickFilter.ALL) }
        AppFilterChip(noCategory && query.quickFilter == QuickFilter.TEXT, stringResource(R.string.filter_text), Icons.AutoMirrored.Outlined.Notes) { onQuickFilter(QuickFilter.TEXT) }
        AppFilterChip(noCategory && query.quickFilter == QuickFilter.CHECKLISTS, stringResource(R.string.filter_checklists), Icons.Outlined.Checklist) { onQuickFilter(QuickFilter.CHECKLISTS) }
        categories.forEach { c ->
            AppFilterChip(query.categoryId == c.id, c.name, CategoryIcons.forCategory(c).icon) { onCategory(if (query.categoryId == c.id) null else c.id) }
        }
    }
}

/** „5 Ergebnisse für „Reise““ bzw. Bereichstitel + Sortier-Menü (wie im Screenshot „Suchen & sortieren“). */
@Composable
private fun ResultsBar(
    state: HomeUiState,
    query: NoteQuery,
    settings: AppSettings,
    onSort: (SortMode) -> Unit,
    onFavoritesFirst: (Boolean) -> Unit,
    onMoreFilters: () -> Unit,
) {
    val searching = query.search.isNotBlank()
    val showBar = searching || state.tab != HomeTab.NOTES || query.isFiltered || settings.homeLayout != HomeLayout.OVERVIEW
    if (!showBar || !state.loaded) return
    var open by remember { mutableStateOf(false) }
    Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 12.dp, top = 2.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        val label = when {
            searching -> pluralStringResource(R.plurals.search_results, state.notes.size, state.notes.size, query.search.trim())
            state.tab == HomeTab.FAVORITES -> stringResource(R.string.tab_favorites) + " · " + state.notes.size
            state.tab == HomeTab.ARCHIVE -> stringResource(R.string.tab_archive) + " · " + state.notes.size
            else -> pluralStringResource(R.plurals.note_count, state.notes.size, state.notes.size)
        }
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
        if (state.tab != HomeTab.ARCHIVE) {
            Box {
                Surface(
                    onClick = { open = true },
                    shape = CircleShape,
                    color = LocalAppExtras.current.cardHigh,
                    border = androidx.compose.foundation.BorderStroke(1.dp, LocalAppExtras.current.cardBorder),
                ) {
                    Row(Modifier.padding(start = 12.dp, end = 6.dp, top = 6.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.AutoMirrored.Filled.Sort, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(stringResource(sortShortLabel(settings.sortMode)), style = MaterialTheme.typography.labelLarge)
                        Icon(Icons.Filled.ArrowDropDown, stringResource(R.string.sort_title))
                    }
                }
                DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
                    SortMode.entries.forEach { mode ->
                        DropdownMenuItem(
                            text = { Text(stringResource(sortLabel(mode))) },
                            trailingIcon = { if (mode == settings.sortMode) Icon(Icons.Filled.Check, null, tint = LocalAppExtras.current.accent) },
                            onClick = { onSort(mode); open = false },
                        )
                    }
                    HorizontalDivider()
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.sort_favorites_first)) },
                        trailingIcon = { Switch(checked = settings.favoritesFirst, onCheckedChange = null) },
                        onClick = { onFavoritesFirst(!settings.favoritesFirst) },
                    )
                    DropdownMenuItem(text = { Text(stringResource(R.string.filter_more)) }, leadingIcon = { Icon(Icons.Outlined.Tune, null) },
                        onClick = { open = false; onMoreFilters() })
                }
            }
        }
    }
}

// ------------------------------------------------------------------------------------------ Inhalt

@Composable
private fun HomeContent(
    state: HomeUiState,
    query: NoteQuery,
    settings: AppSettings,
    categories: List<Category>,
    categoryById: Map<Int, Category>,
    bottomPadding: Dp,
    window: WindowLayout,
    vm: NotesViewModel,
    onMenu: (Note) -> Unit,
    onOpenCategories: () -> Unit,
) {
    if (!state.loaded) return
    val searching = query.search.isNotBlank()
    val overview = settings.homeLayout == HomeLayout.OVERVIEW && state.tab == HomeTab.NOTES && !query.isFiltered
    // Ohne schwebenden Button (seitliche Navigation) braucht es unten weniger Platz
    val contentBottom = bottomPadding + if (window.useNavigationRail) 24.dp else 96.dp

    if (state.notes.isEmpty() && !(overview && state.totalActive > 0)) {
        Box(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), contentAlignment = Alignment.TopCenter) {
            when {
                state.tab == HomeTab.ARCHIVE -> EmptyState(Icons.Outlined.Inventory2, stringResource(R.string.archive_empty_title), stringResource(R.string.archive_empty_subtitle), Modifier.padding(top = 32.dp))
                state.tab == HomeTab.FAVORITES -> EmptyState(Icons.Outlined.StarOutline, stringResource(R.string.favorites_empty_title), stringResource(R.string.favorites_empty_subtitle), Modifier.padding(top = 32.dp))
                state.totalActive == 0 -> EmptyState(Icons.AutoMirrored.Outlined.NoteAdd, stringResource(R.string.empty_title), stringResource(R.string.empty_subtitle), Modifier.padding(top = 32.dp))
                searching -> EmptyState(Icons.Outlined.SearchOff, stringResource(R.string.empty_search_title, query.search.trim()), stringResource(R.string.empty_search_subtitle), Modifier.padding(top = 32.dp))
                else -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    EmptyState(Icons.Outlined.Tune, stringResource(R.string.empty_filter_title), stringResource(R.string.empty_filter_subtitle), Modifier.padding(top = 32.dp))
                    TextButton(onClick = { vm.resetFilters() }) { Text(stringResource(R.string.filter_reset)) }
                }
            }
        }
        return
    }

    if (overview) {
        OverviewContent(state, settings, categories, categoryById, contentBottom, window, vm, onMenu, onOpenCategories)
        return
    }

    // Suche zeigt Treffer immer als Liste (wie im Screenshot); sonst Raster oder Liste nach Einstellung
    val asGrid = settings.homeLayout == HomeLayout.GRID && !searching
    val columns = when {
        asGrid -> GridCells.Adaptive(window.gridMinCell)
        window.listColumns > 1 -> GridCells.Fixed(window.listColumns)
        else -> GridCells.Fixed(1)
    }
    val listState = rememberLazyGridState()
    val dragState = remember { NoteDragState() }
    val editLabel = stringResource(R.string.action_edit)
    val upLabel = stringResource(R.string.action_move_up)
    val downLabel = stringResource(R.string.action_move_down)
    ReorderableNoteList(
        notes = state.notes,
        reorderEnabled = state.reorderAllowed,
        listState = listState,
        dragState = dragState,
        columns = columns,
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = contentBottom),
        onReorder = vm::reorder,
        onLongPressWithoutMove = onMenu,
        modifier = Modifier.fillMaxSize(),
    ) { note, isPlaceholder, isOverlay ->
        val a11y = buildList {
            add(CustomAccessibilityAction(editLabel) { vm.openEditor(note); true })
            if (state.reorderAllowed) {
                add(CustomAccessibilityAction(upLabel) { vm.moveByOne(note.id, true); true })
                add(CustomAccessibilityAction(downLabel) { vm.moveByOne(note.id, false); true })
            }
        }
        NoteCard(
            note = note,
            category = note.categoryId?.let(categoryById::get),
            layout = if (asGrid) CardLayout.GRID else CardLayout.LIST,
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
            accessibilityActions = a11y,
            onToggleFavorite = if (asGrid) null else ({ vm.toggleFavorite(note) }),
        )
    }
}

/** Übersicht wie im Screenshot „Alles schön organisiert“. */
@Composable
private fun OverviewContent(
    state: HomeUiState,
    settings: AppSettings,
    categories: List<Category>,
    categoryById: Map<Int, Category>,
    bottomPadding: Dp,
    window: WindowLayout,
    vm: NotesViewModel,
    onMenu: (Note) -> Unit,
    onOpenCategories: () -> Unit,
) {
    val cols = window.listColumns
    val others = remember(state.notes) { state.notes.filterNot { it.isPinned } }
    LazyColumn(
        contentPadding = PaddingValues(top = 8.dp, bottom = bottomPadding),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxSize().testTag("overview"),
    ) {
        // Auf Tablets stehen Listenkarten nebeneinander (Zeilen mit 2–3 Karten), auf Handys untereinander
        items(state.pinned.chunked(cols), key = { row -> "pin_${row.first().id}" }) { row ->
            CardRow(row, cols, Modifier.animateItem()) { note, m ->
                NoteCard(
                    note = note, category = note.categoryId?.let(categoryById::get), layout = CardLayout.LIST,
                    density = settings.cardDensity, searchQuery = "", hideCheckedItems = settings.hideCheckedItems,
                    isPlaceholder = false, isOverlay = false, onClick = { vm.openEditor(note) }, onLongClick = { onMenu(note) },
                    onMenu = { onMenu(note) }, accessibilityActions = emptyList(),
                    modifier = m,
                )
            }
        }
        if (state.favorites.isNotEmpty()) {
            item(key = "fav_header") {
                SectionHeader(Icons.Filled.Star, Color(0xFFF2B21B), stringResource(R.string.tab_favorites), onAll = { vm.setTab(HomeTab.FAVORITES) })
            }
            item(key = "fav_row") {
                LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(state.favorites, key = { it.id }) { note ->
                        NoteCard(
                            note = note, category = note.categoryId?.let(categoryById::get), layout = CardLayout.GRID,
                            density = settings.cardDensity, searchQuery = "", hideCheckedItems = settings.hideCheckedItems,
                            isPlaceholder = false, isOverlay = false, onClick = { vm.openEditor(note) }, onLongClick = { onMenu(note) },
                            onMenu = { onMenu(note) }, accessibilityActions = emptyList(),
                            modifier = Modifier.width(window.favoriteCardWidth),
                        )
                    }
                }
            }
        }
        if (categories.isNotEmpty()) {
            item(key = "cat_header") {
                SectionHeader(Icons.Outlined.Category, LocalAppExtras.current.accent, stringResource(R.string.categories_title), onAll = onOpenCategories)
            }
            items(categories.chunked(window.categoryColumns), key = { row -> "cat_${row.first().id}" }) { row ->
                CardRow(row, window.categoryColumns) { c, m ->
                    CategoryTile(c, state.categoryCounts[c.id] ?: 0, m) { vm.setCategoryFilter(c.id) }
                }
            }
        }
        if (others.isNotEmpty()) {
            item(key = "notes_header") {
                SectionHeader(Icons.AutoMirrored.Outlined.Notes, LocalAppExtras.current.accent, stringResource(R.string.section_my_notes),
                    onAll = { vm.setHomeLayout(HomeLayout.LIST) })
            }
            items(others.chunked(cols), key = { row -> row.first().id }) { row ->
                CardRow(row, cols, Modifier.animateItem()) { note, m ->
                    NoteCard(
                        note = note, category = note.categoryId?.let(categoryById::get), layout = CardLayout.LIST,
                        density = settings.cardDensity, searchQuery = "", hideCheckedItems = settings.hideCheckedItems,
                        isPlaceholder = false, isOverlay = false, onClick = { vm.openEditor(note) }, onLongClick = { onMenu(note) },
                        onMenu = { onMenu(note) }, accessibilityActions = emptyList(),
                        onToggleFavorite = { vm.toggleFavorite(note) },
                        modifier = m,
                    )
                }
            }
        }
    }
}

/** Eine Zeile mit [columns] gleich breiten Feldern; fehlende Plätze bleiben leer, damit alle Karten gleich breit sind. */
@Composable
private fun <T> CardRow(row: List<T>, columns: Int, modifier: Modifier = Modifier, cell: @Composable (T, Modifier) -> Unit) {
    Row(modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        row.forEach { cell(it, Modifier.weight(1f)) }
        repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
    }
}

@Composable
private fun SectionHeader(icon: ImageVector, tint: Color, title: String, onAll: (() -> Unit)?) {
    Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp, top = 14.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(24.dp))
        Spacer(Modifier.width(10.dp))
        Text(title, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onBackground, modifier = Modifier.weight(1f))
        if (onAll != null) {
            TextButton(onClick = onAll) {
                Text(stringResource(R.string.show_all), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun CategoryTile(category: Category, count: Int, modifier: Modifier, onClick: () -> Unit) {
    val extras = LocalAppExtras.current
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        color = extras.card,
        border = androidx.compose.foundation.BorderStroke(1.dp, extras.cardBorder),
        shadowElevation = 1.dp,
        modifier = modifier,
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            IconTile(CategoryIcons.forCategory(category), size = 44.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(category.name, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(pluralStringResource(R.plurals.note_count, count, count), style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

// ------------------------------------------------------------------------------------------ Navigation

@Composable
private fun HomeBottomBar(tab: HomeTab, archivedCount: Int, onTab: (HomeTab) -> Unit) {
    val extras = LocalAppExtras.current
    Column {
        HorizontalDivider(color = extras.cardBorder)
        NavigationBar(containerColor = extras.cardHigh, tonalElevation = 0.dp) {
            val colors = NavigationBarItemDefaults.colors(
                selectedIconColor = extras.leatherDark.takeUnless { extras.isDark } ?: extras.accent,
                selectedTextColor = MaterialTheme.colorScheme.onSurface,
                indicatorColor = extras.accent.copy(alpha = if (extras.isDark) 0.22f else 0.45f),
                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            NavigationBarItem(
                selected = tab == HomeTab.NOTES, onClick = { onTab(HomeTab.NOTES) }, colors = colors,
                icon = { Icon(if (tab == HomeTab.NOTES) Icons.Filled.Home else Icons.Outlined.Home, null) },
                label = { Text(stringResource(R.string.tab_notes)) },
                modifier = Modifier.testTag("tab_notes"),
            )
            NavigationBarItem(
                selected = tab == HomeTab.FAVORITES, onClick = { onTab(HomeTab.FAVORITES) }, colors = colors,
                icon = { Icon(if (tab == HomeTab.FAVORITES) Icons.Filled.Star else Icons.Outlined.StarOutline, null) },
                label = { Text(stringResource(R.string.tab_favorites)) },
                modifier = Modifier.testTag("tab_favorites"),
            )
            NavigationBarItem(
                selected = tab == HomeTab.ARCHIVE, onClick = { onTab(HomeTab.ARCHIVE) }, colors = colors,
                icon = { Icon(if (tab == HomeTab.ARCHIVE) Icons.Filled.Inventory2 else Icons.Outlined.Inventory2, null) },
                label = { Text(stringResource(R.string.tab_archive)) },
                modifier = Modifier.testTag("tab_archive"),
            )
        }
    }
}

/** Seitliche Navigation im Querformat und auf großen Tablets – auf dem Leder, links neben dem Papier. */
@Composable
private fun HomeRail(tab: HomeTab, showFab: Boolean, short: Boolean, onNewNote: () -> Unit, onTab: (HomeTab) -> Unit, modifier: Modifier = Modifier) {
    val gap = if (short) 0.dp else 8.dp
    val extras = LocalAppExtras.current
    NavigationRail(
        containerColor = Color.Transparent,
        contentColor = extras.onLeather,
        windowInsets = WindowInsets(0),
        header = {
            if (showFab) {
                FloatingActionButton(
                    onClick = onNewNote,
                    shape = CircleShape,
                    containerColor = extras.accent,
                    contentColor = extras.onAccent,
                    modifier = Modifier.padding(top = 4.dp).size(56.dp).testTag("fab_new_note"),
                ) {
                    Icon(Icons.Filled.Add, stringResource(R.string.new_note), Modifier.size(28.dp))
                }
            } else {
                Spacer(Modifier.size(60.dp))
            }
        },
        modifier = modifier.fillMaxHeight().width(88.dp).testTag("nav_rail"),
    ) {
        Spacer(Modifier.height(if (short) 4.dp else 16.dp))
        val colors = NavigationRailItemDefaults.colors(
            selectedIconColor = extras.leatherDark,
            selectedTextColor = extras.onLeather,
            indicatorColor = extras.accent,
            unselectedIconColor = extras.onLeather.copy(alpha = 0.8f),
            unselectedTextColor = extras.onLeather.copy(alpha = 0.8f),
        )
        NavigationRailItem(
            selected = tab == HomeTab.NOTES, onClick = { onTab(HomeTab.NOTES) }, colors = colors,
            icon = { Icon(if (tab == HomeTab.NOTES) Icons.Filled.Home else Icons.Outlined.Home, null) },
            label = { Text(stringResource(R.string.tab_notes)) },
            modifier = Modifier.testTag("tab_notes"),
        )
        Spacer(Modifier.height(gap))
        NavigationRailItem(
            selected = tab == HomeTab.FAVORITES, onClick = { onTab(HomeTab.FAVORITES) }, colors = colors,
            icon = { Icon(if (tab == HomeTab.FAVORITES) Icons.Filled.Star else Icons.Outlined.StarOutline, null) },
            label = { Text(stringResource(R.string.tab_favorites)) },
            modifier = Modifier.testTag("tab_favorites"),
        )
        Spacer(Modifier.height(gap))
        NavigationRailItem(
            selected = tab == HomeTab.ARCHIVE, onClick = { onTab(HomeTab.ARCHIVE) }, colors = colors,
            icon = { Icon(if (tab == HomeTab.ARCHIVE) Icons.Filled.Inventory2 else Icons.Outlined.Inventory2, null) },
            label = { Text(stringResource(R.string.tab_archive)) },
            modifier = Modifier.testTag("tab_archive"),
        )
    }
}

@Composable
private fun AppDrawer(
    tab: HomeTab,
    showPremium: Boolean,
    onTab: (HomeTab) -> Unit,
    onTrash: () -> Unit,
    onCategories: () -> Unit,
    onSettings: () -> Unit,
    onPremium: () -> Unit,
) {
    val extras = LocalAppExtras.current
    ModalDrawerSheet(drawerContainerColor = MaterialTheme.colorScheme.background, modifier = Modifier.width(300.dp)) {
        Column(Modifier.fillMaxWidth().leather().statusBarsPadding().padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 28.dp)) {
            AppLogo(size = 64.dp)
            Spacer(Modifier.height(12.dp))
            Text(stringResource(R.string.app_name), style = MaterialTheme.typography.titleLarge, color = extras.onLeather)
            Text(stringResource(R.string.drawer_tagline), style = MaterialTheme.typography.bodySmall, color = extras.onLeather.copy(alpha = 0.75f))
        }
        Spacer(Modifier.height(12.dp))
        val itemColors = NavigationDrawerItemDefaults.colors(
            selectedContainerColor = extras.accent.copy(alpha = if (extras.isDark) 0.22f else 0.45f),
            unselectedContainerColor = Color.Transparent,
        )
        @Composable
        fun item(icon: ImageVector, label: Int, selected: Boolean = false, onClick: () -> Unit) =
            NavigationDrawerItem(
                icon = { Icon(icon, null) }, label = { Text(stringResource(label), style = MaterialTheme.typography.titleSmall) },
                selected = selected, onClick = onClick, colors = itemColors,
                modifier = Modifier.padding(horizontal = 12.dp),
            )
        item(Icons.Outlined.Home, R.string.drawer_all_notes, tab == HomeTab.NOTES) { onTab(HomeTab.NOTES) }
        item(Icons.Outlined.StarOutline, R.string.tab_favorites, tab == HomeTab.FAVORITES) { onTab(HomeTab.FAVORITES) }
        item(Icons.Outlined.Inventory2, R.string.tab_archive, tab == HomeTab.ARCHIVE) { onTab(HomeTab.ARCHIVE) }
        item(Icons.Outlined.Delete, R.string.trash_title, onClick = onTrash)
        HorizontalDivider(Modifier.padding(horizontal = 28.dp, vertical = 8.dp), color = extras.cardBorder)
        item(Icons.Outlined.Category, R.string.categories_title, onClick = onCategories)
        item(Icons.Outlined.Settings, R.string.settings_title, onClick = onSettings)
        if (showPremium) item(Icons.Outlined.WorkspacePremium, R.string.premium_unlock, onClick = onPremium)
    }
}

@Composable
private fun AdFreeBadge(onClose: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().background(LocalAppExtras.current.sunken).padding(start = 20.dp, end = 8.dp, top = 2.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("★", style = MaterialTheme.typography.labelLarge, color = LocalAppExtras.current.accent)
        Spacer(Modifier.width(10.dp))
        Text(stringResource(R.string.ad_free_thanks), style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
        IconButton(onClick = onClose) { Icon(Icons.Filled.Close, stringResource(R.string.action_close), Modifier.size(18.dp)) }
    }
}

// ------------------------------------------------------------------------------------------ Sortieren & Filtern

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
    onLayout: (HomeLayout) -> Unit,
    onReset: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.background,
    ) {
        Column(Modifier.verticalScroll(rememberScrollState()).navigationBarsPadding()) {
            Row(Modifier.padding(horizontal = 24.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.filter_sort_title), style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                TextButton(onClick = onReset) { Text(stringResource(R.string.filter_reset)) }
            }
            SheetLabel(stringResource(R.string.view_title))
            Row(Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                HomeLayout.entries.forEach { l -> AppFilterChip(settings.homeLayout == l, stringResource(layoutLabel(l)), layoutIcon(l)) { onLayout(l) } }
            }
            SheetLabel(stringResource(R.string.sort_title))
            SortMode.entries.forEach { mode ->
                Row(
                    Modifier.fillMaxWidth().clickable(role = Role.RadioButton) { onSort(mode) }.padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(selected = settings.sortMode == mode, onClick = { onSort(mode) })
                    Text(stringResource(sortLabel(mode)), style = MaterialTheme.typography.bodyLarge)
                }
            }
            SwitchRow(stringResource(R.string.sort_favorites_first), settings.favoritesFirst, onFavoritesFirst)
            if (settings.sortMode != SortMode.MANUAL || settings.favoritesFirst) {
                Text(stringResource(R.string.sort_manual_hint), style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp))
            }
            SheetLabel(stringResource(R.string.filter_period))
            Row(Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DateFilter.entries.forEach { f -> AppFilterChip(query.dateFilter == f, stringResource(dateLabel(f))) { onDateFilter(f) } }
            }
            SwitchRow(stringResource(R.string.filter_only_content), query.onlyWithContent, onOnlyWithContent)
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SwitchRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(role = Role.Switch) { onChange(!checked) }.padding(horizontal = 24.dp, vertical = 10.dp),
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

private fun sortShortLabel(mode: SortMode): Int = when (mode) {
    SortMode.MANUAL -> R.string.sort_short_manual
    SortMode.UPDATED -> R.string.sort_short_updated
    SortMode.NEWEST -> R.string.sort_short_newest
    SortMode.OLDEST -> R.string.sort_short_oldest
    SortMode.TITLE_AZ -> R.string.sort_title_az
    SortMode.TITLE_ZA -> R.string.sort_title_za
}

private fun dateLabel(f: DateFilter): Int = when (f) {
    DateFilter.ANY -> R.string.filter_any_time
    DateFilter.TODAY -> R.string.filter_today
    DateFilter.LAST_7_DAYS -> R.string.filter_7_days
    DateFilter.LAST_30_DAYS -> R.string.filter_30_days
}
