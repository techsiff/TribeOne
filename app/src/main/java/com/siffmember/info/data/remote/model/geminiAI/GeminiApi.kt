package com.siffmember.info.data.remote.model.geminiAI

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query

interface GeminiApi {
   // @POST("models/gemini-3.1-flash-lite-preview:generateContent")
    @POST("models/gemini-3.1-flash-lite:generateContent")
    fun generateContent(@Query("key") apiKey: String, @Body request: GeminiRequest): Call<GeminiResponse>

    //@POST("models/gemini-3.1-flash-lite-preview:generateContent")
    @POST("models/gemini-3.1-flash-lite:generateContent")
    suspend fun collectUsersData(@Query("key") apiKey: String, @Body request: GeminiRequest): GeminiResponse
}