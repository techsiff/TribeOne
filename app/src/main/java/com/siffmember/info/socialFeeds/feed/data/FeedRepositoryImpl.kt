package com.siffmember.info.socialFeeds.feed.data

import com.siffmember.info.socialFeeds.feed.model.Post
import com.siffmember.info.socialFeeds.feed.model.PostType
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.update
import java.util.Date
import android.util.Log
import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.DocumentReference
import com.siffmember.info.utils.AppConstants

import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class FeedRepositoryImpl : FeedRepository {

    private val firestore: FirebaseFirestore? by lazy {
        try {
            FirebaseFirestore.getInstance()
        } catch (e: Exception) {
            Log.w("FeedRepositoryImpl", "Firestore not initialized: ${e.message}")
            null
        }
    }

    // High performance local fallback cache for instant reactivity and mock-fallback execution
    private val localPosts = MutableStateFlow<List<Post>>(getMockPosts())

    private fun getMockPosts(): List<Post> {
        val now = Date().time
        return listOf(
            Post(
                postId = "post_1",
                userId = "user_elon",
                userName = "Elon Musk",
                userProfileImage = "https://api.dicebear.com/7.x/adventurer/png?seed=Elon",
                content = "Excited about the future of tech and space! What are you building today on the Android platform? 📱🚀",
                postType = PostType.TEXT,
                likesCount = 1420,
                commentsCount = 42,
                resharesCount = 12,
                createdAt = now - 3600000 * 2,
                updatedAt = now - 3600000 * 2
            ),
            Post(
                postId = "post_2",
                userId = "user_jane",
                userName = "Jane Doe",
                userProfileImage = "https://api.dicebear.com/7.x/adventurer/png?seed=Jane",
                content = "Just captured this beautiful sunrise! Absolutely majestic start to the weekend.",
                postType = PostType.IMAGE,
                imageUrls = listOf("https://images.unsplash.com/photo-1470252649378-9c29740c9fa8?q=80&w=720"),
                likesCount = 88,
                commentsCount = 3,
                resharesCount = 1,
                createdAt = now - 3600000 * 5,
                updatedAt = now - 3600000 * 5
            ),
            Post(
                postId = "post_3",
                userId = "user_tech",
                userName = "Tech Insider",
                userProfileImage = "https://api.dicebear.com/7.x/adventurer/png?seed=Tech",
                content = "Check out the official Android Material Design 3 guidelines for building beautiful responsive applications! High fidelity, dynamic color themes, and adaptive layouts.",
                postType = PostType.LINK,
                linkUrl = "https://m3.material.io",
                linkTitle = "Material Design 3 Guidelines",
                linkDescription = "Guidelines, tools, and templates of the latest design system from Google.",
                linkThumbnail = "https://images.unsplash.com/photo-1507238691740-187a5b1d37b8?q=80&w=720",
                likesCount = 56,
                commentsCount = 12,
                resharesCount = 5,
                createdAt = now - 3600000 * 24,
                updatedAt = now - 3600000 * 24
            )
        )
    }

    override fun getPublicFeed(limit: Int): Flow<List<Post>> {
        val db = firestore
        if (db == null) {
            return localPosts
        }

        return callbackFlow {
            val subscription = db.collection(AppConstants.TABLE_SOCIAL_POSTS)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(limit.toLong())
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.e("FeedRepositoryImpl", "Listen failed", error)
                        // Safe fallback to local cache
                        val local = localPosts.value
                        trySend(local)
                        return@addSnapshotListener
                    }

                    if (snapshot != null) {
                        val posts = snapshot.documents.map { doc ->
                            Post.fromMap(doc.id, doc.data ?: emptyMap())
                        }
                        // Refresh local cache for seamless transitions
                        localPosts.value = posts
                        trySend(posts)
                    }
                }

            awaitClose { subscription.remove() }
        }
    }

    override suspend fun loadMoreFeed(lastPostId: String, limit: Int): List<Post> {
        val db = firestore ?: return emptyList()
        return try {
            val lastDocSnapshot = db.collection(AppConstants.TABLE_SOCIAL_POSTS).document(lastPostId).get().getCompleted()
            val querySnapshot = db.collection(AppConstants.TABLE_SOCIAL_POSTS)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .startAfter(lastDocSnapshot)
                .limit(limit.toLong())
                .get().getCompleted()

            querySnapshot.documents.map { doc ->
                Post.fromMap(doc.id, doc.data ?: emptyMap())
            }
        } catch (e: Exception) {
            Log.e("FeedRepositoryImpl", "Error loading more posts, falling back", e)
            emptyList()
        }
    }

    // Helper to bypass kotlinx.coroutines dependency version await issues
    private suspend fun <T> Task<T>.getCompleted(): T = suspendCancellableCoroutine { cont ->
        addOnCompleteListener { task ->
            if (task.isSuccessful) {
                cont.resume(task.result)
            } else {
                cont.resumeWithException(task.exception ?: Exception("Task failed"))
            }
        }
    }

    override fun getPostDetails(postId: String): Flow<Post?> {
        val db = firestore
        if (db == null) {
            return callbackFlow {
                val flowSubscription = localPosts.collect { posts ->
                    trySend(posts.find { it.postId == postId })
                }
                awaitClose { /* Flow ends */ }
            }
        }

        return callbackFlow {
            val subscription = db.collection(AppConstants.TABLE_SOCIAL_POSTS).document(postId)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.e("FeedRepositoryImpl", "Listen post details failed", error)
                        trySend(localPosts.value.find { it.postId == postId })
                        return@addSnapshotListener
                    }

                    if (snapshot != null && snapshot.exists()) {
                        val post = Post.fromMap(snapshot.id, snapshot.data ?: emptyMap())
                        trySend(post)
                    } else {
                        trySend(null)
                    }
                }
            awaitClose { subscription.remove() }
        }
    }

    override fun getUserPosts(userId: String): Flow<List<Post>> {
        val db = firestore
        if (db == null) {
            return callbackFlow {
                val subscription = localPosts.collect { posts ->
                    trySend(posts.filter { it.userId == userId })
                }
                awaitClose { }
            }
        }

        return callbackFlow {
            val subscription = db.collection(AppConstants.TABLE_SOCIAL_POSTS)
                .whereEqualTo("userId", userId)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.e("FeedRepositoryImpl", "Listen user posts failed", error)
                        trySend(localPosts.value.filter { it.userId == userId })
                        return@addSnapshotListener
                    }

                    if (snapshot != null) {
                        val posts = snapshot.documents.map { doc ->
                            Post.fromMap(doc.id, doc.data ?: emptyMap())
                        }
                        trySend(posts)
                    }
                }
            awaitClose { subscription.remove() }
        }
    }

    override suspend fun createPost(post: Post): Result<String> {
        val db = firestore
        val newId = post.postId.ifEmpty { "post_${System.currentTimeMillis()}" }
        val readyPost = post.copy(postId = newId)

        if (db == null) {
            // Local persistence
            localPosts.update { current ->
                listOf(readyPost) + current
            }
            return Result.success(newId)
        }

        return try {
            db.collection(AppConstants.TABLE_SOCIAL_POSTS).document(newId).set(readyPost.toMap()).getCompleted()
            Result.success(newId)
        } catch (e: Exception) {
            Log.e("FeedRepositoryImpl", "Create post firestore failed, adding locally", e)
            localPosts.update { current ->
                listOf(readyPost) + current
            }
            Result.success(newId)
        }
    }

    override suspend fun editPost(
        postId: String,
        newContent: String,
        newPostType: String,
        imageUrls: List<String>,
        linkUrl: String?,
        linkTitle: String?,
        linkDescription: String?,
        linkThumbnail: String?
    ): Result<Unit> {
        val db = firestore
        if (db == null) {
            localPosts.update { list ->
                list.map {
                    if (it.postId == postId) {
                        it.copy(
                            content = newContent,
                            postType = PostType.valueOf(newPostType),
                            imageUrls = imageUrls,
                            linkUrl = linkUrl,
                            linkTitle = linkTitle,
                            linkDescription = linkDescription,
                            linkThumbnail = linkThumbnail,
                            updatedAt = System.currentTimeMillis()
                        )
                    } else it
                }
            }
            return Result.success(Unit)
        }

        return try {
            val updates = mapOf(
                "content" to newContent,
                "postType" to newPostType,
                "imageUrls" to imageUrls,
                "linkUrl" to linkUrl,
                "linkTitle" to linkTitle,
                "linkDescription" to linkDescription,
                "linkThumbnail" to linkThumbnail,
                "updatedAt" to System.currentTimeMillis()
            )
            db.collection(AppConstants.TABLE_SOCIAL_POSTS).document(postId).update(updates).getCompleted()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("FeedRepositoryImpl", "Edit post failed, updating locally", e)
            localPosts.update { list ->
                list.map {
                    if (it.postId == postId) {
                        it.copy(
                            content = newContent,
                            postType = PostType.valueOf(newPostType),
                            imageUrls = imageUrls,
                            linkUrl = linkUrl,
                            linkTitle = linkTitle,
                            linkDescription = linkDescription,
                            linkThumbnail = linkThumbnail,
                            updatedAt = System.currentTimeMillis()
                        )
                    } else it
                }
            }
            Result.success(Unit)
        }
    }

    /*override suspend fun deletePost(postId: String): Result<Unit> {
        val db = firestore
        if (db == null) {
            localPosts.update { list ->
                list.filter { it.postId != postId }
            }
            return Result.success(Unit)
        }

        return try {
            db.collection(AppConstants.TABLE_SOCIAL_POSTS).document(postId).delete().getCompleted()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("FeedRepositoryImpl", "Delete post failed", e)
            localPosts.update { list ->
                list.filter { it.postId != postId }
            }
            Result.success(Unit)
        }
    }*/

    private suspend fun deleteSubCollection(
        postRef: DocumentReference,
        name: String
    ) {
        val docs = postRef.collection(name).get().await()

        docs.documents.forEach {
            it.reference.delete().await()
        }
    }

    override suspend fun deletePost(postId: String): Result<Unit> {
        val db = firestore ?: return Result.success(Unit)

        return try {
            val postRef = db.collection(AppConstants.TABLE_SOCIAL_POSTS)
                .document(postId)

            deleteSubCollection(postRef, "likes")
            deleteSubCollection(postRef, "comments")
            deleteSubCollection(postRef, "reshares")

            postRef.delete().await()

            localPosts.update { list ->
                list.filter { it.postId != postId }
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("FeedRepositoryImpl", "Delete post failed", e)
            Result.failure(e)
        }
    }

    override suspend fun toggleLike(
        postId: String,
        userId: String,
        userName: String,
        userProfileImage: String
    ): Result<Boolean> {
        val db = firestore
        if (db == null) {
            var finalLiked = false
            localPosts.update { list ->
                list.map { post ->
                    if (post.postId == postId) {
                        val isLiked = post.isLikedByCurrentUser
                        finalLiked = !isLiked
                        post.copy(
                            isLikedByCurrentUser = !isLiked,
                            likesCount = if (isLiked) post.likesCount - 1 else post.likesCount + 1
                        )
                    } else post
                }
            }
            return Result.success(finalLiked)
        }

        return try {
            val likeDocRef = db.collection(AppConstants.TABLE_SOCIAL_POSTS).document(postId).collection("likes").document(userId)
            val exists = likeDocRef.get().getCompleted().exists()

            val postDocRef = db.collection(AppConstants.TABLE_SOCIAL_POSTS).document(postId)

            if (exists) {
                // Unlike atomically
                db.runTransaction { transaction ->
                    val snapshot = transaction.get(postDocRef)
                    val likesCount = (snapshot.get("likesCount") as? Number)?.toLong() ?: 0
                    transaction.delete(likeDocRef)
                    transaction.update(postDocRef, "likesCount", maxOf(0L, likesCount - 1))
                }.getCompleted()
                Result.success(false)
            } else {
                // Like atomically
                db.runTransaction { transaction ->
                    val snapshot = transaction.get(postDocRef)
                    val likesCount = (snapshot.get("likesCount") as? Number)?.toLong() ?: 0
                    transaction.set(likeDocRef, mapOf("userId" to userId, "createdAt" to System.currentTimeMillis()))
                    transaction.update(postDocRef, "likesCount", likesCount + 1)
                }.getCompleted()
                Result.success(true)
            }
        } catch (e: Exception) {
            Log.e("FeedRepositoryImpl", "Toggle like Firestore failed, toggling locally", e)
            var finalLiked = false
            localPosts.update { list ->
                list.map { post ->
                    if (post.postId == postId) {
                        val isLiked = post.isLikedByCurrentUser
                        finalLiked = !isLiked
                        post.copy(
                            isLikedByCurrentUser = !isLiked,
                            likesCount = if (isLiked) post.likesCount - 1 else post.likesCount + 1
                        )
                    } else post
                }
            }
            Result.success(finalLiked)
        }
    }

    override suspend fun toggleReshare(
        postId: String,
        userId: String,
        userName: String,
        userProfileImage: String
    ): Result<Boolean> {
        val db = firestore
        if (db == null) {
            var finalReshared = false
            localPosts.update { list ->
                var updatedList = list.map { post ->
                    if (post.postId == postId) {
                        val isReshared = post.isResharedByCurrentUser
                        finalReshared = !isReshared
                        post.copy(
                            isResharedByCurrentUser = !isReshared,
                            resharesCount = if (isReshared) post.resharesCount - 1 else post.resharesCount + 1
                        )
                    } else post
                }

                if (finalReshared) {
                    val originalPost = updatedList.find { it.postId == postId }
                    if (originalPost != null) {
                        val resharePost = Post(
                            postId = "reshare_${System.currentTimeMillis()}",
                            userId = userId,
                            userName = userName,
                            userProfileImage = userProfileImage,
                            content = "Re-shared: ${originalPost.content}",
                            postType = originalPost.postType,
                            imageUrls = originalPost.imageUrls,
                            linkUrl = originalPost.linkUrl,
                            linkTitle = originalPost.linkTitle,
                            linkDescription = originalPost.linkDescription,
                            linkThumbnail = originalPost.linkThumbnail,
                            originalPostId = postId,
                            createdAt = System.currentTimeMillis()
                        )
                        updatedList = listOf(resharePost) + updatedList
                    }
                } else {
                    // Remove the reshare post
                    updatedList = updatedList.filterNot { it.originalPostId == postId && it.userId == userId }
                }
                updatedList
            }
            return Result.success(finalReshared)
        }

        return try {
            val reshareDocRef = db.collection(AppConstants.TABLE_SOCIAL_POSTS).document(postId).collection("reshares").document(userId)
            val exists = reshareDocRef.get().getCompleted().exists()

            val postDocRef = db.collection(AppConstants.TABLE_SOCIAL_POSTS).document(postId)

            if (exists) {
                db.runTransaction { transaction ->
                    val snapshot = transaction.get(postDocRef)
                    val resharesCount = (snapshot.get("resharesCount") as? Number)?.toLong() ?: 0
                    transaction.delete(reshareDocRef)
                    transaction.update(postDocRef, "resharesCount", maxOf(0L, resharesCount - 1))
                }.getCompleted()

                // Find and delete the created reshared post copy
                val resharedPostQuery = db.collection(AppConstants.TABLE_SOCIAL_POSTS)
                    .whereEqualTo("userId", userId)
                    .whereEqualTo("originalPostId", postId)
                    .get().getCompleted()

                for (doc in resharedPostQuery.documents) {
                    db.collection(AppConstants.TABLE_SOCIAL_POSTS).document(doc.id).delete().getCompleted()
                }

                Result.success(false)
            } else {
                db.runTransaction { transaction ->
                    val snapshot = transaction.get(postDocRef)
                    val resharesCount = (snapshot.get("resharesCount") as? Number)?.toLong() ?: 0
                    transaction.set(reshareDocRef, mapOf("userId" to userId, "createdAt" to System.currentTimeMillis()))
                    transaction.update(postDocRef, "resharesCount", resharesCount + 1)
                }.getCompleted()

                // Create a real copy post with originalPostId link
                val originalPostSnap = postDocRef.get().getCompleted()
                val originalPost = Post.fromMap(originalPostSnap.id, originalPostSnap.data ?: emptyMap())
                val resharePost = Post(
                    postId = "reshare_${System.currentTimeMillis()}",
                    userId = userId,
                    userName = userName,
                    userProfileImage = userProfileImage,
                    content = "Re-shared: ${originalPost.content}",
                    postType = originalPost.postType,
                    imageUrls = originalPost.imageUrls,
                    linkUrl = originalPost.linkUrl,
                    linkTitle = originalPost.linkTitle,
                    linkDescription = originalPost.linkDescription,
                    linkThumbnail = originalPost.linkThumbnail,
                    originalPostId = postId,
                    createdAt = System.currentTimeMillis()
                )

                db.collection(AppConstants.TABLE_SOCIAL_POSTS).document(resharePost.postId).set(resharePost.toMap()).getCompleted()

                Result.success(true)
            }
        } catch (e: Exception) {
            Log.e("FeedRepositoryImpl", "Toggle reshare failed, doing locally", e)
            var finalReshared = false
            localPosts.update { list ->
                var updatedList = list.map { post ->
                    if (post.postId == postId) {
                        val isReshared = post.isResharedByCurrentUser
                        finalReshared = !isReshared
                        post.copy(
                            isResharedByCurrentUser = !isReshared,
                            resharesCount = if (isReshared) post.resharesCount - 1 else post.resharesCount + 1
                        )
                    } else post
                }

                if (finalReshared) {
                    val originalPost = updatedList.find { it.postId == postId }
                    if (originalPost != null) {
                        val resharePost = Post(
                            postId = "reshare_${System.currentTimeMillis()}",
                            userId = userId,
                            userName = userName,
                            userProfileImage = userProfileImage,
                            content = "Re-shared: ${originalPost.content}",
                            postType = originalPost.postType,
                            imageUrls = originalPost.imageUrls,
                            linkUrl = originalPost.linkUrl,
                            linkTitle = originalPost.linkTitle,
                            linkDescription = originalPost.linkDescription,
                            linkThumbnail = originalPost.linkThumbnail,
                            originalPostId = postId,
                            createdAt = System.currentTimeMillis()
                        )
                        updatedList = listOf(resharePost) + updatedList
                    }
                } else {
                    updatedList = updatedList.filterNot { it.originalPostId == postId && it.userId == userId }
                }
                updatedList
            }
            Result.success(finalReshared)
        }
    }
}
