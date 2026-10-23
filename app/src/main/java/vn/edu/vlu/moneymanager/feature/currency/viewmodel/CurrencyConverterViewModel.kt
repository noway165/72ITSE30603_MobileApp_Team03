package vn.edu.vlu.moneymanager.feature.currency.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import vn.edu.vlu.moneymanager.data.AppContainer
import vn.edu.vlu.moneymanager.data.repository.CurrencyRepository
import vn.edu.vlu.moneymanager.util.Validators

data class ConverterInput(
    val amount: String = "100",
    val from: String = "USD",
    val to: String = ""
)

data class CurrencyConverterUiState(
    val input: ConverterInput = ConverterInput(),
    val currencies: List<String> = emptyList(),
    /** null when the amount is invalid or no rate exists for the pair */
    val result: Double? = null,
    val rate: Double? = null,
    val lastUpdatedMillis: Long? = null,
    val isRefreshing: Boolean = false,
    val refreshFailed: Boolean = false
)

class CurrencyConverterViewModel(
    private val currencyRepository: CurrencyRepository = AppContainer.currencyRepository
) : ViewModel() {

    private val input = MutableStateFlow(ConverterInput(to = AppContainer.settingsRepository.defaultCurrency.value))
    private val isRefreshing = MutableStateFlow(false)
    private val refreshFailed = MutableStateFlow(false)

    val uiState: StateFlow<CurrencyConverterUiState> = combine(
        input,
        currencyRepository.rates,
        isRefreshing,
        refreshFailed
    ) { input, rates, refreshing, failed ->
        val rate = rates.rate(input.from, input.to)
        val amount = Validators.parseAmount(input.amount)
        CurrencyConverterUiState(
            input = input,
            currencies = currencyRepository.currencies,
            result = if (rate != null && amount != null) amount * rate else null,
            rate = rate,
            lastUpdatedMillis = rates.updatedAtMillis,
            isRefreshing = refreshing,
            refreshFailed = failed
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CurrencyConverterUiState(input = input.value))

    fun onAmountChanged(text: String) = input.update { it.copy(amount = text) }

    fun onFromSelected(code: String) = input.update { it.copy(from = code) }

    fun onToSelected(code: String) = input.update { it.copy(to = code) }

    fun swap() = input.update { it.copy(from = it.to, to = it.from) }

    fun refreshRates() {
        if (isRefreshing.value) return
        viewModelScope.launch {
            isRefreshing.value = true
            refreshFailed.value = runCatching { currencyRepository.refresh() }.isFailure
            isRefreshing.value = false
        }
    }
}
