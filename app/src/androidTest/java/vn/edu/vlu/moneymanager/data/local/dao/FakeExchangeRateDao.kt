package vn.edu.vlu.moneymanager.data.local.dao

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import vn.edu.vlu.moneymanager.data.local.entity.ExchangeRateEntity

class FakeExchangeRateDao(
    initialRates: List<ExchangeRateEntity> = listOf(
        ExchangeRateEntity(
            currency_code = "USD",
            rate_per_usd = 1.0,
            updated_at = System.currentTimeMillis()
        ),
        ExchangeRateEntity(
            currency_code = "VND",
            rate_per_usd = 25400.0,
            updated_at = System.currentTimeMillis()
        )
    )
) : ExchangeRateDao {

    private val ratesFlow = MutableStateFlow(initialRates)

    override fun observeAll(): Flow<List<ExchangeRateEntity>> = ratesFlow

    override suspend fun upsertAll(rates: List<ExchangeRateEntity>) {
        ratesFlow.value = rates
    }
}