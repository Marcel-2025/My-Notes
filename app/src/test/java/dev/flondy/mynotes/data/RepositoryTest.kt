package dev.flondy.mynotes.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.flondy.mynotes.AppDatabase
import dev.flondy.mynotes.Note
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34], application = android.app.Application::class)
class RepositoryTest {
    private lateinit var db: AppDatabase
    private lateinit var repo: NoteRepository

    @Before fun setUp() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java)
            .allowMainThreadQueries().build()
        repo = NoteRepository(db)
    }

    @After fun tearDown() = db.close()

    private suspend fun add(title: String, content: String = "") =
        repo.insertNote(Note(title = title, content = content, createdAt = 1000, updatedAt = 1000))

    private suspend fun activeIds() = repo.activeNotes.first().map { it.title }

    @Test fun createEditKeepsIdOrderAndCreatedAt() = runBlocking {
        val a = add("A"); add("B")
        assertEquals(listOf("B", "A"), activeIds()) // neue Notizen oben
        val note = repo.getNote(a)!!
        repo.updateNote(note.copy(title = "A2", content = "neu", updatedAt = 5000))
        val edited = repo.getNote(a)!!
        assertEquals(a, edited.id)
        assertEquals(1000, edited.createdAt)
        assertEquals(note.displayOrder, edited.displayOrder)
        assertEquals(listOf("B", "A2"), activeIds())
    }

    @Test fun dragReorderIsPersisted() = runBlocking {
        val ids = (1..5).map { add("N$it") }.reversed() // Anzeige: N1..N5? neue oben → N5 oben
        val shown = repo.activeNotes.first().map { it.id }
        assertEquals(ids, shown)
        // Oberstes Element ans Ende ziehen
        val moved = shown.drop(1) + shown.first()
        repo.reorder(moved)
        assertEquals(moved, repo.activeNotes.first().map { it.id })
        // „Neustart“: neue Repository-Instanz liest dieselbe Reihenfolge
        assertEquals(moved, NoteRepository(db).activeNotes.first().map { it.id })
    }

    @Test fun trashRestoreAndPurge() = runBlocking {
        val a = add("A"); val b = add("B")
        repo.moveToTrash(a, now = 10_000)
        assertEquals(listOf("B"), activeIds())
        assertEquals(listOf(a), repo.trash.first().map { it.id })
        repo.restore(a)
        assertEquals(2, repo.activeNotes.first().size)
        repo.moveToTrash(b, now = 0)
        // Aufbewahrung 30 Tage: Notiz von Zeitpunkt 0 ist nach 31 Tagen weg
        assertEquals(1, repo.purgeTrash(30, now = 31L * 86_400_000))
        assertNull(repo.getNote(b))
        assertEquals(listOf(a), repo.activeNotes.first().map { it.id })
    }

    @Test fun deleteForeverOnlyWorksInTrash() = runBlocking {
        val a = add("A")
        repo.deleteForever(a)
        assertEquals("aktive Notiz darf nicht endgültig gelöscht werden", 1, repo.activeNotes.first().size)
    }

    @Test fun deletingCategoryKeepsNotes() = runBlocking {
        val cat = repo.addCategory("Arbeit")
        val a = add("A")
        repo.setCategory(a, cat)
        repo.deleteCategory(cat)
        val note = repo.getNote(a)!!
        assertNull(note.categoryId)
        assertTrue(repo.categories.first().isEmpty())
    }

    @Test fun backupRoundTrip_skipsDuplicates_andMapsCategories() = runBlocking {
        val cat = repo.addCategory("Ideen")
        val a = add("A", "Inhalt"); repo.setCategory(a, cat); repo.setFavorite(a, true)
        add("B", "[ ] x")
        val json = repo.exportBackupJson()

        // Import in dieselbe DB: alles Duplikate
        val preview = repo.previewImport(json)
        assertEquals(0, preview.newNotes); assertEquals(2, preview.duplicates)

        // Import in leere DB
        val other = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java).allowMainThreadQueries().build()
        try {
            val repo2 = NoteRepository(other)
            repo2.addCategory("ideen") // gleichnamig (Groß/Klein egal) → wird wiederverwendet
            val p2 = repo2.previewImport(json)
            assertEquals(2, p2.newNotes)
            assertEquals(2, repo2.importBackup(p2.data))
            val imported = repo2.activeNotes.first()
            val ia = imported.first { it.title == "A" }
            assertTrue(ia.isFavorite)
            assertEquals(repo2.categories.first().single().id, ia.categoryId)
            // erneuter Import erzeugt keine Duplikate
            assertEquals(0, repo2.importBackup(repo2.previewImport(json).data))
        } finally { other.close() }
    }

    @Test fun invalidBackupIsRejected() = runBlocking {
        for (bad in listOf("kein json", "{\"format\":\"andere-app\",\"version\":1,\"notes\":[]}", "{\"format\":\"mynotes-backup\",\"version\":99,\"notes\":[]}")) {
            try { repo.previewImport(bad); fail("sollte fehlschlagen: $bad") } catch (_: BackupFormat.InvalidBackupException) {}
        }
    }

    @Test fun concurrentWritesDoNotLoseNotes() = runBlocking {
        (1..50).map { i -> async { add("N$i") } }.awaitAll()
        assertEquals(50, repo.activeNotes.first().size)
        assertEquals(50, repo.activeNotes.first().map { it.displayOrder }.size)
    }
}
