package com.siffmember.info.socialFeeds.feed.data

import com.siffmember.info.socialFeeds.feed.domain.usecase.GetPublicFeedUseCase
import com.siffmember.info.socialFeeds.feed.domain.usecase.CreatePostUseCase
import com.siffmember.info.socialFeeds.feed.domain.usecase.LikePostUseCase
import com.siffmember.info.socialFeeds.feed.domain.usecase.ResharePostUseCase
import com.siffmember.info.socialFeeds.feed.domain.usecase.ManagePostUseCase
import com.siffmember.info.socialFeeds.feed.domain.usecase.CommentUseCase

object Inject {
    val feedRepository: FeedRepository by lazy { FeedRepositoryImpl() }
    val commentRepository: CommentRepository by lazy { CommentRepositoryImpl() }
    val storageRepository: StorageRepository by lazy { StorageRepositoryImpl() }

    val getPublicFeedUseCase: GetPublicFeedUseCase by lazy {
        GetPublicFeedUseCase(feedRepository)
    }

    val createPostUseCase: CreatePostUseCase by lazy {
        CreatePostUseCase(feedRepository, storageRepository)
    }

    val likePostUseCase: LikePostUseCase by lazy {
        LikePostUseCase(feedRepository)
    }

    val resharePostUseCase: ResharePostUseCase by lazy {
        ResharePostUseCase(feedRepository)
    }

    val managePostUseCase: ManagePostUseCase by lazy {
        ManagePostUseCase(feedRepository)
    }

    val commentUseCase: CommentUseCase by lazy {
        CommentUseCase(commentRepository)
    }
}
