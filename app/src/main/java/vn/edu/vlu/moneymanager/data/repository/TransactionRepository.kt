package vn.edu.vlu.moneymanager.data.repository

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import retrofit2.HttpException
import vn.edu.vlu.moneymanager.data.local.dao.TransactionDao
import vn.edu.vlu.moneymanager.data.local.entity.TransactionEntity
import vn.edu.vlu.moneymanager.data.model.Transaction
import vn.edu.vlu.moneymanager.data.remote.TransactionApi
import vn.edu.vlu.moneymanager.data.remote.TransactionDto
import vn.edu.vlu.moneymanager.data.remote.UiState
import vn.edu.vlu.moneymanager.data.remote.apiCall
import vn.edu.vlu.moneymanager.data.remote.toDto
import java.io.IOException

/**
 * Transactions live on MockAPI; the Room table "transactions" is the offline copy the screens read, newest first.
 * The rest of the app only sees the [Transaction] model: entities and DTOs stay inside the data layer.
 *
 * Unlike categories, saving works without a network: the row is kept on this device with pending_sync = 1
 * (and a negative id when it is new) until [syncPending] uploads it. Deleting needs the network.
 * Deleting a category needs nothing here: its foreign key (ON DELETE SET NULL) makes the rows "Uncategorized".
 */
class TransactionRepository(
    private val transactionDao: TransactionDao,
    private val api: TransactionApi,
    private val userId: () -> String?,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {

    /** Re-emits after every insert / update / delete, so Transactions, Dashboard, Budgets and Reports stay in sync. */
    val transactions: Flow<List<Transaction>> =
        transactionDao.observeAll().map { rows -> rows.map(TransactionEntity::toModel) }

    // Startup sync, Sync now and the list screen can run together: one row must not be uploaded twice
    private val syncLock = Mutex()

    suspend fun get(id: Long): Transaction? = transactionDao.getById(id)?.toModel()

    /** Replaces the synced rows with the server's list; rows waiting to sync are kept. */
    suspend fun refresh(): UiState<Unit> = syncLock.withLock {
        apiCall(ioDispatcher) {
            val uid = userId() ?: return@apiCall
            val categoryIds = transactionDao.categoryIds().toSet()
            val rows = fetch(uid).map { dto ->
                val row = dto.toEntity()
                if (row.categoryId in categoryIds) row else row.copy(categoryId = null)
            }
            transactionDao.replaceSynced(rows)
        }
    }

    /**
     * Creates the transaction when its id is 0, otherwise updates it. Without a network the change is kept
     * on this device as pending (Success with pendingSync = true); other server errors leave Room unchanged.
     */
    suspend fun save(transaction: Transaction): UiState<Transaction> = apiCall(ioDispatcher) {
        val uid = checkNotNull(userId()) { "not signed in" }
        try {
            val uploaded = upload(transaction, uid)
            transactionDao.replaceRow(transaction.id, uploaded)
            uploaded.toModel()
        } catch (e: IOException) {
            // Offline or timeout: keep the change here, Sync now uploads it later
            val id = if (transaction.id == 0L) nextLocalId() else transaction.id
            val pending = TransactionEntity.fromModel(transaction.copy(id = id, pendingSync = true))
            transactionDao.upsertAll(listOf(pending))
            pending.toModel()
        }
    }

    /** Undo of a delete: the server gave the old id away, so the row comes back as a new transaction. */
    suspend fun restore(transaction: Transaction): UiState<Transaction> = save(transaction.copy(id = 0L))

    /** Returns the removed transaction (for undo), or Success(null) when the id does not exist. */
    suspend fun delete(id: Long): UiState<Transaction?> = apiCall(ioDispatcher) {
        val removed = transactionDao.getById(id) ?: return@apiCall null
        // A negative id was never uploaded; 404 means another device already deleted it
        if (id > 0) ignoreNotFound { api.delete(id) }
        transactionDao.deleteById(id)
        removed.toModel()
    }

    /** "Sync now": uploads every pending row (new ones get their server id), oldest change first. */
    suspend fun syncPending(): UiState<Int> = syncLock.withLock {
        apiCall(ioDispatcher) {
            val uid = checkNotNull(userId()) { "not signed in" }
            val pending = transactionDao.getPending()
            pending.forEach { row ->
                transactionDao.replaceRow(row.id, upload(row.toModel(), uid))
            }
            pending.size
        }
    }

    /** POST for a row the server has never seen (id <= 0), PUT otherwise. Returns the synced row. */
    private suspend fun upload(transaction: Transaction, uid: String): TransactionEntity {
        val dto = transaction.toDto(uid)
        val result = if (transaction.id <= 0L) {
            api.create(dto)
        } else {
            // Edited offline but deleted on the server meanwhile: upload it again as a new row
            ignoreNotFound { api.update(transaction.id, dto) } ?: api.create(dto)
        }
        return result.toEntity()
    }

    private suspend fun nextLocalId(): Long = minOf(transactionDao.minId() ?: 0L, 0L) - 1

    /** MockAPI can answer 404 instead of an empty list when the filter matches no row. */
    private suspend fun fetch(uid: String): List<TransactionDto> = ignoreNotFound { api.getAll(uid) } ?: emptyList()
}

/** Runs [call]; a 404 from the server becomes null, every other error is thrown. */
internal suspend fun <T> ignoreNotFound(call: suspend () -> T): T? = try {
    call()
} catch (e: HttpException) {
    if (e.code() == 404) null else throw e
}
