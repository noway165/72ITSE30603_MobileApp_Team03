package vn.edu.vlu.moneymanager.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import vn.edu.vlu.moneymanager.data.local.dao.CategoryDao
import vn.edu.vlu.moneymanager.data.local.entity.CategoryEntity
import vn.edu.vlu.moneymanager.data.model.Category

/**
 * Categories stored in the Room table "categories". The rest of the app only sees the [Category] model:
 * entities stay inside the data layer.
 */
class CategoryRepository(private val categoryDao: CategoryDao) {

    /** Re-emits after every insert / update / delete, so all screens stay in sync. */
    val categories: Flow<List<Category>> = categoryDao.observeAll().map { rows -> rows.map(CategoryEntity::toModel) }

    suspend fun get(id: Long?): Category? = id?.let { categoryDao.getById(it)?.toModel() }

    /** Inserts when [category] has id 0 (Room assigns the id), otherwise updates the row with the same id. */
    suspend fun save(category: Category): Category {
        val entity = CategoryEntity.fromModel(category)
        return if (category.id == 0L) {
            category.copy(id = categoryDao.insert(entity))
        } else {
            categoryDao.update(entity)
            category
        }
    }

    suspend fun delete(id: Long) {
        categoryDao.deleteById(id)
    }
}
