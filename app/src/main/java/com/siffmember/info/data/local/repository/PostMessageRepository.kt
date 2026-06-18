package com.siffmember.info.data.local.repository

import android.content.Context
import androidx.lifecycle.LiveData
import com.siffmember.info.data.local.database.AppDatabase
import com.siffmember.info.data.local.entity.DeleteMessages
import com.siffmember.info.data.local.entity.PostMessage
import com.siffmember.info.data.local.entity.ReplyPostMessage
import com.siffmember.info.ui.model.PostWithReplyCount
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class PostMessageRepository(context: Context) {

    //Post messages
    private val dbPost: AppDatabase = AppDatabase.getDatabase(context)
    private val postsDao = dbPost.postDao()
    private val postsReplyDao = dbPost.replyDao()
    private val deletedMessageDao = dbPost.deletedMessageDao()

    fun insertPostMessage(message: PostMessage) {
        CoroutineScope(Dispatchers.IO).launch {
            postsDao.insertPostMessage(message)
        }
    }

    fun insertAllPosts(posts: List<PostMessage>) {
        CoroutineScope(Dispatchers.IO).launch {
            postsDao.insertAllPosts(posts)
        }
    }

    fun getAlreadyInsertedPostIds(postIds: List<String>): List<String> = postsDao.getAlreadyInsertedPostIds(postIds)

    //fun getAllPostMessages(): LiveData<List<PostMessage>> = postsDao.getAllPostMessages()

    //fun getGroupAllPostMessage(groupId: String): LiveData<List<PostMessage>> = postsDao.getPostMessagesByGroup(groupId)

    fun getGroupLastPostMessage(groupId: String): LiveData<PostMessage> = postsDao.getLatestPostMessageByGroup(groupId)

    fun getLatestPostMessageByPostId(postId: String): LiveData<PostMessage?> = postsDao.getLatestPostMessageByPostId(postId)

    suspend fun deletePostMessagesByPostId(postId: String) {
        postsDao.deletePostMessagesByPostId(postId)
    }

    suspend fun deletePostMessagesByGroupId(groupId: String) {
        postsDao.deletePostMessagesByGroupId(groupId)
    }

    suspend fun deletePostsByPostIds(ids: List<String>) {
        postsDao.deletePostsByPostIds(ids)
    }

    suspend fun deleteAllPostMessages() {
        postsDao.deleteAllPostMessages()
    }

    fun getPostsWithReplyCountByGroup(groupId: String): LiveData<List<PostWithReplyCount>> {
        return postsDao.getPostsWithReplyCountByGroup(groupId)
    }

    /*suspend fun getPostById(postId: String): PostMessage? {
        return postsDao.getPostById(postId)
    }*/

    //Post reply messages
    fun insertPostReplyMessage(message: ReplyPostMessage) {
        CoroutineScope(Dispatchers.IO).launch {
            postsReplyDao.insertReplyPostMessage(message)
        }
    }

    fun insertAllReplies(replies: List<ReplyPostMessage>) {
        CoroutineScope(Dispatchers.IO).launch {
            postsReplyDao.insertAllReplies(replies)
        }
    }

    fun getAlreadyInsertedCommentsIds(commentIds: List<String>): List<String> = postsReplyDao.getAlreadyInsertedCommentsIds(commentIds)


    // fun getAllPostReplyMessages(): LiveData<List<ReplyPostMessage>> = postsReplyDao.getAllReplyPostMessages()

    fun getGroupAllPostReplyMessage(postId: String): LiveData<List<ReplyPostMessage>> = postsReplyDao.getReplyPostMessagesByPost(postId)

   // fun getGroupLastPostReplyMessage(postId: String): LiveData<ReplyPostMessage> = postsReplyDao.getLatestReplyPostMessageByPost(postId)

    suspend fun deleteReplyPostMessagesByID(id: String) {
        postsReplyDao.deleteReplyPostMessagesByID(id)
    }

    suspend fun deleteReplyPostMessagesByGroupID(groupId: String) {
        postsReplyDao.deleteReplyPostMessagesByGroupID(groupId)
    }

    suspend fun deleteReplyPostMessagesByPostID(postId: String) {
        postsReplyDao.deleteReplyPostMessagesByPostID(postId)
    }

    suspend fun deletePostsReplyByPostIds(ids: List<String>) {
        postsReplyDao.deletePostsReplyByPostIds(ids)
    }

    suspend fun deleteReplyPostMessagesByCommentIDs(ids: List<String>) {
        postsReplyDao.deleteReplyPostMessagesByCommentIDs(ids)
    }

    suspend fun deleteAllReplyPostMessages() {
        postsReplyDao.deleteAllReplyPostMessages()
    }

    /*suspend fun getReplayPostById(commentId: String): ReplyPostMessage? {
        return postsReplyDao.getReplayPostById(commentId)
    }*/

    //Deleted messages
    fun insertDeletedMessage(message: List<DeleteMessages>) {
        CoroutineScope(Dispatchers.IO).launch {
            deletedMessageDao.insertDeletedMessage(message)
        }
    }

    fun getAlreadyInsertedDeletedIds(postIds: List<String>): List<String> = deletedMessageDao.getAlreadyInsertedDeletedIds(postIds)

    /*suspend fun deleteDeletedMessageByPostID(messageId: String) {
        deletedMessageDao.deleteDeletedMessageByPostID(messageId)
    }*/

}