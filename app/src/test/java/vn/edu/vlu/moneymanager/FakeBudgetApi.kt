package vn.edu.vlu.moneymanager

import okhttp3.ResponseBody.Companion.toResponseBody
import retrofit2.HttpException
import retrofit2.Response
import vn.edu.vlu.moneymanager.data.remote.BudgetApi
import vn.edu.vlu.moneymanager.data.remote.BudgetDto
import vn.edu.vlu.moneymanager.data.remote.toDto
import vn.edu.vlu.moneymanager.data.source.SampleData

/**
 * In-memory MockAPI "budgets", starting with the same rows as [FakeBudgetDao].
 * Like the real one, a filter that matches nothing is a 404. Set [failure] to simulate errors (IOException = offline).
 */
class FakeBudgetApi(
    initial: List<BudgetDto> = SampleData.budgets.map { it.toDto(TEST_USER_ID).copy(id = it.id.toString()) }
) : BudgetApi {

    val rows = initial.toMutableList()
    var failure: Exception? = null
    private var nextId = (initial.maxOfOrNull { checkNotNull(it.id).toLong() } ?: 0L) + 1

    private fun http404() = HttpException(Response.error<Unit>(404, "".toResponseBody()))

    private fun failIfSet() {
        failure?.let { throw it }
    }

    override suspend fun getAll(userId: String): List<BudgetDto> {
        failIfSet()
        return rows.filter { it.userId == userId }.ifEmpty { throw http404() }
    }

    override suspend fun create(budget: BudgetDto): BudgetDto {
        failIfSet()
        return budget.copy(id = (nextId++).toString()).also { rows += it }
    }

    override suspend fun update(id: Long, budget: BudgetDto): BudgetDto {
        failIfSet()
        val index = rows.indexOfFirst { it.id == id.toString() }
        if (index < 0) throw http404()
        return budget.copy(id = id.toString()).also { rows[index] = it }
    }

    override suspend fun delete(id: Long): BudgetDto {
        failIfSet()
        val index = rows.indexOfFirst { it.id == id.toString() }
        if (index < 0) throw http404()
        return rows.removeAt(index)
    }
}
