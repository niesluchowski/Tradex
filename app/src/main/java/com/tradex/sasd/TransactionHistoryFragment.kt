package com.tradex.sasd

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.tradex.sasd.databinding.FragmentTransactionHistoryBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TransactionHistoryFragment : Fragment() {

    private var _binding: FragmentTransactionHistoryBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: PortfolioViewModel
    private val adapter = TransactionAdapter()

    private val createCsvDocumentLauncher =
        registerForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
            if (uri != null) {
                exportTransactionsToCsv(uri)
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTransactionHistoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(requireActivity())[PortfolioViewModel::class.java]

        binding.recyclerTransactions.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerTransactions.adapter = adapter

        viewModel.transactions.observe(viewLifecycleOwner) { transactions ->
            adapter.submitList(transactions)
            binding.tvEmptyTransactions.visibility =
                if (transactions.isEmpty()) View.VISIBLE else View.GONE
        }

        binding.btnExportCsv.setOnClickListener {
            val transactions = viewModel.transactions.value.orEmpty()
            if (transactions.isEmpty()) {
                Toast.makeText(requireContext(), "Brak transakcji do eksportu", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val fileName = "tradex_transactions_${System.currentTimeMillis()}.csv"
            createCsvDocumentLauncher.launch(fileName)
        }
    }

    private fun exportTransactionsToCsv(uri: Uri) {
        val transactions = viewModel.transactions.value.orEmpty()
        val dateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())

        val csv = buildString {
            appendLine("id,type,coinId,coinName,coinSymbol,amount,pricePerCoin,totalValue,timestamp,date")
            transactions.forEach { item ->
                appendLine(
                    listOf(
                        item.id,
                        escapeCsv(item.type),
                        escapeCsv(item.coinId),
                        escapeCsv(item.coinName),
                        escapeCsv(item.coinSymbol),
                        item.amount,
                        item.pricePerCoin,
                        item.totalValue,
                        item.timestamp,
                        escapeCsv(dateFormat.format(Date(item.timestamp)))
                    ).joinToString(",")
                )
            }
        }

        try {
            requireContext().contentResolver.openOutputStream(uri)?.bufferedWriter().use { writer ->
                writer?.write(csv)
            }
            Toast.makeText(requireContext(), "Plik CSV zapisany", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Nie udało się zapisać pliku", Toast.LENGTH_SHORT).show()
        }
    }

    private fun escapeCsv(value: String): String {
        val escaped = value.replace("\"", "\"\"")
        return "\"$escaped\""
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}