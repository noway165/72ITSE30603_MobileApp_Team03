package vn.edu.vlu.moneymanager.feature.currency

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import vn.edu.vlu.moneymanager.FakeExchangeRateApi
import vn.edu.vlu.moneymanager.FakeExchangeRateDao
import vn.edu.vlu.moneymanager.data.local.entity.ExchangeRateEntity
import vn.edu.vlu.moneymanager.data.remote.UiState
import vn.edu.vlu.moneymanager.data.repository.CurrencyRepository
import vn.edu.vlu.moneymanager.data.source.SampleData

/** Milestone 5: the rates list shows (and refresh stores) only the currencies the app offers. */
class CurrencyRepositoryTest {

    // Two of the 160 extra currencies ExchangeRate-API sends besides the six of the app
    private val extraRates = mapOf("AED" to 3.67, "ZWL" to 322.0)

    @Test
    fun ratesList_keepsOnlyAppCurrencies_sortedByCode() = runTest {
        // A database filled before Milestone 5 also holds currencies the app never offers
        val dao = FakeExchangeRateDao(
            initialRates = (SampleData.exchangeRates.rates + extraRates).map { (code, rate) ->
                ExchangeRateEntity(currency_code = code, rate_per_usd = rate, updated_at = 0L)
            }
        )
        val repository = CurrencyRepository(dao, FakeExchangeRateApi(), Dispatchers.Unconfined)

        val codes = repository.ratesList.first().map { it.currency_code }

        assertEquals(SampleData.currencies.sorted(), codes)
    }

    @Test
    fun refresh_storesOnlyAppCurrencies() = runTest {
        val dao = FakeExchangeRateDao(initialRates = emptyList())
        val api = FakeExchangeRateApi(rates = SampleData.exchangeRates.rates + extraRates)
        val repository = CurrencyRepository(dao, api, Dispatchers.Unconfined)

        val result = repository.refresh()

        assertTrue(result is UiState.Success)
        val storedCodes = dao.observeAll().first().map { it.currency_code }.toSet()
        assertEquals(SampleData.currencies.toSet(), storedCodes)
    }
}
