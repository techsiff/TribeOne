package com.siffmember.info.data.remote.api

import com.siffmember.info.data.remote.model.youtube.YouTubeResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface YouTubeApiService {
    @GET("videos")
    suspend fun getVideoDetails(
        @Query("part") part: String,
        @Query("id") videoId: String,
        @Query("key") apiKey: String
    ): YouTubeResponse
}