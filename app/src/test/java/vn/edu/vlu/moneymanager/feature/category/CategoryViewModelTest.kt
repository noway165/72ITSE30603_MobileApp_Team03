package vn.edu.vlu.moneymanager.feature.category

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
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
import vn.edu.vlu.moneymanager.sampleTransactionRepository
import vn.edu.vlu.moneymanager.feature.category.viewmodel.CategoryUiState
import vn.edu.vlu.moneymanager.feature.category.viewmodel.CategoryViewModel
import vn.edu.vlu.moneymanager.sampleCategoryRepository

@OptIn(ExperimentalCoroutinesApi::class)
class CategoryViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val categoryRepository = sampleCategoryRepository()
    private val transactionRepository = sampleTransactionRepository()

    // lazy: created inside the test body, after MainDispatcherRule has replaced Dispatchers.Main
    private val viewModel by lazy { CategoryViewModel(categoryRepository, transactionRepository) }

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
        val expected = categoryRepository.categories.first().filter { it.type == TransactionType.INCOME }
        assertEquals(expected, state.items.map { it.category })
    }

    @Test
    fun usageCount_isNumberOfTransactionsInThatCategory() = runTest {
        val state = currentState()

        state.items.forEach { item ->
            val expected = transactionRepository.transactions.first().count { it.categoryId == item.category.id }
            assertEquals(item.category.name, expected, item.usageCount)
        }
    }

    // ---- add / edit ----

    @Test
    fun saveCategory_blankName_returnsNameRequiredAndSavesNothing() = runTest {
        val before = categoryRepository.categories.first()

        val error = viewModel.saveCategory(null, "   ", TransactionType.EXPENSE, "#EF4444")

        assertEquals(R.string.error_name_required, error)
        assertEquals(before, categoryRepository.categories.first())
    }

    @Test
    fun saveCategory_sameNameAndTypeIgnoringCase_returnsExistsAndSavesNothing() = runTest {
        currentState()
        val before = categoryRepository.categories.first()

        val error = viewModel.saveCategory(null, "  food & drinks ", TransactionType.EXPENSE, "#EF4444")

        assertEquals(R.string.error_category_exists, error)
        assertEquals(before, categoryRepository.categories.first())
    }

    @Test
    fun saveCategory_sameNameOtherType_isAllowed() = runTest {
        currentState()

        val error = viewModel.saveCategory(null, "Food & Drinks", TransactionType.INCOME, "#EF4444")

        assertNull(error)
        assertTrue(categoryRepository.categories.first().any { it.name == "Food & Drinks" && it.type == TransactionType.INCOME })
    }

    @Test
    fun saveCategory_editKeepingItsOwnName_isNotADuplicate() = runTest {
        currentState()
        val food = categoryRepository.categories.first().first { it.name == "Food & Drinks" }

        val error = viewModel.saveCategory(food.id, food.name, food.type, "#F97316")

        assertNull(error)
        assertEquals("#F97316", categoryRepository.get(food.id)?.colorHex)
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
        val food = categoryRepository.categories.first().first { it.name == "Food & Drinks" }

        viewModel.saveCategory(food.id, "Eating out", TransactionType.EXPENSE, "#F97316")

        val edited = categoryRepository.get(food.id)!!
        assertEquals("Eating out", edited.name)
        assertEquals("#F97316", edited.colorHex)
        assertEquals(categoryRepository.categories.first().size, categoryRepository.categories.first().distinctBy { it.id }.size)
    }

    @Test
    fun saveCategory_changeTypeToIncome_movesItToTheIncomeTab() = runTest {
        val food = categoryRepository.categories.first().first { it.name == "Food & Drinks" }

        viewModel.saveCategory(food.id, food.name, TransactionType.INCOME, food.colorHex)

        assertFalse(currentState().items.any { it.category.id == food.id })
        viewModel.selectType(TransactionType.INCOME)
        assertTrue(currentState().items.any { it.category.id == food.id })
    }

    // ---- delete (what the confirm dialog promises) ----
    // "Its transactions become Uncategorized, its budgets are removed" is done by the SQLite foreign keys
    // (ON DELETE SET NULL / CASCADE), so it is tested on a real database in androidTest: TransactionDaoTest, BudgetDaoTest.

    @Test
    fun deleteCategory_removesItFromTheListAndTheRepository() = runTest {
        val food = categoryRepository.categories.first().first { it.name == "Food & Drinks" }

        viewModel.deleteCategory(food.id)

        assertNull(categoryRepository.get(food.id))
        assertFalse(currentState().items.any { it.category.id == food.id })
    }

    @Test
    fun deleteCategory_leavesOtherCategoriesAlone() = runTest {
        val food = categoryRepository.categories.first().first { it.name == "Food & Drinks" }
        val transport = categoryRepository.categories.first().first { it.name == "Transport" }
        val transportCount = transactionRepository.transactions.first().count { it.categoryId == transport.id }

        viewModel.deleteCategory(food.id)

        assertEquals(transport, categoryRepository.get(transport.id))
        assertEquals(transportCount, transactionRepository.transactions.first().count { it.categoryId == transport.id })
    }
}
