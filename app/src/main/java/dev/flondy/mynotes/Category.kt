package dev.flondy.mynotes

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/** Benutzerdefinierte Kategorie (seit Datenbankversion 4). */
@Entity(tableName = "categories")
data class Category(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String,
    @ColumnInfo(defaultValue = "0")
    val position: Int = 0,
)
