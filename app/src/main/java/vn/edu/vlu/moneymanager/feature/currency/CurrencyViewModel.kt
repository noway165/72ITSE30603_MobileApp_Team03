package vn.edu.vlu.moneymanager.feature.currency

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import vn.edu.vlu.moneymanager.data.AppContainer
import vn.edu.vlu.moneymanager.data.local.entity.ExchangeRateEntity
import vn.edu.vlu.moneymanager.data.repository.CurrencyRepository

/** Rates list for the RecyclerView; always read from the Room copy, so it also works offline. */
class CurrencyViewModel(
    repository: CurrencyRepository = AppContainer.currencyRepository
) : ViewModel() {

    val rates: StateFlow<List<ExchangeRateEntity>> = repository.ratesList
        .map { list -> list.sortedBy { it.currency_code } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}