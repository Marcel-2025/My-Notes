package dev.flondy.mynotes.data

import androidx.room.withTransaction
import dev.flondy.mynotes.AppDatabase
import dev.flondy.mynotes.Category
import dev.flondy.mynotes.Note
import kotlinx.coroutines.flow.Flow

/** Einziger Zugriffspunkt der UI-Schicht auf Notizen und Kategorien. */
class NoteRepository(private val db: AppDatabase) {
    private val noteDao = db.noteDao()
    private val categoryDao = db.categoryDao()

    val activeNotes: Flow<List<Note>> = noteDao.observeActiveNotes()
    val trash: Flow<List<Note>> = noteDao.observeTrash()
    val archived: Flow<List<Note>> = noteDao.observeArchived()
    val categories: Flow<List<Category>> = categoryDao.observeAll()

    suspend fun getNote(id: Int): Note? = noteDao.getNoteById(id)

    /** Neue Notizen erscheinen in der manuellen Reihenfolge ganz oben. */
    suspend fun insertNote(note: Note): Int = db.withTransaction {
        val top = noteDao.getMinDisplayOrder()
        noteDao.insert(note.copy(id = 0, displayOrder = (top ?: 1) - 1)).toInt()
    }

    suspend fun updateNote(note: Note) = noteDao.update(note)

    suspend fun reorder(newSubsetOrder: List<Int>) = db.withTransaction {
        val global = noteDao.getActiveNotes()
        val changes = NoteFilters.computeReorder(global, newSubsetOrder)
        if (changes.isNotEmpty()) noteDao.applyDisplayOrder(changes)
    }

    suspend fun setPinned(id: Int, pinned: Boolean) = noteDao.setPinned(id, pinned)
    suspend fun setFavorite(id: Int, favorite: Boolean) = noteDao.setFavorite(id, favorite)
    suspend fun setColor(id: Int, color: Int) = noteDao.setColor(id, color)
    suspend fun setCategory(id: Int, categoryId: Int?) = noteDao.setCategory(id, categoryId)

    suspend fun setArchived(id: Int, archived: Boolean) = noteDao.setArchived(id, archived)

    /** Legt eine Kopie oben in der Liste an (eigene ID, neues Datum, nicht angeheftet). */
    suspend fun duplicate(id: Int, copySuffix: String, now: Long = System.currentTimeMillis()): Int? {
        val original = noteDao.getNoteById(id) ?: return null
        val title = if (original.title.isBlank()) original.title else "${original.title} $copySuffix"
        return insertNote(original.copy(id = 0, title = title, createdAt = now, updatedAt = now, isPinned = false, isArchived = false, deletedAt = null))
    }

    suspend fun moveToTrash(id: Int, now: Long = System.currentTimeMillis()) = noteDao.moveToTrash(id, now)
    suspend fun moveAllToTrash(now: Long = System.currentTimeMillis()) = noteDao.moveAllToTrash(now)
    suspend fun restore(id: Int) = noteDao.restoreFromTrash(id)
    suspend fun deleteForever(id: Int) = noteDao.deleteFromTrash(id)
    suspend fun emptyTrash(): Int = noteDao.emptyTrash()
    suspend fun purgeTrash(retentionDays: Int, now: Long = System.currentTimeMillis()): Int =
        noteDao.purgeTrashOlderThan(now - retentionDays * 24L * 60 * 60 * 1000)

    /** Nur für ungespeicherte, leere neue Notizen beim Verwerfen im Editor. */
    suspend fun deleteDraft(id: Int) = noteDao.deleteById(id)

    suspend fun noteCount(): Int = noteDao.getNoteCount()

    // --- Kategorien ---
    suspend fun addCategory(name: String, icon: String = "", color: Int = 0): Int {
        val pos = (categoryDao.maxPosition() ?: -1) + 1
        return categoryDao.insert(Category(name = name.trim(), position = pos, icon = icon, color = color)).toInt()
    }

    suspend fun renameCategory(category: Category, name: String) = categoryDao.update(category.copy(name = name.trim()))
    suspend fun updateCategory(category: Category) = categoryDao.update(category.copy(name = category.name.trim()))
    suspend fun deleteCategory(id: Int) = db.deleteCategorySafely(id)
    suspend fun categoryCount(): Int = categoryDao.count()

    /** Legt Standardkategorien an (nur wenn noch keine existieren). Paare aus Name und Icon-Schlüssel. */
    suspend fun seedCategories(entries: List<Pair<String, String>>) = db.withTransaction {
        if (categoryDao.count() == 0) {
            entries.forEachIndexed { i, (name, icon) -> categoryDao.insert(Category(name = name, position = i, icon = icon)) }
        }
    }

    suspend fun getCategories(): List<Category> = categoryDao.getAll()

    // --- Backup ---
    suspend fun exportBackupJson(): String =
        BackupFormat.encode(noteDao.getAllIncludingTrash(), categoryDao.getAll())

    data class ImportPreview(
        val data: BackupFormat.BackupData,
        val newNotes: Int,
        val duplicates: Int,
        val newCategories: Int,
    )

    suspend fun previewImport(json: String): ImportPreview {
        val data = BackupFormat.decode(json)
        val existing = noteDao.getAllIncludingTrash().map(BackupFormat::fingerprint).toSet()
        val dup = data.notes.count { BackupFormat.fingerprint(it) in existing }
        val existingCats = categoryDao.getAll().map { it.name.lowercase() }.toSet()
        val newCats = data.categories.count { it.name.lowercase() !in existingCats }
        return ImportPreview(data, data.notes.size - dup, dup, newCats)
    }

    /** Importiert ein Backup additiv: keine bestehende Notiz wird verändert, Duplikate werden übersprungen. */
    suspend fun importBackup(data: BackupFormat.BackupData): Int = db.withTransaction {
        val existingCats = categoryDao.getAll()
        val catIdMap = mutableMapOf<Int, Int>()
        var pos = (categoryDao.maxPosition() ?: -1) + 1
        data.categories.forEach { c ->
            val match = existingCats.firstOrNull { it.name.equals(c.name, ignoreCase = true) }
            catIdMap[c.id] = match?.id ?: categoryDao.insert(Category(name = c.name, position = pos++, icon = c.icon, color = c.color)).toInt()
        }
        val existing = noteDao.getAllIncludingTrash().map(BackupFormat::fingerprint).toMutableSet()
        var order = (noteDao.getMaxDisplayOrder() ?: -1) + 1
        val toInsert = data.notes
            .sortedBy { it.displayOrder }
            .filter { existing.add(BackupFormat.fingerprint(it)) }
            .map { it.copy(id = 0, displayOrder = order++, categoryId = it.categoryId?.let(catIdMap::get)) }
        noteDao.insertAll(toInsert)
        toInsert.size
    }
}
