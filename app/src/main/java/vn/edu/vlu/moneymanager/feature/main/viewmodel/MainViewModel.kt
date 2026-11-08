package vn.edu.vlu.moneymanager.feature.main.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import vn.edu.vlu.moneymanager.data.AppContainer
import vn.edu.vlu.moneymanager.data.repository.CategoryRepository

/**
 * Syncs shared data once when the signed-in app opens. Errors are ignored here: every screen still has
 * the Room copy and shows its own banner. Categories come first because transactions and budgets reference them.
 */
class MainViewModel(
    private val categoryRepository: CategoryRepository = AppContainer.categoryRepository
) : ViewModel() {

    init {
        viewModelScope.launch { categoryRepository.refresh() }
    }
}
