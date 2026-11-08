package vn.edu.vlu.moneymanager.data.remote

import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query
import vn.edu.vlu.moneymanager.data.local.entity.TransactionEntity
import vn.edu.vlu.moneymanager.data.model.Transaction
import vn.edu.vlu.moneymanager.data.model.TransactionType

/** MockAPI resource "transactions" (base URL MOCKAPI_FINANCE_URL). Each row belongs to one Firebase user through userId. */
interface TransactionApi {

    @GET("transactions")
    suspend fun getAll(@Query("userId") userId: String): List<TransactionDto>

    @POST("transactions")
    suspend fun create(@Body transaction: TransactionDto): TransactionDto

    @PUT("transactions/{id}")
    suspend fun update(@Path("id") id: Long, @Body transaction: TransactionDto): TransactionDto

    @DELETE("transactions/{id}")
    suspend fun delete(@Path("id") id: Long): TransactionDto
}

/**
 * JSON of a transaction. MockAPI sends the id as a numeric string; null on create, so the server assigns it.
 * categoryId is a string on the server (MockAPI ids are strings); null = Uncategorized.
 */
data class TransactionDto(
    val id: String? = null,
    val userId: String,
    val type: String,
    val amount: Double,
    val currency: String,
    val categoryId: String?,
    val dateMillis: Long,
    val note: String
) {
    /** A row that came from the server is synced by definition. */
    fun toEntity() = TransactionEntity(
        id = checkNotNull(id).toLong(),
        type = TransactionType.valueOf(type),
        amount = amount,
        currency = currency,
        categoryId = categoryId?.toLongOrNull(),
        dateMillis = dateMillis,
        note = note,
        pendingSync = false
    )
}

fun Transaction.toDto(userId: String) = TransactionDto(
    userId = userId,
    type = type.name,
    amount = amount,
    currency = currency,
    categoryId = categoryId?.toString(),
    dateMillis = dateMillis,
    note = note
)
