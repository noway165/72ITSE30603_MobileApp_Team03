package vn.edu.vlu.moneymanager.feature.budget

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import vn.edu.vlu.moneymanager.MainDispatcherRule
import vn.edu.vlu.moneymanager.R
import vn.edu.vlu.moneymanager.data.model.Budget
import vn.edu.vlu.moneymanager.data.model.BudgetProgress
import vn.edu.vlu.moneymanager.data.model.MonthKey
import vn.edu.vlu.moneymanager.data.model.TransactionType
import vn.edu.vlu.moneymanager.data.model.totalIn
import vn.edu.vlu.moneymanager.data.repository.BudgetRepository
import vn.edu.vlu.moneymanager.data.repository.CategoryRepository
import vn.edu.vlu.moneymanager.data.repository.CurrencyRepository
import vn.edu.vlu.moneymanager.data.repository.SettingsRepository
import vn.edu.vlu.moneymanager.data.repository.TransactionRepository
import vn.edu.vlu.moneymanager.feature.budget.viewmodel.BudgetEvent
import vn.edu.vlu.moneymanager.feature.budget.viewmodel.BudgetUiState
import vn.edu.vlu.moneymanager.feature.budget.viewmodel.BudgetViewModel

@OptIn(ExperimentalCoroutinesApi::class)
class BudgetViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val budgetRepository = BudgetRepository()
    private val transactionRepository = TransactionRepository()
    private val currencyRepository = CurrencyRepository()
    private val currentMonth = MonthKey.current()

    // lazy: created inside the test body, after MainDispatcherRule has replaced Dispatchers.Main
    private val viewModel by lazy {
        BudgetViewModel(budgetRepository, transactionRepository, CategoryRepository(), currencyRepository, SettingsRepository())
    }

    private fun TestScope.collectUiState() {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }
    }

    private fun TestScope.currentState(): BudgetUiState {
        advanceUntilIdle()
        return viewModel.uiState.value
    }

    // ---- validation of the budget dialog ----

    @Test
    fun saveBudget_withoutCategory_returnsCategoryRequired() = runTest {
        val before = budgetRepository.budgets.value

        val errors = viewModel.saveBudget(editingId = null, categoryId = null, limitText = "500000")

        assertEquals(R.string.error_category_required, errors.categoryError)
        assertNull(errors.limitError)
        assertEquals(before, budgetRepository.budgets.value)
    }

    @Test
    fun saveBudget_categoryAlreadyHasBudgetThisMonth_returnsDuplicateError() = runTest {
        val before = budgetRepository.budgets.value

        // Sample data already has a budget for category 1 (Food & Drinks) this month
        val errors = viewModel.saveBudget(editingId = null, categoryId = 1L, limitText = "500000")

        assertEquals(R.string.error_budget_exists, errors.categoryError)
        assertEquals(before, budgetRepository.budgets.value)
    }

    @Test
    fun saveBudget_limitZeroNegativeOrEmpty_returnsAmountError() = runTest {
        val before = budgetRepository.budgets.value

        listOf("0", "-5", "", "abc").forEach { text ->
            val errors = viewModel.saveBudget(editingId = null, categoryId = 5L, limitText = text)
            assertEquals("limit \"$text\"", R.string.error_amount_invalid, errors.limitError)
            assertNull(errors.categoryError)
        }
        assertEquals(before, budgetRepository.budgets.value)
    }

    @Test
    fun saveBudget_valid_addsBudgetForShownMonth() = runTest {
        val sizeBefore = budgetRepository.budgets.value.size

        val errors = viewModel.saveBudget(editingId = null, categoryId = 5L, limitText = "1,000,000")

        assertFalse(errors.hasErrors)
        assertEquals(sizeBefore + 1, budgetRepository.budgets.value.size)
        val saved = budgetRepository.budgets.value.last()
        assertEquals(5L, saved.categoryId)
        assertEquals(currentMonth, saved.month)
        assertEquals(1_000_000.0, saved.limit, 0.0)
    }

    @Test
    fun saveBudget_editingSameBudget_isNotADuplicate() = runTest {
        val existing = budgetRepository.budgets.value.first { it.categoryId == 1L }

        val errors = viewModel.saveBudget(editingId = existing.id, categoryId = 1L, limitText = "4000000")

        assertFalse(errors.hasErrors)
        assertEquals(4_000_000.0, budgetRepository.budgets.value.first { it.id == existing.id }.limit, 0.0)
    }

    @Test
    fun saveBudget_inAnotherMonth_sameCategoryIsAllowed() = runTest {
        collectUiState()
        viewModel.previousMonth()
        advanceUntilIdle()

        val errors = viewModel.saveBudget(editingId = null, categoryId = 1L, limitText = "500000")

        assertFalse(errors.hasErrors)
        assertEquals(currentMonth.plusMonths(-1), budgetRepository.budgets.value.last().month)
    }

    // ---- percent / progress ----

    @Test
    fun budgetProgress_percent() {
        val budget = Budget(1L, 1L, currentMonth, limit = 1_000.0)

        assertEquals(25, BudgetProgress(budget, null, spent = 250.0).percent)
        assertEquals(100, BudgetProgress(budget, null, spent = 1_000.0).percent)
        assertEquals(150, BudgetProgress(budget, null, spent = 1_500.0).percent)
        assertEquals(-500.0, BudgetProgress(budget, null, spent = 1_500.0).remaining, 0.0)
        assertEquals(0, BudgetProgress(budget.copy(limit = 0.0), null, spent = 500.0).percent)
    }

    @Test
    fun uiState_showsCurrentMonthBudgets_sortedByPercentDescending() = runTest {
        collectUiState()

        val state = currentState()
        assertEquals(currentMonth, state.month)
        assertEquals(budgetRepository.budgets.value.count { it.month == currentMonth }, state.items.size)
        val percents = state.items.map { it.percent }
        assertEquals(percents.sortedDescending(), percents)
        assertTrue(state.expenseCategories.all { it.type == TransactionType.EXPENSE })
    }

    @Test
    fun uiState_spentIsThisMonthsExpenseOfThatCategory_inDefaultCurrency() = runTest {
        collectUiState()
        // Category 5 (Entertainment) only has a USD expense this month: it must be converted to VND
        viewModel.saveBudget(editingId = null, categoryId = 5L, limitText = "2000000")

        val item = currentState().items.first { it.budget.categoryId == 5L }
        val expected = transactionRepository.transactions.value
            .filter { it.categoryId == 5L && it.type == TransactionType.EXPENSE && currentMonth.contains(it.dateMillis) }
            .totalIn("VND", currencyRepository.rates.value, TransactionType.EXPENSE).amount
        assertTrue(expected > 0)
        assertEquals(expected, item.spent, 0.001)
        assertEquals((expected * 100 / 2_000_000).toInt(), item.percent)
    }

    @Test
    fun uiState_totalPercent_isTotalSpentOverTotalLimit() = runTest {
        collectUiState()

        val state = currentState()
        assertEquals((state.totalSpent * 100 / state.totalLimit).toInt(), state.totalPercent)
        assertEquals(0, BudgetUiState().totalPercent)
    }

    @Test
    fun changingMonth_showsThatMonthsBudgets() = runTest {
        collectUiState()

        viewModel.previousMonth()
        var state = currentState()
        assertEquals(currentMonth.plusMonths(-1), state.month)
        assertTrue("sample budgets only exist for the current month", state.items.isEmpty())

        viewModel.nextMonth()
        state = currentState()
        assertEquals(currentMonth, state.month)
        assertTrue(state.items.isNotEmpty())
    }

    // ---- delete + undo ----

    @Test
    fun delete_thenUndo_restoresBudget() = runTest {
        collectUiState()
        val target = budgetRepository.budgets.value.first()

        viewModel.delete(target.id)

        assertEquals(BudgetEvent.ShowUndoDelete, viewModel.events.first())
        assertTrue(currentState().items.none { it.budget.id == target.id })

        viewModel.undoDelete()

        assertTrue(budgetRepository.budgets.value.contains(target))
        assertTrue(currentState().items.any { it.budget.id == target.id })
    }
}
