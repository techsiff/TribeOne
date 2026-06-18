package com.siffmember.info.data.remote.model.zoom.meeting

import com.google.gson.annotations.SerializedName

data class Recurrence (

    @SerializedName("end_date_time") val end_date_time : String,
    @SerializedName("end_times") val end_times : Int,
    @SerializedName("monthly_day") val monthly_day : Int,
    @SerializedName("monthly_week") val monthly_week : Int,
    @SerializedName("monthly_week_day") val monthly_week_day : Int,
    @SerializedName("repeat_interval") val repeat_interval : Int,
    @SerializedName("type") val type : Int,
    @SerializedName("weekly_days") val weekly_days : Int
)