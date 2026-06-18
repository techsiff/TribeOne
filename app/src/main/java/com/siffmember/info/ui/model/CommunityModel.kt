package com.siffmember.info.ui.model

data class CommunityModel(
    val groupID: String,
    val groupName: String,
    val description: String,
    val createdBy: String,
    val createdAt: String = "",
    val groupIcon: String = "",
    val chats: LastChatsModel
)