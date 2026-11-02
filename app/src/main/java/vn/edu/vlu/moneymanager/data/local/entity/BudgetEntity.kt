package vn.edu.vlu.moneymanager.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import vn.edu.vlu.moneymanager.data.model.Budget
import vn.edu.vlu.moneymanager.data.model.MonthKey

/**
 * Table "budgets": one spending limit per expense category per month.
 * Deleting the category deletes its budgets (ON DELETE CASCADE); the UNIQUE index stops a second budget
 * for the same category in the same month. The index also serves the foreign key lookup on category_id.
 */
@Entity(
    tableName = "budgets",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["category_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["category_id", "year", "month"], unique = true)]
)
data class BudgetEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,
    @ColumnInfo(name = "category_id")
    val categoryId: Long,
    @ColumnInfo(name = "year")
    val year: Int,
    @ColumnInfo(name = "month")
    val month: Int,
    @ColumnInfo(name = "limit_amount")
    val limitAmount: Double
) {
    fun toModel() = Budget(id, categoryId, MonthKey(year, month), limitAmount)

    companion object {
        fun fromModel(budget: Budget) =
            BudgetEntity(budget.id, budget.categoryId, budget.month.year, budget.month.month, budget.limit)
    }
}
