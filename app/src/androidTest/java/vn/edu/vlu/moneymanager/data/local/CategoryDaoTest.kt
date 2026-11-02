package vn.edu.vlu.moneymanager.data.local

import android.database.sqlite.SQLiteConstraintException
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import vn.edu.vlu.moneymanager.data.local.dao.CategoryDao
import vn.edu.vlu.moneymanager.data.local.entity.CategoryEntity
import vn.edu.vlu.moneymanager.data.model.TransactionType
import vn.edu.vlu.moneymanager.data.source.SampleData

/**
 * Runs on a device / emulator against a real SQLite database kept in memory (nothing written to disk).
 * Template for the other DAO tests: build the in-memory database in @Before, close it in @After.
 */
@RunWith(AndroidJUnit4::class)
class CategoryDaoTest {

    private lateinit var database: AppDatabase
    private lateinit var dao: CategoryDao

    @Before
    fun createDatabase() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .addCallback(SeedDatabaseCallback)
            .build()
        dao = database.categoryDao()
    }

    @After
    fun closeDatabase() {
        database.close()
    }

    @Test
    fun newDatabase_isSeededWithSampleCategories() = runBlocking {
        val categories = dao.observeAll().first().map { it.toModel() }

        assertEquals(SampleData.categories, categories)
    }

    @Test
    fun insert_returnsNewIdAndEmitsUpdatedList() = runBlocking {
        val id = dao.insert(CategoryEntity(name = "Pets", type = TransactionType.EXPENSE, colorHex = "#22C55E"))

        assertEquals("Pets", dao.getById(id)?.name)
        assertEquals(SampleData.categories.size + 1, dao.observeAll().first().size)
    }

    @Test(expected = SQLiteConstraintException::class)
    fun insert_sameNameAndType_isRejectedByUniqueIndex() = runBlocking<Unit> {
        dao.insert(CategoryEntity(name = "Food & Drinks", type = TransactionType.EXPENSE, colorHex = "#000000"))
    }

    @Test
    fun update_changesTheRow() = runBlocking {
        val food = dao.getById(1L)!!

        dao.update(food.copy(name = "Eating out"))

        assertEquals("Eating out", dao.getById(1L)?.name)
    }

    @Test
    fun deleteById_removesTheRow() = runBlocking {
        dao.deleteById(1L)

        assertNull(dao.getById(1L))
    }
}
