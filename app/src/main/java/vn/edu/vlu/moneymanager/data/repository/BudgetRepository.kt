package vn.edu.vlu.moneymanager.data.repository

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import vn.edu.vlu.moneymanager.data.model.Budget
import vn.edu.vlu.moneymanager.data.source.SampleData

class BudgetRepository {

    private val _budgets = MutableStateFlow(SampleData.budgets)
    val budgets: StateFlow<List<Budget>> = _budgets.asStateFlow()

    private var nextId = (SampleData.budgets.maxOfOrNull { it.id } ?: 0L) + 1

    fun save(budget: Budget): Budget {
        val saved = if (budget.id == 0L) budget.copy(id = nextId++) else budget
        _budgets.update { list ->
            if (list.any { it.id == saved.id }) list.map { if (it.id == saved.id) saved else it } else list + saved
        }
        return saved
    }

    fun delete(id: Long): Budget? {
        val removed = _budgets.value.firstOrNull { it.id == id } ?: return null
        _budgets.update { list -> list.filterNot { it.id == id } }
        return removed
    }

    fun deleteByCategory(categoryId: Long) {
        _budgets.update { list -> list.filterNot { it.categoryId == categoryId } }
    }
}
