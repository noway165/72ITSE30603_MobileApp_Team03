package vn.edu.vlu.moneymanager.feature.category

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import vn.edu.vlu.moneymanager.data.model.Category
import vn.edu.vlu.moneymanager.data.model.TransactionType
import vn.edu.vlu.moneymanager.data.repository.CategoryRepository

/** CategoryRepository is plain Kotlin: a fresh instance per test, no rule needed. */
class CategoryRepositoryTest {

    private val repo = CategoryRepository()

    @Test
    fun save_newCategory_getsNextFreeIdAndIsAppended() {
        val before = repo.categories.value
        val maxIdBefore = before.maxOf { it.id }

        val saved = repo.save(Category(0L, "Pets", TransactionType.EXPENSE, "#22C55E"))

        assertEquals(maxIdBefore + 1, saved.id)
        assertEquals(before + saved, repo.categories.value)
    }

    @Test
    fun save_twoNewCategories_getDifferentIds() {
        val first = repo.save(Category(0L, "Pets", TransactionType.EXPENSE, "#22C55E"))
        val second = repo.save(Category(0L, "Bonus", TransactionType.INCOME, "#14B8A6"))

        assertEquals(first.id + 1, second.id)
    }

    @Test
    fun save_existingCategory_replacesItInPlace() {
        val before = repo.categories.value
        val original = before.first()

        val renamed = repo.save(original.copy(name = "Eating out", colorHex = "#F97316"))

        assertEquals(original.id, renamed.id)
        assertEquals(before.size, repo.categories.value.size)
        // Same position in the list, new values
        assertEquals(renamed, repo.categories.value.first())
        assertEquals(renamed, repo.get(original.id))
    }

    @Test
    fun get_nullOrUnknownId_returnsNull() {
        assertNull(repo.get(null))
        assertNull(repo.get(9_999L))
    }

    @Test
    fun delete_removesOnlyThatCategory() {
        val before = repo.categories.value
        val target = before.first()

        repo.delete(target.id)

        assertNull(repo.get(target.id))
        assertEquals(before.size - 1, repo.categories.value.size)
        assertTrue(repo.categories.value.containsAll(before - target))
    }
}
