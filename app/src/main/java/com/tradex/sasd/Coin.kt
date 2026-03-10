package com.tradex.sasd

import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class Coin(
    val id: String,
    val symbol: String,
    val name: String,
    val image: String,

    @SerializedName("current_price")
    val currentPrice: Double,

    @SerializedName("market_cap")
    val marketCap: Double,

    @SerializedName("market_cap_rank")
    val marketCapRank: Int? = null,

    @SerializedName("price_change_percentage_24h")
    val priceChangePercentage24h: Double? = null
) : Serializable