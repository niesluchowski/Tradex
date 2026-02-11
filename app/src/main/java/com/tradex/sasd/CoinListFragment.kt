package com.tradex.sasd

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.tradex.sasd.databinding.FragmentCoinListBinding
import kotlinx.coroutines.*

class CoinListFragment : Fragment() {

    private var _binding: FragmentCoinListBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: CoinViewModel
    private lateinit var portfolioViewModel: PortfolioViewModel
    private val adapter = CoinAdapter()

    private var refreshJob: Job? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCoinListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(this)[CoinViewModel::class.java]
        portfolioViewModel = ViewModelProvider(requireActivity())[PortfolioViewModel::class.java]

        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = adapter

        viewModel.coins.observe(viewLifecycleOwner) { coins ->
            adapter.submitList(coins)

            portfolioViewModel.updatePrices(coins)

            binding.progressBar.visibility = View.GONE
            binding.swipeRefresh.isRefreshing = false
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { loading ->
            if (adapter.itemCount == 0) {
                binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
            }
        }

        binding.swipeRefresh.setOnRefreshListener {
            viewModel.loadCoins(forceRefresh = true)
        }

        binding.spinnerSort.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, v: View?, position: Int, id: Long) {
                when (position) {
                    0 -> viewModel.sortByMarketCap()
                    1 -> viewModel.sortByPrice()
                    2 -> viewModel.sortByChange24h()
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        startAutoRefresh()
    }

    private fun startAutoRefresh() {
        refreshJob?.cancel()
        refreshJob = CoroutineScope(Dispatchers.Main).launch {
            while (isAdded) {
                delay(60_000)
                if (isAdded) {
                    viewModel.loadCoins(forceRefresh = false)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (adapter.itemCount == 0) {
            viewModel.loadCoins(forceRefresh = false)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        refreshJob?.cancel()
        _binding = null
    }
}