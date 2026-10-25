package vn.edu.vlu.moneymanager.feature.transaction

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
import vn.edu.vlu.moneymanager.data.model.TransactionType
import vn.edu.vlu.moneymanager.data.repository.CategoryRepository
import vn.edu.vlu.moneymanager.data.repository.CurrencyRepository
import vn.edu.vlu.moneymanager.data.repository.SettingsRepository
import vn.edu.vlu.moneymanager.data.repository.TransactionRepository
import vn.edu.vlu.moneymanager.feature.transaction.viewmodel.TransactionListEvent
import vn.edu.vlu.moneymanager.feature.transaction.viewmodel.TransactionListUiState
import vn.edu.vlu.moneymanager.feature.transaction.viewmodel.TransactionListViewModel
import vn.edu.vlu.moneymanager.util.DateUtils
import java.util.Calendar

@OptIn(ExperimentalCoroutinesApi::class)
class TransactionListViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val transactionRepository = TransactionRepository()
    // lazy: created inside the test body, after MainDispatcherRule has replaced Dispatchers.Main
    private val viewModel by lazy {
        TransactionListViewModel(transactionRepository, CategoryRepository(), CurrencyRepository(), SettingsRepository())
    }

    /** uiState uses WhileSubscribed: it only updates while something collects it. */
    private fun TestScope.collectUiState() {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }
    }

    private fun TestScope.currentState(): TransactionListUiState {
        advanceUntilIdle()
        return viewModel.uiState.value
    }

    @Test
    fun noFilter_showsEveryTransaction() = runTest {
        collectUiState()

        val state = currentState()
        assertEquals(transactionRepository.transactions.value.map { it.id }, state.items.map { it.transaction.id })
        assertTrue(state.hasAnyTransactions)
        assertFalse(state.filter.isActive)
        assertEquals("VND", state.currency)
    }

    @Test
    fun filterByType_keepsOnlyThatType() = runTest {
        collectUiState()

        viewModel.setType(TransactionType.INCOME)

        val state = currentState()
        val expected = transactionRepository.transactions.value.count { it.type == TransactionType.INCOME }
        assertEquals(expected, state.items.size)
        assertTrue(state.items.all { it.transaction.type == TransactionType.INCOME })
        assertTrue(state.filter.isActive)
        assertTrue("only income -> net is positive", state.net > 0)
    }

    @Test
    fun filterByCategory_keepsOnlyThatCategory() = runTest {
        collectUiState()

        viewModel.setCategory(1L)

        val state = currentState()
        val expected = transactionRepository.transactions.value.count { it.categoryId == 1L }
        assertEquals(expected, state.items.size)
        assertTrue(state.items.all { it.transaction.categoryId == 1L })
        assertEquals("Food & Drinks", state.selectedCategory?.name)
    }

    @Test
    fun filterByTypeAndCategory_canBeEmpty_butHasAnyTransactionsStaysTrue() = runTest {
        collectUiState()

        viewModel.setType(TransactionType.EXPENSE)
        viewModel.setCategory(6L) // Salary is an income category

        val state = currentState()
        assertTrue(state.items.isEmpty())
        assertTrue(state.hasAnyTransactions)
    }

    @Test
    fun searchQuery_matchesNoteIgnoringCaseAndSpaces() = runTest {
        collectUiState()

        viewModel.setQuery("  GROCERIES ")

        val state = currentState()
        val expected = transactionRepository.transactions.value.count { it.note.contains("groceries", ignoreCase = true) }
        assertTrue(expected > 0)
        assertEquals(expected, state.items.size)
    }

    @Test
    fun searchQuery_matchesCategoryName() = runTest {
        collectUiState()

        viewModel.setQuery("transport")

        val state = currentState()
        val expected = transactionRepository.transactions.value.count { it.categoryId == 2L }
        assertEquals(expected, state.items.size)
        assertTrue(state.items.all { it.transaction.categoryId == 2L })
    }

    @Test
    fun dateRange_keepsOnlyThatDay_andNullClearsIt() = runTest {
        collectUiState()
        val target = transactionRepository.transactions.value.first()
        val pickerDay = DateUtils.toPickerUtc(target.dateMillis)

        viewModel.setDateRange(pickerDay, pickerDay)

        var state = currentState()
        val expected = transactionRepository.transactions.value
            .filter { sameLocalDay(it.dateMillis, target.dateMillis) }
            .map { it.id }
        assertEquals(expected, state.items.map { it.transaction.id })
        assertTrue(state.filter.isActive)

        viewModel.setDateRange(null, null)

        state = currentState()
        assertNull(state.filter.dateFrom)
        assertEquals(transactionRepository.transactions.value.size, state.items.size)
    }

    @Test
    fun delete_removesRowAndSendsUndoEvent() = runTest {
        collectUiState()
        val target = transactionRepository.transactions.value.first()

        viewModel.delete(target.id)

        assertEquals(TransactionListEvent.ShowUndoDelete, viewModel.events.first())
        assertNull(transactionRepository.get(target.id))
        assertTrue(currentState().items.none { it.transaction.id == target.id })
    }

    @Test
    fun undoDelete_restoresTheSameTransaction() = runTest {
        collectUiState()
        val target = transactionRepository.transactions.value.first()
        val sizeBefore = transactionRepository.transactions.value.size

        viewModel.delete(target.id)
        viewModel.undoDelete()

        assertEquals(target, transactionRepository.get(target.id))
        assertEquals(sizeBefore, currentState().items.size)
    }

    @Test
    fun delete_unknownId_changesNothing() = runTest {
        collectUiState()
        val before = transactionRepository.transactions.value

        viewModel.delete(9_999L)
        viewModel.undoDelete()

        assertEquals(before, transactionRepository.transactions.value)
    }

    private fun sameLocalDay(a: Long, b: Long): Boolean {
        val first = Calendar.getInstance().apply { timeInMillis = a }
        val second = Calendar.getInstance().apply { timeInMillis = b }
        return first.get(Calendar.YEAR) == second.get(Calendar.YEAR) &&
            first.get(Calendar.DAY_OF_YEAR) == second.get(Calendar.DAY_OF_YEAR)
    }
}
