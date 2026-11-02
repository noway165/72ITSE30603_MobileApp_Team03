package vn.edu.vlu.moneymanager.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import vn.edu.vlu.moneymanager.data.model.Category
import vn.edu.vlu.moneymanager.data.model.TransactionType

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val type: TransactionType,
    val colorHex: String
)

fun CategoryEntity.toModel() = Category(id = id, name = name, type = type, colorHex = colorHex)

fun Category.toEntity() = CategoryEntity(id = id, name = name, type = type, colorHex = colorHex)