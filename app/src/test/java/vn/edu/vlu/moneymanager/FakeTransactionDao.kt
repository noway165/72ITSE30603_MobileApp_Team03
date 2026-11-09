package vn.edu.vlu.moneymanager

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import vn.edu.vlu.moneymanager.data.local.dao.TransactionDao
import vn.edu.vlu.moneymanager.data.local.entity.TransactionEntity
import vn.edu.vlu.moneymanager.data.source.SampleData

/**
 * In-memory TransactionDao for JVM unit tests (no Android, no SQLite). Behaves like the Room table:
 * auto ids, newest first, observeAll() re-emits after each write.
 * Foreign keys (ON DELETE SET NULL) only exist in SQLite: they are tested in androidTest/.../TransactionDaoTest.
 */
class FakeTransactionDao(
    initial: List<TransactionEntity> = SampleData.transactions.map(TransactionEntity::fromModel)
) : TransactionDao {

    private val rows = MutableStateFlow(initial)
    private var nextId = (initial.maxOfOrNull { it.id } ?: 0L) + 1

    override fun observeAll(): Flow<List<TransactionEntity>> =
        rows.map { list -> list.sortedWith(compareByDescending<TransactionEntity> { it.dateMillis }.thenByDescending { it.id }) }

    override suspend fun getById(id: Long): TransactionEntity? = rows.value.firstOrNull { it.id == id }

    override suspend fun insert(transaction: TransactionEntity): Long {
        check(rows.value.none { it.id == transaction.id }) { "UNIQUE constraint failed: transactions.id" }
        val id = if (transaction.id == 0L) nextId++ else transaction.id
        rows.update { it + transaction.copy(id = id) }
        return id
    }

    override suspend fun update(transaction: TransactionEntity) {
        rows.update { list -> list.map { if (it.id == transaction.id) transaction else it } }
    }

    override suspend fun deleteById(id: Long) {
        rows.update { list -> list.filterNot { it.id == id } }
    }

    override suspend fun upsertAll(transactions: List<TransactionEntity>) {
        val ids = transactions.map { it.id }
        rows.update { list -> list.filterNot { it.id in ids } + transactions }
    }

    override suspend fun deleteAll() {
        rows.value = emptyList()
    }

    override suspend fun getPending(): List<TransactionEntity> = rows.value.filter { it.pendingSync }

    override suspend fun minId(): Long? = rows.value.minOfOrNull { it.id }

    override suspend fun categoryIds(): List<Long> = SampleData.categories.map { it.id }

    override suspend fun deleteSyncedExcept(ids: List<Long>) {
        rows.update { list -> list.filter { it.pendingSync || it.id in ids } }
    }
}
