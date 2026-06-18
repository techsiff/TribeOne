package com.siffmember.info.data.remote.model.zoom.meeting

import com.google.gson.annotations.SerializedName

data class TrackingFields (
    @SerializedName("field") val field : String,
    @SerializedName("value") val value : String,
    @SerializedName("visible") val visible : Boolean
)