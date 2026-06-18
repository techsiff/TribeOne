package com.siffmember.info.ui.model

data class PostCommentModel(
    val commentId: String? = null,
    val postId: String? = null,
    val postTitle: String? = null,
    val content: String? = null,
    val timestamp: String? = null,
    val userName: String? = null,
    val userId: String? = null
)
