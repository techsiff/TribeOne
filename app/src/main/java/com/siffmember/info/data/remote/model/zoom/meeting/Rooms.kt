package com.siffmember.info.data.remote.model.zoom.meeting

import com.google.gson.annotations.SerializedName

data class Rooms (

    @SerializedName("name") val name : String,
    @SerializedName("participants") val participants : List<String>
)