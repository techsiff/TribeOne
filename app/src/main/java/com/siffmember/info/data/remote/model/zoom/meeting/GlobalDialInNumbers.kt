package com.siffmember.info.data.remote.model.zoom.meeting

import com.google.gson.annotations.SerializedName


data class GlobalDialInNumbers (

    @SerializedName("city") val city : String,
    @SerializedName("country") val country : String,
    @SerializedName("country_name") val country_name : String,
    @SerializedName("number") val number : String,
    @SerializedName("type") val type : String
)