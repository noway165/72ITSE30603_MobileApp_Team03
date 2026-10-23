package vn.edu.vlu.moneymanager.feature.budget.ui

import android.annotation.SuppressLint
import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.color.MaterialColors
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch
import vn.edu.vlu.moneymanager.R
import vn.edu.vlu.moneymanager.data.model.BudgetProgress
import vn.edu.vlu.moneymanager.databinding.DialogBudgetBinding
import vn.edu.vlu.moneymanager.databinding.FragmentBudgetBinding
import vn.edu.vlu.moneymanager.feature.budget.viewmodel.BudgetEvent
import vn.edu.vlu.moneymanager.feature.budget.viewmodel.BudgetUiState
import vn.edu.vlu.moneymanager.feature.budget.viewmodel.BudgetViewModel
import vn.edu.vlu.moneymanager.util.DateFormatter
import vn.edu.vlu.moneymanager.util.MoneyFormatter
import vn.edu.vlu.moneymanager.util.repeatOnViewStarted

class BudgetFragment : Fragment() {

    private var _binding: FragmentBudgetBinding? = null
    private val binding get() = _binding!!

    private val viewModel: BudgetViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentBudgetBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val adapter = BudgetAdapter { item -> showBudgetDialog(item) }
        binding.rvBudgets.adapter = adapter
        attachSwipeToDelete(adapter)

        binding.monthSelector.btnPrevMonth.setOnClickListener { viewModel.previousMonth() }
        binding.monthSelector.btnNextMonth.setOnClickListener { viewModel.nextMonth() }
        binding.fabAddBudget.setOnClickListener { showBudgetDialog(null) }

        binding.emptyBudgets.ivEmptyIcon.setImageResource(R.drawable.ic_pie_chart)
        binding.emptyBudgets.tvEmptyTitle.setText(R.string.empty_budgets_title)
        binding.emptyBudgets.tvEmptyMessage.setText(R.string.empty_budgets_message)

        repeatOnViewStarted {
            launch { viewModel.uiState.collect { render(it, adapter) } }
            launch { viewModel.events.collect(::handleEvent) }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    @SuppressLint("NotifyDataSetChanged") // the currency of every row changed
    private fun render(state: BudgetUiState, adapter: BudgetAdapter) {
        binding.monthSelector.tvMonthTitle.text = DateFormatter.monthTitle(state.month)

        if (adapter.currency != state.currency) {
            adapter.currency = state.currency
            adapter.notifyDataSetChanged()
        }
        adapter.submitList(state.items)

        val hasBudgets = state.items.isNotEmpty()
        binding.cardBudgetSummary.isVisible = hasBudgets
        binding.rvBudgets.isVisible = hasBudgets
        binding.emptyBudgets.root.isVisible = !hasBudgets

        val percent = state.totalPercent
        val color = when {
            percent > 100 -> ContextCompat.getColor(requireContext(), R.color.expense)
            percent >= 80 -> ContextCompat.getColor(requireContext(), R.color.warning)
            else -> MaterialColors.getColor(binding.root, androidx.appcompat.R.attr.colorPrimary)
        }
        binding.tvSummaryPercent.text = getString(R.string.percent_value, percent)
        binding.tvSummaryPercent.setTextColor(color)
        binding.progressSummary.setProgressCompat(percent.coerceIn(0, 100), true)
        binding.progressSummary.setIndicatorColor(color)
        binding.tvSummaryAmount.text = getString(
            R.string.budget_spent_of_limit,
            MoneyFormatter.format(state.totalSpent, state.currency),
            MoneyFormatter.format(state.totalLimit, state.currency)
        )
        val remaining = state.totalLimit - state.totalSpent
        binding.tvSummaryRemaining.text = if (remaining >= 0) {
            getString(R.string.budget_left, MoneyFormatter.format(remaining, state.currency))
        } else {
            getString(R.string.budget_over_by, MoneyFormatter.format(-remaining, state.currency))
        }
    }

    private fun handleEvent(event: BudgetEvent) {
        when (event) {
            BudgetEvent.ShowUndoDelete ->
                Snackbar.make(binding.root, R.string.budget_deleted, Snackbar.LENGTH_LONG)
                    .setAnchorView(binding.fabAddBudget)
                    .setAction(R.string.action_undo) { viewModel.undoDelete() }
                    .show()
        }
    }

    /** Add ([editing] == null) or edit a budget of the month shown. Validation happens in the ViewModel. */
    private fun showBudgetDialog(editing: BudgetProgress?) {
        val state = viewModel.uiState.value
        val categories = state.expenseCategories
        val dialogBinding = DialogBudgetBinding.inflate(layoutInflater)
        var selectedCategoryId: Long? = editing?.budget?.categoryId

        dialogBinding.tvBudgetMonth.text = getString(R.string.budget_month, DateFormatter.monthNumeric(state.month))
        dialogBinding.actvBudgetCategory.setAdapter(
            ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, categories.map { it.name })
        )
        dialogBinding.actvBudgetCategory.setText(editing?.category?.name.orEmpty(), false)
        dialogBinding.actvBudgetCategory.setOnItemClickListener { _, _, position, _ ->
            selectedCategoryId = categories[position].id
            dialogBinding.tilBudgetCategory.error = null
        }
        editing?.let {
            val limit = it.budget.limit
            dialogBinding.edtBudgetLimit.setText(if (limit % 1.0 == 0.0) limit.toLong().toString() else limit.toString())
        }

        val builder = MaterialAlertDialogBuilder(requireContext())
            .setTitle(if (editing == null) R.string.title_add_budget else R.string.title_edit_budget)
            .setView(dialogBinding.root)
            .setNegativeButton(R.string.action_cancel, null)
            .setPositiveButton(R.string.action_save, null)
        if (editing != null) {
            builder.setNeutralButton(R.string.action_delete) { _, _ -> viewModel.delete(editing.budget.id) }
        }
        val dialog = builder.create()
        dialog.setOnShowListener {
            // Override the positive button so the dialog stays open while there are errors.
            dialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener {
                val errors = viewModel.saveBudget(
                    editing?.budget?.id,
                    selectedCategoryId,
                    dialogBinding.edtBudgetLimit.text.toString()
                )
                dialogBinding.tilBudgetCategory.error = errors.categoryError?.let(::getString)
                dialogBinding.tilBudgetLimit.error = errors.limitError?.let(::getString)
                if (!errors.hasErrors) dialog.dismiss()
            }
        }
        dialog.show()
    }

    private fun attachSwipeToDelete(adapter: BudgetAdapter) {
        val callback = object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
            override fun onMove(rv: RecyclerView, vh: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder) = false

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) viewModel.delete(adapter.currentList[position].budget.id)
            }
        }
        ItemTouchHelper(callback).attachToRecyclerView(binding.rvBudgets)
    }
}
