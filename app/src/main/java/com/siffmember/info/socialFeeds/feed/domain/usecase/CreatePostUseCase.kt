package com.siffmember.info.socialFeeds.feed.domain.usecase

import android.net.Uri
import com.siffmember.info.socialFeeds.feed.data.FeedRepository
import com.siffmember.info.socialFeeds.feed.data.StorageRepository
import com.siffmember.info.socialFeeds.feed.domain.LinkPreviewHelper
import com.siffmember.info.socialFeeds.feed.model.Post
import com.siffmember.info.socialFeeds.feed.model.PostType
import java.util.Date

class CreatePostUseCase(
    private val feedRepository: FeedRepository,
    private val storageRepository: StorageRepository
) {
    suspend fun execute(
        userId: String,
        userName: String,
        userProfileImage: String,
        content: String,
        imageUris: List<Uri>,
        inputLinkUrl: String?,
        sourceType: String = "",
        originalSharedUrl: String? = null,
        linkMetadata: Map<String, String>? = null,
        sharedFromApp: String? = null,
        onUploadProgress: (Float) -> Unit = {}
    ): Result<String> {
        try {
            // 1. Upload all local image URIs and get URLs
            val uploadedUrls = mutableListOf<String>()
            val totalImages = imageUris.size
            if (totalImages > 0) {
                imageUris.forEachIndexed { index, uri ->
                    onUploadProgress((index.toFloat() / totalImages.toFloat()) * 0.8f)
                    val uploadResult = storageRepository.uploadPostImage(uri)
                    if (uploadResult.isSuccess) {
                        uploadedUrls.add(uploadResult.getOrThrow())
                    } else {
                        return Result.failure(uploadResult.exceptionOrNull() ?: Exception("Image upload failed"))
                    }
                }
            }

            onUploadProgress(0.85f)

            // 2. Discover / parse Link Url
            var linkUrl: String? = inputLinkUrl
            if (linkUrl.isNullOrEmpty()) {
                linkUrl = LinkPreviewHelper.extractUrl(content)
            }

            // 3. Resolve metadata if Link Url is present
            var linkTitle: String? = null
            var linkDescription: String? = null
            var linkThumbnail: String? = null

            if (!linkUrl.isNullOrEmpty()) {
                onUploadProgress(0.9f)
                val metadata = LinkPreviewHelper.fetchMetadata(linkUrl)
                linkUrl = metadata.url
                linkTitle = metadata.title
                linkDescription = metadata.description
                linkThumbnail = metadata.thumbnailUrl
            }

            onUploadProgress(0.95f)

            // 4. Determine Post Type
            val hasImages = uploadedUrls.isNotEmpty()
            val hasLink = !linkUrl.isNullOrEmpty()
            val hasText = content.trim().isNotEmpty()

            val postType = when {
                hasText && hasImages && hasLink -> PostType.MIXED
                hasText && hasImages -> PostType.MIXED
                hasText && hasLink -> PostType.MIXED
                hasImages -> PostType.IMAGE
                hasLink -> PostType.LINK
                else -> PostType.TEXT
            }

            // 5. Construct Post item
            val post = Post(
                postId = "",
                userId = userId,
                userName = userName,
                userProfileImage = userProfileImage,
                content = content,
                postType = postType,
                imageUrls = uploadedUrls,
                linkUrl = linkUrl,
                linkTitle = linkTitle,
                linkDescription = linkDescription,
                linkThumbnail = linkThumbnail,
                createdAt = Date().time,
                updatedAt = Date().time,
                sourceType = sourceType.ifEmpty { postType.name },
                originalSharedUrl = originalSharedUrl ?: linkUrl,
                linkMetadata = linkMetadata ?: if (linkUrl != null) mapOf(
                    "title" to (linkTitle ?: ""),
                    "description" to (linkDescription ?: ""),
                    "thumbnailUrl" to (linkThumbnail ?: "")
                ) else null,
                sharedFromApp = sharedFromApp
            )

            val result = feedRepository.createPost(post)
            onUploadProgress(1.0f)
            return result
        } catch (e: Exception) {
            return Result.failure(e)
        }
    }
}
