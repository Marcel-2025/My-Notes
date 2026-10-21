package dev.flondy.mynotes

import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * Prüft, dass ein Update von Datenbankversion 3 (bisher veröffentlichte App) auf Version 4
 * keine einzige Notiz verliert und alle Inhalte, IDs und die manuelle Reihenfolge erhalten bleiben.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34], application = android.app.Application::class)
class MigrationTest {
    private val dbName = "migration-test"

    @get:Rule
    val helper = MigrationTestHelper(InstrumentationRegistry.getInstrumentation(), AppDatabase::class.java)

    private fun createV3WithSampleData() {
        helper.createDatabase(dbName, 3).apply {
            execSQL("INSERT INTO notes_table (id, title, content, timestamp, displayOrder) VALUES (1, 'Einkauf', '- Milch\n- Brot', 1700000000000, 2)")
            execSQL("INSERT INTO notes_table (id, title, content, timestamp, displayOrder) VALUES (2, '', 'Nur Inhalt ohne Titel', 1700000100000, 0)")
            execSQL("INSERT INTO notes_table (id, title, content, timestamp, displayOrder) VALUES (5, 'Ümlaute & Emoji 🎉', '', 1700000200000, 1)")
            val long = "x".repeat(100_000)
            execSQL("INSERT INTO notes_table (id, title, content, timestamp, displayOrder) VALUES (7, 'Lang', '$long', 1700000300000, 3)")
            close()
        }
    }

    @Test
    fun migrate3To4_keepsAllDataAndValidatesSchema() {
        createV3WithSampleData()
        // validateDroppedTables = true: Room prüft das Ergebnis exakt gegen das exportierte Schema v4
        val db = helper.runMigrationsAndValidate(dbName, 4, true, AppDatabase.MIGRATION_3_4)

        db.query("SELECT id, title, content, timestamp, displayOrder, createdAt, isPinned, isFavorite, color, categoryId, noteType, deletedAt FROM notes_table ORDER BY id").use { c ->
            assertEquals(4, c.count)
            c.moveToFirst()
            assertEquals(1, c.getInt(0))
            assertEquals("Einkauf", c.getString(1))
            assertEquals("- Milch\n- Brot", c.getString(2))
            assertEquals(1700000000000L, c.getLong(3))
            assertEquals(2, c.getInt(4))
            assertEquals("createdAt übernimmt den alten timestamp", 1700000000000L, c.getLong(5))
            assertEquals(0, c.getInt(6)); assertEquals(0, c.getInt(7)); assertEquals(0, c.getInt(8))
            assertTrue(c.isNull(9)); assertEquals(0, c.getInt(10)); assertTrue(c.isNull(11))
            c.moveToNext(); assertEquals(2, c.getInt(0)); assertEquals("", c.getString(1))
            c.moveToNext(); assertEquals(5, c.getInt(0)); assertEquals("Ümlaute & Emoji 🎉", c.getString(1))
            c.moveToNext(); assertEquals(7, c.getInt(0)); assertEquals(100_000, c.getString(2).length)
        }
        db.query("SELECT COUNT(*) FROM categories").use { c -> c.moveToFirst(); assertEquals(0, c.getInt(0)) }
    }

    @Test
    fun migrate4To5_movesChecklistsAndKeepsText() {
        helper.createDatabase(dbName, 4).apply {
            execSQL("INSERT INTO categories (id, name, position) VALUES (3, 'Einkaufen', 0)")
            execSQL("INSERT INTO notes_table (id, title, content, timestamp, displayOrder, createdAt, isPinned, isFavorite, color, categoryId, noteType, deletedAt) " +
                "VALUES (1, 'Liste', '[ ] Milch\n[x] Brot', 2000, 0, 1500, 1, 1, 2, 3, 1, NULL)")
            execSQL("INSERT INTO notes_table (id, title, content, timestamp, displayOrder, createdAt, isPinned, isFavorite, color, categoryId, noteType, deletedAt) " +
                "VALUES (2, 'Text', 'Nur **Text**', 3000, 1, 2500, 0, 0, 0, NULL, 0, 4000)")
            close()
        }
        val db = helper.runMigrationsAndValidate(dbName, 5, true, AppDatabase.MIGRATION_4_5)
        db.query("SELECT id, content, checklist, isArchived, isPinned, isFavorite, color, categoryId, noteType, deletedAt, createdAt FROM notes_table ORDER BY id").use { c ->
            c.moveToFirst()
            assertEquals("", c.getString(1))
            assertEquals("[ ] Milch\n[x] Brot", c.getString(2))
            assertEquals(0, c.getInt(3))
            assertEquals(1, c.getInt(4)); assertEquals(1, c.getInt(5)); assertEquals(2, c.getInt(6)); assertEquals(3, c.getInt(7))
            assertEquals(1, c.getInt(8)); assertTrue(c.isNull(9)); assertEquals(1500L, c.getLong(10))
            c.moveToNext()
            assertEquals("Nur **Text**", c.getString(1))
            assertEquals("", c.getString(2))
            assertEquals("Papierkorb-Status bleibt erhalten", 4000L, c.getLong(9))
        }
        db.query("SELECT name, icon, color FROM categories").use { c ->
            c.moveToFirst(); assertEquals("Einkaufen", c.getString(0)); assertEquals("", c.getString(1)); assertEquals(0, c.getInt(2))
        }
    }

    @Test
    fun roomOpensMigratedDatabase_andKeepsManualOrder() = runBlocking {
        createV3WithSampleData()
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val room = Room.databaseBuilder(context, AppDatabase::class.java, dbName)
            .addMigrations(*AppDatabase.ALL_MIGRATIONS)
            .allowMainThreadQueries()
            .build()
        try {
            val notes = room.noteDao().getActiveNotes()
            assertEquals(listOf(2, 5, 1, 7), notes.map { it.id })
            assertEquals("Nur Inhalt ohne Titel", notes.first().content)
            assertNull(notes.first().deletedAt)
            // Neue Funktionen funktionieren auf migrierten Daten
            room.noteDao().setPinned(7, true)
            assertEquals(7, room.noteDao().getActiveNotes().first().id)
        } finally {
            room.close()
        }
    }
}
