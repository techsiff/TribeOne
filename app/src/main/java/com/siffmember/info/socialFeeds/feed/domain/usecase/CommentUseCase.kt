package com.siffmember.info.socialFeeds.feed.domain.usecase

import com.siffmember.info.socialFeeds.feed.data.CommentRepository
import com.siffmember.info.socialFeeds.feed.model.Comment
import kotlinx.coroutines.flow.Flow
import java.util.Date

class CommentUseCase(private val commentRepository: CommentRepository) {
    
    fun getComments(postId: String): Flow<List<Comment>> {
        return commentRepository.getCommentsForPost(postId)
    }

    suspend fun addComment(
        postId: String,
        userId: String,
        userName: String,
        userProfileImage: String,
        content: String,
        parentCommentId: String? = null
    ): Result<String> {
        val comment = Comment(
            commentId = "",
            postId = postId,
            userId = userId,
            userName = userName,
            userProfileImage = userProfileImage,
            content = content,
            parentCommentId = parentCommentId,
            createdAt = Date().time
        )
        return commentRepository.addComment(comment)
    }

    suspend fun editComment(postId: String, commentId: String, newContent: String): Result<Unit> {
        return commentRepository.editComment(postId, commentId, newContent)
    }

    suspend fun deleteComment(postId: String, commentId: String): Result<Unit> {
        return commentRepository.deleteComment(postId, commentId)
    }
}
