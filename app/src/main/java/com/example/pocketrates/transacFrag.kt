package com.example.pocketrates

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels

class transacFrag : Fragment() {

    private val viewModel: TransactionViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_transac, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val container = view.findViewById<LinearLayout>(R.id.transactionContainer)

        viewModel.allTransactions.observe(viewLifecycleOwner) { transactions ->
            container.removeAllViews()

            transactions.forEach { transaction ->
                val itemView = layoutInflater.inflate(R.layout.item_transaction, container, false)

                itemView.findViewById<TextView>(R.id.lblExch).text    = transaction.fromCurrency
                itemView.findViewById<TextView>(R.id.lblCurr).text    = transaction.toCurrency
                itemView.findViewById<TextView>(R.id.lblExchVal).text = transaction.fromAmount
                itemView.findViewById<TextView>(R.id.lblCurrVal).text = transaction.toAmount
                itemView.findViewById<TextView>(R.id.lblTrs_Date).text = transaction.date
                itemView.findViewById<TextView>(R.id.lblTrs_Time).text = transaction.time

                container.addView(itemView)
            }
        }
    }
}