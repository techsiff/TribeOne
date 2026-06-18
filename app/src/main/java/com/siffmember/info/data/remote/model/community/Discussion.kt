package com.siffmember.info.data.remote.model.community

data class Discussion(
    val title: String = "",
    val content: String = "",
    val userName: String = "",
    val timestamp: Long = System.currentTimeMillis()
)