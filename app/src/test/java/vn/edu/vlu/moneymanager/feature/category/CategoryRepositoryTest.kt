package vn.edu.vlu.moneymanager.feature.category

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import vn.edu.vlu.moneymanager.FakeCategoryApi
import vn.edu.vlu.moneymanager.FakeCategoryDao
import vn.edu.vlu.moneymanager.R
import vn.edu.vlu.moneymanager.TEST_USER_ID
import vn.edu.vlu.moneymanager.data.local.entity.CategoryEntity
import vn.edu.vlu.moneymanager.data.model.Category
import vn.edu.vlu.moneymanager.data.model.TransactionType
import vn.edu.vlu.moneymanager.data.remote.CategoryDto
import vn.edu.vlu.moneymanager.data.remote.UiState
import vn.edu.vlu.moneymanager.data.repository.CategoryRepository
import vn.edu.vlu.moneymanager.data.source.SampleData
import java.net.UnknownHostException

class CategoryRepositoryTest {

    private val api = FakeCategoryApi()
    private val dao = FakeCategoryDao()
    private var uid: String? = TEST_USER_ID
    private val repo = CategoryRepository(dao, api, { uid }, Dispatchers.Unconfined)

    private val pets = Category(0L, "Pets", TransactionType.EXPENSE, "#22C55E")

    @Test
    fun refresh_replacesLocalRowsWithTheServerList() = runTest {
        api.rows.clear()
        api.rows += CategoryDto("42", TEST_USER_ID, "Rent", "EXPENSE", "#111111")

        assertEquals(UiState.Success(Unit), repo.refresh())

        assertEquals(listOf(Category(42L, "Rent", TransactionType.EXPENSE, "#111111")), repo.categories.first())
    }

    @Test
    fun refresh_onlyLoadsThisUsersRows() = runTest {
        api.rows += CategoryDto("99", "someone-else", "Secret", "EXPENSE", "#111111")

        repo.refresh()

        assertFalse(repo.categories.first().any { it.name == "Secret" })
    }

    @Test
    fun refresh_newUser_createsTheDefaultsOnTheServer() = runTest {
        api.rows.clear()

        assertEquals(UiState.Success(Unit), repo.refresh())

        assertEquals(SampleData.categories.size, api.rows.size)
        assertTrue(api.rows.all { it.userId == TEST_USER_ID })
        assertEquals(SampleData.categories.map { it.name }, repo.categories.first().map { it.name })
    }

    @Test
    fun refresh_offline_keepsTheLocalCopyAndReportsTheError() = runTest {
        val before = repo.categories.first()
        api.failure = UnknownHostException()

        assertEquals(UiState.Error(R.string.error_network_offline), repo.refresh())

        assertEquals(before, repo.categories.first())
    }

    @Test
    fun refresh_notSignedIn_doesNothing() = runTest {
        uid = null
        val before = repo.categories.first()
        api.rows.clear()

        assertEquals(UiState.Success(Unit), repo.refresh())

        assertTrue(api.rows.isEmpty())
        assertEquals(before, repo.categories.first())
    }

    @Test
    fun save_new_usesTheServerIdInRoom() = runTest {
        val saved = (repo.save(pets) as UiState.Success).data

        assertEquals(api.rows.last().id, saved.id.toString())
        assertEquals(saved, repo.get(saved.id))
        assertEquals(TEST_USER_ID, api.rows.last().userId)
    }

    @Test
    fun save_offline_doesNotChangeTheLocalCopy() = runTest {
        val before = repo.categories.first()
        api.failure = UnknownHostException()

        assertEquals(UiState.Error(R.string.error_network_offline), repo.save(pets))

        assertEquals(before, repo.categories.first())
    }

    @Test
    fun save_existing_updatesServerAndRoom() = runTest {
        val original = repo.categories.first().first()
        val renamed = original.copy(name = "Eating out", colorHex = "#F97316")

        assertEquals(UiState.Success(renamed), repo.save(renamed))

        assertEquals(renamed, repo.get(original.id))
        assertEquals("Eating out", api.rows.first { it.id == original.id.toString() }.name)
    }

    @Test
    fun delete_removesFromServerAndRoom() = runTest {
        val target = repo.categories.first().first()

        assertEquals(UiState.Success(Unit), repo.delete(target.id))

        assertNull(repo.get(target.id))
        assertFalse(api.rows.any { it.id == target.id.toString() })
    }

    @Test
    fun delete_failed_keepsTheRow() = runTest {
        val target = repo.categories.first().first()
        api.failure = UnknownHostException()

        repo.delete(target.id)

        assertEquals(target, repo.get(target.id))
    }

    @Test
    fun get_nullOrUnknownId_returnsNull() = runTest {
        assertNull(repo.get(null))
        assertNull(repo.get(9_999L))
    }

    @Test
    fun entityMapping_keepsEveryField() {
        val dto = CategoryDto("7", TEST_USER_ID, "Rent", "INCOME", "#111111")

        assertEquals(CategoryEntity(7L, "Rent", TransactionType.INCOME, "#111111"), dto.toEntity())
    }
}
