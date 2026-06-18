package com.siffmember.info.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "post_messages")
data class PostMessage(
    @PrimaryKey val postId: String,
    val postTitle: String,
    val content: String,
    val timestamp: String,
    val groupName: String,
    val groupId: String,
    val userName: String,
    val userId: String
)
