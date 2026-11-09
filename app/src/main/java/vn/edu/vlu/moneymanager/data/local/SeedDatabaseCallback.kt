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
 * categories, then exchange_rates. Transactions and budgets are not seeded: since Milestone 4 they come
 * from the user's MockAPI rows, and sample rows would be uploaded to the account as if they were real.
 */
object SeedDatabaseCallback : RoomDatabase.Callback() {

    override fun onCreate(db: SupportSQLiteDatabase) {
        super.onCreate(db)
        seedCategories(db)
        seedExchangeRates(db)
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

    /** updated_at = 0 means "never downloaded": the converter shows that until the user taps Refresh. */
    private fun seedExchangeRates(db: SupportSQLiteDatabase) {
        SampleData.exchangeRates.rates.forEach { (code, ratePerUsd) ->
            db.insert(
                "exchange_rates",
                SQLiteDatabase.CONFLICT_IGNORE,
                ContentValues().apply {
                    put("currency_code", code)
                    put("rate_per_usd", ratePerUsd)
                    put("updated_at", 0L)
                }
            )
        }
    }
}
