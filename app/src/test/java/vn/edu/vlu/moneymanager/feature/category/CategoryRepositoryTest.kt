package vn.edu.vlu.moneymanager.feature.category

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import vn.edu.vlu.moneymanager.FakeCategoryDao
import vn.edu.vlu.moneymanager.data.model.Category
import vn.edu.vlu.moneymanager.data.model.TransactionType
import vn.edu.vlu.moneymanager.data.repository.CategoryRepository
import vn.edu.vlu.moneymanager.data.source.SampleData

/** CategoryRepository over an in-memory DAO: checks the mapping Entity <-> Category and insert vs update. */
class CategoryRepositoryTest {

    private val repo = CategoryRepository(FakeCategoryDao())

    @Test
    fun categories_emitsTheSeededRowsAsModels() = runTest {
        assertEquals(SampleData.categories, repo.categories.first())
    }

    @Test
    fun save_newCategory_getsNextFreeIdAndIsAppended() = runTest {
        val before = repo.categories.first()
        val maxIdBefore = before.maxOf { it.id }

        val saved = repo.save(Category(0L, "Pets", TransactionType.EXPENSE, "#22C55E"))

        assertEquals(maxIdBefore + 1, saved.id)
        assertEquals(before + saved, repo.categories.first())
    }

    @Test
    fun save_twoNewCategories_getDifferentIds() = runTest {
        val first = repo.save(Category(0L, "Pets", TransactionType.EXPENSE, "#22C55E"))
        val second = repo.save(Category(0L, "Bonus", TransactionType.INCOME, "#14B8A6"))

        assertEquals(first.id + 1, second.id)
    }

    @Test
    fun save_existingCategory_updatesTheRowInPlace() = runTest {
        val before = repo.categories.first()
        val original = before.first()

        val renamed = repo.save(original.copy(name = "Eating out", colorHex = "#F97316"))

        assertEquals(original.id, renamed.id)
        val after = repo.categories.first()
        assertEquals(before.size, after.size)
        assertEquals(renamed, after.first())
        assertEquals(renamed, repo.get(original.id))
    }

    @Test
    fun get_nullOrUnknownId_returnsNull() = runTest {
        assertNull(repo.get(null))
        assertNull(repo.get(9_999L))
    }

    @Test
    fun delete_removesOnlyThatCategory() = runTest {
        val before = repo.categories.first()
        val target = before.first()

        repo.delete(target.id)

        val after = repo.categories.first()
        assertNull(repo.get(target.id))
        assertEquals(before.size - 1, after.size)
        assertTrue(after.containsAll(before - target))
    }
}
