package dev.flondy.mynotes // Dein Paketname

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

// Stelle sicher, dass Note und AppDatabase korrekt importiert werden,
// falls sie in anderen Paketen liegen.
// import com.example.myapplication.data.Note // Beispiel
// import com.example.myapplication.data.AppDatabase // Beispiel

class NotesViewModel(application: Application) : AndroidViewModel(application) {

    private val noteDao = AppDatabase.getDatabase(application).noteDao()

    val notes: StateFlow<List<Note>> = noteDao.getAllNotes()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    init {
        // Beim Initialisieren des ViewModels prüfen, ob Beispieldaten hinzugefügt werden sollen.
        viewModelScope.launch(Dispatchers.IO) {
            // Prüfe, ob die Datenbank leer ist (oder zumindest keine Notizen hat)
            if (noteDao.getNoteCount() == 0) { // Du brauchst eine getNoteCount() Methode im DAO
                addSampleNotes()
            }
        }
    }

    private suspend fun addSampleNotes() {
        // Füge hier deine Beispieldaten ein.
        // Achte darauf, dass die displayOrder korrekt gesetzt wird.
        val sampleNotes = listOf(
            Note(title = "Willkommen!", content = "Dies ist eine Beispielnotiz. Du kannst sie bearbeiten oder löschen.", timestamp = System.currentTimeMillis() - 86400000 * 2, displayOrder = 0),
            Note(title = "Einkaufsliste", content = "- Milch\n- Brot\n- Eier", timestamp = System.currentTimeMillis() - 86400000, displayOrder = 1),
            Note(title = "Ideen für das Wochenende", content = "Wandern gehen oder ein Buch lesen.", timestamp = System.currentTimeMillis(), displayOrder = 2)
        )
        // noteDao.insertAll(sampleNotes) // Wenn du eine insertAll Methode hast
        // Oder einzeln einfügen, falls insertAll nicht existiert oder du spezifische Logik pro Insert brauchst
        sampleNotes.forEach { noteDao.insert(it) }
    }

    fun addNote(title: String, content: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val maxOrder = noteDao.getMaxDisplayOrder() ?: -1
            val newNote = Note(
                title = title.ifBlank { "Unbenannte Notiz" },
                content = content,
                timestamp = System.currentTimeMillis(),
                displayOrder = maxOrder + 1
            )
            noteDao.insert(newNote)
        }
    }

    fun removeNote(note: Note) {
        viewModelScope.launch(Dispatchers.IO) {
            noteDao.delete(note)
            // Passe displayOrder der verbleibenden Notizen an
            val currentNotes = notes.value.filterNot { it.id == note.id }
            val updatedNotes = currentNotes.mapIndexed { index, n ->
                n.copy(displayOrder = index)
            }
            if (updatedNotes.isNotEmpty() || currentNotes.isEmpty()) { // Nur updaten, wenn nötig
                noteDao.updateNotes(updatedNotes)
            }
        }
    }

    fun updateNote(note: Note) {
        viewModelScope.launch(Dispatchers.IO) {
            noteDao.update(note)
        }
    }

    fun moveNote(fromIndexInDisplayedList: Int, toIndexInDisplayedList: Int) {
        if (fromIndexInDisplayedList == toIndexInDisplayedList) return

        viewModelScope.launch(Dispatchers.IO) {
            val currentNotesSnapshot = notes.value.toMutableList()

            if (fromIndexInDisplayedList < 0 || fromIndexInDisplayedList >= currentNotesSnapshot.size ||
                toIndexInDisplayedList < 0 || toIndexInDisplayedList >= currentNotesSnapshot.size) {
                return@launch
            }

            val movedItem = currentNotesSnapshot.removeAt(fromIndexInDisplayedList)
            currentNotesSnapshot.add(toIndexInDisplayedList, movedItem)

            val updatedNotesWithNewOrder = currentNotesSnapshot.mapIndexed { newIndex, note ->
                note.copy(displayOrder = newIndex)
            }
            noteDao.updateNotes(updatedNotesWithNewOrder)
        }
    }

    fun deleteAllNotes() {
        viewModelScope.launch(Dispatchers.IO) {
            noteDao.deleteAllNotes()
        }
    }
}
