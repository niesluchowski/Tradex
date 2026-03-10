package com.tradex.sasd

import java.io.Serializable

data class TransactionItem(
    val id: Long,
    val type: String,
    val coinId: String,
    val coinName: String,
    val coinSymbol: String,
    val amount: Double,
    val pricePerCoin: Double,
    val totalValue: Double,
    val timestamp: Long
) : Serializable