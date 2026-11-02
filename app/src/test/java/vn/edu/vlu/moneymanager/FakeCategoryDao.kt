package vn.edu.vlu.moneymanager

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import vn.edu.vlu.moneymanager.data.local.dao.CategoryDao
import vn.edu.vlu.moneymanager.data.local.entity.CategoryEntity
import vn.edu.vlu.moneymanager.data.source.SampleData

/**
 * In-memory CategoryDao for JVM unit tests (no Android, no SQLite). Behaves like the Room table:
 * auto ids, rows ordered by id, observeAll() re-emits after each write, UNIQUE (name, type).
 * The real DAO is tested on a device in androidTest/.../CategoryDaoTest.
 */
class FakeCategoryDao(
    initial: List<CategoryEntity> = SampleData.categories.map(CategoryEntity::fromModel)
) : CategoryDao {

    private val rows = MutableStateFlow(initial)
    private var nextId = (initial.maxOfOrNull { it.id } ?: 0L) + 1

    override fun observeAll(): Flow<List<CategoryEntity>> = rows.map { list -> list.sortedBy { it.id } }

    override suspend fun getById(id: Long): CategoryEntity? = rows.value.firstOrNull { it.id == id }

    override suspend fun insert(category: CategoryEntity): Long {
        check(rows.value.none { it.name == category.name && it.type == category.type }) {
            "UNIQUE constraint failed: categories.name, categories.type"
        }
        val id = if (category.id == 0L) nextId++ else category.id
        rows.update { it + category.copy(id = id) }
        return id
    }

    override suspend fun update(category: CategoryEntity) {
        rows.update { list -> list.map { if (it.id == category.id) category else it } }
    }

    override suspend fun deleteById(id: Long) {
        rows.update { list -> list.filterNot { it.id == id } }
    }
}
