package vn.edu.vlu.moneymanager.data.repository

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import vn.edu.vlu.moneymanager.data.local.dao.BudgetDao
import vn.edu.vlu.moneymanager.data.local.entity.BudgetEntity
import vn.edu.vlu.moneymanager.data.model.Budget
import vn.edu.vlu.moneymanager.data.remote.BudgetApi
import vn.edu.vlu.moneymanager.data.remote.UiState
import vn.edu.vlu.moneymanager.data.remote.apiCall
import vn.edu.vlu.moneymanager.data.remote.toDto

/**
 * Monthly budgets live on MockAPI; the Room table "budgets" is the offline copy the screens read.
 * Like categories, writes go to the API first: without a network nothing changes and the screen shows the error.
 * Deleting a category needs nothing here: its foreign key (ON DELETE CASCADE) removes the category's budgets.
 */
class BudgetRepository(
    private val budgetDao: BudgetDao,
    private val api: BudgetApi,
    private val userId: () -> String?,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {

    val budgets: Flow<List<Budget>> = budgetDao.observeAll().map { rows -> rows.map(BudgetEntity::toModel) }

    /** Replaces the local copy with the server's list. */
    suspend fun refresh(): UiState<Unit> = apiCall(ioDispatcher) {
        val uid = userId() ?: return@apiCall
        val categoryIds = budgetDao.categoryIds().toSet()
        val rows = (ignoreNotFound { api.getAll(uid) } ?: emptyList())
            .map { it.toEntity() }
            .filter { it.categoryId in categoryIds }
            // Two devices could create the same category + month: keep one so UNIQUE does not fail the refresh
            .distinctBy { Triple(it.categoryId, it.year, it.month) }
        budgetDao.replaceAll(rows)
    }

    /** Sign-out: the next account must not see this account's budgets. */
    suspend fun clearLocal() = budgetDao.replaceAll(emptyList())

    /** Creates the budget when its id is 0, otherwise updates the row with the same id. */
    suspend fun save(budget: Budget): UiState<Budget> = apiCall(ioDispatcher) {
        val uid = checkNotNull(userId()) { "not signed in" }
        if (budget.id == 0L) {
            val created = api.create(budget.toDto(uid)).toEntity()
            budgetDao.insert(created)
            created.toModel()
        } else {
            api.update(budget.id, budget.toDto(uid))
            budgetDao.update(BudgetEntity.fromModel(budget))
            budget
        }
    }

    /** Undo of a delete: the server gave the old id away, so the budget comes back as a new row. */
    suspend fun restore(budget: Budget): UiState<Budget> = save(budget.copy(id = 0L))

    /** 404 means another device already deleted it: still remove the local copy. */
    suspend fun delete(id: Long): UiState<Unit> = apiCall(ioDispatcher) {
        ignoreNotFound { api.delete(id) }
        budgetDao.deleteById(id)
    }
}
