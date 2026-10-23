package vn.edu.vlu.moneymanager.feature.dashboard.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.IdRes
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import kotlinx.coroutines.launch
import vn.edu.vlu.moneymanager.MainActivity
import vn.edu.vlu.moneymanager.R
import vn.edu.vlu.moneymanager.databinding.FragmentDashboardBinding
import vn.edu.vlu.moneymanager.feature.dashboard.viewmodel.DashboardUiState
import vn.edu.vlu.moneymanager.feature.dashboard.viewmodel.DashboardViewModel
import vn.edu.vlu.moneymanager.feature.transaction.ui.AddEditTransactionActivity
import vn.edu.vlu.moneymanager.feature.transaction.ui.TransactionAdapter
import vn.edu.vlu.moneymanager.util.DateFormatter
import vn.edu.vlu.moneymanager.util.MoneyFormatter
import vn.edu.vlu.moneymanager.util.repeatOnViewStarted

class DashboardFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!

    private val viewModel: DashboardViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val adapter = TransactionAdapter { item ->
            startActivity(AddEditTransactionActivity.newIntent(requireContext(), item.transaction.id))
        }
        binding.rvRecent.adapter = adapter

        binding.emptyRecent.tvEmptyTitle.setText(R.string.empty_transactions_title)
        binding.emptyRecent.tvEmptyMessage.setText(R.string.empty_transactions_message)

        binding.fabAddTransaction.setOnClickListener {
            startActivity(AddEditTransactionActivity.newIntent(requireContext()))
        }
        binding.btnSeeAll.setOnClickListener { openTab(R.id.transactionListFragment) }
        binding.btnQuickBudgets.setOnClickListener { openTab(R.id.budgetFragment) }
        binding.btnQuickCurrency.setOnClickListener {
            findNavController().navigate(R.id.action_dashboard_to_currency)
        }
        binding.cardSyncBanner.setOnClickListener { openTab(R.id.profileFragment) }

        repeatOnViewStarted {
            launch {
                viewModel.uiState.collect { state ->
                    render(state)
                    adapter.submitList(state.recent)
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun render(state: DashboardUiState) {
        binding.cardSyncBanner.isVisible = state.pendingSyncCount > 0
        binding.tvSyncBanner.text = resources.getQuantityString(
            R.plurals.sync_pending_count, state.pendingSyncCount, state.pendingSyncCount
        )
        binding.tvBalanceMonth.text = DateFormatter.monthNumeric(state.month)
        binding.tvBalance.text = MoneyFormatter.format(state.balance, state.currency)
        binding.tvMonthIncome.text = MoneyFormatter.format(state.monthIncome, state.currency)
        binding.tvMonthExpense.text = MoneyFormatter.format(state.monthExpense, state.currency)
        binding.tvMissingRates.isVisible = state.hasMissingRates

        binding.rvRecent.isVisible = state.recent.isNotEmpty()
        binding.emptyRecent.root.isVisible = state.recent.isEmpty()
    }

    private fun openTab(@IdRes destinationId: Int) {
        (activity as? MainActivity)?.openTab(destinationId)
    }
}
