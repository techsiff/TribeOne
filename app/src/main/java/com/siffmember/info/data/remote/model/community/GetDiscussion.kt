package com.siffmember.info.data.remote.model.community

data class GetDiscussion(
    val title: String = "",
    val content: String = "",
    val userName: String = "",
    val timestamp: Long = 0,
    var replies: List<Reply> = emptyList(),
    var id: String = ""
 )
