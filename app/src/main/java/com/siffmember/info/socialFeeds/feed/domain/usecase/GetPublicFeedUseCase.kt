package com.siffmember.info.socialFeeds.feed.domain.usecase

import com.siffmember.info.socialFeeds.feed.data.FeedRepository
import com.siffmember.info.socialFeeds.feed.model.Post
import kotlinx.coroutines.flow.Flow

class GetPublicFeedUseCase(private val feedRepository: FeedRepository) {
    fun execute(limit: Int = 20): Flow<List<Post>> {
        return feedRepository.getPublicFeed(limit)
    }

    suspend fun loadMore(lastPostId: String, limit: Int = 20): List<Post> {
        return feedRepository.loadMoreFeed(lastPostId, limit)
    }

    fun getPostDetails(postId: String): Flow<Post?> {
        return feedRepository.getPostDetails(postId)
    }

    fun getUserPosts(userId: String): Flow<List<Post>> {
        return feedRepository.getUserPosts(userId)
    }
}
