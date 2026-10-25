package vn.edu.vlu.moneymanager

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import vn.edu.vlu.moneymanager.data.model.MonthKey
import vn.edu.vlu.moneymanager.data.model.Transaction
import vn.edu.vlu.moneymanager.data.model.TransactionType
import vn.edu.vlu.moneymanager.data.repository.CategoryRepository
import vn.edu.vlu.moneymanager.data.repository.SettingsRepository
import vn.edu.vlu.moneymanager.data.repository.TransactionRepository
import java.util.Calendar

/** uiState uses WhileSubscribed, so a test must keep a collector alive for uiState.value to update. */
@OptIn(ExperimentalCoroutinesApi::class)
fun <T> TestScope.collectInBackground(flow: Flow<T>) {
    backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { flow.collect { } }
}

/** Repositories start with SampleData; tests want a clean slate so totals are predictable. */
fun emptyTransactionRepository(): TransactionRepository = TransactionRepository().also { repo ->
    repo.transactions.value.forEach { repo.delete(it.id) }
}

fun emptyCategoryRepository(): CategoryRepository = CategoryRepository().also { repo ->
    repo.categories.value.forEach { repo.delete(it.id) }
}

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