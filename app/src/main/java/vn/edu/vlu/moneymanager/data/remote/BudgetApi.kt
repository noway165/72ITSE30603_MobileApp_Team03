package vn.edu.vlu.moneymanager.data.remote

import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query
import vn.edu.vlu.moneymanager.data.local.entity.BudgetEntity
import vn.edu.vlu.moneymanager.data.model.Budget

/** MockAPI resource "budgets" (base URL MOCKAPI_FINANCE_URL). Each row belongs to one Firebase user through userId. */
interface BudgetApi {

    @GET("budgets")
    suspend fun getAll(@Query("userId") userId: String): List<BudgetDto>

    @POST("budgets")
    suspend fun create(@Body budget: BudgetDto): BudgetDto

    @PUT("budgets/{id}")
    suspend fun update(@Path("id") id: Long, @Body budget: BudgetDto): BudgetDto

    @DELETE("budgets/{id}")
    suspend fun delete(@Path("id") id: Long): BudgetDto
}

/** JSON of a budget. MockAPI sends ids as numeric strings; id is null on create, so the server assigns it. */
data class BudgetDto(
    val id: String? = null,
    val userId: String,
    val categoryId: String,
    val year: Int,
    val month: Int,
    val limitAmount: Double
) {
    fun toEntity() = BudgetEntity(checkNotNull(id).toLong(), categoryId.toLong(), year, month, limitAmount)
}

fun Budget.toDto(userId: String) = BudgetDto(
    userId = userId,
    categoryId = categoryId.toString(),
    year = month.year,
    month = month.month,
    limitAmount = limit
)
