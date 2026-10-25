package vn.edu.vlu.moneymanager.feature.transaction

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import vn.edu.vlu.moneymanager.data.model.Transaction
import vn.edu.vlu.moneymanager.data.model.TransactionType
import vn.edu.vlu.moneymanager.data.repository.TransactionRepository

/** TransactionRepository is plain Kotlin: a fresh instance per test, no rule needed. */
class TransactionRepositoryTest {

    private val repo = TransactionRepository()

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
    fun save_newTransaction_getsNextFreeId() {
        val maxIdBefore = repo.transactions.value.maxOf { it.id }

        val saved = repo.save(newTransaction())

        assertEquals(maxIdBefore + 1, saved.id)
        assertEquals(saved, repo.get(saved.id))
    }

    @Test
    fun save_twoNewTransactions_getDifferentIds() {
        val first = repo.save(newTransaction(note = "First"))
        val second = repo.save(newTransaction(note = "Second"))

        assertNotEquals(first.id, second.id)
        assertEquals(2, repo.transactions.value.count { it.id == first.id || it.id == second.id })
    }

    @Test
    fun save_keepsListSortedNewestFirst() {
        val oldest = repo.transactions.value.minOf { it.dateMillis }
        val saved = repo.save(newTransaction(dateMillis = oldest - 1))

        val dates = repo.transactions.value.map { it.dateMillis }
        assertEquals(dates.sortedDescending(), dates)
        assertEquals(saved.id, repo.transactions.value.last().id)
    }

    @Test
    fun save_existingId_replacesRowWithoutAddingOne() {
        val original = repo.transactions.value.first()
        val sizeBefore = repo.transactions.value.size

        repo.save(original.copy(amount = 99_000.0, note = "Edited"))

        assertEquals(sizeBefore, repo.transactions.value.size)
        val edited = repo.get(original.id)!!
        assertEquals(99_000.0, edited.amount, 0.0)
        assertEquals("Edited", edited.note)
    }

    @Test
    fun delete_existingId_removesAndReturnsIt() {
        val target = repo.transactions.value.first()
        val sizeBefore = repo.transactions.value.size

        val removed = repo.delete(target.id)

        assertEquals(target, removed)
        assertNull(repo.get(target.id))
        assertEquals(sizeBefore - 1, repo.transactions.value.size)
    }

    @Test
    fun delete_unknownId_returnsNullAndKeepsList() {
        val before = repo.transactions.value

        assertNull(repo.delete(9_999L))
        assertEquals(before, repo.transactions.value)
    }

    @Test
    fun get_unknownId_returnsNull() {
        assertNull(repo.get(9_999L))
    }

    @Test
    fun clearCategory_onlyTouchesThatCategory() {
        repo.markAllSynced()
        val categoryId = 1L
        val otherBefore = repo.transactions.value.filter { it.categoryId != categoryId }
        val affectedIds = repo.transactions.value.filter { it.categoryId == categoryId }.map { it.id }
        assertTrue(affectedIds.isNotEmpty())

        repo.clearCategory(categoryId)

        val after = repo.transactions.value
        assertTrue(after.none { it.categoryId == categoryId })
        after.filter { it.id in affectedIds }.forEach {
            assertNull(it.categoryId)
            assertTrue(it.pendingSync)
        }
        assertEquals(otherBefore, after.filter { it.id !in affectedIds })
    }

    @Test
    fun markAllSynced_clearsEveryPendingFlag() {
        repo.save(newTransaction().copy(pendingSync = true))
        assertTrue(repo.transactions.value.any { it.pendingSync })

        repo.markAllSynced()

        assertFalse(repo.transactions.value.any { it.pendingSync })
    }
}
