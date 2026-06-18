package com.siffmember.info.data.local.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.siffmember.info.data.local.entity.PostMessage
import com.siffmember.info.ui.model.PostWithReplyCount

@Dao
interface PostMessageDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPostMessage(message: PostMessage)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAllPosts(posts: List<PostMessage>)

    @Query("SELECT postId FROM post_messages WHERE postId IN (:postIds)")
    fun getAlreadyInsertedPostIds(postIds: List<String>): List<String>

    @Query("SELECT * FROM post_messages ORDER BY timestamp ASC")
    fun getAllPostMessages(): LiveData<List<PostMessage>>

    @Query("SELECT * FROM post_messages ORDER BY timestamp ASC")
    fun getAllPostsMessages(): List<PostMessage>

    @Query("SELECT * FROM post_messages WHERE groupId = :groupId ORDER BY timestamp ASC")
    fun getPostMessagesByGroup(groupId: String): LiveData<List<PostMessage>>

    @Query("SELECT * FROM post_messages WHERE groupId = :groupId ORDER BY timestamp DESC LIMIT 1")
    fun getLatestPostMessageByGroup(groupId: String): LiveData<PostMessage>

    @Query("SELECT * FROM post_messages WHERE postId = :postId ORDER BY timestamp DESC LIMIT 1")
    fun getLatestPostMessageByPostId(postId: String): LiveData<PostMessage?>

    @Query("DELETE FROM post_messages WHERE postId = :postId")
    suspend fun deletePostMessagesByPostId(postId: String)

    @Query("DELETE FROM post_messages WHERE groupId = :groupId")
    suspend fun deletePostMessagesByGroupId(groupId: String)

    @Query("DELETE FROM post_messages WHERE postId IN (:ids)")
    suspend fun deletePostsByPostIds(ids: List<String>)

    @Query("DELETE FROM post_messages")
    suspend fun deleteAllPostMessages()

    @Query("""
    SELECT p.*, COUNT(r.id) AS replyCount 
    FROM post_messages p 
    LEFT JOIN replyPost_messages r ON p.postId = r.postId 
    WHERE p.groupId = :groupId 
    GROUP BY p.postId
""")
    fun getPostsWithReplyCountByGroup(groupId: String): LiveData<List<PostWithReplyCount>>

    @Query("SELECT * FROM post_messages WHERE postId = :postId LIMIT 1")
    suspend fun getPostById(postId: String): PostMessage?
}