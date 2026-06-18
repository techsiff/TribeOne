package com.siffmember.info.data.remote.api

import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitInstanceYoutube {
    private const val BASE_URL = "https://www.googleapis.com/youtube/v3/"
    private var retrofit: Retrofit? = null
    @JvmStatic
    val client: Retrofit?
        get() {
            if (retrofit == null) {
                retrofit = Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .addCallAdapterFactory(RetryCallAdapterFactory.create())
                    .addConverterFactory(GsonConverterFactory.create())
                    .client(okHttpClient)
                    .build()
            }
            return retrofit
        }

    private val loggingInterceptor = HttpLoggingInterceptor { message ->
         Log.e("loggingInterceptor",""+message)
    }.apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private var okHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .build()

    val youtubeService: YouTubeApiService = client!!.create(YouTubeApiService::class.java)
}