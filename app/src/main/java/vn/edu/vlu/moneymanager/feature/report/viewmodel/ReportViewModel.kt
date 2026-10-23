package vn.edu.vlu.moneymanager.feature.report.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import vn.edu.vlu.moneymanager.data.AppContainer
import vn.edu.vlu.moneymanager.data.model.MonthKey
import vn.edu.vlu.moneymanager.data.model.TransactionType
import vn.edu.vlu.moneymanager.data.model.totalIn
import vn.edu.vlu.moneymanager.data.repository.CategoryRepository
import vn.edu.vlu.moneymanager.data.repository.CurrencyRepository
import vn.edu.vlu.moneymanager.data.repository.SettingsRepository
import vn.edu.vlu.moneymanager.data.repository.TransactionRepository

/** One slice of the pie chart. categoryName == null means "Uncategorized". */
data class CategorySlice(val categoryName: String?, val colorHex: String?, val amount: Double)

/** One group of the bar chart. */
data class MonthBar(val month: MonthKey, val income: Double, val expense: Double)

data class ReportUiState(
    val month: MonthKey = MonthKey.current(),
    val currency: String = "",
    val income: Double = 0.0,
    val expense: Double = 0.0,
    val slices: List<CategorySlice> = emptyList(),
    val bars: List<MonthBar> = emptyList(),
    val hasMissingRates: Boolean = false
)

class ReportViewModel(
    transactionRepository: TransactionRepository = AppContainer.transactionRepository,
    categoryRepository: CategoryRepository = AppContainer.categoryRepository,
    currencyRepository: CurrencyRepository = AppContainer.currencyRepository,
    settingsRepository: SettingsRepository = AppContainer.settingsRepository
) : ViewModel() {

    private val month = MutableStateFlow(MonthKey.current())

    val uiState: StateFlow<ReportUiState> = combine(
        month,
        transactionRepository.transactions,
        categoryRepository.categories,
        currencyRepository.rates,
        settingsRepository.defaultCurrency
    ) { shownMonth, transactions, categories, rates, currency ->
        val inMonth = transactions.filter { shownMonth.contains(it.dateMillis) }
        val income = inMonth.totalIn(currency, rates, TransactionType.INCOME)
        val expense = inMonth.totalIn(currency, rates, TransactionType.EXPENSE)

        val slices = inMonth.filter { it.type == TransactionType.EXPENSE }
            .groupBy { it.categoryId }
            .map { (categoryId, rows) ->
                val category = categories.firstOrNull { it.id == categoryId }
                CategorySlice(category?.name, category?.colorHex, rows.totalIn(currency, rates, TransactionType.EXPENSE).amount)
            }
            .filter { it.amount > 0 }
            .sortedByDescending { it.amount }

        val bars = (BAR_MONTHS - 1 downTo 0).map { monthsBack ->
            val barMonth = shownMonth.plusMonths(-monthsBack)
            val rows = transactions.filter { barMonth.contains(it.dateMillis) }
            MonthBar(
                barMonth,
                rows.totalIn(currency, rates, TransactionType.INCOME).amount,
                rows.totalIn(currency, rates, TransactionType.EXPENSE).amount
            )
        }

        ReportUiState(
            month = shownMonth,
            currency = currency,
            income = income.amount,
            expense = expense.amount,
            slices = slices,
            bars = bars,
            hasMissingRates = income.hasMissingRates || expense.hasMissingRates
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ReportUiState())

    fun previousMonth() = month.update { it.plusMonths(-1) }

    fun nextMonth() = month.update { it.plusMonths(1) }

    private companion object {
        const val BAR_MONTHS = 6
    }
}
