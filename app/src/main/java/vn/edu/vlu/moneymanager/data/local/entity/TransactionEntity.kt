package vn.edu.vlu.moneymanager.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import vn.edu.vlu.moneymanager.data.model.Transaction
import vn.edu.vlu.moneymanager.data.model.TransactionType

/**
 * Table "transactions". Deleting a category keeps its transactions: the foreign key sets category_id to NULL
 * ("Uncategorized"). category_id and date_millis are indexed because every screen filters or sorts on them.
 */
@Entity(
    tableName = "transactions",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["category_id"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index("category_id"), Index("date_millis")]
)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,
    @ColumnInfo(name = "type")
    val type: TransactionType,
    @ColumnInfo(name = "amount")
    val amount: Double,
    @ColumnInfo(name = "currency")
    val currency: String,
    @ColumnInfo(name = "category_id")
    val categoryId: Long?,
    @ColumnInfo(name = "date_millis")
    val dateMillis: Long,
    @ColumnInfo(name = "note")
    val note: String,
    @ColumnInfo(name = "pending_sync")
    val pendingSync: Boolean
) {
    fun toModel() = Transaction(id, type, amount, currency, categoryId, dateMillis, note, pendingSync)

    companion object {
        fun fromModel(transaction: Transaction) = with(transaction) {
            TransactionEntity(id, type, amount, currency, categoryId, dateMillis, note, pendingSync)
        }
    }
}
