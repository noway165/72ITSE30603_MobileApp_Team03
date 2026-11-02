package vn.edu.vlu.moneymanager

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import vn.edu.vlu.moneymanager.data.local.dao.BudgetDao
import vn.edu.vlu.moneymanager.data.local.entity.BudgetEntity
import vn.edu.vlu.moneymanager.data.source.SampleData

/**
 * In-memory BudgetDao for JVM unit tests (no Android, no SQLite). Behaves like the Room table:
 * auto ids, rows ordered by id, observeAll() re-emits after each write, UNIQUE (category_id, year, month).
 * Foreign keys (ON DELETE CASCADE) only exist in SQLite: they are tested in androidTest/.../BudgetDaoTest.
 */
class FakeBudgetDao(
    initial: List<BudgetEntity> = SampleData.budgets.map(BudgetEntity::fromModel)
) : BudgetDao {

    private val rows = MutableStateFlow(initial)
    private var nextId = (initial.maxOfOrNull { it.id } ?: 0L) + 1

    override fun observeAll(): Flow<List<BudgetEntity>> = rows.map { list -> list.sortedBy { it.id } }

    override suspend fun insert(budget: BudgetEntity): Long {
        check(rows.value.none { it.categoryId == budget.categoryId && it.year == budget.year && it.month == budget.month }) {
            "UNIQUE constraint failed: budgets.category_id, budgets.year, budgets.month"
        }
        val id = if (budget.id == 0L) nextId++ else budget.id
        rows.update { it + budget.copy(id = id) }
        return id
    }

    override suspend fun update(budget: BudgetEntity) {
        rows.update { list -> list.map { if (it.id == budget.id) budget else it } }
    }

    override suspend fun deleteById(id: Long) {
        rows.update { list -> list.filterNot { it.id == id } }
    }
}
