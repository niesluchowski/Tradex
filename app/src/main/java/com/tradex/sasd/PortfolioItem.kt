package com.tradex.sasd

import java.io.Serializable

data class PortfolioItem(
    val coin: Coin,
    var amount: Double = 0.0,
    var buyPriceUsd: Double = 0.0
) : Serializable