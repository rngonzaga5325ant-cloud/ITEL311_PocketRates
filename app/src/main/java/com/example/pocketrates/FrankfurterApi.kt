package com.example.pocketrates

import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Path

interface FrankfurterApi {

    @GET("v2/rate/{base}/{quote}")
    fun getRate(
        @Path("base") base: String,
        @Path("quote") quote: String
    ): Call<RateResponse>
}