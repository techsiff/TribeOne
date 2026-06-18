package com.siffmember.info.data.remote.model.zoom

data class CreateMeetingPayload(
    val topic: String,
    val agenda: String,
    val start_time: String,
)
