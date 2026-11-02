package vn.edu.vlu.moneymanager.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import vn.edu.vlu.moneymanager.data.local.entity.TransactionEntity

/** Reads return Flow (Room re-emits after every change); writes are suspend and run off the main thread. */
@Dao
interface TransactionDao {

    /** Newest first; id breaks ties so rows on the same date keep a stable order. */
    @Query("SELECT * FROM transactions ORDER BY date_millis DESC, id DESC")
    fun observeAll(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun getById(id: Long): TransactionEntity?

    /** Returns the row id. With id 0 Room assigns a new one; a non-zero id (undo delete) is kept. */
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(transaction: TransactionEntity): Long

    @Update
    suspend fun update(transaction: TransactionEntity)

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("UPDATE transactions SET pending_sync = 0 WHERE pending_sync = 1")
    suspend fun markAllSynced()
}
