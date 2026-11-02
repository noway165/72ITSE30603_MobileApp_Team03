package vn.edu.vlu.moneymanager

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import vn.edu.vlu.moneymanager.data.model.MonthKey
import vn.edu.vlu.moneymanager.data.model.Transaction
import vn.edu.vlu.moneymanager.data.model.TransactionType
import vn.edu.vlu.moneymanager.data.repository.BudgetRepository
import vn.edu.vlu.moneymanager.data.repository.CategoryRepository
import vn.edu.vlu.moneymanager.data.repository.SettingsRepository
import vn.edu.vlu.moneymanager.data.repository.TransactionRepository
import java.util.Calendar

/** uiState uses WhileSubscribed, so a test must keep a collector alive for uiState.value to update. */
@OptIn(ExperimentalCoroutinesApi::class)
fun <T> TestScope.collectInBackground(flow: Flow<T>) {
    backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { flow.collect { } }
}

/** Transaction repository over an in-memory DAO holding the SampleData transactions. */
fun sampleTransactionRepository(): TransactionRepository = TransactionRepository(FakeTransactionDao())

/** No transactions at all: tests that check totals want a clean slate. */
fun emptyTransactionRepository(): TransactionRepository = TransactionRepository(FakeTransactionDao(initial = emptyList()))

/** Budget repository over an in-memory DAO holding the SampleData budgets (current month). */
fun sampleBudgetRepository(): BudgetRepository = BudgetRepository(FakeBudgetDao())

/** Category repository over an in-memory DAO holding the SampleData categories. */
fun sampleCategoryRepository(): CategoryRepository = CategoryRepository(FakeCategoryDao())

fun emptyCategoryRepository(): CategoryRepository = CategoryRepository(FakeCategoryDao(initial = emptyList()))

/** Default currency USD: transactions in USD convert at rate 1.0, so tests do not depend on SampleData rates. */
fun usdSettings(): SettingsRepository = SettingsRepository().also { it.setDefaultCurrency("USD") }

/** Noon on the 15th of [month] (+ [minuteOffset] minutes): always inside that month, never near a boundary. */
fun midMonthMillis(month: MonthKey, minuteOffset: Int = 0): Long {
    val calendar = Calendar.getInstance().apply {
        clear()
        set(month.year, month.month - 1, 15, 12, 0, 0)
    }
    return calendar.timeInMillis + minuteOffset * 60_000L
}

fun testTransaction(
    type: TransactionType,
    amount: Double,
    dateMillis: Long,
    currency: String = "USD",
    categoryId: Long? = null,
    pendingSync: Boolean = false
) = Transaction(
    id = 0,
    type = type,
    amount = amount,
    currency = currency,
    categoryId = categoryId,
    dateMillis = dateMillis,
    note = "",
    pendingSync = pendingSync
)