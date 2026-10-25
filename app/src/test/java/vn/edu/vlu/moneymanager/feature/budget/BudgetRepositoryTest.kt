package vn.edu.vlu.moneymanager.feature.budget

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import vn.edu.vlu.moneymanager.data.model.Budget
import vn.edu.vlu.moneymanager.data.model.MonthKey
import vn.edu.vlu.moneymanager.data.repository.BudgetRepository

class BudgetRepositoryTest {

    private val repo = BudgetRepository()
    private val month = MonthKey.current()

    @Test
    fun save_newBudget_getsNextFreeIdAndIsAppended() {
        val maxIdBefore = repo.budgets.value.maxOf { it.id }

        val saved = repo.save(Budget(0L, categoryId = 5L, month = month, limit = 700_000.0))

        assertEquals(maxIdBefore + 1, saved.id)
        assertEquals(saved, repo.budgets.value.last())
    }

    @Test
    fun save_existingId_updatesInPlace() {
        val ordersBefore = repo.budgets.value.map { it.id }
        val target = repo.budgets.value.first()

        repo.save(target.copy(limit = 4_500_000.0))

        assertEquals(ordersBefore, repo.budgets.value.map { it.id })
        assertEquals(4_500_000.0, repo.budgets.value.first { it.id == target.id }.limit, 0.0)
    }

    @Test
    fun delete_existingId_removesAndReturnsIt() {
        val target = repo.budgets.value.first()

        val removed = repo.delete(target.id)

        assertEquals(target, removed)
        assertTrue(repo.budgets.value.none { it.id == target.id })
    }

    @Test
    fun delete_unknownId_returnsNullAndKeepsList() {
        val before = repo.budgets.value

        assertNull(repo.delete(9_999L))
        assertEquals(before, repo.budgets.value)
    }

    @Test
    fun deleteByCategory_onlyRemovesThatCategory() {
        repo.save(Budget(0L, categoryId = 1L, month = month.plusMonths(1), limit = 100.0))
        val others = repo.budgets.value.filter { it.categoryId != 1L }

        repo.deleteByCategory(1L)

        assertTrue(repo.budgets.value.none { it.categoryId == 1L })
        assertEquals(others, repo.budgets.value)
    }
}
