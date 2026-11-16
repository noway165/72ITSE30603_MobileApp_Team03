package vn.edu.vlu.moneymanager.data.repository

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import vn.edu.vlu.moneymanager.BuildConfig
import vn.edu.vlu.moneymanager.data.local.dao.ExchangeRateDao
import vn.edu.vlu.moneymanager.data.local.entity.ExchangeRateEntity
import vn.edu.vlu.moneymanager.data.model.ExchangeRates
import vn.edu.vlu.moneymanager.data.remote.ExchangeRateApi
import vn.edu.vlu.moneymanager.data.remote.UiState
import vn.edu.vlu.moneymanager.data.remote.apiCall
import vn.edu.vlu.moneymanager.data.source.SampleData

/**
 * Exchange rates cached in the Room table "exchange_rates", so conversions and reports work offline.
 * refresh() downloads the live rates from ExchangeRate-API and writes them to Room.
 */
class CurrencyRepository(
    private val exchangeRateDao: ExchangeRateDao,
    private val api: ExchangeRateApi,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {

    val currencies: List<String> = SampleData.currencies

    private val supportedCodes: Set<String> = currencies.toSet()

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

    /**
     * Rows of the rates list: only the currencies the app offers ([currencies]), sorted by code.
     * A database filled before Milestone 5 still holds all 166 currencies of ExchangeRate-API.
     */
    val ratesList: Flow<List<ExchangeRateEntity>> = exchangeRateDao.observeAll().map { entities ->
        entities.filter { it.currency_code in supportedCodes }.sortedBy { it.currency_code }
    }

    /**
     * A failed call (wrong key, quota, no network) throws, apiCall turns it into UiState.Error,
     * and the screens keep showing the cached rows from Room.
     */
    suspend fun refresh(): UiState<Unit> = apiCall(ioDispatcher) {
        val response = api.getLatestRates(BuildConfig.EXCHANGE_RATE_API_KEY, BASE_CURRENCY)
        // The API answers with 166 currencies; the app converts between 6, so only those are stored
        val ratesMap = response.conversionRates.orEmpty().filterKeys { it in supportedCodes }
        check(response.result == "success" && ratesMap.isNotEmpty()) { "empty rates" }
        val now = System.currentTimeMillis()
        exchangeRateDao.upsertAll(
            ratesMap.map { (code, rate) ->
                ExchangeRateEntity(currency_code = code, rate_per_usd = rate, updated_at = now)
            }
        )
    }

    private companion object {
        const val BASE_CURRENCY = "USD"
    }
}