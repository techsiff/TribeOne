package com.siffmember.info.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "replyPost_messages")
data class ReplyPostMessage(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val commentId: String,
    val postId: String,
    val postTitle: String,
    val content: String,
    val timestamp: String,
    val groupId: String,
    val userName: String,
    val userId: String
)
