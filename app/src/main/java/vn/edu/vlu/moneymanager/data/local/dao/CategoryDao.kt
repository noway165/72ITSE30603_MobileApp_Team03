package vn.edu.vlu.moneymanager.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import vn.edu.vlu.moneymanager.data.local.entity.CategoryEntity

/** Reads return Flow (Room re-emits after every change); writes are suspend and run off the main thread. */
@Dao
interface CategoryDao {

    @Query("SELECT * FROM categories ORDER BY id")
    fun observeAll(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories WHERE id = :id")
    suspend fun getById(id: Long): CategoryEntity?

    /** Returns the new row id. Fails (SQLiteConstraintException) when the same name + type already exists. */
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(category: CategoryEntity): Long

    @Update
    suspend fun update(category: CategoryEntity)

    @Query("DELETE FROM categories WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM categories WHERE id NOT IN (:ids)")
    suspend fun deleteExcept(ids: List<Long>)

    // Upsert, not REPLACE: REPLACE deletes the old row first, which would fire ON DELETE on transactions and budgets
    @Upsert
    suspend fun upsertAll(categories: List<CategoryEntity>)

    /** Makes the table equal to [categories]. Old rows go first so a renamed row cannot hit UNIQUE (name, type). */
    @Transaction
    suspend fun replaceAll(categories: List<CategoryEntity>) {
        deleteExcept(categories.map { it.id })
        upsertAll(categories)
    }
}
