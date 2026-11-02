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
}
