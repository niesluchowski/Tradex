package com.tradex.sasd

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.tradex.sasd.databinding.ItemTransactionBinding
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TransactionAdapter : RecyclerView.Adapter<TransactionAdapter.TransactionVH>() {

    private var items = listOf<TransactionItem>()

    class TransactionVH(val binding: ItemTransactionBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransactionVH {
        val binding = ItemTransactionBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return TransactionVH(binding)
    }

    override fun onBindViewHolder(holder: TransactionVH, position: Int) {
        val item = items[position]
        val currencyFmt = NumberFormat.getCurrencyInstance(Locale.US).apply { maximumFractionDigits = 8 }
        val amountFmt = NumberFormat.getNumberInstance(Locale.US).apply { maximumFractionDigits = 8 }
        val dateFmt = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())

        with(holder.binding) {
            tvType.text = item.type
            tvType.setTextColor(
                when (item.type) {
                    "BUY" -> Color.parseColor("#00E676")
                    "SELL" -> Color.parseColor("#FF5252")
                    "DEPOSIT" -> Color.parseColor("#4FC3F7")
                    "WITHDRAW" -> Color.parseColor("#FFB74D")
                    else -> Color.WHITE
                }
            )

            tvDate.text = dateFmt.format(Date(item.timestamp))
            tvCoin.text = "${item.coinName} (${item.coinSymbol})"
            tvAmount.text = "Ilość: ${amountFmt.format(item.amount)}"
            tvPrice.text = "Cena: ${currencyFmt.format(item.pricePerCoin)}"
            tvTotal.text = "Wartość: ${currencyFmt.format(item.totalValue)}"
        }
    }

    override fun getItemCount(): Int = items.size

    fun submitList(newList: List<TransactionItem>) {
        items = newList
        notifyDataSetChanged()
    }
}