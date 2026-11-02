package vn.edu.vlu.moneymanager.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import vn.edu.vlu.moneymanager.data.local.dao.BudgetDao
import vn.edu.vlu.moneymanager.data.local.dao.CategoryDao
import vn.edu.vlu.moneymanager.data.local.dao.TransactionDao
import vn.edu.vlu.moneymanager.data.local.entity.BudgetEntity
import vn.edu.vlu.moneymanager.data.local.entity.CategoryEntity
import vn.edu.vlu.moneymanager.data.local.entity.TransactionEntity

/**
 * The app's only Room database (file "finance_tracker.db").
 *
 * Adding a table: put its Entity in [entities], add an abstract DAO getter below and a seed function in
 * [SeedDatabaseCallback]. While the schema still changes during development the version stays 1:
 * uninstall the app (or clear its data) after a schema change, otherwise Room refuses to open the old file.
 */
@Database(
    entities = [CategoryEntity::class, TransactionEntity::class, BudgetEntity::class],
    version = 1,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun categoryDao(): CategoryDao

    abstract fun transactionDao(): TransactionDao

    abstract fun budgetDao(): BudgetDao

    companion object {
        private const val DATABASE_NAME = "finance_tracker.db"

        // @Volatile: a write by one thread is seen at once by the others
        @Volatile
        private var instance: AppDatabase? = null

        /**
         * Thread-safe singleton (double-checked locking): the fast path skips the lock once the database exists,
         * and synchronized makes sure two threads arriving together never build two instances.
         */
        fun getInstance(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: buildDatabase(context.applicationContext).also { instance = it }
            }

        private fun buildDatabase(appContext: Context): AppDatabase =
            Room.databaseBuilder(appContext, AppDatabase::class.java, DATABASE_NAME)
                .addCallback(SeedDatabaseCallback)
                .build()
    }
}
