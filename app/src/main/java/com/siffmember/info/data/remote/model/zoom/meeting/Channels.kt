package com.siffmember.info.data.remote.model.zoom.meeting

import com.google.gson.annotations.SerializedName


data class Channels (

    @SerializedName("channel_id") val channel_id : String,
    @SerializedName("name") val name : String
)