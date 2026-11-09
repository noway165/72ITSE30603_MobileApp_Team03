package vn.edu.vlu.moneymanager.data.model

import com.google.gson.annotations.SerializedName

data class ExchangeRateResponse(
    @SerializedName("result") val result: String? = null,
    @SerializedName("base_code") val baseCode: String? = null,
    @SerializedName("conversion_rates") val conversionRates: Map<String, Double>? = null
)