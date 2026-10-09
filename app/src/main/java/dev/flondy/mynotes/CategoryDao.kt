package dev.flondy.mynotes

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories ORDER BY position ASC, id ASC")
    fun observeAll(): Flow<List<Category>>

    @Query("SELECT * FROM categories ORDER BY position ASC, id ASC")
    suspend fun getAll(): List<Category>

    @Query("SELECT COUNT(id) FROM categories")
    suspend fun count(): Int

    @Query("SELECT MAX(position) FROM categories")
    suspend fun maxPosition(): Int?

    @Insert
    suspend fun insert(category: Category): Long

    @Update
    suspend fun update(category: Category)

    @Query("DELETE FROM categories WHERE id = :id")
    suspend fun deleteById(id: Int)
}
