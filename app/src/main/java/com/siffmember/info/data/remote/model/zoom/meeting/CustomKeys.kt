package com.siffmember.info.data.remote.model.zoom.meeting

import com.google.gson.annotations.SerializedName


data class CustomKeys (

    @SerializedName("key") val key : String,
    @SerializedName("value") val value : String
)