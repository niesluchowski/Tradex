package com.tradex.sasd
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class CoinViewModel : ViewModel() {

    private val api = Retrofit.Builder()
        .baseUrl("https://api.coingecko.com/api/v3/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(ApiService::class.java)

    private val _coins = MutableLiveData<List<Coin>>(emptyList())
    val coins: LiveData<List<Coin>> = _coins

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private var cachedCoins: List<Coin> = emptyList()
    private var lastSuccessfulFetchTime = 0L
    private var lastManualRefreshTime = 0L
    private var isRequestInProgress = false

    private var currentSort = SortType.MARKET_CAP

    fun loadCoins(forceRefresh: Boolean = false) {
        val now = System.currentTimeMillis()

        if (isRequestInProgress) {
            return
        }

        if (!forceRefresh && cachedCoins.isNotEmpty() && now - lastSuccessfulFetchTime < 60_000L) {
            _coins.value = applyCurrentSort(cachedCoins)
            return
        }

        if (forceRefresh && now - lastManualRefreshTime < 5_000L) {
            _coins.value = applyCurrentSort(cachedCoins)
            return
        }

        if (forceRefresh) {
            lastManualRefreshTime = now
        }

        isRequestInProgress = true
        _isLoading.value = true

        viewModelScope.launch {
            try {
                val result = api.getTopCoins()

                if (result.isNotEmpty()) {
                    cachedCoins = result
                    lastSuccessfulFetchTime = System.currentTimeMillis()
                    _coins.value = applyCurrentSort(cachedCoins)
                } else if (cachedCoins.isNotEmpty()) {
                    _coins.value = applyCurrentSort(cachedCoins)
                }
            } catch (e: Exception) {
                if (cachedCoins.isNotEmpty()) {
                    _coins.value = applyCurrentSort(cachedCoins)
                }
            } finally {
                isRequestInProgress = false
                _isLoading.value = false
            }
        }
    }

    fun sortByMarketCap() {
        currentSort = SortType.MARKET_CAP
        _coins.value = applyCurrentSort(cachedCoins)
    }

    fun sortByPrice() {
        currentSort = SortType.PRICE
        _coins.value = applyCurrentSort(cachedCoins)
    }

    fun sortByChange24h() {
        currentSort = SortType.CHANGE_24H
        _coins.value = applyCurrentSort(cachedCoins)
    }

    private fun applyCurrentSort(list: List<Coin>): List<Coin> {
        return when (currentSort) {
            SortType.MARKET_CAP -> list.sortedByDescending { it.marketCap }
            SortType.PRICE -> list.sortedByDescending { it.currentPrice }
            SortType.CHANGE_24H -> list.sortedByDescending { it.priceChangePercentage24h ?: 0.0 }
        }
    }

    private enum class SortType {
        MARKET_CAP,
        PRICE,
        CHANGE_24H
    }
}