package vn.edu.vlu.moneymanager.data.repository

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import vn.edu.vlu.moneymanager.data.local.dao.ExchangeRateDao
import vn.edu.vlu.moneymanager.data.local.entity.ExchangeRateEntity
import vn.edu.vlu.moneymanager.data.model.ExchangeRates
import vn.edu.vlu.moneymanager.data.source.SampleData

/**
 * Exchange rates cached in the Room table "exchange_rates", so conversions and reports work offline.
 * refresh() simulates the network call that a later milestone makes to a real API, then writes to Room.
 */
class CurrencyRepository(
    private val exchangeRateDao: ExchangeRateDao
) {

    val currencies: List<String> = SampleData.currencies

    /** Re-emits after every refresh. updated_at = 0 (the seeded rows) means the rates were never downloaded. */
    val rates: Flow<ExchangeRates> = exchangeRateDao.observeAll().map { entities ->
        if (entities.isEmpty()) {
            SampleData.exchangeRates
        } else {
            ExchangeRates(
                base = "USD",
                rates = entities.associate { it.currency_code to it.rate_per_usd },
                updatedAtMillis = entities.maxOf { it.updated_at }.takeIf { it > 0 }
            )
        }
    }

    suspend fun refresh() {
        delay(SIMULATED_NETWORK_DELAY_MS)

        val updatedAt = System.currentTimeMillis()
        val entities = SampleData.exchangeRates.rates.map { (currencyCode, rate) ->
            ExchangeRateEntity(
                currency_code = currencyCode,
                rate_per_usd = rate,
                updated_at = updatedAt
            )
        }
        exchangeRateDao.upsertAll(entities)
    }

    private companion object {
        const val SIMULATED_NETWORK_DELAY_MS = 800L
    }
}
