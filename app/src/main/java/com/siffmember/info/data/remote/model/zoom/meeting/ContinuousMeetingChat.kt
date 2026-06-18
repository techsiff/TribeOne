package com.siffmember.info.data.remote.model.zoom.meeting

import com.google.gson.annotations.SerializedName


data class ContinuousMeetingChat (

    @SerializedName("enable") val enable : Boolean,
    @SerializedName("channel_id") val channel_id : String
)