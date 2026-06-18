package com.siffmember.info.data.remote.model.zoom.meeting

import com.google.gson.annotations.SerializedName


data class Interpreters (

    @SerializedName("email") val email : String,
    @SerializedName("sign_language") val sign_language : String
)