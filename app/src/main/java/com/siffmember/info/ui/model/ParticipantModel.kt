package com.siffmember.info.ui.model

data class ParticipantModel(
    var userId: String? = null,
    var userName: String? = null,
    var sessions: ArrayList<SessionModel> = arrayListOf()
)