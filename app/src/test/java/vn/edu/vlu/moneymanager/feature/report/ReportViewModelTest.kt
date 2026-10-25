package vn.edu.vlu.moneymanager.feature.report

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
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
import vn.edu.vlu.moneymanager.feature.report.viewmodel.ReportViewModel
import vn.edu.vlu.moneymanager.midMonthMillis
import vn.edu.vlu.moneymanager.testTransaction
import vn.edu.vlu.moneymanager.usdSettings

class ReportViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val transactions = emptyTransactionRepository()
    private val categories = emptyCategoryRepository()
    private val settings = usdSettings()
    private val thisMonth = MonthKey.current()
    private val lastMonth = thisMonth.plusMonths(-1)

    private fun createViewModel() =
        ReportViewModel(transactions, categories, CurrencyRepository(), settings)

    @Test
    fun startsOnCurrentMonth() = runTest {
        val viewModel = createViewModel()
        collectInBackground(viewModel.uiState)

        assertEquals(thisMonth, viewModel.uiState.value.month)
    }

    @Test
    fun previousAndNextMonth_changeShownMonth() = runTest {
        val viewModel = createViewModel()
        collectInBackground(viewModel.uiState)

        viewModel.previousMonth()
        assertEquals(lastMonth, viewModel.uiState.value.month)

        viewModel.nextMonth()
        viewModel.nextMonth()
        assertEquals(thisMonth.plusMonths(1), viewModel.uiState.value.month)
    }

    @Test
    fun incomeAndExpense_coverOnlyTheShownMonth() = runTest {
        transactions.save(testTransaction(INCOME, 500.0, midMonthMillis(thisMonth)))
        transactions.save(testTransaction(EXPENSE, 200.0, midMonthMillis(thisMonth, 1)))
        transactions.save(testTransaction(EXPENSE, 999.0, midMonthMillis(lastMonth)))
        val viewModel = createViewModel()
        collectInBackground(viewModel.uiState)

        assertEquals(500.0, viewModel.uiState.value.income, DELTA)
        assertEquals(200.0, viewModel.uiState.value.expense, DELTA)

        viewModel.previousMonth()

        assertEquals(0.0, viewModel.uiState.value.income, DELTA)
        assertEquals(999.0, viewModel.uiState.value.expense, DELTA)
    }

    @Test
    fun slices_groupExpensesByCategory_sortedDescending_withoutZeroOrIncome() = runTest {
        val food = categories.save(Category(0, "Food", EXPENSE, "#FF0000"))
        val rent = categories.save(Category(0, "Rent", EXPENSE, "#00FF00"))
        val unused = categories.save(Category(0, "Unused", EXPENSE, "#0000FF"))
        val at = midMonthMillis(thisMonth)
        transactions.save(testTransaction(EXPENSE, 100.0, at, categoryId = food.id))
        transactions.save(testTransaction(EXPENSE, 50.0, at, categoryId = food.id))
        transactions.save(testTransaction(EXPENSE, 300.0, at, categoryId = rent.id))
        transactions.save(testTransaction(EXPENSE, 20.0, at))                          // uncategorized
        transactions.save(testTransaction(EXPENSE, 0.0, at, categoryId = unused.id))   // amount 0: dropped
        transactions.save(testTransaction(INCOME, 700.0, at, categoryId = food.id))    // income: not a slice
        val viewModel = createViewModel()
        collectInBackground(viewModel.uiState)

        val slices = viewModel.uiState.value.slices
        assertEquals(listOf("Rent", "Food", null), slices.map { it.categoryName })
        assertEquals(listOf(300.0, 150.0, 20.0), slices.map { it.amount })
    }

    @Test
    fun bars_coverSixMonthsEndingAtShownMonth() = runTest {
        transactions.save(testTransaction(EXPENSE, 200.0, midMonthMillis(thisMonth)))
        transactions.save(testTransaction(INCOME, 40.0, midMonthMillis(lastMonth)))
        val viewModel = createViewModel()
        collectInBackground(viewModel.uiState)

        val bars = viewModel.uiState.value.bars
        assertEquals(6, bars.size)
        assertEquals(thisMonth.plusMonths(-5), bars.first().month)
        assertEquals(thisMonth, bars.last().month)
        assertEquals(200.0, bars.last().expense, DELTA)
        assertEquals(40.0, bars[4].income, DELTA)

        viewModel.previousMonth()

        assertEquals(lastMonth, viewModel.uiState.value.bars.last().month)
    }

    @Test
    fun unknownCurrency_setsHasMissingRates() = runTest {
        transactions.save(testTransaction(EXPENSE, 10.0, midMonthMillis(thisMonth), currency = "XXX"))
        val viewModel = createViewModel()
        collectInBackground(viewModel.uiState)

        assertTrue(viewModel.uiState.value.hasMissingRates)
    }

    private companion object {
        const val DELTA = 1e-9
    }
}