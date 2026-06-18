package com.siffmember.info.socialFeeds.feed.model

import java.util.Date

data class Comment(
    val commentId: String = "",
    val postId: String = "",
    val userId: String = "",
    val userName: String = "",
    val userProfileImage: String = "",
    val content: String = "",
    val parentCommentId: String? = null,
    val createdAt: Long = Date().time
) {
    companion object {
        fun fromMap(id: String, postId: String, map: Map<String, Any?>): Comment {
            return Comment(
                commentId = id,
                postId = postId,
                userId = map["userId"] as? String ?: "",
                userName = map["userName"] as? String ?: "Anonymous",
                userProfileImage = map["userProfileImage"] as? String ?: "",
                content = map["content"] as? String ?: "",
                parentCommentId = map["parentCommentId"] as? String,
                createdAt = (map["createdAt"] as? Number)?.toLong() ?: Date().time
            )
        }
    }

    fun toMap(): Map<String, Any?> {
        return mapOf(
            "commentId" to commentId,
            "postId" to postId,
            "userId" to userId,
            "userName" to userName,
            "userProfileImage" to userProfileImage,
            "content" to content,
            "parentCommentId" to parentCommentId,
            "createdAt" to createdAt
        )
    }
}
