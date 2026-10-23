package vn.edu.vlu.moneymanager.feature.currency.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import kotlinx.coroutines.launch
import vn.edu.vlu.moneymanager.R
import vn.edu.vlu.moneymanager.databinding.FragmentCurrencyConverterBinding
import vn.edu.vlu.moneymanager.feature.currency.viewmodel.CurrencyConverterUiState
import vn.edu.vlu.moneymanager.feature.currency.viewmodel.CurrencyConverterViewModel
import vn.edu.vlu.moneymanager.util.DateFormatter
import vn.edu.vlu.moneymanager.util.MoneyFormatter
import vn.edu.vlu.moneymanager.util.repeatOnViewStarted

class CurrencyConverterFragment : Fragment() {

    private var _binding: FragmentCurrencyConverterBinding? = null
    private val binding get() = _binding!!

    private val viewModel: CurrencyConverterViewModel by viewModels()

    private var shownCurrencies: List<String> = emptyList()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentCurrencyConverterBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        shownCurrencies = emptyList() // new view, new dropdowns: adapters must be set again

        binding.edtConvertAmount.setText(viewModel.uiState.value.input.amount)
        binding.edtConvertAmount.doAfterTextChanged { viewModel.onAmountChanged(it.toString()) }
        binding.actvFromCurrency.setOnItemClickListener { _, _, position, _ ->
            viewModel.onFromSelected(shownCurrencies[position])
        }
        binding.actvToCurrency.setOnItemClickListener { _, _, position, _ ->
            viewModel.onToSelected(shownCurrencies[position])
        }
        binding.btnSwap.setOnClickListener { viewModel.swap() }
        binding.btnRefreshRates.setOnClickListener { viewModel.refreshRates() }

        repeatOnViewStarted {
            launch { viewModel.uiState.collect(::render) }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun render(state: CurrencyConverterUiState) {
        val input = state.input
        if (state.currencies != shownCurrencies) {
            shownCurrencies = state.currencies
            binding.actvFromCurrency.setAdapter(ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, shownCurrencies))
            binding.actvToCurrency.setAdapter(ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, shownCurrencies))
        }
        binding.actvFromCurrency.setText(input.from, false)
        binding.actvToCurrency.setText(input.to, false)

        binding.tvConvertResult.text = state.result?.let { MoneyFormatter.format(it, input.to) } ?: "—"
        binding.tvRate.text = state.rate?.let {
            getString(R.string.converter_rate, input.from, MoneyFormatter.formatRate(it), input.to)
        } ?: getString(R.string.converter_no_rate)

        binding.tvLastUpdated.text = state.lastUpdatedMillis?.let {
            getString(R.string.converter_updated_at, DateFormatter.dateTime(it))
        } ?: getString(R.string.converter_never_updated)
        binding.progressRates.isVisible = state.isRefreshing
        binding.btnRefreshRates.isEnabled = !state.isRefreshing
        binding.tvOfflineNote.isVisible = state.refreshFailed
    }
}
