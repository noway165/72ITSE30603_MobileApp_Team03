package vn.edu.vlu.moneymanager.feature.category.viewmodel

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import vn.edu.vlu.moneymanager.R
import vn.edu.vlu.moneymanager.data.AppContainer
import vn.edu.vlu.moneymanager.data.model.Category
import vn.edu.vlu.moneymanager.data.model.TransactionType
import vn.edu.vlu.moneymanager.data.repository.BudgetRepository
import vn.edu.vlu.moneymanager.data.repository.CategoryRepository
import vn.edu.vlu.moneymanager.data.repository.TransactionRepository

data class CategoryItem(val category: Category, val usageCount: Int)

data class CategoryUiState(
    val selectedType: TransactionType = TransactionType.EXPENSE,
    val items: List<CategoryItem> = emptyList()
)

class CategoryViewModel(
    private val categoryRepository: CategoryRepository = AppContainer.categoryRepository,
    private val transactionRepository: TransactionRepository = AppContainer.transactionRepository,
    private val budgetRepository: BudgetRepository = AppContainer.budgetRepository
) : ViewModel() {

    private val selectedType = MutableStateFlow(TransactionType.EXPENSE)

    val uiState: StateFlow<CategoryUiState> = combine(
        selectedType,
        categoryRepository.categories,
        transactionRepository.transactions
    ) { type, categories, transactions ->
        CategoryUiState(
            selectedType = type,
            items = categories.filter { it.type == type }.map { category ->
                CategoryItem(category, transactions.count { it.categoryId == category.id })
            }
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CategoryUiState())

    fun selectType(type: TransactionType) {
        selectedType.value = type
    }

    /** Returns the error for the name field, or null when the category was saved. */
    @StringRes
    fun saveCategory(editingId: Long?, name: String, type: TransactionType, colorHex: String): Int? {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return R.string.error_name_required
        categoryRepository.save(Category(editingId ?: 0L, trimmed, type, colorHex))
        return null
    }

    /** Its transactions become Uncategorized and its budgets are removed (as the confirm dialog says). */
    fun deleteCategory(categoryId: Long) {
        transactionRepository.clearCategory(categoryId)
        budgetRepository.deleteByCategory(categoryId)
        categoryRepository.delete(categoryId)
    }
}
