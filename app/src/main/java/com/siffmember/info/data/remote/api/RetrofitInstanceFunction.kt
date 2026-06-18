package com.siffmember.info.data.remote.api

import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitInstanceFunction {

    private val retrofit by lazy {
        Retrofit.Builder()
            .baseUrl("https://asia-south1-siffmembershipinfo.cloudfunctions.net/") // Replace with your function URL
            .addConverterFactory(GsonConverterFactory.create())
            .client(okHttpClient)
            .build()
    }

    val api: FirebaseFunctionApi by lazy {
        retrofit.create(FirebaseFunctionApi::class.java)
    }
    private val loggingInterceptor = HttpLoggingInterceptor { message ->
        Log.e("loggingInterceptor",""+ message)
    }.apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private var okHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .build()
}