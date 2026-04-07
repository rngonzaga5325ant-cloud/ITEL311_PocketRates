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

        for ((currency, rate) in ratesMap.toSortedMap()) {

            val convertedToPHP = baseToPHP / rate

            // Create TextView for currency item
            val textView = TextView(requireContext())
            textView.text =
                "$currency - ${currencyMap[currency] ?: currency} → ₱ %.2f".format(convertedToPHP)
            textView.textSize = 16f
            textView.setPadding(24, 24, 24, 24)
            textView.setTextColor(0xFF000000.toInt())
            liveRatesContainer.addView(textView)

            // Add divider after each item (except the last one)
            if (currency != ratesMap.keys.last()) {
                val divider = View(requireContext())
                val layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    1.dpToPx(requireContext()) // Convert 1dp to pixels
                )
                layoutParams.setMargins(24, 0, 24, 0) // Match padding of text views
                divider.layoutParams = layoutParams
                divider.setBackgroundColor(ContextCompat.getColor(requireContext(), android.R.color.darker_gray))
                liveRatesContainer.addView(divider)
            }
        }
    }
}

// Extension function to convert dp to pixels
fun Int.dpToPx(context: android.content.Context): Int {
    return (this * context.resources.displayMetrics.density).toInt()
}