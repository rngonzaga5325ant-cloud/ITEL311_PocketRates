package com.example.pocketrates

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


class dashboardFrag : Fragment() {

    // =========================
    // DATA
    // =========================
    private var selectedBaseCurrency = "USD"

    private var baseCurrency = "USD"
    private val quoteCurrency = "PHP"

    private val currencyMap = mutableMapOf<String, String>()
    private val currencyList = mutableListOf<String>()
    private var ratesMap: Map<String, Double> = emptyMap()

    // =========================
    // UI
    // =========================
    private lateinit var btnExchange: ImageButton
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
        txtBaseCurrency = view.findViewById(R.id.txtBaseCurrency)
        txtRate = view.findViewById(R.id.txtRate)
        txtCurrencyName = view.findViewById(R.id.txtCurrencyName)
        liveRatesContainer = view.findViewById(R.id.liveRatesContainer)

        // Set black font color for txtRate and txtCurrencyName
        txtRate.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.black))
        txtCurrencyName.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.black))

        // Click
        btnExchange.setOnClickListener {
            showCurrencyDropdown()
        }

        // Initial UI state
        txtBaseCurrency.text = selectedBaseCurrency

        // Load data
        loadCurrencies()
        fetchRate()
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
                    val phpRate = ratesMap["PHP"] ?: return@runOnUiThread
                    txtRate.text = "₱ %.2f".format(phpRate)
                    updateLiveRatesList()
                }
            }
        })
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

        val baseToPHP = ratesMap["PHP"] ?: return

        val entries = ratesMap.toSortedMap().entries.toList()

        for ((index, entry) in entries.withIndex()) {
            val currency = entry.key
            val rate = entry.value

            val convertedToPHP = baseToPHP / rate

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
            txtRateView.text = "%.2f".format(convertedToPHP)

            when {
                currency == "PHP" -> txtRateView.setTextColor(0xFF9E9E9E.toInt()) // gray — base
                convertedToPHP > 1.0 -> txtRateView.setTextColor(0xFF4CAF50.toInt()) // green — higher
                else -> txtRateView.setTextColor(0xFFF44336.toInt()) // red — lower
            }

            liveRatesContainer.addView(rowView)

            // Divider (skip after last item)
            if (index < entries.size - 1) {
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