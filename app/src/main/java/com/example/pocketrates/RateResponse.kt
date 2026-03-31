package com.example.pocketrates

data class RateResponse(
    val date: String,
    val base: String,
    val quote: String,
    val rate: Double
)