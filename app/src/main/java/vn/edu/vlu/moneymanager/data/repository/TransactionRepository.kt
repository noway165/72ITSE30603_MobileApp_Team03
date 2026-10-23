package vn.edu.vlu.moneymanager.data.repository

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import vn.edu.vlu.moneymanager.data.model.Transaction
import vn.edu.vlu.moneymanager.data.source.SampleData

/** In-memory store of transactions, newest first. Room will replace the MutableStateFlow later. */
class TransactionRepository {

    private val _transactions = MutableStateFlow(SampleData.transactions.sortedByDescending { it.dateMillis })
    val transactions: StateFlow<List<Transaction>> = _transactions.asStateFlow()

    private var nextId = (SampleData.transactions.maxOfOrNull { it.id } ?: 0L) + 1

    fun get(id: Long): Transaction? = _transactions.value.firstOrNull { it.id == id }

    /** Inserts when [transaction] has id 0, otherwise replaces the row with the same id. */
    fun save(transaction: Transaction): Transaction {
        val saved = if (transaction.id == 0L) transaction.copy(id = nextId++) else transaction
        _transactions.update { list ->
            (list.filterNot { it.id == saved.id } + saved).sortedByDescending { it.dateMillis }
        }
        return saved
    }

    fun delete(id: Long): Transaction? {
        val removed = get(id) ?: return null
        _transactions.update { list -> list.filterNot { it.id == id } }
        return removed
    }

    /** Called when a category is deleted: its transactions become "Uncategorized". */
    fun clearCategory(categoryId: Long) {
        _transactions.update { list ->
            list.map { if (it.categoryId == categoryId) it.copy(categoryId = null, pendingSync = true) else it }
        }
    }

    fun markAllSynced() {
        _transactions.update { list -> list.map { it.copy(pendingSync = false) } }
    }
}
