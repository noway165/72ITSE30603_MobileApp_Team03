package vn.edu.vlu.moneymanager.feature.currency

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import vn.edu.vlu.moneymanager.MainDispatcherRule
import vn.edu.vlu.moneymanager.collectInBackground
import vn.edu.vlu.moneymanager.data.repository.CurrencyRepository
import vn.edu.vlu.moneymanager.feature.currency.viewmodel.CurrencyConverterViewModel

@OptIn(ExperimentalCoroutinesApi::class)
class CurrencyConverterViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = CurrencyRepository()
    private val codes = repository.rates.value.rates.keys.toList()
    private val a get() = codes[0]
    private val b get() = codes[1]

    private fun createViewModel() = CurrencyConverterViewModel(repository)

    @Test
    fun initialState_hasDefaultAmountAndCurrencyList() = runTest {
        val viewModel = createViewModel()
        collectInBackground(viewModel.uiState)

        val state = viewModel.uiState.value
        assertEquals("100", state.input.amount)
        assertEquals("USD", state.input.from)
        assertEquals(repository.currencies, state.currencies)
    }

    @Test
    fun validAmount_resultIsAmountTimesRate() = runTest {
        val viewModel = createViewModel()
        collectInBackground(viewModel.uiState)

        viewModel.onFromSelected(a)
        viewModel.onToSelected(b)
        viewModel.onAmountChanged("250")

        val expectedRate = repository.rates.value.rate(a, b)!!
        val state = viewModel.uiState.value
        assertEquals(expectedRate, state.rate!!, DELTA)
        assertEquals(250 * expectedRate, state.result!!, DELTA)
    }

    @Test
    fun sameCurrency_rateIsOne() = runTest {
        val viewModel = createViewModel()
        collectInBackground(viewModel.uiState)

        viewModel.onFromSelected(a)
        viewModel.onToSelected(a)
        viewModel.onAmountChanged("100")

        assertEquals(1.0, viewModel.uiState.value.rate!!, DELTA)
        assertEquals(100.0, viewModel.uiState.value.result!!, DELTA)
    }

    @Test
    fun amountWithThousandsSeparator_isParsed() = runTest {
        val viewModel = createViewModel()
        collectInBackground(viewModel.uiState)

        viewModel.onFromSelected(a)
        viewModel.onToSelected(a)
        viewModel.onAmountChanged("85,000")

        assertEquals(85_000.0, viewModel.uiState.value.result!!, DELTA)
    }

    @Test
    fun invalidAmount_resultIsNull() = runTest {
        val viewModel = createViewModel()
        collectInBackground(viewModel.uiState)
        viewModel.onFromSelected(a)
        viewModel.onToSelected(b)

        for (bad in listOf("", "abc", "0", "-5")) {
            viewModel.onAmountChanged(bad)
            assertNull("amount \"$bad\" should give no result", viewModel.uiState.value.result)
        }
    }

    @Test
    fun unknownCurrency_hasNoRateAndNoResult() = runTest {
        val viewModel = createViewModel()
        collectInBackground(viewModel.uiState)

        viewModel.onFromSelected("XXX")
        viewModel.onToSelected(a)
        viewModel.onAmountChanged("100")

        assertNull(viewModel.uiState.value.rate)
        assertNull(viewModel.uiState.value.result)
    }

    @Test
    fun swap_exchangesFromAndTo() = runTest {
        val viewModel = createViewModel()
        collectInBackground(viewModel.uiState)
        viewModel.onFromSelected(a)
        viewModel.onToSelected(b)

        viewModel.swap()

        assertEquals(b, viewModel.uiState.value.input.from)
        assertEquals(a, viewModel.uiState.value.input.to)
    }

    @Test
    fun refreshRates_showsSpinnerThenFinishesWithoutError() = runTest {
        val viewModel = createViewModel()
        collectInBackground(viewModel.uiState)

        viewModel.refreshRates()
        assertTrue(viewModel.uiState.value.isRefreshing)

        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isRefreshing)
        assertFalse(state.refreshFailed)
        assertNotNull(state.lastUpdatedMillis)
    }

    private companion object {
        const val DELTA = 1e-9
    }
}