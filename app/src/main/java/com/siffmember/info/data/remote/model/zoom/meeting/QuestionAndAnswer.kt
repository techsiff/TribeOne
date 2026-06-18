package com.siffmember.info.data.remote.model.zoom.meeting

import com.google.gson.annotations.SerializedName

data class QuestionAndAnswer (

    @SerializedName("enable") val enable : Boolean,
    @SerializedName("allow_submit_questions") val allow_submit_questions : Boolean,
    @SerializedName("allow_anonymous_questions") val allow_anonymous_questions : Boolean,
    @SerializedName("question_visibility") val question_visibility : String,
    @SerializedName("attendees_can_comment") val attendees_can_comment : Boolean,
    @SerializedName("attendees_can_upvote") val attendees_can_upvote : Boolean
)