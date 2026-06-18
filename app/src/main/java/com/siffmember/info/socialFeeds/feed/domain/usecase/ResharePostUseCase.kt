package com.siffmember.info.socialFeeds.feed.domain.usecase

import com.siffmember.info.socialFeeds.feed.data.FeedRepository

class ResharePostUseCase(private val feedRepository: FeedRepository) {
    suspend fun execute(
        postId: String,
        userId: String,
        userName: String,
        userProfileImage: String
    ): Result<Boolean> {
        return feedRepository.toggleReshare(postId, userId, userName, userProfileImage)
    }
}
