package vn.edu.vlu.moneymanager.data.repository

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import retrofit2.HttpException
import vn.edu.vlu.moneymanager.data.local.dao.CategoryDao
import vn.edu.vlu.moneymanager.data.local.entity.CategoryEntity
import vn.edu.vlu.moneymanager.data.model.Category
import vn.edu.vlu.moneymanager.data.remote.CategoryApi
import vn.edu.vlu.moneymanager.data.remote.CategoryDto
import vn.edu.vlu.moneymanager.data.remote.UiState
import vn.edu.vlu.moneymanager.data.remote.apiCall
import vn.edu.vlu.moneymanager.data.remote.toDto
import vn.edu.vlu.moneymanager.data.source.SampleData

/**
 * Categories live on MockAPI; the Room table "categories" is the offline copy the screens read.
 * The Room id is the MockAPI id, so foreign keys stay valid after a reinstall.
 * Writes go to the API first: without a network nothing changes locally and the screen shows the error.
 */
class CategoryRepository(
    private val categoryDao: CategoryDao,
    private val api: CategoryApi,
    private val userId: () -> String?,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {

    /** Re-emits after every insert / update / delete, so all screens stay in sync. */
    val categories: Flow<List<Category>> = categoryDao.observeAll().map { rows -> rows.map(CategoryEntity::toModel) }

    // The startup sync and the Categories screen can refresh together: a new user must get the defaults only once
    private val refreshLock = Mutex()

    suspend fun get(id: Long?): Category? = id?.let { categoryDao.getById(it)?.toModel() }

    /** Replaces the local copy with the server's list. A new user starts with the default categories. */
    suspend fun refresh(): UiState<Unit> = refreshLock.withLock {
        apiCall(ioDispatcher) {
            val uid = userId() ?: return@apiCall
            // shortcut: if this stops halfway the user keeps a partial default list, fine for sample defaults
            val remote = fetch(uid).ifEmpty { SampleData.categories.map { api.create(it.toDto(uid)) } }
            categoryDao.replaceAll(remote.map(CategoryDto::toEntity))
        }
    }

    /** Creates the category when its id is 0, otherwise updates the row with the same id. */
    suspend fun save(category: Category): UiState<Category> = apiCall(ioDispatcher) {
        val uid = checkNotNull(userId()) { "not signed in" }
        if (category.id == 0L) {
            val created = api.create(category.toDto(uid)).toEntity()
            categoryDao.insert(created)
            created.toModel()
        } else {
            api.update(category.id, category.toDto(uid))
            categoryDao.update(CategoryEntity.fromModel(category))
            category
        }
    }

    suspend fun delete(id: Long): UiState<Unit> = apiCall(ioDispatcher) {
        api.delete(id)
        categoryDao.deleteById(id)
    }

    /** MockAPI can answer 404 instead of an empty list when the filter matches no row. */
    private suspend fun fetch(uid: String): List<CategoryDto> = try {
        api.getAll(uid)
    } catch (e: HttpException) {
        if (e.code() == 404) emptyList() else throw e
    }
}
