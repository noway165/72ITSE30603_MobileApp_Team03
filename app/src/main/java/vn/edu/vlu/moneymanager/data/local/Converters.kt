package vn.edu.vlu.moneymanager.data.local

import androidx.room.TypeConverter
import vn.edu.vlu.moneymanager.data.model.TransactionType

/** Room stores enums as their name ("INCOME" / "EXPENSE"), readable in Database Inspector. */
class Converters {

    @TypeConverter
    fun fromTransactionType(type: TransactionType): String = type.name

    @TypeConverter
    fun toTransactionType(value: String): TransactionType = TransactionType.valueOf(value)
}
