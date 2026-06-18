package com.siffmember.info.socialFeeds.feed.model

import java.util.Date

enum class PostType {
    TEXT, IMAGE, LINK, MIXED
}

data class Post(
    val postId: String = "",
    val userId: String = "",
    val userName: String = "",
    val userProfileImage: String = "",
    val content: String = "",
    val postType: PostType = PostType.TEXT,
    val imageUrls: List<String> = emptyList(),
    val linkUrl: String? = null,
    val linkTitle: String? = null,
    val linkDescription: String? = null,
    val linkThumbnail: String? = null,
    val likesCount: Int = 0,
    val commentsCount: Int = 0,
    val resharesCount: Int = 0,
    val createdAt: Long = Date().time,
    val updatedAt: Long = Date().time,
    val originalPostId: String? = null, // Used for reshares
    val isLikedByCurrentUser: Boolean = false,
    val isResharedByCurrentUser: Boolean = false,
    val sourceType: String = "TEXT", // "TEXT", "IMAGE", "LINK", "MIXED", "SHARED"
    val originalSharedUrl: String? = null,
    val linkMetadata: Map<String, String>? = null,
    val sharedFromApp: String? = null
) {
    // Helper to map from Firestore document data
    companion object {
        fun fromMap(id: String, map: Map<String, Any?>, currentUserId: String? = null): Post {
            val imageUrlsList = when (val urls = map["imageUrls"]) {
                is List<*> -> urls.filterIsInstance<String>()
                else -> emptyList()
            }
            
            val linkMetaMap = when (val lm = map["linkMetadata"]) {
                is Map<*, *> -> lm.entries.associate { it.key.toString() to it.value.toString() }
                else -> null
            }

            val defaultsType = try {
                PostType.valueOf(map["postType"] as? String ?: "TEXT")
            } catch (e: Exception) {
                PostType.TEXT
            }

            return Post(
                postId = id,
                userId = map["userId"] as? String ?: "",
                userName = map["userName"] as? String ?: "Anonymous",
                userProfileImage = map["userProfileImage"] as? String ?: "",
                content = map["content"] as? String ?: "",
                postType = defaultsType,
                imageUrls = imageUrlsList,
                linkUrl = map["linkUrl"] as? String,
                linkTitle = map["linkTitle"] as? String,
                linkDescription = map["linkDescription"] as? String,
                linkThumbnail = map["linkThumbnail"] as? String,
                likesCount = (map["likesCount"] as? Number)?.toInt() ?: 0,
                commentsCount = (map["commentsCount"] as? Number)?.toInt() ?: 0,
                resharesCount = (map["resharesCount"] as? Number)?.toInt() ?: 0,
                createdAt = (map["createdAt"] as? Number)?.toLong() ?: Date().time,
                updatedAt = (map["updatedAt"] as? Number)?.toLong() ?: Date().time,
                originalPostId = map["originalPostId"] as? String,
                sourceType = map["sourceType"] as? String ?: defaultsType.name,
                originalSharedUrl = map["originalSharedUrl"] as? String,
                linkMetadata = linkMetaMap,
                sharedFromApp = map["sharedFromApp"] as? String
            )
        }
    }

    fun toMap(): Map<String, Any?> {
        return mapOf(
            "postId" to postId,
            "userId" to userId,
            "userName" to userName,
            "userProfileImage" to userProfileImage,
            "content" to content,
            "postType" to postType.name,
            "imageUrls" to imageUrls,
            "linkUrl" to linkUrl,
            "linkTitle" to linkTitle,
            "linkDescription" to linkDescription,
            "linkThumbnail" to linkThumbnail,
            "likesCount" to likesCount,
            "commentsCount" to commentsCount,
            "resharesCount" to resharesCount,
            "createdAt" to createdAt,
            "updatedAt" to updatedAt,
            "originalPostId" to originalPostId,
            "sourceType" to sourceType,
            "originalSharedUrl" to originalSharedUrl,
            "linkMetadata" to linkMetadata,
            "sharedFromApp" to sharedFromApp
        )
    }

    fun commentUrlForShare(): String {
        return "Check out ${userName}'s post: \"${content.take(60)}\" at: ${linkUrl ?: "https://ais-pre-6guj7germ4jged37ou4hus-725294490205.asia-east1.run.app"}"
    }
}
