package vn.edu.vlu.moneymanager

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import vn.edu.vlu.moneymanager.data.local.dao.ExchangeRateDao
import vn.edu.vlu.moneymanager.data.local.entity.ExchangeRateEntity
import vn.edu.vlu.moneymanager.data.source.SampleData

/**
 * In-memory ExchangeRateDao for JVM unit tests, started with the same rows the database is seeded with
 * (SampleData rates, updated_at = 0 = never downloaded). upsertAll replaces rows with the same currency code.
 */
class FakeExchangeRateDao(
    initialRates: List<ExchangeRateEntity> = SampleData.exchangeRates.rates.map { (code, rate) ->
        ExchangeRateEntity(currency_code = code, rate_per_usd = rate, updated_at = 0L)
    }
) : ExchangeRateDao {

    private val rows = MutableStateFlow(initialRates)

    override fun observeAll(): Flow<List<ExchangeRateEntity>> = rows

    override suspend fun upsertAll(rates: List<ExchangeRateEntity>) {
        rows.update { current ->
            val newCodes = rates.map { it.currency_code }.toSet()
            current.filterNot { it.currency_code in newCodes } + rates
        }
    }
}
