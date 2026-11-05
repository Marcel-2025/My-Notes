package dev.flondy.mynotes.ui

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.background
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.flondy.mynotes.Note
import dev.flondy.mynotes.NotesViewModel
import dev.flondy.mynotes.R
import dev.flondy.mynotes.SettingsViewModel
import dev.flondy.mynotes.UiEvent
import dev.flondy.mynotes.billing.BillingManager
import dev.flondy.mynotes.data.NoteRepository
import dev.flondy.mynotes.ui.editor.EditorActions
import dev.flondy.mynotes.ui.editor.EditorScreen
import dev.flondy.mynotes.ui.home.HomeActions
import dev.flondy.mynotes.ui.home.HomeScreen
import dev.flondy.mynotes.ui.premium.PremiumScreen
import dev.flondy.mynotes.ui.settings.CategoriesScreen
import dev.flondy.mynotes.ui.settings.SettingsActions
import dev.flondy.mynotes.ui.settings.SettingsScreen
import dev.flondy.mynotes.ui.theme.LocalAppExtras
import dev.flondy.mynotes.ui.theme.isAppInDarkTheme
import dev.flondy.mynotes.ui.trash.TrashScreen
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class Route { HOME, SETTINGS, CATEGORIES, TRASH, PREMIUM }

object Links {
    const val PLAY_STORE = "https://play.google.com/store/apps/details?id=dev.flondy.mynotes"
    const val LANDING_PAGE = "https://flondy.github.io/my-notes-landing-page"
    const val PRIVACY_POLICY = "https://marcel-2025.github.io/My-Notes-privacy/"
    const val FEEDBACK_EMAIL = "flondy.dev@gmail.com"
}

