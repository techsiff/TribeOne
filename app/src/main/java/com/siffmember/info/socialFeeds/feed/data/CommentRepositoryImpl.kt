package com.siffmember.info.socialFeeds.feed.data

import com.siffmember.info.socialFeeds.feed.model.Comment
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.update
import android.util.Log
import com.google.android.gms.tasks.Task
import com.siffmember.info.utils.AppConstants

import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class CommentRepositoryImpl : CommentRepository {

    private val firestore: FirebaseFirestore? by lazy {
        try {
            FirebaseFirestore.getInstance()
        } catch (e: Exception) {
            Log.w("CommentRepositoryImpl", "Firestore not initialized: ${e.message}")
            null
        }
    }

    private val localComments = MutableStateFlow<Map<String, List<Comment>>>(getMockCommentsMap())

    private fun getMockCommentsMap(): Map<String, List<Comment>> {
        val now = System.currentTimeMillis()
        return mapOf(
            "post_1" to listOf(
                Comment(
                    commentId = "comment_1_1",
                    postId = "post_1",
                    userId = "user_jane",
                    userName = "Jane Doe",
                    userProfileImage = "https://api.dicebear.com/7.x/adventurer/png?seed=Jane",
                    content = "Absolutely! Looking forward to seeing some cutting edge Jetpack Compose layouts! 🚀",
                    createdAt = now - 3600000
                ),
                Comment(
                    commentId = "comment_1_2",
                    postId = "post_1",
                    userId = "user_tech",
                    userName = "Tech Insider",
                    userProfileImage = "https://api.dicebear.com/7.x/adventurer/png?seed=Tech",
                    content = "Same here! Compose makes UI so intuitive and fast to design.",
                    parentCommentId = "comment_1_1",
                    createdAt = now - 1800000
                ),
                Comment(
                    commentId = "comment_2_1",
                    postId = "post_1",
                    userId = "user_dev",
                    userName = "Dev Pro",
                    userProfileImage = "https://api.dicebear.com/7.x/adventurer/png?seed=Dev",
                    content = "Is anyone utilizing Room database or Hilt for local state?",
                    createdAt = now - 3000000
                )
            ),
            "post_2" to listOf(
                Comment(
                    commentId = "comment_2_1",
                    postId = "post_2",
                    userId = "user_elon",
                    userName = "Elon Musk",
                    userProfileImage = "https://api.dicebear.com/7.x/adventurer/png?seed=Elon",
                    content = "Splendid photo! Looks extremely tranquil.",
                    createdAt = now - 3600000 * 4
                )
            )
        )
    }

    override fun getCommentsForPost(postId: String): Flow<List<Comment>> {
        val db = firestore
        if (db == null) {
            return callbackFlow {
                val flowSubscription = localComments.collect { map ->
                    trySend(map[postId] ?: emptyList())
                }
                awaitClose { /* Flow completed */ }
            }
        }

        return callbackFlow {
            val subscription = db.collection(AppConstants.TABLE_SOCIAL_POSTS).document(postId).collection("comments")
                .orderBy("createdAt", Query.Direction.ASCENDING)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.e("CommentRepositoryImpl", "Listen comments failed", error)
                        trySend(localComments.value[postId] ?: emptyList())
                        return@addSnapshotListener
                    }

                    if (snapshot != null) {
                        val commentsList = snapshot.documents.map { doc ->
                            Comment.fromMap(doc.id, postId, doc.data ?: emptyMap())
                        }
                        // Update local cache
                        localComments.update { current ->
                            current.toMutableMap().apply { this[postId] = commentsList }
                        }
                        trySend(commentsList)
                    }
                }
            awaitClose { subscription.remove() }
        }
    }

    override suspend fun addComment(comment: Comment): Result<String> {
        val db = firestore
        val newId = comment.commentId.ifEmpty { "comment_${System.currentTimeMillis()}" }
        val readyComment = comment.copy(commentId = newId)

        if (db == null) {
            localComments.update { current ->
                val list = (current[comment.postId] ?: emptyList()) + readyComment
                current.toMutableMap().apply { this[comment.postId] = list }
            }
            return Result.success(newId)
        }

        return try {
            // Write comment to subcollection
            val postRef = db.collection(AppConstants.TABLE_SOCIAL_POSTS).document(comment.postId)
            val commentRef = postRef.collection("comments").document(newId)

            db.runTransaction { transaction ->
                val postSnapshot = transaction.get(postRef)
                val commentsCount = (postSnapshot.get("commentsCount") as? Number)?.toLong() ?: 0
                transaction.set(commentRef, readyComment.toMap())
                transaction.update(postRef, "commentsCount", commentsCount + 1)
            }.getCompleted()

            Result.success(newId)
        } catch (e: Exception) {
            Log.e("CommentRepositoryImpl", "Add comment Firestore failed, fallback to local", e)
            localComments.update { current ->
                val list = (current[comment.postId] ?: emptyList()) + readyComment
                current.toMutableMap().apply { this[comment.postId] = list }
            }
            Result.success(newId)
        }
    }

    override suspend fun editComment(postId: String, commentId: String, newContent: String): Result<Unit> {
        val db = firestore
        if (db == null) {
            localComments.update { current ->
                val list = (current[postId] ?: emptyList()).map {
                    if (it.commentId == commentId) it.copy(content = newContent) else it
                }
                current.toMutableMap().apply { this[postId] = list }
            }
            return Result.success(Unit)
        }

        return try {
            db.collection(AppConstants.TABLE_SOCIAL_POSTS).document(postId).collection("comments").document(commentId)
                .update("content", newContent).getCompleted()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("CommentRepositoryImpl", "Edit comment failed, fallback to local", e)
            localComments.update { current ->
                val list = (current[postId] ?: emptyList()).map {
                    if (it.commentId == commentId) it.copy(content = newContent) else it
                }
                current.toMutableMap().apply { this[postId] = list }
            }
            Result.success(Unit)
        }
    }

    override suspend fun deleteComment(postId: String, commentId: String): Result<Unit> {
        val db = firestore
        if (db == null) {
            localComments.update { current ->
                val list = (current[postId] ?: emptyList()).filter { it.commentId != commentId }
                current.toMutableMap().apply { this[postId] = list }
            }
            return Result.success(Unit)
        }

        return try {
            val postRef = db.collection(AppConstants.TABLE_SOCIAL_POSTS).document(postId)
            val commentRef = postRef.collection("comments").document(commentId)

            db.runTransaction { transaction ->
                val postSnapshot = transaction.get(postRef)
                val commentsCount = (postSnapshot.get("commentsCount") as? Number)?.toLong() ?: 0
                transaction.delete(commentRef)
                transaction.update(postRef, "commentsCount", maxOf(0L, commentsCount - 1))
            }.getCompleted()

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("CommentRepositoryImpl", "Delete comment failed, fallback to local", e)
            localComments.update { current ->
                val list = (current[postId] ?: emptyList()).filter { it.commentId != commentId }
                current.toMutableMap().apply { this[postId] = list }
            }
            Result.success(Unit)
        }
    }

    private suspend fun <T> Task<T>.getCompleted(): T = suspendCancellableCoroutine { cont ->
        addOnCompleteListener { task ->
            if (task.isSuccessful) {
                cont.resume(task.result)
            } else {
                cont.resumeWithException(task.exception ?: Exception("Task failed"))
            }
        }
    }
}
