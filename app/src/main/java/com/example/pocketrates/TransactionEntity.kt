package com.example.pocketrates

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val fromCurrency: String,
    val toCurrency: String,
    val fromAmount: String,
    val toAmount: String,
    val rate: String,
    val date: String,
    val time: String
)
