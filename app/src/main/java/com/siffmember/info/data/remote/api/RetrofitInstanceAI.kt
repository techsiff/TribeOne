package com.siffmember.info.data.remote.api

import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitInstanceAI {

    private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/"

    fun getClient(): Retrofit {
        val okHttpClient = OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .addInterceptor(loggingInterceptor)
            .build()

        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    private val loggingInterceptor = HttpLoggingInterceptor { message ->
        Log.e("loggingInterceptor",""+ message)
    }.apply {
        level = HttpLoggingInterceptor.Level.BODY
    }
}