@Composable
fun AppRoot(notesVm: NotesViewModel, settingsVm: SettingsViewModel) {
    val context = LocalContext.current
    val activity = context as Activity
    val scope = rememberCoroutineScope()
    val settings by settingsVm.settings.collectAsStateWithLifecycle()
    val premium by settingsVm.premium.collectAsStateWithLifecycle()
    val adsReady by settingsVm.adsReady.collectAsStateWithLifecycle()
    val privacyRequired by settingsVm.privacyOptionsRequired.collectAsStateWithLifecycle()
    val categories by notesVm.categories.collectAsStateWithLifecycle()
    val trash by notesVm.trash.collectAsStateWithLifecycle()
    val homeState by notesVm.homeState.collectAsStateWithLifecycle()
    val isDark = isAppInDarkTheme(settings.themeMode)
    val snackbar = remember { SnackbarHostState() }
    val animate = LocalAppExtras.current.animationsEnabled

    val backStack = rememberSaveable(
        saver = listSaver(save = { it.map(Route::name) }, restore = { mutableStateListOf(*it.map(Route::valueOf).toTypedArray()) })
    ) { mutableStateListOf(Route.HOME) }
    fun navigate(route: Route) { if (backStack.last() != route) backStack.add(route) }
    fun back() { if (backStack.size > 1) backStack.removeAt(backStack.lastIndex) }

    var importPreview by remember { mutableStateOf<NoteRepository.ImportPreview?>(null) }
    var pendingTxtNote by remember { mutableStateOf<Note?>(null) }

    // --- Dateiauswahl (Storage Access Framework – keine Speicherberechtigung nötig)
    val exportBackupLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        uri?.let(notesVm::exportBackup)
    }
    val importBackupLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let(notesVm::previewImport)
    }
    val exportTxtLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/plain")) { uri ->
        val note = pendingTxtNote
        if (uri != null && note != null) notesVm.exportNoteAsTxt(note, uri)
        pendingTxtNote = null
    }

    fun share(note: Note) {
        val text = notesVm.shareText(note)
        if (text.isBlank()) return
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
            if (note.title.isNotBlank()) putExtra(Intent.EXTRA_SUBJECT, note.title)
        }
        context.startActivity(Intent.createChooser(send, context.getString(R.string.action_share)))
    }

    fun exportTxt(note: Note) {
        pendingTxtNote = note
        val name = note.title.ifBlank { context.getString(R.string.untitled_note) }.replace(Regex("[\\\\/:*?\"<>|]"), "_").take(60)
        exportTxtLauncher.launch("$name.txt")
    }

    // --- Ereignisse aus den ViewModels als Snackbar
    val undoLabel = stringResource(R.string.action_undo)
    LaunchedEffect(Unit) {
        notesVm.events.collect { event ->
            when (event) {
                is UiEvent.MovedToTrash -> scope.launch {
                    val result = snackbar.showSnackbar(context.getString(R.string.moved_to_trash), undoLabel, duration = SnackbarDuration.Short)
                    if (result == SnackbarResult.ActionPerformed) notesVm.restoreFromTrash(event.noteId)
                }
                is UiEvent.Message -> scope.launch { snackbar.showSnackbar(context.formatMessage(event.resId, event.arg)) }
                is UiEvent.ImportPreviewReady -> importPreview = event.preview
                is UiEvent.Archived -> scope.launch {
                    val result = snackbar.showSnackbar(context.getString(R.string.archived_message), undoLabel, duration = SnackbarDuration.Short)
                    if (result == SnackbarResult.ActionPerformed) notesVm.unarchive(event.noteId)
                }
                is UiEvent.Duplicated -> scope.launch { snackbar.showSnackbar(context.getString(R.string.duplicated_message)) }
            }
        }
    }
    LaunchedEffect(Unit) {
        settingsVm.billingEvents.collect { event ->
            val msg = when (event) {
                BillingManager.Event.PurchaseSuccess -> R.string.billing_success
                BillingManager.Event.PurchasePending -> R.string.premium_pending
                BillingManager.Event.PurchaseCanceled -> R.string.billing_canceled
                BillingManager.Event.AlreadyOwned -> R.string.billing_already_owned
                is BillingManager.Event.RestoreResult -> if (event.restored) R.string.billing_restored else R.string.billing_nothing_to_restore
                is BillingManager.Event.Error -> R.string.billing_error
            }
            scope.launch { snackbar.showSnackbar(context.getString(msg)) }
        }
    }

    val editor = notesVm.editor
    // Im Querformat liegen Navigationsleiste/Kamera-Aussparung seitlich: Inhalte davon fernhalten, Fläche dahinter in Leder-Braun
    Box(
        Modifier
            .fillMaxSize()
            .background(dev.flondy.mynotes.ui.theme.LocalAppExtras.current.leatherDark)
            .windowInsetsPadding(
                androidx.compose.foundation.layout.WindowInsets.safeDrawing
                    .only(androidx.compose.foundation.layout.WindowInsetsSides.Horizontal),
            ),
    ) {
        AnimatedContent(
            targetState = backStack.last(),
            transitionSpec = { routeTransition(initialState, targetState, animate) },
            label = "routes",
        ) { route ->
            when (route) {
                Route.HOME -> HomeScreen(
                    vm = notesVm,
                    settings = settings,
                    premium = premium,
                    adsReady = adsReady,
                    isDark = isDark,
                    snackbarHostState = snackbar,
                    actions = HomeActions(
                        onOpenSettings = { navigate(Route.SETTINGS) },
                        onOpenPremium = { navigate(Route.PREMIUM) },
                        onOpenTrash = { navigate(Route.TRASH) },
                        onOpenCategories = { navigate(Route.CATEGORIES) },
                        onToggleTheme = { settingsVm.toggleTheme(isDark) },
                        onHideAdFreeBadge = { settingsVm.setShowAdFreeBadge(false) },
                        onShare = ::share,
                        onExportTxt = ::exportTxt,
                    ),
                )
                Route.SETTINGS -> SettingsScreen(
                    settings = settings,
                    premium = premium,
                    privacyOptionsRequired = privacyRequired,
                    trashCount = trash.size,
                    actions = SettingsActions(
                        onBack = ::back,
                        onThemeMode = settingsVm::setThemeMode,
                        onAccent = settingsVm::setAccent,
                        onCardDensity = settingsVm::setCardDensity,
                        onAnimations = settingsVm::setAnimationsEnabled,
                        onImmersive = settingsVm::setImmersiveMode,
                        onAppIcon = settingsVm::setAppIcon,
                        onAdFreeBadge = settingsVm::setShowAdFreeBadge,
                        onSortMode = notesVm::setSortMode,
                        onHideChecked = settingsVm::setHideCheckedItems,
                        onOpenTrash = { navigate(Route.TRASH) },
                        onOpenCategories = { navigate(Route.CATEGORIES) },
                        onExportBackup = {
                            val date = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
                            exportBackupLauncher.launch("MyNotes-Backup-$date.json")
                        },
                        onImportBackup = { importBackupLauncher.launch(arrayOf("application/json", "application/octet-stream", "text/plain")) },
                        onOpenPremium = { navigate(Route.PREMIUM) },
                        onRestorePurchases = settingsVm::restorePurchases,
                        onPrivacyPolicy = { context.openUrl(Links.PRIVACY_POLICY) },
                        onPrivacyOptions = { settingsVm.showPrivacyOptions(activity) {} },
                        onDeleteAllNotes = { notesVm.deleteAllNotes() },
                        onRate = { context.openPlayStore() },
                        onFeedback = { context.sendFeedback() },
                        onLandingPage = { context.openUrl(Links.LANDING_PAGE) },
                        onDebugAdFree = settingsVm::setDebugForceAdFree,
                    ),
                )
                Route.CATEGORIES -> CategoriesScreen(
                    categories = categories,
                    counts = homeState.categoryCounts,
                    onBack = ::back,
                    onAdd = { name, icon -> notesVm.addCategory(name, icon) },
                    onUpdate = { notesVm.updateCategory(it) },
                    onDelete = { notesVm.deleteCategory(it) },
                )
                Route.TRASH -> TrashScreen(
                    notes = trash,
                    retentionDays = settings.trashRetentionDays,
                    onBack = ::back,
                    onRestore = { notesVm.restoreFromTrash(it.id) },
                    onDeleteForever = { notesVm.deleteForever(it.id) },
                    onEmptyTrash = { notesVm.emptyTrash() },
                )
                Route.PREMIUM -> PremiumScreen(
                    state = premium,
                    onBack = ::back,
                    onPurchase = { settingsVm.purchaseAdFree(activity) },
                    onRestore = settingsVm::restorePurchases,
                    onRefresh = settingsVm::refreshBilling,
                )
            }
        }

        // Editor als Vollbild-Ebene mit sanftem Übergang – hier wird niemals Werbung angezeigt.
        AnimatedVisibility(
            visible = editor != null,
            enter = if (animate) fadeIn(tween(220)) + scaleIn(tween(260), initialScale = 0.96f) else fadeIn(tween(0)),
            exit = if (animate) fadeOut(tween(180)) + scaleOut(tween(200), targetScale = 0.97f) else fadeOut(tween(0)),
        ) {
            // Letzten Zustand während der Ausblend-Animation weiter anzeigen
            val last = remember { mutableStateOf(editor) }
            if (editor != null) last.value = editor
            val state = last.value ?: return@AnimatedVisibility
            EditorScreen(
                state = state,
                categories = categories,
                hideCheckedItems = settings.hideCheckedItems,
                actions = EditorActions(
                    onTitle = notesVm::onEditorTitleChange,
                    onContent = notesVm::onEditorContentChange,
                    onItems = notesVm::onEditorItemsChange,
                    onTogglePin = notesVm::onEditorTogglePinned,
                    onToggleFavorite = notesVm::onEditorToggleFavorite,
                    onColor = notesVm::onEditorColor,
                    onCategory = notesVm::onEditorCategory,
                    onToggleArchive = notesVm::onEditorToggleArchived,
                    onToggleChecklist = notesVm::onEditorToggleChecklist,
                    onTextToChecklist = notesVm::onEditorTextToChecklist,
                    onShare = { notesVm.editorAsNote()?.let(::share) },
                    onExportTxt = { notesVm.editorAsNote()?.let(::exportTxt) },
                    onDuplicate = notesVm::duplicateFromEditor,
                    onClose = notesVm::closeEditor,
                    onDiscard = notesVm::discardEditorChanges,
                    onDelete = notesVm::deleteFromEditor,
                ),
            )
        }
    }

    importPreview?.let { preview ->
        AlertDialog(
            onDismissRequest = { importPreview = null },
            title = { Text(stringResource(R.string.backup_import_preview_title)) },
            text = {
                Column {
                    Text(stringResource(R.string.backup_import_preview_text, preview.data.notes.size, preview.newNotes, preview.duplicates, preview.newCategories))
                }
            },
            confirmButton = {
                TextButton(enabled = preview.newNotes > 0, onClick = { notesVm.confirmImport(preview); importPreview = null }) {
                    Text(stringResource(R.string.backup_import_confirm, preview.newNotes))
                }
            },
            dismissButton = { TextButton(onClick = { importPreview = null }) { Text(stringResource(R.string.action_cancel)) } },
            shape = MaterialTheme.shapes.extraLarge,
        )
    }
}

