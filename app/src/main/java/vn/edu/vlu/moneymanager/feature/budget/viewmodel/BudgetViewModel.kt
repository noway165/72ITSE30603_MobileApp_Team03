package vn.edu.vlu.moneymanager.feature.budget.viewmodel

import androidx.annotation.StringRes
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
import vn.edu.vlu.moneymanager.R
import vn.edu.vlu.moneymanager.data.AppContainer
import vn.edu.vlu.moneymanager.data.model.Budget
import vn.edu.vlu.moneymanager.data.model.BudgetProgress
import vn.edu.vlu.moneymanager.data.model.Category
import vn.edu.vlu.moneymanager.data.model.MonthKey
import vn.edu.vlu.moneymanager.data.model.TransactionType
import vn.edu.vlu.moneymanager.data.model.totalIn
import vn.edu.vlu.moneymanager.data.repository.BudgetRepository
import vn.edu.vlu.moneymanager.data.repository.CategoryRepository
import vn.edu.vlu.moneymanager.data.repository.CurrencyRepository
import vn.edu.vlu.moneymanager.data.repository.SettingsRepository
import vn.edu.vlu.moneymanager.data.repository.TransactionRepository
import vn.edu.vlu.moneymanager.util.Validators

data class BudgetUiState(
    val month: MonthKey = MonthKey.current(),
    val currency: String = "",
    val items: List<BudgetProgress> = emptyList(),
    val expenseCategories: List<Category> = emptyList()
) {
    val totalSpent: Double get() = items.sumOf { it.spent }
    val totalLimit: Double get() = items.sumOf { it.budget.limit }
    val totalPercent: Int get() = if (totalLimit <= 0) 0 else (totalSpent * 100 / totalLimit).toInt()
}

/** Validation result of the budget dialog; both null means the budget was saved. */
data class BudgetFormErrors(
    @param:StringRes val categoryError: Int? = null,
    @param:StringRes val limitError: Int? = null
) {
    val hasErrors: Boolean get() = categoryError != null || limitError != null
}

sealed interface BudgetEvent {
    data object ShowUndoDelete : BudgetEvent
}

class BudgetViewModel(
    private val budgetRepository: BudgetRepository = AppContainer.budgetRepository,
    transactionRepository: TransactionRepository = AppContainer.transactionRepository,
    categoryRepository: CategoryRepository = AppContainer.categoryRepository,
    currencyRepository: CurrencyRepository = AppContainer.currencyRepository,
    settingsRepository: SettingsRepository = AppContainer.settingsRepository
) : ViewModel() {

    private val month = MutableStateFlow(MonthKey.current())

    private val _events = Channel<BudgetEvent>(Channel.BUFFERED)
    val events: Flow<BudgetEvent> = _events.receiveAsFlow()

    private var lastDeleted: Budget? = null

    val uiState: StateFlow<BudgetUiState> = combine(
        month,
        budgetRepository.budgets,
        transactionRepository.transactions,
        categoryRepository.categories,
        combine(currencyRepository.rates, settingsRepository.defaultCurrency, ::Pair)
    ) { shownMonth, budgets, transactions, categories, (rates, currency) ->
        val monthExpenses = transactions.filter { it.type == TransactionType.EXPENSE && shownMonth.contains(it.dateMillis) }
        val items = budgets.filter { it.month == shownMonth }.map { budget ->
            val spent = monthExpenses.filter { it.categoryId == budget.categoryId }
                .totalIn(currency, rates, TransactionType.EXPENSE).amount
            BudgetProgress(budget, categories.firstOrNull { it.id == budget.categoryId }, spent)
        }.sortedByDescending { it.percent }
        BudgetUiState(
            month = shownMonth,
            currency = currency,
            items = items,
            expenseCategories = categories.filter { it.type == TransactionType.EXPENSE }
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BudgetUiState())

    fun previousMonth() = month.update { it.plusMonths(-1) }

    fun nextMonth() = month.update { it.plusMonths(1) }

    /** Creates a budget ([editingId] == null) or updates one. Returns the errors to show in the dialog. */
    fun saveBudget(editingId: Long?, categoryId: Long?, limitText: String): BudgetFormErrors {
        val limit = Validators.parseAmount(limitText)
        val selectedMonth = month.value
        val duplicate = budgetRepository.budgets.value.any {
            it.month == selectedMonth && it.categoryId == categoryId && it.id != editingId
        }
        val errors = BudgetFormErrors(
            categoryError = when {
                categoryId == null -> R.string.error_category_required
                duplicate -> R.string.error_budget_exists
                else -> null
            },
            limitError = if (limit == null) R.string.error_amount_invalid else null
        )
        if (errors.hasErrors || categoryId == null || limit == null) return errors

        budgetRepository.save(Budget(editingId ?: 0L, categoryId, selectedMonth, limit))
        return errors
    }

    fun delete(budgetId: Long) {
        lastDeleted = budgetRepository.delete(budgetId) ?: return
        viewModelScope.launch { _events.send(BudgetEvent.ShowUndoDelete) }
    }

    fun undoDelete() {
        lastDeleted?.let { budgetRepository.save(it) }
        lastDeleted = null
    }
}
