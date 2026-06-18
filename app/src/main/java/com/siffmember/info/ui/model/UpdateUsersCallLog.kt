package com.siffmember.info.ui.model

data class UpdateUsersCallLog(
    val fromUserName: String? = null,
    val fromUserPhoneNumber: String? = null,
    val toUserName: String? = null,
    val toUserPhoneNumber: String? = null,
    val duration: String? = null,
    val timestamp: String? = null
)
