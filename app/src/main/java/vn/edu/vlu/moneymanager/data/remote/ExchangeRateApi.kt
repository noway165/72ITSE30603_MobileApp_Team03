package vn.edu.vlu.moneymanager.data.remote

import retrofit2.http.GET
import retrofit2.http.Path
import vn.edu.vlu.moneymanager.data.model.ExchangeRateResponse

interface ExchangeRateApi {
    @GET("v6/{apiKey}/latest/{base}")
    suspend fun getLatestRates(
        @Path("apiKey") apiKey: String,
        @Path("base") base: String
    ): ExchangeRateResponse
}