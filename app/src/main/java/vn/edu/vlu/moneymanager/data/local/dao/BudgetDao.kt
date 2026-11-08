package vn.edu.vlu.moneymanager.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import vn.edu.vlu.moneymanager.data.local.entity.BudgetEntity

/** Reads return Flow (Room re-emits after every change); writes are suspend and run off the main thread. */
@Dao
interface BudgetDao {

    @Query("SELECT * FROM budgets ORDER BY id")
    fun observeAll(): Flow<List<BudgetEntity>>

    /** Returns the row id. Fails (SQLiteConstraintException) when the category already has a budget that month. */
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(budget: BudgetEntity): Long

    @Update
    suspend fun update(budget: BudgetEntity)

    @Query("DELETE FROM budgets WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM budgets WHERE id NOT IN (:ids)")
    suspend fun deleteExcept(ids: List<Long>)

    @Upsert
    suspend fun upsertAll(budgets: List<BudgetEntity>)

    /** Ids a budget may point to; a server budget whose category is gone is skipped (ON DELETE CASCADE). */
    @Query("SELECT id FROM categories")
    suspend fun categoryIds(): List<Long>

    /** Makes the table equal to [budgets]. Old rows go first so a moved budget cannot hit UNIQUE (category, month). */
    @Transaction
    suspend fun replaceAll(budgets: List<BudgetEntity>) {
        deleteExcept(budgets.map { it.id })
        upsertAll(budgets)
    }
}
