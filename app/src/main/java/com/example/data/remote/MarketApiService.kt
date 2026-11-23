package com.example.data.remote

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path
import retrofit2.http.Query

@JsonClass(generateAdapter = true)
data class YahooChartResponse(
    @Json(name = "chart") val chart: YahooChartBody?
)

@JsonClass(generateAdapter = true)
data class YahooChartBody(
    @Json(name = "result") val result: List<YahooChartResult>?
)

@JsonClass(generateAdapter = true)
data class YahooChartResult(
    @Json(name = "meta") val meta: YahooChartMeta?,
    @Json(name = "timestamp") val timestamp: List<Long>?,
    @Json(name = "indicators") val indicators: YahooIndicators?
)

@JsonClass(generateAdapter = true)
data class YahooChartMeta(
    @Json(name = "regularMarketPrice") val regularMarketPrice: Double?,
    @Json(name = "chartPreviousClose") val chartPreviousClose: Double?,
    @Json(name = "regularMarketTime") val regularMarketTime: Long?
)

@JsonClass(generateAdapter = true)
data class YahooIndicators(
    @Json(name = "quote") val quote: List<YahooQuote>?
)

@JsonClass(generateAdapter = true)
data class YahooQuote(
    @Json(name = "open") val open: List<Double?>?,
    @Json(name = "high") val high: List<Double?>?,
    @Json(name = "low") val low: List<Double?>?,
    @Json(name = "close") val close: List<Double?>?,
    @Json(name = "volume") val volume: List<Double?>?
)

@JsonClass(generateAdapter = true)
data class SpotRatesResponse(
    @Json(name = "result") val result: String?,
    @Json(name = "time_last_update_unix") val timeLastUpdateUnix: Long?,
    @Json(name = "rates") val rates: Map<String, Double>?
)

interface YahooFinanceApi {
    @GET("v8/finance/chart/{symbol}")
    suspend fun getChart(
        @Path("symbol") symbol: String,
        @Query("interval") interval: String = "1m",
        @Query("range") range: String = "1d",
        @Header("User-Agent") userAgent: String = "Mozilla/5.0 (Linux; Android 14) SignalPulse/1.0"
    ): YahooChartResponse
}

interface ExchangeRateApi {
    @GET("v6/latest/{base}")
    suspend fun getLatestRates(
        @Path("base") baseCurrency: String
    ): SpotRatesResponse
}
