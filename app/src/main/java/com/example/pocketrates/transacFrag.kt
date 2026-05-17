package com.example.pocketrates

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.widget.AppCompatButton
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import java.text.SimpleDateFormat
import java.util.*

class transacFrag : Fragment() {

    private val viewModel: TransactionViewModel by activityViewModels()
    private lateinit var container: LinearLayout
    private var currentFilter = "All Time"

    private lateinit var btnAllTime: AppCompatButton
    private lateinit var btnThisWeek: AppCompatButton
    private lateinit var btnToday: AppCompatButton

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_transac, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        container = view.findViewById(R.id.transactionContainer)
        val btnBack = view.findViewById<ImageButton>(R.id.btnBack)

        btnAllTime = view.findViewById(R.id.btnAllTime)
        btnThisWeek = view.findViewById(R.id.btnThisWeek)
        btnToday = view.findViewById(R.id.btnToday)

        btnBack.setOnClickListener {
            (activity as MainActivity).apply {
                setSelectedButton(binding.btnDashboard)
                gotoFrag(dashboardFrag())
            }
        }

        btnAllTime.setOnClickListener {
            updateFilter("All Time")
        }
        btnThisWeek.setOnClickListener {
            updateFilter("This Week")
        }
        btnToday.setOnClickListener {
            updateFilter("Today")
        }

        updateFilterButtonStyles()

        viewModel.allTransactions.observe(viewLifecycleOwner) { transactions ->
            displayTransactions(transactions)
        }
    }

    private fun updateFilter(filter: String) {
        currentFilter = filter
        updateFilterButtonStyles()
        viewModel.allTransactions.value?.let { displayTransactions(it) }
    }

    private fun updateFilterButtonStyles() {
        val selectedBg = ContextCompat.getDrawable(requireContext(), R.drawable.btn_rounded)
        val unselectedBg = ContextCompat.getDrawable(requireContext(), R.drawable.scroll_container)
        val selectedTextColor = ContextCompat.getColor(requireContext(), R.color.white)
        val unselectedTextColor = ContextCompat.getColor(requireContext(), R.color.text_color)

        btnAllTime.background = if (currentFilter == "All Time") selectedBg else unselectedBg
        btnAllTime.setTextColor(if (currentFilter == "All Time") selectedTextColor else unselectedTextColor)

        btnThisWeek.background = if (currentFilter == "This Week") selectedBg else unselectedBg
        btnThisWeek.setTextColor(if (currentFilter == "This Week") selectedTextColor else unselectedTextColor)

        btnToday.background = if (currentFilter == "Today") selectedBg else unselectedBg
        btnToday.setTextColor(if (currentFilter == "Today") selectedTextColor else unselectedTextColor)
    }

    private fun displayTransactions(transactions: List<TransactionEntity>) {
        container.removeAllViews()

        val filteredList = when (currentFilter) {
            "Today" -> {
                val cal = Calendar.getInstance()
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                val startOfDay = cal.timeInMillis
                transactions.filter { it.timestamp >= startOfDay }
            }
            "This Week" -> {
                val cal = Calendar.getInstance()
                cal.set(Calendar.DAY_OF_WEEK, cal.firstDayOfWeek)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                val startOfWeek = cal.timeInMillis
                transactions.filter { it.timestamp >= startOfWeek }
            }
            else -> transactions
        }.sortedByDescending { it.timestamp }

        var lastDateString = ""
        val headerDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

        filteredList.forEach { transaction ->
            val currentDateString = headerDateFormat.format(Date(transaction.timestamp))
            
            // Add date header if date changes
            if (currentDateString != lastDateString) {
                val headerView = layoutInflater.inflate(R.layout.item_date_header, container, false)
                headerView.findViewById<TextView>(R.id.txtHeaderDate).text = currentDateString
                container.addView(headerView)
                lastDateString = currentDateString
            }

            val itemView = layoutInflater.inflate(R.layout.item_transaction, container, false)

            itemView.findViewById<TextView>(R.id.lblExchInfo).text = "${transaction.fromCurrency} -> ${transaction.toCurrency}"
            
            val fromSymbol = getCurrencySymbol(transaction.fromCurrency)
            val toSymbol = getCurrencySymbol(transaction.toCurrency)

            val fromAmountFormatted = formatAmount(transaction.fromAmount)
            val toAmountFormatted = formatAmount(transaction.toAmount)

            itemView.findViewById<TextView>(R.id.lblExchVal).text = "$fromSymbol$fromAmountFormatted"
            itemView.findViewById<TextView>(R.id.lblCurrVal).text = "$toSymbol$toAmountFormatted"
            itemView.findViewById<TextView>(R.id.lblTrs_DateTime).text = "${transaction.date} • ${transaction.time}"

            container.addView(itemView)
        }
    }

    private fun formatAmount(amount: String): String {
        return try {
            val value = amount.replace(",", "").toDouble()
            java.text.NumberFormat.getNumberInstance(Locale.US).format(value)
        } catch (e: Exception) {
            amount
        }
    }

    private fun getCurrencySymbol(code: String): String {
        return try {
            Currency.getInstance(code).getSymbol(Locale.US)
        } catch (e: Exception) {
            when (code) {
                "PHP" -> "₱"
                "USD" -> "$"
                "EUR" -> "€"
                "GBP" -> "£"
                "JPY" -> "¥"
                else -> ""
            }
        }
    }
}
