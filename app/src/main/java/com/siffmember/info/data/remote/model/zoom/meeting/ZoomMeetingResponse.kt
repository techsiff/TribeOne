package com.siffmember.info.data.remote.model.zoom.meeting

import com.google.gson.annotations.SerializedName


data class ZoomMeetingResponse (

    @SerializedName("assistant_id") val assistant_id : String,
    @SerializedName("host_email") val host_email : String,
    @SerializedName("host_id") val host_id : String,
    @SerializedName("id") val id : Long,
    @SerializedName("uuid") val uuid : String,
    @SerializedName("registration_url") val registration_url : String,
    @SerializedName("agenda") val agenda : String,
    @SerializedName("created_at") val created_at : String,
    @SerializedName("duration") val duration : Int,
    @SerializedName("encrypted_password") val encrypted_password : String,
    @SerializedName("pstn_password") val pstn_password : Int,
    @SerializedName("h323_password") val h323_password : Int,
    @SerializedName("join_url") val join_url : String,
    @SerializedName("chat_join_url") val chat_join_url : String,
    @SerializedName("occurrences") val occurrences : List<Occurrences>,
    @SerializedName("password") val password : String,
    @SerializedName("pmi") val pmi : Int,
    @SerializedName("pre_schedule") val pre_schedule : Boolean,
    @SerializedName("recurrence") val recurrence : Recurrence,
    @SerializedName("settings") val settings : Settings,
    @SerializedName("start_time") val start_time : String,
    @SerializedName("start_url") val start_url : String,
    @SerializedName("status") val status : String,
    @SerializedName("timezone") val timezone : String,
    @SerializedName("topic") val topic : String,
    @SerializedName("tracking_fields") val tracking_fields : List<TrackingFields>,
    @SerializedName("type") val type : Int,
    @SerializedName("dynamic_host_key") val dynamic_host_dynamic_host_key : Int,
    @SerializedName("creation_source") val creation_source : String
)