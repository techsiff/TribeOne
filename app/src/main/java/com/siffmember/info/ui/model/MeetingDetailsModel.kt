package com.siffmember.info.ui.model

data class MeetingDetailsModel(
    val topicName: String? = null,
    val agenda: String? = null,
    val hostName: String? = null,
    val hostNumber: String? = null,
    val meetingNumber: String? = null,
    val meetingPasscode: String? = null,
    val joinURL: String? = null,
    val timestamp: String? = null,
    val memberIds: List<String> = listOf(),
    val members: List<MembersZoomMeeting> = listOf(),
    val meetingStartTimestamp: String? = null,
    var inMeeting: Boolean = false
)
