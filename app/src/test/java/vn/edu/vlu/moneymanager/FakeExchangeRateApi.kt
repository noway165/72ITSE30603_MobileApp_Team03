package vn.edu.vlu.moneymanager

import vn.edu.vlu.moneymanager.data.model.ExchangeRateResponse
import vn.edu.vlu.moneymanager.data.remote.ExchangeRateApi
import vn.edu.vlu.moneymanager.data.source.SampleData

/** In-memory ExchangeRate-API: answers with the SampleData rates. Set [failure] to simulate errors (e.g. IOException). */
class FakeExchangeRateApi(
    private val rates: Map<String, Double> = SampleData.exchangeRates.rates
) : ExchangeRateApi {

    var failure: Exception? = null
    var calls = 0
        private set

    override suspend fun getLatestRates(apiKey: String, base: String): ExchangeRateResponse {
        calls++
        failure?.let { throw it }
        return ExchangeRateResponse(result = "success", baseCode = base, conversionRates = rates)
    }
}
