package vn.edu.vlu.moneymanager.data.remote

import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query
import vn.edu.vlu.moneymanager.data.local.entity.CategoryEntity
import vn.edu.vlu.moneymanager.data.model.Category
import vn.edu.vlu.moneymanager.data.model.TransactionType

/** MockAPI resource "categories". Each row belongs to one Firebase user through userId. */
interface CategoryApi {

    @GET("categories")
    suspend fun getAll(@Query("userId") userId: String): List<CategoryDto>

    @POST("categories")
    suspend fun create(@Body category: CategoryDto): CategoryDto

    @PUT("categories/{id}")
    suspend fun update(@Path("id") id: Long, @Body category: CategoryDto): CategoryDto

    @DELETE("categories/{id}")
    suspend fun delete(@Path("id") id: Long): CategoryDto
}

/** JSON of a category. MockAPI sends the id as a numeric string; null on create, so the server assigns it. */
data class CategoryDto(
    val id: String? = null,
    val userId: String,
    val name: String,
    val type: String,
    val colorHex: String
) {
    fun toEntity() = CategoryEntity(checkNotNull(id).toLong(), name, TransactionType.valueOf(type), colorHex)
}

fun Category.toDto(userId: String) = CategoryDto(userId = userId, name = name, type = type.name, colorHex = colorHex)
