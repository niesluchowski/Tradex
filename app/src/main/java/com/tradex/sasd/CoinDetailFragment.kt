package com.tradex.sasd

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.tradex.sasd.databinding.FragmentCoinDetailBinding
import java.text.NumberFormat
import java.util.*

class CoinDetailFragment : Fragment() {

    private var _binding: FragmentCoinDetailBinding? = null
    private val binding get() = _binding!!

    private val coin: Coin by lazy {
        arguments?.getSerializable("coin") as? Coin
            ?: Coin("error", "ERR", "Błąd", "", 0.0, 0L, null)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentCoinDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val fmt = NumberFormat.getCurrencyInstance(Locale.US).apply {
            maximumFractionDigits = 8
        }


        binding.tvName.text = coin.name
        binding.tvSymbol.text = coin.symbol.uppercase()
        binding.tvPrice.text = fmt.format(coin.currentPrice)
        Glide.with(this).load(coin.image).into(binding.ivLogo)

        coin.priceChange24h?.let { change ->
            binding.tvChange.text = String.format("%+.2f%%", change)
            binding.tvChange.setTextColor(if (change >= 0) 0xFF00E676.toInt() else 0xFFFF5252.toInt())
        } ?: run { binding.tvChange.text = "–" }


        binding.rowMarketCap.tvLabel.text = "Market Cap"
        binding.rowMarketCap.tvValue.text = fmt.format(coin.marketCap)

        binding.rowVolume.tvLabel.text = "Volume 24h"
        binding.rowVolume.tvValue.text = fmt.format(coin.marketCap * 0.05)

        binding.rowAth.tvLabel.text = "All Time High"
        binding.rowAth.tvValue.text = fmt.format(coin.currentPrice * 1.6)

        binding.rowRank.tvLabel.text = "Rank"
        binding.rowRank.tvValue.text = "#${coin.marketCapRank ?: "-"}"

        binding.btnBuy.setOnClickListener {
            BuyCoinDialogFragment
                .newInstance(coin)
                .show(parentFragmentManager, "buy_coin")
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}