private fun routeTransition(from: Route, to: Route, animate: Boolean): ContentTransform {
    if (!animate) return fadeIn(tween(0)) togetherWith fadeOut(tween(0))
    if (to == Route.PREMIUM) return (slideInVertically(tween(320)) { it / 3 } + fadeIn(tween(250))) togetherWith fadeOut(tween(200))
    if (from == Route.PREMIUM) return fadeIn(tween(200)) togetherWith (slideOutVertically(tween(280)) { it / 3 } + fadeOut(tween(220)))
    val forward = to.ordinal > from.ordinal
    return (slideInHorizontally(tween(300)) { if (forward) it / 4 else -it / 4 } + fadeIn(tween(250))) togetherWith
        (slideOutHorizontally(tween(300)) { if (forward) -it / 6 else it / 6 } + fadeOut(tween(200)))
}

private fun Context.formatMessage(resId: Int, arg: Any?): String =
    if (arg == null) getString(resId) else getString(resId, arg)

fun Context.openUrl(url: String) {
    try {
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    } catch (_: ActivityNotFoundException) {
    }
}

private fun Context.openPlayStore() {
    try {
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$packageName")).setPackage("com.android.vending"))
    } catch (_: ActivityNotFoundException) {
        openUrl(Links.PLAY_STORE)
    }
}

private fun Context.sendFeedback() {
    val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:")).apply {
        putExtra(Intent.EXTRA_EMAIL, arrayOf(Links.FEEDBACK_EMAIL))
        putExtra(Intent.EXTRA_SUBJECT, getString(R.string.feedback_subject))
    }
    try {
        startActivity(intent)
    } catch (_: ActivityNotFoundException) {
        openUrl(Links.LANDING_PAGE)
    }
}
