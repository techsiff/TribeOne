package com.siffmember.info.data.remote.model.zoom

data class ZoomMeetingDeleteRequest(
    val meetingId: String,
    val occurrenceId: String,
    val scheduleForReminder: Boolean,
    val cancelMeetingReminder: Boolean,
    val clientId: String,
    val clientSecret: String,
    val accountId: String
)
