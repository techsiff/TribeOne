package com.siffmember.info.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import com.siffmember.info.data.local.entity.DeleteMessages
import com.siffmember.info.data.local.entity.PostMessage
import com.siffmember.info.data.local.entity.ReplyPostMessage
import com.siffmember.info.data.local.repository.PostMessageRepository
import com.siffmember.info.ui.model.PostWithReplyCount
import kotlinx.coroutines.launch

class PostsMessageViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: PostMessageRepository = PostMessageRepository(application)
    //Post messages
    //val postMessages: LiveData<List<PostMessage>> = repository.getAllPostMessages()

    fun insertPostMessage(postId: String, postTitle: String, groupName: String, groupId: String, content: String, timeStamp: String, userName: String, userId: String) {
        val message = PostMessage(postId, postTitle, content, timeStamp, groupName, groupId, userName, userId)
        repository.insertPostMessage(message)
    }

    fun insertAllPosts(posts: List<PostMessage>) {
        repository.insertAllPosts(posts)
    }

    fun getAlreadyInsertedPostIds(postIds: List<String>): List<String> {
        return repository.getAlreadyInsertedPostIds(postIds)
    }
   /* fun fetchAllPostMessages(): LiveData<List<PostMessage>> = postMessages

    fun getGroupPostMessages(groupId: String): LiveData<List<PostMessage>> {
        return repository.getGroupAllPostMessage(groupId)
    }*/

    fun getGroupLastPostMessages(groupId: String): LiveData<PostMessage> {
        return repository.getGroupLastPostMessage(groupId)
    }

    fun getLatestPostMessageByPostId(postId: String): LiveData<PostMessage?> {
        return repository.getLatestPostMessageByPostId(postId)
    }

    fun deletePostMessagesByPostId(postId: String) {
        viewModelScope.launch {
            repository.deletePostMessagesByPostId(postId)
        }
    }

    fun deletePostMessagesByGroupId(groupId: String) {
        viewModelScope.launch {
            repository.deletePostMessagesByGroupId(groupId)
        }
    }

    fun deletePostsByPostIds(ids: List<String>) {
        viewModelScope.launch {
            repository.deletePostsByPostIds(ids)
        }
    }

    fun deleteAllPostMessages() {
        viewModelScope.launch {
            repository.deleteAllPostMessages()
        }
    }

    fun getGroupPostsWithReplyCount(groupId: String): LiveData<List<PostWithReplyCount>> {
        return repository.getPostsWithReplyCountByGroup(groupId)
    }

    /*suspend fun getPostById(postId: String): PostMessage? {
        return repository.getPostById(postId)
    }*/

    //Reply Post messages
    //val postReplyMessages: LiveData<List<ReplyPostMessage>> = repository.getAllPostReplyMessages()

    fun sendPostReplyMessage(commentId: String, postId: String, postTitle: String, content: String, timeStamp: String, groupId: String, userName: String, userId: String) {
        val message = ReplyPostMessage(0, commentId, postId, postTitle, content, timeStamp, groupId, userName, userId)
        repository.insertPostReplyMessage(message)
    }

    fun insertAllReplies(replies: List<ReplyPostMessage>) {
        repository.insertAllReplies(replies)
    }

    fun getAlreadyInsertedCommentsIds(commentIds: List<String>): List<String> {
        return repository.getAlreadyInsertedCommentsIds(commentIds)
    }

 //   fun fetchAllPostReplyMessages(): LiveData<List<ReplyPostMessage>> = postReplyMessages

    fun getGroupPostReplyMessages(postId: String): LiveData<List<ReplyPostMessage>> {
        return repository.getGroupAllPostReplyMessage(postId)
    }

  /*  fun getGroupLastPostReplyMessages(postId: String): LiveData<ReplyPostMessage> {
        return repository.getGroupLastPostReplyMessage(postId)
    }*/

    fun deleteReplyPostMessagesByID(id: String) {
        viewModelScope.launch {
            repository.deleteReplyPostMessagesByID(id)
        }
    }

    fun deleteReplyPostMessagesByGroupID(groupId: String) {
        viewModelScope.launch {
            repository.deleteReplyPostMessagesByGroupID(groupId)
        }
    }

    fun deleteReplyPostMessagesByPostID(postId: String) {
        viewModelScope.launch {
            repository.deleteReplyPostMessagesByPostID(postId)
        }
    }

   /* fun deleteReplyPostMessagesByCommentID(commentId: String) {
        viewModelScope.launch {
            repository.deleteReplyPostMessagesByCommentID(commentId)
        }
    }*/
   fun deletePostsReplyByPostIds(ids: List<String>) {
       viewModelScope.launch {
           repository.deletePostsReplyByPostIds(ids)
       }
   }

    fun deleteReplyPostMessagesByCommentIDs(ids: List<String>) {
        viewModelScope.launch {
            repository.deleteReplyPostMessagesByCommentIDs(ids)
        }
    }
    fun deleteAllReplyPostMessages() {
        viewModelScope.launch {
            repository.deleteAllReplyPostMessages()
        }
    }

    /*suspend fun getReplayPostById(commentId: String): ReplyPostMessage? {
        return repository.getReplayPostById(commentId)
    }*/

    //Deleted messages
    fun insertDeletedMessage(message: List<DeleteMessages>) {
        repository.insertDeletedMessage(message)
    }

    fun getAlreadyInsertedDeletedIds(postIds: List<String>): List<String> {
        return repository.getAlreadyInsertedDeletedIds(postIds)
    }
    /*fun deleteDeletedMessageByPostID(messageId: String) {
        viewModelScope.launch {
            repository.deleteDeletedMessageByPostID(messageId)
        }
    }*/

}