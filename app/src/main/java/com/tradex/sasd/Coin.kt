package com.tradex.sasd

import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class Coin(
    val id: String,
    val symbol: String,
    val name: String,
    val image: String,

    @SerializedName("current_price")
    val currentPrice: Double = 0.0,

    @SerializedName("market_cap")
    val marketCap: Long = 0L,

    @SerializedName("market_cap_rank")
    val marketCapRank: Int? = null,

    @SerializedName("price_change_percentage_24h")
    val priceChange24h: Double? = null
) : Serializable