package vn.edu.vlu.moneymanager.feature.budget.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.color.MaterialColors
import vn.edu.vlu.moneymanager.R
import vn.edu.vlu.moneymanager.data.model.BudgetProgress
import vn.edu.vlu.moneymanager.databinding.ItemBudgetBinding
import vn.edu.vlu.moneymanager.util.MoneyFormatter
import vn.edu.vlu.moneymanager.util.showCategoryInitial

class BudgetAdapter(
    private val onClick: (BudgetProgress) -> Unit
) : ListAdapter<BudgetProgress, BudgetAdapter.ViewHolder>(DIFF) {

    /** Currency of the budget limits (the default currency); set before submitList. */
    var currency: String = ""

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
        ViewHolder(ItemBudgetBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(getItem(position))

    inner class ViewHolder(private val binding: ItemBudgetBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: BudgetProgress) {
            val context = binding.root.context
            val percent = item.percent
            val isOver = percent > 100

            binding.tvCategoryInitial.showCategoryInitial(item.category)
            binding.tvCategoryName.text = item.category?.name ?: context.getString(R.string.uncategorized)
            binding.tvPercent.text = context.getString(R.string.percent_value, percent)
            binding.tvSpentOfLimit.text = context.getString(
                R.string.budget_spent_of_limit,
                MoneyFormatter.format(item.spent, currency),
                MoneyFormatter.format(item.budget.limit, currency)
            )

            // >= 80%: warning color, > 100%: expense color + "Over budget by ..." (see docs/UI_LAYOUTS.md)
            val color = when {
                isOver -> ContextCompat.getColor(context, R.color.expense)
                percent >= 80 -> ContextCompat.getColor(context, R.color.warning)
                else -> MaterialColors.getColor(binding.root, androidx.appcompat.R.attr.colorPrimary)
            }
            binding.progressBudget.setProgressCompat(percent.coerceIn(0, 100), false)
            binding.progressBudget.setIndicatorColor(color)
            binding.tvPercent.setTextColor(color)

            binding.tvRemaining.isVisible = !isOver
            binding.tvRemaining.text = context.getString(R.string.budget_left, MoneyFormatter.format(item.remaining, currency))
            binding.tvOverBudget.isVisible = isOver
            binding.tvOverBudget.text = context.getString(R.string.budget_over_by, MoneyFormatter.format(-item.remaining, currency))

            binding.root.setOnClickListener { onClick(item) }
        }
    }

    private companion object {
        val DIFF = object : DiffUtil.ItemCallback<BudgetProgress>() {
            override fun areItemsTheSame(oldItem: BudgetProgress, newItem: BudgetProgress) =
                oldItem.budget.id == newItem.budget.id

            override fun areContentsTheSame(oldItem: BudgetProgress, newItem: BudgetProgress) = oldItem == newItem
        }
    }
}
