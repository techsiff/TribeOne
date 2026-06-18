package com.siffmember.info.ui.model

data class LastChatsModel(
    var groupName: String = "",
    var groupId: String = "",
    var content: String = "",
    var timestamp: String = "",
    var userName: String = "",
    var userId: String = "",
    var isSent: Boolean = false
)