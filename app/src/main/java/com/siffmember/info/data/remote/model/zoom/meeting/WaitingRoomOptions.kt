package com.siffmember.info.data.remote.model.zoom.meeting

import com.google.gson.annotations.SerializedName

data class WaitingRoomOptions (
    @SerializedName("mode") val mode : String,
    @SerializedName("who_goes_to_waiting_room") val who_goes_to_waiting_room : String
)