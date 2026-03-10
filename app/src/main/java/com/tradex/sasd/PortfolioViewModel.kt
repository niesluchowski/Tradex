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

    private val _transactions = MutableLiveData<MutableList<TransactionItem>>(mutableListOf())
    val transactions: LiveData<MutableList<TransactionItem>> = _transactions

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

        addTransaction(
            TransactionItem(
                id = System.currentTimeMillis(),
                type = "BUY",
                coinId = coin.id,
                coinName = coin.name,
                coinSymbol = coin.symbol.uppercase(),
                amount = amount,
                pricePerCoin = coin.currentPrice,
                totalValue = amount * coin.currentPrice,
                timestamp = System.currentTimeMillis()
            )
        )

        _portfolio.value = list
        saveToPrefs()
    }

    fun sellCoin(coinId: String, amount: Double): Boolean {
        if (amount <= 0.0) return false

        val list = _portfolio.value ?: return false
        val item = list.find { it.coin.id == coinId } ?: return false

        if (item.amount + 1e-12 < amount) return false

        val proceeds = amount * item.coin.currentPrice
        _balance.value = (_balance.value ?: 0.0) + proceeds

        addTransaction(
            TransactionItem(
                id = System.currentTimeMillis(),
                type = "SELL",
                coinId = item.coin.id,
                coinName = item.coin.name,
                coinSymbol = item.coin.symbol.uppercase(),
                amount = amount,
                pricePerCoin = item.coin.currentPrice,
                totalValue = proceeds,
                timestamp = System.currentTimeMillis()
            )
        )

        item.amount -= amount
        if (item.amount <= 1e-12) {
            list.remove(item)
        }

        _portfolio.value = list
        saveToPrefs()
        return true
    }

    fun depositFunds(amount: Double) {
        if (amount <= 0.0) return

        _balance.value = (_balance.value ?: 0.0) + amount

        addTransaction(
            TransactionItem(
                id = System.currentTimeMillis(),
                type = "DEPOSIT",
                coinId = "cash",
                coinName = "Saldo konta",
                coinSymbol = "USD",
                amount = amount,
                pricePerCoin = 1.0,
                totalValue = amount,
                timestamp = System.currentTimeMillis()
            )
        )

        saveToPrefs()
    }

    fun withdrawFunds(amount: Double): Boolean {
        if (amount <= 0.0) return false

        val currentBalance = _balance.value ?: 0.0
        if (amount > currentBalance) return false

        _balance.value = currentBalance - amount

        addTransaction(
            TransactionItem(
                id = System.currentTimeMillis(),
                type = "WITHDRAW",
                coinId = "cash",
                coinName = "Saldo konta",
                coinSymbol = "USD",
                amount = amount,
                pricePerCoin = 1.0,
                totalValue = amount,
                timestamp = System.currentTimeMillis()
            )
        )

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

    private fun addTransaction(transaction: TransactionItem) {
        val list = _transactions.value ?: mutableListOf()
        list.add(0, transaction)
        _transactions.value = list
    }

    private fun saveToPrefs() {
        val portfolioJson = gson.toJson(_portfolio.value)
        val transactionsJson = gson.toJson(_transactions.value)
        val balanceVal = _balance.value ?: 10000.0

        prefs.edit()
            .putString("portfolio", portfolioJson)
            .putString("transactions", transactionsJson)
            .putFloat("balance", balanceVal.toFloat())
            .apply()
    }

    private fun loadFromPrefs() {
        val portfolioJson = prefs.getString("portfolio", null)
        if (portfolioJson != null) {
            val type = object : TypeToken<MutableList<PortfolioItem>>() {}.type
            val list: MutableList<PortfolioItem> = gson.fromJson(portfolioJson, type) ?: mutableListOf()
            _portfolio.value = list
        }

        val transactionsJson = prefs.getString("transactions", null)
        if (transactionsJson != null) {
            val type = object : TypeToken<MutableList<TransactionItem>>() {}.type
            val list: MutableList<TransactionItem> = gson.fromJson(transactionsJson, type) ?: mutableListOf()
            _transactions.value = list
        }

        val savedBalance = prefs.getFloat("balance", 10000f).toDouble()
        _balance.value = savedBalance
    }
}