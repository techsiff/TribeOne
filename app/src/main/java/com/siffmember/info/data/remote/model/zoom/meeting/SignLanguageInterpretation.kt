package com.siffmember.info.data.remote.model.zoom.meeting

import com.google.gson.annotations.SerializedName

data class SignLanguageInterpretation (

    @SerializedName("enable") val enable : Boolean,
    @SerializedName("interpreters") val interpreters : List<Interpreters>
)