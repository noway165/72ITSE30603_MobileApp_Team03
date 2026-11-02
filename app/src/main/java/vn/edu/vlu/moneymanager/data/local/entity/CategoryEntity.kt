package vn.edu.vlu.moneymanager.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import vn.edu.vlu.moneymanager.data.model.Category
import vn.edu.vlu.moneymanager.data.model.TransactionType

/** Table "categories". A name can be used once per type (an expense and an income category may share it). */
@Entity(
    tableName = "categories",
    indices = [Index(value = ["name", "type"], unique = true)]
)
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,
    @ColumnInfo(name = "name")
    val name: String,
    @ColumnInfo(name = "type")
    val type: TransactionType,
    @ColumnInfo(name = "color_hex")
    val colorHex: String
) {
    fun toModel() = Category(id, name, type, colorHex)

    companion object {
        fun fromModel(category: Category) = CategoryEntity(category.id, category.name, category.type, category.colorHex)
    }
}
