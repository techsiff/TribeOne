package com.siffmember.info.ui.model

data class Community(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val createdBy: String = "",  // Admin's email/ID
    val createdAt: String = "",
    val groupIcon: String = "",
    val members: List<MembersGroup> = listOf() // List of member IDs
)
