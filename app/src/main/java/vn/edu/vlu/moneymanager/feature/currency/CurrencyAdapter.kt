package vn.edu.vlu.moneymanager.feature.currency

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil3.load
import coil3.request.CachePolicy
import coil3.request.crossfade
import coil3.request.error
import coil3.request.placeholder
import vn.edu.vlu.moneymanager.R
import vn.edu.vlu.moneymanager.data.local.entity.ExchangeRateEntity
import vn.edu.vlu.moneymanager.databinding.ItemExchangeRateBinding
import java.util.Locale

class CurrencyAdapter : ListAdapter<ExchangeRateEntity, CurrencyAdapter.ViewHolder>(DiffCallback) {

    class ViewHolder(private val binding: ItemExchangeRateBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: ExchangeRateEntity) {
            binding.tvCurrencyCode.text = item.currency_code
            binding.tvRate.text = String.format(Locale.US, "%.4f", item.rate_per_usd)

            // Flag = first 2 letters of the currency code (USD -> us, VND -> vn, EUR -> eu)
            val countryCode = item.currency_code.take(2).lowercase()
            binding.ivFlag.load("https://flagcdn.com/w80/$countryCode.png") {
                crossfade(true)
                placeholder(R.drawable.ic_flag_placeholder)
                error(R.drawable.ic_flag_error)
                memoryCachePolicy(CachePolicy.ENABLED)
                diskCachePolicy(CachePolicy.ENABLED)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemExchangeRateBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    companion object DiffCallback : DiffUtil.ItemCallback<ExchangeRateEntity>() {
        override fun areItemsTheSame(oldItem: ExchangeRateEntity, newItem: ExchangeRateEntity): Boolean =
            oldItem.currency_code == newItem.currency_code

        override fun areContentsTheSame(oldItem: ExchangeRateEntity, newItem: ExchangeRateEntity): Boolean =
            oldItem == newItem
    }
}