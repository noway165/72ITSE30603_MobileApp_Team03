package vn.edu.vlu.moneymanager.feature.main.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import vn.edu.vlu.moneymanager.data.AppContainer
import vn.edu.vlu.moneymanager.data.repository.BudgetRepository
import vn.edu.vlu.moneymanager.data.repository.CategoryRepository
import vn.edu.vlu.moneymanager.data.repository.TransactionRepository
import vn.edu.vlu.moneymanager.data.repository.syncAll

/**
 * Syncs shared data once when the signed-in app opens. Errors are ignored here: every screen still has
 * the Room copy and shows its own banner. Categories come first because transactions and budgets reference them.
 */
class MainViewModel(
    private val categoryRepository: CategoryRepository = AppContainer.categoryRepository,
    private val transactionRepository: TransactionRepository = AppContainer.transactionRepository,
    private val budgetRepository: BudgetRepository = AppContainer.budgetRepository
) : ViewModel() {

    init {
        viewModelScope.launch { syncAll(categoryRepository, transactionRepository, budgetRepository) }
    }
}
