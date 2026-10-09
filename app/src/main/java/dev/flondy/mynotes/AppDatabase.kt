package dev.flondy.mynotes

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.withTransaction
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Versionshistorie (Schemas liegen unter app/schemas):
 *  - v2 → v3: Spalte `displayOrder`
 *  - v3 → v4: Favoriten, Anheften, Farben, Kategorien, Checklisten, Papierkorb, Erstellungsdatum
 *
 * WICHTIG: Kein fallbackToDestructiveMigration() – Nutzerdaten dürfen niemals verloren gehen.
 * Jede künftige Schemaänderung braucht eine eigene, getestete Migration.
 */
@Database(entities = [Note::class, Category::class], version = 4, exportSchema = true)
abstract class AppDatabase : RoomDatabase() {

    abstract fun noteDao(): NoteDao
    abstract fun categoryDao(): CategoryDao

    suspend fun deleteCategorySafely(categoryId: Int) = withTransaction {
        noteDao().clearCategory(categoryId)
        categoryDao().deleteById(categoryId)
    }

    companion object {
        const val DATABASE_NAME = "note_database"

        @Volatile
        private var INSTANCE: AppDatabase? = null

        /** Fügt die Spalte 'displayOrder' hinzu. */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE notes_table ADD COLUMN displayOrder INTEGER NOT NULL DEFAULT 0")
            }
        }

        /**
         * Rein additive Migration: nur neue Spalten und eine neue Tabelle.
         * Das bisherige `timestamp` (= letzte Änderung) wird als bestmögliche Annäherung
         * an das Erstellungsdatum übernommen.
         */
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE notes_table ADD COLUMN createdAt INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE notes_table ADD COLUMN isPinned INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE notes_table ADD COLUMN isFavorite INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE notes_table ADD COLUMN color INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE notes_table ADD COLUMN categoryId INTEGER DEFAULT NULL")
                db.execSQL("ALTER TABLE notes_table ADD COLUMN noteType INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE notes_table ADD COLUMN deletedAt INTEGER DEFAULT NULL")
                db.execSQL("UPDATE notes_table SET createdAt = timestamp")
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `categories` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`name` TEXT NOT NULL, " +
                        "`position` INTEGER NOT NULL DEFAULT 0)"
                )
            }
        }

        val ALL_MIGRATIONS = arrayOf(MIGRATION_2_3, MIGRATION_3_4)

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DATABASE_NAME
                )
                    .addMigrations(*ALL_MIGRATIONS)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
