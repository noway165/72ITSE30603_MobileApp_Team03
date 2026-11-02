package vn.edu.vlu.moneymanager.data.local

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import vn.edu.vlu.moneymanager.data.source.SampleData

/**
 * Fills a brand-new database with SampleData, once, when the file is first created.
 * Runs synchronously inside Room's onCreate, so the data is there before the first query
 * (no coroutine racing with the screens). Rows keep their SampleData ids so foreign keys match.
 *
 * Each table owner adds one seed function here and calls it from onCreate, parents before children:
 * categories -> transactions -> budgets, then exchange_rates.
 */
object SeedDatabaseCallback : RoomDatabase.Callback() {

    override fun onCreate(db: SupportSQLiteDatabase) {
        super.onCreate(db)
        seedCategories(db)
        seedTransactions(db)
        seedBudgets(db)
    }

    private fun seedCategories(db: SupportSQLiteDatabase) {
        SampleData.categories.forEach { category ->
            db.insert(
                "categories",
                SQLiteDatabase.CONFLICT_IGNORE,
                ContentValues().apply {
                    put("id", category.id)
                    put("name", category.name)
                    put("type", category.type.name)
                    put("color_hex", category.colorHex)
                }
            )
        }
    }

    private fun seedTransactions(db: SupportSQLiteDatabase) {
        SampleData.transactions.forEach { transaction ->
            db.insert(
                "transactions",
                SQLiteDatabase.CONFLICT_IGNORE,
                ContentValues().apply {
                    put("id", transaction.id)
                    put("type", transaction.type.name)
                    put("amount", transaction.amount)
                    put("currency", transaction.currency)
                    put("category_id", transaction.categoryId)
                    put("date_millis", transaction.dateMillis)
                    put("note", transaction.note)
                    put("pending_sync", transaction.pendingSync)
                }
            )
        }
    }

    private fun seedBudgets(db: SupportSQLiteDatabase) {
        SampleData.budgets.forEach { budget ->
            db.insert(
                "budgets",
                SQLiteDatabase.CONFLICT_IGNORE,
                ContentValues().apply {
                    put("id", budget.id)
                    put("category_id", budget.categoryId)
                    put("year", budget.month.year)
                    put("month", budget.month.month)
                    put("limit_amount", budget.limit)
                }
            )
        }
    }
}
