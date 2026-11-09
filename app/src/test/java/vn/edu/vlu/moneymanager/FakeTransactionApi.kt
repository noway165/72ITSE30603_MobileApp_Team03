package vn.edu.vlu.moneymanager

import okhttp3.ResponseBody.Companion.toResponseBody
import retrofit2.HttpException
import retrofit2.Response
import vn.edu.vlu.moneymanager.data.remote.TransactionApi
import vn.edu.vlu.moneymanager.data.remote.TransactionDto
import vn.edu.vlu.moneymanager.data.remote.toDto
import vn.edu.vlu.moneymanager.data.source.SampleData

/**
 * In-memory MockAPI "transactions", starting with the same rows as [FakeTransactionDao].
 * Like the real one, a filter that matches nothing is a 404. Set [failure] to simulate errors (IOException = offline).
 */
class FakeTransactionApi(
    initial: List<TransactionDto> = SampleData.transactions.map { it.toDto(TEST_USER_ID).copy(id = it.id.toString()) }
) : TransactionApi {

    val rows = initial.toMutableList()
    var failure: Exception? = null
    private var nextId = (initial.maxOfOrNull { checkNotNull(it.id).toLong() } ?: 0L) + 1

    private fun http404() = HttpException(Response.error<Unit>(404, "".toResponseBody()))

    private fun failIfSet() {
        failure?.let { throw it }
    }

    override suspend fun getAll(userId: String): List<TransactionDto> {
        failIfSet()
        return rows.filter { it.userId == userId }.ifEmpty { throw http404() }
    }

    override suspend fun create(transaction: TransactionDto): TransactionDto {
        failIfSet()
        return transaction.copy(id = (nextId++).toString()).also { rows += it }
    }

    override suspend fun update(id: Long, transaction: TransactionDto): TransactionDto {
        failIfSet()
        val index = rows.indexOfFirst { it.id == id.toString() }
        if (index < 0) throw http404()
        return transaction.copy(id = id.toString()).also { rows[index] = it }
    }

    override suspend fun delete(id: Long): TransactionDto {
        failIfSet()
        val index = rows.indexOfFirst { it.id == id.toString() }
        if (index < 0) throw http404()
        return rows.removeAt(index)
    }
}
