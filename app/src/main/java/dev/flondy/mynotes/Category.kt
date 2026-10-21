package dev.flondy.mynotes

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/** Benutzerdefinierte Kategorie (seit Datenbankversion 4, Icon und Farbe seit Version 5). */
@Entity(tableName = "categories")
data class Category(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String,
    @ColumnInfo(defaultValue = "0")
    val position: Int = 0,
    /** Schlüssel aus [dev.flondy.mynotes.ui.components.CategoryIcons] – leer = automatisch nach Name. */
    @ColumnInfo(defaultValue = "''")
    val icon: String = "",
    /** Index in den Kategoriefarben – 0 = automatisch passend zum Icon. */
    @ColumnInfo(defaultValue = "0")
    val color: Int = 0,
)
