package com.tradex.sasd
import androidx.lifecycle.*
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

class CoinViewModel : ViewModel() {

    private val _coins = MutableLiveData<List<Coin>>(emptyList())
    val coins: LiveData<List<Coin>> = _coins

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .addInterceptor { chain ->
            val request = chain.request().newBuilder()
                .header("User-Agent", "TradeX")
                .header("Accept", "application/json")
                .build()
            chain.proceed(request)
        }
        .build()

    private val api = Retrofit.Builder()
        .baseUrl("https://api.coingecko.com/api/v3/")
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(ApiService::class.java)

    init {
        loadCoins()
    }

    fun loadCoins(forceRefresh: Boolean = false) {
        if (isLoading.value == true && !forceRefresh) return

        viewModelScope.launch {
            _isLoading.value = true

            try {
                if (!forceRefresh && _coins.value?.isNotEmpty() == true) {
                    _isLoading.value = false
                    return@launch
                }

                val response = api.getTopCoins(
                    vsCurrency = "usd",
                    order = "market_cap_desc",
                    perPage = 100,
                    page = 1,
                    priceChange = "24h",
                    sparkline = false
                )

                _coins.value = response

            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun sortByMarketCap() {
        _coins.value = _coins.value?.sortedByDescending { it.marketCap } ?: emptyList()
    }

    fun sortByPrice() {
        _coins.value = _coins.value?.sortedBy { it.currentPrice } ?: emptyList()
    }

    fun sortByChange24h() {
        _coins.value = _coins.value?.sortedByDescending { it.priceChange24h ?: -99999.0 } ?: emptyList()
    }
}