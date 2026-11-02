
package vn.edu.vlu.moneymanager.data.repository

import kotlinx.coroutines.delay
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import vn.edu.vlu.moneymanager.data.local.dao.ExchangeRateDao
import vn.edu.vlu.moneymanager.data.local.entity.ExchangeRateEntity
import vn.edu.vlu.moneymanager.data.model.ExchangeRates
import vn.edu.vlu.moneymanager.data.source.SampleData

/**
 * Exchange rates. refresh() simulates the network call
 * that a later milestone makes to a real API.
 */
class CurrencyRepository(
    private val exchangeRateDao: ExchangeRateDao
) {

    val currencies: List<String> = SampleData.currencies

    private val repositoryScope =
        CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val rates: StateFlow<ExchangeRates> =
        exchangeRateDao.observeAll()
            .map { entities ->
                if (entities.isEmpty()) {
                    SampleData.exchangeRates
                } else {
                    ExchangeRates(
                        base = "USD",
                        rates = entities.associate {
                            it.currency_code to it.rate_per_usd
                        },
                        updatedAtMillis = entities.maxOfOrNull {
                            it.updated_at
                        }
                    )
                }
            }
            .stateIn(
                repositoryScope,
                SharingStarted.Eagerly,
                SampleData.exchangeRates
            )

    suspend fun refresh() {
        delay(SIMULATED_NETWORK_DELAY_MS)

        val updatedAt = System.currentTimeMillis()

        val entities = SampleData.exchangeRates.rates.map {
                (currencyCode, rate) ->
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
