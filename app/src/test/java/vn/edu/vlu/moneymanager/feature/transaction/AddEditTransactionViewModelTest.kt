package vn.edu.vlu.moneymanager.feature.transaction

import androidx.lifecycle.SavedStateHandle
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
import vn.edu.vlu.moneymanager.data.AppContainer
import vn.edu.vlu.moneymanager.data.model.Transaction
import vn.edu.vlu.moneymanager.data.model.TransactionType
import vn.edu.vlu.moneymanager.feature.transaction.viewmodel.AddEditTransactionEvent
import vn.edu.vlu.moneymanager.feature.transaction.viewmodel.AddEditTransactionUiState
import vn.edu.vlu.moneymanager.feature.transaction.viewmodel.AddEditTransactionViewModel

/**
 * AddEditTransactionViewModel takes its repositories from AppContainer (shared by every test),
 * so each test compares against the repository state it reads first instead of fixed sizes.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AddEditTransactionViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repo = AppContainer.transactionRepository

    private fun TestScope.collectUiState(viewModel: AddEditTransactionViewModel) {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }
    }

    private fun TestScope.currentState(viewModel: AddEditTransactionViewModel): AddEditTransactionUiState {
        advanceUntilIdle()
        return viewModel.uiState.value
    }

    @Test
    fun newTransaction_startsEmptyInDefaultCurrency() = runTest {
        val viewModel = AddEditTransactionViewModel(SavedStateHandle())
        collectUiState(viewModel)

        val state = currentState(viewModel)
        assertFalse(state.isEditMode)
        assertEquals("", state.form.amount)
        assertEquals("VND", state.form.currency)
        assertEquals(TransactionType.EXPENSE, state.form.type)
        assertTrue(state.categories.all { it.type == TransactionType.EXPENSE })
    }

    @Test
    fun save_withoutAmountAndCategory_showsBothErrors_andSavesNothing() = runTest {
        val viewModel = AddEditTransactionViewModel(SavedStateHandle())
        collectUiState(viewModel)
        val before = repo.transactions.value

        viewModel.save()

        val form = currentState(viewModel).form
        assertEquals(R.string.error_amount_invalid, form.amountError)
        assertEquals(R.string.error_category_required, form.categoryError)
        assertEquals(before, repo.transactions.value)
    }

    @Test
    fun save_withZeroAmount_showsAmountError() = runTest {
        val viewModel = AddEditTransactionViewModel(SavedStateHandle())
        collectUiState(viewModel)
        val before = repo.transactions.value

        viewModel.onAmountChanged("0")
        viewModel.onCategorySelected(1L)
        viewModel.save()

        val form = currentState(viewModel).form
        assertEquals(R.string.error_amount_invalid, form.amountError)
        assertNull(form.categoryError)
        assertEquals(before, repo.transactions.value)
    }

    @Test
    fun save_withoutCategory_showsOnlyCategoryError() = runTest {
        val viewModel = AddEditTransactionViewModel(SavedStateHandle())
        collectUiState(viewModel)
        val before = repo.transactions.value

        viewModel.onAmountChanged("50000")
        viewModel.save()

        val form = currentState(viewModel).form
        assertNull(form.amountError)
        assertEquals(R.string.error_category_required, form.categoryError)
        assertEquals(before, repo.transactions.value)
    }

    @Test
    fun sameAmountText_keepsError_newTextClearsIt() = runTest {
        val viewModel = AddEditTransactionViewModel(SavedStateHandle())
        collectUiState(viewModel)
        viewModel.save()

        // Rotation: the EditText restores the same (empty) text and fires the watcher again
        viewModel.onAmountChanged("")
        assertEquals(R.string.error_amount_invalid, currentState(viewModel).form.amountError)

        viewModel.onAmountChanged("1")
        assertNull(currentState(viewModel).form.amountError)
    }

    @Test
    fun switchingType_clearsCategory_andShowsOnlyThatTypesCategories() = runTest {
        val viewModel = AddEditTransactionViewModel(SavedStateHandle())
        collectUiState(viewModel)
        viewModel.onCategorySelected(1L)

        viewModel.onTypeSelected(TransactionType.INCOME)

        val state = currentState(viewModel)
        assertNull(state.form.categoryId)
        assertTrue(state.categories.isNotEmpty())
        assertTrue(state.categories.all { it.type == TransactionType.INCOME })
    }

    @Test
    fun save_validNewTransaction_addsItAndFinishes() = runTest {
        val viewModel = AddEditTransactionViewModel(SavedStateHandle())
        collectUiState(viewModel)
        val sizeBefore = repo.transactions.value.size
        val maxIdBefore = repo.transactions.value.maxOf { it.id }

        viewModel.onAmountChanged("85,000")
        viewModel.onCategorySelected(1L)
        viewModel.onNoteChanged("  Lunch  ")
        viewModel.save()

        assertEquals(AddEditTransactionEvent.Finished(R.string.transaction_saved), viewModel.events.first())
        assertEquals(sizeBefore + 1, repo.transactions.value.size)
        val saved = repo.transactions.value.first { it.id > maxIdBefore }
        assertEquals(85_000.0, saved.amount, 0.0)
        assertEquals(1L, saved.categoryId)
        assertEquals("Lunch", saved.note)
        assertEquals(TransactionType.EXPENSE, saved.type)
        assertTrue(saved.pendingSync)
    }

    @Test
    fun editMode_loadsExistingTransaction_andSaveUpdatesIt() = runTest {
        val existing = repo.get(1L)!!
        val sizeBefore = repo.transactions.value.size
        val viewModel = AddEditTransactionViewModel(SavedStateHandle(mapOf("transaction_id" to 1L)))
        collectUiState(viewModel)

        var state = currentState(viewModel)
        assertTrue(state.isEditMode)
        assertEquals(existing.type, state.form.type)
        assertEquals(existing.categoryId, state.form.categoryId)
        assertEquals(existing.note, state.form.note)
        assertEquals(existing.amount, state.form.amount.toDouble(), 0.0)

        viewModel.onAmountChanged("123456")
        viewModel.save()

        assertEquals(AddEditTransactionEvent.Finished(R.string.transaction_saved), viewModel.events.first())
        assertEquals(sizeBefore, repo.transactions.value.size)
        assertEquals(123_456.0, repo.get(1L)!!.amount, 0.0)
        state = currentState(viewModel)
        assertTrue(state.isEditMode)
    }

    @Test
    fun delete_inEditMode_removesTransactionAndFinishes() = runTest {
        val created = repo.save(
            Transaction(0L, TransactionType.EXPENSE, 10_000.0, "VND", 2L, System.currentTimeMillis(), "To delete")
        )
        val viewModel = AddEditTransactionViewModel(
            SavedStateHandle(mapOf(AddEditTransactionViewModel.EXTRA_TRANSACTION_ID to created.id))
        )

        viewModel.delete()

        assertEquals(AddEditTransactionEvent.Finished(R.string.transaction_deleted), viewModel.events.first())
        assertNull(repo.get(created.id))
    }
}
