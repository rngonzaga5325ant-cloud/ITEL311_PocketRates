package com.example.pocketrates

import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface FrankfurterApi {
    @GET("latest")
    fun getRate(
        @Query("from") base: String,
        @Query("to") quote: String
    ): Call<RateResponse>

    @GET("currencies")
    fun getCurrencies(): Call<Map<String, String>>
}