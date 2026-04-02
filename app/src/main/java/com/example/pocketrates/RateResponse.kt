package com.example.pocketrates

import com.google.gson.annotations.SerializedName

data class RateResponse(
    @SerializedName("rates")
    val rates: Map<String, Double>, // Change 'rate' to 'rates'
    val amount: Double,
    val base: String,
    val date: String
)