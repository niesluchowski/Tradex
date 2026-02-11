package com.tradex.sasd

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.tradex.sasd.databinding.ItemCoinBinding
import java.text.NumberFormat
import java.util.*

class CoinAdapter : RecyclerView.Adapter<CoinAdapter.CoinVH>() {

    private var coins = listOf<Coin>()

    class CoinVH(val binding: ItemCoinBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CoinVH {
        val binding = ItemCoinBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return CoinVH(binding)
    }

    override fun onBindViewHolder(holder: CoinVH, position: Int) {
        val coin = coins[position]
        with(holder.binding) {
            tvName.text = coin.name
            tvSymbol.text = coin.symbol.uppercase()
            tvPrice.text = "$" + NumberFormat.getNumberInstance(Locale.US).format(coin.currentPrice)

            val change = coin.priceChange24h ?: 0.0
            tvChange.text = String.format("%.2f%%", change)
            tvChange.setTextColor(
                if (change >= 0)
                    root.context.getColor(android.R.color.holo_green_dark)
                else
                    root.context.getColor(android.R.color.holo_red_dark)
            )

            Glide.with(root.context)
                .load(coin.image)
                .into(ivLogo)

            root.setOnClickListener {
                val detailFragment = CoinDetailFragment().apply {
                    arguments = Bundle().apply {
                        putSerializable("coin", coin)
                    }
                }

                val activity = root.context as MainActivity
                activity.supportFragmentManager.beginTransaction()
                    .replace(R.id.container, detailFragment)
                    .addToBackStack(null)
                    .commit()
            }

            root.setOnLongClickListener {
                val activity = root.context as MainActivity
                BuyCoinDialogFragment.newInstance(coin)
                    .show(activity.supportFragmentManager, "buy_coin")
                true
            }
        }
    }

    override fun getItemCount() = coins.size

    fun submitList(newList: List<Coin>) {
        coins = newList
        notifyDataSetChanged()
    }
}