package com.siffmember.info.data.remote.model.zoom.meeting

import com.google.gson.annotations.SerializedName


data class AuthenticationException (

    @SerializedName("email") val email : String,
    @SerializedName("name") val name : String,
    @SerializedName("join_url") val join_url : String
)