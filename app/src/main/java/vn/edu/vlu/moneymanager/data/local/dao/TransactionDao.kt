package vn.edu.vlu.moneymanager.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import vn.edu.vlu.moneymanager.data.local.entity.TransactionEntity

/**
 * Reads return Flow (Room re-emits after every change); writes are suspend and run off the main thread.
 *
 * Ids: a synced row uses its MockAPI id (> 0). A row created offline gets a negative id until "Sync now"
 * uploads it and swaps in the server id, so "never uploaded" needs no extra column.
 */
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

    @Upsert
    suspend fun upsertAll(transactions: List<TransactionEntity>)

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM transactions")
    suspend fun deleteAll()

    /** Changes made on this device that the server does not have yet. */
    @Query("SELECT * FROM transactions WHERE pending_sync = 1")
    suspend fun getPending(): List<TransactionEntity>

    /** Lowest id in the table, or null when empty: the next offline row takes min(this, 0) - 1. */
    @Query("SELECT MIN(id) FROM transactions")
    suspend fun minId(): Long?

    /** Ids a transaction may point to; a server row whose category is gone is stored as Uncategorized. */
    @Query("SELECT id FROM categories")
    suspend fun categoryIds(): List<Long>

    @Query("DELETE FROM transactions WHERE pending_sync = 0 AND id NOT IN (:ids)")
    suspend fun deleteSyncedExcept(ids: List<Long>)

    /**
     * Makes the synced rows equal to the server's list. Pending rows are kept untouched,
     * so an offline change is not lost before it is uploaded.
     */
    @Transaction
    suspend fun replaceSynced(serverRows: List<TransactionEntity>) {
        val pendingIds = getPending().map { it.id }.toSet()
        deleteSyncedExcept(serverRows.map { it.id })
        upsertAll(serverRows.filterNot { it.id in pendingIds })
    }

    /** Swaps the row [oldId] for [uploaded] (same row with its server id), in one transaction. */
    @Transaction
    suspend fun replaceRow(oldId: Long, uploaded: TransactionEntity) {
        deleteById(oldId)
        upsertAll(listOf(uploaded))
    }
}
