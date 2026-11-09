package vn.edu.vlu.moneymanager

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import vn.edu.vlu.moneymanager.data.model.Category
import vn.edu.vlu.moneymanager.data.model.MonthKey
import vn.edu.vlu.moneymanager.data.model.Transaction
import vn.edu.vlu.moneymanager.data.model.TransactionType
import vn.edu.vlu.moneymanager.data.remote.UiState
import vn.edu.vlu.moneymanager.data.repository.BudgetRepository
import vn.edu.vlu.moneymanager.data.repository.CategoryRepository
import vn.edu.vlu.moneymanager.data.repository.CurrencyRepository
import vn.edu.vlu.moneymanager.data.repository.SettingsRepository
import vn.edu.vlu.moneymanager.data.repository.TransactionRepository
import java.util.Calendar

/** uiState uses WhileSubscribed, so a test must keep a collector alive for uiState.value to update. */
@OptIn(ExperimentalCoroutinesApi::class)
fun <T> TestScope.collectInBackground(flow: Flow<T>) {
    backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { flow.collect { } }
}

/** Transaction repository over in-memory fakes holding the SampleData transactions; runs inline, no real IO thread. */
fun sampleTransactionRepository(api: FakeTransactionApi = FakeTransactionApi()): TransactionRepository =
    TransactionRepository(FakeTransactionDao(), api, { TEST_USER_ID }, Dispatchers.Unconfined)

/** No transactions at all: tests that check totals want a clean slate. */
fun emptyTransactionRepository(api: FakeTransactionApi = FakeTransactionApi(initial = emptyList())): TransactionRepository =
    TransactionRepository(FakeTransactionDao(initial = emptyList()), api, { TEST_USER_ID }, Dispatchers.Unconfined)

/** For tests that only need a transaction to exist: save() on the fakes cannot fail unless the API is told to. */
suspend fun TransactionRepository.saved(transaction: Transaction): Transaction =
    (save(transaction) as UiState.Success).data

/** Budget repository over in-memory fakes holding the SampleData budgets (current month). */
fun sampleBudgetRepository(api: FakeBudgetApi = FakeBudgetApi()): BudgetRepository =
    BudgetRepository(FakeBudgetDao(), api, { TEST_USER_ID }, Dispatchers.Unconfined)

/** Currency repository over an in-memory DAO holding the SampleData rates (never downloaded). */
fun sampleCurrencyRepository(): CurrencyRepository = CurrencyRepository(FakeExchangeRateDao())

/** Category repository over in-memory fakes holding the SampleData categories; runs inline, no real IO thread. */
fun sampleCategoryRepository(api: FakeCategoryApi = FakeCategoryApi()): CategoryRepository =
    CategoryRepository(FakeCategoryDao(), api, { TEST_USER_ID }, Dispatchers.Unconfined)

/** For tests that only need a category to exist: save() on the fakes cannot fail. */
suspend fun CategoryRepository.saved(category: Category): Category = (save(category) as UiState.Success).data

fun emptyCategoryRepository(): CategoryRepository =
    CategoryRepository(FakeCategoryDao(initial = emptyList()), FakeCategoryApi(), { TEST_USER_ID }, Dispatchers.Unconfined)

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