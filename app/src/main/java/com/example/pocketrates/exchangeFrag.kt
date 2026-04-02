package com.example.pocketrates

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.ImageButton
import androidx.appcompat.widget.PopupMenu


class exchangeFrag : Fragment() {

    //prevent from delay of fetching conversion
    private val handler = android.os.Handler(android.os.Looper.getMainLooper())
    private var runnable: Runnable? = null

    private lateinit var txtCurrentVal: TextView
    private lateinit var lblConvertedBal: TextView
    private lateinit var lblConvertionRate: TextView
    private lateinit var lblChangeCurrVal: TextView
    private lateinit var lblChangeConvVal: TextView

    private var currentInput = ""
    private var fromCurrency = "PHP"
    private var toCurrency = "USD"
    private var currencyList = listOf<String>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_exchange, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        txtCurrentVal = view.findViewById(R.id.txtCurrentVal)
        lblConvertedBal = view.findViewById(R.id.lblConvertedBal)
        lblConvertionRate = view.findViewById(R.id.lblConvertionRate)
        lblChangeCurrVal = view.findViewById(R.id.lblChangeCurrVal)
        lblChangeConvVal = view.findViewById(R.id.lblChangeConvVal)

        lblChangeCurrVal.text = fromCurrency
        lblChangeConvVal.text = toCurrency

        val btnSwap = view.findViewById<ImageButton>(R.id.btn_swap)
        val btnChangeCurrVal = view.findViewById<ImageButton>(R.id.btnChangeCurrVal)
        val btnChangeConvVal = view.findViewById<ImageButton>(R.id.btnChangeConvVal)

        btnSwap.setOnClickListener {
            // 1. Swap the internal currency codes
            val tempCurrency = fromCurrency
            fromCurrency = toCurrency
            toCurrency = tempCurrency

            // 2. Update the UI labels (PHP <-> USD)
            lblChangeCurrVal.text = fromCurrency
            lblChangeConvVal.text = toCurrency

            // 3. Swap the values: Move the converted result to the top input
            val previousResult = lblConvertedBal.text.toString()

            if (previousResult.isNotEmpty()) {
                // Clean any potential commas and update the input variable
                currentInput = previousResult.replace(",", "")
                txtCurrentVal.text = currentInput

                // 4. Trigger a fresh calculation with the new base/quote
                recalculate()
            } else {
                // Just swap symbols if there was no number entered
                txtCurrentVal.text = ""
                currentInput = ""
                lblConvertedBal.text = ""
            }
        }


        btnChangeCurrVal.setOnClickListener {
            showCurrencyPopup(it, isFrom = true)
        }

        btnChangeConvVal.setOnClickListener {
            showCurrencyPopup(it, isFrom = false)
        }

        view.findViewById<Button>(R.id.btn1).setOnClickListener { onNumberClick("1") }
        view.findViewById<Button>(R.id.btn2).setOnClickListener { onNumberClick("2") }
        view.findViewById<Button>(R.id.btn3).setOnClickListener { onNumberClick("3") }
        view.findViewById<Button>(R.id.btn4).setOnClickListener { onNumberClick("4") }
        view.findViewById<Button>(R.id.btn5).setOnClickListener { onNumberClick("5") }
        view.findViewById<Button>(R.id.btn6).setOnClickListener { onNumberClick("6") }
        view.findViewById<Button>(R.id.btn7).setOnClickListener { onNumberClick("7") }
        view.findViewById<Button>(R.id.btn8).setOnClickListener { onNumberClick("8") }
        view.findViewById<Button>(R.id.btn9).setOnClickListener { onNumberClick("9") }
        view.findViewById<Button>(R.id.btn0).setOnClickListener { onNumberClick("0") }
        view.findViewById<Button>(R.id.btnDec).setOnClickListener { onNumberClick(".") }
        view.findViewById<Button>(R.id.btnDelete).setOnClickListener { onDeleteClick() }

        fetchCurrencies()
    }

    private fun fetchCurrencies() {
        RetrofitClient.api.getCurrencies()
            .enqueue(object : retrofit2.Callback<Map<String, String>> {
                override fun onResponse(
                    call: retrofit2.Call<Map<String, String>>,
                    response: retrofit2.Response<Map<String, String>>
                ) {
                    if (response.isSuccessful) {
                        val symbols = response.body() ?: return
                        currencyList = symbols.keys.toList().sorted()
                    }
                }

                override fun onFailure(call: retrofit2.Call<Map<String, String>>, t: Throwable) {
                    t.printStackTrace()
                }
            })
    }

    private fun showCurrencyPopup(anchor: View, isFrom: Boolean) {
        val popup = PopupMenu(requireContext(), anchor)
        currencyList.forEach { currency ->
            popup.menu.add(currency)
        }
        popup.setOnMenuItemClickListener { item ->
            if (isFrom) {
                fromCurrency = item.title.toString()
                lblChangeCurrVal.text = fromCurrency
            } else {
                toCurrency = item.title.toString()
                lblChangeConvVal.text = toCurrency
            }
            recalculate()
            true
        }
        popup.show()
    }

    private fun onNumberClick(value: String) {
        if (value == "." && currentInput.contains(".")) return
        currentInput += value
        txtCurrentVal.text = currentInput
        handleScroll()

        // 1. Cancel the previous timer/request
        runnable?.let { handler.removeCallbacks(it) }

        // 2. Start a 400ms timer. If the user types another number before 400ms,
        // the previous request is CANCELLED. Only the final number gets sent to the API.
        runnable = Runnable { recalculate() }
        handler.postDelayed(runnable!!, 400)
    }

    private fun onDeleteClick() {
        if (currentInput.isNotEmpty()) {
            currentInput = currentInput.dropLast(1)
            txtCurrentVal.text = currentInput
            recalculate()
            handleScroll()
        }
    }

    private fun recalculate() {
        if (currentInput.isNotEmpty() && currentInput != ".") {
            // delay
            lblConvertedBal.text = "..."
            convertCurrency(currentInput.toDouble())
        } else {
            lblConvertedBal.text = ""
            lblConvertionRate.text = ""
        }
    }

    private fun handleScroll() {
        txtCurrentVal.post {
            val textWidth = txtCurrentVal.layout?.getLineRight(0)?.toInt() ?: 0
            val viewWidth = txtCurrentVal.width - txtCurrentVal.paddingStart - txtCurrentVal.paddingEnd
            if (textWidth > viewWidth) {
                txtCurrentVal.scrollTo(textWidth - viewWidth, 0)
            } else {
                txtCurrentVal.scrollTo(0, 0)
            }
        }
    }

    private fun convertCurrency(amount: Double) {
        RetrofitClient.api.getRate(fromCurrency, toCurrency)
            .enqueue(object : retrofit2.Callback<RateResponse> {
                override fun onResponse(call: retrofit2.Call<RateResponse>, response: retrofit2.Response<RateResponse>) {
                    if (response.isSuccessful) {
                        val rates = response.body()?.rates ?: return
                        val rate = rates[toCurrency] ?: return
                        val converted = rate * amount

                        // Fix: Use Locale.US to ensure a "." decimal for the swap logic
                        lblConvertedBal.text = String.format(java.util.Locale.US, "%.2f", converted)

                        // Fix: Use a template for the rate label
                        lblConvertionRate.text = "1 $fromCurrency = ${String.format(java.util.Locale.US, "%.4f", rate)} $toCurrency"
                    }
                }
                override fun onFailure(call: retrofit2.Call<RateResponse>, t: Throwable) {
                    lblConvertedBal.text = "Error"
                }
            })
    }

}