package vn.edu.vlu.moneymanager.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "exchange_rates")
data class ExchangeRateEntity(
    @PrimaryKey
    @ColumnInfo(name = "currency_code")
    val currency_code: String,

    @ColumnInfo(name = "rate_per_usd")
    val rate_per_usd: Double,

    @ColumnInfo(name = "updated_at")
    val updated_at: Long = System.currentTimeMillis()
)