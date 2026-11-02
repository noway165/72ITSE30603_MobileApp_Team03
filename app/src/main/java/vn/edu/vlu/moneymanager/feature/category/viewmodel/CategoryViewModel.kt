package vn.edu.vlu.moneymanager.feature.category.viewmodel

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import vn.edu.vlu.moneymanager.R
import vn.edu.vlu.moneymanager.data.AppContainer
import vn.edu.vlu.moneymanager.data.model.Category
import vn.edu.vlu.moneymanager.data.model.TransactionType
import vn.edu.vlu.moneymanager.data.repository.CategoryRepository
import vn.edu.vlu.moneymanager.data.repository.TransactionRepository

data class CategoryItem(val category: Category, val usageCount: Int)

data class CategoryUiState(
    val selectedType: TransactionType = TransactionType.EXPENSE,
    val items: List<CategoryItem> = emptyList()
)

class CategoryViewModel(
    private val categoryRepository: CategoryRepository = AppContainer.categoryRepository,
    transactionRepository: TransactionRepository = AppContainer.transactionRepository
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

    // Every category of both types, kept up to date for the duplicate-name check of the dialog
    private val allCategories: StateFlow<List<Category>> =
        categoryRepository.categories.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    /**
     * Returns the error for the name field, or null when the category is being saved.
     * Validation is synchronous so the dialog can stay open; the Room write runs in viewModelScope.
     */
    @StringRes
    fun saveCategory(editingId: Long?, name: String, type: TransactionType, colorHex: String): Int? {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return R.string.error_name_required
        // Checked here so the user gets a message; the UNIQUE (name, type) index rejects it in the database too.
        val duplicate = allCategories.value.any {
            it.id != editingId && it.type == type && it.name.equals(trimmed, ignoreCase = true)
        }
        if (duplicate) return R.string.error_category_exists
        viewModelScope.launch { categoryRepository.save(Category(editingId ?: 0L, trimmed, type, colorHex)) }
        return null
    }

    /**
     * Its transactions become Uncategorized and its budgets are removed (as the confirm dialog says):
     * the foreign keys of "transactions" (ON DELETE SET NULL) and "budgets" (ON DELETE CASCADE) do this
     * inside the same SQLite delete.
     */
    fun deleteCategory(categoryId: Long) {
        viewModelScope.launch { categoryRepository.delete(categoryId) }
    }
}
