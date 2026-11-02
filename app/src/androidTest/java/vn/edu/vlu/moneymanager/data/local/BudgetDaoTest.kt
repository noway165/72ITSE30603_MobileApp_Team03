package vn.edu.vlu.moneymanager.data.local

import android.database.sqlite.SQLiteConstraintException
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import vn.edu.vlu.moneymanager.data.local.dao.BudgetDao
import vn.edu.vlu.moneymanager.data.local.entity.BudgetEntity
import vn.edu.vlu.moneymanager.data.model.MonthKey
import vn.edu.vlu.moneymanager.data.source.SampleData

/** Runs on a device / emulator against a real SQLite database kept in memory (same setup as CategoryDaoTest). */
@RunWith(AndroidJUnit4::class)
class BudgetDaoTest {

    private lateinit var database: AppDatabase
    private lateinit var dao: BudgetDao
    private val month = MonthKey.current()

    @Before
    fun createDatabase() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .addCallback(SeedDatabaseCallback)
            .build()
        dao = database.budgetDao()
    }

    @After
    fun closeDatabase() {
        database.close()
    }

    @Test
    fun newDatabase_isSeededWithSampleBudgets() = runBlocking {
        val budgets = dao.observeAll().first().map { it.toModel() }

        assertEquals(SampleData.budgets, budgets)
    }

    @Test
    fun insert_returnsNewIdAndEmitsUpdatedList() = runBlocking {
        val id = dao.insert(BudgetEntity(categoryId = 5L, year = month.year, month = month.month, limitAmount = 700_000.0))

        val rows = dao.observeAll().first()
        assertEquals(SampleData.budgets.size + 1, rows.size)
        assertEquals(700_000.0, rows.first { it.id == id }.limitAmount, 0.0)
    }

    @Test(expected = SQLiteConstraintException::class)
    fun insert_sameCategoryAndMonth_isRejectedByUniqueIndex() = runBlocking<Unit> {
        // Sample data already has a Food & Drinks (id 1) budget for the current month
        dao.insert(BudgetEntity(categoryId = 1L, year = month.year, month = month.month, limitAmount = 1.0))
    }

    @Test
    fun insert_sameCategoryOtherMonth_isAllowed() = runBlocking {
        val next = month.plusMonths(1)

        dao.insert(BudgetEntity(categoryId = 1L, year = next.year, month = next.month, limitAmount = 1.0))

        assertEquals(2, dao.observeAll().first().count { it.categoryId == 1L })
    }

    @Test
    fun update_changesTheRow() = runBlocking {
        val budget = dao.observeAll().first().first()

        dao.update(budget.copy(limitAmount = 9_000_000.0))

        assertEquals(9_000_000.0, dao.observeAll().first().first { it.id == budget.id }.limitAmount, 0.0)
    }

    @Test
    fun deleteById_removesTheRow() = runBlocking {
        dao.deleteById(1L)

        assertTrue(dao.observeAll().first().none { it.id == 1L })
    }

    @Test
    fun deletingCategory_deletesItsBudgets() = runBlocking {
        assertTrue("sample data has a Food budget", dao.observeAll().first().any { it.categoryId == 1L })
        val otherBefore = dao.observeAll().first().filter { it.categoryId != 1L }

        database.categoryDao().deleteById(1L)   // ON DELETE CASCADE

        assertTrue(dao.observeAll().first().none { it.categoryId == 1L })
        assertEquals(otherBefore, dao.observeAll().first())
    }

    @Test(expected = SQLiteConstraintException::class)
    fun insert_unknownCategory_isRejectedByForeignKey() = runBlocking<Unit> {
        dao.insert(BudgetEntity(categoryId = 9_999L, year = month.year, month = month.month, limitAmount = 1.0))
    }
}
