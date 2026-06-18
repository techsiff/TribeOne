package com.siffmember.info.socialFeeds.feed.domain.usecase

import com.siffmember.info.socialFeeds.feed.data.FeedRepository

class ManagePostUseCase(private val feedRepository: FeedRepository) {
    suspend fun edit(
        postId: String,
        newContent: String,
        newPostType: String,
        imageUrls: List<String>,
        linkUrl: String?,
        linkTitle: String?,
        linkDescription: String?,
        linkThumbnail: String?
    ): Result<Unit> {
        return feedRepository.editPost(postId, newContent, newPostType, imageUrls, linkUrl, linkTitle, linkDescription, linkThumbnail)
    }

    suspend fun delete(postId: String): Result<Unit> {
        return feedRepository.deletePost(postId)
    }
}
