package com.siffmember.info.socialFeeds.feed.data

import com.siffmember.info.socialFeeds.feed.model.Post
import kotlinx.coroutines.flow.Flow

interface FeedRepository {
    fun getPublicFeed(limit: Int = 20): Flow<List<Post>>
    suspend fun loadMoreFeed(lastPostId: String, limit: Int = 20): List<Post>
    fun getPostDetails(postId: String): Flow<Post?>
    fun getUserPosts(userId: String): Flow<List<Post>>
    suspend fun createPost(post: Post): Result<String>
    suspend fun editPost(postId: String, newContent: String, newPostType: String, imageUrls: List<String>, linkUrl: String?, linkTitle: String?, linkDescription: String?, linkThumbnail: String?): Result<Unit>
    suspend fun deletePost(postId: String): Result<Unit>
    suspend fun toggleLike(postId: String, userId: String, userName: String, userProfileImage: String): Result<Boolean>
    suspend fun toggleReshare(postId: String, userId: String, userName: String, userProfileImage: String): Result<Boolean>
}
