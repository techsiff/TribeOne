package com.siffmember.info.ui.model

data class MeetingAnalyticsModel(
    var hostName: String? = null,
    var hostNumber: String? = null,
    var meetingNumber: String? = null,
    var topicName: String? = null,
    var startTime: String? = null,
    var endTime: String? = null,
    var participants: ArrayList<ParticipantModel> = arrayListOf()
)
