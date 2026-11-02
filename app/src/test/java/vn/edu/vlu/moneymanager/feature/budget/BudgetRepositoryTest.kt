package vn.edu.vlu.moneymanager.feature.budget

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import vn.edu.vlu.moneymanager.FakeBudgetDao
import vn.edu.vlu.moneymanager.data.model.Budget
import vn.edu.vlu.moneymanager.data.model.MonthKey
import vn.edu.vlu.moneymanager.data.repository.BudgetRepository

/**
 * The repository gets its DAO through the constructor, so the test passes an in-memory fake (SampleData budgets).
 * The real Room DAO, UNIQUE index and foreign key included, is tested on a device in androidTest/.../BudgetDaoTest.
 */
class BudgetRepositoryTest {

    private val repo = BudgetRepository(FakeBudgetDao())
    private val month = MonthKey.current()

    @Test
    fun save_newBudget_getsNextFreeIdAndIsAppended() = runTest {
        val maxIdBefore = repo.budgets.first().maxOf { it.id }

        val saved = repo.save(Budget(0L, categoryId = 5L, month = month, limit = 700_000.0))

        assertEquals(maxIdBefore + 1, saved.id)
        assertEquals(saved, repo.budgets.first().last())
    }

    @Test
    fun save_existingId_updatesInPlace() = runTest {
        val idsBefore = repo.budgets.first().map { it.id }
        val target = repo.budgets.first().first()

        repo.save(target.copy(limit = 4_500_000.0))

        assertEquals(idsBefore, repo.budgets.first().map { it.id })
        assertEquals(4_500_000.0, repo.budgets.first().first { it.id == target.id }.limit, 0.0)
    }

    @Test
    fun budgets_keepTheirMonth() = runTest {
        val nextMonth = month.plusMonths(1)

        val saved = repo.save(Budget(0L, categoryId = 1L, month = nextMonth, limit = 100.0))

        assertEquals(nextMonth, repo.budgets.first().first { it.id == saved.id }.month)
    }

    @Test
    fun delete_removesTheBudget() = runTest {
        val target = repo.budgets.first().first()

        repo.delete(target.id)

        assertTrue(repo.budgets.first().none { it.id == target.id })
    }

    @Test
    fun delete_unknownId_keepsList() = runTest {
        val before = repo.budgets.first()

        repo.delete(9_999L)

        assertEquals(before, repo.budgets.first())
    }

    @Test
    fun restore_putsTheDeletedBudgetBackWithItsId() = runTest {
        val target = repo.budgets.first().first()

        repo.delete(target.id)
        repo.restore(target)

        assertTrue(repo.budgets.first().contains(target))
    }
}
