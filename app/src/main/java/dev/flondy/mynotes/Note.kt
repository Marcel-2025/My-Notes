package dev.flondy.mynotes // Dein Paketname

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notes_table")
data class Note(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val title: String,
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val displayOrder: Int = 0 // NEU: Für die benutzerdefinierte Sortierreihenfolge
)