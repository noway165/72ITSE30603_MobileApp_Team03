package vn.edu.vlu.moneymanager.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import vn.edu.vlu.moneymanager.data.local.entity.ExchangeRateEntity

@Dao
interface ExchangeRateDao {
    @Query("SELECT * FROM exchange_rates")
    fun observeAll(): Flow<List<ExchangeRateEntity>>

    @Upsert
    suspend fun upsertAll(rates: List<ExchangeRateEntity>)
}