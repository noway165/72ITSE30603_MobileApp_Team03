package vn.edu.vlu.moneymanager.feature.dashboard

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import vn.edu.vlu.moneymanager.MainDispatcherRule
import vn.edu.vlu.moneymanager.collectInBackground
import vn.edu.vlu.moneymanager.data.model.Category
import vn.edu.vlu.moneymanager.data.model.MonthKey
import vn.edu.vlu.moneymanager.data.model.TransactionType.EXPENSE
import vn.edu.vlu.moneymanager.data.model.TransactionType.INCOME
import vn.edu.vlu.moneymanager.data.repository.CurrencyRepository
import vn.edu.vlu.moneymanager.emptyCategoryRepository
import vn.edu.vlu.moneymanager.emptyTransactionRepository
import vn.edu.vlu.moneymanager.feature.dashboard.viewmodel.DashboardViewModel
import vn.edu.vlu.moneymanager.midMonthMillis
import vn.edu.vlu.moneymanager.testTransaction
import vn.edu.vlu.moneymanager.usdSettings

class DashboardViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val transactions = emptyTransactionRepository()
    private val categories = emptyCategoryRepository()
    private val settings = usdSettings()
    private val thisMonth = MonthKey.current()

    private fun createViewModel() =
        DashboardViewModel(transactions, categories, CurrencyRepository(), settings)

    @Test
    fun noTransactions_everythingIsZero() = runTest {
        val viewModel = createViewModel()
        collectInBackground(viewModel.uiState)

        val state = viewModel.uiState.value
        assertEquals("USD", state.currency)
        assertEquals(0.0, state.balance, DELTA)
        assertEquals(0.0, state.monthIncome, DELTA)
        assertEquals(0.0, state.monthExpense, DELTA)
        assertTrue(state.recent.isEmpty())
        assertEquals(0, state.pendingSyncCount)
        assertFalse(state.hasMissingRates)
    }

    @Test
    fun balanceCountsAllMonths_monthTotalsCountOnlyThisMonth() = runTest {
        transactions.save(testTransaction(INCOME, 1000.0, midMonthMillis(thisMonth)))
        transactions.save(testTransaction(EXPENSE, 300.0, midMonthMillis(thisMonth, 1)))
        transactions.save(testTransaction(EXPENSE, 50.0, midMonthMillis(thisMonth.plusMonths(-2))))
        val viewModel = createViewModel()
        collectInBackground(viewModel.uiState)

        val state = viewModel.uiState.value
        assertEquals(650.0, state.balance, DELTA)
        assertEquals(1000.0, state.monthIncome, DELTA)
        assertEquals(300.0, state.monthExpense, DELTA)
    }

    @Test
    fun recent_showsFiveNewestTransactions() = runTest {
        repeat(7) { i -> transactions.save(testTransaction(EXPENSE, 10.0, midMonthMillis(thisMonth, i))) }
        val viewModel = createViewModel()
        collectInBackground(viewModel.uiState)

        val recent = viewModel.uiState.value.recent
        assertEquals(5, recent.size)
        assertEquals(midMonthMillis(thisMonth, 6), recent.first().transaction.dateMillis)
        assertEquals(midMonthMillis(thisMonth, 2), recent.last().transaction.dateMillis)
    }

    @Test
    fun recent_joinsCategory_andUncategorizedHasNull() = runTest {
        val food = categories.save(Category(0, "Food", EXPENSE, "#FF0000"))
        transactions.save(testTransaction(EXPENSE, 10.0, midMonthMillis(thisMonth, 2), categoryId = food.id))
        transactions.save(testTransaction(EXPENSE, 10.0, midMonthMillis(thisMonth, 1)))
        val viewModel = createViewModel()
        collectInBackground(viewModel.uiState)

        val recent = viewModel.uiState.value.recent
        assertEquals("Food", recent[0].category?.name)
        assertNull(recent[1].category)
    }

    @Test
    fun pendingSyncCount_countsOnlyUnsyncedTransactions() = runTest {
        transactions.save(testTransaction(EXPENSE, 10.0, midMonthMillis(thisMonth, 1), pendingSync = true))
        transactions.save(testTransaction(EXPENSE, 10.0, midMonthMillis(thisMonth, 2), pendingSync = true))
        transactions.save(testTransaction(EXPENSE, 10.0, midMonthMillis(thisMonth, 3)))
        val viewModel = createViewModel()
        collectInBackground(viewModel.uiState)

        assertEquals(2, viewModel.uiState.value.pendingSyncCount)
    }

    @Test
    fun unknownCurrency_setsHasMissingRates_andIsLeftOutOfBalance() = runTest {
        transactions.save(testTransaction(INCOME, 100.0, midMonthMillis(thisMonth), currency = "XXX"))
        transactions.save(testTransaction(INCOME, 40.0, midMonthMillis(thisMonth, 1)))
        val viewModel = createViewModel()
        collectInBackground(viewModel.uiState)

        val state = viewModel.uiState.value
        assertTrue(state.hasMissingRates)
        assertEquals(40.0, state.balance, DELTA)
    }

    @Test
    fun uiState_updatesWhenRepositoryChanges() = runTest {
        val viewModel = createViewModel()
        collectInBackground(viewModel.uiState)
        assertEquals(0.0, viewModel.uiState.value.balance, DELTA)

        transactions.save(testTransaction(INCOME, 500.0, midMonthMillis(thisMonth)))

        assertEquals(500.0, viewModel.uiState.value.balance, DELTA)
    }

    private companion object {
        const val DELTA = 1e-9
    }
}