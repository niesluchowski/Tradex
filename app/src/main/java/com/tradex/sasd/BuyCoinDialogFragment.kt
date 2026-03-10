package com.tradex.sasd

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModelProvider
import com.tradex.sasd.databinding.DialogBuyCoinBinding
import java.text.NumberFormat

class BuyCoinDialogFragment : DialogFragment() {

    private var _binding: DialogBuyCoinBinding? = null
    private val binding get() = _binding!!

    private lateinit var portfolioViewModel: PortfolioViewModel

    companion object {
        private const val ARG_COIN = "coin"

        fun newInstance(coin: Coin): BuyCoinDialogFragment {
            return BuyCoinDialogFragment().apply {
                arguments = Bundle().apply {
                    putSerializable(ARG_COIN, coin)
                }
            }
        }
    }

    override fun onCreateView(inf: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = DialogBuyCoinBinding.inflate(inf, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        portfolioViewModel = ViewModelProvider(requireActivity())[PortfolioViewModel::class.java]

        val coin = arguments?.getSerializable(ARG_COIN) as? Coin
        if (coin == null) {
            dismiss()
            return
        }

        val fmt = NumberFormat.getCurrencyInstance().apply { maximumFractionDigits = 8 }

        binding.tvCoinName.text = "${coin.name} (${coin.symbol.uppercase()})"
        binding.tvPrice.text = "Cena: ${fmt.format(coin.currentPrice)}"

        binding.btnCancel.setOnClickListener { dismiss() }

        binding.btnBuy.setOnClickListener {
            val amountText = binding.etAmount.text.toString()
            if (amountText.isBlank()) {
                binding.etAmount.error = "Wpisz ilość"
                return@setOnClickListener
            }

            val amount = amountText.toDoubleOrNull()
            if (amount == null || amount <= 0) {
                binding.etAmount.error = "Nieprawidłowa ilość"
                return@setOnClickListener
            }

            val balance = portfolioViewModel.balance.value ?: 0.0
            val totalCost = amount * coin.currentPrice
            if (totalCost > balance) {
                Toast.makeText(requireContext(), "Za mało środków!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            portfolioViewModel.addCoin(coin, amount)
            Toast.makeText(requireContext(), "Kupiono $amount ${coin.symbol.uppercase()}", Toast.LENGTH_LONG).show()
            dismiss()
        }
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}