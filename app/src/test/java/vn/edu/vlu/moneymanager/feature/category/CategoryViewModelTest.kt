package vn.edu.vlu.moneymanager.feature.category

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
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
import vn.edu.vlu.moneymanager.collectInBackground
import vn.edu.vlu.moneymanager.data.model.TransactionType
import vn.edu.vlu.moneymanager.data.repository.BudgetRepository
import vn.edu.vlu.moneymanager.data.repository.CategoryRepository
import vn.edu.vlu.moneymanager.data.repository.TransactionRepository
import vn.edu.vlu.moneymanager.feature.category.viewmodel.CategoryUiState
import vn.edu.vlu.moneymanager.feature.category.viewmodel.CategoryViewModel

@OptIn(ExperimentalCoroutinesApi::class)
class CategoryViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val categoryRepository = CategoryRepository()
    private val transactionRepository = TransactionRepository()
    private val budgetRepository = BudgetRepository()

    // lazy: created inside the test body, after MainDispatcherRule has replaced Dispatchers.Main
    private val viewModel by lazy { CategoryViewModel(categoryRepository, transactionRepository, budgetRepository) }

    private fun TestScope.currentState(): CategoryUiState {
        collectInBackground(viewModel.uiState)
        advanceUntilIdle()
        return viewModel.uiState.value
    }

    // ---- list per tab ----

    @Test
    fun startsOnExpenseTab_withOnlyExpenseCategories() = runTest {
        val state = currentState()

        assertEquals(TransactionType.EXPENSE, state.selectedType)
        assertTrue(state.items.isNotEmpty())
        assertTrue(state.items.all { it.category.type == TransactionType.EXPENSE })
    }

    @Test
    fun selectType_income_showsOnlyIncomeCategories() = runTest {
        viewModel.selectType(TransactionType.INCOME)
        val state = currentState()

        assertEquals(TransactionType.INCOME, state.selectedType)
        val expected = categoryRepository.categories.value.filter { it.type == TransactionType.INCOME }
        assertEquals(expected, state.items.map { it.category })
    }

    @Test
    fun usageCount_isNumberOfTransactionsInThatCategory() = runTest {
        val state = currentState()

        state.items.forEach { item ->
            val expected = transactionRepository.transactions.value.count { it.categoryId == item.category.id }
            assertEquals(item.category.name, expected, item.usageCount)
        }
    }

    // ---- add / edit ----

    @Test
    fun saveCategory_blankName_returnsNameRequiredAndSavesNothing() = runTest {
        val before = categoryRepository.categories.value

        val error = viewModel.saveCategory(null, "   ", TransactionType.EXPENSE, "#EF4444")

        assertEquals(R.string.error_name_required, error)
        assertEquals(before, categoryRepository.categories.value)
    }

    @Test
    fun saveCategory_new_trimsNameAndAppearsInItsTab() = runTest {
        val error = viewModel.saveCategory(null, "  Pets  ", TransactionType.EXPENSE, "#22C55E")
        val state = currentState()

        assertNull(error)
        val pets = state.items.single { it.category.name == "Pets" }
        assertEquals("#22C55E", pets.category.colorHex)
        assertEquals(0, pets.usageCount)
    }

    @Test
    fun saveCategory_edit_keepsIdAndChangesValues() = runTest {
        val food = categoryRepository.categories.value.first { it.name == "Food & Drinks" }

        viewModel.saveCategory(food.id, "Eating out", TransactionType.EXPENSE, "#F97316")

        val edited = categoryRepository.get(food.id)!!
        assertEquals("Eating out", edited.name)
        assertEquals("#F97316", edited.colorHex)
        assertEquals(categoryRepository.categories.value.size, categoryRepository.categories.value.distinctBy { it.id }.size)
    }

    @Test
    fun saveCategory_changeTypeToIncome_movesItToTheIncomeTab() = runTest {
        val food = categoryRepository.categories.value.first { it.name == "Food & Drinks" }

        viewModel.saveCategory(food.id, food.name, TransactionType.INCOME, food.colorHex)

        assertFalse(currentState().items.any { it.category.id == food.id })
        viewModel.selectType(TransactionType.INCOME)
        assertTrue(currentState().items.any { it.category.id == food.id })
    }

    // ---- delete (what the confirm dialog promises) ----

    @Test
    fun deleteCategory_transactionsBecomeUncategorizedAndBudgetsAreRemoved() = runTest {
        val food = categoryRepository.categories.value.first { it.name == "Food & Drinks" }
        val foodTransactionIds = transactionRepository.transactions.value.filter { it.categoryId == food.id }.map { it.id }
        assertTrue("sample data has Food transactions", foodTransactionIds.isNotEmpty())
        assertTrue("sample data has a Food budget", budgetRepository.budgets.value.any { it.categoryId == food.id })

        viewModel.deleteCategory(food.id)

        assertNull(categoryRepository.get(food.id))
        foodTransactionIds.forEach { id ->
            val transaction = transactionRepository.get(id)!!
            assertNull(transaction.categoryId)
            assertTrue(transaction.pendingSync)
        }
        assertFalse(budgetRepository.budgets.value.any { it.categoryId == food.id })
        assertFalse(currentState().items.any { it.category.id == food.id })
    }

    @Test
    fun deleteCategory_leavesOtherCategoriesAlone() = runTest {
        val food = categoryRepository.categories.value.first { it.name == "Food & Drinks" }
        val transport = categoryRepository.categories.value.first { it.name == "Transport" }
        val transportCount = transactionRepository.transactions.value.count { it.categoryId == transport.id }

        viewModel.deleteCategory(food.id)

        assertEquals(transport, categoryRepository.get(transport.id))
        assertEquals(transportCount, transactionRepository.transactions.value.count { it.categoryId == transport.id })
        assertTrue(budgetRepository.budgets.value.any { it.categoryId == transport.id })
    }
}
