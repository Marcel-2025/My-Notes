package dev.flondy.mynotes

import android.app.Application
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.flondy.mynotes.data.BackupFormat
import dev.flondy.mynotes.data.ChecklistCodec
import dev.flondy.mynotes.data.ChecklistItem
import dev.flondy.mynotes.data.DateFilter
import dev.flondy.mynotes.data.NoteFilters
import dev.flondy.mynotes.data.NoteQuery
import dev.flondy.mynotes.data.NoteRepository
import dev.flondy.mynotes.data.QuickFilter
import dev.flondy.mynotes.settings.AppSettings
import dev.flondy.mynotes.settings.SortMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/** Untere Navigation der Startseite. */
enum class HomeTab { NOTES, FAVORITES, ARCHIVE }

data class HomeUiState(
    val tab: HomeTab = HomeTab.NOTES,
    val notes: List<Note> = emptyList(),
    val totalActive: Int = 0,
    val reorderAllowed: Boolean = true,
    val loaded: Boolean = false,
    /** Für die Übersicht: angeheftete und Favoriten (ohne Filter), Anzahl Notizen je Kategorie. */
    val pinned: List<Note> = emptyList(),
    val favorites: List<Note> = emptyList(),
    val categoryCounts: Map<Int, Int> = emptyMap(),
    val archivedCount: Int = 0,
)

enum class SaveStatus { IDLE, PENDING, SAVED }

/** Zustand des Editors – als Compose-Snapshot-State, damit Texteingaben synchron und ohne Cursor-Sprünge bleiben. */
data class EditorState(
    val noteId: Int?,
    val type: Int,
    val title: String,
    /** Notiztext (mit optionaler Formatierung). */
    val content: String,
    /** Checklisten-Einträge – sichtbar, wenn [showChecklist]. */
    val items: List<ChecklistItem>,
    val isPinned: Boolean,
    val isFavorite: Boolean,
    val color: Int,
    val categoryId: Int?,
    val createdAt: Long?,
    val updatedAt: Long?,
    val saveStatus: SaveStatus = SaveStatus.IDLE,
    val focusTitle: Boolean = false,
    val showChecklist: Boolean = false,
    val isArchived: Boolean = false,
) {
    val isChecklist get() = type == Note.TYPE_CHECKLIST
    val encodedChecklist: String get() = if (showChecklist) ChecklistCodec.encode(items) else ""
    val isBlank: Boolean get() = title.isBlank() && content.isBlank() && encodedChecklist.isBlank()
}

sealed interface UiEvent {
    data class MovedToTrash(val noteId: Int) : UiEvent
    data class Message(val resId: Int, val arg: Any? = null) : UiEvent
    data class ImportPreviewReady(val preview: NoteRepository.ImportPreview) : UiEvent
    data class Archived(val noteId: Int) : UiEvent
    data class Duplicated(val newId: Int) : UiEvent
}

class NotesViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application.app
    private val repo: NoteRepository = app.repository
    private val settingsStore = app.settings

    val settings: StateFlow<AppSettings> = settingsStore.settingsFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, AppSettings())

    val categories: StateFlow<List<Category>> = repo.categories
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    /** Alle aktiven Notizen in manueller Reihenfolge (Kompatibilität zur bisherigen API). */
    val notes: StateFlow<List<Note>> = repo.activeNotes
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val trash: StateFlow<List<Note>> = repo.trash
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _query = MutableStateFlow(NoteQuery())
    val query: StateFlow<NoteQuery> = _query.asStateFlow()

    private val _tab = MutableStateFlow(HomeTab.NOTES)
    val tab: StateFlow<HomeTab> = _tab.asStateFlow()

    val homeState: StateFlow<HomeUiState> = combine(repo.activeNotes, repo.archived, _query, settings, _tab) { all, archived, q, s, tab ->
        val visible = when (tab) {
            HomeTab.NOTES -> NoteFilters.apply(all, q, s.sortMode, s.favoritesFirst)
            HomeTab.FAVORITES -> NoteFilters.apply(all, q.copy(quickFilter = QuickFilter.FAVORITES, categoryId = null), s.sortMode, false)
            HomeTab.ARCHIVE -> NoteFilters.apply(archived, q.copy(quickFilter = QuickFilter.ALL, categoryId = null), SortMode.UPDATED, false)
        }
        HomeUiState(
            tab = tab,
            notes = visible,
            totalActive = all.size,
            reorderAllowed = tab == HomeTab.NOTES && NoteFilters.isReorderAllowed(q, s.sortMode, s.favoritesFirst),
            loaded = true,
            pinned = all.filter { it.isPinned },
            favorites = NoteFilters.sort(all.filter { it.isFavorite && !it.isPinned }, SortMode.UPDATED, false),
            categoryCounts = all.mapNotNull { it.categoryId }.groupingBy { it }.eachCount(),
            archivedCount = archived.size,
        )
    }.flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HomeUiState())

    private val _events = Channel<UiEvent>(Channel.BUFFERED)
    val events: Flow<UiEvent> = _events.receiveAsFlow()

    init {
        viewModelScope.launch(Dispatchers.IO) {
            seedFirstRun()
            val s = settingsStore.settingsFlow.first()
            repo.purgeTrash(s.trashRetentionDays)
        }
    }

    /**
     * Beispielnotizen nur bei einer echten Erstinstallation (vorher wurden sie bei jeder leeren DB
     * erneut angelegt – auch nachdem der Nutzer alles gelöscht hatte).
     */
    private suspend fun seedCategoriesIfNeeded() {
        val ctx = getApplication<Application>()
        if (!settingsStore.isCategoriesSeeded()) {
            repo.seedCategories(
                listOf(
                    R.string.category_private to "heart", R.string.category_work to "work",
                    R.string.category_school to "school", R.string.category_ideas to "idea",
                    R.string.category_shopping to "shopping", R.string.category_travel to "travel",
                    R.string.category_tasks to "tasks", R.string.category_other to "folder",
                ).map { (res, icon) -> ctx.getString(res) to icon }
            )
            settingsStore.markCategoriesSeeded()
        }
    }

    /** Ordnung: zuerst Kategorien anlegen, dann Beispielnotizen – damit diese zugeordnet werden können. */
    private suspend fun seedFirstRun() {
        seedCategoriesIfNeeded()
        val ctx = getApplication<Application>()
        val samplesKey = booleanPreferencesKey("sample_notes_seeded")
        val prefs = ctx.dataStore.data.first()
        if (prefs[samplesKey] != true) {
            if (repo.noteCount() == 0) addSampleNotes()
            ctx.dataStore.edit { it[samplesKey] = true }
        }
    }

    private suspend fun addSampleNotes() {
        val ctx = getApplication<Application>()
        val now = System.currentTimeMillis()
        val day = 86_400_000L
        val cats = repo.getCategories()
        fun cat(res: Int) = cats.firstOrNull { it.name == ctx.getString(res) }?.id
        val samples = listOf(
            Note(title = ctx.getString(R.string.sample_welcome_title), content = ctx.getString(R.string.sample_welcome_body),
                createdAt = now - 3 * day, updatedAt = now - 3 * day, isPinned = true),
            Note(title = ctx.getString(R.string.sample_shopping_title), content = "", checklist = ctx.getString(R.string.sample_shopping_body),
                createdAt = now - 2 * day, updatedAt = now - 2 * day, noteType = Note.TYPE_CHECKLIST,
                categoryId = cat(R.string.category_shopping)),
            Note(title = ctx.getString(R.string.sample_travel_title), checklist = ctx.getString(R.string.sample_travel_list),
                content = ctx.getString(R.string.sample_travel_body), createdAt = now - day, updatedAt = now - day,
                noteType = Note.TYPE_CHECKLIST, categoryId = cat(R.string.category_travel), isFavorite = true),
            Note(title = ctx.getString(R.string.sample_ideas_title), content = ctx.getString(R.string.sample_ideas_body),
                createdAt = now, updatedAt = now, categoryId = cat(R.string.category_ideas)),
        )
        samples.forEach { repo.insertNote(it) }
    }

    // ---------------------------------------------------------------- Suche, Filter, Sortierung

    fun setTab(tab: HomeTab) { _tab.value = tab }
    fun setSearch(text: String) = _query.update { it.copy(search = text) }
    fun setQuickFilter(filter: QuickFilter) = _query.update { it.copy(quickFilter = filter, categoryId = null) }
    fun setCategoryFilter(categoryId: Int?) = _query.update { it.copy(categoryId = categoryId, quickFilter = QuickFilter.ALL) }
    fun setDateFilter(filter: DateFilter) = _query.update { it.copy(dateFilter = filter) }
    fun setOnlyWithContent(value: Boolean) = _query.update { it.copy(onlyWithContent = value) }
    fun resetFilters() = _query.update { NoteQuery(search = it.search) }
    fun setSortMode(mode: SortMode) = viewModelScope.launch { settingsStore.setSortMode(mode) }
    fun setFavoritesFirst(value: Boolean) = viewModelScope.launch { settingsStore.setFavoritesFirst(value) }
    fun setHomeLayout(value: dev.flondy.mynotes.settings.HomeLayout) = viewModelScope.launch { settingsStore.setHomeLayout(value) }

    // ---------------------------------------------------------------- Notiz-Aktionen (bestehende API)

    fun addNote(title: String, content: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val now = System.currentTimeMillis()
            repo.insertNote(Note(title = title.trim(), content = content.trim(), createdAt = now, updatedAt = now))
        }
    }

    /** Verschiebt in den Papierkorb (wiederherstellbar) statt endgültig zu löschen. */
    fun removeNote(note: Note) = moveToTrash(note.id)

    fun updateNote(note: Note) {
        viewModelScope.launch(Dispatchers.IO) { repo.updateNote(note) }
    }

    /** Kompatibilität: verschiebt innerhalb der gesamten manuellen Reihenfolge. */
    fun moveNote(fromIndexInDisplayedList: Int, toIndexInDisplayedList: Int) {
        val current = notes.value.map { it.id }.toMutableList()
        if (fromIndexInDisplayedList !in current.indices || toIndexInDisplayedList !in current.indices) return
        if (fromIndexInDisplayedList == toIndexInDisplayedList) return
        current.add(toIndexInDisplayedList, current.removeAt(fromIndexInDisplayedList))
        reorder(current)
    }

    /** Speichert die neue Reihenfolge der angezeigten Notizen dauerhaft in Room. */
    fun reorder(newOrderIds: List<Int>) {
        viewModelScope.launch(Dispatchers.IO) { repo.reorder(newOrderIds) }
    }

    /** Barrierefreie Alternative zu Drag & Drop (TalkBack-Aktionen „nach oben/unten“). */
    fun moveByOne(noteId: Int, up: Boolean) {
        val visible = homeState.value.notes
        val i = visible.indexOfFirst { it.id == noteId }
        val j = if (up) i - 1 else i + 1
        if (i < 0 || j !in visible.indices || visible[i].isPinned != visible[j].isPinned) return
        val ids = visible.map { it.id }.toMutableList()
        ids.add(j, ids.removeAt(i))
        reorder(ids)
    }

    fun setArchived(note: Note, archived: Boolean) = viewModelScope.launch(Dispatchers.IO) {
        repo.setArchived(note.id, archived)
        if (archived) _events.send(UiEvent.Archived(note.id))
    }

    fun unarchive(noteId: Int) = viewModelScope.launch(Dispatchers.IO) { repo.setArchived(noteId, false) }

    fun duplicate(noteId: Int) = viewModelScope.launch(Dispatchers.IO) {
        val copy = repo.duplicate(noteId, getApplication<Application>().getString(R.string.copy_suffix))
        if (copy != null) _events.send(UiEvent.Duplicated(copy))
    }

    fun togglePinned(note: Note) = viewModelScope.launch(Dispatchers.IO) { repo.setPinned(note.id, !note.isPinned) }
    fun toggleFavorite(note: Note) = viewModelScope.launch(Dispatchers.IO) { repo.setFavorite(note.id, !note.isFavorite) }
    fun setColor(note: Note, color: Int) = viewModelScope.launch(Dispatchers.IO) { repo.setColor(note.id, color) }
    fun setCategory(note: Note, categoryId: Int?) = viewModelScope.launch(Dispatchers.IO) { repo.setCategory(note.id, categoryId) }

    fun moveToTrash(noteId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            repo.moveToTrash(noteId)
            _events.send(UiEvent.MovedToTrash(noteId))
        }
    }

    fun restoreFromTrash(noteId: Int) = viewModelScope.launch(Dispatchers.IO) { repo.restore(noteId) }
    fun deleteForever(noteId: Int) = viewModelScope.launch(Dispatchers.IO) { repo.deleteForever(noteId) }
    fun emptyTrash() = viewModelScope.launch(Dispatchers.IO) {
        val n = repo.emptyTrash()
        _events.send(UiEvent.Message(R.string.trash_emptied, n))
    }

    /** Bestehende API: verschiebt jetzt alle Notizen in den Papierkorb (wiederherstellbar). */
    fun deleteAllNotes() = viewModelScope.launch(Dispatchers.IO) {
        repo.moveAllToTrash()
        _events.send(UiEvent.Message(R.string.all_moved_to_trash))
    }

    // ---------------------------------------------------------------- Kategorien

    fun addCategory(name: String, icon: String = "", color: Int = 0) = viewModelScope.launch(Dispatchers.IO) {
        if (name.isNotBlank()) repo.addCategory(name, icon, color)
    }

    fun updateCategory(category: Category) = viewModelScope.launch(Dispatchers.IO) {
        if (category.name.isNotBlank()) repo.updateCategory(category)
    }

    fun renameCategory(category: Category, name: String) = viewModelScope.launch(Dispatchers.IO) {
        if (name.isNotBlank()) repo.renameCategory(category, name)
    }

    fun deleteCategory(category: Category) = viewModelScope.launch(Dispatchers.IO) {
        repo.deleteCategory(category.id)
        if (_query.value.categoryId == category.id) setCategoryFilter(null)
    }

    // ---------------------------------------------------------------- Teilen / Export / Backup

    fun shareText(note: Note): String {
        val list = if (note.checklist.isNotBlank()) ChecklistCodec.toShareText(note.checklist) else ""
        val text = dev.flondy.mynotes.data.RichText.toPlainText(note.content)
        return listOf(note.title, list, text).filter { it.isNotBlank() }.joinToString("\n\n")
    }

    fun exportNoteAsTxt(note: Note, uri: Uri) = writeToUri(uri, shareText(note), R.string.export_txt_done)

    fun exportBackup(uri: Uri) = viewModelScope.launch(Dispatchers.IO) {
        val json = repo.exportBackupJson()
        writeToUri(uri, json, R.string.backup_export_done)
    }

    private fun writeToUri(uri: Uri, text: String, successRes: Int) = viewModelScope.launch(Dispatchers.IO) {
        val ok = runCatching {
            getApplication<Application>().contentResolver.openOutputStream(uri, "wt")?.use {
                it.write(text.toByteArray(Charsets.UTF_8))
            } ?: error("no stream")
        }.isSuccess
        _events.send(UiEvent.Message(if (ok) successRes else R.string.error_file_write))
    }

    fun previewImport(uri: Uri) = viewModelScope.launch(Dispatchers.IO) {
        val result = runCatching {
            val text = getApplication<Application>().contentResolver.openInputStream(uri)?.use {
                // Schutz vor versehentlich gewählten Riesendateien
                val bytes = it.readBytes()
                require(bytes.size < 50 * 1024 * 1024)
                String(bytes, Charsets.UTF_8)
            } ?: error("no stream")
            repo.previewImport(text)
        }
        result.onSuccess { _events.send(UiEvent.ImportPreviewReady(it)) }
        result.onFailure { e ->
            val msg = when ((e as? BackupFormat.InvalidBackupException)?.message) {
                "too_new" -> R.string.backup_error_too_new
                else -> R.string.backup_error_invalid
            }
            _events.send(UiEvent.Message(msg))
        }
    }

    fun confirmImport(preview: NoteRepository.ImportPreview) = viewModelScope.launch(Dispatchers.IO) {
        val count = runCatching { repo.importBackup(preview.data) }.getOrElse {
            _events.send(UiEvent.Message(R.string.backup_error_invalid)); return@launch
        }
        _events.send(UiEvent.Message(R.string.backup_import_done, count))
    }

    // ---------------------------------------------------------------- Editor-Sitzung

    var editor by mutableStateOf<EditorState?>(null)
        private set

    /** Eine Editor-Sitzung: merkt sich die (ggf. erst beim Speichern vergebene) ID und das Original. */
    private class EditorSession(var noteId: Int?, val original: Note?)

    private var session: EditorSession? = null
    private var saveJob: Job? = null
    private val saveMutex = Mutex()

    fun openEditor(note: Note) {
        flushPendingSave()
        session = EditorSession(note.id, note)
        editor = EditorState(
            noteId = note.id, type = note.noteType, title = note.title,
            content = note.content,
            items = ChecklistCodec.decode(note.checklist),
            showChecklist = note.isChecklist, isArchived = note.isArchived,
            isPinned = note.isPinned, isFavorite = note.isFavorite, color = note.color,
            categoryId = note.categoryId, createdAt = note.createdAt, updatedAt = note.updatedAt,
        )
    }

    fun openNewNote(type: Int = Note.TYPE_TEXT) {
        flushPendingSave()
        session = EditorSession(null, null)
        val q = _query.value
        editor = EditorState(
            noteId = null, type = type, title = "", content = "",
            items = if (type == Note.TYPE_CHECKLIST) listOf(ChecklistItem(ChecklistCodec.newId(), "", false)) else emptyList(),
            isPinned = q.quickFilter == QuickFilter.PINNED,
            isFavorite = q.quickFilter == QuickFilter.FAVORITES,
            color = 0, categoryId = q.categoryId, createdAt = null, updatedAt = null,
            focusTitle = true,
            showChecklist = type == Note.TYPE_CHECKLIST,
        )
    }

    /** Neue Notiz aus einer Vorlage (2.4): Editor öffnet vorausgefüllt, gespeichert wird wie bei jeder Eingabe. */
    fun openTemplate(t: dev.flondy.mynotes.data.NoteTemplate) {
        openNewNote(if (t.isChecklist) Note.TYPE_CHECKLIST else Note.TYPE_TEXT)
        edit {
            it.copy(
                title = t.title, content = t.text, focusTitle = false,
                items = if (t.isChecklist) t.items.map { text -> ChecklistItem(ChecklistCodec.newId(), text, false) } else it.items,
            )
        }
    }

    private fun edit(transform: (EditorState) -> EditorState) {
        val current = editor ?: return
        editor = transform(current).copy(saveStatus = SaveStatus.PENDING)
        scheduleSave()
    }

    fun onEditorTitleChange(value: String) = edit { it.copy(title = value) }
    fun onEditorContentChange(value: String) = edit { it.copy(content = value) }
    fun onEditorItemsChange(items: List<ChecklistItem>) = edit { it.copy(items = items) }
    fun onEditorTogglePinned() = edit { it.copy(isPinned = !it.isPinned) }
    fun onEditorToggleFavorite() = edit { it.copy(isFavorite = !it.isFavorite) }
    fun onEditorColor(color: Int) = edit { it.copy(color = color) }
    fun onEditorCategory(categoryId: Int?) = edit { it.copy(categoryId = categoryId) }

    fun onEditorToggleArchived() = edit { it.copy(isArchived = !it.isArchived) }

    /** Checkliste hinzufügen (leer) bzw. entfernen – vorhandene Einträge wandern dabei als Aufzählung in den Text. */
    fun onEditorToggleChecklist() = edit {
        if (it.showChecklist) {
            val asText = it.items.filter { i -> i.text.isNotBlank() }.joinToString("\n") { i -> dev.flondy.mynotes.data.RichText.BULLET + i.text }
            val content = listOf(asText, it.content).filter { t -> t.isNotBlank() }.joinToString("\n")
            it.copy(type = Note.TYPE_TEXT, showChecklist = false, items = emptyList(), content = content)
        } else {
            it.copy(type = Note.TYPE_CHECKLIST, showChecklist = true,
                items = it.items.ifEmpty { listOf(ChecklistItem(ChecklistCodec.newId(), "", false)) })
        }
    }

    /** Text → Checkliste: jede Zeile des Notiztexts wird ein Eintrag. */
    fun onEditorTextToChecklist() = edit {
        val newItems = ChecklistCodec.fromPlainText(it.content.lines().joinToString("\n") { l -> l.removePrefix(dev.flondy.mynotes.data.RichText.BULLET) })
        if (newItems.isEmpty()) it else it.copy(type = Note.TYPE_CHECKLIST, showChecklist = true,
            items = it.items.filter { i -> i.text.isNotBlank() } + newItems, content = "")
    }

    /** Kompatibilität: Text ↔ Checkliste umschalten. */
    fun onEditorToggleType() = if (editor?.showChecklist == true) onEditorToggleChecklist() else onEditorTextToChecklist()

    /** Automatisches Zwischenspeichern (Debounce), damit beim Tippen nichts verloren geht. */
    private fun scheduleSave() {
        saveJob?.cancel()
        val s = session ?: return
        saveJob = viewModelScope.launch {
            delay(AUTOSAVE_DELAY_MS)
            editor?.let { persist(it, s) }
        }
    }

    /** Speichert ausstehende Änderungen sofort – unabhängig davon, ob der Editor danach geschlossen wird. */
    private fun flushPendingSave() {
        val snapshot = editor ?: return
        val s = session ?: return
        saveJob?.cancel()
        if (snapshot.saveStatus != SaveStatus.PENDING) return
        viewModelScope.launch { withContext(NonCancellable) { persist(snapshot, s) } }
    }

    private suspend fun persist(snapshot: EditorState, s: EditorSession) = saveMutex.withLock {
        withContext(Dispatchers.IO) {
            val id = s.noteId
            val now = System.currentTimeMillis()
            if (id == null) {
                if (snapshot.isBlank) return@withContext
                val newId = repo.insertNote(
                    Note(
                        title = snapshot.title.trim(), content = snapshot.content.trimEnd(),
                        checklist = snapshot.encodedChecklist, isArchived = snapshot.isArchived,
                        createdAt = now, updatedAt = now, isPinned = snapshot.isPinned,
                        isFavorite = snapshot.isFavorite, color = snapshot.color,
                        categoryId = snapshot.categoryId, noteType = snapshot.type,
                    )
                )
                s.noteId = newId
                markSaved(s, snapshot, newId, now, now)
            } else {
                val existing = repo.getNote(id) ?: return@withContext
                val newTitle = snapshot.title.trim()
                val newContent = snapshot.content.trimEnd()
                val newList = snapshot.encodedChecklist
                val textChanged = existing.title != newTitle || existing.content != newContent ||
                    existing.checklist != newList || existing.noteType != snapshot.type
                val updated = existing.copy(
                    title = newTitle, content = newContent, checklist = newList, noteType = snapshot.type,
                    isArchived = snapshot.isArchived,
                    isPinned = snapshot.isPinned, isFavorite = snapshot.isFavorite,
                    color = snapshot.color, categoryId = snapshot.categoryId,
                    // Änderungsdatum nur bei echten Inhaltsänderungen – Erstellungsdatum bleibt immer erhalten
                    updatedAt = if (textChanged) now else existing.updatedAt,
                )
                if (updated != existing) updateNoteNow(updated)
                markSaved(s, snapshot, id, existing.createdAt, updated.updatedAt)
            }
        }
    }

    private suspend fun updateNoteNow(note: Note) = repo.updateNote(note)

    private suspend fun markSaved(s: EditorSession, snapshot: EditorState, id: Int, createdAt: Long, updatedAt: Long) =
        withContext(Dispatchers.Main) {
            val e = editor ?: return@withContext
            if (session !== s) return@withContext
            // Wurde seit dem Snapshot weitergetippt, bleibt der Status „ausstehend“.
            val unchanged = e.title == snapshot.title && e.content == snapshot.content &&
                e.encodedChecklist == snapshot.encodedChecklist && e.isArchived == snapshot.isArchived &&
                e.type == snapshot.type && e.isPinned == snapshot.isPinned && e.isFavorite == snapshot.isFavorite &&
                e.color == snapshot.color && e.categoryId == snapshot.categoryId
            editor = e.copy(
                noteId = id, createdAt = createdAt, updatedAt = updatedAt,
                saveStatus = if (unchanged) SaveStatus.SAVED else SaveStatus.PENDING,
            )
        }

    /** Editor schließen und ausstehende Änderungen sofort speichern. */
    fun closeEditor() {
        flushPendingSave()
        editor = null
        session = null
    }

    /** Speichert und legt eine Kopie der aktuellen Notiz an (Editor bleibt geöffnet). */
    fun duplicateFromEditor() {
        val snapshot = editor ?: return
        val s = session ?: return
        saveJob?.cancel()
        viewModelScope.launch {
            withContext(NonCancellable) {
                persist(snapshot, s)
                val id = s.noteId ?: return@withContext
                val copy = withContext(Dispatchers.IO) {
                    repo.duplicate(id, getApplication<Application>().getString(R.string.copy_suffix))
                }
                if (copy != null) _events.send(UiEvent.Duplicated(copy))
            }
        }
    }

    /** Alle Änderungen dieser Sitzung verwerfen (Originalzustand wiederherstellen). */
    fun discardEditorChanges() {
        saveJob?.cancel()
        val s = session
        editor = null
        session = null
        if (s == null) return
        viewModelScope.launch(Dispatchers.IO) {
            withContext(NonCancellable) {
                saveMutex.withLock {
                    val id = s.noteId
                    val original = s.original
                    when {
                        original != null -> repo.updateNote(original)
                        id != null -> repo.deleteDraft(id)
                    }
                }
            }
        }
    }

    fun deleteFromEditor() {
        saveJob?.cancel()
        val s = session
        editor = null
        session = null
        if (s == null) return
        viewModelScope.launch(Dispatchers.IO) {
            withContext(NonCancellable) {
                saveMutex.withLock {
                    val id = s.noteId ?: return@withLock
                    repo.moveToTrash(id)
                    _events.send(UiEvent.MovedToTrash(id))
                }
            }
        }
    }

    /** Aktueller Editor-Inhalt als Note (z. B. zum Teilen oder TXT-Export). */
    fun editorAsNote(): Note? = editor?.let {
        Note(id = session?.noteId ?: 0, title = it.title, content = it.content, checklist = it.encodedChecklist, noteType = it.type)
    }

    override fun onCleared() {
        // Ausstehende Änderungen nicht verlieren, falls die Activity endgültig beendet wird
        val snapshot = editor
        val s = session
        if (snapshot != null && s != null && snapshot.saveStatus == SaveStatus.PENDING) {
            app.appScope.launch { persist(snapshot, s) }
        }
        super.onCleared()
    }

    companion object {
        const val AUTOSAVE_DELAY_MS = 600L
    }
}
