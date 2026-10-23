package vn.edu.vlu.moneymanager.feature.category.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import vn.edu.vlu.moneymanager.R
import vn.edu.vlu.moneymanager.databinding.ItemCategoryBinding
import vn.edu.vlu.moneymanager.feature.category.viewmodel.CategoryItem
import vn.edu.vlu.moneymanager.util.showCategoryInitial

class CategoryAdapter(
    private val onEdit: (CategoryItem) -> Unit
) : ListAdapter<CategoryItem, CategoryAdapter.ViewHolder>(DIFF) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
        ViewHolder(ItemCategoryBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(getItem(position))

    inner class ViewHolder(private val binding: ItemCategoryBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: CategoryItem) {
            binding.tvCategoryInitial.showCategoryInitial(item.category)
            binding.tvCategoryName.text = item.category.name
            binding.tvCategoryUsage.text = binding.root.resources.getQuantityString(
                R.plurals.category_usage, item.usageCount, item.usageCount
            )
            binding.btnEditCategory.setOnClickListener { onEdit(item) }
            binding.root.setOnClickListener { onEdit(item) }
        }
    }

    private companion object {
        val DIFF = object : DiffUtil.ItemCallback<CategoryItem>() {
            override fun areItemsTheSame(oldItem: CategoryItem, newItem: CategoryItem) =
                oldItem.category.id == newItem.category.id

            override fun areContentsTheSame(oldItem: CategoryItem, newItem: CategoryItem) = oldItem == newItem
        }
    }
}
