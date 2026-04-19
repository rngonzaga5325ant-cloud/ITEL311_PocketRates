package com.example.pocketrates

import android.os.Bundle
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.appcompat.widget.PopupMenu
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

class exchangeFrag : Fragment() {

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
    private var lastRate = ""

    private val viewModel: TransactionViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_exchange, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        txtCurrentVal   = view.findViewById(R.id.txtCurrentVal)
        lblConvertedBal = view.findViewById(R.id.lblConvertedBal)
        lblConvertionRate = view.findViewById(R.id.lblConvertionRate)
        lblChangeCurrVal  = view.findViewById(R.id.lblChangeCurrVal)
        lblChangeConvVal  = view.findViewById(R.id.lblChangeConvVal)

        lblChangeCurrVal.text = fromCurrency
        lblChangeConvVal.text = toCurrency

        val btnSwap         = view.findViewById<ImageButton>(R.id.btn_swap)
        val btnChangeCurrVal = view.findViewById<ImageButton>(R.id.btnChangeCurrVal)
        val btnChangeConvVal = view.findViewById<ImageButton>(R.id.btnChangeConvVal)
        val btnExchange      = view.findViewById<ImageButton>(R.id.btnExchange)

        btnExchange.setOnClickListener { saveTransaction() }

        btnSwap.setOnClickListener {
            val temp = fromCurrency
            fromCurrency = toCurrency
            toCurrency = temp
            lblChangeCurrVal.text = fromCurrency
            lblChangeConvVal.text = toCurrency
            val previousResult = lblConvertedBal.text.toString()
            if (previousResult.isNotEmpty()) {
                currentInput = previousResult.replace(",", "")
                txtCurrentVal.text = currentInput
                recalculate()
            } else {
                txtCurrentVal.text = ""
                currentInput = ""
                lblConvertedBal.text = ""
            }
        }

        btnChangeCurrVal.setOnClickListener { showCurrencyPopup(it, isFrom = true) }
        btnChangeConvVal.setOnClickListener { showCurrencyPopup(it, isFrom = false) }

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

    private fun saveTransaction() {
        val fromAmount = currentInput
        val toAmount   = lblConvertedBal.text.toString()

        if (fromAmount.isEmpty() || toAmount.isEmpty() || toAmount == "..." || toAmount == "Error") {
            Toast.makeText(requireContext(), "Please enter a valid amount first.", Toast.LENGTH_SHORT).show()
            return
        }

        val calendar = java.util.Calendar.getInstance()
        val date = java.text.SimpleDateFormat("MMM d", java.util.Locale.getDefault()).format(calendar.time)
        val time = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(calendar.time)

        viewModel.insert(
            TransactionEntity(
                fromCurrency = fromCurrency,
                toCurrency   = toCurrency,
                fromAmount   = fromAmount,
                toAmount     = toAmount,
                rate         = lastRate,
                date         = date,
                time         = time
            )
        )

        Toast.makeText(requireContext(), "Transaction saved!", Toast.LENGTH_SHORT).show()
        (activity as MainActivity).apply {
            setSelectedButton(binding.btnTransaction)
            gotoFrag(transacFrag())
        }
    }

    private fun fetchCurrencies() {
        RetrofitClient.api.getCurrencies()
            .enqueue(object : retrofit2.Callback<Map<String, String>> {
                override fun onResponse(call: retrofit2.Call<Map<String, String>>, response: retrofit2.Response<Map<String, String>>) {
                    if (response.isSuccessful) {
                        currencyList = response.body()?.keys?.toList()?.sorted() ?: return
                    }
                }
                override fun onFailure(call: retrofit2.Call<Map<String, String>>, t: Throwable) {
                    t.printStackTrace()
                }
            })
    }

    private fun showCurrencyPopup(anchor: View, isFrom: Boolean) {
        val popup = PopupMenu(requireContext(), anchor)
        currencyList.forEach { popup.menu.add(it) }
        popup.setOnMenuItemClickListener { item ->
            if (isFrom) { fromCurrency = item.title.toString(); lblChangeCurrVal.text = fromCurrency }
            else        { toCurrency   = item.title.toString(); lblChangeConvVal.text = toCurrency }
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
        runnable?.let { handler.removeCallbacks(it) }
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
            lblConvertedBal.text = "..."
            convertCurrency(currentInput.toDouble())
        } else {
            lblConvertedBal.text  = ""
            lblConvertionRate.text = ""
        }
    }

    private fun handleScroll() {
        txtCurrentVal.post {
            val textWidth = txtCurrentVal.layout?.getLineRight(0)?.toInt() ?: 0
            val viewWidth = txtCurrentVal.width - txtCurrentVal.paddingStart - txtCurrentVal.paddingEnd
            if (textWidth > viewWidth) txtCurrentVal.scrollTo(textWidth - viewWidth, 0)
            else txtCurrentVal.scrollTo(0, 0)
        }
    }

    private fun convertCurrency(amount: Double) {
        RetrofitClient.api.getRate(fromCurrency, toCurrency)
            .enqueue(object : retrofit2.Callback<RateResponse> {
                override fun onResponse(call: retrofit2.Call<RateResponse>, response: retrofit2.Response<RateResponse>) {
                    if (response.isSuccessful) {
                        val rates = response.body()?.rates ?: return
                        val rate  = rates[toCurrency] ?: return
                        val converted = rate * amount
                        lblConvertedBal.text  = String.format(java.util.Locale.US, "%.2f", converted)
                        lastRate = "1 $fromCurrency = ${String.format(java.util.Locale.US, "%.4f", rate)} $toCurrency"
                        lblConvertionRate.text = lastRate
                    }
                }
                override fun onFailure(call: retrofit2.Call<RateResponse>, t: Throwable) {
                    lblConvertedBal.text = "Error"
                }
            })
    }
}