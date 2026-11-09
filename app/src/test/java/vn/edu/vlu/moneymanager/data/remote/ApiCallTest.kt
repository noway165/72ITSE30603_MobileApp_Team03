package vn.edu.vlu.moneymanager.data.remote

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import vn.edu.vlu.moneymanager.R
import java.net.SocketTimeoutException
import java.net.UnknownHostException

class ApiCallTest {

    private fun http(code: Int) = HttpException(Response.error<Unit>(code, "".toResponseBody()))

    private suspend fun errorOf(failure: Exception) = apiCall<Unit> { throw failure }

    @Test
    fun success_wrapsTheResult() = runTest {
        assertEquals(UiState.Success(42), apiCall { 42 })
    }

    @Test
    fun timeout_andAirplaneMode_haveTheirOwnMessages() = runTest {
        assertEquals(UiState.Error(R.string.error_network_timeout), errorOf(SocketTimeoutException()))
        assertEquals(UiState.Error(R.string.error_network_offline), errorOf(UnknownHostException()))
    }

    @Test
    fun httpCodes_mapToMessages() = runTest {
        assertEquals(UiState.Error(R.string.error_http_bad_request), errorOf(http(400)))
        assertEquals(UiState.Error(R.string.error_http_not_found), errorOf(http(404)))
        assertEquals(UiState.Error(R.string.error_http_server), errorOf(http(500)))
        assertEquals(UiState.Error(R.string.error_http_server), errorOf(http(503)))
        assertEquals(UiState.Error(R.string.error_network_unknown), errorOf(http(429)))
    }

    @Test
    fun badJson_isAnErrorNotACrash() = runTest {
        assertEquals(UiState.Error(R.string.error_network_unknown), errorOf(IllegalStateException("bad json")))
    }

    @Test(expected = CancellationException::class)
    fun cancellation_isNotSwallowed() = runTest {
        errorOf(CancellationException())
    }
}
