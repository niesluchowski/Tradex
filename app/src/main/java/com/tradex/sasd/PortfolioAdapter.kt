package com.tradex.sasd

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.tradex.sasd.databinding.ItemPortfolioBinding
import java.text.NumberFormat
import java.util.*

class PortfolioAdapter : RecyclerView.Adapter<PortfolioAdapter.VH>() {

    private var items = listOf<PortfolioItem>()

    var onItemClick: ((PortfolioItem) -> Unit)? = null
    var onItemLongClick: ((PortfolioItem) -> Unit)? = null

    class VH(val binding: ItemPortfolioBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemPortfolioBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]
        val coin = item.coin

        val priceFmt = NumberFormat.getCurrencyInstance(Locale.US).apply { maximumFractionDigits = 8 }
        val amountFmt = NumberFormat.getNumberInstance(Locale.US).apply { maximumFractionDigits = 8 }

        with(holder.binding) {
            tvName.text = coin.name
            tvAmount.text = "${amountFmt.format(item.amount)} ${coin.symbol.uppercase()}"
            tvValue.text = priceFmt.format(item.amount * coin.currentPrice)

            val profitPercent =
                if (item.buyPriceUsd > 0.0) ((coin.currentPrice - item.buyPriceUsd) / item.buyPriceUsd) * 100.0
                else 0.0

            tvProfit.text = String.format("%+.2f%%", profitPercent)
            tvProfit.setTextColor(
                if (profitPercent >= 0) 0xFF00E676.toInt() else 0xFFFF5252.toInt()
            )

            Glide.with(root.context).load(coin.image).into(ivLogo)

            root.setOnClickListener {
                onItemClick?.invoke(item)
            }

            root.setOnLongClickListener {
                onItemLongClick?.invoke(item)
                onItemLongClick != null
            }
        }
    }

    override fun getItemCount() = items.size

    fun submitList(newList: List<PortfolioItem>) {
        items = newList
        notifyDataSetChanged()
    }
}