package com.tradex.sasd

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModelProvider
import com.tradex.sasd.databinding.DialogSellCoinBinding
import java.text.NumberFormat
import java.util.Locale

class SellCoinDialogFragment : DialogFragment() {

    private var _binding: DialogSellCoinBinding? = null
    private val binding get() = _binding!!

    private lateinit var portfolioViewModel: PortfolioViewModel

    companion object {
        private const val ARG_ITEM = "portfolio_item"

        fun newInstance(item: PortfolioItem): SellCoinDialogFragment {
            return SellCoinDialogFragment().apply {
                arguments = Bundle().apply {
                    putSerializable(ARG_ITEM, item)
                }
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = DialogSellCoinBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        portfolioViewModel = ViewModelProvider(requireActivity())[PortfolioViewModel::class.java]

        val item = arguments?.getSerializable(ARG_ITEM) as? PortfolioItem
        if (item == null) {
            dismiss()
            return
        }

        val coin = item.coin
        val priceFmt = NumberFormat.getCurrencyInstance(Locale.US).apply { maximumFractionDigits = 8 }
        val amountFmt = NumberFormat.getNumberInstance(Locale.US).apply { maximumFractionDigits = 8 }

        binding.tvCoinName.text = "${coin.name} (${coin.symbol.uppercase()})"
        binding.tvAvailable.text = "Dostępne: ${amountFmt.format(item.amount)} ${coin.symbol.uppercase()}"
        binding.tvPrice.text = "Cena: ${priceFmt.format(coin.currentPrice)}"

        binding.btnCancel.setOnClickListener { dismiss() }

        binding.btnSell.setOnClickListener {
            val amountText = binding.etAmount.text?.toString().orEmpty().trim()
            if (amountText.isBlank()) {
                binding.etAmount.error = "Wpisz ilość"
                return@setOnClickListener
            }

            val amount = amountText.toDoubleOrNull()
            if (amount == null || amount <= 0.0) {
                binding.etAmount.error = "Nieprawidłowa ilość"
                return@setOnClickListener
            }

            if (amount > item.amount + 1e-12) {
                binding.etAmount.error = "Nie masz tyle ${coin.symbol.uppercase()}"
                return@setOnClickListener
            }

            val ok = portfolioViewModel.sellCoin(coin.id, amount)
            if (!ok) {
                Toast.makeText(requireContext(), "Nie udało się sprzedać", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            Toast.makeText(
                requireContext(),
                "Sprzedano ${amountFmt.format(amount)} ${coin.symbol.uppercase()}",
                Toast.LENGTH_LONG
            ).show()
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