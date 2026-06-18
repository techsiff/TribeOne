package com.siffmember.info.data.remote.model.zoom.meeting

import com.google.gson.annotations.SerializedName


data class Resources (

    @SerializedName("resource_type") val resource_type : String,
    @SerializedName("resource_id") val resource_id : String,
    @SerializedName("permission_level") val permission_level : String
)