package vn.edu.vlu.moneymanager.feature.transaction.viewmodel

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import vn.edu.vlu.moneymanager.R
import vn.edu.vlu.moneymanager.data.AppContainer
import vn.edu.vlu.moneymanager.data.model.Category
import vn.edu.vlu.moneymanager.data.model.Transaction
import vn.edu.vlu.moneymanager.data.model.TransactionDetail
import vn.edu.vlu.moneymanager.data.model.TransactionType
import vn.edu.vlu.moneymanager.data.model.totalIn
import vn.edu.vlu.moneymanager.data.remote.NetworkMonitor
import vn.edu.vlu.moneymanager.data.remote.UiState
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
    val selectedCategory: Category? = null,
    /** A server call is running (download, delete, undo) */
    val isLoading: Boolean = false,
    /** Banner text: no network, or the last server call failed; null hides the banner */
    @param:StringRes val errorMessage: Int? = null
)

sealed interface TransactionListEvent {
    data object ShowUndoDelete : TransactionListEvent
}

class TransactionListViewModel(
    private val transactionRepository: TransactionRepository = AppContainer.transactionRepository,
    categoryRepository: CategoryRepository = AppContainer.categoryRepository,
    currencyRepository: CurrencyRepository = AppContainer.currencyRepository,
    settingsRepository: SettingsRepository = AppContainer.settingsRepository,
    isOnline: StateFlow<Boolean> = NetworkMonitor.isOnline
) : ViewModel() {

    private val filter = MutableStateFlow(TransactionFilter())

    /** State of the last server call; the list itself always comes from the Room copy */
    private val sync = MutableStateFlow<UiState<Unit>>(UiState.Loading)

    private val _events = Channel<TransactionListEvent>(Channel.BUFFERED)
    val events: Flow<TransactionListEvent> = _events.receiveAsFlow()

    private var lastDeleted: Transaction? = null

    val uiState: StateFlow<TransactionListUiState> = combine(
        transactionRepository.transactions,
        categoryRepository.categories,
        currencyRepository.rates,
        settingsRepository.defaultCurrency,
        combine(filter, sync, isOnline, ::Triple)
    ) { transactions, categories, rates, currency, (activeFilter, syncState, online) ->
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
            selectedCategory = categories.firstOrNull { it.id == activeFilter.categoryId },
            isLoading = syncState is UiState.Loading,
            errorMessage = if (!online) R.string.error_network_offline else (syncState as? UiState.Error)?.messageRes
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TransactionListUiState())

    init {
        refresh()
        // Airplane Mode turned off again: download what changed meanwhile, the banner goes away
        viewModelScope.launch { isOnline.drop(1).filter { it }.collect { refresh() } }
    }

    fun refresh() = runSync { transactionRepository.refresh() }

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

    /** Deleting needs the network: offline the row stays and the banner explains why. */
    fun delete(transactionId: Long) = runSync {
        transactionRepository.delete(transactionId).also { result ->
            val removed = (result as? UiState.Success)?.data ?: return@also
            lastDeleted = removed
            _events.send(TransactionListEvent.ShowUndoDelete)
        }
    }

    fun undoDelete() {
        val deleted = lastDeleted ?: return
        lastDeleted = null
        runSync { transactionRepository.restore(deleted) }
    }

    private fun runSync(call: suspend () -> UiState<*>) {
        viewModelScope.launch {
            sync.value = UiState.Loading
            val result = call()
            sync.value = if (result is UiState.Error) result else UiState.Success(Unit)
        }
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
