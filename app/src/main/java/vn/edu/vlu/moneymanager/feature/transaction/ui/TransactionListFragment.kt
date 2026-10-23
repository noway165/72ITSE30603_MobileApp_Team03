package vn.edu.vlu.moneymanager.feature.transaction.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.util.Pair
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch
import vn.edu.vlu.moneymanager.R
import vn.edu.vlu.moneymanager.data.model.TransactionType
import vn.edu.vlu.moneymanager.databinding.FragmentTransactionListBinding
import vn.edu.vlu.moneymanager.feature.transaction.viewmodel.TransactionListEvent
import vn.edu.vlu.moneymanager.feature.transaction.viewmodel.TransactionListUiState
import vn.edu.vlu.moneymanager.feature.transaction.viewmodel.TransactionListViewModel
import vn.edu.vlu.moneymanager.util.DateFormatter
import vn.edu.vlu.moneymanager.util.DateUtils
import vn.edu.vlu.moneymanager.util.MoneyFormatter
import vn.edu.vlu.moneymanager.util.repeatOnViewStarted

class TransactionListFragment : Fragment() {

    private var _binding: FragmentTransactionListBinding? = null
    private val binding get() = _binding!!

    private val viewModel: TransactionListViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentTransactionListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val adapter = TransactionAdapter { item ->
            startActivity(AddEditTransactionActivity.newIntent(requireContext(), item.transaction.id))
        }
        binding.rvTransactions.adapter = adapter
        attachSwipeToDelete(adapter)

        binding.edtSearch.setText(viewModel.uiState.value.filter.query)
        binding.edtSearch.doAfterTextChanged { viewModel.setQuery(it.toString()) }
        binding.chipAll.setOnClickListener { selectType(null) }
        binding.chipIncome.setOnClickListener { selectType(TransactionType.INCOME) }
        binding.chipExpense.setOnClickListener { selectType(TransactionType.EXPENSE) }
        binding.chipCategory.setOnClickListener { showCategoryChooser() }
        binding.chipCategory.setOnCloseIconClickListener { viewModel.setCategory(null) }
        binding.chipDateRange.setOnClickListener { showDateRangePicker() }
        binding.chipDateRange.setOnCloseIconClickListener { viewModel.setDateRange(null, null) }
        binding.fabAddTransaction.setOnClickListener {
            startActivity(AddEditTransactionActivity.newIntent(requireContext()))
        }

        (childFragmentManager.findFragmentByTag(TAG_DATE_RANGE) as? MaterialDatePicker<*>)?.let(::listenToDateRange)

        repeatOnViewStarted {
            launch { viewModel.uiState.collect { render(it, adapter) } }
            launch { viewModel.events.collect(::handleEvent) }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun render(state: TransactionListUiState, adapter: TransactionAdapter) {
        adapter.submitList(state.items)
        renderTypeChips(state.filter.type)

        binding.chipCategory.text = state.selectedCategory?.name ?: getString(R.string.filter_category)
        binding.chipCategory.isCloseIconVisible = state.selectedCategory != null

        val from = state.filter.dateFrom
        val to = state.filter.dateTo
        binding.chipDateRange.text = if (from != null && to != null) {
            "${DateFormatter.shortDate(from)} – ${DateFormatter.shortDate(to)}"
        } else {
            getString(R.string.filter_date)
        }
        binding.chipDateRange.isCloseIconVisible = from != null

        binding.tvResultSummary.text = resources.getQuantityString(
            R.plurals.transaction_summary,
            state.items.size,
            state.items.size,
            MoneyFormatter.format(state.net, state.currency)
        )

        val isEmpty = state.items.isEmpty()
        binding.rvTransactions.isVisible = !isEmpty
        binding.emptyTransactions.root.isVisible = isEmpty
        if (isEmpty) {
            val filtered = state.hasAnyTransactions && state.filter.isActive
            binding.emptyTransactions.ivEmptyIcon.setImageResource(if (filtered) R.drawable.ic_search else R.drawable.ic_list)
            binding.emptyTransactions.tvEmptyTitle.setText(
                if (filtered) R.string.empty_filter_title else R.string.empty_transactions_title
            )
            binding.emptyTransactions.tvEmptyMessage.setText(
                if (filtered) R.string.empty_filter_message else R.string.empty_transactions_message
            )
        }
    }

    private fun handleEvent(event: TransactionListEvent) {
        when (event) {
            TransactionListEvent.ShowUndoDelete ->
                Snackbar.make(binding.root, R.string.transaction_deleted, Snackbar.LENGTH_LONG)
                    .setAnchorView(binding.fabAddTransaction)
                    .setAction(R.string.action_undo) { viewModel.undoDelete() }
                    .show()
        }
    }

    private fun selectType(type: TransactionType?) {
        viewModel.setType(type)
        // A filter chip toggles itself on click; re-apply the state at once in case it did not change.
        renderTypeChips(type)
    }

    private fun renderTypeChips(type: TransactionType?) {
        binding.chipAll.isChecked = type == null
        binding.chipIncome.isChecked = type == TransactionType.INCOME
        binding.chipExpense.isChecked = type == TransactionType.EXPENSE
    }

    private fun showCategoryChooser() {
        val state = viewModel.uiState.value
        val categories = state.categories
        val labels = listOf(getString(R.string.filter_all_categories)) + categories.map { it.name }
        val checked = categories.indexOfFirst { it.id == state.filter.categoryId } + 1
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.filter_category)
            .setSingleChoiceItems(labels.toTypedArray(), checked) { dialog, which ->
                viewModel.setCategory(if (which == 0) null else categories[which - 1].id)
                dialog.dismiss()
            }
            .show()
    }

    private fun showDateRangePicker() {
        if (childFragmentManager.findFragmentByTag(TAG_DATE_RANGE) != null) return
        val filter = viewModel.uiState.value.filter
        val builder = MaterialDatePicker.Builder.dateRangePicker().setTitleText(R.string.filter_date)
        if (filter.dateFrom != null && filter.dateTo != null) {
            builder.setSelection(Pair(DateUtils.toPickerUtc(filter.dateFrom), DateUtils.toPickerUtc(filter.dateTo)))
        }
        val picker = builder.build()
        listenToDateRange(picker)
        picker.show(childFragmentManager, TAG_DATE_RANGE)
    }

    private fun listenToDateRange(picker: MaterialDatePicker<*>) {
        picker.addOnPositiveButtonClickListener { selection ->
            val range = selection as? Pair<*, *> ?: return@addOnPositiveButtonClickListener
            viewModel.setDateRange(range.first as? Long, range.second as? Long)
        }
    }

    private fun attachSwipeToDelete(adapter: TransactionAdapter) {
        val callback = object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
            override fun onMove(rv: RecyclerView, vh: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder) = false

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    viewModel.delete(adapter.currentList[position].transaction.id)
                }
            }
        }
        ItemTouchHelper(callback).attachToRecyclerView(binding.rvTransactions)
    }

    private companion object {
        const val TAG_DATE_RANGE = "transaction_date_range"
    }
}
