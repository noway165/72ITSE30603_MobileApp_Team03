package vn.edu.vlu.moneymanager.data.local

import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import vn.edu.vlu.moneymanager.data.local.entity.ExchangeRateEntity

class SeedDatabaseCallback(
    private val databaseProvider: () -> AppDatabase,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) : RoomDatabase.Callback() {

    override fun onCreate(db: SupportSQLiteDatabase) {
        super.onCreate(db)
        //seedExchangeRates(db)
        scope.launch {
            populateDatabase(databaseProvider())
        }
    }

    private suspend fun populateDatabase(database: AppDatabase) {
        val seedExchangeRates = listOf(
            ExchangeRateEntity(
                currency_code = "USD",
                rate_per_usd = 1.0,
                updated_at = System.currentTimeMillis()
            ),
            ExchangeRateEntity(
                currency_code = "VND",
                rate_per_usd = 25400.0,
                updated_at = System.currentTimeMillis()
            ),
            ExchangeRateEntity(
                currency_code = "EUR",
                rate_per_usd = 0.92,
                updated_at = System.currentTimeMillis()
            ),
            ExchangeRateEntity(
                currency_code = "JPY",
                rate_per_usd = 155.0,
                updated_at = System.currentTimeMillis()
            ),
            ExchangeRateEntity(
                currency_code = "GBP",
                rate_per_usd = 0.79,
                updated_at = System.currentTimeMillis()
            )
        )
        database.exchangeRateDao().upsertAll(seedExchangeRates)
    }
}