package vn.edu.vlu.moneymanager.feature.transaction.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import vn.edu.vlu.moneymanager.data.AppContainer
import vn.edu.vlu.moneymanager.data.model.Category
import vn.edu.vlu.moneymanager.data.model.Transaction
import vn.edu.vlu.moneymanager.data.model.TransactionDetail
import vn.edu.vlu.moneymanager.data.model.TransactionType
import vn.edu.vlu.moneymanager.data.model.totalIn
import vn.edu.vlu.moneymanager.data.repository.CategoryRepository
import vn.edu.vlu.moneymanager.data.repository.CurrencyRepository
import vn.edu.vlu.moneymanager.data.repository.SettingsRepository
import vn.edu.vlu.moneymanager.data.repository.TransactionRepository
import vn.edu.vlu.moneymanager.util.DateUtils

data class TransactionFilter(
    val query: String = "",
    val type: TransactionType? = null,
    val categoryId: Long? = null,
    /** Picked range as local millis (start of first day, any time of last day) */
    val dateFrom: Long? = null,
    val dateTo: Long? = null
) {
    val isActive: Boolean get() = query.isNotBlank() || type != null || categoryId != null || dateFrom != null
}

data class TransactionListUiState(
    val items: List<TransactionDetail> = emptyList(),
    val net: Double = 0.0,
    val currency: String = "",
    val hasAnyTransactions: Boolean = true,
    val filter: TransactionFilter = TransactionFilter(),
    val categories: List<Category> = emptyList(),
    val selectedCategory: Category? = null
)

sealed interface TransactionListEvent {
    data object ShowUndoDelete : TransactionListEvent
}

class TransactionListViewModel(
    private val transactionRepository: TransactionRepository = AppContainer.transactionRepository,
    categoryRepository: CategoryRepository = AppContainer.categoryRepository,
    currencyRepository: CurrencyRepository = AppContainer.currencyRepository,
    settingsRepository: SettingsRepository = AppContainer.settingsRepository
) : ViewModel() {

    private val filter = MutableStateFlow(TransactionFilter())

    private val _events = Channel<TransactionListEvent>(Channel.BUFFERED)
    val events: Flow<TransactionListEvent> = _events.receiveAsFlow()

    private var lastDeleted: Transaction? = null

    val uiState: StateFlow<TransactionListUiState> = combine(
        transactionRepository.transactions,
        categoryRepository.categories,
        currencyRepository.rates,
        settingsRepository.defaultCurrency,
        filter
    ) { transactions, categories, rates, currency, activeFilter ->
        val details = transactions.map { transaction ->
            TransactionDetail(transaction, categories.firstOrNull { it.id == transaction.categoryId })
        }
        val visible = details.filter { matches(it, activeFilter) }
        TransactionListUiState(
            items = visible,
            net = visible.map { it.transaction }.totalIn(currency, rates).amount,
            currency = currency,
            hasAnyTransactions = transactions.isNotEmpty(),
            filter = activeFilter,
            categories = categories,
            selectedCategory = categories.firstOrNull { it.id == activeFilter.categoryId }
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TransactionListUiState())

    fun setQuery(query: String) = filter.update { it.copy(query = query) }

    fun setType(type: TransactionType?) = filter.update { it.copy(type = type) }

    fun setCategory(categoryId: Long?) = filter.update { it.copy(categoryId = categoryId) }

    /** [fromUtc] / [toUtc] come straight from MaterialDatePicker; null clears the date filter. */
    fun setDateRange(fromUtc: Long?, toUtc: Long?) = filter.update {
        if (fromUtc == null || toUtc == null) {
            it.copy(dateFrom = null, dateTo = null)
        } else {
            it.copy(dateFrom = DateUtils.fromPickerUtc(fromUtc, hour = 0), dateTo = DateUtils.fromPickerUtc(toUtc))
        }
    }

    fun delete(transactionId: Long) {
        lastDeleted = transactionRepository.delete(transactionId) ?: return
        viewModelScope.launch { _events.send(TransactionListEvent.ShowUndoDelete) }
    }

    fun undoDelete() {
        lastDeleted?.let { transactionRepository.save(it) }
        lastDeleted = null
    }

    private fun matches(detail: TransactionDetail, filter: TransactionFilter): Boolean {
        val transaction = detail.transaction
        if (filter.type != null && transaction.type != filter.type) return false
        if (filter.categoryId != null && transaction.categoryId != filter.categoryId) return false
        if (filter.dateFrom != null && transaction.dateMillis < filter.dateFrom) return false
        if (filter.dateTo != null && transaction.dateMillis > DateUtils.endOfDay(filter.dateTo)) return false
        val query = filter.query.trim()
        if (query.isEmpty()) return true
        return transaction.note.contains(query, ignoreCase = true) ||
            detail.category?.name?.contains(query, ignoreCase = true) == true
    }
}
