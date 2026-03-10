package com.tradex.sasd

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.bumptech.glide.Glide
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.CandleData
import com.github.mikephil.charting.data.CandleDataSet
import com.github.mikephil.charting.data.CandleEntry
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.listener.OnChartValueSelectedListener
import com.tradex.sasd.databinding.FragmentCoinDetailBinding
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CoinDetailFragment : Fragment() {

    private var _binding: FragmentCoinDetailBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: CoinDetailViewModel

    private val coin: Coin by lazy {
        arguments?.getSerializable("coin") as? Coin
            ?: Coin("error", "ERR", "Błąd", "", 0.0, 0.0, null, null)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCoinDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(this)[CoinDetailViewModel::class.java]

        val fmt = NumberFormat.getCurrencyInstance(Locale.US).apply {
            maximumFractionDigits = 8
        }

        binding.tvName.text = coin.name
        binding.tvSymbol.text = coin.symbol.uppercase()
        binding.tvPrice.text = fmt.format(coin.currentPrice)
        Glide.with(this).load(coin.image).into(binding.ivLogo)

        coin.priceChangePercentage24h?.let { change ->
            binding.tvChange.text = String.format("%+.2f%%", change)
            binding.tvChange.setTextColor(
                if (change >= 0) 0xFF00E676.toInt() else 0xFFFF5252.toInt()
            )
        } ?: run {
            binding.tvChange.text = "–"
        }

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

        setupChartUi()
        observeChartData()

        binding.toggleChartType.check(binding.btnLineChart.id)

        binding.btn1h.setOnClickListener { selectRange(CoinDetailViewModel.Range.HOUR_1) }
        binding.btn6h.setOnClickListener { selectRange(CoinDetailViewModel.Range.HOURS_6) }
        binding.btn24h.setOnClickListener { selectRange(CoinDetailViewModel.Range.HOURS_24) }
        binding.btn7d.setOnClickListener { selectRange(CoinDetailViewModel.Range.DAYS_7) }
        binding.btn14d.setOnClickListener { selectRange(CoinDetailViewModel.Range.DAYS_14) }
        binding.btn30d.setOnClickListener { selectRange(CoinDetailViewModel.Range.DAYS_30) }

        binding.toggleChartType.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked) return@addOnButtonCheckedListener

            when (checkedId) {
                binding.btnLineChart.id -> {
                    viewModel.setChartType(CoinDetailViewModel.ChartType.LINE)
                    showLineChart()
                }
                binding.btnCandleChart.id -> {
                    val candlePoints = viewModel.candleData.value.orEmpty()
                    if (candlePoints.isEmpty()) {
                        binding.toggleChartType.check(binding.btnLineChart.id)
                        showLineChart()
                        binding.tvChartMarker.text =
                            "Brak wystarczających danych do wykresu świecowego dla tego zakresu."
                    } else {
                        viewModel.setChartType(CoinDetailViewModel.ChartType.CANDLE)
                        showCandleChart()
                    }
                }
            }
        }

        selectRange(CoinDetailViewModel.Range.HOURS_24)
    }

    private fun setupChartUi() {
        setupLineChart()
        setupCandleChart()
    }

    private fun setupLineChart() {
        with(binding.lineChart) {
            description.isEnabled = false
            setTouchEnabled(true)
            isDragEnabled = true
            setScaleEnabled(true)
            setPinchZoom(true)
            setNoDataText("Brak danych wykresu")
            legend.isEnabled = false

            axisLeft.textColor = Color.WHITE
            axisRight.isEnabled = false
            xAxis.textColor = Color.LTGRAY
            xAxis.position = XAxis.XAxisPosition.BOTTOM
            xAxis.setDrawGridLines(false)

            setOnChartValueSelectedListener(object : OnChartValueSelectedListener {
                override fun onValueSelected(e: Entry?, h: Highlight?) {
                    if (e == null) return
                    val points = viewModel.chartData.value.orEmpty()
                    val index = e.x.toInt()
                    val point = points.getOrNull(index) ?: return
                    showMarker(point.price, point.timestamp)
                }

                override fun onNothingSelected() {
                    binding.tvChartMarker.text =
                        "Przytrzymaj punkt na wykresie, aby zobaczyć cenę i czas"
                }
            })
        }
    }

    private fun setupCandleChart() {
        with(binding.candleChart) {
            description.isEnabled = false
            setTouchEnabled(true)
            isDragEnabled = true
            setScaleEnabled(true)
            setPinchZoom(true)
            setNoDataText("Brak danych świec")
            legend.isEnabled = false

            axisLeft.textColor = Color.WHITE
            axisRight.isEnabled = false
            xAxis.textColor = Color.LTGRAY
            xAxis.position = XAxis.XAxisPosition.BOTTOM
            xAxis.setDrawGridLines(false)

            setOnChartValueSelectedListener(object : OnChartValueSelectedListener {
                override fun onValueSelected(e: Entry?, h: Highlight?) {
                    if (e == null) return
                    val points = viewModel.candleData.value.orEmpty()
                    val index = e.x.toInt()
                    val point = points.getOrNull(index) ?: return
                    showMarker(point.close.toDouble(), point.timestamp)
                }

                override fun onNothingSelected() {
                    binding.tvChartMarker.text =
                        "Przytrzymaj punkt na wykresie, aby zobaczyć cenę i czas"
                }
            })
        }
    }

    private fun observeChartData() {
        viewModel.chartData.observe(viewLifecycleOwner) { points ->
            val entries = points.mapIndexed { index, point ->
                Entry(index.toFloat(), point.price.toFloat())
            }

            val dataSet = LineDataSet(entries, "Cena").apply {
                color = Color.parseColor("#00E676")
                setCircleColor(Color.WHITE)
                lineWidth = 2f
                circleRadius = 3f
                setDrawValues(false)
                setDrawFilled(true)
                fillColor = Color.parseColor("#1B5E20")
                highLightColor = Color.WHITE
            }

            binding.lineChart.data = LineData(dataSet)
            binding.lineChart.invalidate()
        }

        viewModel.candleData.observe(viewLifecycleOwner) { points ->
            val entries = points.map { point ->
                CandleEntry(
                    point.x,
                    point.shadowHigh,
                    point.shadowLow,
                    point.open,
                    point.close
                )
            }

            val dataSet = CandleDataSet(entries, "Świece").apply {
                shadowColor = Color.LTGRAY
                shadowWidth = 0.8f
                decreasingColor = Color.parseColor("#FF5252")
                decreasingPaintStyle = android.graphics.Paint.Style.FILL
                increasingColor = Color.parseColor("#00E676")
                increasingPaintStyle = android.graphics.Paint.Style.FILL
                neutralColor = Color.WHITE
                setDrawValues(false)
                highLightColor = Color.WHITE
            }

            binding.candleChart.data = CandleData(dataSet)
            binding.candleChart.invalidate()
        }

        viewModel.priceChange.observe(viewLifecycleOwner) { change ->
            binding.tvChange.text = String.format("%+.2f%%", change)
            binding.tvChange.setTextColor(
                if (change >= 0) 0xFF00E676.toInt() else 0xFFFF5252.toInt()
            )
        }
    }

    private fun selectRange(range: CoinDetailViewModel.Range) {
        activateRangeButton(range)
        binding.toggleChartType.check(binding.btnLineChart.id)
        showLineChart()
        viewModel.setChartType(CoinDetailViewModel.ChartType.LINE)
        viewModel.loadChart(coin.id, range)
    }

    private fun activateRangeButton(range: CoinDetailViewModel.Range) {
        val activeBg = Color.parseColor("#2E7D32")
        val inactiveBg = Color.parseColor("#2A2A2A")
        val activeText = Color.WHITE
        val inactiveText = Color.parseColor("#BDBDBD")

        val buttons = listOf(
            binding.btn1h,
            binding.btn6h,
            binding.btn24h,
            binding.btn7d,
            binding.btn14d,
            binding.btn30d
        )

        buttons.forEach {
            it.setBackgroundColor(inactiveBg)
            it.setTextColor(inactiveText)
        }

        val activeView = when (range) {
            CoinDetailViewModel.Range.HOUR_1 -> binding.btn1h
            CoinDetailViewModel.Range.HOURS_6 -> binding.btn6h
            CoinDetailViewModel.Range.HOURS_24 -> binding.btn24h
            CoinDetailViewModel.Range.DAYS_7 -> binding.btn7d
            CoinDetailViewModel.Range.DAYS_14 -> binding.btn14d
            CoinDetailViewModel.Range.DAYS_30 -> binding.btn30d
        }

        activeView.setBackgroundColor(activeBg)
        activeView.setTextColor(activeText)
    }

    private fun showLineChart() {
        binding.lineChart.visibility = View.VISIBLE
        binding.candleChart.visibility = View.GONE
    }

    private fun showCandleChart() {
        binding.lineChart.visibility = View.GONE
        binding.candleChart.visibility = View.VISIBLE
    }

    private fun showMarker(price: Double, timestamp: Long) {
        val priceFormat = NumberFormat.getCurrencyInstance(Locale.US).apply {
            maximumFractionDigits = 8
        }
        val dateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())

        binding.tvChartMarker.text =
            "Cena: ${priceFormat.format(price)}\nCzas: ${dateFormat.format(Date(timestamp))}"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}