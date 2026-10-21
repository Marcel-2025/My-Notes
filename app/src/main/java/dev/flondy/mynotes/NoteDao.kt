package dev.flondy.mynotes

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {

    @Insert
    suspend fun insert(note: Note): Long

    @Insert
    suspend fun insertAll(notes: List<Note>): List<Long>

    @Update
    suspend fun update(note: Note)

    @Update
    suspend fun updateNotes(notes: List<Note>)

    @Delete
    suspend fun delete(note: Note)

    /** Aktive (nicht archivierte, nicht gelöschte) Notizen in manueller Reihenfolge (angeheftete zuerst). */
    @Query("SELECT * FROM notes_table WHERE deletedAt IS NULL AND isArchived = 0 ORDER BY isPinned DESC, displayOrder ASC, timestamp DESC")
    fun observeActiveNotes(): Flow<List<Note>>

    @Query("SELECT * FROM notes_table WHERE deletedAt IS NULL AND isArchived = 0 ORDER BY isPinned DESC, displayOrder ASC, timestamp DESC")
    suspend fun getActiveNotes(): List<Note>

    @Query("SELECT * FROM notes_table WHERE deletedAt IS NULL AND isArchived = 1 ORDER BY timestamp DESC")
    fun observeArchived(): Flow<List<Note>>

    @Query("UPDATE notes_table SET isArchived = :archived WHERE id = :id")
    suspend fun setArchived(id: Int, archived: Boolean)

    @Query("SELECT * FROM notes_table WHERE deletedAt IS NOT NULL ORDER BY deletedAt DESC")
    fun observeTrash(): Flow<List<Note>>

    @Query("SELECT * FROM notes_table ORDER BY displayOrder ASC")
    suspend fun getAllIncludingTrash(): List<Note>

    @Query("SELECT * FROM notes_table WHERE id = :noteId")
    suspend fun getNoteById(noteId: Int): Note?

    @Query("SELECT * FROM notes_table WHERE id = :noteId")
    fun observeNoteById(noteId: Int): Flow<Note?>

    @Query("SELECT MIN(displayOrder) FROM notes_table WHERE deletedAt IS NULL")
    suspend fun getMinDisplayOrder(): Int?

    @Query("SELECT MAX(displayOrder) FROM notes_table")
    suspend fun getMaxDisplayOrder(): Int?

    @Query("SELECT COUNT(id) FROM notes_table")
    suspend fun getNoteCount(): Int

    @Query("UPDATE notes_table SET displayOrder = :order WHERE id = :id")
    suspend fun setDisplayOrder(id: Int, order: Int)

    /** Speichert nur die Reihenfolge – andere Felder (z. B. parallel bearbeiteter Text) bleiben unberührt. */
    @Transaction
    suspend fun applyDisplayOrder(orderById: Map<Int, Int>) {
        orderById.forEach { (id, order) -> setDisplayOrder(id, order) }
    }

    @Query("UPDATE notes_table SET isPinned = :pinned WHERE id = :id")
    suspend fun setPinned(id: Int, pinned: Boolean)

    @Query("UPDATE notes_table SET isFavorite = :favorite WHERE id = :id")
    suspend fun setFavorite(id: Int, favorite: Boolean)

    @Query("UPDATE notes_table SET color = :color WHERE id = :id")
    suspend fun setColor(id: Int, color: Int)

    @Query("UPDATE notes_table SET categoryId = :categoryId WHERE id = :id")
    suspend fun setCategory(id: Int, categoryId: Int?)

    @Query("UPDATE notes_table SET deletedAt = :time WHERE id = :id")
    suspend fun moveToTrash(id: Int, time: Long)

    @Query("UPDATE notes_table SET deletedAt = :time WHERE deletedAt IS NULL AND isArchived = 0")
    suspend fun moveAllToTrash(time: Long)

    @Query("UPDATE notes_table SET deletedAt = NULL WHERE id = :id")
    suspend fun restoreFromTrash(id: Int)

    @Query("DELETE FROM notes_table WHERE id = :id AND deletedAt IS NOT NULL")
    suspend fun deleteFromTrash(id: Int)

    @Query("DELETE FROM notes_table WHERE deletedAt IS NOT NULL")
    suspend fun emptyTrash(): Int

    @Query("DELETE FROM notes_table WHERE deletedAt IS NOT NULL AND deletedAt < :cutoff")
    suspend fun purgeTrashOlderThan(cutoff: Long): Int

    @Query("UPDATE notes_table SET categoryId = NULL WHERE categoryId = :categoryId")
    suspend fun clearCategory(categoryId: Int)

    @Query("DELETE FROM notes_table WHERE id = :id")
    suspend fun deleteById(id: Int)
}
