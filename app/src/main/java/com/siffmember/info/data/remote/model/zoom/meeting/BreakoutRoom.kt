package com.siffmember.info.data.remote.model.zoom.meeting

import com.google.gson.annotations.SerializedName


data class BreakoutRoom (

    @SerializedName("enable") val enable : Boolean,
    @SerializedName("rooms") val rooms : List<Rooms>
)