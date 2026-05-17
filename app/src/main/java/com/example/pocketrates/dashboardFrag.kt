package com.example.pocketrates

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import okhttp3.*
import org.json.JSONObject
import java.io.IOException
import android.widget.TextView
import android.widget.LinearLayout
import java.util.Currency
import java.util.Locale


class dashboardFrag : Fragment() {

    // =========================
    // DATA
    // =========================
    private var selectedBaseCurrency = "USD"
    private var selectedQuoteCurrency = "PHP"
    private var searchQuery: String = ""

    private val currencyMap = mutableMapOf<String, String>()
    private val currencyList = mutableListOf<String>()
    private var ratesMap: Map<String, Double> = emptyMap()

    // =========================
    // UI
    // =========================
    private lateinit var btnExchange: ImageButton
    private lateinit var btnSearch: ImageButton
    private lateinit var txtBaseCurrency: TextView
    private lateinit var txtRate: TextView
    private lateinit var txtCurrencyName: TextView
    private lateinit var liveRatesContainer: LinearLayout

    private val client = OkHttpClient()

    // =========================
    // LIFECYCLE
    // =========================
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_dashboard, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Bind UI
        btnExchange = view.findViewById(R.id.btnExchange)
        btnSearch = view.findViewById(R.id.btnSearch)
        txtBaseCurrency = view.findViewById(R.id.txtBaseCurrency)
        txtRate = view.findViewById(R.id.txtRate)
        txtCurrencyName = view.findViewById(R.id.txtCurrencyName)
        liveRatesContainer = view.findViewById(R.id.liveRatesContainer)

