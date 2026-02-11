
package com.tradex.sasd

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.tradex.sasd.databinding.FragmentPortfolioBinding
import java.text.NumberFormat
import java.util.*

class PortfolioFragment : Fragment() {

    private var _binding: FragmentPortfolioBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: PortfolioViewModel
    private val adapter = PortfolioAdapter()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPortfolioBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(requireActivity())[PortfolioViewModel::class.java]

        binding.recyclerPortfolio.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerPortfolio.adapter = adapter

        adapter.onItemLongClick = { item ->
            SellCoinDialogFragment
                .newInstance(item)
                .show(childFragmentManager, "sell_coin")
        }

        val fmt = NumberFormat.getCurrencyInstance(Locale.US).apply { maximumFractionDigits = 2 }

        viewModel.balance.observe(viewLifecycleOwner) { balance ->
            binding.tvBalance.text = fmt.format(balance)
        }

        viewModel.portfolio.observe(viewLifecycleOwner) { items ->
            adapter.submitList(items)

            val totalValue = items.sumOf { it.amount * it.coin.currentPrice }
            binding.tvPortfolioValue.text = "Wartość portfela: ${fmt.format(totalValue)}"

            binding.tvEmpty.visibility = if (items.isEmpty()) View.VISIBLE else View.GONE
        }

        binding.fabAdd.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.container, CoinListFragment())
                .addToBackStack(null)
                .commit()
        }

        // USUNIĘTE: binding.fabTransfer (nie istnieje w layoucie)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}