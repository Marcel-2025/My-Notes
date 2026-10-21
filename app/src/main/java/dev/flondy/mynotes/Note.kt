package dev.flondy.mynotes

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Eine Notiz (Text oder Checkliste).
 *
 * Datenbankversion 5 – alle neuen Spalten wurden per additiver Migration ergänzt
 * (3 → 4, 4 → 5), bestehende Notizen bleiben vollständig erhalten.
 *
 * - [content] ist der (optional formatierte) Notiztext, [checklist] die Checkliste
 *   (Zeilenformat „[ ] …“ / „[x] …“). Seit v5 kann eine Notiz beides enthalten.
 * - [noteType] = TYPE_CHECKLIST markiert Notizen, die als „Liste“ angelegt wurden.
 *
 * - [updatedAt] liegt aus Kompatibilitätsgründen weiterhin in der Spalte `timestamp`.
 * - [createdAt] wurde bei der Migration mit dem bisherigen `timestamp` befüllt.
 * - [deletedAt] != null bedeutet: Notiz liegt im Papierkorb.
 */
@Entity(tableName = "notes_table")
data class Note(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val title: String,
    val content: String,
    @ColumnInfo(name = "timestamp")
    val updatedAt: Long = System.currentTimeMillis(),
    val displayOrder: Int = 0,
    @ColumnInfo(defaultValue = "0")
    val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(defaultValue = "0")
    val isPinned: Boolean = false,
    @ColumnInfo(defaultValue = "0")
    val isFavorite: Boolean = false,
    @ColumnInfo(defaultValue = "0")
    val color: Int = 0,
    val categoryId: Int? = null,
    @ColumnInfo(defaultValue = "0")
    val noteType: Int = TYPE_TEXT,
    val deletedAt: Long? = null,
    @ColumnInfo(defaultValue = "0")
    val isArchived: Boolean = false,
    @ColumnInfo(defaultValue = "''")
    val checklist: String = "",
) {
    /** „Liste“ – als Checkliste angelegt oder enthält Checklisten-Einträge. */
    val isChecklist: Boolean get() = noteType == TYPE_CHECKLIST || checklist.isNotBlank()
    val hasChecklist: Boolean get() = checklist.isNotBlank()
    val isInTrash: Boolean get() = deletedAt != null
    val hasContent: Boolean get() = content.isNotBlank() || checklist.isNotBlank()

    companion object {
        const val TYPE_TEXT = 0
        const val TYPE_CHECKLIST = 1
    }
}
