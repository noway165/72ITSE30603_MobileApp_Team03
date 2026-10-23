package vn.edu.vlu.moneymanager.data.repository

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import vn.edu.vlu.moneymanager.data.model.Category
import vn.edu.vlu.moneymanager.data.source.SampleData

class CategoryRepository {

    private val _categories = MutableStateFlow(SampleData.categories)
    val categories: StateFlow<List<Category>> = _categories.asStateFlow()

    private var nextId = (SampleData.categories.maxOfOrNull { it.id } ?: 0L) + 1

    fun get(id: Long?): Category? = id?.let { categoryId -> _categories.value.firstOrNull { it.id == categoryId } }

    fun save(category: Category): Category {
        val saved = if (category.id == 0L) category.copy(id = nextId++) else category
        _categories.update { list ->
            if (list.any { it.id == saved.id }) list.map { if (it.id == saved.id) saved else it } else list + saved
        }
        return saved
    }

    fun delete(id: Long) {
        _categories.update { list -> list.filterNot { it.id == id } }
    }
}
