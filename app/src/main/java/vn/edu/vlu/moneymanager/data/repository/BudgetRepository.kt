package vn.edu.vlu.moneymanager.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import vn.edu.vlu.moneymanager.data.local.dao.BudgetDao
import vn.edu.vlu.moneymanager.data.local.entity.BudgetEntity
import vn.edu.vlu.moneymanager.data.model.Budget

/**
 * Monthly budgets stored in the Room table "budgets".
 * Deleting a category needs nothing here: its foreign key (ON DELETE CASCADE) removes the category's budgets.
 */
class BudgetRepository(private val budgetDao: BudgetDao) {

    val budgets: Flow<List<Budget>> = budgetDao.observeAll().map { rows -> rows.map(BudgetEntity::toModel) }

    /** Inserts when [budget] has id 0 (Room assigns the id), otherwise updates the row with the same id. */
    suspend fun save(budget: Budget): Budget {
        val entity = BudgetEntity.fromModel(budget)
        return if (budget.id == 0L) {
            budget.copy(id = budgetDao.insert(entity))
        } else {
            budgetDao.update(entity)
            budget
        }
    }

    /** Puts a deleted budget back with its old id (undo). */
    suspend fun restore(budget: Budget) {
        budgetDao.insert(BudgetEntity.fromModel(budget))
    }

    suspend fun delete(id: Long) {
        budgetDao.deleteById(id)
    }
}
