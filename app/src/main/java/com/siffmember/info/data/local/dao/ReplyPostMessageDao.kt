package com.siffmember.info.data.local.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.siffmember.info.data.local.entity.ReplyPostMessage

@Dao
interface ReplyPostMessageDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReplyPostMessage(message: ReplyPostMessage)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAllReplies(replies: List<ReplyPostMessage>)

    @Query("SELECT commentId FROM replyPost_messages WHERE commentId IN (:commentsIds)")
    fun getAlreadyInsertedCommentsIds(commentsIds: List<String>): List<String>

    @Query("SELECT * FROM replyPost_messages ORDER BY timestamp ASC")
    fun getAllReplyPostMessages(): LiveData<List<ReplyPostMessage>>

    @Query("SELECT * FROM replyPost_messages ORDER BY timestamp ASC")
    fun getAllReplyPostsMessages(): List<ReplyPostMessage>

    @Query("SELECT * FROM replyPost_messages WHERE postId = :postId ORDER BY timestamp ASC")
    fun getReplyPostMessagesByPost(postId: String): LiveData<List<ReplyPostMessage>>

    @Query("SELECT * FROM replyPost_messages WHERE postId = :postId ORDER BY timestamp DESC LIMIT 1")
    fun getLatestReplyPostMessageByPost(postId: String): LiveData<ReplyPostMessage>

    @Query("DELETE FROM replyPost_messages WHERE id = :id")
    suspend fun deleteReplyPostMessagesByID(id: String)

    @Query("DELETE FROM replyPost_messages WHERE postId = :postId")
    suspend fun deleteReplyPostMessagesByPostID(postId: String)

    @Query("DELETE FROM replyPost_messages WHERE commentId = :commentId")
    suspend fun deleteReplyPostMessagesByCommentID(commentId: String)

    @Query("DELETE FROM replyPost_messages WHERE groupId = :groupId")
    suspend fun deleteReplyPostMessagesByGroupID(groupId: String)

    @Query("DELETE FROM replyPost_messages WHERE postId IN (:ids)")
    suspend fun deletePostsReplyByPostIds(ids: List<String>)

    @Query("DELETE FROM replyPost_messages WHERE commentId IN (:ids)")
    suspend fun deleteReplyPostMessagesByCommentIDs(ids: List<String>)

    @Query("DELETE FROM replyPost_messages")
    suspend fun deleteAllReplyPostMessages()

    @Query("SELECT * FROM replyPost_messages WHERE commentId = :commentId LIMIT 1")
    suspend fun getReplayPostById(commentId: String): ReplyPostMessage?
}