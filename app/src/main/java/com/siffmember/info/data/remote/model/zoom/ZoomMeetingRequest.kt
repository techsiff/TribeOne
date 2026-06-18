package com.siffmember.info.data.remote.model.zoom

data class ZoomMeetingRequest(
    val payload: CreateMeetingPayload,
    val clientId: String,
    val clientSecret: String,
    val accountId: String
)