        // Set white font color for txtRate and txtCurrencyName in the card
        txtRate.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.white))
        txtCurrencyName.setTextColor(0xFFE0E0E0.toInt())

        // Load Preferences
        val sharedPreferences = requireContext().getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
        selectedBaseCurrency = sharedPreferences.getString("defaultCurrency", "USD") ?: "USD"
        selectedQuoteCurrency = sharedPreferences.getString("defaultQuoteCurrency", "PHP") ?: "PHP"

        // Click
        btnExchange.setOnClickListener {
            showCurrencyDropdown()
        }

        btnSearch.setOnClickListener {
            showSearchDialog()
        }

        // Initial UI state
        txtBaseCurrency.text = selectedBaseCurrency

        // Load data
        loadCurrencies()
        fetchRate()
    }

    // =========================
    // SEARCH DIALOG
    // =========================
    private fun showSearchDialog() {
        if (currencyList.isEmpty()) return

        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Search Live Rates")

        val input = EditText(requireContext())
        input.hint = "Enter currency code (e.g. USD)"
        input.setText(searchQuery)
        val lp = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        lp.setMargins(20, 0, 20, 0)
        input.layoutParams = lp
        
        val container = LinearLayout(requireContext())
        container.orientation = LinearLayout.VERTICAL
        container.addView(input)
        builder.setView(container)

        builder.setPositiveButton("Filter") { _, _ ->
            val query = input.text.toString().uppercase(Locale.ROOT).trim()
            searchQuery = query
            updateLiveRatesList()
            if (query.isNotEmpty() && !currencyList.contains(query)) {
                Toast.makeText(requireContext(), "Currency '$query' not found in live rates", Toast.LENGTH_SHORT).show()
            }
        }
        builder.setNeutralButton("Clear") { _, _ ->
            searchQuery = ""
            updateLiveRatesList()
        }
        builder.setNegativeButton("Cancel") { dialog, _ -> dialog.cancel() }

        builder.show()
    }

    // =========================
    // FETCH RATE (RETROFIT)
    // =========================
    private fun fetchRate() {

        val url = "https://api.frankfurter.dev/v1/latest?from=$selectedBaseCurrency"

        val request = Request.Builder()
            .url(url)
            .build()

        client.newCall(request).enqueue(object : Callback {

            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace()
            }

            override fun onResponse(call: Call, response: Response) {

                val json = response.body?.string() ?: return
                val obj = JSONObject(json)

                val ratesObj = obj.getJSONObject("rates")

                val map = mutableMapOf<String, Double>()

                val keys = ratesObj.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    map[key] = ratesObj.getDouble(key)
                }

                // IMPORTANT: store base itself
                map[selectedBaseCurrency] = 1.0

                ratesMap = map

                requireActivity().runOnUiThread {
                    val quoteRate = ratesMap[selectedQuoteCurrency] ?: 0.0
                    val symbol = getCurrencySymbol(selectedQuoteCurrency)
                    txtRate.text = "$symbol %.2f".format(quoteRate)
                    updateLiveRatesList()
                }
            }
        })
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
                else -> code
            }
        }
    }

    // =========================
    // LOAD CURRENCIES (FRANKFURTER API)
    // =========================
    private fun loadCurrencies() {

        val url = "https://api.frankfurter.dev/v1/currencies"

        val request = Request.Builder()
            .url(url)
            .build()

        client.newCall(request).enqueue(object : Callback {

            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace()
            }

            override fun onResponse(call: Call, response: Response) {

                val json = response.body?.string() ?: return
                val obj = JSONObject(json)

                currencyMap.clear()
                currencyList.clear()

                val keys = obj.keys()
                while (keys.hasNext()) {
                    val code = keys.next()
                    val name = obj.getString(code)

                    currencyMap[code] = name
                    currencyList.add(code)
                }

                requireActivity().runOnUiThread {

                    // Set default label
                    updateCurrencyLabels()

                    // Populate scroll list
                    updateLiveRatesList()
                }
            }
        })
    }

    // =========================
    // DROPDOWN
    // =========================
    private fun showCurrencyDropdown() {

        if (currencyList.isEmpty()) return

        AlertDialog.Builder(requireContext())
            .setTitle("Select Currency")
            .setItems(currencyList.toTypedArray()) { _, which ->

                selectedBaseCurrency = currencyList[which]

                txtBaseCurrency.text = selectedBaseCurrency

                updateCurrencyLabels()
                fetchRate()
            }
            .show()
    }

    // =========================
    // UPDATE MAIN LABEL
    // =========================
    private fun updateCurrencyLabels() {

        val name = currencyMap[selectedBaseCurrency]
            ?: selectedBaseCurrency

        txtCurrencyName.text = "1 $name"
    }

    // =========================
    // SCROLL VIEW LIST
    // =========================
    private fun updateLiveRatesList() {

        liveRatesContainer.removeAllViews()

        val baseToQuote = ratesMap[selectedQuoteCurrency] ?: return
        val quoteSymbol = getCurrencySymbol(selectedQuoteCurrency)

        val entries = ratesMap.toSortedMap().entries.toList()
        
        // Filter entries based on search query
        val filteredEntries = if (searchQuery.isEmpty()) {
            entries
        } else {
            entries.filter { it.key.contains(searchQuery, ignoreCase = true) }
        }

        if (filteredEntries.isEmpty() && searchQuery.isNotEmpty()) {
            val emptyMsg = TextView(requireContext())
            emptyMsg.text = "No results found for '$searchQuery'"
            emptyMsg.setPadding(20, 20, 20, 20)
            emptyMsg.gravity = android.view.Gravity.CENTER
            liveRatesContainer.addView(emptyMsg)
            return
        }

        for ((index, entry) in filteredEntries.withIndex()) {
            val currency = entry.key
            val rate = entry.value

            val convertedToQuote = baseToQuote / rate

            // INFLATE THE ROW
            val rowView = LayoutInflater.from(requireContext())
                .inflate(R.layout.item_live_rate, liveRatesContainer, false)

            // CIRCLE (1ST 2 LETTERS)
            val txtCircle = rowView.findViewById<TextView>(R.id.txtCurrencyCircle)
            txtCircle.text = currency.take(2)

            // CURRENCY CODE (USD, EU)
            val txtCode = rowView.findViewById<TextView>(R.id.txtCurrencyCode)
            txtCode.text = currency

            // FULL NAME (UNITED STATES DOLLAR)
            val txtName = rowView.findViewById<TextView>(R.id.txtCurrencyFullName)
            txtName.text = currencyMap[currency] ?: currency

            // RATE & COLOR
            val txtRateView = rowView.findViewById<TextView>(R.id.txtCurrencyRate)
            txtRateView.text = "$quoteSymbol %.2f".format(convertedToQuote)

            when {
                currency == selectedQuoteCurrency -> txtRateView.setTextColor(0xFF9E9E9E.toInt()) // gray — base
                convertedToQuote > 1.0 -> txtRateView.setTextColor(0xFF4CAF50.toInt()) // green — higher
                else -> txtRateView.setTextColor(0xFFF44336.toInt()) // red — lower
            }

            liveRatesContainer.addView(rowView)

            // Divider (skip after last item)
            if (index < filteredEntries.size - 1) {
                val divider = View(requireContext())
                val lp = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    1.dpToPx(requireContext())
                )
                lp.setMargins(74, 0, 16, 0) // indent to align under text, not circle
                divider.layoutParams = lp
                divider.setBackgroundColor(0xFFEEEEEE.toInt())
                liveRatesContainer.addView(divider)
            }
        }
    }
}

// Extension function to convert dp to pixels
fun Int.dpToPx(context: android.content.Context): Int {
    return (this * context.resources.displayMetrics.density).toInt()
}
