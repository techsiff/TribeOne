package com.siffmember.info.data.remote.model.zoom.meeting

import com.google.gson.annotations.SerializedName


data class MeetingInvitees (

	@SerializedName("email") val email : String
)