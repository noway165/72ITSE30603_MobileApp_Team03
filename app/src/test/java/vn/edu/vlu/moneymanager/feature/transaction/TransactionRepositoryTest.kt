package vn.edu.vlu.moneymanager.feature.transaction

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import vn.edu.vlu.moneymanager.FakeTransactionApi
import vn.edu.vlu.moneymanager.R
import vn.edu.vlu.moneymanager.TEST_USER_ID
import vn.edu.vlu.moneymanager.data.model.Transaction
import vn.edu.vlu.moneymanager.data.model.TransactionType
import vn.edu.vlu.moneymanager.data.remote.TransactionDto
import vn.edu.vlu.moneymanager.data.remote.UiState
import vn.edu.vlu.moneymanager.sampleTransactionRepository
import vn.edu.vlu.moneymanager.saved
import java.io.IOException

/**
 * The repository gets its DAO and API through the constructor, so the test passes in-memory fakes
 * (both start with the SampleData rows). The real Room DAO is tested on a device in androidTest/.../TransactionDaoTest.
 */
class TransactionRepositoryTest {

    private val api = FakeTransactionApi()
    private val repo = sampleTransactionRepository(api)

    private fun newTransaction(
        categoryId: Long? = 1L,
        dateMillis: Long = System.currentTimeMillis(),
        note: String = "Test"
    ) = Transaction(
        id = 0L,
        type = TransactionType.EXPENSE,
        amount = 50_000.0,
        currency = "VND",
        categoryId = categoryId,
        dateMillis = dateMillis,
        note = note
    )

    private fun goOffline() {
        api.failure = IOException("Airplane Mode")
    }

    @Test
    fun save_newTransaction_getsTheServerIdAndIsSynced() = runTest {
        val saved = repo.saved(newTransaction())

        assertFalse(saved.pendingSync)
        assertEquals(saved, repo.get(saved.id))
        assertEquals(saved.id.toString(), api.rows.last().id)
        assertEquals(TEST_USER_ID, api.rows.last().userId)
    }

    @Test
    fun save_twoNewTransactions_getDifferentIds() = runTest {
        val first = repo.saved(newTransaction(note = "First"))
        val second = repo.saved(newTransaction(note = "Second"))

        assertNotEquals(first.id, second.id)
        assertEquals(2, repo.transactions.first().count { it.id == first.id || it.id == second.id })
    }

    @Test
    fun transactions_areNewestFirst() = runTest {
        val oldest = repo.transactions.first().minOf { it.dateMillis }
        val saved = repo.saved(newTransaction(dateMillis = oldest - 1))

        val dates = repo.transactions.first().map { it.dateMillis }
        assertEquals(dates.sortedDescending(), dates)
        assertEquals(saved.id, repo.transactions.first().last().id)
    }

    @Test
    fun save_existingId_updatesRowOnServerAndHere() = runTest {
        val original = repo.transactions.first().first { !it.pendingSync }
        val sizeBefore = repo.transactions.first().size

        repo.saved(original.copy(amount = 99_000.0, note = "Edited"))

        assertEquals(sizeBefore, repo.transactions.first().size)
        val edited = repo.get(original.id)!!
        assertEquals(99_000.0, edited.amount, 0.0)
        assertEquals("Edited", api.rows.first { it.id == original.id.toString() }.note)
    }

    @Test
    fun save_offline_keepsTheRowAsPendingWithANegativeId() = runTest {
        goOffline()
        val serverSize = api.rows.size

        val saved = repo.saved(newTransaction())

        assertTrue(saved.pendingSync)
        assertTrue(saved.id < 0)
        assertEquals(saved, repo.get(saved.id))
        assertEquals(serverSize, api.rows.size)
    }

    @Test
    fun save_offlineTwice_givesDifferentLocalIds() = runTest {
        goOffline()

        val first = repo.saved(newTransaction(note = "First"))
        val second = repo.saved(newTransaction(note = "Second"))

        assertNotEquals(first.id, second.id)
    }

    @Test
    fun syncPending_uploadsOfflineRowsAndSwapsInTheServerId() = runTest {
        goOffline()
        val offline = repo.saved(newTransaction(note = "Offline"))
        api.failure = null

        val result = repo.syncPending()

        assertTrue(result is UiState.Success)
        assertNull(repo.get(offline.id))
        val uploaded = repo.transactions.first().first { it.note == "Offline" }
        assertTrue(uploaded.id > 0)
        assertFalse(repo.transactions.first().any { it.pendingSync })
        assertTrue(api.rows.any { it.id == uploaded.id.toString() && it.note == "Offline" })
    }

    @Test
    fun syncPending_offline_returnsErrorAndKeepsPendingRows() = runTest {
        goOffline()
        repo.saved(newTransaction())

        assertEquals(UiState.Error(R.string.error_network_offline), repo.syncPending())
        assertTrue(repo.transactions.first().any { it.pendingSync })
    }

    @Test
    fun refresh_replacesSyncedRowsButKeepsPendingOnes() = runTest {
        goOffline()
        val offline = repo.saved(newTransaction(note = "Not uploaded yet"))
        api.failure = null
        api.rows.clear()
        api.rows += TransactionDto("500", TEST_USER_ID, "INCOME", 10.0, "USD", "1", 1_000L, "From another phone")

        assertEquals(UiState.Success(Unit), repo.refresh())

        val rows = repo.transactions.first()
        assertTrue(rows.any { it.note == "From another phone" })
        assertTrue(rows.any { it.id == offline.id })
        assertTrue(rows.all { it.id == 500L || it.pendingSync })
    }

    @Test
    fun refresh_serverRowWithUnknownCategory_becomesUncategorized() = runTest {
        api.rows.clear()
        api.rows += TransactionDto("500", TEST_USER_ID, "EXPENSE", 10.0, "USD", "9999", 1_000L, "Old")

        repo.refresh()

        assertNull(repo.get(500L)!!.categoryId)
    }

    @Test
    fun refresh_offline_returnsErrorAndKeepsTheLocalCopy() = runTest {
        val before = repo.transactions.first()
        goOffline()

        assertEquals(UiState.Error(R.string.error_network_offline), repo.refresh())
        assertEquals(before, repo.transactions.first())
    }

    @Test
    fun delete_existingId_removesItEverywhereAndReturnsIt() = runTest {
        val target = repo.transactions.first().first { it.id > 0 }

        val removed = repo.delete(target.id)

        assertEquals(UiState.Success(target), removed)
        assertNull(repo.get(target.id))
        assertTrue(api.rows.none { it.id == target.id.toString() })
    }

    @Test
    fun delete_offline_returnsErrorAndKeepsTheRow() = runTest {
        val target = repo.transactions.first().first { it.id > 0 }
        goOffline()

        assertEquals(UiState.Error(R.string.error_network_offline), repo.delete(target.id))
        assertEquals(target, repo.get(target.id))
    }

    @Test
    fun delete_unknownId_returnsNullAndKeepsList() = runTest {
        val before = repo.transactions.first()

        assertEquals(UiState.Success(null), repo.delete(9_999L))
        assertEquals(before, repo.transactions.first())
    }

    @Test
    fun restore_putsTheDeletedTransactionBackAsANewRow() = runTest {
        val target = repo.transactions.first().first { it.id > 0 && !it.pendingSync }

        repo.delete(target.id)
        val restored = (repo.restore(target) as UiState.Success).data

        assertEquals(target.copy(id = restored.id), restored)
        assertEquals(restored, repo.get(restored.id))
    }

    @Test
    fun get_unknownId_returnsNull() = runTest {
        assertNull(repo.get(9_999L))
    }
}
