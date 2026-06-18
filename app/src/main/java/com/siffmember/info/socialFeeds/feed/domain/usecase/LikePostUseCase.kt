package com.siffmember.info.socialFeeds.feed.domain.usecase

import com.siffmember.info.socialFeeds.feed.data.FeedRepository

class LikePostUseCase(private val feedRepository: FeedRepository) {
    suspend fun execute(
        postId: String,
        userId: String,
        userName: String,
        userProfileImage: String
    ): Result<Boolean> {
        return feedRepository.toggleLike(postId, userId, userName, userProfileImage)
    }
}
