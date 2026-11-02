package vn.edu.vlu.moneymanager.feature.transaction

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import vn.edu.vlu.moneymanager.FakeTransactionDao
import vn.edu.vlu.moneymanager.data.model.Transaction
import vn.edu.vlu.moneymanager.data.model.TransactionType
import vn.edu.vlu.moneymanager.data.repository.TransactionRepository

/**
 * The repository gets its DAO through the constructor, so the test passes an in-memory fake (SampleData rows).
 * The real Room DAO, foreign key included, is tested on a device in androidTest/.../TransactionDaoTest.
 */
class TransactionRepositoryTest {

    private val repo = TransactionRepository(FakeTransactionDao())

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

    @Test
    fun save_newTransaction_getsNextFreeId() = runTest {
        val maxIdBefore = repo.transactions.first().maxOf { it.id }

        val saved = repo.save(newTransaction())

        assertEquals(maxIdBefore + 1, saved.id)
        assertEquals(saved, repo.get(saved.id))
    }

    @Test
    fun save_twoNewTransactions_getDifferentIds() = runTest {
        val first = repo.save(newTransaction(note = "First"))
        val second = repo.save(newTransaction(note = "Second"))

        assertNotEquals(first.id, second.id)
        assertEquals(2, repo.transactions.first().count { it.id == first.id || it.id == second.id })
    }

    @Test
    fun transactions_areNewestFirst() = runTest {
        val oldest = repo.transactions.first().minOf { it.dateMillis }
        val saved = repo.save(newTransaction(dateMillis = oldest - 1))

        val dates = repo.transactions.first().map { it.dateMillis }
        assertEquals(dates.sortedDescending(), dates)
        assertEquals(saved.id, repo.transactions.first().last().id)
    }

    @Test
    fun save_existingId_updatesRowWithoutAddingOne() = runTest {
        val original = repo.transactions.first().first()
        val sizeBefore = repo.transactions.first().size

        repo.save(original.copy(amount = 99_000.0, note = "Edited"))

        assertEquals(sizeBefore, repo.transactions.first().size)
        val edited = repo.get(original.id)!!
        assertEquals(99_000.0, edited.amount, 0.0)
        assertEquals("Edited", edited.note)
    }

    @Test
    fun delete_existingId_removesAndReturnsIt() = runTest {
        val target = repo.transactions.first().first()
        val sizeBefore = repo.transactions.first().size

        val removed = repo.delete(target.id)

        assertEquals(target, removed)
        assertNull(repo.get(target.id))
        assertEquals(sizeBefore - 1, repo.transactions.first().size)
    }

    @Test
    fun delete_unknownId_returnsNullAndKeepsList() = runTest {
        val before = repo.transactions.first()

        assertNull(repo.delete(9_999L))
        assertEquals(before, repo.transactions.first())
    }

    @Test
    fun restore_putsTheDeletedTransactionBackWithItsId() = runTest {
        val target = repo.transactions.first().first()

        repo.delete(target.id)
        repo.restore(target)

        assertEquals(target, repo.get(target.id))
    }

    @Test
    fun get_unknownId_returnsNull() = runTest {
        assertNull(repo.get(9_999L))
    }

    @Test
    fun markAllSynced_clearsEveryPendingFlag() = runTest {
        repo.save(newTransaction().copy(pendingSync = true))
        assertTrue(repo.transactions.first().any { it.pendingSync })

        repo.markAllSynced()

        assertFalse(repo.transactions.first().any { it.pendingSync })
    }
}
