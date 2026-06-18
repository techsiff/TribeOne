package com.siffmember.info.data.remote.model.zoom.meeting

import com.google.gson.annotations.SerializedName

data class Occurrences (

    @SerializedName("duration") val duration : Int,
    @SerializedName("occurrence_id") val occurrence_id : Int,
    @SerializedName("start_time") val start_time : String,
    @SerializedName("status") val status : String
)