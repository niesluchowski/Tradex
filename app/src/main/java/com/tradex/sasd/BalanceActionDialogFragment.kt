package com.tradex.sasd

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModelProvider
import com.tradex.sasd.databinding.DialogBalanceActionBinding
import java.text.NumberFormat
import java.util.Locale

class BalanceActionDialogFragment : DialogFragment() {

    private var _binding: DialogBalanceActionBinding? = null
    private val binding get() = _binding!!

    private lateinit var portfolioViewModel: PortfolioViewModel

    companion object {
        private const val ARG_TYPE = "action_type"
        const val TYPE_DEPOSIT = "DEPOSIT"
        const val TYPE_WITHDRAW = "WITHDRAW"

        fun newInstance(type: String): BalanceActionDialogFragment {
            return BalanceActionDialogFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_TYPE, type)
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogBalanceActionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        portfolioViewModel = ViewModelProvider(requireActivity())[PortfolioViewModel::class.java]

        val actionType = arguments?.getString(ARG_TYPE) ?: TYPE_DEPOSIT
        val balanceFmt = NumberFormat.getCurrencyInstance(Locale.US)

        binding.tvTitle.text = if (actionType == TYPE_DEPOSIT) {
            "Wpłata środków"
        } else {
            "Wypłata środków"
        }

        val currentBalance = portfolioViewModel.balance.value ?: 0.0
        binding.tvCurrentBalance.text = "Aktualne saldo: ${balanceFmt.format(currentBalance)}"

        binding.btnCancel.setOnClickListener { dismiss() }

        binding.btnConfirm.setOnClickListener {
            val amountText = binding.etAmount.text?.toString().orEmpty().trim()

            if (amountText.isBlank()) {
                binding.etAmount.error = "Wpisz kwotę"
                return@setOnClickListener
            }

            val amount = amountText.toDoubleOrNull()
            if (amount == null || amount <= 0.0) {
                binding.etAmount.error = "Nieprawidłowa kwota"
                return@setOnClickListener
            }

            val success = if (actionType == TYPE_DEPOSIT) {
                portfolioViewModel.depositFunds(amount)
                true
            } else {
                portfolioViewModel.withdrawFunds(amount)
            }

            if (!success) {
                Toast.makeText(
                    requireContext(),
                    "Za mało środków na wypłatę",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            Toast.makeText(
                requireContext(),
                if (actionType == TYPE_DEPOSIT) "Wpłata wykonana" else "Wypłata wykonana",
                Toast.LENGTH_SHORT
            ).show()

            dismiss()
        }
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}