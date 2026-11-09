package vn.edu.vlu.moneymanager.feature.currency.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import vn.edu.vlu.moneymanager.data.AppContainer
import vn.edu.vlu.moneymanager.data.remote.UiState
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
    /** State of the last ExchangeRate-API call; the rates themselves always come from the Room copy */
    val sync: UiState<Unit> = UiState.Loading
) {
    val isRefreshing: Boolean get() = sync is UiState.Loading
}

class CurrencyConverterViewModel(
    private val currencyRepository: CurrencyRepository = AppContainer.currencyRepository
) : ViewModel() {

    private val input = MutableStateFlow(ConverterInput(to = AppContainer.settingsRepository.defaultCurrency.value))
    private val sync = MutableStateFlow<UiState<Unit>>(UiState.Loading)
    private var refreshJob: Job? = null

    val uiState: StateFlow<CurrencyConverterUiState> = combine(
        input,
        currencyRepository.rates,
        sync
    ) { input, rates, syncState ->
        val rate = rates.rate(input.from, input.to)
        val amount = Validators.parseAmount(input.amount)
        CurrencyConverterUiState(
            input = input,
            currencies = currencyRepository.currencies,
            result = if (rate != null && amount != null) amount * rate else null,
            rate = rate,
            lastUpdatedMillis = rates.updatedAtMillis,
            sync = syncState
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CurrencyConverterUiState(input = input.value))

    init {
        refreshRates()
    }

    fun onAmountChanged(text: String) = input.update { it.copy(amount = text) }

    fun onFromSelected(code: String) = input.update { it.copy(from = code) }

    fun onToSelected(code: String) = input.update { it.copy(to = code) }

    fun swap() = input.update { it.copy(from = it.to, to = it.from) }

    /** Loading -> Success, or Error (offline, timeout, wrong key...) while the cached rates stay on screen. */
    fun refreshRates() {
        if (refreshJob?.isActive == true) return
        refreshJob = viewModelScope.launch {
            sync.value = UiState.Loading
            sync.value = currencyRepository.refresh()
        }
    }
}
