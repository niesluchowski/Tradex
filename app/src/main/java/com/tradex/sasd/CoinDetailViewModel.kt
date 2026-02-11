
package com.tradex.sasd
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

class CoinDetailViewModel : ViewModel() {

    private val _chartData = MutableLiveData<List<Pair<Long, Double>>>()
    val chartData: LiveData<List<Pair<Long, Double>>> = _chartData

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor { chain ->
            val request = chain.request().newBuilder()
                .header("User-Agent", "TradeX")
                .build()
            chain.proceed(request)
        }
        .build()

    class CoinDetailViewModel : ViewModel() {
        private val _priceChange = MutableLiveData<Double>()
        val priceChange: LiveData<Double> = _priceChange

        private val api = Retrofit.Builder()
            .baseUrl("https://api.coingecko.com/api/v3/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(CoinChartApi::class.java)

        fun loadPriceChange(coinId: String, days: Int) {
            viewModelScope.launch {
                try {
                    val response = api.getMarketChart(coinId.lowercase(), days)
                    val prices = response.prices
                    if (prices.size > 1) {
                        val first = prices.first()[1]
                        val last = prices.last()[1]
                        val change = (last - first) / first * 100
                        _priceChange.value = change
                    }
                } catch (e: Exception) {
                    _priceChange.value = 0.0
                }
            }
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
)}