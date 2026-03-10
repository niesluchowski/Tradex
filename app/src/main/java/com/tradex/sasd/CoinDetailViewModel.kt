package com.tradex.sasd

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

class CoinDetailViewModel : ViewModel() {

    enum class Range(val apiDays: Int, val label: String) {
        HOUR_1(1, "1H"),
        HOURS_6(1, "6H"),
        HOURS_24(1, "24H"),
        DAYS_7(7, "7D"),
        DAYS_14(14, "14D"),
        DAYS_30(30, "30D")
    }

    enum class ChartType {
        LINE,
        CANDLE
    }

    data class ChartPoint(
        val timestamp: Long,
        val price: Double
    )

    data class CandlePoint(
        val x: Float,
        val shadowHigh: Float,
        val shadowLow: Float,
        val open: Float,
        val close: Float,
        val timestamp: Long
    )

    private data class CachedRangeData(
        val linePoints: List<ChartPoint>,
        val candlePoints: List<CandlePoint>,
        val changePercent: Double
    )

    private val _chartData = MutableLiveData<List<ChartPoint>>(emptyList())
    val chartData: LiveData<List<ChartPoint>> = _chartData

    private val _candleData = MutableLiveData<List<CandlePoint>>(emptyList())
    val candleData: LiveData<List<CandlePoint>> = _candleData

    private val _priceChange = MutableLiveData(0.0)
    val priceChange: LiveData<Double> = _priceChange

    private val _selectedRange = MutableLiveData(Range.HOURS_24)
    val selectedRange: LiveData<Range> = _selectedRange

    private val _selectedChartType = MutableLiveData(ChartType.LINE)
    val selectedChartType: LiveData<ChartType> = _selectedChartType

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor { chain ->
            val request = chain.request().newBuilder()
                .header("User-Agent", "TradeX")
                .build()
            chain.proceed(request)
        }
        .build()

    private val api = Retrofit.Builder()
        .baseUrl("https://api.coingecko.com/api/v3/")
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(CoinChartApi::class.java)

    private val rangeCache = mutableMapOf<String, MutableMap<Range, CachedRangeData>>()
    private var currentJob: Job? = null
    private var currentCoinId: String? = null
    private var requestToken = 0L

    fun setChartType(type: ChartType) {
        _selectedChartType.value = type
    }

    fun loadChart(coinId: String, range: Range, forceRefresh: Boolean = false) {
        currentCoinId = coinId
        _selectedRange.value = range

        val coinCache = rangeCache.getOrPut(coinId) { mutableMapOf() }
        val cached = coinCache[range]

        if (!forceRefresh && cached != null) {
            _chartData.value = cached.linePoints
            _candleData.value = cached.candlePoints
            _priceChange.value = cached.changePercent
            return
        }

        currentJob?.cancel()
        _isLoading.value = true
        val localToken = ++requestToken

        currentJob = viewModelScope.launch {
            try {
                val response = api.getMarketChart(coinId.lowercase(), range.apiDays)
                val rawPrices = response.prices

                val filtered = filterPointsForRange(rawPrices, range)
                val points = filtered.mapNotNull { point ->
                    val timestamp = point.getOrNull(0)?.toLong() ?: return@mapNotNull null
                    val price = point.getOrNull(1) ?: return@mapNotNull null
                    ChartPoint(timestamp, price)
                }

                val candlePoints = buildCandles(points, range)
                val change = calculateChange(points)

                if (localToken != requestToken || currentCoinId != coinId) {
                    return@launch
                }

                val cachedData = CachedRangeData(
                    linePoints = points,
                    candlePoints = candlePoints,
                    changePercent = change
                )
                coinCache[range] = cachedData

                _chartData.value = points
                _candleData.value = candlePoints
                _priceChange.value = change
            } catch (e: Exception) {
                if (localToken != requestToken) {
                    return@launch
                }

                val fallback = coinCache[range]
                if (fallback != null) {
                    _chartData.value = fallback.linePoints
                    _candleData.value = fallback.candlePoints
                    _priceChange.value = fallback.changePercent
                } else {
                    _chartData.value = emptyList()
                    _candleData.value = emptyList()
                    _priceChange.value = 0.0
                }
            } finally {
                if (localToken == requestToken) {
                    _isLoading.value = false
                }
            }
        }
    }

    private fun calculateChange(points: List<ChartPoint>): Double {
        if (points.size < 2) return 0.0
        val first = points.first().price
        val last = points.last().price
        if (first == 0.0) return 0.0
        return ((last - first) / first) * 100.0
    }

    private fun filterPointsForRange(
        rawPrices: List<List<Double>>,
        range: Range
    ): List<List<Double>> {
        if (rawPrices.isEmpty()) return emptyList()

        val now = rawPrices.last().firstOrNull()?.toLong() ?: return rawPrices

        val minTimestamp = when (range) {
            Range.HOUR_1 -> now - 60L * 60L * 1000L
            Range.HOURS_6 -> now - 6L * 60L * 60L * 1000L
            Range.HOURS_24 -> now - 24L * 60L * 60L * 1000L
            Range.DAYS_7 -> now - 7L * 24L * 60L * 60L * 1000L
            Range.DAYS_14 -> now - 14L * 24L * 60L * 60L * 1000L
            Range.DAYS_30 -> now - 30L * 24L * 60L * 60L * 1000L
        }

        return rawPrices.filter { point ->
            val timestamp = point.firstOrNull()?.toLong() ?: return@filter false
            timestamp >= minTimestamp
        }
    }

    private fun buildCandles(
        points: List<ChartPoint>,
        range: Range
    ): List<CandlePoint> {
        if (points.size < 2) return emptyList()

        val desiredCandles = when (range) {
            Range.HOUR_1 -> 12
            Range.HOURS_6 -> 18
            Range.HOURS_24 -> 24
            Range.DAYS_7 -> 28
            Range.DAYS_14 -> 28
            Range.DAYS_30 -> 30
        }

        val bucketSize = (points.size / desiredCandles).coerceAtLeast(2)

        return points
            .chunked(bucketSize)
            .mapIndexedNotNull { index, chunk ->
                if (chunk.size < 2) return@mapIndexedNotNull null

                val open = chunk.first().price.toFloat()
                val close = chunk.last().price.toFloat()
                val high = chunk.maxOf { it.price }.toFloat()
                val low = chunk.minOf { it.price }.toFloat()
                val timestamp = chunk.last().timestamp

                CandlePoint(
                    x = index.toFloat(),
                    shadowHigh = high,
                    shadowLow = low,
                    open = open,
                    close = close,
                    timestamp = timestamp
                )
            }
    }

    interface CoinChartApi {
        @GET("coins/{id}/market_chart")
        suspend fun getMarketChart(
            @Path("id") id: String,
            @Query("days") days: Int,
            @Query("vs_currency") vsCurrency: String = "usd"
        ): ChartResponse
    }

    data class ChartResponse(
        val prices: List<List<Double>>
    )
}