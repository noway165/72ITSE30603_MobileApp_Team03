package vn.edu.vlu.moneymanager.feature.currency

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import vn.edu.vlu.moneymanager.data.AppContainer
import vn.edu.vlu.moneymanager.data.local.entity.ExchangeRateEntity
import vn.edu.vlu.moneymanager.data.repository.CurrencyRepository
import vn.edu.vlu.moneymanager.data.remote.UiState

class CurrencyViewModel(
    private val repository: CurrencyRepository = AppContainer.currencyRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState<List<ExchangeRateEntity>>>(UiState.Loading)
    val uiState: StateFlow<UiState<List<ExchangeRateEntity>>> = _uiState.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            // 1. Luôn lắng nghe dữ liệu từ Room Database
            launch {
                repository.ratesList.collect { rates ->
                    if (rates.isNotEmpty()) {
                        _uiState.value = UiState.Success(rates)
                    }
                }
            }

            // 2. Gọi API để cập nhật DB
            val apiResult = repository.refresh()

            // Xử lý báo lỗi nếu mất mạng và chưa có dữ liệu cache
            if (apiResult is UiState.Error) {
                if (_uiState.value !is UiState.Success)
                    _uiState.value = UiState.Error(apiResult.messageRes)
            }
        }
    }
}