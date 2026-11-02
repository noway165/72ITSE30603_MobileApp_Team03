package vn.edu.vlu.moneymanager.data.local

import android.database.sqlite.SQLiteConstraintException
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import vn.edu.vlu.moneymanager.data.local.dao.TransactionDao
import vn.edu.vlu.moneymanager.data.local.entity.TransactionEntity
import vn.edu.vlu.moneymanager.data.model.TransactionType
import vn.edu.vlu.moneymanager.data.source.SampleData

/** Runs on a device / emulator against a real SQLite database kept in memory (same setup as CategoryDaoTest). */
@RunWith(AndroidJUnit4::class)
class TransactionDaoTest {

    private lateinit var database: AppDatabase
    private lateinit var dao: TransactionDao

    @Before
    fun createDatabase() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .addCallback(SeedDatabaseCallback)
            .build()
        dao = database.transactionDao()
    }

    @After
    fun closeDatabase() {
        database.close()
    }

    private fun newTransaction(categoryId: Long? = 1L, dateMillis: Long = System.currentTimeMillis()) =
        TransactionEntity(
            type = TransactionType.EXPENSE,
            amount = 85_000.0,
            currency = "VND",
            categoryId = categoryId,
            dateMillis = dateMillis,
            note = "Lunch",
            pendingSync = true
        )

    @Test
    fun newDatabase_isSeededWithSampleTransactions_newestFirst() = runBlocking {
        val rows = dao.observeAll().first()

        assertEquals(SampleData.transactions.size, rows.size)
        assertEquals(SampleData.transactions.map { it.id }.toSet(), rows.map { it.id }.toSet())
        val dates = rows.map { it.dateMillis }
        assertEquals(dates.sortedDescending(), dates)
    }

    @Test
    fun insert_returnsNewId_andGetByIdReadsItBack() = runBlocking {
        val id = dao.insert(newTransaction())

        val row = dao.getById(id)!!
        assertEquals(85_000.0, row.amount, 0.0)
        assertEquals(TransactionType.EXPENSE, row.type)
        assertEquals(SampleData.transactions.size + 1, dao.observeAll().first().size)
    }

    @Test
    fun update_changesTheRow() = runBlocking {
        val row = dao.getById(1L)!!

        dao.update(row.copy(amount = 1.0, note = "Edited"))

        assertEquals("Edited", dao.getById(1L)?.note)
        assertEquals(1.0, dao.getById(1L)!!.amount, 0.0)
    }

    @Test
    fun deleteById_removesTheRow() = runBlocking {
        dao.deleteById(1L)

        assertNull(dao.getById(1L))
    }

    @Test
    fun markAllSynced_clearsEveryPendingFlag() = runBlocking {
        dao.insert(newTransaction())
        assertTrue(dao.observeAll().first().any { it.pendingSync })

        dao.markAllSynced()

        assertFalse(dao.observeAll().first().any { it.pendingSync })
    }

    @Test
    fun deletingCategory_setsCategoryIdToNull_andKeepsTheTransactions() = runBlocking {
        val foodIds = dao.observeAll().first().filter { it.categoryId == 1L }.map { it.id }
        assertTrue("sample data has Food transactions", foodIds.isNotEmpty())
        val countBefore = dao.observeAll().first().size

        database.categoryDao().deleteById(1L)   // ON DELETE SET NULL

        assertEquals(countBefore, dao.observeAll().first().size)
        foodIds.forEach { assertNull(dao.getById(it)!!.categoryId) }
        assertTrue(dao.observeAll().first().any { it.categoryId == 2L })
    }

    @Test(expected = SQLiteConstraintException::class)
    fun insert_unknownCategory_isRejectedByForeignKey() = runBlocking<Unit> {
        dao.insert(newTransaction(categoryId = 9_999L))
    }

    @Test
    fun insert_withoutCategory_isAllowed() = runBlocking {
        val id = dao.insert(newTransaction(categoryId = null))

        assertNull(dao.getById(id)!!.categoryId)
    }
}
