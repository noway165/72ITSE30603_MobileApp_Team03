package vn.edu.vlu.moneymanager.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import vn.edu.vlu.moneymanager.data.local.dao.TransactionDao
import vn.edu.vlu.moneymanager.data.local.entity.TransactionEntity
import vn.edu.vlu.moneymanager.data.model.Transaction

/**
 * Transactions stored in the Room table "transactions", newest first. The rest of the app only sees the
 * [Transaction] model: entities stay inside the data layer.
 *
 * Deleting a category needs nothing here: its foreign key (ON DELETE SET NULL) makes the rows "Uncategorized".
 */
class TransactionRepository(private val transactionDao: TransactionDao) {

    /** Re-emits after every insert / update / delete, so Transactions, Dashboard, Budgets and Reports stay in sync. */
    val transactions: Flow<List<Transaction>> =
        transactionDao.observeAll().map { rows -> rows.map(TransactionEntity::toModel) }

    suspend fun get(id: Long): Transaction? = transactionDao.getById(id)?.toModel()

    /** Inserts when [transaction] has id 0 (Room assigns the id), otherwise updates the row with the same id. */
    suspend fun save(transaction: Transaction): Transaction {
        val entity = TransactionEntity.fromModel(transaction)
        return if (transaction.id == 0L) {
            transaction.copy(id = transactionDao.insert(entity))
        } else {
            transactionDao.update(entity)
            transaction
        }
    }

    /** Puts a deleted transaction back with its old id (undo). */
    suspend fun restore(transaction: Transaction) {
        transactionDao.insert(TransactionEntity.fromModel(transaction))
    }

    /** Returns the removed transaction (for undo), or null when the id does not exist. */
    suspend fun delete(id: Long): Transaction? {
        val removed = get(id) ?: return null
        transactionDao.deleteById(id)
        return removed
    }

    suspend fun markAllSynced() {
        transactionDao.markAllSynced()
    }
}
