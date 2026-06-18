package com.siffmember.info.data.remote.model.zoom.meeting

import com.google.gson.annotations.SerializedName


data class AutoAddRecordingToVideoManagement (

    @SerializedName("enable") val enable : Boolean,
    @SerializedName("channels") val channels : List<Channels>
)