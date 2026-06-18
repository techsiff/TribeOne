package com.siffmember.info.data.remote.model.zoom.meeting

import com.google.gson.annotations.SerializedName

data class ApprovedORDeniedCountriesORRegions (

    @SerializedName("approved_list") val approved_list : List<String>,
    @SerializedName("denied_list") val denied_list : List<String>,
    @SerializedName("enable") val enable : Boolean,
    @SerializedName("method") val method : String
)