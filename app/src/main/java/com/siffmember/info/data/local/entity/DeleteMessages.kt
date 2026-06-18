package com.siffmember.info.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "delete_messages")
data class DeleteMessages(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val postId: String,
    val timestamp: String
)
