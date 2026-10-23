package vn.edu.vlu.moneymanager.feature.dashboard.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import vn.edu.vlu.moneymanager.data.AppContainer
import vn.edu.vlu.moneymanager.data.model.MonthKey
import vn.edu.vlu.moneymanager.data.model.TransactionDetail
import vn.edu.vlu.moneymanager.data.model.TransactionType
import vn.edu.vlu.moneymanager.data.model.totalIn
import vn.edu.vlu.moneymanager.data.repository.CategoryRepository
import vn.edu.vlu.moneymanager.data.repository.CurrencyRepository
import vn.edu.vlu.moneymanager.data.repository.SettingsRepository
import vn.edu.vlu.moneymanager.data.repository.TransactionRepository

data class DashboardUiState(
    val currency: String = "",
    val month: MonthKey = MonthKey.current(),
    val balance: Double = 0.0,
    val monthIncome: Double = 0.0,
    val monthExpense: Double = 0.0,
    val hasMissingRates: Boolean = false,
    val pendingSyncCount: Int = 0,
    val recent: List<TransactionDetail> = emptyList()
)

class DashboardViewModel(
    transactionRepository: TransactionRepository = AppContainer.transactionRepository,
    categoryRepository: CategoryRepository = AppContainer.categoryRepository,
    currencyRepository: CurrencyRepository = AppContainer.currencyRepository,
    settingsRepository: SettingsRepository = AppContainer.settingsRepository
) : ViewModel() {

    val uiState: StateFlow<DashboardUiState> = combine(
        transactionRepository.transactions,
        categoryRepository.categories,
        currencyRepository.rates,
        settingsRepository.defaultCurrency
    ) { transactions, categories, rates, currency ->
        val month = MonthKey.current()
        val thisMonth = transactions.filter { month.contains(it.dateMillis) }
        val balance = transactions.totalIn(currency, rates)
        val income = thisMonth.totalIn(currency, rates, TransactionType.INCOME)
        val expense = thisMonth.totalIn(currency, rates, TransactionType.EXPENSE)
        DashboardUiState(
            currency = currency,
            month = month,
            balance = balance.amount,
            monthIncome = income.amount,
            monthExpense = expense.amount,
            hasMissingRates = balance.hasMissingRates,
            pendingSyncCount = transactions.count { it.pendingSync },
            recent = transactions.take(RECENT_COUNT).map { transaction ->
                TransactionDetail(transaction, categories.firstOrNull { it.id == transaction.categoryId })
            }
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), DashboardUiState())

    private companion object {
        const val RECENT_COUNT = 5
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
