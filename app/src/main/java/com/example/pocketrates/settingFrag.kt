package com.example.pocketrates

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.edit
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import okhttp3.*
import org.json.JSONObject
import java.io.IOException

class settingFrag : Fragment() {

    private val client = OkHttpClient()
    private val currencyList = mutableListOf<String>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_setting, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val btnSetDefaultCurrency: Button = view.findViewById(R.id.btnSetDefaultCurrency)
        val btnClearTransaction: Button = view.findViewById(R.id.btnClearTransaction)
        val btnResetSettings: Button = view.findViewById(R.id.btnResetSettings)

        btnSetDefaultCurrency.setOnClickListener {
            if (currencyList.isEmpty()) {
                loadCurrencies {
                    showQuoteCurrencySelectionDialog()
                }
            } else {
                showQuoteCurrencySelectionDialog()
            }
        }

        btnClearTransaction.setOnClickListener {
            showClearTransactionDialog()
        }

        btnResetSettings.setOnClickListener {
            showResetSettingsDialog()
        }

        loadCurrencies()
    }

    private fun loadCurrencies(onLoaded: (() -> Unit)? = null) {
        val url = "https://api.frankfurter.dev/v1/currencies"
        val request = Request.Builder().url(url).build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                activity?.runOnUiThread {
                    Toast.makeText(context, "Failed to load currencies", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val json = response.body?.string() ?: return
                val obj = JSONObject(json)
                
                currencyList.clear()
                val keys = obj.keys()
                while (keys.hasNext()) {
                    currencyList.add(keys.next())
                }
                currencyList.sort()

                activity?.runOnUiThread {
                    onLoaded?.invoke()
                }
            }
        })
    }

    private fun showQuoteCurrencySelectionDialog() {
        if (currencyList.isEmpty()) return

        AlertDialog.Builder(requireContext())
            .setTitle("Select Quote Currency")
            .setItems(currencyList.toTypedArray()) { _, which ->
                val selectedCurrency = currencyList[which]
                showQuoteCurrencyConfirmation(selectedCurrency)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showQuoteCurrencyConfirmation(currency: String) {
        val dialog = Dialog(requireContext())
        dialog.setContentView(R.layout.activity_default_dialog_settings)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)

        val btnConfirm: Button = dialog.findViewById(R.id.btnResetSetting)
        val btnSkip: Button = dialog.findViewById(R.id.btnSkip)
        
        // Update text for quote currency confirmation
        dialog.findViewById<View>(R.id.main).let { container ->
            if (container is ViewGroup) {
                val textLayout = container.getChildAt(0) as? ViewGroup
                (textLayout?.getChildAt(0) as? TextView)?.text = "Confirm Quote Currency"
                (textLayout?.getChildAt(1) as? TextView)?.text = "Set $currency as quote currency?"
            }
        }
        btnConfirm.text = "Set Quote"

        btnConfirm.setOnClickListener {
            saveQuoteCurrency(currency)
            Toast.makeText(context, "Quote currency set to $currency", Toast.LENGTH_SHORT).show()
            dialog.dismiss()
            
            // Direct to dashboardFrag to see changes
            (activity as? MainActivity)?.let { main ->
                main.setSelectedButton(main.binding.btnDashboard)
                main.gotoFrag(dashboardFrag())
            }
        }

        btnSkip.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun showClearTransactionDialog() {
        val dialog = Dialog(requireContext())
        dialog.setContentView(R.layout.activity_clear_dialog_settings)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)

        val btnClear: Button = dialog.findViewById(R.id.btnDialogClearDB)
        val btnCancel: Button = dialog.findViewById(R.id.btnCancel)
        val loadingLayout: LinearLayout = dialog.findViewById(R.id.loadingLayout)
        val buttonLayout: LinearLayout = dialog.findViewById(R.id.buttonLayout)

        btnClear.setOnClickListener {
            lifecycleScope.launch {
                // Show loading UI
                buttonLayout.visibility = View.GONE
                loadingLayout.visibility = View.VISIBLE
                
                // Simulate processing time for animation
                delay(1500)

                val db = AppDatabase.getDatabase(requireContext())
                db.transactionDao().deleteAllTransactions()
                
                activity?.runOnUiThread {
                    Toast.makeText(context, "Transaction history cleared", Toast.LENGTH_SHORT).show()
                    dialog.dismiss()
                    
                    // Direct to transacFrag
                    (activity as? MainActivity)?.let { main ->
                        main.setSelectedButton(main.binding.btnTransaction)
                        main.gotoFrag(transacFrag())
                    }
                }
            }
        }

        btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun showResetSettingsDialog() {
        val dialog = Dialog(requireContext())
        dialog.setContentView(R.layout.activity_default_dialog_settings)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)

        val btnReset: Button = dialog.findViewById(R.id.btnResetSetting)
        val btnSkip: Button = dialog.findViewById(R.id.btnSkip)

        btnReset.setOnClickListener {
            resetAllSettings()
            Toast.makeText(context, "Settings reset to PHP default", Toast.LENGTH_SHORT).show()
            dialog.dismiss()

            // Direct to dashboardFrag to see changes
            (activity as? MainActivity)?.let { main ->
                main.setSelectedButton(main.binding.btnDashboard)
                main.gotoFrag(dashboardFrag())
            }
        }

        btnSkip.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun saveQuoteCurrency(currency: String) {
        val sharedPreferences = requireContext().getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
        sharedPreferences.edit {
            putString("defaultQuoteCurrency", currency)
        }
    }

    private fun resetAllSettings() {
        val sharedPreferences = requireContext().getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
        sharedPreferences.edit {
            clear()
            putBoolean("isFirstRun", false)
            putString("defaultQuoteCurrency", "PHP")
            putString("defaultCurrency", "USD")
        }
    }
}
