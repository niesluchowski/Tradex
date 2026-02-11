package com.tradex.sasd

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class PortfolioViewModel(app: Application) : AndroidViewModel(app) {

    private val prefs = app.getSharedPreferences("portfolio_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()

    private val _portfolio = MutableLiveData<MutableList<PortfolioItem>>(mutableListOf())
    val portfolio: LiveData<MutableList<PortfolioItem>> = _portfolio

    private val _balance = MutableLiveData<Double>(10000.0)
    val balance: LiveData<Double> = _balance

    init {
        loadFromPrefs()
    }

    fun addCoin(coin: Coin, amount: Double) {
        val list = _portfolio.value ?: mutableListOf()
        val existing = list.find { it.coin.id == coin.id }

        val cost = amount * coin.currentPrice
        _balance.value = (_balance.value ?: 10000.0) - cost

        if (existing != null) {
            val oldAmount = existing.amount
            val newAmount = oldAmount + amount

            val oldAvg = existing.buyPriceUsd
            val newAvg = if (newAmount > 0.0) {
                ((oldAmount * oldAvg) + (amount * coin.currentPrice)) / newAmount
            } else {
                coin.currentPrice
            }

            existing.amount = newAmount
            existing.buyPriceUsd = newAvg
        } else {
            list.add(PortfolioItem(coin, amount, coin.currentPrice))
        }

        _portfolio.value = list
        saveToPrefs()
    }

    // DODANE: sprzedaż po aktualnym kursie
    fun sellCoin(coinId: String, amount: Double): Boolean {
        if (amount <= 0.0) return false

        val list = _portfolio.value ?: return false
        val item = list.find { it.coin.id == coinId } ?: return false

        if (item.amount + 1e-12 < amount) return false

        val proceeds = amount * item.coin.currentPrice
        _balance.value = (_balance.value ?: 0.0) + proceeds

        item.amount -= amount
        if (item.amount <= 1e-12) {
            list.remove(item)
        }

        _portfolio.value = list
        saveToPrefs()
        return true
    }

    fun updatePrices(latestCoins: List<Coin>) {
        val list = _portfolio.value ?: return
        if (list.isEmpty()) return

        val byId = latestCoins.associateBy { it.id }
        var changed = false

        list.forEach { item ->
            val latest = byId[item.coin.id] ?: return@forEach
            if (item.coin.currentPrice != latest.currentPrice) {
                val idx = list.indexOf(item)
                if (idx >= 0) {
                    list[idx] = item.copy(coin = latest)
                    changed = true
                }
            }
        }

        if (changed) {
            _portfolio.value = list
            saveToPrefs()
        }
    }

    fun getTotalPortfolioValue(): Double {
        return _portfolio.value?.sumOf { it.amount * it.coin.currentPrice } ?: 0.0
    }

    private fun saveToPrefs() {
        val json = gson.toJson(_portfolio.value)
        val balanceVal = _balance.value ?: 10000.0
        prefs.edit()
            .putString("portfolio", json)
            .putFloat("balance", balanceVal.toFloat())
            .apply()
    }

    private fun loadFromPrefs() {
        val json = prefs.getString("portfolio", null)
        if (json != null) {
            val type = object : TypeToken<MutableList<PortfolioItem>>() {}.type
            val list: MutableList<PortfolioItem> = gson.fromJson(json, type) ?: mutableListOf()
            _portfolio.value = list
        }

        val savedBalance = prefs.getFloat("balance", 10000f).toDouble()
        _balance.value = savedBalance
    }
}