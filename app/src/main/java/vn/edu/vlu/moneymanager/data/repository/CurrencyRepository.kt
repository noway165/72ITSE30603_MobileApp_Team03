package vn.edu.vlu.moneymanager.data.repository

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import vn.edu.vlu.moneymanager.data.model.ExchangeRates
import vn.edu.vlu.moneymanager.data.source.SampleData

/** Exchange rates. refresh() simulates the network call that a later milestone makes to a real API. */
class CurrencyRepository {

    val currencies: List<String> = SampleData.currencies

    private val _rates = MutableStateFlow(SampleData.exchangeRates)
    val rates: StateFlow<ExchangeRates> = _rates.asStateFlow()

    suspend fun refresh() {
        delay(SIMULATED_NETWORK_DELAY_MS)
        _rates.update { it.copy(updatedAtMillis = System.currentTimeMillis()) }
    }

    private companion object {
        const val SIMULATED_NETWORK_DELAY_MS = 800L
    }
}
