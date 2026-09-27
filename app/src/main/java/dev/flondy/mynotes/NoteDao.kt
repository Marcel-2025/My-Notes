package dev.flondy.mynotes // Dein Paketname

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {

    // Wichtig: onConflict hier auf REPLACE setzen, falls wir beim Sortieren IDs mitgeben
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(note: Note)

    @Update
    suspend fun update(note: Note)

    // NEU: Eine Liste von Notizen aktualisieren (nützlich für das Speichern der neuen Reihenfolge)
    @Update
    suspend fun updateNotes(notes: List<Note>)

    @Delete
    suspend fun delete(note: Note)

    // GEÄNDERT: Sortierung nach displayOrder ASC (kleinste zuerst oben)
    @Query("SELECT * FROM notes_table ORDER BY displayOrder ASC, timestamp DESC") // Sekundäre Sortierung nach Zeitstempel
    fun getAllNotes(): Flow<List<Note>>

    @Query("SELECT * FROM notes_table WHERE id = :noteId")
    fun getNoteById(noteId: Int): Flow<Note?>

    @Query("DELETE FROM notes_table")
    suspend fun deleteAllNotes()

    // NEU: Die höchste displayOrder bekommen, um neuen Notizen eine Position zu geben
    @Query("SELECT MAX(displayOrder) FROM notes_table")
    suspend fun getMaxDisplayOrder(): Int?

    @Query("SELECT COUNT(id) FROM notes_table") // "notes_table" ist dein Tabellenname
    suspend fun getNoteCount(): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE) // Oder eine andere Strategie
    suspend fun insertAll(notes: List<Note>)
}
