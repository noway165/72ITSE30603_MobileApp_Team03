package vn.edu.vlu.moneymanager.feature.transaction.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import vn.edu.vlu.moneymanager.R
import vn.edu.vlu.moneymanager.data.model.TransactionDetail
import vn.edu.vlu.moneymanager.data.model.TransactionType
import vn.edu.vlu.moneymanager.databinding.ItemTransactionBinding
import vn.edu.vlu.moneymanager.util.DateFormatter
import vn.edu.vlu.moneymanager.util.MoneyFormatter
import vn.edu.vlu.moneymanager.util.showCategoryInitial

/** Shared by Dashboard (recent transactions) and the Transactions tab. */
class TransactionAdapter(
    private val onClick: (TransactionDetail) -> Unit
) : ListAdapter<TransactionDetail, TransactionAdapter.ViewHolder>(DIFF) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
        ViewHolder(ItemTransactionBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(getItem(position))

    inner class ViewHolder(private val binding: ItemTransactionBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: TransactionDetail) {
            val transaction = item.transaction
            val context = binding.root.context
            binding.tvCategoryInitial.showCategoryInitial(item.category)
            binding.tvTitle.text = item.category?.name ?: context.getString(R.string.uncategorized)
            binding.tvSubtitle.text = listOfNotNull(
                transaction.note.takeIf { it.isNotBlank() },
                DateFormatter.date(transaction.dateMillis)
            ).joinToString(" · ")
            binding.tvAmount.text = MoneyFormatter.formatSigned(transaction.amount, transaction.currency, transaction.type)
            binding.tvAmount.setTextColor(
                ContextCompat.getColor(context, if (transaction.type == TransactionType.INCOME) R.color.income else R.color.expense)
            )
            binding.ivSyncPending.isVisible = transaction.pendingSync
            binding.root.setOnClickListener { onClick(item) }
        }
    }

    private companion object {
        val DIFF = object : DiffUtil.ItemCallback<TransactionDetail>() {
            override fun areItemsTheSame(oldItem: TransactionDetail, newItem: TransactionDetail) =
                oldItem.transaction.id == newItem.transaction.id

            override fun areContentsTheSame(oldItem: TransactionDetail, newItem: TransactionDetail) = oldItem == newItem
        }
    }
}
