package vn.edu.vlu.moneymanager.feature.transaction.viewmodel

import androidx.annotation.StringRes
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import vn.edu.vlu.moneymanager.R
import vn.edu.vlu.moneymanager.data.AppContainer
import vn.edu.vlu.moneymanager.data.model.Category
import vn.edu.vlu.moneymanager.data.model.Transaction
import vn.edu.vlu.moneymanager.data.model.TransactionType
import vn.edu.vlu.moneymanager.util.DateUtils
import vn.edu.vlu.moneymanager.util.Validators

/** What the user has typed so far. Kept in the ViewModel so it survives rotation. */
data class TransactionForm(
    val type: TransactionType = TransactionType.EXPENSE,
    val amount: String = "",
    val currency: String = "",
    val categoryId: Long? = null,
    val dateMillis: Long = System.currentTimeMillis(),
    val note: String = "",
    @param:StringRes val amountError: Int? = null,
    @param:StringRes val categoryError: Int? = null
)

data class AddEditTransactionUiState(
    val isEditMode: Boolean = false,
    val form: TransactionForm = TransactionForm(),
    /** Categories of the selected type, for the category dropdown */
    val categories: List<Category> = emptyList(),
    val currencies: List<String> = emptyList()
) {
    val selectedCategory: Category? get() = categories.firstOrNull { it.id == form.categoryId }
}

sealed interface AddEditTransactionEvent {
    data class Finished(@param:StringRes val messageRes: Int) : AddEditTransactionEvent
}

/**
 * The transaction id arrives through [SavedStateHandle]: the default ViewModel factory copies the
 * Activity's Intent extras into it, so the ViewModel never touches the Intent or the Activity.
 */
class AddEditTransactionViewModel(savedStateHandle: SavedStateHandle) : ViewModel() {

    private val transactionRepository = AppContainer.transactionRepository
    private val categoryRepository = AppContainer.categoryRepository
    private val currencyRepository = AppContainer.currencyRepository

    private val transactionId: Long = savedStateHandle[EXTRA_TRANSACTION_ID] ?: NO_ID
    private val existing: Transaction? = transactionRepository.get(transactionId)

    private val form = MutableStateFlow(
        existing?.let {
            TransactionForm(
                type = it.type,
                amount = formatAmountForEditing(it.amount),
                currency = it.currency,
                categoryId = it.categoryId,
                dateMillis = it.dateMillis,
                note = it.note
            )
        } ?: TransactionForm(currency = AppContainer.settingsRepository.defaultCurrency.value)
    )

    private val _events = Channel<AddEditTransactionEvent>(Channel.BUFFERED)
    val events: Flow<AddEditTransactionEvent> = _events.receiveAsFlow()

    val uiState: StateFlow<AddEditTransactionUiState> = combine(form, categoryRepository.categories) { form, categories ->
        AddEditTransactionUiState(
            isEditMode = existing != null,
            form = form,
            categories = categories.filter { it.type == form.type },
            currencies = currencyRepository.currencies
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        AddEditTransactionUiState(isEditMode = existing != null, form = form.value)
    )

    fun onTypeSelected(type: TransactionType) = form.update {
        if (it.type == type) it else it.copy(type = type, categoryId = null, categoryError = null)
    }

    // EditText restores its text after rotation and fires the watcher again: same text must not clear the error.
    fun onAmountChanged(text: String) = form.update { if (it.amount == text) it else it.copy(amount = text, amountError = null) }

    fun onCurrencySelected(code: String) = form.update { it.copy(currency = code) }

    fun onCategorySelected(categoryId: Long) = form.update { it.copy(categoryId = categoryId, categoryError = null) }

    fun onDatePicked(utcMillis: Long) = form.update { it.copy(dateMillis = DateUtils.fromPickerUtc(utcMillis)) }

    fun onNoteChanged(text: String) = form.update { it.copy(note = text) }

    fun save() {
        val current = form.value
        val amount = Validators.parseAmount(current.amount)
        val amountError = if (amount == null) R.string.error_amount_invalid else null
        val categoryError = if (current.categoryId == null) R.string.error_category_required else null
        form.update { it.copy(amountError = amountError, categoryError = categoryError) }
        if (amount == null || categoryError != null) return

        transactionRepository.save(
            Transaction(
                id = existing?.id ?: 0L,
                type = current.type,
                amount = amount,
                currency = current.currency,
                categoryId = current.categoryId,
                dateMillis = current.dateMillis,
                note = current.note.trim(),
                pendingSync = true
            )
        )
        viewModelScope.launch { _events.send(AddEditTransactionEvent.Finished(R.string.transaction_saved)) }
    }

    fun delete() {
        existing?.let { transactionRepository.delete(it.id) }
        viewModelScope.launch { _events.send(AddEditTransactionEvent.Finished(R.string.transaction_deleted)) }
    }

    private fun formatAmountForEditing(amount: Double): String =
        if (amount % 1.0 == 0.0) amount.toLong().toString() else amount.toString()

    companion object {
        const val EXTRA_TRANSACTION_ID = "transaction_id"
        private const val NO_ID = -1L
    }
}
