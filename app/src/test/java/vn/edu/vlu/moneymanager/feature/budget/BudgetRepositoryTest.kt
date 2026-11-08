package vn.edu.vlu.moneymanager.feature.budget

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import vn.edu.vlu.moneymanager.FakeBudgetApi
import vn.edu.vlu.moneymanager.R
import vn.edu.vlu.moneymanager.TEST_USER_ID
import vn.edu.vlu.moneymanager.data.model.Budget
import vn.edu.vlu.moneymanager.data.model.MonthKey
import vn.edu.vlu.moneymanager.data.remote.BudgetDto
import vn.edu.vlu.moneymanager.data.remote.UiState
import vn.edu.vlu.moneymanager.sampleBudgetRepository
import java.io.IOException

/**
 * The repository gets its DAO and API through the constructor, so the test passes in-memory fakes
 * (both start with the SampleData budgets). The real Room DAO is tested on a device in androidTest/.../BudgetDaoTest.
 */
class BudgetRepositoryTest {

    private val api = FakeBudgetApi()
    private val repo = sampleBudgetRepository(api)
    private val month = MonthKey.current()

    @Test
    fun save_newBudget_getsTheServerIdAndIsAppended() = runTest {
        val saved = (repo.save(Budget(0L, categoryId = 5L, month = month, limit = 700_000.0)) as UiState.Success).data

        assertEquals(saved, repo.budgets.first().last())
        assertEquals(saved.id.toString(), api.rows.last().id)
        assertEquals(TEST_USER_ID, api.rows.last().userId)
    }

    @Test
    fun save_existingId_updatesInPlace() = runTest {
        val idsBefore = repo.budgets.first().map { it.id }
        val target = repo.budgets.first().first()

        repo.save(target.copy(limit = 4_500_000.0))

        assertEquals(idsBefore, repo.budgets.first().map { it.id })
        assertEquals(4_500_000.0, repo.budgets.first().first { it.id == target.id }.limit, 0.0)
        assertEquals(4_500_000.0, api.rows.first { it.id == target.id.toString() }.limitAmount, 0.0)
    }

    @Test
    fun save_offline_returnsErrorAndChangesNothing() = runTest {
        val before = repo.budgets.first()
        api.failure = IOException("Airplane Mode")

        val result = repo.save(Budget(0L, categoryId = 5L, month = month, limit = 700_000.0))

        assertEquals(UiState.Error(R.string.error_network_offline), result)
        assertEquals(before, repo.budgets.first())
    }

    @Test
    fun budgets_keepTheirMonth() = runTest {
        val nextMonth = month.plusMonths(1)

        val saved = (repo.save(Budget(0L, categoryId = 1L, month = nextMonth, limit = 100.0)) as UiState.Success).data

        assertEquals(nextMonth, repo.budgets.first().first { it.id == saved.id }.month)
    }

    @Test
    fun refresh_replacesTheLocalCopyWithTheServerList() = runTest {
        api.rows.clear()
        api.rows += BudgetDto("77", TEST_USER_ID, "1", month.year, month.month, 123.0)

        assertEquals(UiState.Success(Unit), repo.refresh())

        assertEquals(listOf(Budget(77L, 1L, month, 123.0)), repo.budgets.first())
    }

    @Test
    fun refresh_skipsBudgetsOfUnknownCategoriesAndDuplicates() = runTest {
        api.rows.clear()
        api.rows += BudgetDto("1", TEST_USER_ID, "9999", month.year, month.month, 1.0)
        api.rows += BudgetDto("2", TEST_USER_ID, "1", month.year, month.month, 2.0)
        api.rows += BudgetDto("3", TEST_USER_ID, "1", month.year, month.month, 3.0)

        repo.refresh()

        assertEquals(listOf(2L), repo.budgets.first().map { it.id })
    }

    @Test
    fun refresh_noBudgetsOnServer_404_meansEmpty() = runTest {
        api.rows.clear()

        assertEquals(UiState.Success(Unit), repo.refresh())
        assertTrue(repo.budgets.first().isEmpty())
    }

    @Test
    fun delete_removesTheBudgetEverywhere() = runTest {
        val target = repo.budgets.first().first()

        assertEquals(UiState.Success(Unit), repo.delete(target.id))

        assertTrue(repo.budgets.first().none { it.id == target.id })
        assertTrue(api.rows.none { it.id == target.id.toString() })
    }

    @Test
    fun delete_unknownId_keepsList() = runTest {
        val before = repo.budgets.first()

        repo.delete(9_999L)

        assertEquals(before, repo.budgets.first())
    }

    @Test
    fun restore_putsTheDeletedBudgetBackAsANewRow() = runTest {
        val target = repo.budgets.first().first()

        repo.delete(target.id)
        val restored = (repo.restore(target) as UiState.Success).data

        assertEquals(target.copy(id = restored.id), restored)
        assertTrue(repo.budgets.first().contains(restored))
    }
}
