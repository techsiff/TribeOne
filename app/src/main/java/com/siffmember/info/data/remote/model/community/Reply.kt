package com.siffmember.info.data.remote.model.community

data class Reply(
    val content: String = "",
    val userName: String = "",
    val timestamp: Long = System.currentTimeMillis()
)