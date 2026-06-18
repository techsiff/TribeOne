package com.siffmember.info.socialFeeds.feed.data

import com.siffmember.info.socialFeeds.feed.model.Comment
import kotlinx.coroutines.flow.Flow

interface CommentRepository {
    fun getCommentsForPost(postId: String): Flow<List<Comment>>
    suspend fun addComment(comment: Comment): Result<String>
    suspend fun editComment(postId: String, commentId: String, newContent: String): Result<Unit>
    suspend fun deleteComment(postId: String, commentId: String): Result<Unit>
}
